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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.escet.common.java.Maps;
import org.junit.jupiter.api.Test;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.fsa.NFAs;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.options.EntityType;
import nl.tno.mids.compare.options.ModelType;
import nl.tno.mids.compare.util.TestAutomata;
import nl.tno.mids.gltsdiff.extensions.AnnotatedProperty;

class ComputeVariantsTest {
    private static final String COMPUTED_PREFIX = "computed_";

    private static final String COMPONENT_A_NAME = "A";

    private static final String COMPONENT_B_NAME = "B";

    private static final String COMPONENT_C_NAME = "C";

    private static final EntityType componentEntityType = new EntityType("component");

    /**
     * Test comparing two model sets with two components each.
     * 
     * For both components, two component variants should be computed.
     * 
     * For the model sets, two model set variants should be computed.
     */
    @Test
    public void baseVariantTest() {
        List<ModelSet> modelSets = new ArrayList<>();

        Map<String, Model> modelSet1 = Maps.map();
        modelSet1.put(COMPONENT_A_NAME, TestAutomata.createModelA(COMPONENT_A_NAME));
        modelSet1.put(COMPONENT_B_NAME, TestAutomata.createModelA(COMPONENT_B_NAME));

        ModelSet modelSetX = new ModelSet(COMPUTED_PREFIX + 1, modelSet1, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSetX);

        Map<String, Model> modelSet2 = Maps.map();
        modelSet2.put(COMPONENT_A_NAME, TestAutomata.createModelB(COMPONENT_A_NAME));
        modelSet2.put(COMPONENT_B_NAME, TestAutomata.createModelB(COMPONENT_B_NAME));

        ModelSet modelSetY = new ModelSet(COMPUTED_PREFIX + 2, modelSet2, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSetY);

        ComparisonData comparisonData = new ComparisonData(modelSets);

        ComputeVariants.computeEntityVariants(comparisonData);

        assertEquals(comparisonData.getEntities().size(), 2);

        for (Entity component: comparisonData.getEntities()) {
            assertEquals(component.getVariants().size(), 2);
        }

        ComputeVariants.computeModelSetVariants(comparisonData);
    }

    /**
     * Test comparing three model sets with two components each, where the first and third model set have the same
     * models.
     * 
     * For both components, two component variants should be computed.
     * 
     * For the model sets, two model set variants should be computed.
     */
    @Test
    public void repeatedVariantTest() {
        List<ModelSet> modelSets = new ArrayList<>();

        Map<String, Model> modelSet1 = Maps.map();
        modelSet1.put(COMPONENT_A_NAME, TestAutomata.createModelA(COMPONENT_A_NAME));
        modelSet1.put(COMPONENT_B_NAME, TestAutomata.createModelA(COMPONENT_B_NAME));

        ModelSet modelSetX = new ModelSet(COMPUTED_PREFIX + 1, modelSet1, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSetX);

        Map<String, Model> modelSet2 = Maps.map();
        modelSet2.put(COMPONENT_A_NAME, TestAutomata.createModelB(COMPONENT_A_NAME));
        modelSet2.put(COMPONENT_B_NAME, TestAutomata.createModelB(COMPONENT_B_NAME));

        ModelSet modelSetY = new ModelSet(COMPUTED_PREFIX + 2, modelSet2, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSetY);

        Map<String, Model> modelSet3 = Maps.map();
        modelSet3.put(COMPONENT_A_NAME, TestAutomata.createModelA(COMPONENT_A_NAME));
        modelSet3.put(COMPONENT_B_NAME, TestAutomata.createModelA(COMPONENT_B_NAME));

        ModelSet modelSetZ = new ModelSet(COMPUTED_PREFIX + 3, modelSet3, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSetZ);

        ComparisonData comparisonData = new ComparisonData(modelSets);

        ComputeVariants.computeEntityVariants(comparisonData);

        assertEquals(2, comparisonData.getEntities().size(), "Unexpected number of components.");

        for (Entity component: comparisonData.getEntities()) {
            assertEquals(2, component.getVariants().size(), "Unexpected number of variants.");
        }

        ComputeVariants.computeModelSetVariants(comparisonData);

        assertEquals(2, comparisonData.getModelSetVariants().size(), "Unexpected number of model set variants.");
    }

