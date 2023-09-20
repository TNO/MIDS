package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Preconditions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;

@SuppressWarnings("all")
public class PrefixCloseProvider extends PostProcessingOperationProvider<PrefixClose, PrefixCloseOptions> {
  @Override
  public String getOperationReadableName() {
    return "Prefix close";
  }
  
  @Override
  public String getOperationDescription() {
    return "Make automata prefix closed.";
  }
  
  @Override
  public Class<PrefixClose> getOperationClass() {
    return PrefixClose.class;
  }
  
  @Override
  public Class<PrefixCloseOptions> getOperationOptionsClass() {
    return PrefixCloseOptions.class;
  }
  
  @Override
  public PrefixClose getOperation(final PrefixCloseOptions options) {
    return new PrefixClose(options);
  }
  
  @Override
  public PrefixCloseOptions getOptions(final String args) {
    Preconditions.checkArgument(args.isEmpty(), ("Invalid arguments for PrefixClose operation: " + args));
    return new PrefixCloseOptions();
  }
  
  @Override
  public String writeOptions(final PrefixCloseOptions options) {
    return "";
  }
  
  @Override
  public boolean supportsFilteredComponentsAsInput() {
    return true;
  }
}
