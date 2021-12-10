package eu.ill.webx.transport;

import eu.ill.webx.Configuration;
import eu.ill.webx.model.ConnectionData;
import eu.ill.webx.model.DisconnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;

public class Transport {

    private static final Logger logger = LoggerFactory.getLogger(Transport.class);

    private ZContext context;
    private boolean connected = false;

    private ClientConnector connector;
    private MessageSubscriber messageSubscriber;
    private InstructionPublisher instructionPublisher;
    private SessionChannel sessionChannel;

    public Transport() {
    }

    public ClientConnector getConnector() {
        return connector;
    }

    public MessageSubscriber getMessageSubscriber() {
        return messageSubscriber;
    }

    public InstructionPublisher getInstructionPublisher() {
        return instructionPublisher;
    }

    public SessionChannel getSessionChannel() {
        return sessionChannel;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void connect(Configuration configuration) throws DisconnectedException {

        if (this.context == null) {
            this.connected = false;
            this.context = new ZContext();

            String webXServerHost = configuration.getWebXHost();

            try {
                this.connector = new ClientConnector();
                ConnectionData connectionData = this.connector.connect(this.context, "tcp://" + webXServerHost + ":" + configuration.getWebXPort(), configuration.getSocketTimeoutMs());

                this.messageSubscriber = new MessageSubscriber();
                this.messageSubscriber.start(this.context, "tcp://" + webXServerHost + ":" + connectionData.getPublisherPort());

                this.instructionPublisher = new InstructionPublisher();
                this.instructionPublisher.connect(this.context, "tcp://" + webXServerHost + ":" + connectionData.getCollectorPort());

                this.sessionChannel = new SessionChannel();
                this.sessionChannel.connect(this.context, "tcp://" + webXServerHost + ":" + connectionData.getSessionPort(), configuration.getSocketTimeoutMs(), connectionData.getServerPublicKey());

                this.connected = true;

            } catch (DisconnectedException e) {
                this.disconnect();
                throw e;

            } catch (Exception e) {
                this.disconnect();
                throw new DisconnectedException();
            }

        }
    }

    public void disconnect() {
        if (this.context != null) {

            if (this.connector != null) {
                this.connector.disconnect();
                this.connector = null;
            }

            if (this.messageSubscriber != null) {
                this.messageSubscriber.stop();
                this.messageSubscriber = null;
            }

            if (this.instructionPublisher != null) {
                this.instructionPublisher.disconnect();
                this.instructionPublisher = null;
            }

            if (this.sessionChannel != null) {
                this.sessionChannel.disconnect();
                this.sessionChannel = null;
            }

            this.context.destroy();
            this.context = null;

            this.connected = false;
        }
    }
}