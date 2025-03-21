package eu.ill.webx;


import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.SessionId;
import eu.ill.webx.model.SocketResponse;
import eu.ill.webx.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebXSessionValidator extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(WebXSessionValidator.class);
    private static final int PING_DELAY_MS = 15000;

    public interface OnErrorHandler { void onError(String error); }

    private final SessionId sessionId;
    private final Transport transport;
    private final OnErrorHandler onErrorHandler;

    private boolean running = false;

    public WebXSessionValidator(final SessionId sessionId, final Transport transport, final OnErrorHandler onErrorHandler) {
        this.sessionId = sessionId;
        this.transport = transport;
        this.onErrorHandler = onErrorHandler != null ? onErrorHandler : error -> {};
    }

    @Override
    public void start() {
        running = true;
        super.start();
    }

    @Override
    public void interrupt() {
        running = false;
        super.interrupt();
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                Thread.sleep(PING_DELAY_MS);

                if (this.running) {
                    try {
                        logger.trace("Sending ping to session {}", this.sessionId.hexString());
                        SocketResponse response = this.transport.sendRequest("ping," + this.sessionId.hexString());

                        String[] responseElements = response.toString().split(",");

                        if (responseElements[0].equals("pang")) {
                            this.onErrorHandler.onError(String.format("Failed to ping webX Session %s: %s", this.sessionId.hexString(), responseElements[2]));
                        }

                    } catch (WebXDisconnectedException e) {
                        this.onErrorHandler.onError(String.format("Failed to get response from connector ping to session %s", this.sessionId.hexString()));
                    }
                }

            } catch (InterruptedException ignored) {
            }
        }
    }
}
