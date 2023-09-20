/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Preconditions;

import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;

public class RenameComponentOptions extends PostProcessingOperationOptions {
    /** The old component name that is to be replaced by {@link #newComponentName}. */
    private String oldComponentName;

    /** The new component name, replacing {@link #oldComponentName}. */
    private String newComponentName;

    public RenameComponentOptions(String oldComponentName, String newComponentName) {
        this.oldComponentName = oldComponentName;
        this.newComponentName = newComponentName;
    }

    /**
     * @return The old component name that is to be replaced by {@link #getNewComponentName()}.
     */
    public String getOldComponentName() {
        return oldComponentName;
    }

    /**
     * @return The new component name, replacing {@link #getOldComponentName()}.
     */
    public String getNewComponentName() {
        return newComponentName;
    }

    @Override
    public void validate() throws IllegalStateException {
        validateName(oldComponentName);
        validateName(newComponentName);
    }

    /**
     * Validates a given component name pattern.
     * 
     * @param pattern The pattern to validate.
     */
    private void validateName(String pattern) {
        Preconditions.checkState(pattern != null);
        Preconditions.checkState(!pattern.trim().isEmpty());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(PostProcessingOperationProvider.getOperationFormalName(RenameComponent.class));
        builder.append(" ");
        builder.append(oldComponentName);
        builder.append(" ");
        builder.append(newComponentName);
        builder.append(" ");
        builder.append(super.toString());
        return builder.toString();
    }
}
