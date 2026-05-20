package be.vibes.testgeneration.experiment;

import be.vibes.dsl.io.Dot;
import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.Feature;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
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
 * Builds a stepwise walkthrough for the M2 SCC pipeline on a hand-picked
 * configuration of each MVP SPL. For each SPL, renders three PNGs:
 *
 * <ol>
 *   <li>the SPL-level FTS (canonical, post-bisimulation),</li>
 *   <li>the projected FTS after applying the chosen product configuration,</li>
 *   <li>the repaired FTS after {@link InitialSccFilter} keeps the SCC
 *       containing the initial state.</li>
 * </ol>
 *
 * Output:
 * {@code milestone-reports/scc-walkthrough/<SPL>/<SPL>-stepwise-walkthrough.{md,html}}
 */
public final class SccWalkthroughGenerator {

    private static final class SplSpec {
        final String name;
        final String mxe;
        final String dimacs;
        final String mapping;
        final int productToShow;
        SplSpec(String name, String mxe, String dimacs, String mapping, int productToShow) {
            this.name = name; this.mxe = mxe; this.dimacs = dimacs;
            this.mapping = mapping; this.productToShow = productToShow;
        }
    }

    private static final SplSpec[] SPLS = new SplSpec[] {
            // SVM product 1 is the canonical fragmentation case: projection
            // splits the FTS into multiple SCCs and the repair drops one of
            // them. Good demonstration material.
            new SplSpec("SVM",
                    "cases/SodaVendingMachine/SVM_ESGFx.mxe",
                    "cases/SodaVendingMachine/configs/SVM.dimacs",
                    "cases/SodaVendingMachine/configs/SVM_dimacsmapping.txt",
                    1),
            // eMail product 7: 3 SCCs after projection, the initial-state
            // SCC has most of the surviving states; clear visual contrast
            // between steps 2 and 3.
            new SplSpec("eMail",
                    "cases/eMail/eM_ESGFx.mxe",
                    "cases/eMail/configs/eM.dimacs",
                    "cases/eMail/configs/eM_dimacsmapping.txt",
                    7),
            // Elevator: many products either produce a fully strongly-
            // connected projection (no repair to show) or collapse the
            // initial SCC to a single state. Product 2 hits a sweet spot:
            // multiple SCCs after projection but the initial SCC keeps a
            // sensible chunk of the FTS.
            new SplSpec("Elevator",
                    "cases/Elevator/El_ESGFx.mxe",
                    "cases/Elevator/configs/El.dimacs",
                    "cases/Elevator/configs/El_dimacsmapping.txt",
                    2),
    };

    private SccWalkthroughGenerator() {}

    public static void main(String[] args) throws Exception {
        for (SplSpec spec : SPLS) runSpl(spec);
    }

