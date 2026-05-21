/*
 * Copyright The Reshapr Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reshapr.ctrl.security;

import org.jboss.logging.Logger;
import org.jose4j.http.SimpleGet;
import org.jose4j.http.SimpleResponse;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Verifies Kubernetes service account tokens using the Kubernetes API server's JWKS endpoint.
 * This class is responsible for validating the integrity and claims of Kubernetes-issued
 * JWT tokens, ensuring they are properly signed and include the expected audience and issuer.
 *
 * The class uses the following configurations:
 * - The issuer is expected to be "https://kubernetes.default.svc".
 * - The JWKS endpoint is located at "/openid/v1/jwks" under the Kubernetes API server.
 * - The audience is expected to match "https://app.reshapr.io".
 *
 * Features:
 * - Verifies token signature using keys fetched from the JWKS endpoint.
 * - Ensures required claims like expiration time, subject, issuer, and audience are present and valid.
 * - Extracts Kubernetes identity (namespace and service account name) from the token.
 *
 * Usage Notes:
 * - Use {@link #create()} to instantiate the verifier. Direct instantiation is not allowed.
 * - This implementation assumes the application is running inside the same cluster as the JWT issuer,
 *   and the cluster's CA is accessible to the JVM trust store.
 *
 * Dependencies:
 * - Relies on the JwtConsumerBuilder library for JWT processing.
 * - Uses HttpsJwks for handling the JWKS endpoint.
 * @author laurent
 */
public class KubernetesTokenVerifier {

   private final Logger logger = Logger.getLogger(getClass());

   private static final String K8S_ISSUER = "https://kubernetes.default.svc.cluster.local";
   private static final String K8S_JWKS_URI = K8S_ISSUER + "/openid/v1/jwks";
   private static final String EXPECTED_AUDIENCE = "https://app.reshapr.io";

   /** Path to the Kubernetes cluster CA certificate, automatically mounted in every pod. */
   private static final String K8S_CA_CERT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";

   /** Path to the pod's own ServiceAccount token, used to authenticate against the K8s API server. */
   private static final String K8S_SA_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";

   private JwtConsumer consumer;

   private KubernetesTokenVerifier() {
      // To prevent direct default instantiation.
   }

   public static KubernetesTokenVerifier create() {
      KubernetesTokenVerifier instance = new KubernetesTokenVerifier();
      instance.init();
      return instance;
   }

   private void init() {
      var httpsJwks = new HttpsJwks(K8S_JWKS_URI);
      httpsJwks.setDefaultCacheDuration(3600L);

      // Build an SSLContext that trusts the Kubernetes cluster CA certificate
      // mounted at /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
      // and authenticate with the pod's own SA token (required when JWKS endpoint is RBAC-protected).
      try {
         SSLContext sslContext = buildK8sSslContext();
         String saToken = Files.readString(Path.of(K8S_SA_TOKEN_PATH)).trim();
         httpsJwks.setSimpleHttpGet(new K8sAuthenticatedGet(sslContext, saToken));
         logger.info("K8s cluster CA certificate and SA token loaded for JWKS endpoint access");
      } catch (IOException | GeneralSecurityException e) {
         logger.warnf("Could not load K8s cluster CA/token from %s, falling back to JVM truststore: %s",
               K8S_CA_CERT_PATH, e.getMessage());
      }

      consumer = new JwtConsumerBuilder()
            .setRequireExpirationTime()
            .setRequireSubject()
            .setExpectedIssuer(K8S_ISSUER)
            .setExpectedAudience(EXPECTED_AUDIENCE)
            .setVerificationKeyResolver(new HttpsJwksVerificationKeyResolver(httpsJwks))
            .build();

      logger.info("K8s token verifier initialized (same-cluster mode)");
   }

   /**
    * Build an SSLContext that trusts the Kubernetes cluster CA certificate.
    * The CA cert is mounted automatically by kubelet at {@value K8S_CA_CERT_PATH}.
    */
   private SSLContext buildK8sSslContext() throws IOException, GeneralSecurityException {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      X509Certificate caCert;
      try (InputStream caInput = Files.newInputStream(Path.of(K8S_CA_CERT_PATH))) {
         caCert = (X509Certificate) cf.generateCertificate(caInput);
      }

      KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      trustStore.load(null, null);
      trustStore.setCertificateEntry("k8s-cluster-ca", caCert);

      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(trustStore);

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, tmf.getTrustManagers(), null);
      return sslContext;
   }

   /** Verify this token has been issued by the local cluster with the correct audience and validity date. */
   public Optional<K8sIdentity> verify(String token) {
      try {
         JwtClaims claims = consumer.processToClaims(token);
         String sub = claims.getSubject();
         String[] parts = sub.split(":");
         if (parts.length != 4) {
            return Optional.empty();
         }

         return Optional.of(new K8sIdentity(parts[2], parts[3]));
      } catch (InvalidJwtException | MalformedClaimException e) {
         logger.warnf("K8s token verification failed: %s", e.getMessage());
         return Optional.empty();
      }
   }

   public record K8sIdentity(String namespace, String serviceAccountName) {}

   /**
    * A custom {@link SimpleGet} implementation that uses a specific SSLContext (to trust the K8s cluster CA)
    * and sends a Bearer token (the pod's own SA token) to authenticate against the K8s API server JWKS endpoint.
    */
   private record K8sAuthenticatedGet(SSLContext sslContext, String bearerToken) implements SimpleGet {
      @Override
      public SimpleResponse get(String location) throws IOException {
         var url = URI.create(location).toURL();
         var conn = (HttpsURLConnection) url.openConnection();
         conn.setSSLSocketFactory(sslContext.getSocketFactory());
         conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
         conn.setRequestMethod("GET");
         conn.setConnectTimeout(5_000);
         conn.setReadTimeout(5_000);

         int statusCode = conn.getResponseCode();
         String body;
         try (InputStream is = (statusCode < 400) ? conn.getInputStream() : conn.getErrorStream()) {
            body = (is != null) ? new String(is.readAllBytes(), StandardCharsets.UTF_8) : "";
         }

         Map<String, List<String>> headers = conn.getHeaderFields();
         return new SimpleResponse() {
            @Override public int getStatusCode() { return statusCode; }
            @Override public String getStatusMessage() { return ""; }
            @Override public Collection<String> getHeaderNames() { return headers.keySet(); }
            @Override public List<String> getHeaderValues(String name) { return headers.getOrDefault(name, List.of()); }
            @Override public String getBody() { return body; }
         };
      }
   }
}
