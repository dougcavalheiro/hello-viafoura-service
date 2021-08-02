package com.viafoura.helloviafoura.config;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Properties;

public final class ConfigModule extends AbstractModule {

    private final static Config config = ConfigFactory.load();
    private final Properties properties = new Properties();

    public ConfigModule() {
        config.entrySet().forEach((entry) -> properties.put(entry.getKey(), entry.getValue().unwrapped().toString()));
    }

    @Override
    protected void configure() {
        super.configure();
        Names.bindProperties(binder(), properties);
        bind(Config.class).toInstance(ConfigModule.config);
    }
}
