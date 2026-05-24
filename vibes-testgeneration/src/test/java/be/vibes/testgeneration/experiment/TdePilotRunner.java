package be.vibes.testgeneration.experiment;

import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.testgeneration.coverage.RandomBaselineGenerator;
import be.vibes.testgeneration.coverage.StateCoverageGenerator;
import be.vibes.testgeneration.coverage.TransitionCoverageGenerator;
import be.vibes.testgeneration.coverage.TransitionPairCoverageGenerator;
import be.vibes.testgeneration.coverage.baseline.AllStatesGenerator;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.mutation.FaultDetector;
import be.vibes.testgeneration.mutation.TransitionDestinationExchange;
import be.vibes.testgeneration.product.FExpressionPreservingProjection;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.TestCase;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Standalone pilot runner for the TransitionDestinationExchange (TDE)
 * operator. Does NOT touch PerProductMutationReportGenerator's CSV /
 * MD / HTML outputs — only prints per-(SPL × coverage) TDE detection
 * rates to stdout so the (α)/(β) decision (pair > transition vs.
 * pair ≈ transition) can be made before committing to a full
 * 4-operator wire-up.
 *
 * <p>Methodology:
 * <ul>
 *   <li>4 coverage suites: family (Devroey 2014 projected), state,
 *       transition, pair.</li>
 *   <li>Inozemtseva-style equivalent-mutant treatment: a mutant is
 *       equivalent iff it survives all 4 coverage suites AND every one
 *       of {@code RANDOM_SEED_COUNT} random suites (default 100, single
 *       transition-budget — pilot, not full ensemble). Equivalent
 *       mutants are excluded from both numerator and denominator.</li>
 *   <li>A-only kill criterion via {@link FaultDetector#killsDynamic}:
 *       mid-walk {@code canExecute} refusal. NO behavioural-divergence
 *       check; that is the (β)-fallback for the next iteration.</li>
 * </ul>
 *
 * <p>Invoke:
 * <pre>
 *   mvn -pl vibes-testgeneration test-compile
 *   java -cp ... be.vibes.testgeneration.experiment.TdePilotRunner Elevator SAS
 * </pre>
 */
public final class TdePilotRunner {

    private TdePilotRunner() {
    }

    private static final int RANDOM_SEED_COUNT =
            Integer.parseInt(System.getenv().getOrDefault("RANDOM_SEED_COUNT", "100"));
    private static final int RANDOM_MAX_STEPS_MULTIPLIER =
            Integer.parseInt(System.getenv().getOrDefault("RANDOM_MAX_STEPS_MULTIPLIER", "2"));

    private static final SplSpec[] ALL_SPLS = new SplSpec[] {
            new SplSpec("SVM",
                    "cases/SodaVendingMachine/SVM_ESGFx.mxe",
                    "cases/SodaVendingMachine/configs/SVM.dimacs",
                    "cases/SodaVendingMachine/configs/SVM_dimacsmapping.txt"),
            new SplSpec("eMail",
                    "cases/eMail/eM_ESGFx.mxe",
                    "cases/eMail/configs/eM.dimacs",
                    "cases/eMail/configs/eM_dimacsmapping.txt"),
            new SplSpec("Elevator",
                    "cases/Elevator/El_ESGFx.mxe",
                    "cases/Elevator/configs/El.dimacs",
                    "cases/Elevator/configs/El_dimacsmapping.txt"),
            new SplSpec("BankAccountv2",
                    "cases/BankAccountv2/BAv2_ESGFx.mxe",
                    "cases/BankAccountv2/configs/BAv2.dimacs",
                    "cases/BankAccountv2/configs/BAv2_dimacsmapping.txt"),
            new SplSpec("StudentAttendanceSystem",
                    "cases/StudentAttendanceSystem/SAS_ESGFx.mxe",
                    "cases/StudentAttendanceSystem/configs/SAS.dimacs",
                    "cases/StudentAttendanceSystem/configs/SAS_dimacsmapping.txt"),
    };

    public static void main(String[] args) throws Exception {
        SplSpec[] target;
        if (args.length == 0) {
            target = ALL_SPLS;
        } else {
            target = new SplSpec[args.length];
            for (int i = 0; i < args.length; i++) {
                SplSpec found = null;
                for (SplSpec s : ALL_SPLS) {
                    if (s.name.equals(args[i]) || matchesShort(s.name, args[i])) {
                        found = s;
                        break;
                    }
                }
                if (found == null) throw new IllegalArgumentException("Unknown SPL: " + args[i]);
                target[i] = found;
            }
        }
        System.out.println("=== TDE pilot (A-only kill criterion, "
                + RANDOM_SEED_COUNT + " random seeds, transition-budget) ===");
        System.out.printf("%-26s %8s %8s %12s %12s %12s %12s %12s%n",
                "SPL", "products", "TDE-total", "equiv%",
                "family%", "state%", "trans%", "pair%");
        for (SplSpec s : target) {
            runSpl(s);
        }
    }

    private static boolean matchesShort(String full, String shortName) {
        return shortName.equalsIgnoreCase("SAS")
                && full.equalsIgnoreCase("StudentAttendanceSystem");
    }

    private static void runSpl(SplSpec spec) throws Exception {
        FeaturedTransitionSystem fts = loadFts(spec.mxe);
        Sat4JSolverFacade solver = loadSolver(spec.dimacs, spec.mapping);
        Sat4JSolverFacade tdeSolver = loadSolver(spec.dimacs, spec.mapping);
        TransitionDestinationExchange tde = new TransitionDestinationExchange(tdeSolver);

        Sat4JSolverFacade baselineSolver = loadSolver(spec.dimacs, spec.mapping);
        List<TestCase> familyBaseline = AllStatesGenerator.generateForFts(
                fts, baselineSolver, spec.name + "_family");

        long totalMutantCount = 0;
        long totalEquivalent = 0;
        long killedFamily = 0;
        long killedState = 0;
        long killedTrans = 0;
        long killedPair = 0;
        long transOnlyKilled = 0; // trans killed AND pair survived
        long pairOnlyKilled = 0;  // pair killed AND trans survived
        long bothKilled = 0;
        java.util.List<String> sampleTransOnlySurvivors = new java.util.ArrayList<>();
        int productCount = 0;

        Iterator<Configuration> configs = solver.getSolutions();
        while (configs.hasNext()) {
            Configuration cfg = configs.next();
            productCount++;
            FeaturedTransitionSystem projected =
                    FExpressionPreservingProjection.project(fts, cfg);
            FeaturedTransitionSystem repaired = InitialSccFilter.keepInitialScc(projected);

            // All three coverage generators return List<TestCase> as of
            // the structural fix (2026-05-24): each TC = one
            // initial-return trip, uniform reset semantic across criteria.
            List<TestCase> stateSuite = StateCoverageGenerator.generate(
                    fts, cfg, spec.name + "_p" + productCount + "_state");
            List<TestCase> transSuite = TransitionCoverageGenerator.generate(
                    fts, cfg, spec.name + "_p" + productCount + "_trans");
            List<TestCase> pairSuite = TransitionPairCoverageGenerator.generate(
                    fts, cfg, spec.name + "_p" + productCount + "_pair");
            List<TestCase> familyProjected = projectFamilySuite(familyBaseline, fts, cfg);

            tde.generateMutants(repaired);
            Map<String, FeaturedTransitionSystem> tdeMutants =
                    filterRealMutants(tde.getMutants());

            // Random ensemble: transition-coverage-budget × RANDOM_SEED_COUNT.
            // Single-budget pilot variant of PerProductMutationReportGenerator's
            // 3-budget ensemble — enough to compute the equivalence union.
            int transBudget = Math.max(1, countTestSuiteRealActions(transSuite));
            int randomMaxSteps = RandomBaselineGenerator.modelRelativeMaxSteps(
                    countTransitions(repaired), RANDOM_MAX_STEPS_MULTIPLIER);
            Set<String> randomKilled = new HashSet<>();
            for (int seed = 0; seed < RANDOM_SEED_COUNT; seed++) {
                List<TestCase> rs = RandomBaselineGenerator.generate(
                        repaired, spec.name + "_p" + productCount + "_rb",
                        transBudget, randomMaxSteps, (long) seed).getSuite();
                FaultDetector.KillResult r =
                        FaultDetector.scoreSuiteDynamic(rs, tdeMutants);
                Set<String> survivors = new HashSet<>(r.getSurvivors());
                for (String key : tdeMutants.keySet()) {
                    if (!survivors.contains(key)) randomKilled.add(key);
                }
            }

            FaultDetector.KillResult famR = FaultDetector.scoreSuiteDynamic(familyProjected, tdeMutants);
            FaultDetector.KillResult stR = FaultDetector.scoreSuiteDynamic(stateSuite, tdeMutants);
            FaultDetector.KillResult trR = FaultDetector.scoreSuiteDynamic(transSuite, tdeMutants);
            FaultDetector.KillResult prR = FaultDetector.scoreSuiteDynamic(pairSuite, tdeMutants);

            // Inozemtseva equivalent set: survived all 4 coverage suites
            // AND not killed by any of the RANDOM_SEED_COUNT random suites.
            Set<String> survFam = new HashSet<>(famR.getSurvivors());
            Set<String> survSt = new HashSet<>(stR.getSurvivors());
            Set<String> survTr = new HashSet<>(trR.getSurvivors());
            Set<String> survPr = new HashSet<>(prR.getSurvivors());
            int equivalent = 0;
            for (String key : tdeMutants.keySet()) {
                if (survFam.contains(key) && survSt.contains(key)
                        && survTr.contains(key) && survPr.contains(key)
                        && !randomKilled.contains(key)) {
                    equivalent++;
                }
            }
            totalMutantCount += tdeMutants.size();
            totalEquivalent += equivalent;
            killedFamily += famR.getKilled();
            killedState += stR.getKilled();
            killedTrans += trR.getKilled();
            killedPair += prR.getKilled();

            // Per-mutant breakdown: exclude equivalent mutants from both
            // axes (denominator is non-equivalent set, matching the % rows).
            for (String key : tdeMutants.keySet()) {
                if (survFam.contains(key) && survSt.contains(key)
                        && survTr.contains(key) && survPr.contains(key)
                        && !randomKilled.contains(key)) {
                    continue; // equivalent, skip
                }
                boolean killedByTrans = !survTr.contains(key);
                boolean killedByPair = !survPr.contains(key);
                if (killedByTrans && killedByPair) bothKilled++;
                else if (killedByTrans && !killedByPair) {
                    transOnlyKilled++;
                    if (sampleTransOnlySurvivors.size() < 8) {
                        sampleTransOnlySurvivors.add(spec.name + "_p" + productCount + "::" + key);
                    }
                } else if (!killedByTrans && killedByPair) pairOnlyKilled++;
            }
        }

        long denom = totalMutantCount - totalEquivalent;
        System.out.printf("%-26s %8d %8d %11.1f%% %11.1f%% %11.1f%% %11.1f%% %11.1f%%%n",
                spec.name, productCount, totalMutantCount,
                100.0 * totalEquivalent / Math.max(1, totalMutantCount),
                100.0 * killedFamily / Math.max(1, denom),
                100.0 * killedState / Math.max(1, denom),
                100.0 * killedTrans / Math.max(1, denom),
                100.0 * killedPair / Math.max(1, denom));
        System.out.printf("  %s breakdown: both-killed=%d, pair-only=%d, trans-only=%d "
                        + "(of %d non-equivalent)%n",
                spec.name, bothKilled, pairOnlyKilled, transOnlyKilled, denom);
        if (!sampleTransOnlySurvivors.isEmpty()) {
            System.out.println("  Sample trans-killed-pair-survived mutants:");
            for (String s : sampleTransOnlySurvivors) {
                System.out.println("    " + s);
            }
        }
    }

    // ---- shared helpers (intentionally local — pilot does not depend on the report generator) ----

    private static Map<String, FeaturedTransitionSystem> filterRealMutants(
            Map<String, FeaturedTransitionSystem> raw) {
        Map<String, FeaturedTransitionSystem> out = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, FeaturedTransitionSystem> e : raw.entrySet()) {
            String k = e.getKey();
            if (k.contains("__end__")
                    || k.contains(be.vibes.testgeneration.graph.EulerianBalancer.SYNTHETIC_ACTION_PREFIX)
                    || k.contains(be.vibes.testgeneration.graph.EulerianBalancer.DUPLICATE_ACTION_INFIX)) {
                continue;
            }
            out.put(k, e.getValue());
        }
        return out;
    }

    private static int countTestSuiteRealActions(List<TestCase> suite) {
        int n = 0;
        for (TestCase tc : suite) {
            for (be.vibes.ts.Transition t : tc) {
                String name = t.getAction().getName();
                if (name.startsWith(be.vibes.testgeneration.graph.EulerianBalancer.SYNTHETIC_ACTION_PREFIX)) continue;
                if (name.startsWith("__end__")) continue;
                n++;
            }
        }
        return n;
    }

    private static int countTransitions(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<be.vibes.ts.Transition> it = fts.transitions();
        while (it.hasNext()) { it.next(); n++; }
        return n;
    }

    private static List<TestCase> projectFamilySuite(List<TestCase> familySuite,
                                                    FeaturedTransitionSystem fts,
                                                    Configuration cfg) {
        List<TestCase> projected = new java.util.ArrayList<>(familySuite.size());
        for (TestCase familyTc : familySuite) {
            TestCase projectedTc = new TestCase(familyTc.getId() + "_proj");
            int kept = 0;
            try {
                for (be.vibes.ts.Transition t : familyTc) {
                    be.vibes.fexpression.FExpression fexpr = fts.getFExpression(t);
                    if (fexpr == null || fexpr.assign(cfg).applySimplification().isTrue()) {
                        projectedTc.enqueue(t);
                        kept++;
                    } else {
                        break;
                    }
                }
            } catch (be.vibes.ts.exception.TransitionSystenExecutionException e) {
                // halt on contiguity violation
            }
            if (kept > 0) projected.add(projectedTc);
        }
        return projected;
    }

    private static FeaturedTransitionSystem loadFts(String r) throws Exception {
        URL u = TdePilotRunner.class.getClassLoader().getResource(r);
        return new MxeToFtsConverter().convert(new File(u.toURI()));
    }

    private static Sat4JSolverFacade loadSolver(String d, String m) throws Exception {
        URL du = TdePilotRunner.class.getClassLoader().getResource(d);
        URL mu = TdePilotRunner.class.getClassLoader().getResource(m);
        return new Sat4JSolverFacade(DimacsModel.createFromTvlParserGeneratedFiles(
                new File(mu.toURI()), new File(du.toURI())));
    }

    private static final class SplSpec {
        final String name, mxe, dimacs, mapping;
        SplSpec(String n, String m, String d, String map) {
            this.name = n; this.mxe = m; this.dimacs = d; this.mapping = map;
        }
    }
}
