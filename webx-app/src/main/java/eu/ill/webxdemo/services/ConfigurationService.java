package eu.ill.webxdemo.services;

import eu.ill.webxdemo.Configuration;

public class ConfigurationService {

    private static final ConfigurationService instance = new ConfigurationService();

    private Configuration configuration;

    private ConfigurationService() {
    }

    public static ConfigurationService instance() {
        return instance;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
