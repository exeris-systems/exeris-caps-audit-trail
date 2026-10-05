package eu.exeris.caps.audittrail;

import eu.exeris.caps.audittrail.api.AuditLogSink;
import eu.exeris.caps.audittrail.internal.InMemoryAuditLogSink;
import eu.exeris.sdk.annotation.capability.CapabilityLifecycle;
import eu.exeris.sdk.annotation.capability.CapabilityModule;
import eu.exeris.sdk.annotation.capability.Provides;
import eu.exeris.sdk.composition.lifecycle.CapabilityLifecycleHooks;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Capability declaration and lifecycle owner for {@code exeris-caps-audit-trail}.
 *
 * <p>Provides platform audit logging and query sinks for CMS and Service Boundary SKUs.
 *
 * @since 0.1.0
 */
@CapabilityModule
@CapabilityLifecycle
@Provides(service = AuditLogSink.class, version = "1.0.0")
public class AuditTrailModule implements CapabilityLifecycleHooks {

    private final AuditLogSink auditLogSink;
    private final AtomicBoolean ready = new AtomicBoolean(false);

    public AuditTrailModule() {
        this(new InMemoryAuditLogSink());
    }

    public AuditTrailModule(AuditLogSink auditLogSink) {
        this.auditLogSink = Objects.requireNonNull(auditLogSink, "auditLogSink");
    }

    public AuditLogSink auditLogSink() {
        return auditLogSink;
    }

    @Override
    public void initialize() throws Exception {
    }

    @Override
    public void ready() throws Exception {
        ready.set(true);
    }

    @Override
    public void drain(Duration remaining) throws Exception {
        ready.set(false);
    }

    @Override
    public void terminate() {
        ready.set(false);
    }

    public boolean isReady() {
        return ready.get();
    }
}
