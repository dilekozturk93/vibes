package be.vibes.testgeneration.experiment;

import be.vibes.dsl.io.Dot;
import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.Feature;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.testgeneration.coverage.StateCoverageGenerator;
import be.vibes.testgeneration.coverage.TransitionCoverageGenerator;
import be.vibes.testgeneration.coverage.TransitionPairCoverageGenerator;
import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.product.FExpressionPreservingProjection;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Builds a per-product visual + textual report for one SPL: for each valid
 * product configuration we render the projected (and SCC-repaired) FTS as
 * a PNG, then list the test suites generated for state / transition / pair
 * coverage immediately below it. Output is a single markdown document
 * referencing the per-product PNGs, under
 * {@code milestone-reports/per-product/<SPL>/}.
 *
 * <p>The selected/deselected feature list is filtered to features that
 * actually label at least one transition in the SPL FTS. Abstract or
 * root-level features (e.g. SVM's {@code svm} root and {@code b}
 * grouping node) never appear on any transition and so do not appear
 * in any product configuration shown here.
 *
 * <p>Run from the test classpath:
 * <pre>
 *   mvn -pl vibes-testgeneration test-compile && \
 *   mvn -pl vibes-testgeneration exec:java \
 *       -Dexec.mainClass=be.vibes.testgeneration.experiment.PerProductReportGenerator \
 *       -Dexec.classpathScope=test
 * </pre>
 * <p>By default the generator runs SVM only. Pass an SPL name as the first
 * exec argument (or set the {@code SPL} env var) to run on a different one.
 * eMail and Elevator only work once their DIMACS + mapping files have been
 * generated and copied into the resources tree (Phase 1 work).
 */
public final class PerProductReportGenerator {

    private static final class SplSpec {
        final String name;
        final String mxe;
        final String dimacs;
        final String mapping;
        SplSpec(String name, String mxe, String dimacs, String mapping) {
            this.name = name;
            this.mxe = mxe;
            this.dimacs = dimacs;
            this.mapping = mapping;
        }
    }

    private static SplSpec specFor(String name) {
        switch (name) {
            case "SVM":
                return new SplSpec("SVM",
                        "cases/SodaVendingMachine/SVM_ESGFx.mxe",
                        "cases/SodaVendingMachine/configs/SVM.dimacs",
                        "cases/SodaVendingMachine/configs/SVM_dimacsmapping.txt");
            case "eMail":
                return new SplSpec("eMail",
                        "cases/eMail/eM_ESGFx.mxe",
                        "cases/eMail/configs/eM.dimacs",
                        "cases/eMail/configs/eM_dimacsmapping.txt");
            case "Elevator":
                return new SplSpec("Elevator",
                        "cases/Elevator/El_ESGFx.mxe",
                        "cases/Elevator/configs/El.dimacs",
                        "cases/Elevator/configs/El_dimacsmapping.txt");
            default:
                throw new IllegalArgumentException("Unknown SPL: " + name);
        }
    }

    private PerProductReportGenerator() {
    }

