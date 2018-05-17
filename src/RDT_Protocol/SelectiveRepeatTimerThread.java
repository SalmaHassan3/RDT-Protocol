package RDT_Protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SelectiveRepeatTimerThread implements Runnable {

    DatagramSocket clientSocket;
    DatagramPacket packet;
    Packet pck;

    public SelectiveRepeatTimerThread(DatagramSocket clientSocket, Packet pck, DatagramPacket packet) {
        this.clientSocket = clientSocket;
        this.packet = packet;
        this.pck = pck;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        long end = start + 100;
        while (System.currentTimeMillis() <= end);
        if (!pck.acked) {
            try {
                System.out.println("timeout packet "+pck.seqNum);
                byte[] data = Serialization.serialize(pck);
                DatagramPacket dp = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                clientSocket.send(dp);
                System.out.println("re-sending packet "+pck.seqNum);
                Thread t = new Thread(new SelectiveRepeatTimerThread(clientSocket,pck,packet));
                t.start();
            } catch (IOException ex) {
                Logger.getLogger(SelectiveRepeatTimerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
