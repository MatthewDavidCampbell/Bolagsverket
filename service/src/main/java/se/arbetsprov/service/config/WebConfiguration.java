package se.arbetsprov.service.config;

import java.util.Locale;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets enum query parameters ignore letter case, so {@code sort=population&direction=desc} works
 * as well as {@code sort=POPULATION&direction=DESC}.
 */
@Configuration
class WebConfiguration implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new CaseInsensitiveEnumConverterFactory());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class CaseInsensitiveEnumConverterFactory implements ConverterFactory<String, Enum> {

        @Override
        public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
            return source -> source.isBlank()
                    ? null
                    : (T) Enum.valueOf(targetType, source.strip().toUpperCase(Locale.ROOT));
        }
    }
}
