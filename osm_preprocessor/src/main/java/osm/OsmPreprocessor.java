package osm;



import org.openstreetmap.osmosis.pbf2.v0_6.PbfReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import osm.config.CarFilterConfig;
import osm.extractor.CarRoutingExtractor;
import osm.extractor.NodeCollector;
import osm.io.BinaryRecordWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Main orchestration for preprocessing a .osm.pbf file into compact binary snapshots.
 *
 * Preprocess a .osm.pbf file into compact binary snapshots for a car-routing engine:
 *
 * 1. Stream-parse .osm.pbf (Osmosis).
 * 2. Filter car-only ways.
 * 3. Write:
 *    - raw_edges.bin  (OSM way segments -> edges)
 *    - raw_turns.bin  (turn restrictions)
 *    - raw_nodes.bin  (only nodes actually used by car edges)
 */
public class OsmPreprocessor {
    private static final Logger log = LoggerFactory.getLogger(OsmPreprocessor.class);

    // Record sizes in bytes
    private static final int EDGE_RECORD_SIZE = 8 + 8 + 2 + 1; // long + long + short + byte = 19
    private static final int TURN_RECORD_SIZE = 8 + 8 + 8 + 1; // long + long + long + byte = 25
    private static final int NODE_RECORD_SIZE = 8 + 8 + 8;      // long + double + double = 24

    /**
     * Build raw snapshot from OSM PBF file.
     *
     * 1. First pass:
     *    - parse ways + relations
     *    - write raw_edges.bin + raw_turns.bin
     *    - collect used node IDs
     *
     * 2. Second pass:
     *    - parse nodes
     *    - write raw_nodes.bin only for used nodes
     */
    public static void buildRawSnapshot(Path osmPbf, Path outDir) throws IOException {
        Files.createDirectories(outDir);

        CarFilterConfig cfg = CarFilterConfig.defaultConfig();

        // ----- First pass: ways + restrictions --------------------------------
        log.info("First pass: parsing ways + relations from {}", osmPbf);

        BinaryRecordWriter edgesWriter = new BinaryRecordWriter(
                outDir.resolve("raw_edges.bin"),
                EDGE_RECORD_SIZE,
                "raw_edges");

        BinaryRecordWriter turnsWriter = new BinaryRecordWriter(
                outDir.resolve("raw_turns.bin"),
                TURN_RECORD_SIZE,
                "raw_turns");

        CarRoutingExtractor extractor = new CarRoutingExtractor(cfg, edgesWriter, turnsWriter);

        try {
            PbfReader reader = new PbfReader(osmPbf.toFile(), 1);
            reader.setSink(extractor);
            reader.run();
        } finally {
            edgesWriter.close();
            turnsWriter.close();
        }

        Set<Long> usedNodeIds = extractor.getUsedNodeIds();
        log.info("Total used nodes: {}", usedNodeIds.size());

        // ----- Second pass: nodes ---------------------------------------------
        log.info("Second pass: collecting nodes from {}", osmPbf);

        BinaryRecordWriter nodesWriter = new BinaryRecordWriter(
                outDir.resolve("raw_nodes.bin"),
                NODE_RECORD_SIZE,
                "raw_nodes");

        NodeCollector nodeCollector = new NodeCollector(usedNodeIds, nodesWriter);

        try {
            PbfReader reader = new PbfReader(osmPbf.toFile(), 1);
            reader.setSink(nodeCollector);
            reader.run();
        } finally {
            nodesWriter.close();
        }

        log.info("Raw snapshot build completed.");
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: OsmPreprocessor <osm.pbf> [output-dir]");
            System.err.println("  osm.pbf    : Input .osm.pbf file (e.g., jordan-latest.osm.pbf)");
            System.err.println("  output-dir : Directory to write raw_*.bin files (default: ./build/raw_snapshot)");
            System.exit(1);
        }

        Path osmPbf = Path.of(args[0]);
        Path outDir = args.length > 1
                ? Path.of(args[1])
                : Path.of("./build/raw_snapshot");

        if (!Files.isRegularFile(osmPbf)) {
            System.err.println("Input file not found: " + osmPbf);
            System.exit(1);
        }

        try {
            log.info("Building raw snapshot from {} into {}", osmPbf, outDir);
            buildRawSnapshot(osmPbf, outDir);
        } catch (Exception e) {
            log.error("Error processing OSM file", e);
            System.exit(1);
        }
    }
}