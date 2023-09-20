/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing.operations

import com.google.common.base.Preconditions
import java.util.Arrays
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider

class InjectDomainKnowledgeProvider extends PostProcessingOperationProvider<InjectDomainKnowledge, InjectDomainKnowledgeOptions> {
    override getOperationReadableName() {
        return "Domain knowledge injection"
    }

    override getOperationDescription() {
        return "Combine inferred model and domain knowledge CIF model using a specified operator."
    }

    override getOperationClass() {
        return InjectDomainKnowledge
    }

    override getOperationOptionsClass() {
        return InjectDomainKnowledgeOptions
    }

    override getOperation(InjectDomainKnowledgeOptions options) {
        return new InjectDomainKnowledge(options)
    }

    override getOptions(String args) {
        val options = new InjectDomainKnowledgeOptions()
        val argList = Arrays.asList(args.split(","))
        Preconditions.checkArgument(argList.size() == 2, "Invalid arguments for InjectDomainKnowledge operation: " + args)
        options.operator = InjectDomainKnowledgeOperator.valueOf(normalizeEnumValue(argList.get(0)))
        options.modelPath = argList.get(1).trim()
        return options
    }

    override writeOptions(InjectDomainKnowledgeOptions options) {
        return displayEnumValue(options.operator.toString) + "," + options.modelPath
    }

    override supportsFilteredComponentsAsInput() {
        return true
    }
}
