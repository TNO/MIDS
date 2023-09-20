package nl.tno.mids.cmi.postprocessing.operations;

import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;
import org.eclipse.xtend.lib.annotations.Accessors;

@Accessors
@SuppressWarnings("all")
public class ExcludeInternalTransitionsOptions extends PostProcessingOperationOptions {
  @Override
  public void validate() throws IllegalStateException {
  }
  
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(PostProcessingOperationProvider.getOperationFormalName(ExcludeInternalTransitions.class));
    builder.append(" ");
    builder.append(super.toString());
    return builder.toString();
  }
}
