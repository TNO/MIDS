package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Preconditions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.Pure;

@Accessors
@SuppressWarnings("all")
public class MergeInterfaceClientsServersOptions extends PostProcessingOperationOptions {
  /**
   * Enable merging of client components.
   */
  public boolean mergeClients = true;
  
  /**
   * Enable merging of server components.
   */
  public boolean mergeServers = true;
  
  /**
   * Name of interface to merge. If empty, all interfaces are merged.
   */
  public String mergeInterface = "";
  
  @Override
  public void validate() throws IllegalStateException {
    Preconditions.checkState((this.mergeClients || this.mergeServers));
  }
  
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(PostProcessingOperationProvider.getOperationFormalName(MergeInterfaceClientsServers.class));
    builder.append(" ");
    builder.append(this.mergeClients);
    builder.append(" ");
    builder.append(this.mergeServers);
    builder.append(" ");
    builder.append(this.mergeInterface);
    builder.append(" ");
    builder.append(super.toString());
    return builder.toString();
  }
  
  @Pure
  public boolean isMergeClients() {
    return this.mergeClients;
  }
  
  public void setMergeClients(final boolean mergeClients) {
    this.mergeClients = mergeClients;
  }
  
  @Pure
  public boolean isMergeServers() {
    return this.mergeServers;
  }
  
  public void setMergeServers(final boolean mergeServers) {
    this.mergeServers = mergeServers;
  }
  
  @Pure
  public String getMergeInterface() {
    return this.mergeInterface;
  }
  
  public void setMergeInterface(final String mergeInterface) {
    this.mergeInterface = mergeInterface;
  }
}
