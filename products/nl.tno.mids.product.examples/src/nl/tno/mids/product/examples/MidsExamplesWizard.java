
package nl.tno.mids.product.examples;

import static org.eclipse.escet.common.java.Maps.map;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.escet.common.eclipse.ui.CopyFilesNewProjectWizard;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.xtext.builder.nature.ToggleXtextNatureCommand;
import org.eclipse.xtext.builder.nature.XtextNature;
import org.eclipse.xtext.ui.XtextProjectHelper;
import org.osgi.framework.FrameworkUtil;

/** Wizard to create a MIDS examples project. */
@SuppressWarnings("restriction")
public class MidsExamplesWizard extends CopyFilesNewProjectWizard {
    @Override
    protected String getInitialProjectName() {
        String qualifier = FrameworkUtil.getBundle(getClass()).getVersion().toString();
        return "MidsExamples-" + qualifier;
    }

    @Override
    protected Map<String, String> getPathsToCopy() {
        Map<String, String> entries = map();
        entries.put("examples/simple", "simple/input-tmsct");
        return entries;
    }

    @Override
    public boolean performFinish() {
        // Create project.
        super.performFinish();

        // Get project.
        IWizardPage[] pages = getPages();
        Assert.check(pages.length == 1);
        IWizardPage page = pages[0];
        Assert.check(page instanceof WizardNewProjectCreationPage);
        WizardNewProjectCreationPage newProjectPage = (WizardNewProjectCreationPage)page;
        String projectName = newProjectPage.getProjectName();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject project = workspace.getRoot().getProject(projectName);
        Assert.check(project.exists());

        // Add Xtext nature.
        new ToggleXtextNatureCommand().toggleNature(project);
        Assert.check(XtextProjectHelper.hasNature(project));

        // Add Xtext builder.
        XtextNature nature = new XtextNature();
        nature.setProject(project);
        try {
            nature.configure();
        } catch (CoreException e) {
            Activator.getDefault().getLog().error("Failed to configure Xtext nature/builder.", e);
        }
        Assert.check(XtextProjectHelper.hasBuilder(project));

        // Done.
        return true;
    }
}
