package eu.ill.webx.connector;

public interface WebXMessageListener {

    void onMessage(byte[] messageData);

}
