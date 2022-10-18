package io.quarkus.ts.sqldb.panacheflyway;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;

@ApplicationScoped
@Path("/data-source")
public class DataSourceResource {
    @Inject
    AgroalDataSource defaultDataSource;

    @Inject
    @DataSource("with-xa")
    AgroalDataSource xaDataSource;

    @GET
    @Path("/default/connection-provider-class")
    public String defaultConnectionProviderClass() {
        return getConnectionProviderClass(defaultDataSource);
    }

    @GET
    @Path("/with-xa/connection-provider-class")
    public String withXaConnectionProviderClass() {
        return getConnectionProviderClass(xaDataSource);
    }

    private String getConnectionProviderClass(AgroalDataSource dataSource) {
        return dataSource.getConfiguration().connectionPoolConfiguration()
                .connectionFactoryConfiguration().connectionProviderClass().getName();
    }
}
