package be.vibes.testgeneration.coverage.baseline;

import be.vibes.dsl.io.Xml;
import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.FExpression;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.solver.ConstraintIdentifier;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.Transition;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Step 3 of the Devroey-replication audit: prove that the BnB forks BOTH
 * outgoing transitions of the initial state (pay and free in SVM) as
 * candidates, with valid scores, and that the free candidate is in the
 * queue. Trace what happens to it: is it ever selected before toVisit
 * empties, or does it sit at the bottom of the priority queue
 * indefinitely?
 *
 * <p>Reimplements the seed-and-score logic of AllStatesGenerator with
 * verbose instrumentation, on the exact same casestudies SVM model.
 */
public class DevroeyReplicationBranchingTest {

    private static final String CASESTUDIES_ROOT =
            "/Users/dilekozturk/git/vibes-casestudies/sodavendingmachine";

    @Test
    public void traceInitialBranching_svm() throws Exception {
        FeaturedTransitionSystem fts = Xml.loadFeaturedTransitionSystem(
                new java.io.FileInputStream(new File(CASESTUDIES_ROOT, "svm.fts")));
        Sat4JSolverFacade solver = loadCasestudiesSvmSolver();

        State initial = fts.getInitialState();
        System.out.println("=== Branching trace: SVM initial state outgoings ===");
        System.out.println("Initial state: " + initial.getName());
        System.out.println();

        // Build the same accessibility + toVisit setup as AllStatesGenerator.
        WarshallAccessibility accessibility = WarshallAccessibility.forSystem(fts);
        accessibility.iterate(5);

        Set<State> allStates = new HashSet<>();
        Iterator<State> sIt = fts.states();
        while (sIt.hasNext()) allStates.add(sIt.next());
        Set<State> toVisit = new HashSet<>(allStates);
        toVisit.remove(initial);
        System.out.println("toVisit (initial): " + toVisit.size() + " states "
                + stateNames(toVisit));
        System.out.println();

        // List the outgoings of initial as the generator sees them.
        System.out.println("Outgoings of " + initial.getName() + " (per ts.getOutgoing):");
        int idx = 0;
        Iterator<Transition> outs = fts.getOutgoing(initial);
        while (outs.hasNext()) {
            Transition t = outs.next();
            System.out.println("  [" + idx + "] " + t.getSource().getName()
                    + " -" + t.getAction().getName() + "-> " + t.getTarget().getName()
                    + "   fexpr=" + fts.getFExpression(t));
            idx++;
        }
        System.out.println();

        // Now simulate the per-outgoing fork: for EACH outgoing, build a
        // BaselineWalk, check satisfiability, compute score. Report which
        // would be enqueued and at what score.
        System.out.println("Per-outgoing fork analysis:");
        Iterator<Transition> outs2 = fts.getOutgoing(initial);
        while (outs2.hasNext()) {
            Transition t = outs2.next();
            BaselineWalk fork = new BaselineWalk(initial);
            fork.append(t, fts);
            boolean valid = isValid(fork, solver);
            Set<State> uncoveredAfterWalk = new HashSet<>(toVisit);
            uncoveredAfterWalk.removeAll(fork.getVisitedStates());
            int reachable = accessibility.countAccessible(
                    fork.getLastState(), uncoveredAfterWalk);
            int alreadyCovered = 0;
            for (State s : fork.getVisitedStates()) {
                if (toVisit.contains(s)) alreadyCovered++;
            }
            int score = reachable + alreadyCovered;
            System.out.println("  fork(" + t.getAction().getName() + " → "
                    + t.getTarget().getName() + "):");
            System.out.println("    accumulated fexpr: " + fork.getAccumulatedFExpression());
            System.out.println("    SAT-valid:         " + valid);
            System.out.println("    visited:           " + stateNames(fork.getVisitedStates()));
            System.out.println("    last state:        " + fork.getLastState().getName());
            System.out.println("    uncoveredAfterWalk: " + stateNames(uncoveredAfterWalk));
            System.out.println("    accessibility.countAccessible(" + fork.getLastState().getName()
                    + ", uncoveredAfterWalk) = " + reachable);
            System.out.println("    alreadyCovered (walk ∩ toVisit) = " + alreadyCovered);
            System.out.println("    SCORE = " + score
                    + "   →  " + (valid ? "ENQUEUED" : "REJECTED (unsat)"));
            System.out.println();
        }

        // Bonus: trace what happens FROM state2 (after pay) and FROM state3 (after free).
        System.out.println("=== Deeper trace: extensions after 'pay' (state2) and 'free' (state3) ===");
        System.out.println();

        State state2 = fts.getState("state2");
        State state3 = fts.getState("state3");

        System.out.println("Outgoings of state2 (after pay):");
        Iterator<Transition> outs2state = fts.getOutgoing(state2);
        while (outs2state.hasNext()) {
            Transition t = outs2state.next();
            System.out.println("  -" + t.getAction().getName() + "-> "
                    + t.getTarget().getName() + "   fexpr=" + fts.getFExpression(t));
        }

        System.out.println();
        System.out.println("Outgoings of state3 (after free):");
        Iterator<Transition> outs3 = fts.getOutgoing(state3);
        while (outs3.hasNext()) {
            Transition t = outs3.next();
            System.out.println("  -" + t.getAction().getName() + "-> "
                    + t.getTarget().getName() + "   fexpr=" + fts.getFExpression(t));
        }
    }

    private static boolean isValid(BaselineWalk walk, Sat4JSolverFacade solver) {
        if (solver == null) return true;
        FExpression fexpr = walk.getAccumulatedFExpression();
        if (fexpr == null || fexpr.isTrue()) return true;
        ConstraintIdentifier id = null;
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

    private static String stateNames(Set<State> states) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (State s : states) {
            if (!first) sb.append(", ");
            sb.append(s.getName());
            first = false;
        }
        return sb.append("]").toString();
    }

    private Sat4JSolverFacade loadCasestudiesSvmSolver() throws Exception {
        Path combined = new File(CASESTUDIES_ROOT, "svm.splot.dimacs").toPath();
        Path tmpMapping = Files.createTempFile("svm-mapping-", ".txt");
        Path tmpDimacs = Files.createTempFile("svm-dimacs-", ".cnf");
        try (PrintWriter mappingOut = new PrintWriter(new FileWriter(tmpMapping.toFile()));
             PrintWriter dimacsOut = new PrintWriter(new FileWriter(tmpDimacs.toFile()))) {
            for (String line : Files.readAllLines(combined)) {
                if (line.startsWith("c ")) {
                    String[] parts = line.substring(2).trim().split("\\s+", 2);
                    if (parts.length == 2) {
                        mappingOut.println(parts[0] + " " + parts[1]);
                    }
                } else {
                    dimacsOut.println(line);
                }
            }
        }
        DimacsModel model = DimacsModel.createFromTvlParserGeneratedFiles(
                tmpMapping.toFile(), tmpDimacs.toFile());
        return new Sat4JSolverFacade(model);
    }
}
