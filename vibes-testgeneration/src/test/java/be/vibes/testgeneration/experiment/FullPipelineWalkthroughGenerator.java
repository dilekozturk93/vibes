package be.vibes.testgeneration.experiment;

import be.vibes.dsl.io.Dot;
import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.Feature;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.graph.StronglyConnectedComponents;
import be.vibes.testgeneration.product.FExpressionPreservingProjection;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Renders the full conversion-to-balancing pipeline for one product
 * configuration per MVP SPL, showing four stages side by side:
 *
 * <ol>
 *   <li>SPL-level FTS (post-bisimulation, the canonical input to the
 *       pipeline);</li>
 *   <li>Projected FTS (after {@link FExpressionPreservingProjection});</li>
 *   <li>Strongly-connected projected FTS (after
 *       {@link InitialSccFilter} keeps the SCC containing the initial
 *       state);</li>
 *   <li>Strongly-connected + balanced FTS (after
 *       {@link EulerianBalancer} so every state has in-degree =
 *       out-degree — the Eulerian-cycle precondition).</li>
 * </ol>
 *
 * The configuration per SPL is hand-picked to expose the most striking
 * step-by-step change. Output:
 * {@code milestone-reports/full-pipeline-walkthrough/<SPL>/...}.
 */
public final class FullPipelineWalkthroughGenerator {

    private static final class SplSpec {
        final String name;
        final String mxe;
        final String dimacs;
        final String mapping;
        final int productToShow;
        SplSpec(String n, String m, String d, String map, int p) {
            this.name = n; this.mxe = m; this.dimacs = d; this.mapping = map; this.productToShow = p;
        }
    }

    private static final SplSpec[] SPLS = new SplSpec[] {
            new SplSpec("SVM",
                    "cases/SodaVendingMachine/SVM_ESGFx.mxe",
                    "cases/SodaVendingMachine/configs/SVM.dimacs",
                    "cases/SodaVendingMachine/configs/SVM_dimacsmapping.txt",
                    7),
            new SplSpec("eMail",
                    "cases/eMail/eM_ESGFx.mxe",
                    "cases/eMail/configs/eM.dimacs",
                    "cases/eMail/configs/eM_dimacsmapping.txt",
                    7),
            new SplSpec("Elevator",
                    "cases/Elevator/El_ESGFx.mxe",
                    "cases/Elevator/configs/El.dimacs",
                    "cases/Elevator/configs/El_dimacsmapping.txt",
                    7),
    };

    private FullPipelineWalkthroughGenerator() {}

    public static void main(String[] args) throws Exception {
        for (SplSpec spec : SPLS) run(spec);
    }

