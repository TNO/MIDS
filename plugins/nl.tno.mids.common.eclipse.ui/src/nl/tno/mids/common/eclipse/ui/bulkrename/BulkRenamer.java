/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.common.eclipse.ui.bulkrename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.escet.common.java.Maps;
import org.eclipse.jface.viewers.IStructuredSelection;

public class BulkRenamer {
    private static final String TASK_NAME = "Bulk Renaming";

    private List<IResource> resourceListToRename;

    private Set<IContainer> parentFolders;

    private IContainer parentFolder;

    private String formatString;

    private List<String> parentFolderMembers;

    @SuppressWarnings("unchecked")
    public BulkRenamer(IStructuredSelection resourcesToRename) {
        this.resourceListToRename = resourcesToRename.toList();

        parentFolders = ((List<IResource>)resourceListToRename).stream().map(r -> r.getParent())
                .collect(Collectors.toSet());
        parentFolder = parentFolders.size() == 1 ? parentFolders.iterator().next() : null;
        parentFolderMembers = getMembers(parentFolder);
        int numberOfDigits = Integer.toString(resourceListToRename.size()).length();
        formatString = "%0" + numberOfDigits + "d";
    }

    private List<String> getMembers(IContainer parentFolder) {
        List<String> result = new ArrayList<>();
        if (parentFolder != null) {
            try {
                result = Arrays.stream(parentFolder.members()).map(m -> m.getName()).collect(Collectors.toList());
            } catch (CoreException e) {
                throw new RuntimeException("Failed to rename resources in " + parentFolder.getName() + ".", e);
            }
        }
        return result;
    }

    public void rename(String targetName, IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, TASK_NAME, resourceListToRename.size());
        for (IResource resourceToRename: resourceListToRename) {
            renameResource(resourceToRename, new Path(newNames(targetName).get(resourceToRename.getName())),
                    subMonitor.split(1));
        }
    }

    private void renameResource(IResource resource, Path newPath, IProgressMonitor monitor) {
        boolean force = false;
        try {
            resource.move(newPath, force, monitor);
        } catch (CoreException e) {
            throw new RuntimeException("Failed to rename " + resource.getName() + " to " + newPath.toString(), e);
        }
    }

    public String validate(String targetName, String defaultValue) {
        String errMsg = defaultValue;
        if (targetName.contains("/") || targetName.contains("\\")) {
            return "'Output folder name' must not contain path separators";
        }
        if (!targetName.contains("#")) {
            return "The targetname should have a '#' symbol as placeholder for the numbering.";
        }

        if (parentFolders.size() != 1) {
            return "All selected resources should reside in the same parent folder.";
        }
        if (parentFolderMembers.stream()
                .anyMatch(currentMemberName -> newNames(targetName).containsValue(currentMemberName)))
        {
            return "Resources already exist using this target name.";
        }
        return errMsg;
    }

    private Map<String, String> newNames(String targetName) {
        int index = 1;
        Map<String, String> newNames = Maps.mapc(resourceListToRename.size());
        for (IResource resource: resourceListToRename) {
            String newName = targetName.replace("#", String.format(formatString, index++));
            if (!newName.equals(resource.getName())) {
                newNames.put(resource.getName(), newName);
            }
        }
        return newNames;
    }
}
