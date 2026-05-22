package be.vibes.testgeneration.experiment;

import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;

import java.io.File;
import java.net.URL;
import java.util.Iterator;

/**
 * Counts valid configurations enumerated by VIBeS' Sat4JSolverFacade
 * for every SPL under {@code cases/} and compares them to the user's
 * ESG-Fx reference table. No mutation of pipeline state — only counts.
 */
public class ConfigCountAudit {

    private static final String[][] SPLS = {
            {"SVM",            "cases/SodaVendingMachine/configs/SVM.dimacs",
                               "cases/SodaVendingMachine/configs/SVM_dimacsmapping.txt", "12"},
            {"eMail",          "cases/eMail/configs/eM.dimacs",
                               "cases/eMail/configs/eM_dimacsmapping.txt", "23"},
            {"Elevator",       "cases/Elevator/configs/El.dimacs",
                               "cases/Elevator/configs/El_dimacsmapping.txt", "42"},
            {"BankAccountv2",  "cases/BankAccountv2/configs/BAv2.dimacs",
                               "cases/BankAccountv2/configs/BAv2_dimacsmapping.txt", "498"},
    };

    public static void main(String[] args) throws Exception {
        System.out.println();
        System.out.println("| SPL            | Pipeline | Reference | Δ      | Status |");
        System.out.println("|----------------|----------|-----------|--------|--------|");
        for (String[] row : SPLS) {
            String name = row[0];
            String dimacs = row[1];
            String mapping = row[2];
            int expected = Integer.parseInt(row[3]);
            int pipeline = countConfigs(dimacs, mapping);
            int delta = pipeline - expected;
            String status = delta == 0 ? "match" : (delta > 0 ? "OVER" : "UNDER");
            System.out.printf("| %-14s | %8d | %9d | %+6d | %-6s |%n",
                    name, pipeline, expected, delta, status);
        }
    }

    private static int countConfigs(String dimacsResource, String mappingResource) throws Exception {
        URL dimacsUrl = ConfigCountAudit.class.getClassLoader().getResource(dimacsResource);
        URL mappingUrl = ConfigCountAudit.class.getClassLoader().getResource(mappingResource);
        DimacsModel model = DimacsModel.createFromTvlParserGeneratedFiles(
                new File(mappingUrl.toURI()), new File(dimacsUrl.toURI()));
        Sat4JSolverFacade solver = new Sat4JSolverFacade(model);
        Iterator<Configuration> it = solver.getSolutions();
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }
}
