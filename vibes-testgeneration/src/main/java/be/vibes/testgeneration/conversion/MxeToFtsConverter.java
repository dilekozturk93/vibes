package be.vibes.testgeneration.conversion;

import be.vibes.dsl.io.Xml;
import be.vibes.fexpression.FExpression;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.FeaturedTransitionSystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Converts an mxGraph (.mxe) Event Sequence Graph with Feature Expressions
 * (ESG-Fx) into a VIBeS {@link FeaturedTransitionSystem}.
 *
 * <p>ESG-Fx vertices carry an event name and a feature expression
 * (encoded in the mxGraph node name as {@code event/fexpr}). The special
 * vertices "[" and "]" mark test start and termination respectively.
 * Edges encode admissible event sequencing.
 *
 * <p>Mapping rules (see plan, "Design Decisions"):
 * <ul>
 *   <li>"[" is mapped to a single FTS initial state {@code INIT}.</li>
 *   <li>"]" has no corresponding FTS state; it is merged with {@code INIT}
 *       so that the resulting FTS is cyclic.</li>
 *   <li>Any non-"[" non-"]" ESG vertex {@code v} whose outgoing edges all
 *       go to "]" is treated as a "terminal" vertex and also merged with
 *       {@code INIT}. This reproduces the inlining seen in hand-written
 *       VIBeS FTS files (e.g. {@code state9 -[close]-> state1} in
 *       fts-sodaVendingMachine.xml).</li>
 *   <li>Every other ESG vertex {@code v} becomes an FTS state {@code s_v}.</li>
 *   <li>An ESG edge {@code u -> v} becomes an FTS transition
 *       {@code state(u) -[event(v) / fexpr(v)]-> state(v)}, where
 *       {@code state(x)} resolves to {@code INIT} for "[" or terminal
 *       vertices and to {@code s_x} otherwise.</li>
 *   <li>Edges of the form {@code u -> "]"} are dropped (their effect is
 *       already captured by terminal-vertex merging).</li>
 * </ul>
 *
 * <p>Feature expressions in the mxGraph node names support a leading
 * negation ({@code !X}). Both forms produce the same feature symbol space
 * as the corresponding DIMACS / feature model mapping used by the rest of
 * the pipeline.
 */
public class MxeToFtsConverter {

    private static final Logger LOG = LoggerFactory.getLogger(MxeToFtsConverter.class);

    private static final String START_NAME = "[";
    private static final String END_NAME = "]";
    private static final String INITIAL_STATE = "INIT";

    /**
     * Converts the given MXE file into a {@link FeaturedTransitionSystem}.
     *
     * @param mxeFile the input .mxe file
     * @return the resulting FTS
     * @throws Exception if the file cannot be parsed
     */
    public FeaturedTransitionSystem convert(File mxeFile) throws Exception {
        checkNotNull(mxeFile, "MXE file may not be null");
        checkArgument(mxeFile.exists(), "MXE file does not exist: %s", mxeFile);

        EsgGraph esg = parse(mxeFile);
        return buildFts(esg);
    }

    /**
     * Parses the MXE file into an in-memory ESG representation. Only the
     * pieces of the mxGraph schema actually used by ESG-Fx are interpreted
     * (vertex / edge cells and embedded {@code EventNode} children).
     */
    private EsgGraph parse(File mxeFile) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.parse(mxeFile);
        doc.getDocumentElement().normalize();

