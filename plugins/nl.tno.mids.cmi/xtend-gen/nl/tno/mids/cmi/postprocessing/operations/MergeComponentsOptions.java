package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Preconditions;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Pure;

@Accessors
@SuppressWarnings("all")
public class MergeComponentsOptions extends PostProcessingOperationOptions {
  /**
   * Regular expression pattern to use to match component names. The regular expression must contain a named match
   * called 'name' that will be used as the name for the new component. For instance, using regular expression
   * {@code "(?<name>\\w+XX)\\d+"} will match 'abcXX1', 'abcXX2', 'defXX34', 'defXX71', etc, where
   * 'abcXX1' and 'abcXX2' are merged into 'abcXX', etc.
   */
  public String pattern;
  
  @Override
  public void validate() throws IllegalStateException {
    Preconditions.checkState((this.pattern != null));
    int _length = this.pattern.trim().length();
    boolean _greaterThan = (_length > 0);
    Preconditions.checkState(_greaterThan);
    try {
      Pattern.compile(this.pattern);
    } catch (final Throwable _t) {
      if (_t instanceof PatternSyntaxException) {
        final PatternSyntaxException e = (PatternSyntaxException)_t;
        throw new IllegalStateException(("Invalid pattern for MergeComponents operation: " + this.pattern), e);
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
  
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(PostProcessingOperationProvider.getOperationFormalName(MergeComponents.class));
    builder.append(" ");
    builder.append(this.pattern);
    builder.append(" ");
    builder.append(super.toString());
    return builder.toString();
  }
  
  @Pure
  public String getPattern() {
    return this.pattern;
  }
  
  public void setPattern(final String pattern) {
    this.pattern = pattern;
  }
}
