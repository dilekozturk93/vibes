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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
     * Builds the {@link FeaturedTransitionSystem} from the parsed ESG.
     *
     * <p>Pipeline:
     * <ol>
     *   <li>Identify the {@code "["} start vertex, the {@code "]"} end
     *       vertex, and "terminal" vertices (those whose every outgoing
     *       edge targets {@code "]"}). Merge them all into a single
     *       initial FTS state {@code state1}.</li>
     *   <li>Run partition refinement on the remaining vertices to
     *       collapse bisimulation-equivalent classes: two vertices with
     *       identical outgoing multisets of {@code (action, fexpr,
     *       target_class)} are merged. This is what reduces e.g. SVM's
     *       {@code change/!f} and {@code free/f} into a single state,
     *       matching Devroey's hand-written canonical FTS.</li>
     *   <li>Assign sequential state names ({@code state1}, {@code state2},
     *       …) by breadth-first traversal from the initial class.</li>
     *   <li>Emit one FTS state per class and one FTS transition per
     *       distinct {@code (action, fexpr, target_class)} outgoing
     *       edge of each class representative.</li>
     * </ol>
     */
    private FeaturedTransitionSystem buildFts(EsgGraph esg) {
        Map<String, EsgVertex> byId = esg.vertices;

        String startId = esg.findVertexIdByName(START_NAME);
        String endId = esg.findVertexIdByName(END_NAME);
        if (startId == null) {
            throw new IllegalArgumentException("ESG has no '[' (start) vertex");
        }
        if (endId == null) {
            LOG.warn("ESG has no ']' (end) vertex; cyclic-rewiring step will be a no-op");
        }

        // Index outgoing edges per source vertex (cycle-free table lookup).
        Map<String, List<EsgEdge>> outgoing = new HashMap<>();
        for (EsgEdge e : esg.edges) {
            outgoing.computeIfAbsent(e.source, k -> new ArrayList<>()).add(e);
        }

        Set<String> terminalIds = detectTerminalVertices(outgoing, startId, endId);
        Set<String> initMembers = new HashSet<>();
        initMembers.add(startId);
        if (endId != null) {
            initMembers.add(endId);
        }
        initMembers.addAll(terminalIds);
        LOG.info("INIT-class members: start={}, end={}, terminals={}",
                startId, endId, terminalIds);

        // Vertices that are candidates for bisimulation merging.
        List<String> nonInitIds = new ArrayList<>();
        for (String vid : byId.keySet()) {
            if (!initMembers.contains(vid)) {
                nonInitIds.add(vid);
            }
        }

        Map<String, Integer> classOf = partitionRefine(nonInitIds, outgoing, byId,
                initMembers, endId);

        // Pick one canonical representative per class (any member works since
        // they are bisimulation-equivalent; we pick the first encountered).
        Map<Integer, String> classRepresentative = new LinkedHashMap<>();
        classRepresentative.put(INIT_CLASS, startId);
        for (String vid : nonInitIds) {
            classRepresentative.putIfAbsent(classOf.get(vid), vid);
        }

        // BFS from INIT to assign sequential state names state1, state2, ...
        Map<Integer, String> classToStateName = assignSequentialStateNames(
                classRepresentative, outgoing, classOf, initMembers, endId, byId);

        // Build the FTS.
        String initialStateName = classToStateName.get(INIT_CLASS);
        FeaturedTransitionSystemFactory factory =
                new FeaturedTransitionSystemFactory(initialStateName);
        for (String stateName : classToStateName.values()) {
            factory.addState(stateName);
        }
        int transitionCount = 0;
        for (Map.Entry<Integer, String> entry : classRepresentative.entrySet()) {
            int srcClass = entry.getKey();
            String srcName = classToStateName.get(srcClass);
            for (EsgEdge e : outgoing.getOrDefault(entry.getValue(), Collections.emptyList())) {
                if (e.target.equals(endId)) {
                    continue;
                }
                EsgVertex target = byId.get(e.target);
                if (target == null) {
                    continue;
                }
                int targetClass = initMembers.contains(e.target) ? INIT_CLASS : classOf.get(e.target);
                String targetName = classToStateName.get(targetClass);
                EventLabel label = parseEventLabel(target.rawName);
                factory.addAction(label.action);
                factory.addTransition(srcName, label.action, label.fexpr, targetName);
                transitionCount++;
            }
        }
        LOG.info("Built FTS: {} states (classes), {} transitions (post-dedup may be smaller)",
                classToStateName.size(), transitionCount);

        return factory.build();
    }

    private static final int INIT_CLASS = 0;

    /** A vertex is "terminal" iff every outgoing edge of it targets {@code "]"}. */
    private static Set<String> detectTerminalVertices(Map<String, List<EsgEdge>> outgoing,
                                                      String startId, String endId) {
        Set<String> terminals = new HashSet<>();
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
                terminals.add(vid);
            }
        }
        return terminals;
    }

    /**
     * Partition-refinement bisimulation: starts with all non-INIT vertices in
     * one class, refines by outgoing-edge signature
     * {@code sorted [(action, fexpr, target_class)]} until stable.
     */
    private Map<String, Integer> partitionRefine(List<String> nonInitIds,
                                                 Map<String, List<EsgEdge>> outgoing,
                                                 Map<String, EsgVertex> byId,
                                                 Set<String> initMembers,
                                                 String endId) {
        Map<String, Integer> classOf = new HashMap<>();
        int firstNonInitClass = INIT_CLASS + 1;
        for (String vid : nonInitIds) {
            classOf.put(vid, firstNonInitClass);
        }
        int nextClassId = firstNonInitClass + 1;

        boolean changed = true;
        while (changed) {
            changed = false;
            Map<Integer, List<String>> byClass = new LinkedHashMap<>();
            for (String vid : nonInitIds) {
                byClass.computeIfAbsent(classOf.get(vid), k -> new ArrayList<>()).add(vid);
            }
            Map<String, Integer> nextClassOf = new HashMap<>(classOf);
            for (Map.Entry<Integer, List<String>> e : byClass.entrySet()) {
                List<String> members = e.getValue();
                if (members.size() <= 1) {
                    continue;
                }
                Map<String, List<String>> bySignature = new LinkedHashMap<>();
                for (String vid : members) {
                    String sig = signatureOf(vid, outgoing, byId, classOf, initMembers, endId);
                    bySignature.computeIfAbsent(sig, k -> new ArrayList<>()).add(vid);
                }
                if (bySignature.size() > 1) {
                    boolean first = true;
                    for (List<String> group : bySignature.values()) {
                        if (first) {
                            first = false;
                            // First sub-group keeps the original class id.
                        } else {
                            int newId = nextClassId++;
                            for (String vid : group) {
                                nextClassOf.put(vid, newId);
                            }
                        }
                    }
                    changed = true;
                }
            }
            classOf = nextClassOf;
        }
        return classOf;
    }

    /**
     * Returns the canonical outgoing signature of a vertex: a sorted-multiset
     * string of {@code action:fexpr:target_class} entries over its outgoing
     * edges. Edges to {@code "]"} are excluded (they are handled by
     * terminal-vertex merging).
     */
    private String signatureOf(String vid, Map<String, List<EsgEdge>> outgoing,
                               Map<String, EsgVertex> byId, Map<String, Integer> classOf,
                               Set<String> initMembers, String endId) {
        List<String> parts = new ArrayList<>();
        for (EsgEdge e : outgoing.getOrDefault(vid, Collections.emptyList())) {
            if (e.target.equals(endId)) {
                continue;
            }
            EsgVertex tgt = byId.get(e.target);
            if (tgt == null) {
                continue;
            }
            EventLabel label = parseEventLabel(tgt.rawName);
            int tgtClass = initMembers.contains(e.target) ? INIT_CLASS : classOf.get(e.target);
            parts.add(label.action + "/" + label.fexpr.applySimplification().toString()
                    + ":" + tgtClass);
        }
        Collections.sort(parts);
        return String.join("|", parts);
    }

    /**
     * Performs a breadth-first traversal of the class-quotient graph starting
     * from {@code INIT_CLASS} and assigns sequential state names
     * {@code state1, state2, ...} in visitation order. The order is
     * deterministic because outgoing edges are iterated in the order they
     * appeared in the source MXE.
     */
    private Map<Integer, String> assignSequentialStateNames(
            Map<Integer, String> classRepresentative,
            Map<String, List<EsgEdge>> outgoing,
            Map<String, Integer> classOf,
            Set<String> initMembers,
            String endId,
            Map<String, EsgVertex> byId) {
        Map<Integer, String> classToStateName = new LinkedHashMap<>();
        classToStateName.put(INIT_CLASS, "state1");
        int nextStateNumber = 2;
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(INIT_CLASS);
        while (!queue.isEmpty()) {
            int currentClass = queue.poll();
            String rep = classRepresentative.get(currentClass);
            for (EsgEdge e : outgoing.getOrDefault(rep, Collections.emptyList())) {
                if (e.target.equals(endId)) {
                    continue;
                }
                if (byId.get(e.target) == null) {
                    continue;
                }
                int targetClass = initMembers.contains(e.target) ? INIT_CLASS : classOf.get(e.target);
                if (!classToStateName.containsKey(targetClass)) {
                    classToStateName.put(targetClass, "state" + nextStateNumber);
                    nextStateNumber++;
                    queue.add(targetClass);
                }
            }
        }
        return classToStateName;
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
