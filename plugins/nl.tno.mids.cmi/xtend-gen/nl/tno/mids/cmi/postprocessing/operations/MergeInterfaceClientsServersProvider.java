package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;

@SuppressWarnings("all")
public class MergeInterfaceClientsServersProvider extends PostProcessingOperationProvider<MergeInterfaceClientsServers, MergeInterfaceClientsServersOptions> {
  @Override
  public String getOperationReadableName() {
    return "Merge interface clients/servers";
  }
  
  @Override
  public String getOperationDescription() {
    return "Merge multiple clients and/or servers of interfaces into a single instance, considering them a single runtime component.";
  }
  
  @Override
  public Class<MergeInterfaceClientsServers> getOperationClass() {
    return MergeInterfaceClientsServers.class;
  }
  
  @Override
  public Class<MergeInterfaceClientsServersOptions> getOperationOptionsClass() {
    return MergeInterfaceClientsServersOptions.class;
  }
  
  @Override
  public MergeInterfaceClientsServers getOperation(final MergeInterfaceClientsServersOptions options) {
    return new MergeInterfaceClientsServers(options);
  }
  
  @Override
  public MergeInterfaceClientsServersOptions getOptions(final String args) {
    final MergeInterfaceClientsServersOptions options = new MergeInterfaceClientsServersOptions();
    final List<String> argList = Arrays.<String>asList(args.split(","));
    int _size = argList.size();
    boolean _equals = (_size == 3);
    Preconditions.checkArgument(_equals, ("Invalid arguments for MergeInterfaceClientServers operation: " + args));
    options.mergeInterface = argList.get(0).trim();
    options.mergeClients = Boolean.parseBoolean(argList.get(1).trim());
    options.mergeServers = Boolean.parseBoolean(argList.get(2).trim());
    return options;
  }
  
  @Override
  public String writeOptions(final MergeInterfaceClientsServersOptions options) {
    return ((((options.mergeInterface + ",") + Boolean.valueOf(options.mergeClients)) + ",") + Boolean.valueOf(options.mergeServers));
  }
  
  @Override
  public boolean supportsFilteredComponentsAsInput() {
    return true;
  }
}
