package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrinoError {

    @JsonProperty("message")
    private String message;

    @JsonProperty("errorCode")
    private Integer errorCode;

    @JsonProperty("errorName")
    private String errorName;

    @JsonProperty("errorType")
    private String errorType;

    private FailureInfo failureInfo = new FailureInfo(); // ensure this is never null

    public TrinoError withoutStackTraces() {
        return new TrinoError(message, errorCode, errorName, errorType, failureInfo.withoutStackTraces());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FailureInfo {
        private String type;
        private String message;
        private ErrorInfo errorInfo;
        private FailureInfo cause;
        private List<FailureInfo> suppressed;
        private List<String> stack;

        public String getMessageOfCauseType(String fqcn) {
            if (type.equals(fqcn)) {
                return message;
            }
            if (cause == null) {
                return null;
            }
            return cause.getMessageOfCauseType(fqcn);
        }

        public FailureInfo withoutStackTraces() {
            return new FailureInfo(
                    type,
                    message,
                    errorInfo,
                    cause == null ? null : cause.withoutStackTraces(),
                    suppressed == null ? null : suppressed.stream().map(FailureInfo::withoutStackTraces).toList(),
                    null);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(50000);
            appendToString(sb, "", List.of());
            return sb.toString();
        }

        /**
         * Appends a string representation of this failure to the given StringBuilder. Aims to replicate the format of
         * a typical Java stack trace, with common lines omitted and suppressed exceptions indented.
         */
        private void appendToString(StringBuilder sb, String prefix, List<String> enclosingStack) {
            sb.append(prefix).append(type).append(": ").append(message).append(" [").append(errorInfo).append("]");

            int trailingLinesInCommon = trailingLinesInCommon(stack, enclosingStack);
            for (String stackLine : stack.subList(0, stack.size() - trailingLinesInCommon)) {
                sb.append("\n").append(prefix).append("\tat ").append(stackLine);
            }
            if (trailingLinesInCommon > 0) {
                sb.append("\n").append(prefix).append("\t... ").append(trailingLinesInCommon).append(" more");
            }

            if (cause != null) {
                sb.append("\n").append(prefix).append("Caused by: ");
                cause.appendToString(sb, prefix, stack);
            }

            for (FailureInfo suppressedInfo : suppressed) {
                sb.append("\n").append(prefix).append("\tSuppressed: ");
                suppressedInfo.appendToString(sb, prefix + "\t", stack);
            }
        }

        /** Returns the number of lines in common between the two stacks, starting from the end. */
        private int trailingLinesInCommon(List<String> stack, List<String> enclosingStack) {
            int i = 0;
            while (i < stack.size()
                   && i < enclosingStack.size()
                   && stack.get(stack.size() - i - 1).equals(enclosingStack.get(enclosingStack.size() - i - 1))) {
                i++;
            }
            return i;
        }
    }

    @Data
    public static class ErrorInfo {
        private String code;
        private String name;
        private String type;
    }
}
