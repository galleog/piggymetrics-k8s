{{/* vim: set filetype=mustache: */}}
{{/*
Get a list of kafka URLs.
*/}}
{{- define "pgm-dependencies.kafka.brokers" -}}
{{- $count := .Values.kafka.replicaCount | toString | int -}}
{{- $name := include "common.names.fullname" .Subcharts.kafka -}}
{{- $port := .Values.kafka.service.ports.client | toString -}}
{{- range $index, $e := until $count -}}
{{- printf "%s-%d.%s-headless.%s.svc.cluster.local:%s" $name $index $name $.Release.Namespace $port -}}
{{- if lt $index (sub $count 1) -}}
{{- printf "," -}}
{{- end -}}
{{- end -}}
{{- end }}

{{/*
Get the PostgreSQL host URL.
*/}}
{{- define "pgm-dependencies.postgresql.host" -}}
{{- $statefullset := include "postgresql.primary.fullname" .Subcharts.postgresql -}}
{{- $headlessSvc := include "postgresql.primary.svc.headless" .Subcharts.postgresql -}}
{{- printf "%s-0.%s.%s.svc.cluster.local" $statefullset $headlessSvc $.Release.Namespace -}}
{{- end }}

{{/*
Get the Keycloak host URL.
*/}}
{{- define "pgm-dependencies.keycloak.host" -}}
{{- $name := include "keycloak.fullname" .Subcharts.keycloakx -}}
{{- printf "%s-http.%s.svc.cluster.local" $name $.Release.Namespace -}}
{{- end }}