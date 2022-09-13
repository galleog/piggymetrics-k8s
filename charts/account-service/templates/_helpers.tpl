{{/* vim: set filetype=mustache: */}}
{{/*
Get the name of the service config map.
*/}}
{{- define "account-service.configmap" -}}
{{- printf "%s-config" (include "common.names.fullname" .) -}}
{{- end -}}

{{/*
Get the role to get pods and and it to the default service account.
*/}}
{{- define "account-service.role" -}}
{{ default (include "common.names.fullname" .) .Values.role.name }}
{{- end -}}

{{/*
 Get the name of the service account to use
 */}}
{{- define "account-service.serviceAccount" -}}
{{- if .Values.serviceAccount.create -}}
    {{ default (include "common.names.fullname" .) .Values.serviceAccount.name }}
{{- else -}}
    {{ default "default" .Values.serviceAccount.name }}
{{- end -}}
{{- end -}}