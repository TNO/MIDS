package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;

@SuppressWarnings("all")
public class FilterClientServerInteractionsProvider extends PostProcessingOperationProvider<FilterClientServerInteractions, FilterClientServerInteractionsOptions> {
  @Override
  public String getOperationReadableName() {
    return "Filter client/server interactions";
  }
  
  @Override
  public String getOperationDescription() {
    return "Filter models to keep only the interactions between two components (i.e. a client and server).";
  }
  
  @Override
  public Class<FilterClientServerInteractions> getOperationClass() {
    return FilterClientServerInteractions.class;
  }
  
  @Override
  public Class<FilterClientServerInteractionsOptions> getOperationOptionsClass() {
    return FilterClientServerInteractionsOptions.class;
  }
  
  @Override
  public FilterClientServerInteractions getOperation(final FilterClientServerInteractionsOptions options) {
    return new FilterClientServerInteractions(options);
  }
  
  @Override
  public FilterClientServerInteractionsOptions getOptions(final String args) {
    final FilterClientServerInteractionsOptions options = new FilterClientServerInteractionsOptions();
    final List<String> argList = Arrays.<String>asList(args.split(","));
    int _size = argList.size();
    boolean _equals = (_size == 2);
    Preconditions.checkArgument(_equals, ("Invalid arguments for FilterClientServer operation: " + args));
    options.setComponentName1(argList.get(0).trim());
    options.setComponentName2(argList.get(1).trim());
    return options;
  }
  
  @Override
  public String writeOptions(final FilterClientServerInteractionsOptions options) {
    String _componentName1 = options.getComponentName1();
    String _plus = (_componentName1 + ",");
    String _componentName2 = options.getComponentName2();
    return (_plus + _componentName2);
  }
  
  @Override
  public boolean supportsFilteredComponentsAsInput() {
    return false;
  }
}
