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
	import {
		artifactTypeLabel,
		EDITABLE_KINDS,
		isEditableArtifactType,
		matchesTypeFilter,
		TYPE_FILTER_OPTIONS,
		type ArtifactRef,
		type ArtifactTypeFilter,
		type ReshaprArtifactKind
	} from '$lib/artifacts/index.js';
	import { Badge } from '$lib/components/ui/badge/index.js';
	import { Button } from '$lib/components/ui/button/index.js';
	import { Label } from '$lib/components/ui/label/index.js';
	import * as Select from '$lib/components/ui/select/index.js';
	import * as Table from '$lib/components/ui/table/index.js';

	let {
		serviceId,
		artifacts = [],
		loading = false
	}: {
		serviceId: string;
		artifacts?: ArtifactRef[];
		loading?: boolean;
	} = $props();

	let typeFilter = $state<ArtifactTypeFilter>('all');
	let createKind = $state<ReshaprArtifactKind>('Prompts');

	const filterLabel = $derived(
		TYPE_FILTER_OPTIONS.find((opt) => opt.value === typeFilter)?.label ?? 'All types'
	);

	const createKindLabel = $derived(
		EDITABLE_KINDS.find((def) => def.kind === createKind)?.label ?? createKind
	);

	const filtered = $derived(
		artifacts.filter((artifact) => matchesTypeFilter(artifact, typeFilter))
	);

	function artifactHref(id: string): string {
		return `/services/${serviceId}/artifacts/${id}`;
	}

	function createHref(): string {
		return `/services/${serviceId}/artifacts/new?kind=${encodeURIComponent(createKind)}`;
	}
</script>

<div class="mb-4 flex flex-wrap items-end justify-between gap-4">
	<div class="space-y-2">
		<Label for="artifact-type-filter">Filter by type</Label>
		<Select.Root type="single" bind:value={typeFilter}>
			<Select.Trigger id="artifact-type-filter" class="w-[min(100%,16rem)]">
				{filterLabel}
			</Select.Trigger>
			<Select.Content>
				{#each TYPE_FILTER_OPTIONS as opt (opt.value)}
					<Select.Item value={opt.value}>{opt.label}</Select.Item>
				{/each}
			</Select.Content>
		</Select.Root>
	</div>

	<div class="flex flex-wrap items-end gap-2">
		<div class="space-y-2">
			<Label for="artifact-create-kind">New custom artifact</Label>
			<Select.Root type="single" bind:value={createKind}>
				<Select.Trigger id="artifact-create-kind" class="w-[min(100%,14rem)]">
					{createKindLabel}
				</Select.Trigger>
				<Select.Content>
					{#each EDITABLE_KINDS as def (def.kind)}
						<Select.Item value={def.kind}>{def.label}</Select.Item>
					{/each}
				</Select.Content>
			</Select.Root>
		</div>
		<Button href={createHref()} class="mb-0">Create</Button>
	</div>
</div>

<div class="rounded-lg border">
	<Table.Root>
		<Table.Header>
			<Table.Row>
				<Table.Head>Name</Table.Head>
				<Table.Head>Type</Table.Head>
				<Table.Head>Role</Table.Head>
				<Table.Head>Source</Table.Head>
				<Table.Head class="text-right">Actions</Table.Head>
			</Table.Row>
		</Table.Header>
		<Table.Body>
			{#if loading}
				<Table.Row>
					<Table.Cell colspan={5} class="text-muted-foreground">Loading…</Table.Cell>
				</Table.Row>
			{:else if filtered.length === 0}
				<Table.Row>
					<Table.Cell colspan={5} class="text-muted-foreground">
						{artifacts.length === 0
							? 'No artifacts for this service.'
							: 'No artifacts match this filter.'}
					</Table.Cell>
				</Table.Row>
			{:else}
				{#each filtered as artifact (artifact.id)}
					<Table.Row>
						<Table.Cell>
							<div class="font-medium">{artifact.name}</div>
							<div class="text-muted-foreground font-mono text-xs">{artifact.id}</div>
						</Table.Cell>
						<Table.Cell>
							<span class="text-sm">{artifactTypeLabel(artifact.type)}</span>
						</Table.Cell>
						<Table.Cell>
							{#if artifact.mainArtifact}
								<Badge variant="default">Main</Badge>
							{:else}
								<Badge variant="secondary">Attached</Badge>
							{/if}
							{#if !isEditableArtifactType(artifact.type)}
								<Badge variant="outline" class="ml-1">Read-only</Badge>
							{/if}
						</Table.Cell>
						<Table.Cell class="max-w-[12rem] truncate text-sm" title={artifact.sourceArtifact ?? undefined}>
							{artifact.sourceArtifact ?? '—'}
						</Table.Cell>
						<Table.Cell class="text-right">
							<Button variant="link" size="sm" href={artifactHref(artifact.id)}>View</Button>
							{#if isEditableArtifactType(artifact.type)}
								<Button variant="link" size="sm" href={artifactHref(artifact.id)}>Edit</Button>
							{/if}
						</Table.Cell>
					</Table.Row>
				{/each}
			{/if}
		</Table.Body>
	</Table.Root>
</div>
