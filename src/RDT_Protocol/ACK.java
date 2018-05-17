package RDT_Protocol;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public class ACK implements Serializable {

    int seqNum;
    long checkSum;

    public ACK(int seqNum) {
        this.seqNum = seqNum;
    }

    public void checkSum() {
        byte[] seqNumBytes = ByteBuffer.allocate(4).putInt(seqNum).array();
        Checksum checksumEngine = new Adler32();
        checksumEngine.update(seqNumBytes, 0, seqNumBytes.length);
        long checksum = checksumEngine.getValue();
        this.checkSum = checksum;
    }
}
