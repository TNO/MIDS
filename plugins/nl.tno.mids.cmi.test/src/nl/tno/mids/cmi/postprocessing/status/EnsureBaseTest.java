/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing.status;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.escet.cif.metamodel.cif.Specification;

import nl.tno.mids.cif.extensions.CIFOperations;
import nl.tno.mids.cmi.api.general.CmiGeneralDataQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec;
import nl.tno.mids.cmi.postprocessing.PostProcessingStatusExtensions;

abstract class EnsureBaseTest {
    private static Map<String, PostProcessingModelCifSpec> cifModels = new TreeMap<>();
    static {
        // CIF model with data and with tau.
        loadCifModel("" //
                + "automaton CIFAUTOMATON:\n" //
                + "  event e1__abc__fcn;\n" //
                + "  event c2__abc__fcn;\n" //
                + "  disc int[0..3] cnt;\n" //
                + "  location loc1:\n" //
                + "    initial;\n" //
                + "    marked;\n" //
                + "    edge e1__abc__fcn goto loc2;\n" //
                + "  location loc2:\n" //
                + "    marked;\n" //
                + "    edge c2__abc__fcn when cnt < 3 do cnt := cnt + 1 goto loc2;\n" //
                + "    edge tau when cnt = 3 do cnt := 0 goto loc1;\n" //
                + "end");

        // CIF model with data and without tau.
        loadCifModel("" //
                + "automaton CIFAUTOMATON:\n" //
                + "  event e1__abc__fcn;\n" //
                + "  event c2__abc__fcn;\n" //
                + "  event c3__abc__fcn;\n" //
                + "  disc int[0..3] cnt;\n" //
                + "  location loc1:\n" //
                + "    initial;\n" //
                + "    marked;\n" //
                + "    edge e1__abc__fcn goto loc2;\n" //
                + "  location loc2:\n" //
                + "    marked;\n" //
                + "    edge c2__abc__fcn when cnt < 3 do cnt := cnt + 1 goto loc2;\n" //
                + "    edge c3__abc__fcn when cnt = 3 do cnt := 0 goto loc1;\n" //
                + "end");

        // CIF model without data and without tau.
        loadCifModel("" //
                + "automaton CIFAUTOMATON:\n" //
                + "  event e1__abc__fcn;\n" //
                + "  event c2__abc__fcn;\n" //
                + "  event c3__abc__fcn;\n" //
                + "  location loc1:\n" //
                + "    initial;\n" //
                + "    marked;\n" //
                + "    edge e1__abc__fcn goto loc2;\n" //
                + "  location loc2:\n" //
                + "    marked;\n" //
                + "    edge c2__abc__fcn goto loc2;\n" //
                + "    edge c3__abc__fcn goto loc1;\n" //
                + "end");

        // CIF model without data and with tau.
        loadCifModel("" //
                + "automaton CIFAUTOMATON:\n" //
                + "  event e1__abc__fcn;\n" //
                + "  event c2__abc__fcn;\n" //
                + "  event c3__abc__fcn;\n" //
                + "  location loc1:\n" //
                + "    initial;\n" //
                + "    marked;\n" //
                + "    edge e1__abc__fcn goto loc2;\n" //
                + "  location loc2:\n" //
                + "    marked;\n" //
                + "    edge c2__abc__fcn goto loc2;\n" //
                + "    edge tau goto loc1;\n" //
                + "end");
    }

    private static void loadCifModel(String cifSpecString) {
        Specification cifSpec = CIFOperations.read(cifSpecString);
        PostProcessingModelCifSpec model = new PostProcessingModelCifSpec(cifSpec, cifSpec.getName(),
                PostProcessingStatusExtensions.getPostProcessingStatus(cifSpec));
        String modelKey = modelKey(model);
        if (cifModels.containsKey(modelKey)) {
            throw new RuntimeException("CIF model for " + modelKey + " already exists.");
        }
        cifModels.put(modelKey, model);
    }

    protected PostProcessingModelCifSpec getCifModel(boolean data, boolean tau) {
        String modelKey = modelKey(data, tau);
        if (!cifModels.containsKey(modelKey)) {
            throw new RuntimeException("CIF model for " + modelKey + " does not exists.");
        }
        PostProcessingModelCifSpec model = cifModels.get(modelKey);
        // validate the initial model and model.status
        assertEquals(CmiGeneralDataQueries.hasData(model.getCifSpec()), data);
        assertEquals(CmiGeneralEventQueries.hasTau(model.getCifSpec()), tau);

        assertEquals(model.status.dataIsPresent(), data);
        assertEquals(model.status.tauIsPresent(), tau);

        return model;
    }

    private static String modelKey(PostProcessingModelCifSpec model) {
        return modelKey(model.status.dataIsPresent(), model.status.tauIsPresent());
    }

    private static String modelKey(boolean dataIsPresent, boolean tauIsPresent) {
        return String.format("%s_%s", dataIsPresent, tauIsPresent);
    }

    protected void doTests(boolean initialDataIsPresent, boolean initialTauIsPresent, boolean expectedDataIsPresent,
            boolean expectedTauIsPresent)
    {
        doTestWithModel(initialDataIsPresent, initialTauIsPresent, expectedDataIsPresent, expectedTauIsPresent);
        doTestWithStatus(initialDataIsPresent, initialTauIsPresent, expectedDataIsPresent, expectedTauIsPresent);
    }

    private void doTestWithStatus(boolean initialDataIsPresent, boolean initialTauIsPresent,
            boolean expectedDataIsPresent, boolean expectedTauIsPresent)
    {
        // create and process the status
        PostProcessingStatus resultPostProcessingStatus = processStatus(
                new PostProcessingStatus(initialDataIsPresent, initialTauIsPresent));

        // validate the result status
        assertEquals(expectedDataIsPresent, resultPostProcessingStatus.dataIsPresent(), "data");
        assertEquals(expectedTauIsPresent, resultPostProcessingStatus.tauIsPresent(), "tau");
    }

    private void doTestWithModel(boolean initialDataIsPresent, boolean initialTauIsPresent,
            boolean expectedDataIsPresent, boolean expectedTauIsPresent)
    {
        // load the apropriate model
        PostProcessingModelCifSpec initialPostProcessingModel = getCifModel(initialDataIsPresent, initialTauIsPresent);

        // process the model
        PostProcessingModel resultPostProcessingModel = processModel(initialPostProcessingModel);

        // validate the result model and model.status
        assertEquals(expectedDataIsPresent, CmiGeneralDataQueries.hasData(resultPostProcessingModel.getCifSpec()),
                "data");
        assertEquals(expectedTauIsPresent, CmiGeneralEventQueries.hasTau(resultPostProcessingModel.getCifSpec()),
                "tau");

        assertEquals(expectedDataIsPresent, resultPostProcessingModel.status.dataIsPresent(), "data");
        assertEquals(expectedTauIsPresent, resultPostProcessingModel.status.tauIsPresent(), "tau");
    }

    abstract PostProcessingStatus processStatus(PostProcessingStatus initialPostProcessingStatus);

    abstract PostProcessingModel processModel(PostProcessingModelCifSpec initialPostProcessingModel);
}
