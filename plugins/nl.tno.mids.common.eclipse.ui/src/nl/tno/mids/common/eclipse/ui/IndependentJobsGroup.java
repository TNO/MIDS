/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.common.eclipse.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.JobGroup;

/**
 * {@link JobGroup} for a collection of independent jobs. Does not cancel any jobs if previous jobs in the group fail.
 */
public class IndependentJobsGroup extends JobGroup {
    /**
     * @see JobGroup
     * @see JobGroup#JobGroup(String, int, int)
     */
    public IndependentJobsGroup(String name, int maxThreads, int seedJobsCount) {
        super(name, maxThreads, seedJobsCount);
    }

    @Override
    protected boolean shouldCancel(IStatus lastCompletedJobResult, int numberOfFailedJobs, int numberOfCanceledJobs) {
        return false;
    }
}
