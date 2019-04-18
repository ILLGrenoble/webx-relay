package eu.ill.webx.relay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ill.webx.connector.WebXConnector;
import eu.ill.webx.connector.listener.WebXMessageListener;
import eu.ill.webx.transport.instruction.Instruction;
import eu.ill.webx.transport.message.ConnectionMessage;
import eu.ill.webx.transport.message.Message;
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
    private LinkedBlockingDeque<Message> messageQueue = new LinkedBlockingDeque<>();
    private LinkedBlockingDeque<Instruction> instructionQueue = new LinkedBlockingDeque<>();
    private boolean running = false;

    private Session session;
    private RemoteEndpoint remoteEndpoint;

    public Relay(Session session) {
        if (session != null) {
            this.session = session;
            this.remoteEndpoint = session.getRemote();
        }
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
    public void onMessage(Message message) {
        try {
            this.messageQueue.put(message);

        } catch (InterruptedException e) {
            logger.error("Interrupted when adding message to relay message queue");
        }
    }

    public void queueCommand(Instruction command) {
        try {
            this.instructionQueue.put(command);

        } catch (InterruptedException e) {
            logger.error("Interrupted when adding command to relay command queue");
        }
    }

    private void webXListenerLoop() {
        while (this.running) {
            try {
                Message message = this.messageQueue.take();

                // Send message to client through web socket
//                logger.debug(message.toString());

                String responseData = this.objectMapper.writeValueAsString(message);
                this.sendDataToRemote(responseData);

            } catch (InterruptedException ie) {
                logger.info("Relay message listener thread interrupted");

            } catch (JsonProcessingException e) {
                logger.error("Failed to convert object to JSON");
            }
        }
    }

    private void clientCommandLoop() {
        while (this.running) {
            try {
                Instruction command = this.instructionQueue.take();
                Message response = this.handleClientCommand(command);

                String responseData = this.objectMapper.writeValueAsString(response);
                this.sendDataToRemote(responseData);

            } catch (InterruptedException ie) {
                logger.info("Relay message listener thread interrupted");

            } catch (JsonProcessingException e) {
                logger.error("Failed to convert object to JSON");
            }
        }
    }

    private Message handleClientCommand(Instruction command) {

        // Handle command
        Message response = null;
        if (command.getType().equals(Instruction.Type.Connect)) {
            response = new ConnectionMessage(command.getId(), WebXConnector.instance().getScreenSize());

        } else {
            response = WebXConnector.instance().sendRequest(command);
        }

        return response;
    }

    private synchronized void sendDataToRemote(String data) {
        try {
            if (this.remoteEndpoint != null) {
                this.remoteEndpoint.sendString(data);
            }

        } catch (IOException e) {
            logger.error("Failed to write data to web socket");
        }
    }
}
