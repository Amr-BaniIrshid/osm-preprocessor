package osm.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple helper for writing fixed-size binary records.
 * 
 * This keeps all binary layout definitions in one place and avoids
 * manual byte handling scattered through the code.
 */
public class BinaryRecordWriter implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(BinaryRecordWriter.class);
    
    private final Path path;
    private final int recordSize;
    private final String description;
    private final BufferedOutputStream output;
    private final ByteBuffer buffer;
    private int count = 0;
    
    /**
     * Create a new binary record writer.
     * 
     * @param path Where to write the binary file
     * @param recordSize Size of each record in bytes
     * @param description Optional label used in log messages
     */
    public BinaryRecordWriter(Path path, int recordSize, String description) throws IOException {
        this.path = path;
        this.recordSize = recordSize;
        this.description = description != null ? description : path.getFileName().toString();
        this.output = new BufferedOutputStream(Files.newOutputStream(path));
        this.buffer = ByteBuffer.allocate(recordSize).order(ByteOrder.LITTLE_ENDIAN);
    }
    
    public BinaryRecordWriter(Path path, int recordSize) throws IOException {
        this(path, recordSize, null);
    }
    
    /**
     * Write a record with long, long, short, byte format (raw_edges.bin).
     */
    public void writeEdge(long fromId, long toId, int speed, int flags) throws IOException {
        buffer.clear();
        buffer.putLong(fromId);
        buffer.putLong(toId);
        buffer.putShort((short) speed);
        buffer.put((byte) flags);
        output.write(buffer.array(), 0, recordSize);
        count++;
    }
    
    /**
     * Write a record with long, long, long, byte format (raw_turns.bin).
     */
    public void writeTurn(long fromWay, long viaNode, long toWay, int type) throws IOException {
        buffer.clear();
        buffer.putLong(fromWay);
        buffer.putLong(viaNode);
        buffer.putLong(toWay);
        buffer.put((byte) type);
        output.write(buffer.array(), 0, recordSize);
        count++;
    }
    
    /**
     * Write a record with long, double, double format (raw_nodes.bin).
     */
    public void writeNode(long nodeId, double lat, double lon) throws IOException {
        buffer.clear();
        buffer.putLong(nodeId);
        buffer.putDouble(lat);
        buffer.putDouble(lon);
        output.write(buffer.array(), 0, recordSize);
        count++;
    }
    
    @Override
    public void close() throws IOException {
        if (output != null) {
            output.close();
            log.info("Wrote {} records to {}", count, path);
        }
    }
    
    public int getCount() {
        return count;
    }
}