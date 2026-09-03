package io.containerapps.javaruntime.workshop.springboot;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

@MicronautTest(transactional = false)
@Property(name = "datasources.default.url", value = "jdbc:tc:postgresql:14-alpine://testcontainers/postgres")
@Property(name = "datasources.default.driver-class-name", value = "org.testcontainers.jdbc.ContainerDatabaseDriver")
@Property(name = "datasources.default.username", value = "postgres")
@Property(name = "datasources.default.password", value = "password")
class SpringbootApplicationTests {

	@Test
	void contextLoads() {
	}

}
