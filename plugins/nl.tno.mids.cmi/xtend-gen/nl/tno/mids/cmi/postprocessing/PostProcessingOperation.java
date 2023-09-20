package nl.tno.mids.cmi.postprocessing;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import org.eclipse.core.runtime.IProgressMonitor;

@SuppressWarnings("all")
public abstract class PostProcessingOperation<U extends PostProcessingOperationOptions> implements Cloneable {
  protected U options;
  
  public PostProcessingOperation(final U options) {
    this.options = options;
  }
  
  public abstract PostProcessingPreconditionSubset getPreconditionSubset();
  
  public abstract PostProcessingResultSubset getResultSubset();
  
  /**
   * Perform a post-processing operation.
   * 
   * @param models Mapping from component name (absolute name of the CIF automaton) to input model for the component.
   *      This mapping should be modified in-place.
   * @param selectedComponents Names of the components from {@code models} to which to apply the operation.
   * @param relativeResolvePath The absolute path of the input file. During post-processing, all relative paths are
   *      to be resolved against the directory that contains this file.
   * @param monitor The progress monitor.
   */
  public abstract void applyOperation(final Map<String, PostProcessingModel> models, final Set<String> selectedComponents, final Path relativeResolvePath, final IProgressMonitor monitor);
  
  /**
   * The resultStatus is the result of applying the modificationSubset onto the initialStatus.
   */
  public final PostProcessingStatus getResultStatus(final PostProcessingStatus status) {
    final PostProcessingStatus ensuredStatus = this.getPreconditionSubset().apply(status);
    return this.getResultSubset().apply(ensuredStatus);
  }
}
