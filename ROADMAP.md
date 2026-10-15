---
title: "Audit trail capability roadmap"
type: roadmap
visibility: public
owning-repo: exeris-caps-audit-trail
status: active
last-verified: 2026-10-15
---

# `exeris-caps-audit-trail` roadmap

`exeris-caps-audit-trail` is a proof of concept. It provides `AuditLogSink` at version `0.1.0` and one in-memory implementation. This file records what that proof of concept does not cover, read together with the code. A pull request that closes a gap removes or rewrites its entry in the same change.

Milestones are versions, not dates. An item under "Before 0.1.0" blocks the first release and has a tracking issue in the `0.1.0` milestone. An item under "Later" does not block it.

## Before 0.1.0

### Tenant-scoped queries

`AuditEvent` carries a `tenantId`, but `AuditQuery` has no tenant field, so `query` and `count` cannot be bounded to one tenant. `InMemoryAuditLogSink` filters on resource type, resource id, action, actor and time range only, and returns the events of every tenant. A null `tenantId` is also treated differently by the builder, which defaults it to `default`, and by the record constructor, which keeps it null. The Identity SKU records events for several tenants, so a query surface without a tenant bound leaks one tenant's records to another. Code: `api/AuditQuery.java`, `api/AuditEvent.java`, `internal/InMemoryAuditLogSink.java`. Tracking issue: [#10](https://github.com/exeris-systems/exeris-caps-audit-trail/issues/10).

### Immutability of event details

`AuditEvent` is documented as an immutable record, but `details` is copied one level deep. A mutable value inside it can change after `emit`, and the in-memory sink returns the stored instance, so a queried event can differ from the one emitted. The guarantee an audit trail exists to give holds today only for flat values. Code: `api/AuditEvent.java`, `internal/InMemoryAuditLogSink.java`. Tracking issue: [#11](https://github.com/exeris-systems/exeris-caps-audit-trail/issues/11).

## Later

### Persistent sink

The only sink is `InMemoryAuditLogSink`. Events live in one process, are lost on restart, are not shared between processes, and are never evicted, so memory grows with every `emit`. The README states the loss on restart. A durable sink needs a decision on the storage interface it writes through, and that decision belongs to the platform rather than to this capability. Code: `internal/InMemoryAuditLogSink.java`.

### Delivery contract of `emit`

`AuditLogSink.emit` returns `void` and declares no failure. A durable sink therefore cannot tell a caller that an event was not recorded, and the contract does not say whether `emit` takes part in the caller's transaction. A caller that must not proceed without its audit record has no way to ask for that. This waits on the persistent sink, because the failure modes are those of the storage it uses. Code: `api/AuditLogSink.java`.

### Bounded and cheaper queries

`AuditQuery` accepts any positive `limit`, and `InMemoryAuditLogSink.query` filters and sorts every stored event on each call before paging. A caller can request an arbitrarily large page and every query costs time proportional to the number of stored events. A maximum page size and an index by time wait on the persistent sink, which decides what an index looks like. Code: `api/AuditQuery.java`, `internal/InMemoryAuditLogSink.java`.

### Matching rules in queries

`resourceType`, `action` and `actorId` match case-insensitively, while `resourceId` matches exactly, and the contract states neither rule. Whether identifiers are case-sensitive is a design question for the platform to answer once, so that every sink agrees. Code: `internal/InMemoryAuditLogSink.java`.

### Unattributed events

An event with no actor is recorded as actor `system`, so a missing principal and a real system principal cannot be told apart. Whether a missing actor should be refused is a design question for the Identity SKU, which is the main emitter. Code: `api/AuditEvent.java`.

### Lifecycle and readiness

`AuditTrailModule` tracks `ready()` and `drain()` in `isReady()`, but the sink accepts `emit` in every state, including after `drain`. What a draining capability does with new events is not defined. Code: `AuditTrailModule.java`.

### Retention and tamper evidence

There is no retention policy, no deletion path and no integrity chain over stored events. For the in-memory sink that is acceptable. A durable sink needs a decision on retention and on whether records are signed or chained, and that decision should be taken together with its storage.

### A test that pins the declared contract

No test asserts the `@Provides` version or that it matches `src/main/generated/java/cap-manifest.json`. A manifest that drifts from the declaration is caught by the build's `git status` check, not by a test. Code: the test sources under `src/test/java`.

### Javadoc gate

The organisation's Javadoc gate is not called from `.github/workflows/guardrails.yml`. The gate installs the module with a plain `mvn install` and passes no token, while this build needs `exeris-kernel` from GitHub Packages. It waits on the gate in `exeris-systems/.github` resolving such dependencies.
