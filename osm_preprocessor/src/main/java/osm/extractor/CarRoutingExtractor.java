package osm.extractor;


import osm.config.CarFilterConfig;
import osm.io.BinaryRecordWriter;
import osm.model.TurnRestrictionType;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import osm.utils.OsmUtils;

import java.io.IOException;
import java.util.*;

/**
 * First-pass handler:
 *
 *   - Filters car-usable ways
 *   - Emits edges to raw_edges.bin
 *   - Collects used node IDs
 *   - Extracts turn restrictions to raw_turns.bin
 */
public class CarRoutingExtractor implements Sink {
    private static final Logger log = LoggerFactory.getLogger(CarRoutingExtractor.class);

    private final CarFilterConfig cfg;
    private final BinaryRecordWriter edgeWriter;
    private final BinaryRecordWriter turnWriter;
    private final Set<Long> usedNodeIds;

    private int edgeCount = 0;
    private int restrictionCount = 0;

    public CarRoutingExtractor(
            CarFilterConfig cfg,
            BinaryRecordWriter edgeWriter,
            BinaryRecordWriter turnWriter) {
        this.cfg = cfg;
        this.edgeWriter = edgeWriter;
        this.turnWriter = turnWriter;
        this.usedNodeIds = new HashSet<>();
    }

    @Override
    public void process(EntityContainer entityContainer) {
        Entity entity = entityContainer.getEntity();

        if (entity instanceof Way) {
            processWay((Way) entity);
        } else if (entity instanceof Relation) {
            processRelation((Relation) entity);
        }
    }

    private void processWay(Way way) {
        Collection<Tag> tags = way.getTags();

        if (!OsmUtils.isCarWay(tags, cfg)) {
            return;
        }

        int oneway = OsmUtils.isOneway(tags);
        int speedKph = OsmUtils.parseMaxspeedKph(tags, cfg);

        // Flags: bit 0 -> is oneway (1 if oneway != 0)
        int flags = 0;
        if (oneway != 0) {
            flags |= 0b00000001;
        }

        // Each pair of consecutive nodes -> an edge
        List<WayNode> wayNodes = way.getWayNodes();
        if (wayNodes.size() < 2) {
            return;
        }

        // Record that these node ids are used in the final graph
        for (WayNode node : wayNodes) {
            usedNodeIds.add(node.getNodeId());
        }

        try {
            if (oneway == 1) {
                // Forward only
                for (int i = 0; i < wayNodes.size() - 1; i++) {
                    long u = wayNodes.get(i).getNodeId();
                    long v = wayNodes.get(i + 1).getNodeId();
                    emitEdge(u, v, speedKph, flags);
                }
            } else if (oneway == -1) {
                // Backward only
                for (int i = 0; i < wayNodes.size() - 1; i++) {
                    long u = wayNodes.get(i).getNodeId();
                    long v = wayNodes.get(i + 1).getNodeId();
                    emitEdge(v, u, speedKph, flags);
                }
            } else {
                // Bidirectional: emit both directions
                for (int i = 0; i < wayNodes.size() - 1; i++) {
                    long u = wayNodes.get(i).getNodeId();
                    long v = wayNodes.get(i + 1).getNodeId();
                    emitEdge(u, v, speedKph, flags);
                    emitEdge(v, u, speedKph, flags);
                }
            }
        } catch (IOException e) {
            log.error("Error writing edge", e);
            throw new RuntimeException(e);
        }
    }

    private void emitEdge(long u, long v, int speedKph, int flags) throws IOException {
        edgeWriter.writeEdge(u, v, speedKph, flags);
        edgeCount++;
    }

    private void processRelation(Relation relation) {
        Collection<Tag> tags = relation.getTags();

        Optional<String> type = OsmUtils.getTag(tags, "type");
        if (type.isEmpty() || !type.get().equals("restriction")) {
            return;
        }

        String restrictionValue = OsmUtils.getTag(tags, "restriction").orElse(null);
        TurnRestrictionType restriction = TurnRestrictionType.fromOsmValue(restrictionValue);

        Long fromWayId = null;
        Long viaNodeId = null;
        Long toWayId = null;

        for (RelationMember member : relation.getMembers()) {
            if (member.getMemberRole().equals("from") && member.getMemberType() == EntityType.Way) {
                fromWayId = member.getMemberId();
            } else if (member.getMemberRole().equals("via") && member.getMemberType() == EntityType.Node) {
                viaNodeId = member.getMemberId();
            } else if (member.getMemberRole().equals("to") && member.getMemberType() == EntityType.Way) {
                toWayId = member.getMemberId();
            }
        }

        if (fromWayId == null || viaNodeId == null || toWayId == null) {
            return; // incomplete restriction; ignore
        }

        try {
            turnWriter.writeTurn(fromWayId, viaNodeId, toWayId, restriction.getCode());
            restrictionCount++;
        } catch (IOException e) {
            log.error("Error writing turn restriction", e);
            throw new RuntimeException(e);
        }
    }

    public void finalize_processing() {
        log.info("First pass: produced {} edges, {} restrictions, {} unique nodes",
                edgeCount, restrictionCount, usedNodeIds.size());
    }

    public Set<Long> getUsedNodeIds() {
        return usedNodeIds;
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
        // Not needed - writers are closed externally
    }
}