    /**
     * Test comparing two model sets, one with two components with behavior and one with one component with behavior and
     * one without.
     * 
     * For both components, two component variants should be computed.
     * 
     * For the model sets, two model set variants should be computed.
     */
    @Test
    public void missingComponentBehaviorTest() {
        List<ModelSet> modelSets = new ArrayList<>();

        Map<String, Model> modelSet1 = Maps.map();
        modelSet1.put(COMPONENT_A_NAME, TestAutomata.createModelA(COMPONENT_A_NAME));
        modelSet1.put(COMPONENT_B_NAME, TestAutomata.createModelA(COMPONENT_B_NAME));

        ModelSet modelSetX = new ModelSet(COMPUTED_PREFIX + 1, modelSet1, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSetX);

        Map<String, Model> modelSet2 = Maps.map();
        modelSet2.put(COMPONENT_A_NAME, TestAutomata.createModelB(COMPONENT_A_NAME));
        modelSet2.put(COMPONENT_B_NAME, TestAutomata.createModelEmpty(COMPONENT_B_NAME));

        ModelSet modelSetY = new ModelSet(COMPUTED_PREFIX + 2, modelSet2, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSetY);

        ComparisonData comparisonData = new ComparisonData(modelSets);

        ComputeVariants.computeEntityVariants(comparisonData);

        assertEquals(comparisonData.getEntities().size(), 2);

        assertEquals(2, comparisonData.getEntityByName(COMPONENT_A_NAME).getVariants().size());
        assertEquals(2, comparisonData.getEntityByName(COMPONENT_B_NAME).getVariants().size());
        assertEquals(1, comparisonData.getEntityByName(COMPONENT_B_NAME).getVariantsWithBehavior().size());

        ComputeVariants.computeModelSetVariants(comparisonData);

        assertEquals(comparisonData.getModelSetVariants().size(), 2);
    }

    /**
     * Test comparing two model sets: one with two components with behavior and one without, and one with three
     * components with behavior.
     * 
     * For all components, two component variants should be computed. For the component without behavior in one of the
     * input sets, only one variant with behavior should be computed.
     * 
     * For the model sets, two model set variants should be computed.
     */
    @Test
    public void extraComponentTest() {
        List<ModelSet> modelSets = new ArrayList<>();

        Map<String, Model> modelSet1 = Maps.map();
        modelSet1.put(COMPONENT_A_NAME, TestAutomata.createModelA(COMPONENT_A_NAME));
        modelSet1.put(COMPONENT_B_NAME, TestAutomata.createModelA(COMPONENT_B_NAME));
        modelSet1.put(COMPONENT_C_NAME, TestAutomata.createModelEmpty(COMPONENT_C_NAME));

        ModelSet modelSetX = new ModelSet(COMPUTED_PREFIX + 1, modelSet1, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSetX);

        Map<String, Model> modelSet2 = Maps.map();
        modelSet2.put(COMPONENT_A_NAME, TestAutomata.createModelB(COMPONENT_A_NAME));
        modelSet2.put(COMPONENT_B_NAME, TestAutomata.createModelB(COMPONENT_B_NAME));
        modelSet2.put(COMPONENT_C_NAME, TestAutomata.createModelB(COMPONENT_C_NAME));

        ModelSet modelSety = new ModelSet(COMPUTED_PREFIX + 2, modelSet2, ModelType.CMI, componentEntityType, null);
        modelSets.add(modelSety);

        ComparisonData comparisonData = new ComparisonData(modelSets);

        ComputeVariants.computeEntityVariants(comparisonData);

        assertEquals(comparisonData.getEntities().size(), 3);

        assertEquals(2, comparisonData.getEntityByName(COMPONENT_A_NAME).getVariants().size());
        assertEquals(2, comparisonData.getEntityByName(COMPONENT_B_NAME).getVariants().size());
        assertEquals(2, comparisonData.getEntityByName(COMPONENT_C_NAME).getVariants().size());
        assertEquals(1, comparisonData.getEntityByName(COMPONENT_C_NAME).getVariantsWithBehavior().size());

        ComputeVariants.computeModelSetVariants(comparisonData);

        assertEquals(comparisonData.getModelSetVariants().size(), 2);
    }

