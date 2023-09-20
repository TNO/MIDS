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

class FilterClientServerInteractionsTest extends BasePostProcessingOperationTest<FilterClientServerInteractions> {
    @ParameterizedTest
    @ValueSource(strings =
    {"Plain"})
    void test(String testMethod) throws IOException {
        super.performTest(testMethod);
    }

    @Override
    protected FilterClientServerInteractions getOperation() {
        FilterClientServerInteractionsOptions options = new FilterClientServerInteractionsOptions();
        options.setComponentName1("INCL01");
        options.setComponentName2("INCL02");
        return new FilterClientServerInteractions(options);
    }
}
