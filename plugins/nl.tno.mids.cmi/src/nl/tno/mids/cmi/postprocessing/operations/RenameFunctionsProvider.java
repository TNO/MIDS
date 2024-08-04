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

import java.util.Map.Entry;
import java.util.StringJoiner;

import org.eclipse.escet.common.java.Pair;

import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;

/**
 * Configuration provider for {@link RenameFunctions} operation.
 */
public class RenameFunctionsProvider extends PostProcessingOperationProvider<RenameFunctions, RenameFunctionsOptions> {
    @Override
    public String getOperationReadableName() {
        return "Rename functions";
    }

    @Override
    public String getOperationDescription() {
        return "Renames one or more specified functions.";
    }

    @Override
    public Class<RenameFunctions> getOperationClass() {
        return RenameFunctions.class;
    }

    @Override
    public Class<RenameFunctionsOptions> getOperationOptionsClass() {
        return RenameFunctionsOptions.class;
    }

    @Override
    public RenameFunctions getOperation(RenameFunctionsOptions options) {
        return new RenameFunctions(options);
    }

    @Override
    public RenameFunctionsOptions getOptions(String options) {
        return new RenameFunctionsOptions(options);
    }

    public String writeOptions(RenameFunctionsOptions options) {
        StringJoiner stringJoiner = new StringJoiner(",");

        for (Entry<Pair<String, String>, Pair<String, String>> functionMapping: options.functionMappings.entrySet()) {
            stringJoiner.add(functionMapping.getKey().left + ":" + functionMapping.getKey().right + "->"
                    + functionMapping.getValue().left + ":" + functionMapping.getValue().right);
        }

        return stringJoiner.toString();
    }

    @Override
    public boolean supportsFilteredComponentsAsInput() {
        return false;
    }
}
