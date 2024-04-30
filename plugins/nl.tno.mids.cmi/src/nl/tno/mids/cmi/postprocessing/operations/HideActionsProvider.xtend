/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
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

class HideActionsProvider extends PostProcessingOperationProvider<HideActions, HideActionsOptions> {
    override getOperationReadableName() {
        return "Hide actions"
    }

    override getOperationDescription() {
        return "Hide actions matching the given pattern (regular expression)."
    }

    override getOperationClass() {
        return HideActions
    }

    override getOperationOptionsClass() {
        return HideActionsOptions
    }

    override getOperation(HideActionsOptions options) {
        return new HideActions(options)
    }

    override getOptions(String args) {
        Preconditions.checkArgument(!args.trim.empty, "Invalid arguments for HideActions operation: " + args)
        val options = new HideActionsOptions
        options.pattern = args.trim
        return options
    }

    override writeOptions(HideActionsOptions options) {
        return options.pattern
    }

    override supportsFilteredComponentsAsInput() {
        return true
    }

}
