package eu.exeris.caps.audittrail.api;

import java.time.Instant;

/**
 * Filter and pagination query parameters for querying audit log events.
 *
 * @param resourceType optional resource category filter
 * @param resourceId   optional resource identifier filter
 * @param action       optional action type filter
 * @param actorId      optional actor principal filter
 * @param from         optional start timestamp filter (inclusive)
 * @param to           optional end timestamp filter (inclusive)
 * @param limit        maximum results to return (default 50)
 * @param offset       pagination offset (default 0)
 */
public record AuditQuery(
        String resourceType,
        String resourceId,
        String action,
        String actorId,
        Instant from,
        Instant to,
        int limit,
        int offset
) {

    public AuditQuery {
        if (limit <= 0) {
            limit = 50;
        }
        if (offset < 0) {
            offset = 0;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String resourceType;
        private String resourceId;
        private String action;
        private String actorId;
        private Instant from;
        private Instant to;
        private int limit = 50;
        private int offset = 0;

        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder actorId(String actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder from(Instant from) {
            this.from = from;
            return this;
        }

        public Builder to(Instant to) {
            this.to = to;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public AuditQuery build() {
            return new AuditQuery(resourceType, resourceId, action, actorId, from, to, limit, offset);
        }
    }
}
