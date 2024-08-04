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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.escet.common.java.Maps;
import org.junit.jupiter.api.Test;

import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.data.LatticeNode;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.data.Variant;
import nl.tno.mids.compare.options.EntityType;
import nl.tno.mids.compare.options.ModelType;
import nl.tno.mids.compare.util.TestAutomata;

class ComputeModelLatticeTest {
    private static final String COMPUTED_PREFIX = "computed_";

    private static final String COMPONENT_A_NAME = "A";

    private static final String COMPONENT_B_NAME = "B";

    private static final EntityType componentEntityType = new EntityType("component");

    /**
     * Test with one component with two variants where the first is included in the second.
     * 
     * The computed lattice should have two elements and the first variant should be a parent of the second.
     * 
     * @throws IOException In case of an I/O error.
     */
    @Test
    public void simpleLatticeTest() throws IOException {
        List<ModelSet> modelSets = new ArrayList<>();

        {
            Model modelA1 = TestAutomata.createModelA(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA1);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 1, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }
        {
            Model modelA2 = TestAutomata.createModelAB(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA2);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 2, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }
        ComparisonData comparisonData = new ComparisonData(modelSets);

        for (ModelSet modelSet: comparisonData.getModelSets()) {
            for (Model model: modelSet.getModels()) {
                Entity component = comparisonData.getEntityByName(model.getEntityName());
                Variant<Model> variant = component.addVariant(model, false);
                model.setVariant(variant);
            }
        }

        ComputeModelLattice computeComponentLattice = new ComputeModelLattice(comparisonData);
        computeComponentLattice.computeLattices(new NullProgressMonitor());

        Entity componentA = comparisonData.getEntityByName(COMPONENT_A_NAME);

        assertEquals(2, componentA.getLattice().size());

        Optional<LatticeNode<Variant<Model>>> latticeNode1 = componentA.getLattice().stream()
                .filter(n -> !n.getChildren().isEmpty()).findFirst();
        Optional<LatticeNode<Variant<Model>>> latticeNode2 = componentA.getLattice().stream()
                .filter(n -> !n.getParents().isEmpty()).findFirst();

        assertTrue(latticeNode1.isPresent(), "Expected parent node not found");
        assertTrue(latticeNode2.isPresent(), "Expected child node not found");

        LatticeNode<Variant<Model>> parentNode = latticeNode1.get();
        LatticeNode<Variant<Model>> childNode = latticeNode2.get();

        assertTrue(childNode.getParents().contains(parentNode), "Expected parent not found");
        assertTrue(parentNode.getChildren().contains(childNode), "Expected child not found");
    }

    /**
     * Test with one component with two variants where the first is empty, and thus included in the second.
     * 
     * The computed lattice should have two elements and the first variant should be a parent of the second.
     * 
     * @throws IOException In case of an I/O error.
     */
    @Test
    public void emptyModelTest() throws IOException {
        List<ModelSet> modelSets = new ArrayList<>();

        {
            Model modelA1 = TestAutomata.createModelEmpty(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA1);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 1, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }
        {
            Model modelA2 = TestAutomata.createModelAB(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA2);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 2, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }
        ComparisonData comparisonData = new ComparisonData(modelSets);

        for (ModelSet modelSet: comparisonData.getModelSets()) {
            for (Model model: modelSet.getModels()) {
                Entity component = comparisonData.getEntityByName(model.getEntityName());
                Variant<Model> variant = component.addVariant(model, false);
                model.setVariant(variant);
            }
        }

        ComputeModelLattice computeComponentLattice = new ComputeModelLattice(comparisonData);
        computeComponentLattice.computeLattices(new NullProgressMonitor());

        Entity componentA = comparisonData.getEntityByName(COMPONENT_A_NAME);

        assertEquals(2, componentA.getLattice().size());

        Optional<LatticeNode<Variant<Model>>> latticeNode1 = componentA.getLattice().stream()
                .filter(n -> !n.getChildren().isEmpty()).findFirst();
        Optional<LatticeNode<Variant<Model>>> latticeNode2 = componentA.getLattice().stream()
                .filter(n -> !n.getParents().isEmpty()).findFirst();

        assertTrue(latticeNode1.isPresent(), "Expected parent node not found");
        assertTrue(latticeNode2.isPresent(), "Expected child node not found");

        LatticeNode<Variant<Model>> parentNode = latticeNode1.get();
        LatticeNode<Variant<Model>> childNode = latticeNode2.get();

        assertTrue(childNode.getParents().contains(parentNode), "Expected parent not found");
        assertTrue(parentNode.getChildren().contains(childNode), "Expected child not found");
    }

