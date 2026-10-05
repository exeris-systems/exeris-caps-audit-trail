package eu.exeris.caps.audittrail.internal;

import eu.exeris.caps.audittrail.api.AuditEvent;
import eu.exeris.caps.audittrail.api.AuditLogSink;
import eu.exeris.caps.audittrail.api.AuditQuery;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;

/**
 * Thread-safe, lock-free in-memory implementation of {@link AuditLogSink} backed by a {@link ConcurrentLinkedDeque}.
 */
public final class InMemoryAuditLogSink implements AuditLogSink {

    private static final Comparator<AuditEvent> TIMESTAMP_DESC =
            Comparator.comparing(AuditEvent::timestamp, Comparator.reverseOrder())
                    .thenComparing(AuditEvent::eventId);

    private final ConcurrentLinkedDeque<AuditEvent> events = new ConcurrentLinkedDeque<>();

    @Override
    public void emit(AuditEvent event) {
        Objects.requireNonNull(event, "event");
        events.addFirst(event);
    }

    @Override
    public List<AuditEvent> query(AuditQuery query) {
        Predicate<AuditEvent> predicate = filterPredicate(query);
        int offset = query != null ? query.offset() : 0;
        int limit = query != null ? query.limit() : 50;

        return events.stream()
                .filter(predicate)
                .sorted(TIMESTAMP_DESC)
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public long count(AuditQuery query) {
        Predicate<AuditEvent> predicate = filterPredicate(query);
        return events.stream()
                .filter(predicate)
                .count();
    }

    private Predicate<AuditEvent> filterPredicate(AuditQuery q) {
        if (q == null) {
            return event -> true;
        }
        return event -> {
            if (q.resourceType() != null && !q.resourceType().isBlank()
                    && !q.resourceType().equalsIgnoreCase(event.resourceType())) {
                return false;
            }
            if (q.resourceId() != null && !q.resourceId().isBlank()
                    && !q.resourceId().equals(event.resourceId())) {
                return false;
            }
            if (q.action() != null && !q.action().isBlank()
                    && !q.action().equalsIgnoreCase(event.action())) {
                return false;
            }
            if (q.actorId() != null && !q.actorId().isBlank()
                    && !q.actorId().equalsIgnoreCase(event.actorId())) {
                return false;
            }
            if (q.from() != null && event.timestamp().isBefore(q.from())) {
                return false;
            }
            if (q.to() != null && event.timestamp().isAfter(q.to())) {
                return false;
            }
            return true;
        };
    }
}
