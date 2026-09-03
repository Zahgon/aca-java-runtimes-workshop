package io.containerapps.javaruntime.workshop.springboot;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.annotation.ResponseFilter;
import io.micronaut.http.annotation.ServerFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Advertises only the explicitly declared HTTP methods in the {@code Allow} header of a
 * "405 Method Not Allowed" response, as the application did before: the automatically supported
 * {@code HEAD} and {@code OPTIONS} methods were never listed there.
 */
@ServerFilter(Filter.MATCH_ALL_PATTERN)
public class AllowHeaderResponseFilter {

    private static final Set<String> IMPLICIT_METHODS = Set.of("HEAD", "OPTIONS");

    @ResponseFilter
    public void pruneImplicitMethods(MutableHttpResponse<?> response) {
        if (response.status() != HttpStatus.METHOD_NOT_ALLOWED) {
            return;
        }
        String allow = response.getHeaders().get(HttpHeaders.ALLOW);
        if (allow == null) {
            return;
        }
        List<String> declared = new ArrayList<>();
        for (String method : allow.split(",")) {
            String trimmed = method.trim();
            if (!trimmed.isEmpty() && !IMPLICIT_METHODS.contains(trimmed.toUpperCase())) {
                declared.add(trimmed);
            }
        }
        response.getHeaders().remove(HttpHeaders.ALLOW);
        if (!declared.isEmpty()) {
            response.header(HttpHeaders.ALLOW, String.join(",", declared));
        }
    }
}
