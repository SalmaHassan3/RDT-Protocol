package RDT_Protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GoBackNTimerThread implements Runnable {
    DatagramSocket clientSocket;
    DatagramPacket packet;
    ArrayList<Packet> packets;
    Packet pck;
    int windowSize;

    public GoBackNTimerThread(DatagramSocket clientSocket,DatagramPacket packet,ArrayList<Packet> packets,Packet pck, int windowSize) {
        this.clientSocket=clientSocket;
        this.packet=packet;
        this.packets = packets;
        this.pck=pck;
        this.windowSize = windowSize;

    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        long end = start + 100;      
        while (System.currentTimeMillis() <= end && !pck.acked) {
        }      
        if (!pck.acked) {
            Packet resentPck;
            try {
                System.out.println("[timer" + pck.seqNum + "]time out for packet " + pck.seqNum);
                Thread timer = new Thread(new GoBackNTimerThread(clientSocket, packet, packets, pck, windowSize));
                timer.start();
                for (int i = pck.seqNum; i < pck.seqNum + windowSize; i++) {
                    if(i>=packets.size()){
                        break;
                    }
                    resentPck = packets.get(i);
                    byte[] data = Serialization.serialize(resentPck);
                    DatagramPacket dp = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                    clientSocket.send(dp);
                    System.out.println("[timer " + pck.seqNum + "] resending packet " + i + "with seqNum" + resentPck.seqNum );
                }
            } catch (IOException ex) {
                Logger.getLogger(GoBackNTimerThread.class.getName()).log(Level.SEVERE, null, ex);
            }            
        }
    }
}
