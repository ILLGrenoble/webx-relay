package eu.ill.webx.connector.response;

import eu.ill.webx.domain.display.WindowProperties;

import java.util.ArrayList;
import java.util.List;

public class WebXWindowsResponse extends WebXResponse {

    private List<WindowProperties> windows = new ArrayList<>();

    public WebXWindowsResponse() {
    }

    public List<WindowProperties> getWindows() {
        return windows;
    }

    public void setWindows(List<WindowProperties> windows) {
        this.windows = windows;
    }
}
