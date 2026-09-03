package io.containerapps.javaruntime.workshop.springboot;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins the wire contract at every seam where a framework mechanism was swapped: the routing
 * prefix, the media types the server actually writes, the JSON shape of the entity, and the
 * error envelope. Each assertion here fails on a plausible wrong wiring that would still
 * compile and still leave the inherited suite green.
 */
@MicronautTest(transactional = false)
@Property(name = "micronaut.server.port", value = "8804")
@Property(name = "datasources.default.url", value = "jdbc:tc:postgresql:14-alpine://testcontainers/postgres")
@Property(name = "datasources.default.driver-class-name", value = "org.testcontainers.jdbc.ContainerDatabaseDriver")
@Property(name = "datasources.default.username", value = "postgres")
@Property(name = "datasources.default.password", value = "password")
class SpringbootSeamTest {

    private static String basePath = "http://localhost:8804/springboot";

    @Inject
    @Client("http://localhost:8804")
    private HttpClient client;

    /**
     * The controller is mounted under /springboot, not at the root: a route registered without
     * the prefix would answer here and every inherited test would still pass.
     */
    @Test
    void testRoutingPrefixIsSpringboot() {
        HttpResponse<String> response = this.client.toBlocking().
            exchange(HttpRequest.GET(basePath), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        assertThat(response.body()).isEqualTo("Spring Boot: hello");

        HttpClientResponseException notFound = assertThrows(HttpClientResponseException.class, () ->
            this.client.toBlocking().exchange(HttpRequest.GET("http://localhost:8804/"), String.class));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatus());
    }

    /**
     * The text endpoints must advertise a charset. The bytes are identical without it, so only
     * the header proves the encoding was declared to the client.
     */
    @Test
    void testTextEndpointsDeclareUtf8Charset() {
        for (String uri : new String[] { basePath, basePath + "/cpu?iterations=1", basePath + "/memory?bites=1" }) {
            HttpResponse<String> response = this.client.toBlocking().
                exchange(HttpRequest.GET(uri), String.class);
            assertEquals(HttpStatus.OK, response.getStatus());
            assertThat(response.getHeaders().get("content-type")).isEqualTo("text/plain;charset=UTF-8");
        }
    }

    /**
     * The statistics endpoint serves JSON without a charset parameter.
     */
    @Test
    void testStatsDeclaresJsonWithoutCharset() {
        HttpResponse<String> response = this.client.toBlocking().
            exchange(HttpRequest.GET(basePath + "/stats"), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        assertThat(response.getHeaders().get("content-type")).isEqualTo("application/json");
    }

    /**
     * Temporal fields serialise as ISO-8601 text and the identifier stays out of the payload.
     * A default serialisation configuration renders both as numbers and leaks the id, which no
     * inherited test observes because they only assert the status code.
     */
    @Test
    void testStatsEntityJsonShape() {
        this.client.toBlocking().
            exchange(HttpRequest.GET(basePath + "/cpu?iterations=1&db=true"), String.class);

        HttpResponse<String> response = this.client.toBlocking().
            exchange(HttpRequest.GET(basePath + "/stats"), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());

        String body = response.body();
        assertThat(body).startsWith("[{").endsWith("}]");
        assertThat(body).doesNotContain("\"id\":");
        assertThat(body).contains("\"framework\":\"SPRINGBOOT\"");
        assertThat(body).contains("\"type\":\"CPU\"");
        assertThat(body).contains("\"description\":null");
        assertThat(body).containsPattern("\"doneAt\":\"\\d{4}-\\d{2}-\\d{2}T[\\d:.]+Z\"");
        assertThat(body).containsPattern("\"duration\":\"PT[\\d.]+S\"");
    }

    /**
     * Unmapped routes answer with the four-field error envelope, not the framework default.
     */
    @Test
    void testErrorEnvelopeShape() {
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
            this.client.toBlocking().exchange(HttpRequest.GET(basePath + "/nope"), String.class));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        HttpResponse<?> response = exception.getResponse();
        assertThat(response.getHeaders().get("content-type")).isEqualTo("application/json");

        String body = response.getBody(String.class).orElse("");
        assertThat(body).containsPattern("^\\{\"timestamp\":\"[^\"]+\",\"status\":404,\"error\":\"Not Found\",\"path\":\"/springboot/nope\"\\}$");
        assertThat(body).doesNotContain("\"message\"");
        assertThat(body).doesNotContain("_links");
    }

    /**
     * A client that does not accept JSON gets an empty error body rather than a rendered envelope.
     */
    @Test
    void testErrorBodyIsEmptyWhenJsonIsNotAcceptable() {
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
            this.client.toBlocking().exchange(HttpRequest.GET(basePath + "/stats").header("Accept", "text/plain"), String.class));

        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getStatus());
        HttpResponse<?> response = exception.getResponse();
        assertThat(response.getHeaders().get("content-type")).isNull();
        assertThat(response.getBody(String.class).orElse("")).isEmpty();
    }

    /**
     * A trailing slash is not an alias for the controller root.
     */
    @Test
    void testTrailingSlashIsNotFound() {
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
            this.client.toBlocking().exchange(HttpRequest.GET(basePath + "/"), String.class));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertThat(exception.getResponse().getBody(String.class).orElse("")).contains("\"path\":\"/springboot/\"");
    }

    /**
     * An unparseable query parameter is rejected rather than silently coerced to a default.
     */
    @Test
    void testUnparseableQueryParametersAreRejected() {
        HttpClientResponseException badBoolean = assertThrows(HttpClientResponseException.class, () ->
            this.client.toBlocking().exchange(HttpRequest.GET(basePath + "/cpu?db=notabool"), String.class));
        assertEquals(HttpStatus.BAD_REQUEST, badBoolean.getStatus());

        HttpClientResponseException badNumber = assertThrows(HttpClientResponseException.class, () ->
            this.client.toBlocking().exchange(HttpRequest.GET(basePath + "/cpu?iterations=abc"), String.class));
        assertEquals(HttpStatus.BAD_REQUEST, badNumber.getStatus());
    }

    /**
     * The optional description stays optional: omitting it must not fail the request, and the
     * upstream misspelling of the parameter name must keep being ignored.
     */
    @Test
    void testDescriptionParameterIsOptional() {
        HttpResponse<String> omitted = this.client.toBlocking().
            exchange(HttpRequest.GET(basePath + "/cpu?iterations=1&db=true"), String.class);
        assertEquals(HttpStatus.OK, omitted.getStatus());

        HttpResponse<String> misspelled = this.client.toBlocking().
            exchange(HttpRequest.GET(basePath + "/cpu?iterations=1&db=true&dec=Java17"), String.class);
        assertEquals(HttpStatus.OK, misspelled.getStatus());
        assertThat(misspelled.body()).doesNotContain("Java17");
    }
}
