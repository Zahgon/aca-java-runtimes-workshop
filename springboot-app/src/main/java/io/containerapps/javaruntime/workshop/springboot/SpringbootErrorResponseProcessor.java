package io.containerapps.javaruntime.workshop.springboot;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.server.exceptions.response.ErrorContext;
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renders error responses with the same envelope the application exposed before: a JSON object
 * carrying only {@code timestamp}, {@code status}, {@code error} and {@code path}, in that order.
 * Clients asking for HTML get the plain error page the application used to serve, and clients that
 * accept neither JSON nor HTML get an empty body instead, as they did before.
 */
@Singleton
@Replaces(bean = ErrorResponseProcessor.class)
public class SpringbootErrorResponseProcessor implements ErrorResponseProcessor<Object> {

    private static final String TEXT_HTML_UTF8 = MediaType.TEXT_HTML + ";charset=UTF-8";

    @Override
    public MutableHttpResponse<Object> processResponse(ErrorContext errorContext,
                                                       MutableHttpResponse<?> baseResponse) {
        HttpRequest<?> request = errorContext.getRequest();
        HttpStatus status = baseResponse.status();
        if (acceptsHtml(request)) {
            return cast(baseResponse.body(errorPage(status)).contentType(TEXT_HTML_UTF8));
        }
        if (!acceptsJson(request)) {
            return cast(baseResponse.body(null));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", new Date());
        body.put("status", status.getCode());
        body.put("error", status.getReason());
        body.put("path", request.getPath());
        return cast(baseResponse.body(body).contentType(MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     * The error page the application served to browsers: a single line of HTML naming the status
     * that was reached and the moment the page was produced.
     */
    private static String errorPage(HttpStatus status) {
        return "<html><body><h1>Whitelabel Error Page</h1>"
                + "<p>This application has no explicit mapping for /error, so you are seeing this as a fallback.</p>"
                + "<div id='created'>" + new Date() + "</div>"
                + "<div>There was an unexpected error (type=" + status.getReason()
                + ", status=" + status.getCode() + ").</div>"
                + "</body></html>";
    }

    private static boolean acceptsHtml(HttpRequest<?> request) {
        for (MediaType mediaType : request.accept()) {
            if (MediaType.TEXT_HTML.equals(mediaType.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean acceptsJson(HttpRequest<?> request) {
        Collection<MediaType> accepted = request.accept();
        if (accepted.isEmpty()) {
            return true;
        }
        for (MediaType mediaType : accepted) {
            String name = mediaType.getName();
            if (MediaType.ALL.equals(name) || "application/*".equals(name)
                    || MediaType.APPLICATION_JSON.equals(name)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static MutableHttpResponse<Object> cast(MutableHttpResponse<?> response) {
        return (MutableHttpResponse<Object>) response;
    }
}
