package eu.exeris.caps.audittrail;

import eu.exeris.kernel.spi.contract.EntitlementRequirement;
import eu.exeris.kernel.tck.contract.AbstractEntitlementRequirementTck;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("exeris-caps-audit-trail: EntitlementRequirement")
class AuditTrailEntitlementTest extends AbstractEntitlementRequirementTck {

    @Override
    protected Class<? extends EntitlementRequirement> providerClass() {
        return AuditTrailEntitlement.class;
    }

    @Test
    @DisplayName("ServiceLoader discovers the provider from the cap's own service registration")
    void serviceLoaderDiscoversTheProvider() {
        List<Class<? extends EntitlementRequirement>> types = ServiceLoader.load(EntitlementRequirement.class)
                .stream()
                .map(ServiceLoader.Provider::type)
                .toList();

        assertThat(types).contains(AuditTrailEntitlement.class);
    }

    @Test
    @DisplayName("It requires exactly this cap's own capability identifier")
    void requiresOnlyItsOwnCapability() {
        assertThat(new AuditTrailEntitlement().requiredCapabilities()).containsExactly("exeris-caps-audit-trail");
        assertThat(AuditTrailEntitlement.CAPABILITY_ID).isEqualTo("exeris-caps-audit-trail");
    }
}
