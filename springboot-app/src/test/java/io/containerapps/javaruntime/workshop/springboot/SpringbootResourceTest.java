// tag::adocHeader[]
package io.containerapps.javaruntime.workshop.springboot;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(transactional = false)
@Property(name = "micronaut.server.port", value = "8803")
@Property(name = "datasources.default.url", value = "jdbc:tc:postgresql:14-alpine://testcontainers/postgres")
@Property(name = "datasources.default.driver-class-name", value = "org.testcontainers.jdbc.ContainerDatabaseDriver")
@Property(name = "datasources.default.username", value = "postgres")
@Property(name = "datasources.default.password", value = "password")
class SpringbootResourceTest {

    private static String basePath = "http://localhost:8803/springboot";

    @Inject
    @Client("http://localhost:8803")
    private HttpClient restTemplate;
// end::adocHeader[]

// tag::adocTestHello[]
    @Test
    public void testHelloEndpoint() {
        HttpResponse<String> response = this.restTemplate.toBlocking().
            exchange(HttpRequest.GET(basePath), String.class);

        assertEquals(response.getStatus(), HttpStatus.OK);
        assertThat(response.body()).contains("Spring Boot: hello");
    }
// end::adocTestHello[]

    @Test
    public void testCpuEndpoint() {
        HttpResponse<String> response = this.restTemplate.toBlocking().
            exchange(HttpRequest.GET(basePath + "/cpu?iterations=1"), String.class);

        assertEquals(response.getStatus(), HttpStatus.OK);
        assertThat(response.body())
            .startsWith("Spring Boot: CPU consumption is done with")
            .endsWith("nano-seconds.");
    }

    @Test
    public void testCpuWithDBEndpoint() {
        HttpResponse<String> response = this.restTemplate.toBlocking().
            exchange(HttpRequest.GET(basePath + "/cpu?iterations=1&db=true"), String.class);

        assertEquals(response.getStatus(), HttpStatus.OK);
        assertThat(response.body())
            .startsWith("Spring Boot: CPU consumption is done with")
            .endsWith("The result is persisted in the database.");
    }

// tag::adocTestCPU[]
    @Test
    public void testCpuWithDBAndDescEndpoint() {
        HttpResponse<String> response = this.restTemplate.toBlocking().
            exchange(HttpRequest.GET(basePath + "/cpu?iterations=1&db=true&dec=Java17"), String.class);

        assertEquals(response.getStatus(), HttpStatus.OK);
        assertThat(response.body())
            .startsWith("Spring Boot: CPU consumption is done with")
            .doesNotContain("Java17")
            .endsWith("The result is persisted in the database.");
    }
// end::adocTestCPU[]

    @Test
    public void testMemoryEndpoint() {
        HttpResponse<String> response = this.restTemplate.toBlocking().
            exchange(HttpRequest.GET(basePath + "/memory?bites=1"), String.class);

        assertEquals(response.getStatus(), HttpStatus.OK);
        assertThat(response.body())
            .startsWith("Spring Boot: Memory consumption is done with")
            .endsWith("nano-seconds.");
    }

    @Test
    public void testMemoryWithDBEndpoint() {
        HttpResponse<String> response = this.restTemplate.toBlocking().
            exchange(HttpRequest.GET(basePath + "/memory?bites=1&db=true"), String.class);

        assertEquals(response.getStatus(), HttpStatus.OK);
        assertThat(response.body())
            .startsWith("Spring Boot: Memory consumption is done with")
            .endsWith("The result is persisted in the database.");
    }

// tag::adocTestMemory[]
    @Test
    public void testMemoryWithDBAndDescEndpoint() {
        HttpResponse<String> response = this.restTemplate.toBlocking().
            exchange(HttpRequest.GET(basePath + "/memory?bites=1&db=true&desc=Java17"), String.class);

        assertEquals(response.getStatus(), HttpStatus.OK);
        assertThat(response.body())
            .startsWith("Spring Boot: Memory consumption is done with")
            .doesNotContain("Java17")
            .endsWith("The result is persisted in the database.");
    }
// end::adocTestMemory[]

// tag::adocTestStats[]
    @Test
    public void testStats() {
        HttpResponse<String> response = this.restTemplate.toBlocking().
            exchange(HttpRequest.GET(basePath + "/stats"), String.class);

        assertEquals(response.getStatus(), HttpStatus.OK);
    }
// end::adocTestStats[]
}
