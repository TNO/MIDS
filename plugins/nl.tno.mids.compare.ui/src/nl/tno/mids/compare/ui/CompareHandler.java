/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.ui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;

import javax.inject.Named;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import nl.tno.mids.compare.MidsCompare;
import nl.tno.mids.compare.options.CompareOptions;

/** Menu action handler to compare sets of CIF models. */
public class CompareHandler {
    /**
     * Prompt for the {@link CompareOptions} and execute the compare command.
     * 
     * @param selection Selected input folder.
     * @param shell {@link Shell} to use for created dialog window.
     */
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection selection, Shell shell) {
        Path inputPath = Paths.get(((IResource)selection.getFirstElement()).getLocationURI());
        CompareOptions compareOptions = new CompareOptions();
        compareOptions.inputPath = inputPath;
        compareOptions.outputPath = inputPath.getParent().resolve("output");

        /*
         * Pass the initialized compareOptions to the {@link CompareOptionsDialog}. That will store the user chosen
         * options in that compareOptions for further processing.
         */
        CompareOptionsDialog compareOptionsDialog = new CompareOptionsDialog(shell, compareOptions);
        if (compareOptionsDialog.open() != Window.OK) {
            return;
        }

        Job compareJob = Job.create("Comparing models",
                monitor ->
                { MidsCompare.performCompare(compareOptions, Clock.systemDefaultZone(), monitor); });
        compareJob.setUser(true);
        compareJob.schedule();
    }
}
