package be.vibes.testgeneration.coverage.baseline;

import be.vibes.dsl.io.Xml;
import be.vibes.fexpression.DimacsModel;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotNull;

/**
 * Step 2 of Devroey 2014 replication: load the canonical SVM model from
 * the {@code xdevroey/vibes-casestudies} repository and run our ported
 * AllStatesGenerator on it. Compares the produced suite size to Devroey
 * 2014 Table 3's published value of ~5 test cases.
 *
 * <p>This test does NOT use the project's bundled SVM (which is derived
 * via MxeToFtsConverter from an ESG-Fx MXE file with different feature
 * naming conventions). It loads the FTS XML directly from
 * {@code /Users/dilekozturk/git/vibes-casestudies/sodavendingmachine/svm.fts}.
 *
 * <p>The casestudies' DIMACS file mixes the feature mapping (as
 * {@code c <id> <name>} comments) with the CNF body in a single file;
 * VIBeS' {@code DimacsModel.createFromTvlParserGeneratedFiles} expects
 * them split. We adapt at test time without modifying the casestudies repo.
 */
public class DevroeyReplicationSvmTest {

    private static final String CASESTUDIES_ROOT =
            "/Users/dilekozturk/git/vibes-casestudies/sodavendingmachine";

    @Test
    public void replicate_svm_modelIdentity() throws Exception {
        FeaturedTransitionSystem fts = loadCasestudiesSvm();
        // Paper Table 1 / fts-statistics.csv: 9 states, 13 transitions.
        // Action count: 12 unique (vibes-casestudies stats CSV counts
        // occurrences rather than unique-action; both readings give 12 distinct).
        int stateCount = countStates(fts);
        int transitionCount = countTransitions(fts);
        Set<String> actions = collectActionNames(fts);

        System.out.println("=== SVM (vibes-casestudies) structural facts ===");
        System.out.println("  states:      " + stateCount + " (paper expects 9)");
        System.out.println("  transitions: " + transitionCount + " (paper expects 13)");
        System.out.println("  actions:     " + actions.size() + " unique  →  " + actions);
        System.out.println("  initial:     " + fts.getInitialState().getName());
        // No assertion failure — print-and-record only at this step.
        assertNotNull(fts);
    }

    @Test
    public void replicate_svm_allStates_ltsVariant() throws Exception {
        FeaturedTransitionSystem fts = loadCasestudiesSvm();
        // LTS variant: ignore feature expressions, treat as plain transition
        // system. Devroey 2014's all-states algorithm without SAT validation.
        // Expected: a small number of test cases (paper got 5 with SAT;
        // LTS variant should be FEWER since no constraints fragment walks).
        List<TestCase> suite = AllStatesGenerator.generateForLts(
                fts, "svm_casestudies_lts");
        System.out.println("=== SVM all-states (LTS variant — no SAT) ===");
        System.out.println("  suite size: " + suite.size() + " test case(s)");
        for (int i = 0; i < suite.size(); i++) {
            System.out.println("  tc " + i + ":");
            for (Transition t : suite.get(i)) {
                System.out.println("    " + t.getSource().getName() + " -"
                        + t.getAction().getName() + "-> " + t.getTarget().getName());
            }
        }
    }

    @Test
    public void replicate_svm_allStates_ftsVariant() throws Exception {
        FeaturedTransitionSystem fts = loadCasestudiesSvm();
        Sat4JSolverFacade solver = loadCasestudiesSvmSolver();
        // FTS variant: full algorithm with SAT-validated walks. This is
        // Devroey 2014's Algorithm 3. Paper Table 3 SVM result: 5 test
        // cases. Anything materially different is a port-fidelity flag.
        List<TestCase> suite = AllStatesGenerator.generateForFts(
                fts, solver, "svm_casestudies_fts");
        System.out.println("=== SVM all-states (FTS variant — SAT-validated) ===");
        System.out.println("  suite size: " + suite.size() + " test case(s)");
        System.out.println("  paper Table 3 published: 5 test case(s)");
        for (int i = 0; i < suite.size(); i++) {
            System.out.println("  tc " + i + ":");
            for (Transition t : suite.get(i)) {
                System.out.println("    " + t.getSource().getName() + " -"
                        + t.getAction().getName() + "-> " + t.getTarget().getName()
                        + "  [" + fts.getFExpression(t) + "]");
            }
        }
    }

    private FeaturedTransitionSystem loadCasestudiesSvm() throws Exception {
        File ftsFile = new File(CASESTUDIES_ROOT, "svm.fts");
        return Xml.loadFeaturedTransitionSystem(new java.io.FileInputStream(ftsFile));
    }

    /**
     * The casestudies DIMACS file embeds the feature mapping as
     * {@code c <id> <name>} comments at the top. VIBeS' loader expects
     * two separate files (one for mapping, one for the CNF). Splits the
     * single file into a tmp pair on demand.
     */
    private Sat4JSolverFacade loadCasestudiesSvmSolver() throws Exception {
        Path combined = new File(CASESTUDIES_ROOT, "svm.splot.dimacs").toPath();
        Path tmpMapping = Files.createTempFile("svm-mapping-", ".txt");
        Path tmpDimacs = Files.createTempFile("svm-dimacs-", ".cnf");
        try (PrintWriter mappingOut = new PrintWriter(new FileWriter(tmpMapping.toFile()));
             PrintWriter dimacsOut = new PrintWriter(new FileWriter(tmpDimacs.toFile()))) {
            for (String line : Files.readAllLines(combined)) {
                if (line.startsWith("c ")) {
                    // e.g. "c 1 VendingMachine" → "1 VendingMachine"
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

    // ---------- helpers ----------

    private static int countStates(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<State> it = fts.states();
        while (it.hasNext()) { it.next(); n++; }
        return n;
    }

    private static int countTransitions(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) { it.next(); n++; }
        return n;
    }

    private static Set<String> collectActionNames(FeaturedTransitionSystem fts) {
        Set<String> names = new HashSet<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) names.add(it.next().getAction().getName());
        return names;
    }
}
