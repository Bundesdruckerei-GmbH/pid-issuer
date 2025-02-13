FROM flyway/flyway:10.22-alpine@sha256:94a81ca7db9a9f24fd8acd7463fa4560cb8f66aae2aa64485c27eb296f5851cf
ENV FLYWAY_LOCATIONS=filesystem:/flyway/migrations
COPY bdr-ca/ /opt/bdr-ca/
COPY db/migrations /flyway/migrations