        EsgGraph esg = new EsgGraph();
        NodeList cells = doc.getElementsByTagName("mxCell");
        for (int i = 0; i < cells.getLength(); i++) {
            Node n = cells.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element cell = (Element) n;
            if ("1".equals(cell.getAttribute("vertex"))) {
                String id = cell.getAttribute("id");
                String rawName = extractEventName(cell);
                if (rawName == null) {
                    LOG.debug("Vertex cell {} has no EventNode child, skipping", id);
                    continue;
                }
                esg.addVertex(new EsgVertex(id, rawName));
            } else if ("1".equals(cell.getAttribute("edge"))) {
                String source = cell.getAttribute("source");
                String target = cell.getAttribute("target");
                if (source.isEmpty() || target.isEmpty()) {
                    LOG.debug("Edge cell {} has missing endpoint(s), skipping", cell.getAttribute("id"));
                    continue;
                }
                esg.addEdge(new EsgEdge(source, target));
            }
        }
        LOG.info("Parsed {}: {} vertices, {} edges", mxeFile.getName(), esg.vertices.size(), esg.edges.size());
        return esg;
    }

    private static String extractEventName(Element vertexCell) {
        NodeList children = vertexCell.getElementsByTagName("de.upb.adt.tsd.EventNode");
        if (children.getLength() == 0) {
            return null;
        }
        Element event = (Element) children.item(0);
        return event.getAttribute("name");
    }

    /**
     * Builds the {@link FeaturedTransitionSystem} from the parsed ESG using
     * the mapping rules documented in the class javadoc.
     */
    private FeaturedTransitionSystem buildFts(EsgGraph esg) {
        Map<String, EsgVertex> byId = esg.vertices;

        String startId = esg.findVertexIdByName(START_NAME);
        String endId = esg.findVertexIdByName(END_NAME);
        if (startId == null) {
            throw new IllegalArgumentException("ESG has no '[' (start) vertex");
        }
        if (endId == null) {
            // Not fatal; a model may already be cyclic. But we currently rely on
            // it for the canonical mapping; log and continue.
            LOG.warn("ESG has no ']' (end) vertex; cyclic-rewiring step will be a no-op");
        }

        // Index outgoing edges by source vertex id, for efficient terminal detection.
        Map<String, List<EsgEdge>> outgoing = new HashMap<>();
        for (EsgEdge e : esg.edges) {
            outgoing.computeIfAbsent(e.source, k -> new ArrayList<>()).add(e);
        }

        // A vertex is "terminal" iff it has at least one outgoing edge and ALL of
        // them target the end vertex. Such vertices are merged with INIT.
        Set<String> terminalIds = new HashSet<>();
        for (Map.Entry<String, List<EsgEdge>> entry : outgoing.entrySet()) {
            String vid = entry.getKey();
            if (vid.equals(startId) || vid.equals(endId)) {
                continue;
            }
            boolean allToEnd = true;
            for (EsgEdge e : entry.getValue()) {
                if (!e.target.equals(endId)) {
                    allToEnd = false;
                    break;
                }
            }
            if (allToEnd) {
                terminalIds.add(vid);
            }
        }
        LOG.info("Identified {} terminal vertices (merged with {}): {}",
                terminalIds.size(), INITIAL_STATE, terminalIds);

        FeaturedTransitionSystemFactory factory = new FeaturedTransitionSystemFactory(INITIAL_STATE);

        // Pre-declare states for every non-merged vertex so addTransition can
        // resolve names deterministically. Vertex ids are used as state names
        // (they are unique within an mxGraph document) prefixed for readability.
        for (EsgVertex v : byId.values()) {
            if (v.id.equals(startId) || v.id.equals(endId) || terminalIds.contains(v.id)) {
                continue;
            }
            factory.addState(stateNameFor(v));
        }

        // Build transitions.
        int added = 0;
        int skipped = 0;
        for (EsgEdge edge : esg.edges) {
            if (edge.target.equals(endId)) {
                // Already represented by terminal-vertex merging.
                skipped++;
                continue;
            }
            EsgVertex target = byId.get(edge.target);
            if (target == null) {
                LOG.warn("Edge references unknown target vertex {}", edge.target);
                continue;
            }
            String sourceState = stateOf(edge.source, startId, terminalIds, byId);
            String targetState = stateOf(edge.target, startId, terminalIds, byId);
            EventLabel label = parseEventLabel(target.rawName);
            factory.addAction(label.action);
            factory.addTransition(sourceState, label.action, label.fexpr, targetState);
            added++;
        }
        LOG.info("Built FTS: {} transitions added, {} dropped (terminal merge)", added, skipped);

        return factory.build();
    }

    private static String stateOf(String vertexId, String startId, Set<String> terminalIds,
                                  Map<String, EsgVertex> byId) {
        if (vertexId.equals(startId) || terminalIds.contains(vertexId)) {
            return INITIAL_STATE;
        }
        EsgVertex v = byId.get(vertexId);
        return stateNameFor(v);
    }

    private static String stateNameFor(EsgVertex v) {
        return "v" + v.id;
    }

    /**
     * Parses an event label of the form {@code event/fexpr} into its action
     * name and a VIBeS {@link FExpression}. A leading "!" on the feature
     * expression is interpreted as negation. An empty or missing feature
     * expression yields {@code FExpression.trueValue()}.
     */
    static EventLabel parseEventLabel(String raw) {
        checkNotNull(raw, "Event label may not be null");
        String[] parts = raw.split("/", 2);
        String action = parts[0].trim();
        FExpression fexpr;
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            fexpr = FExpression.trueValue();
        } else {
            String fe = parts[1].trim();
            if (fe.startsWith("!")) {
                fexpr = FExpression.featureExpr(fe.substring(1).trim()).not();
            } else {
                fexpr = FExpression.featureExpr(fe);
            }
        }
        return new EventLabel(action, fexpr);
    }

    /** Internal value type for the (action, fexpr) pair carried by an ESG vertex. */
    static final class EventLabel {
        final String action;
        final FExpression fexpr;

        EventLabel(String action, FExpression fexpr) {
            this.action = action;
            this.fexpr = fexpr;
        }
    }

    /** Minimal in-memory ESG representation used during conversion. */
    private static final class EsgGraph {
        final Map<String, EsgVertex> vertices = new HashMap<>();
        final List<EsgEdge> edges = new ArrayList<>();

        void addVertex(EsgVertex v) {
            vertices.put(v.id, v);
        }

        void addEdge(EsgEdge e) {
            edges.add(e);
        }

        String findVertexIdByName(String rawName) {
            for (EsgVertex v : vertices.values()) {
                if (rawName.equals(v.rawName)) {
                    return v.id;
                }
            }
            return null;
        }
    }

    private static final class EsgVertex {
        final String id;
        final String rawName;

        EsgVertex(String id, String rawName) {
            this.id = id;
            this.rawName = rawName;
        }
    }

    private static final class EsgEdge {
        final String source;
        final String target;

        EsgEdge(String source, String target) {
            this.source = source;
            this.target = target;
        }
    }

    /**
     * CLI: converts a single MXE file and prints the resulting FTS as Dot to
     * stdout. Optionally writes the FTS XML to the path given as the second
     * argument.
     *
     * <p>Usage: {@code java MxeToFtsConverter <input.mxe> [output.xml]}
     */
    public static void main(String[] args) throws Exception {
        checkArgument(args.length >= 1,
                "Usage: java %s <input.mxe> [output.xml]", MxeToFtsConverter.class.getName());
        File mxe = new File(args[0]);
        FeaturedTransitionSystem fts = new MxeToFtsConverter().convert(mxe);
        if (args.length >= 2) {
            try (PrintStream out = new PrintStream(args[1])) {
                Xml.print(fts, out);
            }
        }
        System.out.println(be.vibes.dsl.io.Dot.format(fts));
    }
}
