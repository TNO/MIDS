/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.compute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.data.LatticeNode;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.data.Variant;
import nl.tno.mids.compare.options.CompareOptions;
import nl.tno.mids.compare.options.EntityType;

/** Complete model set lattice by adding intersection and union variants. */
public class CompleteModelSetLattice extends CompleteLattice<ModelSet> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompleteModelSetLattice.class);

    private final ComparisonData comparisonData;

    private final CompareOptions options;

    private final EntityType entityType;

    private int index = 1;

    /**
     * @param comparisonData Comparison data containing model sets to complete lattice for.
     * @param options Compare options configuring completion of lattice.
     */
    public CompleteModelSetLattice(ComparisonData comparisonData, CompareOptions options) {
        this.comparisonData = comparisonData;
        this.options = options;
        this.entityType = comparisonData.getEntityType();
    }

    /**
     * Complete variant inclusion lattice.
     * <p>
     * Every arrow in a lattice amounts to language inclusion of the models of the source model set in the models of the
     * target model set.
     * </p>
     * 
     * @param warnings List to collect warnings generated during comparison.
     */
    public void completeModelSetLattice(List<String> warnings) {
        LOGGER.debug("Completing lattice for model sets");

        // Any model set lattice node for a model set that contains a model for which the model variant lattice node is
        // incomplete, leads to the model set lattice node also being incomplete.
        Set<Model> incompleteModelVariants = comparisonData.getEntities().stream().flatMap(e -> e.getLattice().stream())
                .filter(n -> n.hasIncompleteLattice()).map(n -> n.getValue().getValue()).collect(Collectors.toSet());
        for (LatticeNode<Variant<ModelSet>> modelSetLatticeNode: comparisonData.getLattice()) {
            Set<Model> modelsInNode = new HashSet<>(modelSetLatticeNode.getValue().getValue().getModels());
            if (!Sets.intersection(modelsInNode, incompleteModelVariants).isEmpty()) {
                modelSetLatticeNode.setLatticeIncomplete();
            }
        }

        completeLattice(comparisonData.getLattice());

        if (comparisonData.getLattice().stream().anyMatch(n -> n.hasIncompleteLattice())) {
            comparisonData.setModelSetLatticeIncomplete();
            warnings.add("Level 2 lattice was not completely computed.");
        }
    }

    @Override
    protected Variant<ModelSet> computeUnion(ModelSet value, ModelSet otherValue) {
        return computeCombinedVariant(value, otherValue, (n1, n2) -> n1.findCommonDescendant(n2));
    }

    @Override
    protected Variant<ModelSet> computeIntersection(ModelSet value, ModelSet otherValue) {
        return computeCombinedVariant(value, otherValue, (n1, n2) -> n1.findCommonAncestor(n2));
    }

    /**
     * Compute a combination of two model set variants.
     * 
     * @param modelSet First model set variant to compute combination for.
     * @param otherModelSet Second model set variant to compute combination for.
     * @param combiner Function finding combined lattice node for two model lattice nodes. The result may be
     *     {@code null} if the combined lattice node cannot be found.
     * @return An combined variant containing behavior computed by applying {@code combiner} to the models in both
     *     variants, if such can be computed, {@code null} otherwise. If the computed combination is represented by an
     *     existing variant, that variant is returned, otherwise a new variant is created.
     */
    private Variant<ModelSet> computeCombinedVariant(ModelSet modelSet, ModelSet otherModelSet,
            BinaryOperator<LatticeNode<Variant<Model>>> combiner)
    {
        Preconditions.checkArgument(modelSet.getEntities().equals(otherModelSet.getEntities()), "Incorrect number of "
                + entityType.getPlural() + " in model set " + modelSet.getName() + " or " + otherModelSet.getName());
        List<Model> combinedModels = new ArrayList<>();
        for (String entityName: modelSet.getEntities()) {
            Entity entity = comparisonData.getEntityByName(entityName);
            Model model = modelSet.getEntityModel(entityName);
            Model otherModel = otherModelSet.getEntityModel(entityName);

            LatticeNode<Variant<Model>> modelLatticeNode = ComputeModelSetLattice.findLatticeNode(entity, entityType,
                    model);
            LatticeNode<Variant<Model>> otherModelLatticeNode = ComputeModelSetLattice.findLatticeNode(entity,
                    entityType, otherModel);

            LatticeNode<Variant<Model>> combinedLatticeNode = combiner.apply(modelLatticeNode, otherModelLatticeNode);

            if (combinedLatticeNode == null) {
                return null;
            }

            combinedModels.add(combinedLatticeNode.getValue().getValue());
        }

        ModelSet combinedModelSet = modelSet.getModelType().getModelSetBuilder("computed_" + index, options)
                .addAll(combinedModels).createModelSet();

        return findOrCreateVariant(combinedModelSet);
    }

    /**
     * Get the variant corresponding to a given model set, creating a new variant if it doesn't exist yet.
     * 
     * @param modelSet Model set to get variant for.
     * @return Variant corresponding to the given model set.
     */
    private Variant<ModelSet> findOrCreateVariant(ModelSet modelSet) {
        Variant<ModelSet> variant = findVariant(modelSet);

        if (variant == null) {
            variant = createAndAddVariant(modelSet);
        }

        return variant;
    }

    /**
     * Find variant corresponding to a given model set.
     * 
     * @param modelSet Model set whose variant to find.
     * @return Variant containing model set with models that are language equivalent to models in the given model sets,
     *     {@code null} if no such variant exists.
     */
    private Variant<ModelSet> findVariant(ModelSet modelSet) {
        for (Variant<ModelSet> variant: comparisonData.getModelSetVariants()) {
            if (ComputeVariants.areModelSetsEqual(modelSet, variant.getValue())) {
                modelSet.setModelSetVariant(variant);
                return variant;
            }
        }
        return null;
    }

    /**
     * Create new variant based on given model set.
     * 
     * @param modelSet Model set of the new variant.
     * @return Created variant.
     */
    private Variant<ModelSet> createAndAddVariant(ModelSet modelSet) {
        Variant<ModelSet> variant;
        variant = new Variant<>(comparisonData.getModelSetVariants().size() + 1, modelSet,
                modelSet.getNumberOfModelsWithBehavior(), true);
        modelSet.setModelSetVariant(variant);
        index++;

        comparisonData.getModelSets().add(modelSet);
        comparisonData.getModelSetVariants().add(variant);
        return variant;
    }
}
