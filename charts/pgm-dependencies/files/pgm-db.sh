#!/bin/bash

psql=( psql -h localhost -U "$POSTGRES_USER" -p "$POSTGRESQL_PORT_NUMBER" --set ON_ERROR_STOP=on )

PGPASSWORD="$POSTGRES_PASSWORD" "${psql[@]}" <<SQL
  CREATE DATABASE {{ .Values.global.pgm.dbName }};
SQL

psql+=( -d {{ .Values.global.pgm.dbName }} --set AUTOCOMMIT=off )

PGPASSWORD="$POSTGRES_PASSWORD" "${psql[@]}" --set pwd="$PGM_POSTGRES_PASSWORD" <<SQL
  CREATE USER {{ .Values.global.pgm.dbUser }} WITH ENCRYPTED PASSWORD :'pwd';
  CREATE SCHEMA {{ .Values.global.pgm.accountService.dbSchema }} AUTHORIZATION {{ .Values.global.pgm.dbUser }};
  CREATE SCHEMA {{ .Values.global.pgm.notificationService.dbSchema }} AUTHORIZATION {{ .Values.global.pgm.dbUser }};
  CREATE SCHEMA {{ .Values.global.pgm.statisticsService.dbSchema }} AUTHORIZATION {{ .Values.global.pgm.dbUser }};
  COMMIT;
SQL