    private static void run(SplSpec spec) throws Exception {
        Path outDir = Paths.get("milestone-reports/full-pipeline-walkthrough/" + spec.name);
        Files.createDirectories(outDir);

        FeaturedTransitionSystem fts = loadFts(spec.mxe);
        Sat4JSolverFacade solver = loadSolver(spec.dimacs, spec.mapping);

        Configuration cfg = null;
        Iterator<Configuration> it = solver.getSolutions();
        for (int i = 0; i < spec.productToShow && it.hasNext(); i++) cfg = it.next();
        if (cfg == null) {
            System.err.println("Product " + spec.productToShow + " missing for " + spec.name);
            return;
        }

        FeaturedTransitionSystem projected = FExpressionPreservingProjection.project(fts, cfg);
        FeaturedTransitionSystem repaired = InitialSccFilter.keepInitialScc(projected);

        // Step 4 (balanced) only makes sense if step 3 is strongly connected;
        // if Hierholzer's preconditions are not met we'll skip the last step
        // gracefully.
        FeaturedTransitionSystem balanced = null;
        Exception balanceError = null;
        try {
            balanced = EulerianBalancer.balance(repaired);
        } catch (Exception ex) {
            balanceError = ex;
        }

        Path png1 = renderDotPng(fts, outDir, spec.name + "-step1-spl");
        Path png2 = renderDotPng(projected, outDir, spec.name + "-step2-projected");
        Path png3 = renderDotPng(repaired, outDir, spec.name + "-step3-strongly-connected");
        Path png4 = balanced != null
                ? renderDotPng(balanced, outDir, spec.name + "-step4-balanced")
                : null;

        List<Set<State>> sccs = StronglyConnectedComponents.compute(projected);

        Path mdPath = outDir.resolve(spec.name + "-full-pipeline-walkthrough.md");
        Path htmlPath = outDir.resolve(spec.name + "-full-pipeline-walkthrough.html");
        try (BufferedWriter md = new BufferedWriter(new FileWriter(mdPath.toFile()));
             BufferedWriter html = new BufferedWriter(new FileWriter(htmlPath.toFile()))) {
            writeHtmlHeader(html, spec.name);
            String title = "Full Pipeline Walkthrough — " + spec.name
                    + " (product " + spec.productToShow + ")";
            md.write("# " + title + "\n\n");
            html.write("<h1>" + escapeHtml(title) + "</h1>\n");

            String intro = "Four-stage rendering of the full conversion-to-balancing "
                    + "pipeline on one hand-picked product of " + spec.name + ". Each step "
                    + "shows the FTS as a PNG and a one-paragraph summary of what changed.";
            md.write(intro + "\n\n");
            html.write("<p>" + escapeHtml(intro) + "</p>\n");

            String featuresLine = formatFeatures(cfg, collectFeatureNames(fts));
            md.write("**Chosen configuration:** " + featuresLine + "\n\n");
            html.write("<p><strong>Chosen configuration:</strong> "
                    + escapeHtml(featuresLine) + "</p>\n");

            writeStep(md, html, 1, "SPL-level FTS (canonical input)", fts,
                    "Bisimulation-minimized FTS produced by MxeToFtsConverter. Feature "
                    + "expressions are retained on every transition for traceability. "
                    + "Synthetic '__end__' transitions (back-to-INIT for mixed-terminal "
                    + "ESG vertices) appear here if the source ESG has any '['-only-edge "
                    + "vertices.",
                    png1);

            int dropProj = countTransitions(fts) - countTransitions(projected);
            int initSccSize = sccs.isEmpty() ? 0 : sccCount(projected);
            writeStep(md, html, 2, "Projected FTS (after applying the configuration)", projected,
                    "FExpressionPreservingProjection keeps transitions whose feature "
                    + "expression evaluates to true under the configuration. " + dropProj
                    + " transition(s) dropped relative to step 1. Resulting graph has "
                    + sccs.size() + " strongly-connected component(s). If that count is 1 "
                    + "the next step is a no-op; otherwise it removes the parts of the "
                    + "graph the initial state cannot reach AND return from.",
                    png2);

            int dropStates = countStates(projected) - countStates(repaired);
            int dropTrans = countTransitions(projected) - countTransitions(repaired);
            writeStep(md, html, 3, "Strongly-connected projected FTS", repaired,
                    "InitialSccFilter keeps only the SCC containing the initial state and "
                    + "drops every other state plus the transitions touching them. "
                    + dropStates + " state(s) and " + dropTrans + " transition(s) removed. "
                    + "Result is strongly connected by construction — every state can reach "
                    + "every other state.",
                    png3);

            if (balanced != null) {
                int balancedStates = countStates(balanced);
                int balancedTrans = countTransitions(balanced);
                int syntheticBalancing = countSyntheticBalancing(balanced);
                int realTrans = balancedTrans - syntheticBalancing;
                writeStep(md, html, 4, "Strongly-connected + balanced FTS", balanced,
                        "EulerianBalancer adds " + syntheticBalancing + " synthetic '__balance__N' "
                        + "transitions so every state has in-degree = out-degree, the second "
                        + "precondition for Hierholzer's Euler-cycle algorithm. " + realTrans
                        + " real transition(s) preserved, " + syntheticBalancing
                        + " synthetic balancing transition(s) inserted; " + balancedStates
                        + " total state(s).",
                        png4);
            } else {
                md.write("## Step 4 — Strongly-connected + balanced FTS (skipped)\n\n");
                md.write("EulerianBalancer could not run because step 3 did not satisfy its "
                        + "precondition (likely the repaired FTS collapsed to fewer than 2 "
                        + "states). Error: `" + balanceError + "`.\n\n");
                html.write("<h2>Step 4 — Strongly-connected + balanced FTS (skipped)</h2>\n");
                html.write("<p>EulerianBalancer could not run. Error: <code>"
                        + escapeHtml(String.valueOf(balanceError)) + "</code></p>\n");
            }

            writeHtmlFooter(html);
        }
        System.out.println(spec.name + " full-pipeline walkthrough -> " + mdPath);
    }

    private static int sccCount(FeaturedTransitionSystem fts) {
        return StronglyConnectedComponents.compute(fts).size();
    }

