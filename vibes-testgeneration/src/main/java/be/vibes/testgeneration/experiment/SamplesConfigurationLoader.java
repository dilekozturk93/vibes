package be.vibes.testgeneration.experiment;

import be.vibes.fexpression.Feature;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.fexpression.configuration.SimpleConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Builds an {@link Iterator}{@code <Configuration>} from a UniGen-style
 * DIMACS sample file plus its feature-name mapping. Used for SPLs whose
 * feature-model is too large to enumerate exhaustively via
 * {@link be.vibes.solver.Sat4JSolverFacade#getSolutions()}; the user's
 * prior published study (Syngovia, Tesla, HockertyShirts) pre-sampled
 * 400-product subsets through UniGen and we reuse those files verbatim
 * to keep the evaluation methodologically aligned.
 *
 * <p>Inspired by {@code SamplesToProductESGFx} in the user's ESG-Fx
 * codebase. Two simplifications relative to the ESG-Fx-side loader:
 * <ul>
 *   <li>UniGen samples are already feature-model-valid; this loader
 *       does NOT re-run mandatory / root / parent-child propagation
 *       (those are needed only when the sampler's output is partial).
 *       VIBeS' {@code FExpressionPreservingProjection} consumes the
 *       configuration directly via {@link Configuration#isSelected}, so
 *       deselected features are implicit (any feature not in the
 *       positive-literal set is treated as deselected) — matching
 *       {@link SimpleConfiguration} semantics.</li>
 *   <li>No feature-model validity recheck. UniGen output is trusted.</li>
 * </ul>
 *
 * <p>Sample file format (one sample per line):
 * <pre>
 *   1 2 3 -4 5 6 7 -8 ... -64 0
 * </pre>
 * Positive integer = the feature with the matching DIMACS id is selected.
 * Negative integer = the feature is deselected (the loader skips these —
 * implicit-deselect via {@link SimpleConfiguration}). Trailing {@code 0}
 * is the DIMACS terminator and is ignored.
 *
 * <p>DIMACS mapping file format (VIBeS-style, one entry per line):
 * <pre>
 *   1 featureNameA
 *   2 featureNameB
 *   ...
 * </pre>
 * (Note: the ESG-Fx-side files use {@code id:name} colon-separator with
 * an {@code ID:FeatureName} header — copy them into the VIBeS resource
 * tree via the {@code grep -v / sed} pipeline documented in the import
 * script. {@link be.vibes.fexpression.DimacsModel#createFromTvlParserGeneratedFiles}
 * expects the VIBeS form.)
 */
public final class SamplesConfigurationLoader {

    private SamplesConfigurationLoader() {
        // Utility class.
    }

    /**
     * Reads the samples + mapping pair and returns the in-order list of
     * configurations. Order matches the line order in the samples file
     * so per-product reports are stable across reruns.
     */
    public static List<Configuration> load(File samplesFile, File mappingFile) throws IOException {
        Map<Integer, String> idToName = readMapping(mappingFile);
        List<int[]> samples = readSamples(samplesFile);

        List<Configuration> configs = new ArrayList<>(samples.size());
        for (int[] sample : samples) {
            SimpleConfiguration cfg = new SimpleConfiguration();
            for (int literal : sample) {
                if (literal > 0) {
                    String name = idToName.get(literal);
                    if (name != null) {
                        cfg.selectFeature(Feature.feature(name));
                    }
                }
                // Negative literal = implicit deselect; SimpleConfiguration
                // treats "not selected" as deselected, so no action needed.
            }
            configs.add(cfg);
        }
        return configs;
    }

    /**
     * Iterator wrapper for the existing pipeline call sites that consume
     * {@code Iterator<Configuration>} from {@code Sat4JSolverFacade}.
     */
    public static Iterator<Configuration> loadAsIterator(File samplesFile, File mappingFile)
            throws IOException {
        return load(samplesFile, mappingFile).iterator();
    }

    private static Map<Integer, String> readMapping(File mappingFile) throws IOException {
        Map<Integer, String> out = new HashMap<>();
        for (String raw : Files.readAllLines(mappingFile.toPath())) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            // VIBeS-style: "<id> <name>". Tolerate the ESG-Fx-style
            // "<id>:<name>" / "ID:FeatureName" header just in case the
            // import script wasn't applied.
            if (line.startsWith("ID:")) continue;
            String[] parts;
            if (line.contains(":")) {
                parts = line.split(":", 2);
            } else {
                parts = line.split("\\s+", 2);
            }
            if (parts.length < 2) continue;
            try {
                int id = Integer.parseInt(parts[0].trim());
                out.put(id, parts[1].trim());
            } catch (NumberFormatException ignored) {
                // Skip malformed lines silently — keeps the loader
                // permissive on hand-curated mapping files.
            }
        }
        return out;
    }

    private static List<int[]> readSamples(File samplesFile) throws IOException {
        List<int[]> out = new ArrayList<>();
        for (String raw : Files.readAllLines(samplesFile.toPath())) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String[] tokens = line.split("\\s+");
            List<Integer> literals = new ArrayList<>(tokens.length);
            for (String tok : tokens) {
                try {
                    int v = Integer.parseInt(tok);
                    if (v != 0) literals.add(v);
                } catch (NumberFormatException ignored) {
                    // skip non-integer tokens
                }
            }
            if (literals.isEmpty()) continue;
            int[] arr = new int[literals.size()];
            for (int i = 0; i < arr.length; i++) arr[i] = literals.get(i);
            out.add(arr);
        }
        return out;
    }
}
