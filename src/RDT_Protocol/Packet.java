package RDT_Protocol;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public class Packet implements Serializable {

    int seqNum, clientID;
    long checkSum;
    byte data[];
    boolean eof = false, sent = false, acked = false;

    public Packet(byte[] data) {
        this.data = data;
    }

    public byte[] combineByteArrays(byte[] one, byte[] two) {
        byte[] combined = new byte[one.length + two.length];
        for (int i = 0; i < combined.length; ++i) {
            combined[i] = i < one.length ? one[i] : two[i - one.length];
        }
        return combined;
    }

    public byte[] combineHeaderWithData() {
        byte[] eofBytes = new byte[]{(byte) (eof ? 1 : 0)};
        byte[] seqNumBytes = ByteBuffer.allocate(4).putInt(seqNum).array();
        byte[] clientIDBytes = ByteBuffer.allocate(4).putInt(clientID).array();
        byte[] all = combineByteArrays(combineByteArrays(combineByteArrays(data, eofBytes), seqNumBytes), clientIDBytes);
        return all;
    }

    public long checkSum() {
        byte[] all = combineHeaderWithData();
        Checksum checksumEngine = new Adler32();
        checksumEngine.update(all, 0, all.length);
        long checksum = checksumEngine.getValue();
        return checksum;
    }
}
