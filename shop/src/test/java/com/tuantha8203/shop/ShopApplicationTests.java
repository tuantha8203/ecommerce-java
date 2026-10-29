package com.tuantha8203.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ShopApplicationTests {

    @Autowired
    JdbcClient jdbc;

    @Test
    void contextLoads() {}

    @Test
    void flywayTaoDuSchemaTheoModule() {
        List<String> schemas = jdbc.sql("""
                SELECT schema_name FROM information_schema.schemata
                WHERE schema_name IN ('catalog', 'customer', 'inventory', 'ordering', 'payment')
                """).query(String.class).list();

        assertThat(schemas).containsExactlyInAnyOrder("catalog", "customer", "inventory", "ordering", "payment");
    }
}
