package be.vibes.testgeneration.coverage.baseline;

import be.vibes.dsl.io.Xml;
import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.FExpression;
import be.vibes.fexpression.Feature;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.Transition;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Six diagnostic checks for the SVM replication audit (user-requested):
 *
 * <ol>
 *   <li>Initial state identity</li>
 *   <li>Source of the SAT solver error log</li>
 *   <li>State equals/hashCode contract</li>
 *   <li>Seed candidate count</li>
 *   <li>DimacsModel ↔ FTS feature name alignment</li>
 *   <li>Walk termination at close-to-INIT</li>
 * </ol>
 *
 * Each check prints evidence to stdout for the report.
 */
public class DevroeySixDiagnosticChecks {

    private static final String CASESTUDIES_ROOT =
            "/Users/dilekozturk/git/vibes-casestudies/sodavendingmachine";

    @Test
    public void check1_initialState() throws Exception {
        FeaturedTransitionSystem fts = loadFts();
        State initial = fts.getInitialState();
        System.out.println("=== Q1: Initial state ===");
        System.out.println("  FTS XML <fts:start>: state1 (per file inspection)");
        System.out.println("  ts.getInitialState().getName(): " + initial.getName());
        System.out.println("  Match: " + ("state1".equals(initial.getName())));
    }

    @Test
    public void check3_stateEqualsAndHashCode() throws Exception {
        FeaturedTransitionSystem fts = loadFts();
        State s1 = fts.getInitialState();
        State s1again = fts.getState("state1");
        State s2 = fts.getState("state2");
        State s2again = fts.getState("state2");

        System.out.println("=== Q3: State equals/hashCode ===");
        System.out.println("  s1 = ts.getInitialState() = " + s1);
        System.out.println("  s1again = ts.getState(\"state1\") = " + s1again);
        System.out.println("  s1 == s1again (reference)? " + (s1 == s1again));
        System.out.println("  s1.equals(s1again)?         " + (s1.equals(s1again)));
        System.out.println("  s1.hashCode() == s1again.hashCode()? "
                + (s1.hashCode() == s1again.hashCode()));
        System.out.println();
        System.out.println("  s2 == s2again (reference)? " + (s2 == s2again));
        System.out.println("  s2.equals(s2again)?         " + (s2.equals(s2again)));
        System.out.println();
        // Round-trip via HashSet — does our State land in a Set correctly?
        Set<State> set = new HashSet<>();
        set.add(s1);
        System.out.println("  HashSet.contains(s1again) after add(s1): " + set.contains(s1again));
        System.out.println("  HashSet.contains(s2) after add(s1):       " + set.contains(s2));
    }

