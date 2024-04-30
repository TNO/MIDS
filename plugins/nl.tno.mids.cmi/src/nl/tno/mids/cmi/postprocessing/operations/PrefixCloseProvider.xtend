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

class PrefixCloseProvider extends PostProcessingOperationProvider<PrefixClose, PrefixCloseOptions> {
    override getOperationReadableName() {
        return "Prefix close"
    }

    override getOperationDescription() {
        return "Make automata prefix closed."
    }

    override getOperationClass() {
        return PrefixClose
    }

    override getOperationOptionsClass() {
        return PrefixCloseOptions
    }

    override getOperation(PrefixCloseOptions options) {
        return new PrefixClose(options)
    }

    override getOptions(String args) {
        Preconditions.checkArgument(args.empty, "Invalid arguments for PrefixClose operation: " + args)
        return new PrefixCloseOptions()
    }

    override writeOptions(PrefixCloseOptions options) {
        return ""
    }

    override supportsFilteredComponentsAsInput() {
        return true
    }

}
