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
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Devroey 2014 SPLC paper Table 3 replication on three case-study models:
 * SodaVendingMachine, Minepump, Claroline. Compares our port's coverage
 * results to paper Table 3.
 *
 * <p>Success criterion is COVERAGE (faulty-state, faulty-transition,
 * faulty-action) within tolerance ±0.05, NOT suite size. Suite size is
 * reported but the paper Table 3 itself does not report it; the only
 * suite-size figure in the paper is a single sentence in §4 with
 * unclear provenance.
 *
 * <p>Coverage formulas (faults uniformly sampled in paper §4.1):
 * <ul>
 *   <li>Faulty-state coverage = |states visited| / |total states|</li>
 *   <li>Faulty-transition coverage = |transitions traversed| / |total transitions|</li>
 *   <li>Faulty-action coverage = |unique actions used| / |total unique actions|</li>
 * </ul>
 */
public class DevroeyReplicationAllThreeTest {

    private static final String CASESTUDIES = "/Users/dilekozturk/git/vibes-casestudies";

    private static final class Expected {
        final String name;
        final double faultyState;
        final double faultyTransition;
        final double faultyAction;
        Expected(String n, double fs, double ft, double fa) {
            this.name = n;
            this.faultyState = fs;
            this.faultyTransition = ft;
            this.faultyAction = fa;
        }
    }

    private static final Expected[] EXPECTED = {
            // Paper Table 3 "(a-s)" rows. (faulty state, faulty transition, faulty action)
            new Expected("Soda V.M.", 1.00, 1.00, 1.00),
            new Expected("Minepump",  1.00, 1.00, 0.8695),
            new Expected("Claroline", 1.00, 0.1017, 1.00),
    };

    private static final String[][] MODEL_FILES = {
            {"sodavendingmachine", "svm.fts",       "svm.splot.dimacs"},
            {"minepump",           "minepump.fts",  "minepump.splot.dimacs"},
            {"claroline",          "claroline.fts", "claroline.splot.dimacs"},
    };

    @Test
    public void replicate_paperTable3_threeModels() throws Exception {
        System.out.println();
        System.out.println("# Devroey 2014 Table 3 replication — three case-study models");
        System.out.println();
        System.out.println("| Model      | Metric                    | Paper Tbl3 | Port     | Δ        | Status |");
        System.out.println("|------------|---------------------------|------------|----------|----------|--------|");

        for (int i = 0; i < MODEL_FILES.length; i++) {
            String dir = MODEL_FILES[i][0];
            String ftsFile = MODEL_FILES[i][1];
            String dimacsFile = MODEL_FILES[i][2];
            Expected exp = EXPECTED[i];

            FeaturedTransitionSystem fts = loadFtsPreprocessed(
                    new File(CASESTUDIES + "/" + dir, ftsFile));
            Sat4JSolverFacade solver = loadSolver(
                    new File(CASESTUDIES + "/" + dir, dimacsFile));

            int stateCount = countStates(fts);
            int transCount = countTransitions(fts);
            int actionCount = collectActions(fts).size();
            System.out.println("|            | model = " + stateCount + " states / "
                    + transCount + " transitions / " + actionCount
                    + " unique actions | | | | |");

            long t0 = System.currentTimeMillis();
            List<TestCase> suite = AllStatesGenerator.generateForFts(
                    fts, solver, exp.name.toLowerCase().replace(".", "").replace(" ", "_"));
            long elapsedMs = System.currentTimeMillis() - t0;

            // Compute coverage.
            Set<State> visitedStates = new HashSet<>();
            visitedStates.add(fts.getInitialState());
            Set<String> usedTransitions = new HashSet<>();
            Set<String> usedActions = new HashSet<>();
            int totalSteps = 0;
            for (TestCase tc : suite) {
                for (Transition t : tc) {
                    totalSteps++;
                    visitedStates.add(t.getSource());
                    visitedStates.add(t.getTarget());
                    usedTransitions.add(t.getSource().getName() + "|"
                            + t.getAction().getName() + "|" + t.getTarget().getName());
                    usedActions.add(t.getAction().getName());
                }
            }
            // Paper's faulty-* coverage is fraction of injected faults caught,
            // averaged over 100 fault-seeded models. With faults sampled
            // uniformly over states / transitions / actions, the expected
            // fraction is the structural coverage of the suite.
            double stateCov = (double) visitedStates.size() / (double) stateCount;
            double transCov = (double) usedTransitions.size() / (double) transCount;
            double actionCov = (double) usedActions.size() / (double) actionCount;

            printRow(exp.name, "Faulty state coverage", exp.faultyState, stateCov);
            printRow(exp.name, "Faulty transition cov", exp.faultyTransition, transCov);
            printRow(exp.name, "Faulty action coverage", exp.faultyAction, actionCov);
            System.out.println("|            | Suite size               | (n/a)      | "
                    + pad(String.valueOf(suite.size()), 8) + " | "
                    + pad("—", 8) + " |        |");
            System.out.println("|            | Total walk steps         | (n/a)      | "
                    + pad(String.valueOf(totalSteps), 8) + " | "
                    + pad("—", 8) + " |        |");
            System.out.println("|            | Generation time          | (n/a)      | "
                    + pad(elapsedMs + " ms", 8) + " | "
                    + pad("—", 8) + " |        |");
            // Spacer.
            System.out.println("|            |                          |            |          |          |        |");
        }
        System.out.println();
        System.out.println("Tolerance threshold (per user): ±0.05");
        System.out.println();
        System.out.println("Notes:");
        System.out.println("  * Suite size deliberately excluded from success criteria — paper "
                + "Table 3 does not report it; the only suite-size figure is a single sentence "
                + "in §4 with unclear provenance, and §2.2 gives a 3-case example for SVM.");
        System.out.println("  * 'Faulty-*' coverage in paper is the fraction of uniformly-"
                + "sampled injected faults the suite catches; under uniform sampling this "
                + "converges to the suite's structural coverage on the model.");
    }

