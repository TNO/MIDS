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

class RenameFunctionsTest extends BasePostProcessingOperationTest<RenameFunctions> {
    private RenameFunctionsOptions options;

    @ParameterizedTest
    @CsvSource(value =
    {"MultipleComponents;Interface:FunctionCall->Interface:OtherCall", //
            "MultipleRenames;pc1:abc->int1:def,pc2:abc->int2:ghi,pc3:abc->int3:jkl", //
            "PartialMatch;pc1:ab->int1:de", //
            "PlainBoth;pc2:abc->int2:def", //
            "PlainFunction;pc2:abc->pc2:def", //
            "PlainInterface;pc2:abc->int2:abc", //
            "PlainMerge;pc1:abc->pc2:abc"}, delimiter = ';')
    void test(String testMethod, String functionsMappingString) throws IOException {
        options = new RenameFunctionsOptions(functionsMappingString);
        super.performTest(testMethod);
    }

    @Override
    protected RenameFunctions getOperation() {
        return new RenameFunctions(options);
    }
}
