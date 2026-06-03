<!--
  ~ Copyright The Reshapr Authors.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<script lang="ts">
  import { onMount } from 'svelte';
  import { Badge } from '$lib/components/ui/badge/index.js';
  import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '$lib/components/ui/card/index.js';

  interface Service {
    id: string;
    name: string;
    version: string;
    type: string;
    createdOn: string;
    organizationId: string;
  }

  let services = $state<Service[]>([]);
  let loading = $state(true);
  let error = $state('');

  onMount(async () => {
    await loadServices();
  });

  async function loadServices() {
    loading = true;
    error = '';

    try {
      const res = await fetch('/api/v1/services?page=0&size=20');

      if (!res.ok) {
        error = `Failed to load services (${res.status}): ${res.statusText}`;
        return;
      }

      services = await res.json();
    } catch {
      error = 'Network error. Please try again.';
    } finally {
      loading = false;
    }
  }

  function formatDate(iso: string): string {
    try {
      return new Date(iso).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      });
    } catch {
      return iso;
    }
  }

  function typeBadgeStyle(type: string): string {
    switch (type?.toUpperCase()) {
      case 'REST': return 'background-color: #6BBD4F; color: white;';
      case 'GRAPHQL': return 'background-color: #E10098; color: white;';
      case 'GRPC': return 'background-color: #c0a16b; color: white;';
      default: return '';
    }
  }
</script>

<svelte:head>
  <title>Services — reShapr</title>
</svelte:head>

<div class="p-6">
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold tracking-tight">Services</h1>
      <p class="text-muted-foreground">API services registered in your organization.</p>
    </div>

    {#if loading}
      <div class="flex justify-center py-12">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent"></div>
      </div>
    {:else if error}
      <Card>
        <CardContent class="py-8 text-center">
          <p class="text-destructive">{error}</p>
          <button
            class="mt-4 text-sm text-primary underline-offset-4 hover:underline"
            onclick={loadServices}
          >
            Retry
          </button>
        </CardContent>
      </Card>
    {:else if services.length === 0}
      <Card>
        <CardContent class="py-12 text-center">
          <p class="text-muted-foreground">No services found. Import a service to get started.</p>
        </CardContent>
      </Card>
    {:else}
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {#each services as service (service.id)}
          <Card class="flex flex-col transition-colors hover:border-primary/50 hover:bg-accent/50">
            <CardHeader class="flex-1">
              <div class="flex items-start justify-between gap-2 min-w-0">
                <CardTitle class="text-base leading-snug break-all min-w-0">{service.name}</CardTitle>
                <Badge class="shrink-0" style={typeBadgeStyle(service.type)}>
                  {service.type}
                </Badge>
              </div>
              <CardDescription class="mt-1">
                Version: <b>{service.version}</b>
              </CardDescription>
            </CardHeader>
            <CardContent class="pt-0">
              <div class="space-y-1 text-xs text-muted-foreground">
                <p>Created {formatDate(service.createdOn)}</p>
                <p class="truncate">
                  <code class="rounded bg-muted px-1 py-0.5 font-mono">{service.id}</code>
                </p>
              </div>
            </CardContent>
          </Card>
        {/each}
      </div>
    {/if}
  </div>
</div>

