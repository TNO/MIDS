/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.product.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class MIDSPerspective implements IPerspectiveFactory {
    public static final String ID = "nl.tno.mids.product.perspective";

    private static final String ID_ERROR_LOG = "org.eclipse.pde.runtime.LogView";

    private static final String ID_BOTTOM_FOLDER_VIEW = "nl.tno.mids.product.perspective.bottom";

    @Override
    public void createInitialLayout(IPageLayout layout) {
        defineActions(layout);
        defineLayout(layout);
    }

    /**
     * Add items and actions set to the window.
     *
     * @param layout Layout of the perspective.
     */
    private void defineActions(final IPageLayout layout) {
        // Show view shortcuts
        layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
        layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
        layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
        layout.addShowViewShortcut(ID_ERROR_LOG);
    }

    /**
     * Add views to the layout.
     *
     * @param layout Layout of the perspective.
     */
    private void defineLayout(final IPageLayout layout) {
        final String editorArea = layout.getEditorArea();
        layout.addView(IPageLayout.ID_PROJECT_EXPLORER, IPageLayout.LEFT, 0.25f, editorArea);
        layout.addView(IPageLayout.ID_OUTLINE, IPageLayout.BOTTOM, 0.5f, IPageLayout.ID_PROJECT_EXPLORER);

        final IFolderLayout bottom = layout.createFolder(ID_BOTTOM_FOLDER_VIEW, IPageLayout.BOTTOM, 0.65f, editorArea);
        bottom.addView(IPageLayout.ID_PROP_SHEET);
        bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
        bottom.addView(ID_ERROR_LOG);
    }
}
