package net.milanaleksic.baobab.providers.impl;

import net.milanaleksic.baobab.converters.Converter;
import net.milanaleksic.baobab.converters.typed.TypedConverter;
import net.milanaleksic.baobab.integration.loader.Loader;
import net.milanaleksic.baobab.providers.ConverterProvider;
import net.milanaleksic.baobab.util.Configuration;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class ConfigurableConverterProvider implements ConverterProvider {

    private final Loader loader;

    private final AtomicReference<Map<Class<?>, Converter>> mapping = new AtomicReference<>(null);

    @Inject
    public ConfigurableConverterProvider(Loader loader) {
        this.loader = loader;
    }

    private void bootUpLazilyMapping() {
        mapping.compareAndSet(null,
                Configuration.<Converter>loadClassToInstanceMapping(
                        "net.milanaleksic.baobab.converters",
                        Optional.of(loader)
                )
        );
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Converter converter : mapping.get().values()) {
                converter.cleanUp();
            }
        }));
    }

    @SuppressWarnings("unchecked")
    private Converter doProvideConverterForClass(Class<?> clazz) {
        if (mapping.get() == null)
            bootUpLazilyMapping();
        return mapping.get().get(clazz);
    }

    @Override
    public Converter provideConverterForClass(final Class<?> argType) {
        Converter converter = doProvideConverterForClass(argType);
        if (converter == null)
            return doProvideConverterForClass(Object.class);
        return converter;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> Optional<TypedConverter<T>> provideTypedConverterForClass(final Class<T> type) {
        TypedConverter<T> converter = (TypedConverter<T>) doProvideConverterForClass(type);
        if (converter == null)
            return Optional.empty();
        return Optional.of(converter);
    }

}
