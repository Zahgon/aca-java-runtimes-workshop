package io.containerapps.javaruntime.workshop.springboot;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.exceptions.HttpStatusException;

/**
 * Rejects request paths that end with a trailing slash, so that a URI such as
 * <code>/springboot/</code> is not matched to the <code>/springboot</code> route.
 */
@ServerFilter(Filter.MATCH_ALL_PATTERN)
public class TrailingSlashFilter {

    @RequestFilter
    public void rejectTrailingSlash(HttpRequest<?> request) {
        String path = request.getPath();
        if (path.length() > 1 && path.endsWith("/")) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Not Found");
        }
    }
}
