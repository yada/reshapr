
create table service_accounts (
    id varchar(255) not null,
    name varchar(255) not null,
    description varchar(255),
    k8s_subject varchar(255),
    valid_until TIMESTAMP not null,
    allowed_organizations JSONB,
    primary key (id),
    unique (name),
    unique (k8s_subject)
);

