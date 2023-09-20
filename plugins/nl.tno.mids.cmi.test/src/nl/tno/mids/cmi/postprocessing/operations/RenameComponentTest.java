/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
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

class RenameComponentTest extends BasePostProcessingOperationTest<RenameComponent> {
    @ParameterizedTest
    @ValueSource(strings =
    {"Plain", "RenameClient", "RenameServer"})
    public void test(String testMethod) throws IOException {
        super.performTest(testMethod);
    }

    @Override
    protected RenameComponent getOperation() {
        RenameComponentOptions options = new RenameComponentOptions("ComponentA", "ComponentB");
        return new RenameComponent(options);
    }
}
