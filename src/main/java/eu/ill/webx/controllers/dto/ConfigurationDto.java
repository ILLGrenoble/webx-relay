package eu.ill.webx.controllers.dto;

import eu.ill.webx.Configuration;

public class ConfigurationDto {

    private String standaloneHost;
    private Integer standalonePort;

    public ConfigurationDto(Configuration configuration) {
        this.standaloneHost = configuration.getStandaloneHost();
        this.standalonePort = configuration.getStandalonePort();
    }

    public String getStandaloneHost() {
        return standaloneHost;
    }

    public Integer getStandalonePort() {
        return standalonePort;
    }
}
