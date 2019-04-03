package eu.ill.webx.connector.message;

import eu.ill.webx.connector.response.WebXResponse;
import eu.ill.webx.domain.display.WindowProperties;

import java.util.ArrayList;
import java.util.List;

public class WebXWindowsMessage extends WebXResponse {

    private List<WindowProperties> windows = new ArrayList<>();

    public WebXWindowsMessage() {
    }

    public List<WindowProperties> getWindows() {
        return windows;
    }

    public void setWindows(List<WindowProperties> windows) {
        this.windows = windows;
    }
}
