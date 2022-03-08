package io.quarkus.qe.hibernate.search;

import io.quarkus.test.bootstrap.DefaultService;
import io.quarkus.test.bootstrap.PostgresqlService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class PostgresqlMultitenantHibernateNoPanacheIT extends AbstractMultitenantNoPanacheIT {

    static final int ELASTIC_PORT = 9200;
    static final int POSTGRESQL_PORT = 5432;

    @Container(image = "docker.elastic.co/elasticsearch/elasticsearch-oss:7.10.0", port = ELASTIC_PORT, expectedLog = "started")
    static DefaultService elastic = new DefaultService().withProperty("discovery.type", "single-node");

    @Container(image = "${postgresql.13.image}", port = POSTGRESQL_PORT, expectedLog = "listening on IPv4 address")
    static PostgresqlService database = new PostgresqlService();

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperties("postgresql.properties")
            .withProperty("quarkus.datasource.username", database.getUser())
            .withProperty("quarkus.datasource.password", database.getPassword())
            .withProperty("quarkus.datasource.jdbc.url", database::getJdbcUrl)
            .withProperty("quarkus.hibernate-search-orm.elasticsearch.hosts",
                    () -> getElasticSearchConnectionChain(elastic.getHost(), elastic.getPort()));

    @Override
    protected RestService getApp() {
        return app;
    }
}
