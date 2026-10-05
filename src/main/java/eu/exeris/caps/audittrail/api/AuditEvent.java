package eu.exeris.caps.audittrail.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit log record capturing an activity, state transition, or administrative action.
 *
 * @param eventId      unique event identifier
 * @param timestamp    timestamp when the action occurred
 * @param action       action identifier (e.g., CONTENT_CREATED, CONTENT_UPDATED, CONTENT_PUBLISHED,
 *                     CONTENT_SCHEDULED, CONTENT_ROLLBACK, CONTENT_DELETED, CONTENT_IMPORTED, ASSET_UPLOADED)
 * @param resourceType category or model of resource (e.g. content type slug, "asset", "system")
 * @param resourceId   identifier of the affected resource
 * @param actorId      user, agent, or system principal that performed the action
 * @param tenantId     tenant boundary identifier (ADR-012)
 * @param details      arbitrary key-value metadata associated with the event
 */
public record AuditEvent(
        UUID eventId,
        Instant timestamp,
        String action,
        String resourceType,
        String resourceId,
        String actorId,
        String tenantId,
        Map<String, Object> details
) {

    public AuditEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        action = Objects.requireNonNull(action, "action");
        resourceType = resourceType != null ? resourceType : "";
        resourceId = resourceId != null ? resourceId : "";
        actorId = actorId != null ? actorId : "system";
        if (details != null) {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>(details);
            details = java.util.Collections.unmodifiableMap(map);
        } else {
            details = Map.of();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID eventId;
        private Instant timestamp;
        private String action;
        private String resourceType = "";
        private String resourceId = "";
        private String actorId = "system";
        private String tenantId = "default";
        private Map<String, Object> details = Map.of();

        public Builder eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder actorId(String actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            if (details != null) {
                java.util.Map<String, Object> map = new java.util.LinkedHashMap<>(details);
                this.details = java.util.Collections.unmodifiableMap(map);
            } else {
                this.details = Map.of();
            }
            return this;
        }

        public AuditEvent build() {
            return new AuditEvent(eventId, timestamp, action, resourceType, resourceId, actorId, tenantId, details);
        }
    }
}
