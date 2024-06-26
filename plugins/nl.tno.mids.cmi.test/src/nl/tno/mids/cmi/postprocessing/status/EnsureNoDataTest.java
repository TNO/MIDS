/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec;

class EnsureNoDataTest extends EnsureBaseTest {
    @Override
    PostProcessingModel processModel(PostProcessingModelCifSpec initialPostProcessingModel) {
        return EnsureNoData.ensureNoData(initialPostProcessingModel);
    }

    @Override
    PostProcessingStatus processStatus(PostProcessingStatus initialPostProcessingStatus) {
        return EnsureNoData.ensureNoData(initialPostProcessingStatus);
    }

    @ParameterizedTest
    @CsvSource(
    {"true, true", //
            "true, false", //
            "false, true", //
            "false, false",//
    })
    void testEnsureNoData(boolean initialDataIsPresent, boolean initialTauIsPresent) {
        doTests(initialDataIsPresent, initialTauIsPresent, false, initialTauIsPresent);
    }
}
