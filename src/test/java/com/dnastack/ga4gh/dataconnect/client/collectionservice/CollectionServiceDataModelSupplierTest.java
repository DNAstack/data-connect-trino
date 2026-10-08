package com.dnastack.ga4gh.dataconnect.client.collectionservice;

import com.dnastack.ga4gh.dataconnect.model.DataModel;
import com.dnastack.oauth.client.TokenExchangeException;
import com.dnastack.tenancy.context.TenantId;
import com.dnastack.tenancy.context.TestTenantIds;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CollectionServiceDataModelSupplierTest {

    private static final String CATALOG = "collections";

    private final CollectionServiceClient managementClient = mock(CollectionServiceClient.class);
    private final TenantCollectionServiceClient tenantClient = mock(TenantCollectionServiceClient.class);
    private final CollectionServiceDataModelSupplier supplier =
        new CollectionServiceDataModelSupplier(managementClient, tenantClient, CATALOG);

    @Test
    public void supply_should_lookUpTheDataModelInTheRequestTenant_when_theTenantIsNotManagement() {
        TenantId tenantId = TestTenantIds.of(UUID.randomUUID());
        DataModel tenantDataModel = new DataModel();
        when(tenantClient.getItem(tenantId.asString(), "my_collection", "my_table"))
            .thenReturn(CollectionItem.builder().jsonSchema(tenantDataModel).build());

        DataModel supplied = supplier.supply(tenantId, CATALOG + ".my_collection.my_table");

        assertThat(supplied).isSameAs(tenantDataModel);
        verify(managementClient, never()).getItem(any(), any());
    }

    @Test
    public void supply_should_lookUpTheDataModelOnTheManagementPath_when_theTenantIsManagement() {
        DataModel managementDataModel = new DataModel();
        when(managementClient.getItem("my_collection", "my_table"))
            .thenReturn(CollectionItem.builder().jsonSchema(managementDataModel).build());

        DataModel supplied = supplier.supply(TenantId.MANAGEMENT, CATALOG + ".my_collection.my_table");

        assertThat(supplied).isSameAs(managementDataModel);
        verify(tenantClient, never()).getItem(any(), any(), any());
    }

    @Test
    public void supply_should_returnNull_when_theTableIsNotInTheCollectionsCatalog() {
        TenantId tenantId = TestTenantIds.of(UUID.randomUUID());

        assertThat(supplier.supply(tenantId, "other_catalog.my_collection.my_table")).isNull();
        verify(tenantClient, never()).getItem(any(), any(), any());
        verify(managementClient, never()).getItem(any(), any());
    }

    @Test
    public void supply_should_returnNull_when_noTokenCanBeObtainedForTheTenant() {
        TenantId tenantId = TestTenantIds.of(UUID.randomUUID());
        when(tenantClient.getItem(tenantId.asString(), "my_collection", "my_table"))
            .thenThrow(new TokenExchangeException("wallet refused the token request"));

        assertThat(supplier.supply(tenantId, CATALOG + ".my_collection.my_table")).isNull();
    }
}