    public static void main(String[] args) throws Exception {
        String splName = args.length > 0
                ? args[0]
                : System.getenv().getOrDefault("SPL", "SVM");
        SplSpec spec = specFor(splName);
        Path outputDir = Paths.get("milestone-reports/per-product/" + spec.name);
        Path reportFile = outputDir.resolve(spec.name + "-per-product-report.md");
        Path htmlFile = outputDir.resolve(spec.name + "-per-product-report.html");
        Files.createDirectories(outputDir);

        FeaturedTransitionSystem fts = loadFts(spec.mxe);
        Sat4JSolverFacade solver = loadSolver(spec.dimacs, spec.mapping);
        Set<String> ftsFeatures = collectFeatureNames(fts);
        System.out.println("FTS-labelling features (" + ftsFeatures.size() + "): " + ftsFeatures);

        try (BufferedWriter md = new BufferedWriter(new FileWriter(reportFile.toFile()));
             BufferedWriter html = new BufferedWriter(new FileWriter(htmlFile.toFile()))) {
            writeHtmlHeader(html, spec.name);
            md.write("# Per-Product Report — " + spec.name + "\n\n");
            html.write("<h1>Per-Product Report — " + escapeHtml(spec.name) + "</h1>\n");
            String intro = "Generated by PerProductReportGenerator. For each valid "
                    + spec.name + " configuration enumerated by Sat4JSolverFacade, this "
                    + "report shows the projected + SCC-repaired product-level FTS plus "
                    + "the test suites generated for state, all-transitions, and "
                    + "all-transition-pairs coverage.";
            md.write(intro + "\n\nAction sequences are written as `a -> b -> c -> ...`. "
                    + "Synthetic balancing actions (prefix `__balance__`) are not present "
                    + "in the final test cases.\n\n");
            html.write("<p>" + escapeHtml(intro) + " Action sequences are written as "
                    + "<code>a -&gt; b -&gt; c -&gt; ...</code>.</p>\n");
            String featuresLabel = "Features actually labelling transitions in the SPL FTS";
            String featuresValue = ftsFeatures.isEmpty()
                    ? "(none)"
                    : String.join(", ", new TreeSet<>(ftsFeatures));
            md.write("**" + featuresLabel + "** (filter applied below): `"
                    + featuresValue + "`. Abstract / grouping features that do not label "
                    + "any transition are omitted from the per-product feature lists.\n\n");
            html.write("<p><strong>" + escapeHtml(featuresLabel) + "</strong> (filter applied below): "
                    + "<code>" + escapeHtml(featuresValue) + "</code>. "
                    + "Abstract / grouping features are omitted from the per-product feature lists.</p>\n");
            md.write("---\n\n");
            html.write("<hr/>\n");

            int productIndex = 0;
            Iterator<Configuration> configs = solver.getSolutions();
            while (configs.hasNext()) {
                Configuration config = configs.next();
                productIndex++;
                writeProductSection(md, html, spec, outputDir, fts, config, productIndex, ftsFeatures);
            }
            md.write("---\n\nTotal products: " + productIndex + ".\n");
            html.write("<hr/>\n<p>Total products: " + productIndex + ".</p>\n");
            writeHtmlFooter(html);
        }
        System.out.println("Reports written to " + reportFile + " and " + htmlFile);
    }

    private static void writeHtmlHeader(BufferedWriter out, String name) throws IOException {
        out.write("<!DOCTYPE html>\n<html lang=\"en\"><head>\n");
        out.write("<meta charset=\"UTF-8\"/>\n");
        out.write("<title>Per-Product Report — " + escapeHtml(name) + "</title>\n");
        out.write("<style>\n"
                + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; "
                + "max-width: 1000px; margin: 2em auto; padding: 0 1em; color: #222; line-height: 1.5; }\n"
                + "h1 { border-bottom: 2px solid #333; padding-bottom: 0.3em; }\n"
                + "h2 { margin-top: 2em; color: #555; border-bottom: 1px solid #ddd; padding-bottom: 0.2em; }\n"
                + "h3 { color: #666; margin-top: 1.5em; }\n"
                + "img { max-width: 100%; border: 1px solid #ccc; padding: 4px; background: white; "
                + "display: block; margin: 1em 0; }\n"
                + "code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; font-size: 0.95em; "
                + "font-family: ui-monospace, 'Menlo', 'Consolas', monospace; }\n"
                + "pre { background: #f4f4f4; padding: 10px 12px; border-radius: 4px; overflow-x: auto; "
                + "font-family: ui-monospace, 'Menlo', 'Consolas', monospace; font-size: 0.95em; }\n"
                + "ul { padding-left: 1.5em; }\n"
                + "hr { border: 0; border-top: 1px solid #ccc; margin: 2.5em 0; }\n"
                + "</style>\n</head><body>\n");
    }

    private static void writeHtmlFooter(BufferedWriter out) throws IOException {
        out.write("</body></html>\n");
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Returns the set of feature names that appear in some transition's
     * {@link be.vibes.fexpression.FExpression} of the FTS. Used to filter
     * the per-product selected / deselected feature lists so abstract or
     * grouping features (root nodes, AND/OR group nodes that never label a
     * transition) do not pollute the output.
     */
    private static Set<String> collectFeatureNames(FeaturedTransitionSystem fts) {
        Set<String> names = new HashSet<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            be.vibes.fexpression.FExpression fexpr = fts.getFExpression(it.next());
            if (fexpr == null) {
                continue;
            }
            for (be.vibes.fexpression.Feature f : fexpr.getFeatures()) {
                names.add(f.getName());
            }
        }
        return names;
    }

