package eu.exeris.caps.audittrail.internal;

import eu.exeris.caps.audittrail.api.AuditLogSink;

/**
 * ServiceBoundary capability lifecycle class in internal package providing AuditLogSink.
 */
public final class AuditTrailModule extends eu.exeris.caps.audittrail.AuditTrailModule {

    public AuditTrailModule() {
        super();
    }

    public AuditTrailModule(AuditLogSink auditLogSink) {
        super(auditLogSink);
    }
}
