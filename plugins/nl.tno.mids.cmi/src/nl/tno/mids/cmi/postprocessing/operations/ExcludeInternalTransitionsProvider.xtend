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
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider

class ExcludeInternalTransitionsProvider extends PostProcessingOperationProvider<ExcludeInternalTransitions, ExcludeInternalTransitionsOptions> {
    override getOperationReadableName() {
        return "Exclude internal transitions"
    }

    override getOperationDescription() {
        return "Exclude internal/non-communicating transitions where possible."
    }

    override getOperationClass() {
        return ExcludeInternalTransitions
    }

    override getOperationOptionsClass() {
        return ExcludeInternalTransitionsOptions
    }

    override getOperation(ExcludeInternalTransitionsOptions options) {
        return new ExcludeInternalTransitions(options)
    }

    override getOptions(String args) {
        Preconditions.checkArgument(args.empty, "Invalid arguments for ExcludeInternalTransitions operation: " + args)
        return new ExcludeInternalTransitionsOptions()
    }

    override writeOptions(ExcludeInternalTransitionsOptions options) {
        return ""
    }

    override supportsFilteredComponentsAsInput() {
        return true
    }
}
