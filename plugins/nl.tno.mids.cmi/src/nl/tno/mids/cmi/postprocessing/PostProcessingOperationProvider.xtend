/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing

import com.google.common.base.Preconditions
import java.util.Locale
import org.apache.commons.lang3.StringUtils

abstract class PostProcessingOperationProvider<T extends PostProcessingOperation<U>, U extends PostProcessingOperationOptions> {
    /**
     * Returns the formal name of the post-processing operation. Must be unique among post-processing operations.
     * Used for e.g. serialization.
     */
    def String getOperationFormalName() {
        return getOperationFormalName(getOperationClass())
    }

    /** Returns the human readable name of the post-processing operation. */
    abstract def String getOperationReadableName()

    /** Returns a short description (one line) of the post-processing operation. */
    abstract def String getOperationDescription()

    /** Returns {@code true} if the operation will use the filtered component list, {@code false} otherwise. */
    abstract def boolean supportsFilteredComponentsAsInput()

    /** Returns the class of the operation. */
    abstract def Class<T> getOperationClass()

    /** Returns the class of the operation's options. */
    abstract def Class<U> getOperationOptionsClass()

    /**
     * Returns an instance of the operation.
     * 
     * @param options The options for the operation.
     * @return The operation.
     */
    abstract def T getOperation(U options)

    /**
     * Returns the options for the operation based on comma-separated values.
     * 
     * @param options The options for the operation as comma-separated string.
     * @return The options.
     */
    abstract def U getOptions(String options)

    /**
     * Returns the options for the operation in the syntax of the arguments for post-processing operations of the
     * command line option value.
     * 
     * @param options The options for the operation.
     * @return The options for the operation as comma-separated string.
     */
    abstract def String writeOptions(U options);

    /**
     * Returns an instance of the operation.
     * 
     * @param options The options for the operation.
     * @return The operation.
     */
    def T getOperationFromRawOptions(PostProcessingOperationOptions options) {
        return getOperation(options as U)
    }

    static def String getOperationFormalName(Class<?> operationClass) {
        Preconditions.checkArgument(PostProcessingOperation.isAssignableFrom(operationClass))
        var name = operationClass.simpleName
        name = StringUtils.uncapitalize(name)
        return name
    }

    /**
     * Create a user-friendly representation of an enum value.
     * 
     * @param value Enumeration value string.
     * @return {@code value} in lowercase and with {@code _} replaced by {@code -}.
     */
    protected static def String displayEnumValue(String value) {
        return value.replace("_", "-").toLowerCase(Locale.US)
    }

    /**
     * Normalize the string representation of an enumeration value.
     * 
     * @param value Enumeration value string.
     * @return {@code value} trimmed, in uppercase and with {@code -} replaced by {@code _}.
     */
    static def normalizeEnumValue(String value) {
        return value.trim.replace("-", "_").toUpperCase(Locale.US)
    }

}
