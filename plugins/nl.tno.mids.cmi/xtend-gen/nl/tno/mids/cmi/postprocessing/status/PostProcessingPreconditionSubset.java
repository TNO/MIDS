package nl.tno.mids.cmi.postprocessing.status;

import nl.tno.mids.cmi.postprocessing.PostProcessingModel;

@SuppressWarnings("all")
public class PostProcessingPreconditionSubset {
  /**
   * Whether
   * <ul>
   * <li>to require that data need to be present ({@code true})</li>
   * <li> to require that no data is present ({@code false})</li>
   * <li> data may or may not be present ({@code null}).</li>
   * </ul>
   */
  final Boolean data;
  
  /**
   * Whether
   * <ul>
   * <li>to require that tau events need to be present ({@code true})</li>
   * <li> to require no tau events are present ({@code false})</li>
   * <li> tau events may or may not be present ({@code null}).</li>
   * </ul>
   */
  final Boolean tau;
  
  public PostProcessingPreconditionSubset(final Boolean data, final Boolean tau) {
    this.data = data;
    this.tau = tau;
  }
  
  public boolean dataIsNotAllowed() {
    return Boolean.FALSE.equals(this.data);
  }
  
  public boolean dataIsRequired() {
    return Boolean.TRUE.equals(this.data);
  }
  
  public boolean dataIsOptional() {
    return (this.data == null);
  }
  
  public boolean tauIsNotAllowed() {
    return Boolean.FALSE.equals(this.tau);
  }
  
  public boolean tauIsRequired() {
    return Boolean.TRUE.equals(this.tau);
  }
  
  public boolean tauIsOptional() {
    return (this.tau == null);
  }
  
  /**
   * This method applies the postprocessing status to the receiver returning a (new) instance of {@link PostProcessingStatus}
   * 
   * @param postProcessingStatus
   *            The PostProcessingStatus to apply this precondition subset on.
   * @return a PostProcessingStatus reflecting the ensured precondition subset.
   */
  public PostProcessingStatus apply(final PostProcessingStatus postProcessingStatus) {
    return EnsureSubset.ensureSubset(postProcessingStatus, this);
  }
  
  /**
   * This method applies the postprocessing status to the receiver returning a (new) instance of {@link PostProcessingStatus}
   * 
   * @param postProcessingStatus
   *            The PostProcessingStatus to apply this precondition subset on.
   * @return a PostProcessingStatus reflecting the ensured precondition subset.
   */
  public PostProcessingModel ensureSubset(final PostProcessingModel postProcessingModel) {
    return EnsureSubset.ensureSubset(postProcessingModel, this);
  }
}
