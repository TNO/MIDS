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
import org.junit.jupiter.params.provider.ValueSource;

class HideActionsTest extends BasePostProcessingOperationTest<HideActions> {
    @ParameterizedTest
    @ValueSource(strings =
    {"Plain", "SharedLocation", "WithTau"})
    void test(String testMethod) throws IOException {
        super.performTest(testMethod);
    }

    @Override
    protected HideActions getOperation() {
        HideActionsOptions options = new HideActionsOptions();
        options.setPattern(".*_hide_this.*");

        return new HideActions(options);
    }
}
