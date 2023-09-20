/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.common.eclipse.ui.bulkrename;

import javax.inject.Named;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

/**
 * <b>Warning</b> : As explained in
 * <a href="http://wiki.eclipse.org/Eclipse4/RCP/FAQ#Why_aren.27t_my_handler_fields_being_re-injected.3F">this wiki
 * page</a>, it is not recommended to define @Inject fields in a handler. <br/>
 * <br/>
 * <b>Inject the values in the @Execute methods</b>
 */
public class BulkRenameHandler {
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection selection, Shell shell)
            throws CoreException
    {
        BulkRenamer bulkRenamer = new BulkRenamer(selection);
        BulkRenameDialog dialog = new BulkRenameDialog(shell, bulkRenamer);
        if (dialog.open() != Window.OK) {
            return;
        }
        String targetName = dialog.targetName();
        if (targetName == null) {
            return;
        }

        Job job = Job.create("Bulk renaming", monitor -> {
            bulkRenamer.rename(targetName, monitor);
            return Status.OK_STATUS;
        });
        job.setUser(true);
        job.schedule();
    }
}
