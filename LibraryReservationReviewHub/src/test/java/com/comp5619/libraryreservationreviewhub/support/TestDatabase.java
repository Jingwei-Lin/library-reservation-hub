package com.comp5619.libraryreservationreviewhub.support;

import java.nio.file.Path;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Points every Spring Boot test at one throwaway MySQL 8.4, started by Testcontainers when the first application
 * context starts and shared by the rest of the run. Schema.sql is loaded by the image's init scripts, which run it
 * through the mysql client (so DELIMITER works), before the container reports ready. Testcontainers removes the
 * container when the test JVM exits.
 *
 * <p>Registered in {@code META-INF/spring.factories}, so the tests need no annotation; plain unit tests never start
 * it. Needs Docker. Gradle passes the schema's path as {@code testDatabase.schema}.
 */
public class TestDatabase implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /** Started once, on first use: class initialisation is thread-safe and happens only once. */
    private static final class Container {
        static final MySQLContainer<?> MYSQL = start();

        private static MySQLContainer<?> start() {
            Path schema = Path.of(System.getProperty("testDatabase.schema", "Schema.sql")).toAbsolutePath();
            MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
                    .withDatabaseName("library_reservation")
                    .withCopyFileToContainer(MountableFile.forHostPath(schema), "/docker-entrypoint-initdb.d/schema.sql");
            mysql.start();
            return mysql;
        }
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();
        MySQLContainer<?> mysql = Container.MYSQL;
        TestPropertyValues values = TestPropertyValues.of(
                "spring.datasource.url=" + mysql.getJdbcUrl(),
                "spring.datasource.username=" + mysql.getUsername(),
                "spring.datasource.password=" + mysql.getPassword());
        // The DashScope starter refuses an empty key. The live AI test only runs with a real key (PersonalAgentTest).
        String apiKey = environment.getProperty("spring.ai.dashscope.api-key");
        if (apiKey == null || apiKey.isBlank()) {
            values = values.and("spring.ai.dashscope.api-key=not-a-real-key");
        }
        values.applyTo(environment);
    }
}
