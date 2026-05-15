package be.vibes.testgeneration.mutation;

import be.vibes.ts.FeaturedTransitionSystem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for FTS-level mutation operators. Modelled after the
 * ESG-Fx-side {@code MutationOperator} hierarchy used in the user's prior
 * published study, ported to operate on
 * {@link FeaturedTransitionSystem}.
 *
 * <p>Each concrete operator overrides {@link #generateMutants} to populate
 * {@link #mutants} with a map from a stable string key (used as a unique
 * mutant identifier) to a {@link FeaturedTransitionSystem} instance that
 * realises the corresponding mutation.
 *
 * <p>Validation (does the mutant remain a well-formed FTS, e.g. has at
 * least one transition out of the initial state?) is intentionally NOT
 * performed here. Invalid mutants are still emitted and counted; downstream
 * coverage / fault-detection analysis is responsible for deciding what to
 * do with them. This mirrors how the ESG-Fx pipeline reports both valid
 * and "invalid" mutants and matches the user's published methodology.
 */
public abstract class MutationOperator {

    protected final String name;
    protected final Map<String, FeaturedTransitionSystem> mutants;

    protected MutationOperator(String name) {
        this.name = name;
        this.mutants = new LinkedHashMap<>();
    }

    /**
     * Populates {@link #mutants} with one mutant FTS per applicable mutation
     * site in the given original FTS.
     *
     * @param fts the original (un-mutated) FTS to mutate
     */
    public abstract void generateMutants(FeaturedTransitionSystem fts);

    /**
     * Returns the operator's human-readable name (e.g. "Transition Missing").
     */
    public String getName() {
        return name;
    }

    /**
     * Returns an unmodifiable view of all generated mutants, keyed by a
     * stable per-operator mutation identifier.
     */
    public Map<String, FeaturedTransitionSystem> getMutants() {
        return Collections.unmodifiableMap(mutants);
    }

    /**
     * Convenience: total number of mutants currently held.
     */
    public int getMutantCount() {
        return mutants.size();
    }
}
