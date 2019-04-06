package eu.ill.webx.connector.response;

import eu.ill.webx.domain.display.WindowProperties;

import java.util.ArrayList;
import java.util.List;

public class WebXWindowsResponse extends WebXResponse {

    private List<WindowProperties> windows = new ArrayList<>();

    public WebXWindowsResponse() {
    }

    public WebXWindowsResponse(List<WindowProperties> windows) {
        this.windows = windows;
    }

    public List<WindowProperties> getWindows() {
        return windows;
    }

    public void setWindows(List<WindowProperties> windows) {
        this.windows = windows;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebXWindowsResponse{");
        sb.append("windows=").append(windows);
        sb.append('}');
        return sb.toString();
    }
}
