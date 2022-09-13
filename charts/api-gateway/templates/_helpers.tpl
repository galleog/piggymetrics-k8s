{{/* vim: set filetype=mustache: */}}
{{/*
Get the name of the service config map.
*/}}
{{- define "api-gateway.configmap" -}}
{{- printf "%s-config" (include "common.names.fullname" .) -}}
{{- end -}}

{{/*
 Get the name of the service account to use
 */}}
{{- define "api-gateway.serviceAccount" -}}
{{- if .Values.serviceAccount.create -}}
    {{ default (include "common.names.fullname" .) .Values.serviceAccount.name }}
{{- else -}}
    {{ default "default" .Values.serviceAccount.name }}
{{- end -}}
{{- end -}}