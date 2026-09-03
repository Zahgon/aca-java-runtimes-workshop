package io.containerapps.javaruntime.workshop.springboot;

import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.MutableConversionService;
import io.micronaut.core.convert.TypeConverterRegistrar;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;

/**
 * Reproduces the way a repeated query parameter was read before: when the same typed parameter is
 * supplied more than once, the first value wins instead of the request being rejected. Only the
 * scalar types the endpoints declare are registered, so repeated text parameters keep their
 * existing behaviour.
 */
@Singleton
public class RepeatedQueryValueConverters implements TypeConverterRegistrar {

    @Override
    public void register(MutableConversionService conversionService) {
        registerFirstValue(conversionService, Long.class);
        registerFirstValue(conversionService, Integer.class);
        registerFirstValue(conversionService, Boolean.class);
    }

    private static <T> void registerFirstValue(MutableConversionService conversionService, Class<T> targetType) {
        conversionService.addConverter(List.class, targetType,
                (object, type, context) -> convertFirst(conversionService, object, type, context));
    }

    private static <T> Optional<T> convertFirst(ConversionService conversionService, List<?> values,
                                                Class<T> targetType, ConversionContext context) {
        if (values.isEmpty()) {
            return Optional.empty();
        }
        Object first = values.get(0);
        if (first == null) {
            return Optional.empty();
        }
        if (targetType.isInstance(first)) {
            return Optional.of(targetType.cast(first));
        }
        return conversionService.convert(first, targetType, context);
    }
}
