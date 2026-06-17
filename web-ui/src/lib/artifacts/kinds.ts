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

import type { ArtifactType, ArtifactTypeFilter, ReshaprArtifactKind } from './types.js';

export type KindDefinition = {
	kind: ReshaprArtifactKind;
	artifactType: ArtifactType;
	label: string;
	schemaPath: string;
	isEditable: true;
};

/** Registry aligned with `ReshaprArtifactBuilder` kind → schema mappings. */
export const EDITABLE_KINDS: KindDefinition[] = [
	{
		kind: 'Prompts',
		artifactType: 'RESHAPR_PROMPTS',
		label: 'Prompts',
		schemaPath: '/schemas/Prompts-v1alpha1-schema.json',
		isEditable: true
	},
	{
		kind: 'CustomTools',
		artifactType: 'RESHAPR_CUSTOM_TOOLS',
		label: 'Custom tools',
		schemaPath: '/schemas/CustomTools-v1alpha1-schema.json',
		isEditable: true
	},
	{
		kind: 'Resources',
		artifactType: 'RESHAPR_RESOURCES',
		label: 'Resources',
		schemaPath: '/schemas/Resources-v1alpha1-schema.json',
		isEditable: true
	},
	{
		kind: 'ToolsOutputFilters',
		artifactType: 'RESHAPR_TOOLS_OUTPUT_FILTERS',
		label: 'Tools output filters',
		schemaPath: '/schemas/ToolsOutputFilters-v1alpha1-schema.json',
		isEditable: true
	}
];

const KIND_BY_TYPE = new Map(
	EDITABLE_KINDS.map((def) => [def.artifactType, def] as const)
);

export const ARTIFACT_TYPE_LABELS: Record<ArtifactType, string> = {
	JSON_SCHEMA: 'JSON Schema',
	OPEN_API_SPEC: 'OpenAPI spec',
	GRAPHQL_SCHEMA: 'GraphQL schema',
	PROTOBUF_SCHEMA: 'Protobuf schema',
	PROTOBUF_DESCRIPTOR: 'Protobuf descriptor',
	JSON_FRAGMENT: 'JSON fragment',
	RESHAPR_PROMPTS: 'Prompts',
	RESHAPR_CUSTOM_TOOLS: 'Custom tools',
	RESHAPR_RESOURCES: 'Resources',
	RESHAPR_TOOLS_OUTPUT_FILTERS: 'Tools output filters'
};

export const TYPE_FILTER_OPTIONS: { value: ArtifactTypeFilter; label: string }[] = [
	{ value: 'all', label: 'All types' },
	{ value: 'main', label: 'Main specification' },
	...EDITABLE_KINDS.map((def) => ({
		value: def.artifactType as ArtifactTypeFilter,
		label: def.label
	})),
	{ value: 'OPEN_API_SPEC', label: 'OpenAPI spec' },
	{ value: 'GRAPHQL_SCHEMA', label: 'GraphQL schema' },
	{ value: 'PROTOBUF_SCHEMA', label: 'Protobuf schema' },
	{ value: 'PROTOBUF_DESCRIPTOR', label: 'Protobuf descriptor' },
	{ value: 'JSON_SCHEMA', label: 'JSON Schema' },
	{ value: 'JSON_FRAGMENT', label: 'JSON fragment' }
];

export function getKindDefinition(kind: ReshaprArtifactKind): KindDefinition | undefined {
	return EDITABLE_KINDS.find((def) => def.kind === kind);
}

export function getKindForArtifactType(type: ArtifactType): KindDefinition | undefined {
	return KIND_BY_TYPE.get(type);
}

export function isEditableArtifactType(type: ArtifactType): boolean {
	return KIND_BY_TYPE.has(type);
}

export function artifactTypeLabel(type: ArtifactType): string {
	return ARTIFACT_TYPE_LABELS[type] ?? type;
}

export function matchesTypeFilter(
	artifact: { type: ArtifactType; mainArtifact: boolean },
	filter: ArtifactTypeFilter
): boolean {
	if (filter === 'all') return true;
	if (filter === 'main') return artifact.mainArtifact;
	return artifact.type === filter;
}
