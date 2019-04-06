package eu.ill.webx.connector.message;

import eu.ill.webx.domain.display.WindowProperties;

import java.util.ArrayList;
import java.util.List;

public class WebXWindowsMessage extends WebXMessage {

    private List<WindowProperties> windows = new ArrayList<>();

    public WebXWindowsMessage() {
        super("Windows");
    }

    public List<WindowProperties> getWindows() {
        return windows;
    }

    public void setWindows(List<WindowProperties> windows) {
        this.windows = windows;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebXWindowsMessage{");
        sb.append("windows=").append(windows);
        sb.append('}');
        return sb.toString();
    }
}
