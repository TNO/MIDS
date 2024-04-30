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

class MergeInterfaceClientsServersTest extends BasePostProcessingOperationTest<MergeInterfaceClientsServers> {
    @ParameterizedTest
    @ValueSource(strings =
    {"Plain", "MultipleClient", "MultipleServer", "TypesClient", "TypesServer"})
    void test(String testMethod) throws IOException {
        super.performTest(testMethod);
    }

    @Override
    protected MergeInterfaceClientsServers getOperation() {
        MergeInterfaceClientsServersOptions options = new MergeInterfaceClientsServersOptions();
        return new MergeInterfaceClientsServers(options);
    }
}