    /**
     * Test with one component with two variants that are unrelated.
     * 
     * The computed lattice should have two elements and neither variant should have parents or children.
     * 
     * @throws IOException In case of an I/O error.
     */
    @Test
    public void noLatticeTest() throws IOException {
        List<ModelSet> modelSets = new ArrayList<>();

        {
            Model modelA1 = TestAutomata.createModelA(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA1);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 1, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        {
            Model modelA2 = TestAutomata.createModelB(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA2);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 2, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        ComparisonData comparisonData = new ComparisonData(modelSets);

        for (ModelSet modelSet: comparisonData.getModelSets()) {
            for (Model model: modelSet.getModels()) {
                Entity component = comparisonData.getEntityByName(model.getEntityName());
                Variant<Model> variant = component.addVariant(model, false);
                model.setVariant(variant);
            }
        }

        ComputeModelLattice computeComponentLattice = new ComputeModelLattice(comparisonData);
        computeComponentLattice.computeLattices(new NullProgressMonitor());

        Entity componentA = comparisonData.getEntityByName(COMPONENT_A_NAME);

        assertEquals(2, componentA.getLattice().size());

        assertTrue(componentA.getLattice().stream().allMatch(n -> n.getChildren().isEmpty()),
                "Unexpected parent node found");
        assertTrue(componentA.getLattice().stream().allMatch(n -> n.getParents().isEmpty()),
                "Unexpected child node found");
    }

    /**
     * Test with two components with two variants each where one variant is included in the other.
     * 
     * The computed lattice should have four elements with one parent-child relation for each component. Variants of one
     * component should not be related to variants of the other component.
     * 
     * @throws IOException In case of an I/O error.
     */
    @Test
    public void separateLatticeTest() throws IOException {
        List<ModelSet> modelSets = new ArrayList<>();

        {
            Model modelA1 = TestAutomata.createModelA(COMPONENT_A_NAME);
            Model modelB1 = TestAutomata.createModelAB(COMPONENT_B_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA1);
            models.put(COMPONENT_B_NAME, modelB1);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 1, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        {
            Model modelA2 = TestAutomata.createModelAB(COMPONENT_A_NAME);
            Model modelB2 = TestAutomata.createModelA(COMPONENT_B_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA2);
            models.put(COMPONENT_B_NAME, modelB2);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 2, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        ComparisonData comparisonData = new ComparisonData(modelSets);

        for (ModelSet modelSet: comparisonData.getModelSets()) {
            for (Model model: modelSet.getModels()) {
                Entity component = comparisonData.getEntityByName(model.getEntityName());
                Variant<Model> variant = component.addVariant(model, false);
                model.setVariant(variant);
            }
        }

        ComputeModelLattice computeComponentLattice = new ComputeModelLattice(comparisonData);
        computeComponentLattice.computeLattices(new NullProgressMonitor());

        Entity componentA = comparisonData.getEntityByName(COMPONENT_A_NAME);

        assertEquals(2, componentA.getLattice().size());

        Optional<LatticeNode<Variant<Model>>> latticeNodeA1 = componentA.getLattice().stream()
                .filter(n -> !n.getChildren().isEmpty()).findFirst();
        Optional<LatticeNode<Variant<Model>>> latticeNodeA2 = componentA.getLattice().stream()
                .filter(n -> !n.getParents().isEmpty()).findFirst();

        assertTrue(latticeNodeA1.isPresent(), "Expected parent node not found");
        assertTrue(latticeNodeA2.isPresent(), "Expected child node not found");

        LatticeNode<Variant<Model>> parentNodeA = latticeNodeA1.get();
        LatticeNode<Variant<Model>> childNodeA = latticeNodeA2.get();

        assertTrue(childNodeA.getParents().contains(parentNodeA), "Expected parent not found");
        assertEquals(1, childNodeA.getParents().size(), "Unexpected parents found");
        assertTrue(parentNodeA.getChildren().contains(childNodeA), "Expected child not found");
        assertEquals(1, parentNodeA.getChildren().size(), "Unexpected children found");

        Entity componentB = comparisonData.getEntityByName(COMPONENT_B_NAME);

        assertEquals(2, componentB.getLattice().size());

        Optional<LatticeNode<Variant<Model>>> latticeNodeB1 = componentB.getLattice().stream()
                .filter(n -> !n.getChildren().isEmpty()).findFirst();
        Optional<LatticeNode<Variant<Model>>> latticeNodeB2 = componentB.getLattice().stream()
                .filter(n -> !n.getParents().isEmpty()).findFirst();

        assertTrue(latticeNodeB1.isPresent(), "Expected parent node not found");
        assertTrue(latticeNodeB2.isPresent(), "Expected child node not found");

        LatticeNode<Variant<Model>> parentNodeB = latticeNodeB1.get();
        LatticeNode<Variant<Model>> childNodeB = latticeNodeB2.get();

        assertTrue(childNodeB.getParents().contains(parentNodeB), "Expected parent not found");
        assertEquals(1, childNodeB.getParents().size(), "Unexpected parents found");
        assertTrue(parentNodeB.getChildren().contains(childNodeB), "Expected child not found");
        assertEquals(1, parentNodeB.getChildren().size(), "Unexpected children found");
    }

    /**
     * Test with one component with three variants where two variants are included in the third, but not related to each
     * other.
     * 
     * The computed lattice should have three elements and two variants should be parents of the third.
     * 
     * @throws IOException In case of an I/O error.
     */
    @Test
    public void triangleLatticeTest() throws IOException {
        List<ModelSet> modelSets = new ArrayList<>();

        {
            Model modelA1 = TestAutomata.createModelA(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA1);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 1, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        {
            Model modelA2 = TestAutomata.createModelB(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA2);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 2, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        {
            Model modelA3 = TestAutomata.createModelAB(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA3);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 3, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        ComparisonData comparisonData = new ComparisonData(modelSets);

        for (ModelSet modelSet: comparisonData.getModelSets()) {
            for (Model model: modelSet.getModels()) {
                Entity component = comparisonData.getEntityByName(model.getEntityName());
                Variant<Model> variant = component.addVariant(model, false);
                model.setVariant(variant);
            }
        }

        ComputeModelLattice computeComponentLattice = new ComputeModelLattice(comparisonData);
        computeComponentLattice.computeLattices(new NullProgressMonitor());

        Entity componentA = comparisonData.getEntityByName(COMPONENT_A_NAME);

        assertEquals(3, componentA.getVariants().size());

        assertEquals(2, componentA.getLattice().stream().filter(n -> !n.getChildren().isEmpty()).count(),
                "Unexpected parents found");
        assertEquals(1, componentA.getLattice().stream().filter(n -> !n.getParents().isEmpty()).count(),
                "Unexpected children found");

        assertTrue(componentA.getLattice().stream().filter(n -> !n.getChildren().isEmpty())
                .allMatch(n -> n.getChildren().size() == 1), "Unexpected parents found");
        assertTrue(componentA.getLattice().stream().filter(n -> !n.getParents().isEmpty())
                .allMatch(n -> n.getParents().size() == 2), "Unexpected parents found");
    }

    /**
     * Test with one component with three variants where the first is included in the second and the second in the
     * third.
     * 
     * The computed lattice should have three elements, the first variant should be a parent of the second and the
     * second should be a parent of the third.
     * 
     * @throws IOException In case of an I/O error.
     */
    @Test
    public void chainLatticeTest() throws IOException {
        List<ModelSet> modelSets = new ArrayList<>();

        {
            Model modelA1 = TestAutomata.createModelA(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA1);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 1, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        {
            Model modelA2 = TestAutomata.createModelAB(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA2);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 2, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        {
            Model modelA3 = TestAutomata.createModelD(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA3);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 2, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        ComparisonData comparisonData = new ComparisonData(modelSets);

        for (ModelSet modelSet: comparisonData.getModelSets()) {
            for (Model model: modelSet.getModels()) {
                Entity component = comparisonData.getEntityByName(model.getEntityName());
                Variant<Model> variant = component.addVariant(model, false);
                model.setVariant(variant);
            }
        }

        ComputeModelLattice computeComponentLattice = new ComputeModelLattice(comparisonData);
        computeComponentLattice.computeLattices(new NullProgressMonitor());

        Entity componentA = comparisonData.getEntityByName(COMPONENT_A_NAME);

        assertEquals(3, componentA.getVariants().size());

        assertEquals(1, componentA.getLattice().stream()
                .filter(n -> n.getParents().isEmpty() && !n.getChildren().isEmpty()).count(),
                "Expected grandparent not found");
        assertEquals(1, componentA.getLattice().stream()
                .filter(n -> !n.getParents().isEmpty() && !n.getChildren().isEmpty()).count(),
                "Expected parent not found");
        assertEquals(1, componentA.getLattice().stream()
                .filter(n -> !n.getParents().isEmpty() && n.getChildren().isEmpty()).count(),
                "Expected child not found");
        assertTrue(
                componentA.getLattice().stream()
                        .allMatch(n -> n.getParents().size() <= 1 && n.getChildren().size() <= 1),
                "Unexpected parent-child edge found");
    }

    /**
     * Test with one component with three variants where two variants include the third, but not related to each other.
     * 
     * The computed lattice should have three elements and two variants should be children of the third.
     * 
     * @throws IOException In case of an I/O error.
     */
    @Test
    public void reverseTriangleLatticeTest() throws IOException {
        List<ModelSet> modelSets = new ArrayList<>();

        {
            Model modelA1 = TestAutomata.createModelA(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA1);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 1, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        {
            Model modelA2 = TestAutomata.createModelB(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA2);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 2, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        {
            Model modelA3 = TestAutomata.createModelEmpty(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA3);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 2, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        ComparisonData comparisonData = new ComparisonData(modelSets);

        for (ModelSet modelSet: comparisonData.getModelSets()) {
            for (Model model: modelSet.getModels()) {
                Entity component = comparisonData.getEntityByName(model.getEntityName());
                Variant<Model> variant = component.addVariant(model, false);
                model.setVariant(variant);
            }
        }

        ComputeModelLattice computeComponentLattice = new ComputeModelLattice(comparisonData);
        computeComponentLattice.computeLattices(new NullProgressMonitor());

        Entity componentA = comparisonData.getEntityByName(COMPONENT_A_NAME);

        assertEquals(3, componentA.getVariants().size());

        assertEquals(1, componentA.getLattice().stream().filter(n -> !n.getChildren().isEmpty()).count(),
                "Unexpected parents found");
        assertEquals(2, componentA.getLattice().stream().filter(n -> !n.getParents().isEmpty()).count(),
                "Unexpected children found");

        assertTrue(componentA.getLattice().stream().filter(n -> !n.getChildren().isEmpty())
                .allMatch(n -> n.getChildren().size() == 2), "Unexpected children found");
        assertTrue(componentA.getLattice().stream().filter(n -> !n.getParents().isEmpty())
                .allMatch(n -> n.getParents().size() == 1), "Unexpected parents found");
    }

    /**
     * Test with one component with four variants where two variants are included in the third and include the fourth,
     * but not related to each other.
     * 
     * The computed lattice should have four elements and two variants should be parents of the third and children of
     * the fourth. The third and fourth variant should have no direct relation.
     * 
     * @throws IOException In case of an I/O error.
     */
    @Test
    public void diamondLatticeTest() throws IOException {
        List<ModelSet> modelSets = new ArrayList<>();

        {
            Model modelA1 = TestAutomata.createModelA(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA1);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 1, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        {
            Model modelA2 = TestAutomata.createModelB(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA2);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 2, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        {
            Model modelA3 = TestAutomata.createModelEmpty(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA3);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 3, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        {
            Model modelA4 = TestAutomata.createModelAB(COMPONENT_A_NAME);

            Map<String, Model> models = Maps.map();
            models.put(COMPONENT_A_NAME, modelA4);
            ModelSet modelSet = new ModelSet(COMPUTED_PREFIX + 4, models, ModelType.CMI, componentEntityType, null);
            modelSets.add(modelSet);
        }

        ComparisonData comparisonData = new ComparisonData(modelSets);

        for (ModelSet modelSet: comparisonData.getModelSets()) {
            for (Model model: modelSet.getModels()) {
                Entity component = comparisonData.getEntityByName(model.getEntityName());
                Variant<Model> variant = component.addVariant(model, false);
                model.setVariant(variant);
            }
        }

        ComputeModelLattice computeComponentLattice = new ComputeModelLattice(comparisonData);
        computeComponentLattice.computeLattices(new NullProgressMonitor());

        Entity componentA = comparisonData.getEntityByName(COMPONENT_A_NAME);

        assertEquals(4, componentA.getVariants().size());

        assertEquals(3, componentA.getLattice().stream().filter(n -> !n.getChildren().isEmpty()).count(),
                "Unexpected parents found");
        assertEquals(3, componentA.getLattice().stream().filter(n -> !n.getParents().isEmpty()).count(),
                "Unexpected children found");

        assertTrue(componentA.getLattice().stream().allMatch(n -> n.getChildren().size() + n.getParents().size() == 2),
                "Unexpected lattice found found");
        assertEquals(1, componentA.getLattice().stream().filter(n -> n.getChildren().size() == 2).count(),
                "Unexpected parent found");
        assertEquals(1, componentA.getLattice().stream().filter(n -> n.getParents().size() == 2).count(),
                "Unexpected child found");
    }

    /**
     * Test that the simple automaton is covered under language inclusion by an identical automaton.
     */
    @Test
    public void testCompareLanguageInclusionBasic() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME);
        Model modelRight = TestAutomata.createModelA(COMPONENT_A_NAME);

        boolean compareResult = ComputeModelLattice.compareLanguageInclusion(modelLeft.getLanguageAutomaton(),
                modelRight.getLanguageAutomaton());

        assertTrue(compareResult);
    }

    /**
     * Test that the simple automaton is covered under language inclusion by a flower automaton with the same alphabet.
     */
    @Test
    public void testCompareLanguageInclusionLessStates() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME);
        Model modelRight = TestAutomata.createModelD(COMPONENT_A_NAME);

        boolean compareResult = ComputeModelLattice.compareLanguageInclusion(modelLeft.getLanguageAutomaton(),
                modelRight.getLanguageAutomaton());

        assertTrue(compareResult);
    }

    /**
     * Test that a flower automaton is not covered under language inclusion by a simple automaton with the same
     * alphabet.
     */
    @Test
    public void testCompareLanguageInclusionLessStatesReversed() {
        Model modelLeft = TestAutomata.createModelD(COMPONENT_A_NAME);
        Model modelRight = TestAutomata.createModelA(COMPONENT_A_NAME);

        boolean compareResult = ComputeModelLattice.compareLanguageInclusion(modelLeft.getLanguageAutomaton(),
                modelRight.getLanguageAutomaton());

        assertFalse(compareResult);
    }

    /**
     * Test that the simple automaton is not covered under language inclusion by a larger automaton with the same
     * alphabet.
     */
    @Test
    public void testCompareLanguageInclusionMoreStates() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME);
        Model modelRight = TestAutomata.createModelB(COMPONENT_A_NAME);

        boolean compareResult = ComputeModelLattice.compareLanguageInclusion(modelLeft.getLanguageAutomaton(),
                modelRight.getLanguageAutomaton());

        assertFalse(compareResult);
    }

    /**
     * Test that the larger automaton is not covered under language inclusion by a simple automaton with the same
     * alphabet.
     */
    @Test
    public void testCompareLanguageInclusionMoreStatesReversed() {
        Model modelLeft = TestAutomata.createModelB(COMPONENT_A_NAME);
        Model modelRight = TestAutomata.createModelA(COMPONENT_A_NAME);

        boolean compareResult = ComputeModelLattice.compareLanguageInclusion(modelLeft.getLanguageAutomaton(),
                modelRight.getLanguageAutomaton());

        assertFalse(compareResult);
    }

    /**
     * Test that the simple automaton is not covered under language inclusion by an automaton with a different alphabet.
     */
    @Test
    public void testCompareLanguageInclusionDifferentAlphabet() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME);
        Model modelRight = TestAutomata.createModelC(COMPONENT_A_NAME);

        boolean compareResult = ComputeModelLattice.compareLanguageInclusion(modelLeft.getLanguageAutomaton(),
                modelRight.getLanguageAutomaton());

        assertFalse(compareResult);
    }
}
