/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.ui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import javax.inject.Named;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import nl.tno.mids.cmi.ComponentExtraction;
import nl.tno.mids.cmi.ComponentExtractionOptions;
import nl.tno.mids.common.eclipse.ui.IndependentJobsGroup;

/** Menu action handler to apply CMI to infer CIF models from a TMSC, with options configured through a dialog. */
public class CmiFromTmscHandler {
    /**
     * Execute action to infer CIF models from TMSC.
     * 
     * @param selection Selected file to apply inference to.
     * @param shell Parent container for UI components.
     * @throws CoreException If the action fails.
     */
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection selection, Shell shell)
            throws CoreException
    {
        IFile firstIFile = (IFile)selection.getFirstElement();
        ComponentExtractionOptionsDialog dialog = new ComponentExtractionOptionsDialog(shell,
                firstIFile.getParent().getLocation());
        if (dialog.open() != Window.OK) {
            return;
        }

        ComponentExtractionOptions options = dialog.options();
        if (options == null) {
            return;
        }

        Path baseOutputPath = options.getOutput().getPath();

        int maxThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
        JobGroup jobGroup = new IndependentJobsGroup("CMI", maxThreads, selection.size());
        for (Iterator<?> itr = selection.iterator(); itr.hasNext();) {
            IFile tmscFile = (IFile)itr.next();
            ComponentExtractionOptions copiedOptions = (ComponentExtractionOptions)options.clone();
            Path tmscPath = Paths.get(tmscFile.getLocation().toOSString()).toAbsolutePath().normalize();
            copiedOptions.getInput().setPath(tmscPath);
            String tmscName = tmscFile.getLocation().removeFileExtension().lastSegment();
            copiedOptions.getOutput().setPath(baseOutputPath.resolve(tmscName));

            IJobFunction jobFunc = monitor -> {
                new ComponentExtraction().extract(tmscPath, copiedOptions, monitor);
                return Status.OK_STATUS;
            };
            Job job = Job.create("Extracting models from " + tmscFile.getName(), jobFunc);
            job.setJobGroup(jobGroup);
            job.setUser(true);
            job.schedule();
        }
    }
}
