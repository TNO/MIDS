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
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class InjectDomainKnowledgeTest extends BasePostProcessingOperationTest<InjectDomainKnowledge> {
    private InjectDomainKnowledgeOperator testOperator;

    @ParameterizedTest
    @EnumSource(InjectDomainKnowledgeOperator.class)
    void test(InjectDomainKnowledgeOperator testOperator) throws IOException {
        this.testOperator = testOperator;
        relativeResolvePath = Paths.get("testData").resolve("PostProcessing").resolve(testClass)
                .resolve(testOperator.name());
        super.performTest(testOperator.name());
    }

    @Override
    protected InjectDomainKnowledge getOperation() {
        InjectDomainKnowledgeOptions options = new InjectDomainKnowledgeOptions();
        options.setOperator(testOperator);
        Path domainModelPath = Paths.get(testOperator.name()).resolve("injection")
                .resolve(testOperator.name() + "_Injection.cif");
        options.setModelPath(domainModelPath.toString());
        return new InjectDomainKnowledge(options);
    }
}
