package eu.ill.webx.relay.response;

import eu.ill.webx.domain.display.WindowProperties;

import java.util.ArrayList;
import java.util.List;

public class RelayWindowsResponse extends RelayResponse {

    private List<WindowProperties> windows = new ArrayList<>();

    public RelayWindowsResponse() {
    }

    public RelayWindowsResponse(List<WindowProperties> windows) {
        this.windows = windows;
    }

    public List<WindowProperties> getWindows() {
        return windows;
    }

    public void setWindows(List<WindowProperties> windows) {
        this.windows = windows;
    }
}
