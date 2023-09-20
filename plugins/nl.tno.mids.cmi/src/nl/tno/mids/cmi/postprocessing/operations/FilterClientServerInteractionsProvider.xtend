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

class FilterClientServerInteractionsProvider extends PostProcessingOperationProvider<FilterClientServerInteractions, FilterClientServerInteractionsOptions> {
    override getOperationReadableName() {
        return "Filter client/server interactions"
    }

    override getOperationDescription() {
        return "Filter models to keep only the interactions between two components (i.e. a client and server)."
    }

    override getOperationClass() {
        return FilterClientServerInteractions
    }

    override getOperationOptionsClass() {
        return FilterClientServerInteractionsOptions
    }

    override getOperation(FilterClientServerInteractionsOptions options) {
        return new FilterClientServerInteractions(options)
    }

    override getOptions(String args) {
        val options = new FilterClientServerInteractionsOptions()
        val argList = Arrays.asList(args.split(","))
        Preconditions.checkArgument(argList.size() == 2, "Invalid arguments for FilterClientServer operation: " + args)
        options.componentName1 = argList.get(0).trim()
        options.componentName2 = argList.get(1).trim()
        return options
    }
    
    override writeOptions(FilterClientServerInteractionsOptions options) {
        return options.componentName1 + "," + options.componentName2
    }
    
    override supportsFilteredComponentsAsInput() {
        return false
    }

}
