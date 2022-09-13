{{/* vim: set filetype=mustache: */}}
{{/*
Get the service account to use
*/}}
{{- define "piggymetrics.serviceAccount" -}}
{{ default "default" .Values.global.pgm.serviceAccount }}
{{- end -}}

{{/*
Get the role to get pods and and it to the default service account.
*/}}
{{- define "piggymetrics.role" -}}
{{ default (include "common.names.fullname" .) .Values.role.name }}
{{- end -}}

{{/*
Get the Istio virtual service config map.
*/}}
{{- define "piggymetrics.vsvcCM" -}}
{{- printf "%s-istio-vsvc-config" (include "common.names.fullname" .) -}}
{{- end -}}

{{/*
Get the name of Helm hook.
*/}}
{{- define "piggymetrics.hook.name" -}}
{{- printf "%s-hook" (include "common.names.fullname" .) -}}
{{- end -}}