package be.vibes.testgeneration.coverage.baseline;

import be.vibes.dsl.io.Xml;
import be.vibes.fexpression.DimacsModel;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Step 2 of the user's Devroey-replication audit: run AllStatesGenerator
 * 10 times on the same casestudies SVM model and check whether the result
 * is stable (deterministic) or varies (non-deterministic). If it is
 * stable but != 5, the 3-vs-5 difference is a deterministic property of
 * the port (i.e. a bug or a real algorithmic divergence), not run-to-run
 * noise.
 */
public class DevroeyReplicationSvmStabilityTest {

    private static final String CASESTUDIES_ROOT =
            "/Users/dilekozturk/git/vibes-casestudies/sodavendingmachine";
    private static final int RUNS = 10;

    @Test
    public void svm_allStates_isStableAcrossRuns() throws Exception {
        // Load model + solver fresh once.
        FeaturedTransitionSystem fts = Xml.loadFeaturedTransitionSystem(
                new java.io.FileInputStream(new File(CASESTUDIES_ROOT, "svm.fts")));

        List<List<String>> runResults = new ArrayList<>();
        Map<String, Integer> suiteSignatureFrequency = new HashMap<>();
        Map<Integer, Integer> suiteSizeFrequency = new HashMap<>();

        for (int run = 0; run < RUNS; run++) {
            Sat4JSolverFacade solver = loadCasestudiesSvmSolver();
            List<TestCase> suite = AllStatesGenerator.generateForFts(
                    fts, solver, "svm_run" + run);
            // Canonical signature: ordered list of action sequences.
            List<String> actionSequences = new ArrayList<>();
            for (TestCase tc : suite) {
                StringBuilder sb = new StringBuilder();
                for (Transition t : tc) {
                    if (sb.length() > 0) sb.append("->");
                    sb.append(t.getAction().getName());
                }
                actionSequences.add(sb.toString());
            }
            runResults.add(actionSequences);
            // Frequency tables.
            suiteSizeFrequency.merge(suite.size(), 1, Integer::sum);
            String suiteKey = String.join(" | ", actionSequences);
            suiteSignatureFrequency.merge(suiteKey, 1, Integer::sum);
        }

        System.out.println("=== SVM all-states stability over " + RUNS + " runs ===");
        System.out.println();
        System.out.println("Suite-size frequency table:");
        for (Map.Entry<Integer, Integer> e : suiteSizeFrequency.entrySet()) {
            System.out.println("  size " + e.getKey() + ": " + e.getValue() + " run(s)");
        }
        System.out.println();
        System.out.println("Suite-signature frequency table (action sequences, order-sensitive):");
        for (Map.Entry<String, Integer> e : suiteSignatureFrequency.entrySet()) {
            System.out.println("  [" + e.getValue() + "x] " + e.getKey());
        }
        System.out.println();
        if (suiteSignatureFrequency.size() == 1) {
            System.out.println("VERDICT: Algorithm is DETERMINISTIC on this model. "
                    + "Same suite every run.");
        } else {
            System.out.println("VERDICT: Algorithm is NON-DETERMINISTIC on this model. "
                    + "Multiple distinct suites observed.");
        }
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
