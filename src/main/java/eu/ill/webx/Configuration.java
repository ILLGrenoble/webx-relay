package eu.ill.webx;

public class Configuration {


    private String webXHost;

    private Integer webXPort;

    public Configuration(String webXHost, Integer webXPort) {
        this.webXHost = webXHost;
        this.webXPort = webXPort;
    }

    public String getWebXHost() {
        return webXHost;
    }

    public void setWebXHost(String webXHost) {
        this.webXHost = webXHost;
    }

    public Integer getWebXPort() {
        return webXPort;
    }

    public void setWebXPort(Integer webXPort) {
        this.webXPort = webXPort;
    }
}
