package RDT_Protocol;

import java.net.*;

public class Server {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(2000);
        int clientID = 1;
        while (true) {
            byte receivedFileName[] = new byte[1000];
            try {
                DatagramPacket packet = new DatagramPacket(receivedFileName, receivedFileName.length);
                socket.receive(packet);
                DatagramSocket threadSocket = new DatagramSocket();
                Thread t = new Thread(new Connection(threadSocket, packet, clientID++));
                t.start();
            } catch (Exception e) {
                System.err.println("Error in connection attempt.");
            }
        }
    }
}
