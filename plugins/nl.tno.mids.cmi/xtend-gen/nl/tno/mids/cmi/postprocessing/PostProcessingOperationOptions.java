package nl.tno.mids.cmi.postprocessing;

import java.util.Locale;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Pure;

@Accessors
@SuppressWarnings("all")
public abstract class PostProcessingOperationOptions implements Cloneable {
  /**
   * The filter mode to decide to which components to apply the post processing operation.
   *  Only used if the post processing operation supports filtering.
   */
  public PostProcessingFilterMode filterMode = PostProcessingFilterMode.NONE;
  
  /**
   * Regular expression pattern to use to match component names. Effect and applicability depend on
   * {@link #filterMode} and post processing operation filtering support.
   */
  public String filterPattern;
  
  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public abstract void validate() throws IllegalStateException;
  
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(this.filterMode.name().toLowerCase(Locale.US));
    final PostProcessingFilterMode filterMode = this.filterMode;
    if (filterMode != null) {
      switch (filterMode) {
        case NONE:
          break;
        case INCLUSION:
          builder.append(" ");
          builder.append(this.filterPattern);
          break;
        case EXCLUSION:
          builder.append(" ");
          builder.append(this.filterPattern);
          break;
        default:
          throw new RuntimeException(("Unknown filter mode: " + this.filterMode));
      }
    } else {
      throw new RuntimeException(("Unknown filter mode: " + this.filterMode));
    }
    return builder.toString();
  }
  
  public <T extends PostProcessingOperation<U>, U extends PostProcessingOperationOptions> PostProcessingOperationProvider<T, U> getProvider() {
    final PostProcessingOperationProvider<?, ?> provider = PostProcessingOperationProviders.getPostProcessingOperationProvider(this);
    return ((PostProcessingOperationProvider<T, U>) provider);
  }
  
  @Pure
  public PostProcessingFilterMode getFilterMode() {
    return this.filterMode;
  }
  
  public void setFilterMode(final PostProcessingFilterMode filterMode) {
    this.filterMode = filterMode;
  }
  
  @Pure
  public String getFilterPattern() {
    return this.filterPattern;
  }
  
  public void setFilterPattern(final String filterPattern) {
    this.filterPattern = filterPattern;
  }
}
