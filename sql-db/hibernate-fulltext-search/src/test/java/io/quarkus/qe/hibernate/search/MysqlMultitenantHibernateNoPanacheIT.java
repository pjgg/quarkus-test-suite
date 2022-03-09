package io.quarkus.qe.hibernate.search;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.bootstrap.DefaultService;
import io.quarkus.test.bootstrap.MySqlService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
@EnabledIfSystemProperty(named = "ts.redhat.registry.enabled", matches = "true")
public class MysqlMultitenantHibernateNoPanacheIT extends AbstractMultitenantNoPanacheIT {

    static final int ELASTIC_PORT = 9200;
    static final int MYSQL_PORT = 3306;

    @Container(image = "${mysql.80.image}", port = MYSQL_PORT, expectedLog = "Only MySQL server logs after this point")
    static MySqlService base = new MySqlService().withDatabase("base");

    @Container(image = "${mysql.80.image}", port = MYSQL_PORT, expectedLog = "Only MySQL server logs after this point")
    static MySqlService company1 = new MySqlService().withDatabase("company1");

    @Container(image = "${mysql.80.image}", port = MYSQL_PORT, expectedLog = "Only MySQL server logs after this point")
    static MySqlService company2 = new MySqlService().withDatabase("company2");;

    @Container(image = "docker.elastic.co/elasticsearch/elasticsearch-oss:7.10.0", port = ELASTIC_PORT, expectedLog = "started")
    static DefaultService elastic = new DefaultService().withProperty("discovery.type", "single-node");

    @QuarkusApplication
    static RestService app = new RestService()
            .withProperties("mysql.properties")
            .withProperty("quarkus.datasource.username", base.getUser())
            .withProperty("quarkus.datasource.password", base.getPassword())
            .withProperty("quarkus.datasource.jdbc.url", base::getJdbcUrl)
            .withProperty("quarkus.datasource.base.username", base.getUser())
            .withProperty("quarkus.datasource.base.password", base.getPassword())
            .withProperty("quarkus.datasource.base.jdbc.url", base::getJdbcUrl)
            .withProperty("quarkus.datasource.company1.username", company1.getUser())
            .withProperty("quarkus.datasource.company1.password", company1.getPassword())
            .withProperty("quarkus.datasource.company1.jdbc.url", company1::getJdbcUrl)
            .withProperty("quarkus.datasource.company2.username", company2.getUser())
            .withProperty("quarkus.datasource.company2.password", company2.getPassword())
            .withProperty("quarkus.datasource.company2.jdbc.url", company2::getJdbcUrl)
            .withProperty("quarkus.hibernate-search-orm.elasticsearch.hosts",
                    () -> getElasticSearchConnectionChain(elastic.getHost(), elastic.getPort()));

    @Override
    protected RestService getApp() {
        return app;
    }
}
