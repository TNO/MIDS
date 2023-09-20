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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.escet.common.java.Maps;
import org.junit.jupiter.api.Test;

import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.data.LatticeEdge;
import nl.tno.mids.compare.data.LatticeNode;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.data.Variant;
import nl.tno.mids.compare.options.CompareOptions;
import nl.tno.mids.compare.options.EntityType;
import nl.tno.mids.compare.options.ModelType;
import nl.tno.mids.compare.util.TestAutomata;

class CompleteModelSetLatticeTest {
    private static final String COMPUTED_PREFIX = "computed_";

    private static final String COMPONENT_A_NAME = "A";

    private static final String COMPONENT_B_NAME = "B";

    private static final String COMPONENT_C_NAME = "C";

    private static final EntityType componentEntityType = new EntityType("component");

    @Test
    /** Basic test with two variants of a component with a union and an intersection. */
    public void basicTest() {
        List<ModelSet> modelSets = new ArrayList<>();

        Model modelA1 = TestAutomata.createModelA(COMPONENT_A_NAME);
        Map<String, Model> models1 = Maps.map();
        models1.put(COMPONENT_A_NAME, modelA1);
        ModelSet modelSet1 = new ModelSet(COMPUTED_PREFIX + 1, models1, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSet1);

        Model modelA2 = TestAutomata.createModelB(COMPONENT_A_NAME);
        Map<String, Model> models2 = Maps.map();
        models2.put(COMPONENT_A_NAME, modelA2);
        ModelSet modelSet2 = new ModelSet(COMPUTED_PREFIX + 2, models2, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSet2);
        ComparisonData comparisonData = new ComparisonData(modelSets);

        Variant<ModelSet> modelSetVariant1 = new Variant<ModelSet>(1, modelSet1,
                modelSet1.getNumberOfModelsWithBehavior(), false);
        comparisonData.getModelSetVariants().add(modelSetVariant1);
        modelSet1.setModelSetVariant(modelSetVariant1);
        LatticeNode<Variant<ModelSet>> latticeSet1 = new LatticeNode<>(modelSetVariant1);
        comparisonData.getLattice().add(latticeSet1);

        Variant<ModelSet> modelSetVariant2 = new Variant<ModelSet>(2, modelSet2,
                modelSet2.getNumberOfModelsWithBehavior(), false);
        comparisonData.getModelSetVariants().add(modelSetVariant2);
        modelSet2.setModelSetVariant(modelSetVariant2);
        LatticeNode<Variant<ModelSet>> latticeSet2 = new LatticeNode<>(modelSetVariant2);
        comparisonData.getLattice().add(latticeSet2);

        Entity component = comparisonData.getEntityByName(modelA1.getEntityName());

        Variant<Model> variantA1 = component.addVariant(modelA1, false);
        modelA1.setVariant(variantA1);
        LatticeNode<Variant<Model>> latticeA1 = new LatticeNode<>(variantA1);
        component.getLattice().add(latticeA1);
        Variant<Model> variantA2 = component.addVariant(modelA2, false);
        modelA2.setVariant(variantA2);
        LatticeNode<Variant<Model>> latticeA2 = new LatticeNode<>(variantA2);
        component.getLattice().add(latticeA2);

        Model modelIntersection = TestAutomata.createModelEmpty(COMPONENT_A_NAME);
        Variant<Model> variantIntersection = component.addVariant(modelIntersection, true);
        modelIntersection.setVariant(variantIntersection);
        LatticeNode<Variant<Model>> latticeIntersection = new LatticeNode<>(variantIntersection);
        component.getLattice().add(latticeIntersection);
        Model modelUnion = TestAutomata.createModelAB(COMPONENT_A_NAME);
        Variant<Model> variantUnion = component.addVariant(modelUnion, true);
        modelUnion.setVariant(variantUnion);
        LatticeNode<Variant<Model>> latticeUnion = new LatticeNode<>(variantUnion);
        component.getLattice().add(latticeUnion);

        new LatticeEdge<>(latticeA1, latticeUnion);
        new LatticeEdge<>(latticeA2, latticeUnion);
        new LatticeEdge<>(latticeIntersection, latticeA1);
        new LatticeEdge<>(latticeIntersection, latticeA2);

        CompareOptions options = new CompareOptions();
        options.entityType = componentEntityType;
        CompleteModelSetLattice completeModelSetLattice = new CompleteModelSetLattice(comparisonData, options);

        completeModelSetLattice.completeModelSetLattice(new ArrayList<>());

        // 2 input model variants + 1 intersection model variant + 1 union model variant.
        assertEquals(4, comparisonData.getModelSetVariants().size());

        // Each input model variant has a single parent intersection variant.
        assertEquals(1, latticeSet1.getParents().size());
        assertEquals(1, latticeSet2.getParents().size());

        // Each input model variant has a single child union variant.
        assertEquals(1, latticeSet1.getChildren().size());
        assertEquals(1, latticeSet2.getChildren().size());

        // Union and intersection variants are shared.
        assertEquals(latticeSet1.getParents(), latticeSet2.getParents());
        assertEquals(latticeSet1.getChildren(), latticeSet2.getChildren());
    }

