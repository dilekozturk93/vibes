package be.vibes.testgeneration.experiment;

import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.Transition;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * One-shot validator + setup writer for the BankAccountv2 SPL. Loads
 * MXE → FTS, loads SAT model, counts valid configurations, prints
 * structural facts. Lets us check whether the plan's BAv2 expectations
 * (498 valid configs) match before adding BAv2 to the per-product
 * report pipeline.
 */
public class BankAccountv2Setup {

    public static void main(String[] args) throws Exception {
        URL mxeUrl = BankAccountv2Setup.class.getClassLoader().getResource(
                "cases/BankAccountv2/BAv2_ESGFx.mxe");
        URL dimacsUrl = BankAccountv2Setup.class.getClassLoader().getResource(
                "cases/BankAccountv2/configs/BAv2.dimacs");
        URL mappingUrl = BankAccountv2Setup.class.getClassLoader().getResource(
                "cases/BankAccountv2/configs/BAv2_dimacsmapping.txt");

        FeaturedTransitionSystem fts = new MxeToFtsConverter().convert(new File(mxeUrl.toURI()));
        DimacsModel model = DimacsModel.createFromTvlParserGeneratedFiles(
                new File(mappingUrl.toURI()), new File(dimacsUrl.toURI()));
        Sat4JSolverFacade solver = new Sat4JSolverFacade(model);

        System.out.println("=== BankAccountv2 structural facts ===");
        System.out.println("  states: " + countStates(fts) + " (plan expects: ~44 vertices in MXE, after bisimulation expect smaller)");
        System.out.println("  transitions: " + countTransitions(fts));
        System.out.println("  unique actions: " + collectActions(fts).size());
        System.out.println("  initial state: " + fts.getInitialState().getName());
        System.out.println();
        System.out.print("  Enumerating valid configurations via SAT... ");
        long t0 = System.currentTimeMillis();
        int count = 0;
        Iterator<Configuration> it = solver.getSolutions();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        long elapsed = System.currentTimeMillis() - t0;
        System.out.println(count + " configs in " + elapsed + " ms");
        System.out.println("  plan expects: ~498 valid configurations");
    }

    private static int countStates(FeaturedTransitionSystem fts) {
        int n = 0; Iterator<State> it = fts.states(); while (it.hasNext()) { it.next(); n++; } return n;
    }
    private static int countTransitions(FeaturedTransitionSystem fts) {
        int n = 0; Iterator<Transition> it = fts.transitions(); while (it.hasNext()) { it.next(); n++; } return n;
    }
    private static Set<String> collectActions(FeaturedTransitionSystem fts) {
        Set<String> out = new HashSet<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) out.add(it.next().getAction().getName());
        return out;
    }
}