    private static void writeProductSection(BufferedWriter md,
                                            BufferedWriter html,
                                            SplSpec spec,
                                            Path outputDir,
                                            FeaturedTransitionSystem fts,
                                            Configuration config,
                                            int productIndex,
                                            Set<String> ftsFeatures) throws Exception {
        String featuresLine = formatFeatures(config, ftsFeatures);

        FeaturedTransitionSystem projected = FExpressionPreservingProjection.project(fts, config);
        FeaturedTransitionSystem repaired = InitialSccFilter.keepInitialScc(projected);

        // Persist the repaired FTS itself (XML) plus a Dot and a PNG
        // rendering. The XML is the source-of-truth product-level model
        // the test suites below are generated from; keeping it on disk
        // alongside the visualizations makes the report self-contained.
        Path xmlPath = outputDir.resolve(spec.name + "-product" + productIndex + ".fts.xml");
        Path dotPath = outputDir.resolve(spec.name + "-product" + productIndex + ".dot");
        Path pngPath = outputDir.resolve(spec.name + "-product" + productIndex + ".png");
        be.vibes.dsl.io.Xml.print(repaired, xmlPath.toFile());
        try (PrintStream out = new PrintStream(dotPath.toFile())) {
            out.println(Dot.format(repaired));
        }
        Process p = new ProcessBuilder("dot", "-Tpng", dotPath.toString(),
                "-o", pngPath.toString()).inheritIO().start();
        int rc = p.waitFor();
        if (rc != 0) {
            System.err.println("dot exited " + rc + " for product " + productIndex);
        }

        String pngName = pngPath.getFileName().toString();
        int sCount = countStates(repaired);
        int tCount = countTransitions(repaired);

        md.write("## Product " + productIndex + "\n\n");
        md.write("**Selected features:** " + featuresLine + "\n\n");
        md.write("**Repaired FTS:** " + sCount + " states, " + tCount + " transitions\n\n");
        md.write("![Product " + productIndex + " projected FTS](" + pngName + ")\n\n");

        html.write("<h2>Product " + productIndex + "</h2>\n");
        html.write("<p><strong>Selected features:</strong> " + escapeHtml(featuresLine) + "</p>\n");
        html.write("<p><strong>Repaired FTS:</strong> " + sCount + " states, "
                + tCount + " transitions</p>\n");
        html.write("<img src=\"" + escapeHtml(pngName)
                + "\" alt=\"Product " + productIndex + " projected FTS\"/>\n");

        // State coverage
        TestCase stateTc = StateCoverageGenerator.generate(fts, config,
                spec.name + "_p" + productIndex + "_state");
        String stateSeq = actionSequence(stateTc);
        md.write("### State coverage (1 test case, "
                + countTestCaseLength(stateTc) + " transitions)\n\n");
        md.write("```\n" + stateSeq + "\n```\n\n");
        html.write("<h3>State coverage (1 test case, "
                + countTestCaseLength(stateTc) + " transitions)</h3>\n");
        html.write("<pre>" + escapeHtml(stateSeq) + "</pre>\n");

        // Transition coverage
        TestCase transitionTc = TransitionCoverageGenerator.generate(fts, config,
                spec.name + "_p" + productIndex + "_trans");
        List<List<String>> transitionSegments = splitAtSynthetics(transitionTc);
        int totalTrans = 0;
        for (List<String> seg : transitionSegments) {
            totalTrans += seg.size();
        }
        md.write("### All-transitions coverage ("
                + transitionSegments.size() + " test cases, "
                + totalTrans + " transitions total)\n\n");
        html.write("<h3>All-transitions coverage ("
                + transitionSegments.size() + " test cases, "
                + totalTrans + " transitions total)</h3>\n<ul>\n");
        for (int i = 0; i < transitionSegments.size(); i++) {
            String id = spec.name + "_p" + productIndex + "_trans_seg" + i;
            String seq = transitionSegments.get(i).isEmpty()
                    ? "(empty)"
                    : String.join(" -> ", transitionSegments.get(i));
            md.write("- **`" + id + "`**: `" + seq + "`\n");
            html.write("<li><code>" + escapeHtml(id) + "</code>: <code>"
                    + escapeHtml(seq) + "</code></li>\n");
        }
        md.write("\n");
        html.write("</ul>\n");

        // Pair coverage (suite)
        List<TestCase> pairSuite = TransitionPairCoverageGenerator.generate(fts, config,
                spec.name + "_p" + productIndex + "_pair");
        int totalPair = 0;
        for (TestCase tc : pairSuite) {
            totalPair += countTestCaseLength(tc);
        }
        md.write("### All-transition-pairs coverage ("
                + pairSuite.size() + " test cases, " + totalPair + " transitions total)\n\n");
        html.write("<h3>All-transition-pairs coverage ("
                + pairSuite.size() + " test cases, " + totalPair + " transitions total)</h3>\n<ul>\n");
        for (TestCase tc : pairSuite) {
            String seq = actionSequence(tc);
            md.write("- **`" + tc.getId() + "`**: `" + seq + "`\n");
            html.write("<li><code>" + escapeHtml(tc.getId()) + "</code>: <code>"
                    + escapeHtml(seq) + "</code></li>\n");
        }
        md.write("\n");
        html.write("</ul>\n");
    }