    private static void runSpl(SplSpec spec) throws Exception {
        Path outDir = Paths.get("milestone-reports/scc-walkthrough/" + spec.name);
        Files.createDirectories(outDir);

        FeaturedTransitionSystem fts = loadFts(spec.mxe);
        Sat4JSolverFacade solver = loadSolver(spec.dimacs, spec.mapping);

        Configuration chosenConfig = null;
        Iterator<Configuration> configs = solver.getSolutions();
        for (int i = 0; i < spec.productToShow && configs.hasNext(); i++) {
            chosenConfig = configs.next();
        }
        if (chosenConfig == null) {
            System.err.println("Config " + spec.productToShow + " missing for " + spec.name);
            return;
        }

        FeaturedTransitionSystem projected = FExpressionPreservingProjection.project(fts, chosenConfig);
        FeaturedTransitionSystem repaired = InitialSccFilter.keepInitialScc(projected);

        Path step1 = renderDotPng(fts, outDir, spec.name + "-step1-spl");
        Path step2 = renderDotPng(projected, outDir, spec.name + "-step2-projected");
        Path step3 = renderDotPng(repaired, outDir, spec.name + "-step3-repaired");

        List<Set<State>> sccs = StronglyConnectedComponents.compute(projected);
        Set<State> initialScc = StronglyConnectedComponents.containing(
                projected, projected.getInitialState());

        Path mdPath = outDir.resolve(spec.name + "-stepwise-walkthrough.md");
        Path htmlPath = outDir.resolve(spec.name + "-stepwise-walkthrough.html");
        try (BufferedWriter md = new BufferedWriter(new FileWriter(mdPath.toFile()));
             BufferedWriter html = new BufferedWriter(new FileWriter(htmlPath.toFile()))) {
            writeHtmlHeader(html, spec.name);
            String title = "SCC Walkthrough — " + spec.name + " (product " + spec.productToShow + ")";
            md.write("# " + title + "\n\n");
            html.write("<h1>" + escapeHtml(title) + "</h1>\n");

            String intro = "Stepwise rendering of the M2 SCC repair pipeline on one hand-picked "
                    + "product of " + spec.name + ". Each step shows the FTS as a PNG and a "
                    + "one-paragraph summary of what changed. Synthetic balancing edges "
                    + "(introduced later by EulerianBalancer in M3) are not present here.";
            md.write(intro + "\n\n");
            html.write("<p>" + escapeHtml(intro) + "</p>\n");

            String featuresLine = formatFeatures(chosenConfig, collectFeatureNames(fts));
            md.write("**Chosen configuration:** " + featuresLine + "\n\n");
            html.write("<p><strong>Chosen configuration:</strong> "
                    + escapeHtml(featuresLine) + "</p>\n");

            writeStep(md, html, 1,
                    "SPL-level FTS (initial input to the pipeline)",
                    fts,
                    "This is the bisimulation-minimized FTS produced by MxeToFtsConverter from "
                            + "the MXE ESG-Fx model. Every transition still carries its feature "
                            + "expression for traceability. The same FTS is the input for every "
                            + "product.",
                    step1);

            int droppedProj = countTransitions(fts) - countTransitions(projected);
            int initialSccSize = initialScc == null ? 0 : initialScc.size();
            writeStep(md, html, 2,
                    "Projected FTS (after applying the configuration)",
                    projected,
                    "FExpressionPreservingProjection keeps only transitions whose feature "
                            + "expression evaluates to true under the chosen configuration. "
                            + droppedProj + " transition(s) were dropped relative to step 1. After "
                            + "projection the graph has " + sccs.size() + " strongly-connected "
                            + "component(s); the SCC containing the initial state has "
                            + initialSccSize + " state(s). If that count is less than the total "
                            + "state count, the next step removes the unreachable / non-returnable "
                            + "portions.",
                    step2);

            int droppedStates = countStates(projected) - countStates(repaired);
            int droppedTrans = countTransitions(projected) - countTransitions(repaired);
            String step3Explanation = "InitialSccFilter keeps only the SCC containing the "
                    + "initial state and drops every other state along with the transitions "
                    + "touching them. " + droppedStates + " state(s) and " + droppedTrans
                    + " transition(s) were removed. The result is strongly connected by "
                    + "construction and feeds the EulerianBalancer + Hierholzer pipeline in M3 "
                    + "and the coverage generators in M4 / M5 / M6.";
            if (countStates(repaired) <= 1) {
                step3Explanation += " — NOTE: the repaired FTS for this product is degenerate "
                        + "(only the initial state survives). This indicates a structural "
                        + "limitation in the current MXE-to-FTS conversion: when an ESG vertex "
                        + "has both ]-edges and non-]-edges (a 'mixed terminal'), the converter "
                        + "currently drops its ]-edges silently. With no back-to-INIT "
                        + "transitions remaining, the initial state ends up in a singleton SCC "
                        + "and InitialSccFilter collapses the FTS. Elevator is the SPL where "
                        + "this matters most — every event there is mixed-terminal. A planned "
                        + "Phase-1 algorithm refinement will add a synthetic 'end-of-test' "
                        + "transition from each mixed-terminal vertex back to INIT, which "
                        + "should restore meaningful connectivity for Elevator products.";
            }
            writeStep(md, html, 3, "Repaired FTS (initial-state SCC only)", repaired,
                    step3Explanation, step3);

            writeHtmlFooter(html);
        }
        System.out.println(spec.name + " walkthrough -> " + mdPath);
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
        int n = 0; Iterator<State> it = fts.states();
        while (it.hasNext()) { it.next(); n++; }
        return n;
    }

    private static int countTransitions(FeaturedTransitionSystem fts) {
        int n = 0; Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) { it.next(); n++; }
        return n;
    }

    private static FeaturedTransitionSystem loadFts(String resource) throws Exception {
        URL url = SccWalkthroughGenerator.class.getClassLoader().getResource(resource);
        if (url == null) throw new IllegalStateException("Resource missing: " + resource);
        return new MxeToFtsConverter().convert(new File(url.toURI()));
    }

    private static Sat4JSolverFacade loadSolver(String dimacs, String mapping) throws Exception {
        URL d = SccWalkthroughGenerator.class.getClassLoader().getResource(dimacs);
        URL m = SccWalkthroughGenerator.class.getClassLoader().getResource(mapping);
        DimacsModel model = DimacsModel.createFromTvlParserGeneratedFiles(
                new File(m.toURI()), new File(d.toURI()));
        return new Sat4JSolverFacade(model);
    }

    private static void writeHtmlHeader(BufferedWriter out, String name) throws IOException {
        out.write("<!DOCTYPE html>\n<html lang=\"en\"><head>\n<meta charset=\"UTF-8\"/>\n");
        out.write("<title>SCC Walkthrough — " + escapeHtml(name) + "</title>\n");
        out.write("<style>\nbody { font-family: -apple-system, BlinkMacSystemFont, sans-serif; "
                + "max-width: 1000px; margin: 2em auto; padding: 0 1em; line-height: 1.55; }\n"
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
