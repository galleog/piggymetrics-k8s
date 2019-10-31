#!/bin/bash

psql=( psql -h localhost -U "$POSTGRES_USER" -p "$POSTGRESQL_PORT_NUMBER" --set ON_ERROR_STOP=on )

PGPASSWORD="$POSTGRES_PASSWORD" "${psql[@]}" --set pwd="$KEYCLOAK_POSTGRES_PASSWORD" <<SQL
  CREATE USER {{ .Values.keycloak.keycloak.persistence.dbUser }} WITH ENCRYPTED PASSWORD :'pwd';
  CREATE DATABASE {{ .Values.keycloak.keycloak.persistence.dbName }} OWNER {{ .Values.keycloak.keycloak.persistence.dbUser }};
SQL