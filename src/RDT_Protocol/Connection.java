package RDT_Protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Connection implements Runnable {

    int clientID;
    DatagramSocket clientSocket;
    DatagramPacket packet;

    public Connection(DatagramSocket clientSocket, DatagramPacket packet, int clientID) throws IOException {
        this.clientSocket = clientSocket;
        this.packet = packet;
        this.clientID = clientID;
    }

    static byte[] readFile(String fileName) {
        fileName = "server/" + fileName;
        File file = new File(fileName);
        FileInputStream stream = null;
        byte fileContent[] = null;
        try {
            stream = new FileInputStream(fileName);
            fileContent = new byte[(int) file.length()];
            stream.read(fileContent);
            stream.close();
            return fileContent;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return fileContent;
    }

    public static ArrayList<Packet> split(byte data[]) {
        ArrayList<Packet> packets = new ArrayList<>();
        int size = 400;
        int counter = (data.length / size) + 1;
        byte[] range = null;
        int index = 0;
        for (int i = 1; i < counter; i++) {
            range = Arrays.copyOfRange(data, index, index + size);
            packets.add(new Packet(range));
            index = index + size;
        }
        if (data.length % size != 0) {
            range = Arrays.copyOfRange(data, index, data.length);
            packets.add(new Packet(range));
        }
        packets.get((packets.size()) - 1).eof = true;
        return packets;
    }

    void stopAndWait(ArrayList<Packet> packets) throws ClassNotFoundException {
        int seqNum = 0;
        int recSeqNum = -1;
        Packet pck;
        boolean flag;
        Random rand = new Random();
        float random;
        for (int i = 0; i < packets.size(); i++) {
            pck = packets.get(i);
            random = rand.nextFloat();
            try {
                pck.seqNum = seqNum;
                pck.checkSum=pck.checkSum();
                byte[] data = Serialization.serialize(pck);
                DatagramPacket dp = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                if (random > 0.3) {
                    clientSocket.send(dp);
                    System.out.println("sending packet " + i + " with sequence number " + pck.seqNum);
                } else {
                    System.out.println("lost packet " + i + " with sequence number " + pck.seqNum);
                }
                clientSocket.setSoTimeout(100);
                flag = true;
                while (flag) {
                    byte[] ack = new byte[100];
                    DatagramPacket packetAck = new DatagramPacket(ack, ack.length);
                    try {
                        System.out.println("waiting for ack " + seqNum);
                        clientSocket.receive(packetAck);
                        ACK recAck = (ACK) Serialization.deserializeAck(packetAck.getData());
                        System.out.println("receiving ack " + recAck.seqNum + " and the expected is " + seqNum);
                        recSeqNum = recAck.seqNum;
                        if (seqNum == recSeqNum) {
                            seqNum = (seqNum == 0) ? 1 : 0;
                            System.out.println("seqnum is switched");
                            flag = false;
                        }
                    } catch (SocketTimeoutException exception) {
                        System.out.println("time out while sending packet " + i);
                        System.out.println("retransmitting packet " + i);
                        i--;
                        flag = false;
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    void selectiveRepeat(int windowSize, ArrayList<Packet> packets) throws ClassNotFoundException {
        Packet pck;
        Random rand = new Random();
        float random;
        windowSize = Math.min(windowSize, packets.size());
        int start = 0, end = windowSize - 1;
        int maxIndex = packets.size();
        for (int i = 0; i < packets.size(); i++) {
            Packet p = packets.get(i);
            p.seqNum = i;
            p.checkSum=p.checkSum();
        }
        Thread t1 = new Thread(new SelectiveRepeatWaitingForAckThread(clientSocket, packets, windowSize));
        t1.start();
        while (!packets.get(maxIndex - 1).sent) {
            for (int i = start; i <= end; i++) {
                random = rand.nextFloat();
                pck = packets.get(i);
                if (!pck.sent) {
                    try {
                        byte[] data = Serialization.serialize(pck);
                        DatagramPacket dp = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                        pck.sent = true;
                        if (random > 0.3) {
                            clientSocket.send(dp);
                        }
                        System.out.println("sending packet " + i);
                        Thread t2 = new Thread(new SelectiveRepeatTimerThread(clientSocket, pck, packet));
                        t2.start();
                    } catch (IOException ex) {
                        Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            while ((end < maxIndex - 1 && packets.get(start).acked)) {
                System.out.println("sliding window with start " + start + "and end is " + end);
                start++;
                end++;
            }
        }
        System.out.println("5alas server");
    }

    void GoBackN(int windowSize, ArrayList<Packet> packets) throws Exception {
        int base = 0;
        int seqNum = 0;
        boolean start = true;
        boolean end = false;
        for (int i = 0; i < packets.size(); i++) {
            packets.get(i).seqNum = i;
            packets.get(i).checkSum=packets.get(i).checkSum();
        }
        Packet pck;
        while (!end) {
            Random rand = new Random();
            float random;
            random = rand.nextFloat();
            if (seqNum < base + windowSize && seqNum < packets.size()) {//check size incase it is the last packet
                pck = packets.get(seqNum);
                byte[] data = Serialization.serialize(pck);
                DatagramPacket dp = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                if (random > 0.3) {
                    clientSocket.send(dp);
                    System.out.println("[packet sender]sending packet with sequence number " + pck.seqNum);
                } else {
                    System.out.println("[packet sender] LOST!! packet with sequence number  " + pck.seqNum);
                }
                if (start) {
                    Thread timer = new Thread(new GoBackNTimerThread(clientSocket, packet, packets, pck, windowSize));
                    timer.start();
                    Thread t = new Thread(new GoBackNThread(clientSocket, packet, packets, windowSize));
                    t.start();
                    start = false;
                }
                seqNum++;
            }
            while (base + 1 < packets.size() && packets.get(base).acked) {
                base++;
            }
            if (packets.get(packets.size() - 1).acked) {
                end = true;
            }
        }
        System.out.println("[packet sender]finished ");
    }

    public void sendClientID() throws IOException {
        String ID = Integer.toString(clientID);
        byte[] clientIDArray = ID.getBytes();
        DatagramPacket p = new DatagramPacket(clientIDArray, clientIDArray.length, packet.getAddress(), packet.getPort());
        clientSocket.send(p);
    }

    @Override
    public void run() {
        byte[] fileNameAndMethod = new String(packet.getData()).replaceAll("\0", "").getBytes();
        String message = new String(fileNameAndMethod);
        String[] messageArray = message.split("#");
        String fileName = messageArray[0];
        String method = messageArray[1];
        try {
            sendClientID();
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte fileArray[] = readFile(fileName);
        ArrayList<Packet> packets = split(fileArray);
        try {
            if (method.equalsIgnoreCase("1")) {
                stopAndWait(packets);
                System.out.println("Stop and wait finished");
            } else if (method.equalsIgnoreCase("2")) {
                selectiveRepeat(50, packets);
                System.out.println("Selective repeat finished");
            } else if (method.equalsIgnoreCase("3")) {
                GoBackN(50, packets);
                System.out.println("Go back n finished");
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
