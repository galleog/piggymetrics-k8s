{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "pgm-dependencies.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "pgm-dependencies.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "pgm-dependencies.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "pgm-dependencies.labels" -}}
app.kubernetes.io/name: {{ include "pgm-dependencies.name" . }}
helm.sh/chart: {{ include "pgm-dependencies.chart" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Call a nested template.
*/}}
{{- define "call-nested" }}
{{- $dot := index . 0 }}
{{- $subchart := index . 1 }}
{{- $template := index . 2 }}
{{- include $template (dict "Chart" (dict "Name" $subchart) "Values" (index $dot.Values $subchart) "Release" $dot.Release "Capabilities" $dot.Capabilities) }}
{{- end }}

{{/*
Create a list of kafka URLs.
*/}}
{{- define "pgm-dependencies.kafka.brokers" -}}
{{- $count := .Values.kafka.replicaCount | toString | int -}}
{{- $name := include "call-nested" (list . "kafka" "kafka.fullname") -}}
{{- range $index, $e := until $count -}}
{{- printf "%s-%d.%s-headless.%s.svc.cluster.local:%s" $name $index $name $.Release.Namespace $.Values.kafka.service.port -}}
{{- if lt $index (sub $count 1) -}}
{{- printf "," -}}
{{- end -}}
{{- end -}}
{{- end }}

{{/*
Create a postgresql host URL.
*/}}
{{- define "pgm-dependencies.postgresql.host" -}}
{{- $name := include "call-nested" (list . "postgresql" "postgresql.fullname") -}}
{{- $masterName := include "call-nested" (list . "postgresql" "postgresql.master.fullname") -}}
{{- printf "%s-0.%s-headless.%s.svc.cluster.local" $masterName $name $.Release.Namespace -}}
{{- end }}

{{/*
Create a keycloak host URL.
*/}}
{{- define "pgm-dependencies.keycloak.host" -}}
{{- $name := include "call-nested" (list . "keycloak" "keycloak.fullname") -}}
{{- printf "%s-http.%s.svc.cluster.local" $name $.Release.Namespace -}}
{{- end }}

