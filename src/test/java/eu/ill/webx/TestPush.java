package eu.ill.webx;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class TestPush {

    public static void main(String args[]) {

        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.PUSH);
        String fullAddress = "tcp://localhost:5555";
        System.out.println("Connecting to " + fullAddress + "...");
        socket.connect(fullAddress);
        System.out.println("... connected");

        String[] messages = {
            "{\"type\":5,\"id\":257,\"x\":821.691368788143,\"y\":203.50043591979076,\"buttonMask\":0}",
            "{\"type\":5,\"id\":258,\"x\":821.3,\"y\":203.5006,\"buttonMask\":0}",
            "{\"type\":5,\"id\":259,\"x\":821.0,\"y\":203.0,\"buttonMask\":0}",
            "{\"type\":5,\"id\":260,\"x\":821.12,\"y\":203.50,\"buttonMask\":0}",
        };


        for (int i = 0; i < 10000; i++) {
            String message = messages[(int)Math.floor(Math.random() * 4)];
            byte[] messageData = message.getBytes();
            System.out.println("Sending " + message);
            socket.send(messageData, 0);
        }

        System.out.println("Closing socket...");
        socket.close();
        context.close();
        System.out.println("... closed");
    }
}
