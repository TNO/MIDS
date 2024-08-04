
package nl.tno.mids.product.examples;

import static org.eclipse.escet.common.java.Maps.map;

import java.util.Map;

import org.eclipse.escet.common.eclipse.ui.CopyFilesNewProjectWizard;
import org.osgi.framework.FrameworkUtil;

/** Wizard to create a MIDS examples project. */
public class MidsExamplesWizard extends CopyFilesNewProjectWizard {
    @Override
    protected String getInitialProjectName() {
        String qualifier = FrameworkUtil.getBundle(getClass()).getVersion().toString();
        return "MidsExamples-" + qualifier;
    }

    @Override
    protected Map<String, String> getPathsToCopy() {
        Map<String, String> entries = map();
        entries.put("examples/simple1", "simple1/input");
        return entries;
    }
}
