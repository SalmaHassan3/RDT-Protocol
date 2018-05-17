package RDT_Protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SelectiveRepeatWaitingForAckThread implements Runnable {

    DatagramSocket clientSocket;
    ArrayList<Packet> packets;
    int recSeqNum;
    int windowSize;
    public SelectiveRepeatWaitingForAckThread(DatagramSocket clientSocket, ArrayList<Packet> packets,int windowSize) {
        this.clientSocket = clientSocket;
        this.packets = packets;
        this.windowSize=windowSize;
    }
    public boolean isAllAcked(){
        int count=0;
        for(int i=packets.size()-windowSize;i<packets.size();i++){
           Packet p=packets.get(i);
            if(p.acked)
                count++;
        }
        if(count==windowSize)
            return true;
        else return false;
    }
    @Override
    public void run() {
        while (!isAllAcked()){
            try {
                byte[] ack = new byte[100];
                DatagramPacket packetAck = new DatagramPacket(ack, ack.length);
                clientSocket.receive(packetAck);
                ACK recAck = (ACK) Serialization.deserializeAck(packetAck.getData());
                recSeqNum = recAck.seqNum;
                System.out.println("received ack of number " + recSeqNum);    
                packets.get(recSeqNum).acked=true;
                System.out.println("packet " + recSeqNum + " is marked as acked");                      
            } catch (IOException ex) {
                Logger.getLogger(SelectiveRepeatWaitingForAckThread.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(SelectiveRepeatWaitingForAckThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } 
        System.out.println("all packets are acked");
    }
}