    private static String formatFeatures(Configuration config, Set<String> ftsFeatures) {
        TreeSet<String> selected = new TreeSet<>();
        TreeSet<String> deselected = new TreeSet<>();
        for (Feature f : config.getFeatures()) {
            // Only show features that actually label some transition of the
            // SPL FTS. Abstract group / root features (e.g. SVM's "svm" and
            // "b") never appear on any transition and so are not part of
            // what distinguishes one product from another at the
            // test-generation level.
            if (!ftsFeatures.contains(f.getName())) {
                continue;
            }
            if (config.isSelected(f)) {
                selected.add(f.getName());
            } else {
                deselected.add(f.getName());
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("selected = {").append(String.join(", ", selected)).append("}");
        if (!deselected.isEmpty()) {
            sb.append(", deselected = {").append(String.join(", ", deselected)).append("}");
        }
        return sb.toString();
    }

    private static String actionSequence(TestCase tc) {
        List<String> actions = new ArrayList<>();
        for (Transition t : tc) {
            actions.add(t.getAction().getName());
        }
        if (actions.isEmpty()) {
            return "(empty)";
        }
        return String.join(" -> ", actions);
    }

    /**
     * Splits a test case's action sequence at every synthetic balancing
     * action, producing one segment of real (executable) actions per
     * non-synthetic stretch. Mirrors what
     * {@code TransitionPairCoverageGenerator} does internally.
     */
    private static List<List<String>> splitAtSynthetics(TestCase tc) {
        List<List<String>> segments = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (Transition t : tc) {
            if (EulerianBalancer.isSyntheticAction(t.getAction())) {
                if (!current.isEmpty()) {
                    segments.add(current);
                    current = new ArrayList<>();
                }
            } else {
                current.add(t.getAction().getName());
            }
        }
        if (!current.isEmpty()) {
            segments.add(current);
        }
        if (segments.isEmpty()) {
            segments.add(new ArrayList<>());
        }
        return segments;
    }

    private static int countTestCaseLength(TestCase tc) {
        int n = 0;
        Iterator<Transition> it = tc.iterator();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }

    private static int countStates(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<be.vibes.ts.State> it = fts.states();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }

    private static int countTransitions(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }

    private static FeaturedTransitionSystem loadFts(String resource) throws Exception {
        URL url = PerProductReportGenerator.class.getClassLoader().getResource(resource);
        if (url == null) {
            throw new IllegalStateException("Resource missing: " + resource);
        }
        return new MxeToFtsConverter().convert(new File(url.toURI()));
    }

    private static Sat4JSolverFacade loadSolver(String dimacsResource, String mappingResource) throws Exception {
        URL dimacsUrl = PerProductReportGenerator.class.getClassLoader().getResource(dimacsResource);
        URL mappingUrl = PerProductReportGenerator.class.getClassLoader().getResource(mappingResource);
        DimacsModel model = DimacsModel.createFromTvlParserGeneratedFiles(
                new File(mappingUrl.toURI()), new File(dimacsUrl.toURI()));
        return new Sat4JSolverFacade(model);
    }
}
