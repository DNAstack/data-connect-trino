package com.dnastack.ga4gh.dataconnect;

import com.dnastack.tenancy.archunit.TenancyCheck;
import com.dnastack.tenancy.archunit.TenancyRules;
import com.dnastack.tenancy.context.TenantId;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.Test;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

public class TenancyArchitectureTest {

    /**
     * The tenant-resolving layers are as narrow as they will go: {@code ..tenancy..} owns the mirror and the
     * lifecycle feed, {@code ..adapter.security..} compares the caller's tenant to the request's, and
     * {@code ..adapter.trino..} stamps new query jobs, scopes their reads and relays the tenant to Trino.
     */
    private static final TenancyCheck THIS_SERVICE = TenancyRules.forService("com.dnastack.ga4gh.dataconnect")
        .tenantResolvingLayers("..adapter.trino..", "..adapter.security..", "..tenancy..");

    @Test
    public void application_should_satisfyTheTenancyRules() {
        THIS_SERVICE.check();
    }

    @Test
    public void tenantTakingMethods_should_takeTheTenantFirst() {
        tenantTakingMethodsTakeTheTenantFirst().check(THIS_SERVICE.classes());
    }

    /**
     * A house convention rather than a library rule: where a method is scoped to a tenant, the tenant reads as
     * the scope the rest of the arguments are interpreted in, so it comes first — and a caller that has them in
     * the wrong order is then a compile error rather than a silent swap of two same-typed arguments.
     */
    private static ArchRule tenantTakingMethodsTakeTheTenantFirst() {
        return methods()
            .that(takeATenantId())
            .should(takeItFirst())
            .as("every method that takes a TenantId must take it as its first parameter")
            .because("the tenant is the scope the other arguments are read in, and a consistent position is one "
                + "less thing to get wrong at each call site")
            .allowEmptyShould(true);
    }

    private static DescribedPredicate<JavaMethod> takeATenantId() {
        return DescribedPredicate.describe("take a TenantId parameter",
            method -> method.getRawParameterTypes().stream().anyMatch(TenancyArchitectureTest::isTenantId));
    }

    private static ArchCondition<JavaMethod> takeItFirst() {
        return new ArchCondition<>("take the TenantId first") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                List<JavaClass> parameters = method.getRawParameterTypes();
                if (!isTenantId(parameters.getFirst())) {
                    events.add(SimpleConditionEvent.violated(method, method.getFullName()
                        + " takes a TenantId, but not as its first parameter"));
                }
            }
        };
    }

    private static boolean isTenantId(JavaClass type) {
        return type.isEquivalentTo(TenantId.class);
    }
}
