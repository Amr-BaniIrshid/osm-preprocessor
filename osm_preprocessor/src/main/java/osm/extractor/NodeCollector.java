package osm.extractor;

import osm.io.BinaryRecordWriter;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Second-pass handler:
 *
 * Writes only nodes that appear in the used_node_ids set to raw_nodes.bin.
 */
public class NodeCollector implements Sink {
    private static final Logger log = LoggerFactory.getLogger(NodeCollector.class);

    private final Set<Long> usedNodeIds;
    private final BinaryRecordWriter nodeWriter;
    private int count = 0;

    public NodeCollector(Set<Long> usedNodeIds, BinaryRecordWriter nodeWriter) {
        this.usedNodeIds = usedNodeIds;
        this.nodeWriter = nodeWriter;
    }

    @Override
    public void process(EntityContainer entityContainer) {
        Entity entity = entityContainer.getEntity();

        if (entity instanceof Node) {
            processNode((Node) entity);
        }
    }

    private void processNode(Node node) {
        long nodeId = node.getId();

        if (!usedNodeIds.contains(nodeId)) {
            return;
        }

        try {
            nodeWriter.writeNode(nodeId, node.getLatitude(), node.getLongitude());
            count++;
        } catch (IOException e) {
            log.error("Error writing node", e);
            throw new RuntimeException(e);
        }
    }

    public void finalize_processing() {
        log.info("Second pass: wrote {} nodes", count);
    }

    @Override
    public void initialize(Map<String, Object> map) {
        // Not needed
    }

    @Override
    public void complete() {
        finalize_processing();
    }

    @Override
    public void close() {
        // Not needed - writer is closed externally
    }
}