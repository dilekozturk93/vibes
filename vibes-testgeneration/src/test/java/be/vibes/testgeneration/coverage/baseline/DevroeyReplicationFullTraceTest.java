package be.vibes.testgeneration.coverage.baseline;

import be.vibes.dsl.io.Xml;
import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.FExpression;
import be.vibes.solver.ConstraintIdentifier;
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
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

/**
 * Full instrumented re-run of the BnB algorithm on SVM, logging every
 * pop, fork, score, and toVisit transition. Goal: prove or refute the
 * hypothesis "the free walk is enqueued but never popped because toVisit
 * empties before its turn."
 *
 * <p>This is an instrumented copy of AllStatesGenerator's main loop, NOT
 * a replacement. The production generator is unmodified.
 */
public class DevroeyReplicationFullTraceTest {

    private static final String CASESTUDIES_ROOT =
            "/Users/dilekozturk/git/vibes-casestudies/sodavendingmachine";

    @Test
    public void fullBnbTrace_svm() throws Exception {
        FeaturedTransitionSystem fts = Xml.loadFeaturedTransitionSystem(
                new java.io.FileInputStream(new File(CASESTUDIES_ROOT, "svm.fts")));
        Sat4JSolverFacade solver = loadCasestudiesSvmSolver();

        State initial = fts.getInitialState();
        Set<State> allStates = new HashSet<>();
        Iterator<State> sIt = fts.states();
        while (sIt.hasNext()) allStates.add(sIt.next());
        Set<State> toVisit = new HashSet<>(allStates);
        toVisit.remove(initial);

        WarshallAccessibility accessibility = WarshallAccessibility.forSystem(fts);
        accessibility.iterate(5);

        // Max-heap by score (descending).
        PriorityQueue<BaselineWalk> queue =
                new PriorityQueue<>(Comparator.comparingInt(BaselineWalk::getScore).reversed());

        int popCount = 0;
        int acceptedCount = 0;
        // Seed.
        System.out.println("=== Full BnB trace on SVM (casestudies) ===");
        System.out.println("toVisit start: " + names(toVisit));
        System.out.println();
        seed(fts, initial, accessibility, toVisit, queue, solver);
        System.out.println("After seed, queue size = " + queue.size() + ", contents (by score):");
        for (BaselineWalk w : sortedSnapshot(queue)) {
            System.out.println("  score=" + w.getScore() + "  walk=" + walkStr(w));
        }
        System.out.println();

        // Main loop.
        while (!toVisit.isEmpty() && !queue.isEmpty()) {
            BaselineWalk best = queue.poll();
            popCount++;
            System.out.println("[pop #" + popCount + "] score=" + best.getScore()
                    + "  walk=" + walkStr(best)
                    + "  toVisit=" + names(toVisit));
            acceptedCount += extend(fts, initial, accessibility, toVisit, queue, solver, best);
        }

        System.out.println();
        System.out.println("=== END ===");
        System.out.println("Total pops:      " + popCount);
        System.out.println("Test cases accepted: " + acceptedCount);
        System.out.println("toVisit at exit: " + names(toVisit));
        System.out.println("Queue remaining: " + queue.size() + " walks (never popped)");
        for (BaselineWalk leftover : queue) {
            System.out.println("  leftover  score=" + leftover.getScore()
                    + "  walk=" + walkStr(leftover));
        }
    }

    /**
     * Returns 1 if the extension produced an accepted test case, 0 otherwise.
     */
    private int extend(FeaturedTransitionSystem fts, State initial,
                       WarshallAccessibility accessibility, Set<State> toVisit,
                       PriorityQueue<BaselineWalk> queue, Sat4JSolverFacade solver,
                       BaselineWalk parent) {
        int accepted = 0;
        Iterator<Transition> outs = fts.getOutgoing(parent.getLastState());
        while (outs.hasNext()) {
            Transition t = outs.next();
            BaselineWalk fork = parent.copy();
            fork.append(t, fts);
            boolean valid = isValid(fork, solver);
            if (t.getTarget().equals(initial)) {
                if (!valid) {
                    System.out.println("    fork(" + t.getAction().getName() + " → INIT) "
                            + "REJECT closed-but-unsat");
                    continue;
                }
                Set<State> newlyVisited = new HashSet<>(fork.getVisitedStates());
                newlyVisited.retainAll(toVisit);
                if (newlyVisited.isEmpty()) {
                    System.out.println("    fork(" + t.getAction().getName() + " → INIT) "
                            + "REJECT no-progress");
                    continue;
                }
                System.out.println("    fork(" + t.getAction().getName() + " → INIT) "
                        + "ACCEPT walk=" + walkStr(fork)
                        + "  removes=" + names(newlyVisited));
                toVisit.removeAll(newlyVisited);
                accepted++;
            } else {
                if (!valid) {
                    System.out.println("    fork(" + t.getAction().getName() + " → "
                            + t.getTarget().getName() + ") REJECT unsat");
                    continue;
                }
                Set<State> uncoveredAfterWalk = new HashSet<>(toVisit);
                uncoveredAfterWalk.removeAll(fork.getVisitedStates());
                int reachable = accessibility.countAccessible(
                        fork.getLastState(), uncoveredAfterWalk);
                int alreadyCovered = 0;
                for (State s : fork.getVisitedStates()) {
                    if (toVisit.contains(s)) alreadyCovered++;
                }
                int score = reachable + alreadyCovered;
                fork.setScore(score);
                queue.add(fork);
                System.out.println("    fork(" + t.getAction().getName() + " → "
                        + t.getTarget().getName() + ") ENQUEUE score=" + score
                        + "  walk=" + walkStr(fork));
            }
        }
        return accepted;
    }

    private void seed(FeaturedTransitionSystem fts, State initial,
                      WarshallAccessibility accessibility, Set<State> toVisit,
                      PriorityQueue<BaselineWalk> queue, Sat4JSolverFacade solver) {
        BaselineWalk start = new BaselineWalk(initial);
        extend(fts, initial, accessibility, toVisit, queue, solver, start);
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

    private static String walkStr(BaselineWalk w) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Transition t : w.getTransitions()) {
            if (!first) sb.append(",");
            sb.append(t.getAction().getName());
            first = false;
        }
        return sb.append("]").toString();
    }

    private static String names(Set<State> states) {
        TreeSet<String> sorted = new TreeSet<>();
        for (State s : states) sorted.add(s.getName());
        return sorted.toString();
    }

    private static java.util.List<BaselineWalk> sortedSnapshot(PriorityQueue<BaselineWalk> q) {
        java.util.List<BaselineWalk> sorted = new java.util.ArrayList<>(q);
        sorted.sort(Comparator.comparingInt(BaselineWalk::getScore).reversed());
        return sorted;
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
