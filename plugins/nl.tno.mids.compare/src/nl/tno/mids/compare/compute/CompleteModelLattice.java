/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.compute;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import nl.tno.mids.automatalib.extensions.cif.AutomataLibToCif;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.RepetitionCount;
import nl.tno.mids.compare.data.Variant;
import nl.tno.mids.compare.options.CompareOptions;
import nl.tno.mids.gltsdiff.extensions.AnnotatedProperty;

/** Complete model lattice by adding intersection and union variants. */
public class CompleteModelLattice extends CompleteLattice<Model> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompleteModelLattice.class);

    private final ComparisonData comparisonData;

    private final CompareOptions options;

    private Entity currentEntity = null;

    /**
     * @param comparisonData Comparison data containing entities to complete lattice for.
     * @param options Compare options configuring completion of lattice.
     */
    public CompleteModelLattice(ComparisonData comparisonData, CompareOptions options) {
        this.comparisonData = comparisonData;
        this.options = options;
    }

    /**
     * Complete variant inclusion lattice for each entity.
     * <p>
     * Every arrow in a lattice amounts to language inclusion of the source model in the target model.
     * </p>
     * 
     * @param monitor Progress monitor to track execution.
     * @param warnings List to collect warnings generated during comparison.
     */
    public void completeLattices(IProgressMonitor monitor, List<String> warnings) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, comparisonData.getEntities().size());

        for (Entity entity: comparisonData.getEntities()) {
            subMonitor.split(1);

            LOGGER.debug("Completing lattice for " + comparisonData.getEntityType().getName() + " " + entity.getName());
            completeModelLattice(entity, warnings);
        }
    }

    /**
     * Complete variant inclusion lattice for a given entity.
     * 
     * @param entity Entity to complete lattice for.
     * @param warnings List to collect warnings generated during comparison.
     */
    private void completeModelLattice(Entity entity, List<String> warnings) {
        currentEntity = entity;
        completeLattice(entity.getLattice());

        if (entity.getLattice().stream().anyMatch(v -> v.hasIncompleteLattice())) {
            entity.setLatticeIncomplete();
            warnings.add("Level 5 lattice for " + comparisonData.getEntityType().getName() + " " + entity.getName()
                    + " was not completely computed.");
        }
    }

    @Override
    protected Variant<Model> computeUnion(Model value, Model otherValue) {
        if (isOverSizeLimit(value) || isOverSizeLimit(otherValue)) {
            return null;
        }

        CompactDFA<String> union = AutomataLibUtil.unionMinimized(value.getLanguageAutomaton(),
                otherValue.getLanguageAutomaton());

        return findOrCreateVariant(union);
    }

    @Override
    protected Variant<Model> computeIntersection(Model value, Model otherValue) {
        if (isOverSizeLimit(value) || isOverSizeLimit(otherValue)) {
            return null;
        }

        CompactDFA<String> intersection = AutomataLibUtil.intersectionMinimized(value.getLanguageAutomaton(),
                otherValue.getLanguageAutomaton());

        return findOrCreateVariant(intersection);
    }

    /**
     * @param value Model that is possibly too large for union or intersection computation.
     * @return {@code true} if the the model is over the size limit, {@code false} otherwise.
     */
    private boolean isOverSizeLimit(Model value) {
        return value.getModelSize() > options.unionIntersectionSizeLimit;
    }

    /**
     * Get the variant corresponding to a given model, creating a new variant if it doesn't exist yet.
     * 
     * @param model Model to get variant for.
     * @return Variant corresponding to the given model.
     */
    private Variant<Model> findOrCreateVariant(CompactDFA<String> model) {
        Variant<Model> variant = findVariant(currentEntity, model);
        if (variant == null) {
            variant = createAndAddVariant(currentEntity, model);
        }
        return variant;
    }

    /**
     * Find variant corresponding to a given model behavior.
     * 
     * @param entity Entity containing models to search.
     * @param dfa DFA whose variant behavior to find.
     * @return Variant containing model that is language equivalent to the given DFA, or {@code null} if no such variant
     *     exists.
     */
    private Variant<Model> findVariant(Entity entity, CompactDFA<String> dfa) {
        for (Variant<Model> variant: entity.getVariants()) {
            if (ComputeVariants.areLanguageEquivalent(variant.getValue().getLanguageAutomaton(), dfa)) {
                return variant;
            }
        }

        return null;
    }

    /**
     * Create new entity variant based on given model behavior.
     * 
     * @param entity Entity to contain new variant and lattice node.
     * @param dfa DFA with behavior of the new variant.
     * @return Created variant.
     */
    private Variant<Model> createAndAddVariant(Entity entity, CompactDFA<String> dfa) {
        // Create and add model.
        Specification spec = AutomataLibToCif.fsaToCifSpecification(dfa, entity.getName(), true);
        CompactNFA<String> nfa = AutomataLibUtil.dfaToNfa(dfa);
        CompactNFA<AnnotatedProperty<String, RepetitionCount>> annotatedNfa = AutomataLibUtil.rename(nfa,
                AnnotatedProperty::new);
        Model model = new Model(spec, dfa, annotatedNfa, entity.getName());
        entity.addModel(model);

        // Create and add variant.
        Variant<Model> specificationVariant = entity.addVariant(model, true);
        return specificationVariant;
    }
}
