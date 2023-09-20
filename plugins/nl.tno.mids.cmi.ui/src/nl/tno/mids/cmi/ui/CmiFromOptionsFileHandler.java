/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.ui;

import java.io.IOException;
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
import org.eclipse.swt.widgets.Shell;

import nl.tno.mids.cmi.ComponentExtraction;
import nl.tno.mids.cmi.ComponentExtractionOptions;
import nl.tno.mids.common.eclipse.ui.IndependentJobsGroup;

/** Menu action handler to apply CMI to infer CIF models from a TMSC based on options from an options file. */
public class CmiFromOptionsFileHandler {
    /**
     * Execute action to infer CIF models from TMSC.
     * 
     * @param selection Selected files to apply inference to.
     * @param shell Parent container for UI components.
     * @throws CoreException If the action fails.
     */
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection selection, Shell shell)
            throws CoreException
    {
        int maxThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
        JobGroup jobGroup = new IndependentJobsGroup("CMI", maxThreads, selection.size());
        for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
            // Load options from options file.
            IFile optionsFile = (IFile)iterator.next();
            ComponentExtractionOptions options;
            String optionsFileLocation = optionsFile.getLocation().toOSString();
            String[] args = {"-options-file", optionsFileLocation};
            try {
                options = ComponentExtractionOptions.parse(args);
            } catch (IOException e) {
                throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID,
                        "Failed to load options file: " + optionsFileLocation, e));
            }

            Path absOptionsPath = Paths.get(optionsFile.getLocation().toOSString());

            // Find project path.
            Path projectPath = Paths.get(optionsFile.getProject().getLocation().toOSString());

            // Schedule model extraction.
            IJobFunction jobFunc = monitor -> {
                new ComponentExtraction().extract(absOptionsPath, projectPath, options, monitor);
                return Status.OK_STATUS;
            };
            Job job = Job.create("Extracting models from " + options.getInput().getPath().getFileName().toString(),
                    jobFunc);
            job.setJobGroup(jobGroup);
            job.setUser(true);
            job.schedule();
        }
    }
}
