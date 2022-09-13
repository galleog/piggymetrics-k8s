#!/bin/bash

psql=( psql -h localhost -U postgres -p "$POSTGRESQL_PORT_NUMBER" --set ON_ERROR_STOP=on )

PGPASSWORD="$POSTGRES_PASSWORD" "${psql[@]}" --set pwd="$KEYCLOAK_POSTGRES_PASSWORD" <<SQL
  CREATE USER {{ .Values.keycloakx.database.username }} WITH ENCRYPTED PASSWORD :'pwd';
  CREATE DATABASE {{ .Values.keycloakx.database.database }} OWNER {{ .Values.keycloakx.database.username }};
SQL