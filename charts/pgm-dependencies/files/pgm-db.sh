#!/bin/bash

psql=( psql -h localhost -U postgres -p "$POSTGRESQL_PORT_NUMBER" --set ON_ERROR_STOP=on )

PGPASSWORD="$POSTGRES_PASSWORD" "${psql[@]}" <<SQL
  CREATE DATABASE {{ .Values.global.pgm.database.name }};
SQL

psql+=( -d {{ .Values.global.pgm.database.name }} --set AUTOCOMMIT=off )

PGPASSWORD="$POSTGRES_PASSWORD" "${psql[@]}" --set pwd="$PGM_POSTGRES_PASSWORD" <<SQL
  CREATE USER {{ .Values.global.pgm.database.user }} WITH ENCRYPTED PASSWORD :'pwd';
  CREATE SCHEMA {{ .Values.global.pgm.accountService.database.schema }} AUTHORIZATION {{ .Values.global.pgm.database.user }};
  CREATE SCHEMA {{ .Values.global.pgm.notificationService.database.schema }} AUTHORIZATION {{ .Values.global.pgm.database.user }};
  CREATE SCHEMA {{ .Values.global.pgm.statisticsService.database.schema }} AUTHORIZATION {{ .Values.global.pgm.database.user }};
  COMMIT;
SQL