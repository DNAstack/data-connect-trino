package com.dnastack.ga4gh.dataconnect.adapter.shared;

import com.dnastack.audit.aspect.AuditEventCustomizer;
import com.dnastack.audit.model.AuditEventBody;
import com.dnastack.ga4gh.dataconnect.model.TableData;

import java.lang.reflect.Parameter;

public class QueryJobAppenderAuditEventCustomizer extends AuditEventCustomizer {

    @Override
    public AuditEventBody afterController(
        AuditEventBody eventBody, Parameter[] parameters, Object[] args, Object returnValue
    ) {

        if (returnValue instanceof TableData) {
            TableData tableData = (TableData) returnValue;
            if (tableData.getQueryJob() != null) {
                eventBody.getExtraArguments().put("queryJobId", tableData.getQueryJob().getId());
                eventBody.getExtraArguments().put("orginalTraceId", tableData.getQueryJob().getOriginalTraceId());
            }
        }

        return eventBody;
    }

}
