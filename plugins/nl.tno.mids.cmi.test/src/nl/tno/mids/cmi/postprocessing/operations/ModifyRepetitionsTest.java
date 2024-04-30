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

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import nl.tno.mids.cif.extensions.mrr.cif.MrrToCifMode;

class ModifyRepetitionsTest extends BasePostProcessingOperationTest<ModifyRepetitions> {
    private ModifyRepetitionsOptions options;

    @ParameterizedTest
    @CsvSource(
    {"AddData, false, 0, 0, 0, DATA", //
            "LowerThreshold, true, 0, 5, 0, PLAIN", //
            "MakeInfinite, true, 0, 0, 0, PLAIN", //
            "MarkedUnmarked, false, 1, 0, 0, PLAIN", //
            "MaxRepeatsIsOne, false, 1, 0, 0, PLAIN", //
            "NestedLoops, false, 0, 0, 0, DATA", //
            "SplitJoins, true, 0, 0, 0, PLAIN", //
            "UpperThreshold, true, 0, 0, 3, PLAIN"})
    void test(String testMethod, boolean infinite, int maxRepeats, int lowerThreshold, int upperThreshold, String mode)
            throws IOException
    {
        options = new ModifyRepetitionsOptions();
        options.setLowerThreshold(lowerThreshold);
        options.setMakeInfinite(infinite);
        options.setMaxRepeats(maxRepeats);
        options.setMode(MrrToCifMode.valueOf(mode));
        options.setUpperThreshold(upperThreshold);
        super.performTest(testMethod);
    }

    @Override
    protected ModifyRepetitions getOperation() {
        return new ModifyRepetitions(options);
    }
}
