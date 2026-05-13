package be.vibes.testgeneration.graph;

import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;

import java.io.File;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Diagnostic helper (NOT a test). Prints the SCC decomposition of the three
 * MVP SPLs. Useful for the M2 report; run via:
 *
 * <pre>
 *   mvn -pl vibes-testgeneration test-compile && \
 *   mvn -pl vibes-testgeneration exec:java \
 *       -Dexec.mainClass=be.vibes.testgeneration.graph.SccDiagnostic \
 *       -Dexec.classpathScope=test
 * </pre>
 */
public final class SccDiagnostic {

    private SccDiagnostic() {
    }

    public static void main(String[] args) throws Exception {
        report("SVM",      "cases/SodaVendingMachine/SVM_ESGFx.mxe");
        report("eMail",    "cases/eMail/eM_ESGFx.mxe");
        report("Elevator", "cases/Elevator/El_ESGFx.mxe");
    }

    private static void report(String name, String resourcePath) throws Exception {
        URL url = SccDiagnostic.class.getClassLoader().getResource(resourcePath);
        if (url == null) {
            System.err.println("Missing resource: " + resourcePath);
            return;
        }
        FeaturedTransitionSystem fts = new MxeToFtsConverter().convert(new File(url.toURI()));
        List<Set<State>> sccs = new java.util.ArrayList<>(StronglyConnectedComponents.compute(fts));
        sccs.sort(Comparator.comparingInt((Set<State> s) -> s.size()).reversed());

        System.out.printf("%n=== %s ===%n", name);
        System.out.printf("  SCC count    : %d%n", sccs.size());
        System.out.printf("  Largest SCC  : %d states%n", sccs.isEmpty() ? 0 : sccs.get(0).size());
        System.out.printf("  Singletons   : %d%n",
                sccs.stream().filter(s -> s.size() == 1).count());
        System.out.printf("  Sizes (sorted): %s%n",
                sccs.stream().mapToInt(Set::size).toArray().length == 0 ? "[]"
                        : java.util.Arrays.toString(
                                sccs.stream().mapToInt(Set::size).toArray()));
    }
}
