package io.containerapps.javaruntime.workshop.springboot;

import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;
import jakarta.inject.Singleton;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Converts text to a boolean the way the application did before: only the well-known true and false
 * spellings are accepted, and anything else is rejected as a conversion error rather than silently
 * read as false.
 */
@Singleton
public class StringToBooleanConverter implements TypeConverter<CharSequence, Boolean> {

    private static final Set<String> TRUE_VALUES = Set.of("true", "on", "yes", "1");
    private static final Set<String> FALSE_VALUES = Set.of("false", "off", "no", "0");

    @Override
    public Optional<Boolean> convert(CharSequence object, Class<Boolean> targetType, ConversionContext context) {
        String value = object.toString().trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (TRUE_VALUES.contains(normalized)) {
            return Optional.of(Boolean.TRUE);
        }
        if (FALSE_VALUES.contains(normalized)) {
            return Optional.of(Boolean.FALSE);
        }
        context.reject(object, new IllegalArgumentException("Invalid boolean value [" + value + "]"));
        return Optional.empty();
    }
}
