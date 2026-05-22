package be.vibes.testgeneration.experiment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal FeatureIDE feature-model XML to DIMACS exporter.
 *
 * <p>Handles the subset of FeatureIDE schema actually used by the user's
 * bundled SPLs:
 * <ul>
 *   <li>{@code <and>} groups — each child is mandatory or optional per its
 *       own attribute.</li>
 *   <li>{@code <or>} groups — parent selected implies at least one child
 *       selected.</li>
 *   <li>{@code <alt>} groups — XOR; parent selected implies exactly one
 *       child selected.</li>
 *   <li>{@code mandatory="true"} on children of an {@code <and>} parent.</li>
 *   <li>{@code abstract="true"} flag — abstract features participate in
 *       the SAT encoding as boolean variables just like concrete features
 *       (matches the user's existing SVM / Tesla DIMACS files).</li>
 *   <li>{@code <constraints>}: {@code <imp>} (implication), {@code <eq>}
 *       (biconditional), {@code <not>}, {@code <var>}, {@code <disj>},
 *       {@code <conj>}.</li>
 * </ul>
 *
 * <p>Outputs two files in the directory of the input model:
 * {@code <name>.dimacs} (CNF) and {@code <name>_dimacsmapping.txt}
 * (space-separated {@code id name} per line, matching VIBeS'
 * {@code DimacsModel.createFromTvlParserGeneratedFiles} format).
 *
 * <p>CLI: {@code java FeatureIdeToDimacsExporter <model.xml> <out_dimacs> <out_mapping>}.
 */
public final class FeatureIdeToDimacsExporter {

    private final Map<String, Integer> featureToId = new LinkedHashMap<>();
    private final List<int[]> clauses = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: java FeatureIdeToDimacsExporter <model.xml> <out.dimacs> <out_mapping.txt>");
            System.exit(1);
        }
        File input = new File(args[0]);
        File dimacsOut = new File(args[1]);
        File mappingOut = new File(args[2]);

        FeatureIdeToDimacsExporter exporter = new FeatureIdeToDimacsExporter();
        exporter.exportTo(input, dimacsOut, mappingOut);
        System.out.println("Wrote " + dimacsOut + " (" + exporter.featureToId.size()
                + " features, " + exporter.clauses.size() + " clauses) and " + mappingOut);
    }

    public void exportTo(File input, File dimacsOut, File mappingOut) throws Exception {
        Document doc = parse(input);
        Element struct = (Element) doc.getElementsByTagName("struct").item(0);
        Element root = firstFeatureElement(struct);
        if (root == null) {
            throw new IOException("No root feature element under <struct>");
        }
        // Assign sequential IDs by DFS so the root gets id 1, matching the
        // user's existing DIMACS files.
        assignIds(root);
        // Root is mandatory: always selected.
        addClause(idOf(root));
        // Walk the tree and emit structural clauses.
        encodeStructure(root, null);
        // "At-least-one optional concrete feature" clause — ports the
        // user's ESG-Fx-side SATSolverGenerationFromFeatureModel
        // .addOptionalConcreteFeatureClauses logic (their thesis +
        // published-paper convention). Excludes the root-only trivial
        // product without spuriously rejecting any product that selects
        // at least one optional concrete feature. See the method's
        // JavaDoc for the precise set membership rules.
        addAtLeastOneOptionalConcrete(root);
        // Cross-tree constraints.
        NodeList constraintRules = doc.getElementsByTagName("rule");
        for (int i = 0; i < constraintRules.getLength(); i++) {
            Element rule = (Element) constraintRules.item(i);
            encodeConstraint(rule);
        }
        write(dimacsOut, mappingOut);
    }

    /**
     * Emits a single clause {@code f_1 ∨ f_2 ∨ … ∨ f_k} where the
     * features are exactly the user's ESG-Fx
     * {@code optionalConcreteFeatures} set (from
     * {@code SATSolverGenerationFromFeatureModel.addFeatureClauses /
     * addMandatoryORFeatureClauses / addOptionalORFeatureClauses}).
     *
     * <p>Set membership rules (matches the ESG-Fx source line-by-line):
     * <ul>
     *   <li>OR-group node ({@code <or>}, mandatory OR optional):
     *       add every concrete (non-abstract) child;</li>
     *   <li>XOR-group node ({@code <alt>}, mandatory OR optional):
     *       add NOTHING — the alt-group's exactly-one constraint is
     *       handled separately;</li>
     *   <li>AND-group node ({@code <and>}) — including the root: for
     *       each direct {@code <feature>} child that is concrete AND
     *       optional, add it. Recurse into all children of any tag.
     *       Mandatory concrete leaves are excluded because they're
     *       forced by a unit clause upstream;</li>
     *   <li>Root, abstract features, and any non-leaf feature
     *       elements are NEVER added.</li>
     * </ul>
     *
     * <p>Effect on the four MVP SPLs (verified against
     * the published reference table):
     * <ul>
     *   <li>SVM → {s, t, f, c} (b is mandatory OR-group → s, t added; f, c
     *       are optional concrete leaves of svm AND-group)</li>
     *   <li>eMail → {ad, au, f, en, s} (all optional concrete leaves under
     *       AND-parents; no OR/XOR groups)</li>
     *   <li>Elevator → {Alarm, Intercom, ManualDoorControl,
     *       FirefighterService, ExecutiveFloor} (EmergencyFeatures OR-group
     *       → Alarm, Intercom added; AccessControl XOR-group → CardReader,
     *       MobileKey, PinPad NOT added; ControlButtons mandatory → not
     *       added)</li>
     *   <li>BankAccountv2 → {up, t, cd, cw, i, ie, dl} (cancellation OR-group
     *       → cd, cw added; currency / extraMoney XOR-groups → tl/eu/us,
     *       c/o NOT added; d, w mandatory → not added)</li>
     * </ul>
     */
    private void addAtLeastOneOptionalConcrete(Element root) {
        java.util.LinkedHashSet<Integer> ids = new java.util.LinkedHashSet<>();
        collectOptionalConcreteIds(root, ids);
        if (ids.isEmpty()) {
            return;
        }
        int[] clause = new int[ids.size()];
        int i = 0;
        for (int id : ids) {
            clause[i++] = id;
        }
        addClause(clause);
    }

    /**
     * Walks the feature tree implementing the ESG-Fx
     * {@code optionalConcreteFeatures} set semantics.
     */
    private void collectOptionalConcreteIds(Element node, java.util.LinkedHashSet<Integer> acc) {
        String tag = node.getTagName();

        if (tag.equals("or")) {
            // OR-group: add every concrete child, then recurse into all.
            for (Element child : featureChildren(node)) {
                if (child.getTagName().equals("feature")
                        && !"true".equals(child.getAttribute("abstract"))) {
                    acc.add(idOf(child));
                }
                collectOptionalConcreteIds(child, acc);
            }
        } else if (tag.equals("alt")) {
            // XOR-group: do NOT add children. Only recurse for their subtrees
            // (in case they have nested OR/AND/etc., which is rare but
            // syntactically valid in FeatureIDE).
            for (Element child : featureChildren(node)) {
                collectOptionalConcreteIds(child, acc);
            }
        } else if (tag.equals("and")) {
            // AND-group: for each direct <feature> child that is concrete
            // optional, add it. Recurse into all children regardless of tag.
            for (Element child : featureChildren(node)) {
                if (child.getTagName().equals("feature")
                        && !"true".equals(child.getAttribute("abstract"))
                        && !"true".equals(child.getAttribute("mandatory"))) {
                    acc.add(idOf(child));
                }
                collectOptionalConcreteIds(child, acc);
            }
        }
        // tag.equals("feature"): leaf, nothing to do.
    }

    private static Document parse(File f) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder b = dbf.newDocumentBuilder();
        Document d = b.parse(f);
        d.getDocumentElement().normalize();
        return d;
    }

    private static Element firstFeatureElement(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && isFeatureElement((Element) n)) {
                return (Element) n;
            }
        }
        return null;
    }

    private static boolean isFeatureElement(Element e) {
        String tag = e.getTagName();
        return tag.equals("feature") || tag.equals("and") || tag.equals("or") || tag.equals("alt");
    }

    private void assignIds(Element node) {
        String name = node.getAttribute("name");
        if (!featureToId.containsKey(name)) {
            featureToId.put(name, featureToId.size() + 1);
        }
        for (Element child : featureChildren(node)) {
            assignIds(child);
        }
    }

    private List<Element> featureChildren(Element node) {
        List<Element> kids = new ArrayList<>();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && isFeatureElement((Element) n)) {
                kids.add((Element) n);
            }
        }
        return kids;
    }

    /**
     * Walks the feature tree emitting clauses per group semantics:
     * - every non-root feature implies its parent (child -> parent);
     * - every mandatory child of an {@code <and>} parent forces selection
     *   when the parent is selected (parent -> child);
     * - {@code <or>} parent -> at least one child;
     * - {@code <alt>} parent -> exactly one child (at-least + pairwise mutex).
     */
    private void encodeStructure(Element node, Element parent) {
        int nodeId = idOf(node);
        if (parent != null) {
            int parentId = idOf(parent);
            addClause(-nodeId, parentId); // child -> parent
            if ("true".equals(node.getAttribute("mandatory"))) {
                addClause(-parentId, nodeId); // parent -> child (mandatory)
            }
        }
        List<Element> children = featureChildren(node);
        if (children.isEmpty()) {
            return;
        }
        String tag = node.getTagName();

        // For ANY abstract feature with concrete/abstract children, add
        // "abstract -> at least one child". An abstract feature has no
        // semantic meaning of its own; it is only "selected" when at
        // least one descendant is. This is exactly the FeatureIDE
        // semantics that the user's prior published study relies on.
        // The constraint is redundant for <or> / <alt> (which already
        // require at least one child if selected) but matters for
        // abstract <and> features whose children are all optional —
        // without it, an abstract <and> can be selected with all its
        // children deselected, which gives spurious extra SAT solutions.
        if ("true".equals(node.getAttribute("abstract"))) {
            int[] abstractAtLeastOne = new int[children.size() + 1];
            abstractAtLeastOne[0] = -nodeId;
            for (int i = 0; i < children.size(); i++) {
                abstractAtLeastOne[i + 1] = idOf(children.get(i));
            }
            addClause(abstractAtLeastOne);
        }

        if (tag.equals("or")) {
            int[] orClause = new int[children.size() + 1];
            orClause[0] = -nodeId;
            for (int i = 0; i < children.size(); i++) {
                orClause[i + 1] = idOf(children.get(i));
            }
            addClause(orClause);
        } else if (tag.equals("alt")) {
            int[] atLeastOne = new int[children.size() + 1];
            atLeastOne[0] = -nodeId;
            for (int i = 0; i < children.size(); i++) {
                atLeastOne[i + 1] = idOf(children.get(i));
            }
            addClause(atLeastOne);
            // pairwise mutual exclusion among children
            for (int i = 0; i < children.size(); i++) {
                for (int j = i + 1; j < children.size(); j++) {
                    addClause(-idOf(children.get(i)), -idOf(children.get(j)));
                }
            }
        }
        // <and>: per-child mandatory clause is already handled above; nothing
        // extra at the group level.
        for (Element c : children) {
            encodeStructure(c, node);
        }
    }

    /** Encodes a single {@code <rule>} cross-tree constraint as CNF clauses. */
    private void encodeConstraint(Element rule) {
        Element expr = firstChildElement(rule);
        Formula nnf = toNnf(expr, false);
        List<List<Integer>> cnf = toCnf(nnf);
        for (List<Integer> clause : cnf) {
            int[] arr = new int[clause.size()];
            for (int i = 0; i < clause.size(); i++) {
                arr[i] = clause.get(i);
            }
            addClause(arr);
        }
    }

    /**
     * A small AST for constraint formulae in negation normal form. Only
     * literals carry negation; And and Or nodes hold positive children.
     */
    private abstract static class Formula {
    }

    private static final class Lit extends Formula {
        final int signedId;
        Lit(int signedId) { this.signedId = signedId; }
    }

    private static final class And extends Formula {
        final List<Formula> conjuncts;
        And(List<Formula> conjuncts) { this.conjuncts = conjuncts; }
    }

    private static final class Or extends Formula {
        final List<Formula> disjuncts;
        Or(List<Formula> disjuncts) { this.disjuncts = disjuncts; }
    }

    /**
     * Recursively translates a constraint sub-formula to negation normal
     * form. The {@code negated} flag carries the parity inherited from
     * surrounding {@code <not>} / {@code <imp>} / {@code <eq>} nodes; we
     * use De Morgan's laws to push negation down to literals.
     */
    private Formula toNnf(Element node, boolean negated) {
        String tag = node.getTagName();
        List<Element> children = elementChildren(node);
        switch (tag) {
            case "var": {
                int id = idOfName(node.getTextContent().trim());
                return new Lit(negated ? -id : id);
            }
            case "not":
                return toNnf(children.get(0), !negated);
            case "conj": {
                List<Formula> parts = new ArrayList<>();
                for (Element c : children) {
                    parts.add(toNnf(c, negated));
                }
                // (A ∧ B) with negation outside becomes (¬A ∨ ¬B): the negated
                // children are already produced above, just choose container.
                return negated ? new Or(parts) : new And(parts);
            }
            case "disj": {
                List<Formula> parts = new ArrayList<>();
                for (Element c : children) {
                    parts.add(toNnf(c, negated));
                }
                return negated ? new And(parts) : new Or(parts);
            }
            case "imp": {
                // A -> B  ≡  ¬A ∨ B; negated form ≡ A ∧ ¬B.
                Formula left = toNnf(children.get(0), !negated);
                Formula right = toNnf(children.get(1), negated);
                List<Formula> parts = new ArrayList<>();
                parts.add(left);
                parts.add(right);
                return negated ? new And(parts) : new Or(parts);
            }
            case "eq": {
                // A <-> B  ≡  (A ∧ B) ∨ (¬A ∧ ¬B); negated ≡  (A ∧ ¬B) ∨ (¬A ∧ B).
                Element a = children.get(0);
                Element b = children.get(1);
                Formula aPos = toNnf(a, false);
                Formula aNeg = toNnf(a, true);
                Formula bPos = toNnf(b, false);
                Formula bNeg = toNnf(b, true);
                List<Formula> branch1 = new ArrayList<>();
                List<Formula> branch2 = new ArrayList<>();
                if (!negated) {
                    branch1.add(aPos); branch1.add(bPos);
                    branch2.add(aNeg); branch2.add(bNeg);
                } else {
                    branch1.add(aPos); branch1.add(bNeg);
                    branch2.add(aNeg); branch2.add(bPos);
                }
                List<Formula> top = new ArrayList<>();
                top.add(new And(branch1));
                top.add(new And(branch2));
                return new Or(top);
            }
            default:
                throw new IllegalArgumentException("Unsupported constraint formula tag: " + tag);
        }
    }

    /**
     * Converts a NNF formula to CNF clauses via standard distribution.
     * Each returned inner list represents one clause; each integer in a
     * clause is a signed feature id.
     */
    private List<List<Integer>> toCnf(Formula f) {
        if (f instanceof Lit) {
            List<List<Integer>> cnf = new ArrayList<>();
            List<Integer> clause = new ArrayList<>();
            clause.add(((Lit) f).signedId);
            cnf.add(clause);
            return cnf;
        }
        if (f instanceof And) {
            List<List<Integer>> cnf = new ArrayList<>();
            for (Formula c : ((And) f).conjuncts) {
                cnf.addAll(toCnf(c));
            }
            return cnf;
        }
        if (f instanceof Or) {
            // CNF(A ∨ B) — distribute: every clause of A combined with every
            // clause of B yields one clause of the result.
            List<Formula> disjuncts = ((Or) f).disjuncts;
            List<List<Integer>> cnf = toCnf(disjuncts.get(0));
            for (int i = 1; i < disjuncts.size(); i++) {
                List<List<Integer>> other = toCnf(disjuncts.get(i));
                List<List<Integer>> combined = new ArrayList<>();
                for (List<Integer> c1 : cnf) {
                    for (List<Integer> c2 : other) {
                        List<Integer> merged = new ArrayList<>(c1);
                        merged.addAll(c2);
                        combined.add(merged);
                    }
                }
                cnf = combined;
            }
            return cnf;
        }
        throw new IllegalStateException("Unknown formula type: " + f.getClass());
    }

    private static Element firstChildElement(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return (Element) children.item(i);
            }
        }
        return null;
    }

    private static List<Element> elementChildren(Element parent) {
        List<Element> out = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                out.add((Element) children.item(i));
            }
        }
        return out;
    }

    private int idOf(Element node) {
        return idOfName(node.getAttribute("name"));
    }

    private int idOfName(String name) {
        Integer id = featureToId.get(name);
        if (id == null) {
            throw new IllegalStateException("Unknown feature in constraint: '" + name
                    + "' (known features: " + featureToId.keySet() + ")");
        }
        return id;
    }

    private void addClause(int... lits) {
        clauses.add(lits);
    }

    private void write(File dimacsOut, File mappingOut) throws IOException {
        dimacsOut.getParentFile().mkdirs();
        try (PrintWriter dimacs = new PrintWriter(dimacsOut);
             PrintWriter mapping = new PrintWriter(mappingOut)) {
            dimacs.println("c DIMACS Generated by FeatureIdeToDimacsExporter");
            dimacs.println("p cnf " + featureToId.size() + " " + clauses.size());
            for (int[] clause : clauses) {
                StringBuilder sb = new StringBuilder();
                for (int lit : clause) {
                    sb.append(lit).append(' ');
                }
                sb.append('0');
                dimacs.println(sb.toString());
            }
            for (Map.Entry<String, Integer> e : featureToId.entrySet()) {
                mapping.println(e.getValue() + " " + e.getKey());
            }
        }
    }
}
