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
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider
import org.eclipse.xtend.lib.annotations.Accessors

@Accessors
class MergeInterfaceClientsServersOptions extends PostProcessingOperationOptions {
    /** Enable merging of client components. */
    public boolean mergeClients = true

    /** Enable merging of server components. */
    public boolean mergeServers = true

    /** Name of interface to merge. If empty, all interfaces are merged. */
    public String mergeInterface = ""

    override validate() throws IllegalStateException {
        Preconditions.checkState(mergeClients || mergeServers)
    }

    override toString() {
        val builder = new StringBuilder()
        builder.append(PostProcessingOperationProvider.getOperationFormalName(MergeInterfaceClientsServers))
        builder.append(" ")
        builder.append(mergeClients)
        builder.append(" ")
        builder.append(mergeServers)
        builder.append(" ")
        builder.append(mergeInterface)
        builder.append(" ")
        builder.append(super.toString)
        return builder.toString
    }
}
