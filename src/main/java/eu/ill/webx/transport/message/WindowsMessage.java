package eu.ill.webx.transport.message;

import eu.ill.webx.domain.WindowProperties;

import java.util.ArrayList;
import java.util.List;

public class WindowsMessage extends Message {

    private List<WindowProperties> windows = new ArrayList<>();

    public WindowsMessage() {
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