    @Test
    /** Test where the union of one pair of models sets is the intersection of another pair of model sets. */
    public void unionIntersectionOverlapTest() {
        List<ModelSet> modelSets = new ArrayList<>();

        Model modelA1 = TestAutomata.createModelA(COMPONENT_A_NAME);
        Map<String, Model> models1 = Maps.map();
        models1.put(COMPONENT_A_NAME, modelA1);

        Model modelB1 = TestAutomata.createModelAB(COMPONENT_B_NAME);
        models1.put(COMPONENT_B_NAME, modelB1);

        Model modelC1 = TestAutomata.createModelEmpty(COMPONENT_C_NAME);
        models1.put(COMPONENT_C_NAME, modelC1);

        ModelSet modelSet1 = new ModelSet(COMPUTED_PREFIX + 1, models1, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSet1);

        Model modelA2 = TestAutomata.createModelAB(COMPONENT_A_NAME);
        Map<String, Model> models2 = Maps.map();
        models2.put(COMPONENT_A_NAME, modelA2);

        Model modelB2 = TestAutomata.createModelA(COMPONENT_B_NAME);
        models2.put(COMPONENT_B_NAME, modelB2);

        Model modelC2 = TestAutomata.createModelEmpty(COMPONENT_C_NAME);
        models2.put(COMPONENT_C_NAME, modelC2);

        ModelSet modelSet2 = new ModelSet(COMPUTED_PREFIX + 2, models2, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSet2);

        Model modelA3 = TestAutomata.createModelAB(COMPONENT_A_NAME);
        Map<String, Model> models3 = Maps.map();
        models3.put(COMPONENT_A_NAME, modelA3);

        Model modelB3 = TestAutomata.createModelAB(COMPONENT_B_NAME);
        models3.put(COMPONENT_B_NAME, modelB3);

        Model modelC3 = TestAutomata.createModelA(COMPONENT_C_NAME);
        models3.put(COMPONENT_C_NAME, modelC3);

        ModelSet modelSet3 = new ModelSet(COMPUTED_PREFIX + 3, models3, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSet3);

        Model modelA4 = TestAutomata.createModelAB(COMPONENT_A_NAME);
        Map<String, Model> models4 = Maps.map();
        models4.put(COMPONENT_A_NAME, modelA4);

        Model modelB4 = TestAutomata.createModelAB(COMPONENT_B_NAME);
        models4.put(COMPONENT_B_NAME, modelB4);

        Model modelC4 = TestAutomata.createModelB(COMPONENT_C_NAME);
        models4.put(COMPONENT_C_NAME, modelC4);

        ModelSet modelSet4 = new ModelSet(COMPUTED_PREFIX + 4, models4, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSet4);
        ComparisonData comparisonData = new ComparisonData(modelSets);

        Variant<ModelSet> modelSetVariant1 = new Variant<ModelSet>(1, modelSet1,
                modelSet1.getNumberOfModelsWithBehavior(), false);
        comparisonData.getModelSetVariants().add(modelSetVariant1);
        modelSet1.setModelSetVariant(modelSetVariant1);
        LatticeNode<Variant<ModelSet>> latticeSet1 = new LatticeNode<>(modelSetVariant1);
        comparisonData.getLattice().add(latticeSet1);

        Variant<ModelSet> modelSetVariant2 = new Variant<ModelSet>(2, modelSet2,
                modelSet2.getNumberOfModelsWithBehavior(), false);
        comparisonData.getModelSetVariants().add(modelSetVariant2);
        modelSet2.setModelSetVariant(modelSetVariant2);
        LatticeNode<Variant<ModelSet>> latticeSet2 = new LatticeNode<>(modelSetVariant2);
        comparisonData.getLattice().add(latticeSet2);

        Variant<ModelSet> modelSetVariant3 = new Variant<ModelSet>(3, modelSet3,
                modelSet2.getNumberOfModelsWithBehavior(), false);
        comparisonData.getModelSetVariants().add(modelSetVariant3);
        modelSet3.setModelSetVariant(modelSetVariant3);
        LatticeNode<Variant<ModelSet>> latticeSet3 = new LatticeNode<>(modelSetVariant3);
        comparisonData.getLattice().add(latticeSet3);

        Variant<ModelSet> modelSetVariant4 = new Variant<ModelSet>(4, modelSet4,
                modelSet4.getNumberOfModelsWithBehavior(), false);
        comparisonData.getModelSetVariants().add(modelSetVariant4);
        modelSet4.setModelSetVariant(modelSetVariant4);
        LatticeNode<Variant<ModelSet>> latticeSet4 = new LatticeNode<>(modelSetVariant4);
        comparisonData.getLattice().add(latticeSet4);

        new LatticeEdge<>(latticeSet1, latticeSet3);
        new LatticeEdge<>(latticeSet1, latticeSet4);
        new LatticeEdge<>(latticeSet2, latticeSet3);
        new LatticeEdge<>(latticeSet2, latticeSet4);

        Entity componentA = comparisonData.getEntityByName(modelA1.getEntityName());

        Variant<Model> variantA1 = componentA.addVariant(modelA1, false);
        modelA1.setVariant(variantA1);
        LatticeNode<Variant<Model>> latticeA1 = new LatticeNode<>(variantA1);
        componentA.getLattice().add(latticeA1);
        Variant<Model> variantA2 = componentA.addVariant(modelA2, false);
        modelA2.setVariant(variantA2);
        modelA3.setVariant(variantA2);
        modelA4.setVariant(variantA2);
        LatticeNode<Variant<Model>> latticeA2 = new LatticeNode<>(variantA2);
        componentA.getLattice().add(latticeA2);

        new LatticeEdge<>(latticeA1, latticeA2);

        Entity componentB = comparisonData.getEntityByName(modelB1.getEntityName());

        Variant<Model> variantB1 = componentB.addVariant(modelB1, false);
        modelB1.setVariant(variantB1);
        modelB3.setVariant(variantB1);
        modelB4.setVariant(variantB1);
        LatticeNode<Variant<Model>> latticeB1 = new LatticeNode<>(variantB1);
        componentB.getLattice().add(latticeB1);

        Variant<Model> variantB2 = componentB.addVariant(modelB2, false);
        modelB2.setVariant(variantB2);
        LatticeNode<Variant<Model>> latticeB2 = new LatticeNode<>(variantB2);
        componentB.getLattice().add(latticeB2);

        new LatticeEdge<>(latticeB2, latticeB1);

        Entity componentC = comparisonData.getEntityByName(modelC1.getEntityName());

        Variant<Model> variantC1 = componentB.addVariant(modelC1, false);
        modelC1.setVariant(variantC1);
        modelC2.setVariant(variantC1);
        LatticeNode<Variant<Model>> latticeC1 = new LatticeNode<>(variantC1);
        componentC.getLattice().add(latticeC1);

        Variant<Model> variantC2 = componentC.addVariant(modelC3, false);
        modelC3.setVariant(variantC2);
        LatticeNode<Variant<Model>> latticeC2 = new LatticeNode<>(variantC2);
        componentC.getLattice().add(latticeC2);

        Variant<Model> variantC3 = componentC.addVariant(modelC4, false);
        modelC4.setVariant(variantC3);
        LatticeNode<Variant<Model>> latticeC3 = new LatticeNode<>(variantC3);
        componentC.getLattice().add(latticeC3);

        Model modelUnionC = TestAutomata.createModelAB(COMPONENT_C_NAME);
        Variant<Model> variantUnionC = componentC.addVariant(modelUnionC, true);
        modelUnionC.setVariant(variantUnionC);
        LatticeNode<Variant<Model>> latticeUnionC = new LatticeNode<>(variantUnionC);
        componentC.getLattice().add(latticeUnionC);

        new LatticeEdge<>(latticeC1, latticeC2);
        new LatticeEdge<>(latticeC1, latticeC3);
        new LatticeEdge<>(latticeC2, latticeUnionC);
        new LatticeEdge<>(latticeC3, latticeUnionC);

        CompareOptions options = new CompareOptions();
        options.entityType = componentEntityType;
        CompleteModelSetLattice completeModelSetLattice = new CompleteModelSetLattice(comparisonData, options);

        completeModelSetLattice.completeModelSetLattice(new ArrayList<>());

        // 4 input model variants + 1 overall intersection model variant + 1 overall union model variant + 1
        // intermediate intersection/union model variant.
        assertEquals(7, comparisonData.getModelSetVariants().size());

        // Each input model variant has a single parent intersection variant.
        assertEquals(1, latticeSet1.getParents().size());
        assertEquals(1, latticeSet2.getParents().size());
        assertEquals(1, latticeSet3.getParents().size());
        assertEquals(1, latticeSet4.getParents().size());

        // Each input model variant has a single child union variant.
        assertEquals(1, latticeSet1.getChildren().size());
        assertEquals(1, latticeSet2.getChildren().size());
        assertEquals(1, latticeSet3.getChildren().size());
        assertEquals(1, latticeSet4.getChildren().size());

        // Every lattice node refers to unique variant.
        assertTrue(comparisonData.getLattice().size() == comparisonData.getLattice().stream().map(l -> l.getValue())
                .distinct().count());
    }

