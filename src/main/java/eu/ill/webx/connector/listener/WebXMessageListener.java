package eu.ill.webx.connector.listener;

import eu.ill.webx.connector.message.WebXMessage;

public interface WebXMessageListener {

    void onMessage(WebXMessage message);

}
