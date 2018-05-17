package RDT_Protocol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GoBackNThread implements Runnable {

    DatagramSocket clientSocket;
    DatagramPacket packet;
    ArrayList<Packet> packets;
    int windowSize;
    
    public GoBackNThread(DatagramSocket clientSocket, DatagramPacket packet, ArrayList<Packet> packets, int windowSize) {
        this.clientSocket = clientSocket;
        this.packet = packet;
        this.packets = packets;
        this.windowSize = windowSize;
    }
    
    @Override
    public void run() {
        boolean end=false;
        int exSeqNum=0;
        while (!end) {
            try {
                byte[] ack = new byte[100];
                DatagramPacket packetAck = new DatagramPacket(ack, ack.length);
                clientSocket.receive(packetAck);
                ACK recAck = (ACK) Serialization.deserializeAck(packetAck.getData());
                System.out.println("[ack receiver]receiving ack " + recAck.seqNum +"and the expected is"+ exSeqNum );

                int recSeqNum = recAck.seqNum;
                if (recSeqNum >= exSeqNum) {
                    for (int i = recSeqNum; i >= exSeqNum; i--) {
                        packets.get(i).acked = true;
                        System.out.println("[ack receiver]packet " + i + " is marked ack");
                    }
                    if (recSeqNum + 1 < packets.size()) {//check size incase it is the last packet
                        exSeqNum=recSeqNum + 1;      
                        Thread timer = new Thread(new GoBackNTimerThread(clientSocket, packet, packets, packets.get(exSeqNum), windowSize));
                        timer.start();                     
                    }
                    if (packets.get(packets.size()-1).acked) {
                        end = true;
                    }
                } else {
                    System.out.println("[ack receiver] ack " + recAck.seqNum + " is ignored");
                }

            } catch (Exception ex) {
                Logger.getLogger(GoBackNThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("[ack receiver]finished ");        
    }
}
