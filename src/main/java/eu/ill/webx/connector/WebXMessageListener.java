package eu.ill.webx.connector;

import eu.ill.webx.transport.message.Message;

public interface WebXMessageListener {

    void onMessage(Message message);

}