    @Test
    /** Test with three variants of a component, where the union variants are over the size limit. */
    public void incompleteLatticeTest() {
        List<ModelSet> modelSets = new ArrayList<>();

        Model modelA1 = TestAutomata.createModelA(COMPONENT_A_NAME);
        Map<String, Model> models1 = Maps.map();
        models1.put(COMPONENT_A_NAME, modelA1);
        ModelSet modelSet1 = new ModelSet(COMPUTED_PREFIX + 1, models1, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSet1);

        Model modelA2 = TestAutomata.createModelB(COMPONENT_A_NAME);
        Map<String, Model> models2 = Maps.map();
        models2.put(COMPONENT_A_NAME, modelA2);
        ModelSet modelSet2 = new ModelSet(COMPUTED_PREFIX + 2, models2, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSet2);

        Model modelA3 = TestAutomata.createModelC(COMPONENT_A_NAME);
        Map<String, Model> models3 = Maps.map();
        models3.put(COMPONENT_A_NAME, modelA3);
        ModelSet modelSet3 = new ModelSet(COMPUTED_PREFIX + 3, models3, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSet3);
        ComparisonData comparisonData = new ComparisonData(modelSets);

        Variant<ModelSet> modelSetVariant1 = new Variant<ModelSet>(1, modelSet1,
                modelSet1.getNumberOfModelsWithBehavior(), false);
        comparisonData.getModelSetVariants().add(modelSetVariant1);
        modelSet1.setModelSetVariant(modelSetVariant1);
        LatticeNode<Variant<ModelSet>> latticeSet1 = new LatticeNode<>(modelSetVariant1);
        comparisonData.getLattice().add(latticeSet1);

        Variant<ModelSet> modelSetVariant2 = new Variant<ModelSet>(2, modelSet2,
                modelSet2.getNumberOfModelsWithBehavior(), false);
        comparisonData.getModelSetVariants().add(modelSetVariant2);
        modelSet2.setModelSetVariant(modelSetVariant2);
        LatticeNode<Variant<ModelSet>> latticeSet2 = new LatticeNode<>(modelSetVariant2);
        comparisonData.getLattice().add(latticeSet2);

        Variant<ModelSet> modelSetVariant3 = new Variant<ModelSet>(3, modelSet3,
                modelSet3.getNumberOfModelsWithBehavior(), false);
        comparisonData.getModelSetVariants().add(modelSetVariant3);
        modelSet3.setModelSetVariant(modelSetVariant3);
        LatticeNode<Variant<ModelSet>> latticeSet3 = new LatticeNode<>(modelSetVariant3);
        comparisonData.getLattice().add(latticeSet3);

        Entity component = comparisonData.getEntityByName(modelA1.getEntityName());

        Variant<Model> variantA1 = component.addVariant(modelA1, false);
        modelA1.setVariant(variantA1);
        LatticeNode<Variant<Model>> latticeA1 = new LatticeNode<>(variantA1);
        component.getLattice().add(latticeA1);
        Variant<Model> variantA2 = component.addVariant(modelA2, false);
        modelA2.setVariant(variantA2);
        LatticeNode<Variant<Model>> latticeA2 = new LatticeNode<>(variantA2);
        component.getLattice().add(latticeA2);
        Variant<Model> variantA3 = component.addVariant(modelA3, false);
        modelA3.setVariant(variantA3);
        LatticeNode<Variant<Model>> latticeA3 = new LatticeNode<>(variantA3);
        component.getLattice().add(latticeA3);

        Model modelIntersection = TestAutomata.createModelEmpty(COMPONENT_A_NAME);
        Variant<Model> variantIntersection = component.addVariant(modelIntersection, true);
        modelIntersection.setVariant(variantIntersection);
        LatticeNode<Variant<Model>> latticeIntersection = new LatticeNode<>(variantIntersection);
        component.getLattice().add(latticeIntersection);
        new LatticeEdge<>(latticeIntersection, latticeA1);
        new LatticeEdge<>(latticeIntersection, latticeA2);
        new LatticeEdge<>(latticeIntersection, latticeA3);

        Model modelUnion12 = TestAutomata.createModelBig(COMPONENT_A_NAME);
        Variant<Model> variantUnion12 = component.addVariant(modelUnion12, true);
        modelUnion12.setVariant(variantUnion12);
        LatticeNode<Variant<Model>> latticeUnion12 = new LatticeNode<>(variantUnion12);
        latticeUnion12.setLatticeIncomplete();
        component.getLattice().add(latticeUnion12);
        new LatticeEdge<>(latticeA1, latticeUnion12);
        new LatticeEdge<>(latticeA2, latticeUnion12);

        Model modelUnion13 = TestAutomata.createModelBig(COMPONENT_A_NAME);
        Variant<Model> variantUnion13 = component.addVariant(modelUnion13, true);
        modelUnion13.setVariant(variantUnion13);
        LatticeNode<Variant<Model>> latticeUnion13 = new LatticeNode<>(variantUnion13);
        latticeUnion13.setLatticeIncomplete();
        component.getLattice().add(latticeUnion13);
        new LatticeEdge<>(latticeA1, latticeUnion13);
        new LatticeEdge<>(latticeA3, latticeUnion13);

        Model modelUnion23 = TestAutomata.createModelBig(COMPONENT_A_NAME);
        Variant<Model> variantUnion23 = component.addVariant(modelUnion23, true);
        modelUnion23.setVariant(variantUnion23);
        LatticeNode<Variant<Model>> latticeUnion23 = new LatticeNode<>(variantUnion23);
        latticeUnion23.setLatticeIncomplete();
        component.getLattice().add(latticeUnion23);
        new LatticeEdge<>(latticeA2, latticeUnion23);
        new LatticeEdge<>(latticeA3, latticeUnion23);

        CompareOptions options = new CompareOptions();
        options.entityType = componentEntityType;
        CompleteModelSetLattice completeModelSetLattice = new CompleteModelSetLattice(comparisonData, options);

        completeModelSetLattice.completeModelSetLattice(new ArrayList<>());

        // 3 input model set variants + 3 union model set variants + 1 intersection model set variant.
        assertEquals(7, comparisonData.getModelSetVariants().size());

        // All input model set variants share a parent intersection variants.
        assertEquals(1, latticeSet1.getParents().size());
        assertEquals(1, latticeSet2.getParents().size());
        assertEquals(1, latticeSet3.getParents().size());

        // Each input model set variant has 2 union model set variants as children.
        assertEquals(2, latticeSet1.getChildren().size());
        assertEquals(2, latticeSet2.getChildren().size());
        assertEquals(2, latticeSet3.getChildren().size());

        // The union model set variants have no children.
        LatticeNode<Variant<ModelSet>> latticeSetUnion12 = latticeSet1.findCommonDescendant(latticeSet2);
        assertNotNull(latticeSetUnion12);
        assertEquals(0, latticeSetUnion12.getChildren().size());

        LatticeNode<Variant<ModelSet>> latticeSetUnion13 = latticeSet1.findCommonDescendant(latticeSet3);
        assertNotNull(latticeSetUnion13);
        assertEquals(0, latticeSetUnion13.getChildren().size());

        LatticeNode<Variant<ModelSet>> latticeSetUnion23 = latticeSet2.findCommonDescendant(latticeSet3);
        assertNotNull(latticeSetUnion23);
        assertEquals(0, latticeSetUnion23.getChildren().size());
    }
}
