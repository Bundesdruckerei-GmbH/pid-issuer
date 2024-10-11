FROM gcr.io/distroless/java21-debian12:latest@sha256:4ef80b38c61881bdd4d682df9989a9816f4926f8fb41eaaf55d54a6affe6a6c2 AS builder
WORKDIR /opt/app
COPY app/target/*.jar /tmp/app.jar
RUN ["java", "-Djarmode=tools", "-jar", "/tmp/app.jar", "extract", "--destination", "/opt/app"]

# CDS creation:
RUN --mount=type=bind,source=./app/eid/,target=./eid/ \
    --mount=type=bind,source=./app/issuance/,target=./issuance/ \
    ["java", "-XX:ArchiveClassesAtExit=cds.jsa", "-Dspring.context.exit=onRefresh", "-jar", "app.jar"]

FROM gcr.io/distroless/java21-debian12:latest@sha256:4ef80b38c61881bdd4d682df9989a9816f4926f8fb41eaaf55d54a6affe6a6c2
WORKDIR /opt/app
COPY docker/bdr-ca /opt/bdr-ca/

COPY --from=builder /opt/app/ ./
CMD ["-XX:SharedArchiveFile=cds.jsa", "-Xshare:on", "-jar", "app.jar", "--spring.config.location=classpath:/application.properties,classpath:/application-bop.properties"]
