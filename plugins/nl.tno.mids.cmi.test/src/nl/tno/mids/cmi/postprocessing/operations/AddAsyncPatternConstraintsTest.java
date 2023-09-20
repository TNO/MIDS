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

class AddAsyncPatternConstraintsTest extends BasePostProcessingOperationTest<AddAsyncPatternConstraints> {
    @ParameterizedTest
    @ValueSource(strings =
    {"Client_Async_Arslt", "Client_Fcn_Fcncb", "Client_Req_Wait", "SameState", "Server_Async_Arslt", "Server_Fcn_Fcncb",
            "Server_Req_Wait", "WithExistingConstraint"})
    void test(String testMethod) throws IOException {
        performTest(testMethod);
    }

    @Override
    protected AddAsyncPatternConstraints getOperation() {
        AddAsyncPatternConstraintsOptions options = new AddAsyncPatternConstraintsOptions();
        return new AddAsyncPatternConstraints(options);
    }
}