    /**
     * Test comparing zero model sets.
     * 
     * No component variants should be computed.
     * 
     * No model set variants should be computed.
     */
    @Test
    public void emptyVariantTest() {
        List<ModelSet> modelSets = new ArrayList<>();

        ComparisonData comparisonData = new ComparisonData(modelSets);

        ComputeVariants.computeEntityVariants(comparisonData);

        assertEquals(comparisonData.getEntities().size(), 0);

        ComputeVariants.computeModelSetVariants(comparisonData);

        assertEquals(comparisonData.getModelSetVariants().size(), 0);
    }

    /**
     * Test that a simple automaton is language equivalent to an identical automaton.
     */
    @Test
    public void testCompareLanguagePartialBasicEquality() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME);
        Model modelRight = TestAutomata.createModelA(COMPONENT_A_NAME);

        assertTrue(ComputeVariants.areLanguageEquivalent(modelLeft.getLanguageAutomaton(),
                modelRight.getLanguageAutomaton()));
    }

    /**
     * Test that a simple automaton is language equivalent to an identical automaton with an extra 'tau' event.
     */
    @Test
    public void testCompareLanguageEquivalencWeakTraceNormalize() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME); // accepts '(a.b)*'
        Model modelRight = TestAutomata.createModelAWithTau(COMPONENT_A_NAME); // accepts (a.tau.b)*'

        CompactDFA<String> dfaWithTau = NFAs.determinize(
                AutomataLibUtil.rename(modelRight.getStructuralAutomaton(), AnnotatedProperty::getProperty), true,
                false);

        assertFalse(ComputeVariants.areLanguageEquivalent(modelLeft.getLanguageAutomaton(), dfaWithTau));
        assertTrue(ComputeVariants.areLanguageEquivalent(modelLeft.getLanguageAutomaton(),
                modelRight.getLanguageAutomaton()));
    }

    /**
     * Test that a simple automaton is not language equivalent to a flower automaton with the same alphabet.
     */
    @Test
    public void testCompareLanguageEquivalenceLessStates() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME); // accepts '(a.b)*'.
        Model modelRight = TestAutomata.createModelD(COMPONENT_A_NAME); // accepts '(a|b)*', thus accepts also 'a' and
                                                                        // 'b'.

        assertFalse(ComputeVariants.areLanguageEquivalent(modelLeft.getLanguageAutomaton(),
                modelRight.getLanguageAutomaton()));
    }

    /**
     * Test that a simple automaton is not language equivalent to an automaton with more states with the same alphabet.
     */
    @Test
    public void testCompareLanguageEquivalencMoreStates() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME); // accepts '(a.b)*'.
        Model modelRight = TestAutomata.createModelB(COMPONENT_A_NAME); // accepts '(a.b.b)*'.

        assertFalse(ComputeVariants.areLanguageEquivalent(modelLeft.getLanguageAutomaton(),
                modelRight.getLanguageAutomaton()));
    }

    @Test
    /**
     * Test that a simple automaton is not language equivalent to an automaton with a different alphabet.
     */
    public void testCompareLanguageEquivalencDifferentAlphabet() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME); // accepts '(a.b)*'.
        Model modelRight = TestAutomata.createModelC(COMPONENT_A_NAME); // accepts '(a.c)*'.

        assertFalse(ComputeVariants.areLanguageEquivalent(modelLeft.getLanguageAutomaton(),
                modelRight.getLanguageAutomaton()));
    }

    /**
     * Test that a simple automaton is not language equivalent to an automaton with a large number of states.
     */
    @Test
    public void testCompareLanguageEquivalencBigAutomaton() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME); // accepts '(a.b)*'.
        Model modelRight = TestAutomata.createModelBig(COMPONENT_A_NAME); // accepts '(a^BIG_AUTOMATON_SIZE)*'.

        assertFalse(ComputeVariants.areLanguageEquivalent(modelLeft.getLanguageAutomaton(),
                modelRight.getLanguageAutomaton()));
    }
}