    private static int countSyntheticBalancing(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            Transition t = it.next();
            if (t.getAction().getName().startsWith(EulerianBalancer.SYNTHETIC_ACTION_PREFIX)) {
                n++;
            }
        }
        return n;
    }

    private static void writeStep(BufferedWriter md, BufferedWriter html,
                                  int n, String heading,
                                  FeaturedTransitionSystem fts,
                                  String explanation, Path pngPath) throws IOException {
        int s = countStates(fts);
        int t = countTransitions(fts);
        md.write("## Step " + n + " — " + heading + "\n\n");
        md.write("**Size:** " + s + " states, " + t + " transitions\n\n");
        md.write(explanation + "\n\n");
        md.write("![Step " + n + "](" + pngPath.getFileName() + ")\n\n");

        html.write("<h2>Step " + n + " — " + escapeHtml(heading) + "</h2>\n");
        html.write("<p><strong>Size:</strong> " + s + " states, " + t + " transitions</p>\n");
        html.write("<p>" + escapeHtml(explanation) + "</p>\n");
        html.write("<img src=\"" + escapeHtml(pngPath.getFileName().toString())
                + "\" alt=\"Step " + n + "\"/>\n");
    }

    private static Path renderDotPng(FeaturedTransitionSystem fts, Path outDir, String basename)
            throws Exception {
        Path dot = outDir.resolve(basename + ".dot");
        Path png = outDir.resolve(basename + ".png");
        try (PrintStream out = new PrintStream(dot.toFile())) {
            out.println(Dot.format(fts));
        }
        Process p = new ProcessBuilder("dot", "-Tpng", dot.toString(), "-o", png.toString())
                .inheritIO().start();
        p.waitFor();
        return png;
    }

    private static Set<String> collectFeatureNames(FeaturedTransitionSystem fts) {
        Set<String> names = new java.util.HashSet<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            be.vibes.fexpression.FExpression fexpr = fts.getFExpression(it.next());
            if (fexpr == null) continue;
            for (be.vibes.fexpression.Feature f : fexpr.getFeatures()) names.add(f.getName());
        }
        return names;
    }

    private static String formatFeatures(Configuration cfg, Set<String> ftsFeatures) {
        TreeSet<String> sel = new TreeSet<>(), desel = new TreeSet<>();
        for (Feature f : cfg.getFeatures()) {
            if (!ftsFeatures.contains(f.getName())) continue;
            (cfg.isSelected(f) ? sel : desel).add(f.getName());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("selected = {").append(String.join(", ", sel)).append("}");
        if (!desel.isEmpty()) sb.append(", deselected = {").append(String.join(", ", desel)).append("}");
        return sb.toString();
    }

    private static int countStates(FeaturedTransitionSystem fts) {
        int n = 0; Iterator<State> it = fts.states(); while (it.hasNext()) { it.next(); n++; } return n;
    }

    private static int countTransitions(FeaturedTransitionSystem fts) {
        int n = 0; Iterator<Transition> it = fts.transitions(); while (it.hasNext()) { it.next(); n++; } return n;
    }

    private static FeaturedTransitionSystem loadFts(String r) throws Exception {
        URL u = FullPipelineWalkthroughGenerator.class.getClassLoader().getResource(r);
        return new MxeToFtsConverter().convert(new File(u.toURI()));
    }

    private static Sat4JSolverFacade loadSolver(String d, String m) throws Exception {
        URL du = FullPipelineWalkthroughGenerator.class.getClassLoader().getResource(d);
        URL mu = FullPipelineWalkthroughGenerator.class.getClassLoader().getResource(m);
        return new Sat4JSolverFacade(DimacsModel.createFromTvlParserGeneratedFiles(
                new File(mu.toURI()), new File(du.toURI())));
    }

    private static void writeHtmlHeader(BufferedWriter out, String name) throws IOException {
        out.write("<!DOCTYPE html>\n<html lang=\"en\"><head>\n<meta charset=\"UTF-8\"/>\n");
        out.write("<title>Full Pipeline Walkthrough — " + escapeHtml(name) + "</title>\n");
        out.write("<style>\nbody { font-family: -apple-system, BlinkMacSystemFont, sans-serif; "
                + "max-width: 1080px; margin: 2em auto; padding: 0 1em; line-height: 1.55; }\n"
                + "h1 { border-bottom: 2px solid #333; padding-bottom: 0.3em; }\n"
                + "h2 { margin-top: 2em; color: #444; border-bottom: 1px solid #ddd; padding-bottom: 0.2em; }\n"
                + "code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; "
                + "font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 0.95em; }\n"
                + "img { max-width: 100%; border: 1px solid #ccc; padding: 4px; background: white; "
                + "display: block; margin: 1em auto; }\n</style>\n</head><body>\n");
    }

    private static void writeHtmlFooter(BufferedWriter out) throws IOException {
        out.write("</body></html>\n");
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
