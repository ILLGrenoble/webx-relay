package eu.ill.webx;

import eu.ill.webx.connector.WebXConnector;
import eu.ill.webx.relay.Relay;

public class TestConnector {

    public static void main(String args[]) {
        WebXConnector connector = new WebXConnector();
        connector.connect("dssdev2.ill.fr", 5555);

        Relay relay = new Relay(null, connector);
        relay.start();

//        connector.getMessageSubscriber().addListener(messageData -> {
//            System.out.println("Received message of length " + messageData.length);
//        });


        String[] messages = {
                "{\"type\":5,\"id\":257,\"x\":821.691368788143,\"y\":203.50043591979076,\"buttonMask\":0}",
                "{\"type\":5,\"id\":258,\"x\":821.3,\"y\":203.5006,\"buttonMask\":0}",
                "{\"type\":5,\"id\":259,\"x\":821.0,\"y\":203.0,\"buttonMask\":0}",
                "{\"type\":5,\"id\":260,\"x\":821.12,\"y\":203.50,\"buttonMask\":0}",
        };
        for (int i = 0; i < 1000; i++) {
            String message = messages[(int)Math.floor(Math.random() * 4)];
            System.out.println("Sending " + message);
            relay.queueCommand(message.getBytes());
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        relay.stop();

        connector.disconnect();
    }
}
