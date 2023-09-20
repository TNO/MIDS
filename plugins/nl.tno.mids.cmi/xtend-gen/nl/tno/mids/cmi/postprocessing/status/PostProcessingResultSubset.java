package nl.tno.mids.cmi.postprocessing.status;

@SuppressWarnings("all")
public class PostProcessingResultSubset {
  /**
   * Whether to indicate that at the end of the operation
   * <ul>
   * <li>data has been added or remains present ({@code true})</li>
   * <li>data has been removed or remains not present ({@code false})</li>
   * <li>any presence of data remains unchanged ({@code null}).</li>
   * </ul>
   */
  final Boolean data;
  
  /**
   * Whether to indicate that at the end of the operation
   * <ul>
   * <li>tau events have been added or remain present ({@code true})</li>
   * <li>tau events have been removed or remain not present ({@code false})</li>
   * <li>any presence of tau events remain unchanged ({@code null}).</li>
   * </ul>
   */
  final Boolean tau;
  
  public PostProcessingResultSubset(final Boolean data, final Boolean tau) {
    this.data = data;
    this.tau = tau;
  }
  
  /**
   * This method applies the resultSubset to the receiver returning a new instance of {@link PostProcessingStatus}
   * 
   * @param modificationSubset
   *            The modificationSubset to be applied.
   * @return a PostProcessingStatus reflecting the applied result subset.
   */
  public PostProcessingStatus apply(final PostProcessingStatus postProcessingStatus) {
    boolean _apply = this.apply(postProcessingStatus.dataIsPresent(), this.data);
    boolean _apply_1 = this.apply(postProcessingStatus.tauIsPresent(), this.tau);
    return new PostProcessingStatus(_apply, _apply_1);
  }
  
  private boolean apply(final boolean source, final Boolean target) {
    if ((target == null)) {
      return source;
    }
    return (target).booleanValue();
  }
}
