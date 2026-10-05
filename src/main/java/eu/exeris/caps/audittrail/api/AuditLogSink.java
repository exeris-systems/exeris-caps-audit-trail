package eu.exeris.caps.audittrail.api;

import java.util.List;

/**
 * Event sink and query interface for the platform audit trail capability.
 */
public interface AuditLogSink {

    /**
     * Emits an audit event into the log sink.
     *
     * @param event audit event to record
     */
    void emit(AuditEvent event);

    /**
     * Queries matching audit events filtered, sorted by timestamp descending, and paginated.
     *
     * @param query query criteria
     * @return list of matching events
     */
    List<AuditEvent> query(AuditQuery query);

    /**
     * Counts the total number of audit events matching the query criteria (ignoring limit/offset).
     *
     * @param query query criteria
     * @return count of matching events
     */
    long count(AuditQuery query);
}
