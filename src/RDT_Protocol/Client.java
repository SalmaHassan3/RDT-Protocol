package RDT_Protocol;

import java.net.*;
import java.io.*;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Random;

public class Client {

    public static void getFilesNames() {
        File folder = new File("server/");
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                System.out.println(i + 1 + ". " + listOfFiles[i].getName());
            }
        }
    }
    public static PriorityQueue<Packet> heap = new PriorityQueue<>(new Comparator<Packet>() {
        public int compare(Packet packet1, Packet packet2) {
            if (packet1.seqNum < packet2.seqNum) {
                return -1;
            }
            if (packet1.seqNum > packet2.seqNum) {
                return 1;
            }
            return 0;
        }
    });

    public static void stopAndWait(DatagramSocket socket, String fileName, String clientID) throws Exception {
        String[] name = fileName.split("\\.");
        FileOutputStream stream = new FileOutputStream("client/" + name[0] + "_out" + clientID + "." + name[1]);
        int seqNum = 0;
        int recSeqNum = -1;
        int i = 0;
        Random rand = new Random();
        float random = 0;
        while (true) {

            System.out.println("waiting for packet with seqnum " + seqNum);
            byte[] buffer = new byte[635];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
            socket.receive(reply);
            Packet pck = (Packet) Serialization.deserialize(reply.getData());
            random = rand.nextFloat();
            if (random < 0.1) {
                pck.seqNum++;
            }
            if (pck.checkSum == pck.checkSum()) {
                recSeqNum = pck.seqNum;
                System.out.println("received packet with seqnum" + recSeqNum);
                ACK ack = new ACK(recSeqNum);
                byte[] ackData = Serialization.serialize(ack);
                DatagramPacket packetAck = new DatagramPacket(ackData, ackData.length, reply.getAddress(), reply.getPort());
                System.out.println("sending ack " + recSeqNum);
                socket.send(packetAck);
                if (seqNum == recSeqNum) {
                    System.out.println("expected seqnum received " + recSeqNum);
                    System.out.println("writing packet  " + i++);
                    stream.write(pck.data);
                    if (pck.eof) {
                        stream.close();
                        System.out.println("5alas client");
                        return;
                    }
                    seqNum = (seqNum == 0) ? 1 : 0;
                }
            } else {
                System.out.println("corrupted packet");
            }
        }
    }

    public static void selectiveRepeat(DatagramSocket socket, String fileName, String clientID) throws Exception {
        HashSet<Integer> seqNums = new HashSet<>();
        String[] name = fileName.split("\\.");
        FileOutputStream stream = new FileOutputStream("client/" + name[0] + "_out" + clientID + "." + name[1]);
        int start = 0;
        //  int index;
        // int window=50;
//        Packet buff[]=new Packet[window];
//        for(int i=0;i<window;i++){
//            buff[i]=null;
//        }
        Random rand = new Random();
        float random = 0;
        //continueous receiving
        while (true) {
            //receiving packets
            byte[] buffer = new byte[635];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
            socket.receive(reply);
            Packet pck = (Packet) Serialization.deserialize(reply.getData());
            System.out.println("received packet with seqnum " + pck.seqNum);
            random = rand.nextFloat();
            if (random < 0.1) {
                pck.seqNum++;
            }
            if (pck.checkSum == pck.checkSum()) {
                //send ack 
                ACK ack = new ACK(pck.seqNum);
                byte[] ackData = Serialization.serialize(ack);
                DatagramPacket packetAck = new DatagramPacket(ackData, ackData.length, reply.getAddress(), reply.getPort());
                socket.send(packetAck);
                System.out.println("sending ack " + pck.seqNum);
                //checking for duplicates
                if (seqNums.add(pck.seqNum)) {
                    if (pck.seqNum == start) {
                        System.out.println("seqnum equals start of window, writing packet " + pck.seqNum + " in file and sliding window");
                        //slide window
                        start++;
                        //write in file
                        stream.write(pck.data);
                        if (pck.eof) {
                            stream.close();
                            System.out.println("5alas client");
                            return;
                        }
                    } else {
                        //put it in buffer
                        heap.add(pck);
//                    index=pck.seqNum%window;
//                    buff[index]=pck;
                        System.out.println("buffering out of order packet " + pck.seqNum);
                    }
                    while (!heap.isEmpty() && heap.peek().seqNum == start) {
                        //extract it
                        Packet p = heap.poll();
                        //write it
                        stream.write(p.data);
                        if (p.eof) {
                            stream.close();
                            System.out.println("5alas client");
                            return;
                        }
                        //slide window
                        start++;
                        System.out.println("writing packet " + p.seqNum + " in file and sliding window");
                    }
//                     while(buff[start%window]!=null){
//                         Packet p=buff[start%window];
//                         stream.write(p.data);
//                         System.out.println("writing packet " + p.seqNum + " in file and sliding window");
//                         if (p.eof) {
//                        stream.close();
//                        System.out.println("5alas client");
//                        return;
//                    }
//                         buff[start%window]=null;
//                         start++;                                               
//                     }
                } else {
                    System.out.println("packet " + pck.seqNum + " is a duplicate");
                }
            } else {
                System.out.println("corrupted packet");
            }
        }
    }

    static void GoBackN(DatagramSocket socket, String fileName, String clientID) throws Exception {
        String[] name = fileName.split("\\.");
        FileOutputStream stream = new FileOutputStream("client/" + name[0] + "_out" + clientID + "." + name[1]);
        int seqNum = 0;
        int recSeqNum = -1;
        int i = 0;
        boolean finished = false;
        Random rand = new Random();
        float random = 0;
        while (!finished) {
            byte[] buffer = new byte[635];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
            socket.receive(reply);
            Packet pck = (Packet) Serialization.deserialize(reply.getData());
            recSeqNum = pck.seqNum;
            System.out.println("received packet with seqnum " + recSeqNum + "and the expected is " + seqNum);
            random = rand.nextFloat();
            if (random < 0.1) {
                pck.seqNum++;
            }
            if (seqNum == recSeqNum && pck.checkSum == pck.checkSum()) {
                System.out.println("writing packet " + i++);
                stream.write(pck.data);
                if (pck.eof) {
                    stream.close();
                    System.out.println("[writing in file is finished]");
                    finished = true;
                }
                seqNum++;
            } else {
                System.out.println("received packet with seqnum " + recSeqNum + "is ignored[out of order or corrupted]");
            }
            if (seqNum != 0) {//in case pck 0 is lost
//                Random rand = new Random();
//                float random = rand.nextFloat();
                ACK ack = new ACK(seqNum - 1);
                byte[] ackData = Serialization.serialize(ack);
                DatagramPacket packetAck = new DatagramPacket(ackData, ackData.length, reply.getAddress(), reply.getPort());
//                if (random > 0.1) {
                socket.send(packetAck);
                System.out.println("sending ack with sequence number " + (seqNum - 1));
//                } else {
//                    System.out.println("lost ack with sequence number " + (seqNum - 1));
//                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Enter the name of the required file (with the extension)");
        getFilesNames();
        DatagramSocket socket = new DatagramSocket();
        Scanner scan = new Scanner(System.in);
        String name = scan.nextLine();
        System.out.println("Enter the number of the required method");
        System.out.println("1. Stop And Wait");
        System.out.println("2. Selective Repeat");
        System.out.println("3. Go Back N");
        int method = scan.nextInt();
        byte fileName[] = (name + "#" + Integer.toString(method)).getBytes();
        InetAddress IP = InetAddress.getByName("localhost");
        int portNumber = 2000;
        DatagramPacket packet = new DatagramPacket(fileName, fileName.length, IP, portNumber);
        socket.send(packet);
        System.out.println("File name in sent to the server");
        byte receivedCientID[] = new byte[1000];
        DatagramPacket p = new DatagramPacket(receivedCientID, receivedCientID.length);
        socket.receive(p);
        byte clientIDArray[] = new String(p.getData()).replaceAll("\0", "").getBytes();
        String clientID = new String(clientIDArray);
        System.out.println("This is client " + clientID);
        long start = System.currentTimeMillis();
        if (method == 1) {
            stopAndWait(socket, name, clientID);
            System.out.println("Stop and wait finished");
        } else if (method == 2) {
            selectiveRepeat(socket, name, clientID);
            System.out.println("Selective repeat finished");
        } else if (method == 3) {
            GoBackN(socket, name, clientID);
            System.out.println("Go back n finished");
        }
        long end = System.currentTimeMillis();
        System.out.println("Time taken is " + (end - start));
    }
}
