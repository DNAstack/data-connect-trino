{{/*
"true" when this install supports the e2e tests: their Wallet client and permissions, and the helm test that runs them.
The chart's e2etest.enabled wins over global.e2etest.enabled, and both default to off.
*/}}
{{- define "dataConnectTrino.e2eTestEnabled" -}}
{{- $chart := .Values.e2etest | default dict -}}
{{- $global := (.Values.global).e2etest | default dict -}}
{{- if not (kindIs "invalid" $chart.enabled) -}}
{{- $chart.enabled -}}
{{- else -}}
{{- $global.enabled | default false -}}
{{- end -}}
{{- end }}