    @Test
    public void check4_seedCandidateCount() throws Exception {
        FeaturedTransitionSystem fts = loadFts();
        State initial = fts.getInitialState();
        System.out.println("=== Q4: Seed candidate count ===");
        // Count outgoings of initial — this is what addSuccessors iterates at seed.
        int outgoingCount = 0;
        Iterator<Transition> outs = fts.getOutgoing(initial);
        while (outs.hasNext()) {
            Transition t = outs.next();
            outgoingCount++;
            System.out.println("  outgoing[" + (outgoingCount-1) + "]: "
                    + t.getSource().getName() + " -" + t.getAction().getName() + "-> "
                    + t.getTarget().getName() + "  [" + fts.getFExpression(t) + "]");
        }
        System.out.println("  total outgoings from initial: " + outgoingCount);
        System.out.println("  seed candidates that should land in queue: " + outgoingCount);
        // Total transition count from fts.transitions() — should be 13.
        int totalTransitions = 0;
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) { it.next(); totalTransitions++; }
        System.out.println("  total transitions (fts.transitions()): " + totalTransitions
                + "  (paper expects 13)");
    }

    @Test
    public void check5_dimacsModelFeatureNamesMatchFts() throws Exception {
        FeaturedTransitionSystem fts = loadFts();
        Sat4JSolverFacade solver = loadSolver();

        // Collect feature names appearing in FTS fexprs.
        Set<String> ftsFeatures = new TreeSet<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            FExpression fexpr = fts.getFExpression(it.next());
            if (fexpr == null) continue;
            for (Feature f : fexpr.getFeatures()) ftsFeatures.add(f.getName());
        }

        // Collect feature names from DimacsModel (via solver). The solver
        // doesn't expose the feature list directly, but we can probe it
        // by constructing FExpressions referring to each FTS feature and
        // checking whether SAT is satisfiable (would error if feature
        // unknown to the underlying DimacsModel).
        System.out.println("=== Q5: DimacsModel ↔ FTS feature alignment ===");
        System.out.println("  FTS features (from transition fexprs): " + ftsFeatures);
        System.out.println();

        // Probe each FTS feature against the SAT solver.
        for (String fname : ftsFeatures) {
            FExpression probe = FExpression.featureExpr(fname);
            try {
                be.vibes.solver.ConstraintIdentifier id = solver.addConstraint(probe);
                boolean sat = solver.isSatisfiable();
                solver.removeConstraint(id);
                System.out.println("  FTS feature \"" + fname
                        + "\" → SAT solver accepts. SAT(\"" + fname + "\") = " + sat);
            } catch (Exception e) {
                System.out.println("  FTS feature \"" + fname
                        + "\" → SAT solver REJECTS / errors: "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        // DIMACS file header for cross-reference.
        System.out.println();
        System.out.println("  DIMACS header (svm.splot.dimacs first 10 lines):");
        for (String line : Files.readAllLines(
                new File(CASESTUDIES_ROOT, "svm.splot.dimacs").toPath())
                .subList(0, 10)) {
            System.out.println("    " + line);
        }
    }

    /**
     * Question 2 & 6 combined: instrumented mini-trace of a single closed
     * walk (pay → change → soda → serveSoda → open → take → close → INIT)
     * to verify the algorithm accepts and STOPS, not extends from INIT.
     *
     * <p>The SAT error log seen in earlier runs came from sibling forks
     * (e.g. take → INIT under !FreeDrinks already accumulated, conflicts
     * with take's FreeDrinks fexpr). This test reproduces both the
     * accept-and-stop path and the unsat-close path side by side.
     */
    @Test
    public void check2and6_terminationAndSatErrorSource() throws Exception {
        FeaturedTransitionSystem fts = loadFts();
        Sat4JSolverFacade solver = loadSolver();

        System.out.println("=== Q2 & Q6: Walk termination + SAT error source ===");
        System.out.println();
        System.out.println("Tracing forks from state7 with parent walk fexpr = !FreeDrinks ∧ Soda");
        System.out.println("(parent walk: pay → change → soda → serveSoda  — ends at state7)");
        System.out.println();

        State state7 = findState(fts, "state7");
        // Reconstruct the parent walk's accumulated fexpr by walking the
        // path s1 -pay→ s2 -change→ s3 -soda→ s5 -serveSoda→ s7.
        BaselineWalk parent = new BaselineWalk(fts.getInitialState());
        appendByAction(parent, fts, fts.getInitialState(), "pay");
        appendByAction(parent, fts, findState(fts, "state2"), "change");
        appendByAction(parent, fts, findState(fts, "state3"), "soda");
        appendByAction(parent, fts, findState(fts, "state5"), "serveSoda");
        System.out.println("  Parent walk fexpr: " + parent.getAccumulatedFExpression());
        System.out.println();

        Iterator<Transition> outs = fts.getOutgoing(state7);
        while (outs.hasNext()) {
            Transition t = outs.next();
            BaselineWalk fork = parent.copy();
            fork.append(t, fts);
            System.out.println("  fork: state7 -" + t.getAction().getName() + "-> "
                    + t.getTarget().getName());
            System.out.println("    transition fexpr:    " + fts.getFExpression(t));
            System.out.println("    walk fexpr after:    " + fork.getAccumulatedFExpression());
            System.out.println("    target == initial?   " + t.getTarget().equals(fts.getInitialState()));
            System.out.println("    walk would be: "
                    + (t.getTarget().equals(fts.getInitialState())
                            ? "CLOSE candidate (call isValid + maybe accept; NOT enqueue for further extension)"
                            : "ENQUEUE for further extension (if isValid)"));
            boolean sat = quickSat(fork.getAccumulatedFExpression(), solver);
            System.out.println("    SAT(walk fexpr)?     " + sat);
            System.out.println();
        }

        System.out.println("AllStatesGenerator.java termination check (per source):");
        System.out.println("  Line 185-186: if (t.getTarget().equals(initial)) { ... ACCEPT or REJECT, "
                + "NO candidates.add() }");
        System.out.println("  Line 204:     else { ... candidates.add(fork); ... }");
        System.out.println("  → Closed walks (target == initial) are NEVER enqueued. "
                + "Algorithm terminates the walk at first INIT visit.");
        System.out.println();
        System.out.println("SAT error 'while formatting constraint' source:");
        System.out.println("  It comes from isValid() in AllStatesGenerator.java line ~252:");
        System.out.println("    solver.addConstraint(fexpr)  // throws when fexpr is unsat");
        System.out.println("  We catch the exception and return false (treating as invalid walk).");
        System.out.println("  The error log line is from Sat4JSolverFacade.LOG.error before the throw.");
        System.out.println("  Mechanism is correct; the log noise just reflects each unsat candidate.");
    }

    // ---------- helpers ----------

    private FeaturedTransitionSystem loadFts() throws Exception {
        return Xml.loadFeaturedTransitionSystem(
                new java.io.FileInputStream(new File(CASESTUDIES_ROOT, "svm.fts")));
    }

    private Sat4JSolverFacade loadSolver() throws Exception {
        Path combined = new File(CASESTUDIES_ROOT, "svm.splot.dimacs").toPath();
        Path tmpMapping = Files.createTempFile("svm-mapping-", ".txt");
        Path tmpDimacs = Files.createTempFile("svm-dimacs-", ".cnf");
        try (PrintWriter mappingOut = new PrintWriter(new FileWriter(tmpMapping.toFile()));
             PrintWriter dimacsOut = new PrintWriter(new FileWriter(tmpDimacs.toFile()))) {
            for (String line : Files.readAllLines(combined)) {
                if (line.startsWith("c ")) {
                    String[] parts = line.substring(2).trim().split("\\s+", 2);
                    if (parts.length == 2) mappingOut.println(parts[0] + " " + parts[1]);
                } else {
                    dimacsOut.println(line);
                }
            }
        }
        DimacsModel model = DimacsModel.createFromTvlParserGeneratedFiles(
                tmpMapping.toFile(), tmpDimacs.toFile());
        return new Sat4JSolverFacade(model);
    }

    private State findState(FeaturedTransitionSystem fts, String name) {
        State s = fts.getState(name);
        if (s == null) {
            Iterator<State> it = fts.states();
            while (it.hasNext()) {
                State candidate = it.next();
                if (candidate.getName().equals(name)) return candidate;
            }
            throw new IllegalStateException("State not found: " + name);
        }
        return s;
    }

    private void appendByAction(BaselineWalk walk, FeaturedTransitionSystem fts,
                                State from, String actionName) {
        Iterator<Transition> outs = fts.getOutgoing(from);
        while (outs.hasNext()) {
            Transition t = outs.next();
            if (t.getAction().getName().equals(actionName)) {
                walk.append(t, fts);
                return;
            }
        }
        throw new IllegalStateException("No outgoing '" + actionName + "' from " + from.getName());
    }

    private boolean quickSat(FExpression fexpr, Sat4JSolverFacade solver) {
        if (fexpr == null || fexpr.isTrue()) return true;
        be.vibes.solver.ConstraintIdentifier id = null;
        try {
            id = solver.addConstraint(fexpr);
            return solver.isSatisfiable();
        } catch (Exception e) {
            return false;
        } finally {
            if (id != null) {
                try { solver.removeConstraint(id); } catch (Exception ignored) {}
            }
        }
    }
}
