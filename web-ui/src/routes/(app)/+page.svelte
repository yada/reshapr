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
  import { auth } from '$lib/stores/auth.svelte.js';
  import { HugeiconsIcon } from '@hugeicons/svelte';
  import { ApiIcon, Building01Icon } from '@hugeicons/core-free-icons';
</script>

<svelte:head>
  <title>Dashboard — reShapr</title>
</svelte:head>

<div class="p-6">
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold tracking-tight">Dashboard</h1>
      <p class="text-muted-foreground">
        Welcome back, <strong>{auth.user?.username}</strong>.
        You're working in the <strong>{auth.user?.org}</strong> organization.
      </p>
    </div>

    <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <a
        href="/services"
        class="group rounded-lg border border-border bg-card p-6 transition-colors hover:border-primary/50 hover:bg-accent/50"
      >
        <div class="mb-2 text-primary">
          <HugeiconsIcon icon={ApiIcon} size={28} />
        </div>
        <h2 class="font-semibold text-card-foreground group-hover:text-primary">Services</h2>
        <p class="mt-1 text-sm text-muted-foreground">Browse and manage your API services.</p>
      </a>

      {#if auth.isAdmin}
        <a
          href="/admin/organizations"
          class="group rounded-lg border border-border bg-card p-6 transition-colors hover:border-primary/50 hover:bg-accent/50"
        >
          <div class="mb-2 text-primary">
            <HugeiconsIcon icon={Building01Icon} size={28} />
          </div>
          <h2 class="font-semibold text-card-foreground group-hover:text-primary">Organizations</h2>
          <p class="mt-1 text-sm text-muted-foreground">Manage organizations and users.</p>
        </a>
      {/if}
    </div>
  </div>
</div>
