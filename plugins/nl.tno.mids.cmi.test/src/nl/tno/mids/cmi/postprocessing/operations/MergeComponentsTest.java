/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import nl.tno.mids.cmi.api.basic.CmiBasicComponentQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralComponentQueries;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;

class MergeComponentsTest extends BasePostProcessingOperationTest<MergeComponents> {
    @ParameterizedTest
    @ValueSource(strings =
    {"DifferentFragments", "MixedTracedUntraced", "MultipleTE", "Plain", "TaskExecutorAsClient", "Three",
            "UniqueFragments", "Untraced", "Variants", "VariantsTE"})
    void test(String testMethod) throws IOException {
        super.performTest(testMethod);

        List<String> possibleOutputs = Arrays.asList("C4TE", "C4TE_1", "C4TE_2", "C4TE_untraced", "C6TE");
        assertTrue(possibleOutputs.stream().anyMatch(po -> postProcessingModels.containsKey(po)),
                "Output does not contain any known output: possibleOutputs=" + possibleOutputs + ", actualOutputs="
                        + postProcessingModels.keySet());

        for (String possibleOutput: possibleOutputs) {
            if (postProcessingModels.containsKey(possibleOutput)) {
                PostProcessingModel resultModel = postProcessingModels.get(possibleOutput);

                Automaton resultAutomaton = CmiBasicComponentQueries
                        .getSingleComponentWithBehavior(resultModel.getCifSpec());
                assertEquals(possibleOutput, CmiGeneralComponentQueries.getComponentName(resultAutomaton));
            }
        }
    }

    @Override
    protected MergeComponents getOperation() {
        MergeComponentsOptions options = new MergeComponentsOptions();
        options.pattern = "(?<name>\\w+TE)\\d+";
        return new MergeComponents(options);
    }
}