    private static void printRow(String model, String metric, double expected, double actual) {
        double delta = actual - expected;
        boolean within = Math.abs(delta) <= 0.05;
        String status = within ? "✓ PASS" : "✗ FAIL";
        System.out.println("| " + pad(model, 10) + " | " + pad(metric, 25) + " | "
                + pad(String.format("%.4f", expected), 10) + " | "
                + pad(String.format("%.4f", actual), 8) + " | "
                + pad(String.format("%+.4f", delta), 8) + " | "
                + status + " |");
    }

    private static String pad(String s, int len) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < len) sb.append(' ');
        return sb.toString();
    }

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

    private static Set<String> collectActions(FeaturedTransitionSystem fts) {
        Set<String> out = new HashSet<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) out.add(it.next().getAction().getName());
        return out;
    }

    /**
     * Loads a vibes-casestudies FTS XML, normalising two casestudies
     * conventions that the modern VIBeS XML loader does not accept:
     *
     * <ol>
     *   <li>Some files use {@code <fts xmlns="...">} (no prefix); VIBeS
     *       loader handles both, but we strip the default namespace and
     *       add the {@code fts:} prefix to match the loader's expectations
     *       reliably across all three case-study files.</li>
     *   <li>Transitions without an explicit {@code fexpression} attribute
     *       cause an NPE in {@code FeaturedTransitionSystemHandler.handleStartTransitionTag}
     *       at line 53 (the handler dereferences the attribute without
     *       null-check). Per VIBeS/FTS semantics, a missing fexpression
     *       means "true" — we inject {@code fexpression="true"} on every
     *       transition that lacks it.</li>
     * </ol>
     */
    private FeaturedTransitionSystem loadFtsPreprocessed(File source) throws Exception {
        java.util.List<String> lines = Files.readAllLines(source.toPath());
        java.util.List<String> out = new java.util.ArrayList<>(lines.size());
        // Note: attribute values may contain '/' (e.g. Claroline's URL targets
        // "target=\"/claroline/admin/...\""), so the inner class must be
        // `[^>]` not `[^>/]`. We track the optional self-closing slash via a
        // trailing optional capture group AFTER the lazy attribute match.
        java.util.regex.Pattern transitionPattern =
                java.util.regex.Pattern.compile("<transition\\s+([^>]*?)(/?)>");
        for (String line : lines) {
            String normalised = line;
            // 1. FIRST: inject fexpression="true" on any transition that
            // lacks one. Done on the BARE <transition tag (before prefix
            // substitution) so the regex matches.
            java.util.regex.Matcher m = transitionPattern.matcher(normalised);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String attrs = m.group(1);
                String slash = m.group(2);
                String replacement;
                if (!attrs.contains("fexpression=")) {
                    // Note the explicit leading space: the regex's \s+
                    // consumed the whitespace between '<transition' and the
                    // first attribute, so we must add it back to avoid
                    // emitting '<transitiontarget="..."'.
                    replacement = "<transition " + attrs + " fexpression=\"true\"" + slash + ">";
                } else {
                    replacement = m.group();
                }
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            normalised = sb.toString();
            // 2. THEN: add fts: prefix to match the VIBeS loader's schema.
            normalised = normalised.replace("<fts xmlns=\"http://www.unamur.be/fts/\">",
                    "<fts:fts xmlns:fts=\"http://www.unamur.be/xml/fts/\">");
            normalised = normalised.replace("</fts>", "</fts:fts>");
            normalised = normalised.replace("<states>", "<fts:states>");
            normalised = normalised.replace("</states>", "</fts:states>");
            normalised = normalised.replace("<state ", "<fts:state ");
            normalised = normalised.replace("</state>", "</fts:state>");
            normalised = normalised.replace("<start>", "<fts:start>");
            normalised = normalised.replace("</start>", "</fts:start>");
            normalised = normalised.replace("<transition ", "<fts:transition ");
            normalised = normalised.replace("</transition>", "</fts:transition>");
            out.add(normalised);
        }
        Path tmp = Files.createTempFile("fts-preproc-", ".fts");
        Files.write(tmp, out);
        return Xml.loadFeaturedTransitionSystem(new java.io.FileInputStream(tmp.toFile()));
    }

    private Sat4JSolverFacade loadSolver(File combined) throws Exception {
        Path tmpMapping = Files.createTempFile("dim-map-", ".txt");
        Path tmpDimacs = Files.createTempFile("dim-cnf-", ".cnf");
        try (PrintWriter mappingOut = new PrintWriter(new FileWriter(tmpMapping.toFile()));
             PrintWriter dimacsOut = new PrintWriter(new FileWriter(tmpDimacs.toFile()))) {
            for (String line : Files.readAllLines(combined.toPath())) {
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
}
