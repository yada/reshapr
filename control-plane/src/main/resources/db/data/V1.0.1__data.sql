insert into organizations (id, name, description, icon, owner_id) values ('1', 'reshapr', 'Reshapr Organization', 'nope', null);

insert into gateway_groups (id, name, organization_id, labels) values ('1', 'Default Gateway Group', 'reshapr', '{"env": "dev", "team": "reshapr"}');

insert into quotas (id, organization_id, metric, enabled, m_limit, remaining) values ('1', 'reshapr', 'gateway-group.count', 'true', 1000, 999);
insert into quotas (id, organization_id, metric, enabled, m_limit, remaining) values ('2', 'reshapr', 'exposition.count', 'true', 1000, 1000);
insert into quotas (id, organization_id, metric, enabled, m_limit, remaining) values ('3', 'reshapr', 'gateway.count', 'true', 1000, 999);

insert into shared_resources (id, organization_id, type, resource_ids) values ('1', null, 'gateway-group', '["1"]');
