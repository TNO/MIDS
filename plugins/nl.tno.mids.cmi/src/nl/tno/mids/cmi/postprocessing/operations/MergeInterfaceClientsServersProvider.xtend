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

class MergeInterfaceClientsServersProvider extends PostProcessingOperationProvider<MergeInterfaceClientsServers, MergeInterfaceClientsServersOptions> {
    override getOperationReadableName() {
        return "Merge interface clients/servers"
    }

    override getOperationDescription() {
        return "Merge multiple clients and/or servers of interfaces into a single instance, considering them a single runtime component."
    }

    override getOperationClass() {
        return MergeInterfaceClientsServers
    }

    override getOperationOptionsClass() {
        return MergeInterfaceClientsServersOptions
    }

    override getOperation(MergeInterfaceClientsServersOptions options) {
        return new MergeInterfaceClientsServers(options)
    }

    override getOptions(String args) {
        val options = new MergeInterfaceClientsServersOptions()
        val argList = Arrays.asList(args.split(","))
        Preconditions.checkArgument(argList.size() == 3, "Invalid arguments for MergeInterfaceClientServers operation: " + args)
        options.mergeInterface = argList.get(0).trim()
        options.mergeClients = Boolean.parseBoolean(argList.get(1).trim())
        options.mergeServers = Boolean.parseBoolean(argList.get(2).trim())
        return options
    }

    override writeOptions(MergeInterfaceClientsServersOptions options) {
        return options.mergeInterface + "," + options.mergeClients + "," + options.mergeServers
    }

    override supportsFilteredComponentsAsInput() {
        return true
    }
}
