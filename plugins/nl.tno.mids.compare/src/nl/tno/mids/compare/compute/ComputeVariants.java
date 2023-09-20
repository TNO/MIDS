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

import com.google.common.base.Preconditions;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.equivalence.NearLinearEquivalenceTest;
import net.automatalib.util.automata.fsa.DFAs;
import net.automatalib.words.Word;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.data.Variant;

/** Compute variants grouping similar models of the same entity together. */
public class ComputeVariants {
    /**
     * Create {@link Variant} instance for all models in given {@link ComparisonData}.
     * 
     * @param comparisonData {@link ComparisonData} contains models to analyze.
     */
    public static void computeEntityVariants(ComparisonData comparisonData) {
        // Compare all models sets.
        for (ModelSet modelSet: comparisonData.getModelSets()) {
            // Compare all models.
            for (Model model: modelSet.getModels()) {
                // Get entity corresponding to this model.
                Entity entity = comparisonData.getEntityByName(model.getEntityName());

                // Compare model against all known entity variants.
                Variant<Model> modelVariant = findEntityVariant(entity, model.getLanguageAutomaton());

                // If none of the existing variants matched, we found a new variant.
                if (modelVariant == null) {
                    modelVariant = entity.addVariant(model, false);
                }

                model.setVariant(modelVariant);
            }
        }

        comparisonData.setEntityNumbers();
    }

    /**
     * Find variant of a given entity corresponding to a given model.
     * 
     * @param entity {@link Entity} containing variants to compare to.
     * @param dfa DFA whose variant behavior to find.
     * @return {@link Variant} containing model that is language equivalent to the given DFA, or {@code null} if no such
     *     variant exists.
     */
    public static Variant<Model> findEntityVariant(Entity entity, CompactDFA<String> dfa) {
        for (Variant<Model> variant: entity.getVariants()) {
            if (areLanguageEquivalent(dfa, variant.getValue().getLanguageAutomaton())) {
                return variant;
            }
        }

        return null;
    }

    /**
     * Create {@link Variant} instances for all models in given {@link ComparisonData}.
     * 
     * @param comparisonData {@link ComparisonData} containing model sets to analyze.
     */
    public static void computeModelSetVariants(ComparisonData comparisonData) {
        int currentVariant = 1;
        for (ModelSet modelSet: comparisonData.getModelSets()) {
            if (modelSet.getModelSetVariant() == null) {
                Variant<ModelSet> modelSetVariant = new Variant<ModelSet>(currentVariant, modelSet,
                        modelSet.getNumberOfModelsWithBehavior(), false);
                comparisonData.getModelSetVariants().add(modelSetVariant);
                modelSet.setModelSetVariant(modelSetVariant);

                for (ModelSet otherModelSet: comparisonData.getModelSets()) {
                    if (ComputeVariants.areModelSetsEqual(modelSet, otherModelSet)) {
                        otherModelSet.setModelSetVariant(modelSetVariant);
                    }
                }

                currentVariant++;
            }
        }
    }

    /**
     * Compute if two model sets have the same variants for all entities.
     * 
     * @param modelSet {@link Modelset} to compare.
     * @param otherModelSet {@link Modelset} to compare.
     * @return true if all entities in the model sets have the same variants, false otherwise.
     */
    public static boolean areModelSetsEqual(ModelSet modelSet, ModelSet otherModelSet) {
        Preconditions.checkArgument(modelSet.getEntities().equals(otherModelSet.getEntities()));

        for (String entityName: modelSet.getEntities()) {
            Model model = modelSet.getEntityModel(entityName);
            Variant<Model> modelVariant = model.getVariant();

            Model otherModel = otherModelSet.getEntityModel(entityName);
            Variant<Model> otherModelVariant = otherModel.getVariant();

            if (modelVariant != otherModelVariant) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compare two DFAs based on language equivalence.
     * 
     * @param <T> The type of input symbols.
     * @param dfa {@link CompactDFA} to compare.
     * @param otherDfa Other {@link CompactDFA} to compare.
     * @return {@code true} if the DFAs have equivalent languages, {@code false} otherwise.
     */
    public static <T> boolean areLanguageEquivalent(CompactDFA<T> dfa, CompactDFA<T> otherDfa) {
        boolean dfaEmpty = DFAs.acceptsEmptyLanguage(dfa);
        boolean otherDfaEmpty = DFAs.acceptsEmptyLanguage(otherDfa);

        // If both DFAs accept only the empty language, we consider them equivalent.
        if (dfaEmpty && otherDfaEmpty) {
            return true;
        }

        // If one DFA accepts only the empty language and the other DFA does not, they are not equivalent.
        if (dfaEmpty != otherDfaEmpty) {
            return false;
        }

        AutomataLibUtil.synchronizeAlphabets(dfa, otherDfa);

        NearLinearEquivalenceTest<T> equiv = new NearLinearEquivalenceTest<>(dfa);
        Word<T> separatingWord = equiv.findSeparatingWord(otherDfa, dfa.getInputAlphabet());
        return separatingWord == null;
    }
}
