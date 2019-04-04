package eu.ill.webx.relay.response;

import eu.ill.webx.domain.utils.Size;

public class RelayConnectionResponse extends RelayResponse {

    private Size screenSize;

    public RelayConnectionResponse(Size screenSize) {
        this.screenSize = screenSize;
    }

    public Size getScreenSize() {
        return screenSize;
    }
}
