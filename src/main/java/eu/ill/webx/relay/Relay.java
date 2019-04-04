package eu.ill.webx.relay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ill.webx.connector.WebXConnector;
import eu.ill.webx.connector.listener.WebXMessageListener;
import eu.ill.webx.connector.message.WebXMessage;
import eu.ill.webx.relay.command.ClientCommand;
import eu.ill.webx.relay.response.RelayConnectionResponse;
import eu.ill.webx.relay.response.RelayResponse;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;

public class Relay implements WebXMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(Relay.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    private Thread webXListenerThread;
    private Thread clientCommandThread;
    private LinkedBlockingDeque<WebXMessage> webXMessageQueue = new LinkedBlockingDeque<>();
    private LinkedBlockingDeque<ClientCommand> clientCommandQueue = new LinkedBlockingDeque<>();
    private boolean running = false;

    private Session session;
    private RemoteEndpoint remoteEndpoint;

    public Relay(Session session) {
        this.session = session;
        this.remoteEndpoint = session.getRemote();
    }

    public Thread getWebXListenerThread() {
        return webXListenerThread;
    }

    public boolean isRunning() {
        return running;
    }

    public synchronized void start() {
        if (!running) {
            running = true;

            this.webXListenerThread = new Thread(() -> this.webXListenerLoop());
            this.webXListenerThread.start();

            this.clientCommandThread = new Thread(() -> this.clientCommandLoop());
            this.clientCommandThread.start();
        }
    }

    public synchronized void stop() {
        if (running) {
            try {
                running = false;
                if (this.webXListenerThread != null) {
                    this.webXListenerThread.interrupt();
                    this.webXListenerThread.join();
                    this.webXListenerThread = null;
                }

                if (this.clientCommandThread != null) {
                    this.clientCommandThread.interrupt();
                    this.clientCommandThread.join();
                    this.clientCommandThread = null;
                }

            } catch (InterruptedException e) {
                logger.error("Stop of relay message listener and client command threads interrupted");
            }
        }
    }

    @Override
    public void onMessage(WebXMessage message) {
        try {
            this.webXMessageQueue.put(message);

        } catch (InterruptedException e) {
            logger.error("Interrupted when adding message to relay message queue");
        }
    }

    public void queueCommand(ClientCommand command) {
        try {
            this.clientCommandQueue.put(command);

        } catch (InterruptedException e) {
            logger.error("Interrupted when adding command to relay command queue");
        }
    }

    private void webXListenerLoop() {
        while (this.running) {
            try {
                WebXMessage message = this.webXMessageQueue.take();

                // Send message to client through web socket
                logger.info(message.toString());

            } catch (InterruptedException ie) {
                logger.info("Relay message listener thread interrupted");
            }
        }
    }

    private void clientCommandLoop() {
        while (this.running) {
            try {
                ClientCommand command = this.clientCommandQueue.take();
                RelayResponse response = this.handleClientCommand(command);

                String responseData = this.objectMapper.writeValueAsString(response);
                this.sendDataToRemote(responseData);

            } catch (InterruptedException ie) {
                logger.info("Relay message listener thread interrupted");

            } catch (JsonProcessingException e) {
                logger.error("Failed to convert object to JSON");
            }
        }
    }

    private RelayResponse handleClientCommand(ClientCommand command) {

        // Handle command
        logger.info(command.toString());

        RelayResponse response = null;
        if (command.getType().equals(ClientCommand.Type.Connect)) {
            response = new RelayConnectionResponse(WebXConnector.instance().getScreenSize());
        }

        return response;
    }

    private synchronized void sendDataToRemote(String data) {
        try {
            this.remoteEndpoint.sendString(data);

        } catch (IOException e) {
            logger.error("Failed to write data to web socket");
        }
    }
}
