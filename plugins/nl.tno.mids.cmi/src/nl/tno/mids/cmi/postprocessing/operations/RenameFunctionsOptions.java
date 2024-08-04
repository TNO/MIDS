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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.escet.common.java.Pair;

import com.google.common.base.Preconditions;

import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;

/** Contains option configuring the renaming of functions. */
public class RenameFunctionsOptions extends PostProcessingOperationOptions {
    /** The mapping of old function names to be renamed to new function names. */
    Map<Pair<String, String>, Pair<String, String>> functionMappings = new HashMap<>();

    boolean foundNotMatching = false;

    boolean foundDuplicate = false;

    /**
     * Constructs a configuration for function renaming.
     * 
     * @param functionMappingsString Textual description of mapping of old function names to be renamed to new function
     *     names.
     */
    public RenameFunctionsOptions(String functionMappingsString) {
        List<String> functionMappingsList = Arrays.asList(functionMappingsString.split(",|\\R"));
        Pattern mappingPattern = Pattern.compile(
                "(?<oldInterfaceName>\\w+):(?<oldFunctionName>\\w+)->(?<newInterfaceName>\\w+):(?<newFunctionName>\\w+)");
        for (String functionMappingString: functionMappingsList) {
            Matcher matcher = mappingPattern.matcher(functionMappingString);
            if (matcher.matches()) {
                String oldInterfaceName = matcher.group("oldInterfaceName");
                String oldFunctionName = matcher.group("oldFunctionName");
                String newInterfaceName = matcher.group("newInterfaceName");
                String newFunctionName = matcher.group("newFunctionName");
                if (!functionMappings.containsKey(Pair.pair(oldInterfaceName, oldFunctionName))) {
                    functionMappings.put(Pair.pair(oldInterfaceName, oldFunctionName),
                            Pair.pair(newInterfaceName, newFunctionName));
                } else {
                    foundDuplicate = true;
                }
            } else {
                foundNotMatching = true;
            }
        }
    }

    /**
     * @return {@code true} if an entry in the function mappings did not match the expected pattern, {@code false}
     *     otherwise.
     */
    public boolean isFoundNotMatching() {
        return foundNotMatching;
    }

    /**
     * @return {@code true} if there is duplication among entries in the function mappings, {@code false} otherwise.
     */
    public boolean isFoundDuplicate() {
        return foundDuplicate;
    }

    /**
     * @return The mapping of old function names to be renamed to new function names.
     */
    public Map<Pair<String, String>, Pair<String, String>> getFunctionMapping() {
        return functionMappings;
    }

    @Override
    public void validate() throws IllegalStateException {
        Preconditions.checkArgument(!foundNotMatching);
        Preconditions.checkArgument(!foundDuplicate);
    }

    /**
     * Write the function mappings contained in these options to string, with a given separator between entries.
     * 
     * @param separator Separator to add between mapping entries.
     * @return String representation of function mappings.
     */
    public String mappingToString(String separator) {
        return functionMappings.entrySet().stream().map(entry -> entry.getKey().left + ":" + entry.getKey().right + "->"
                + entry.getValue().left + ":" + entry.getValue().right).collect(Collectors.joining(separator));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(PostProcessingOperationProvider.getOperationFormalName(RenameFunctions.class));
        builder.append(" ");
        builder.append(mappingToString(","));
        builder.append(" ");
        builder.append(super.toString());
        return builder.toString();
    }
}
