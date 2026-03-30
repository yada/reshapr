#!/bin/bash

docker run -it --rm -p 7777:7777 -e RESHAPR_CTRL_HOST=host.docker.internal \
  -e RESHAPR_CTRL_PORT=5555 \
  -e RESHAPR_CTRL_TOKEN=reshapr-my-super-secret-token-xyz \
  --add-host=host.docker.internal:host-gateway \
  registry.reshapr.io/reshapr/reshapr-proxy:nightly