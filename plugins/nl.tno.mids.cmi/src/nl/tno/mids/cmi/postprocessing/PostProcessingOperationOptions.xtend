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

import java.util.Locale
import org.eclipse.xtend.lib.annotations.Accessors

@Accessors
abstract class PostProcessingOperationOptions implements Cloneable {

    /** The filter mode to decide to which components to apply the post processing operation. 
     *  Only used if the post processing operation supports filtering. */
    public PostProcessingFilterMode filterMode = PostProcessingFilterMode.NONE;

    /**
     * Regular expression pattern to use to match component names. Effect and applicability depend on
     * {@link #filterMode} and post processing operation filtering support.
     */
    public String filterPattern;

    override clone() {
        super.clone
    }

    abstract def void validate() throws IllegalStateException

    override String toString() {
        val builder = new StringBuilder()
        builder.append(filterMode.name.toLowerCase(Locale.US))
        switch (filterMode) {
            case NONE: {
                // Pattern does not apply.
            }
            case INCLUSION: {
                builder.append(" ")
                builder.append(filterPattern)
            }
            case EXCLUSION: {
                builder.append(" ")
                builder.append(filterPattern)
            }
            default:
                throw new RuntimeException("Unknown filter mode: " + filterMode)
        }
        return builder.toString
    }

    def <T extends PostProcessingOperation<U>, U extends PostProcessingOperationOptions> PostProcessingOperationProvider<T, U> getProvider() {
        val provider = PostProcessingOperationProviders.getPostProcessingOperationProvider(this)
        return provider as PostProcessingOperationProvider<T, U>
    }

}
