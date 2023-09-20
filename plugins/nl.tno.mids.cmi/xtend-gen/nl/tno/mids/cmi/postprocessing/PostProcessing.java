package nl.tno.mids.cmi.postprocessing;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class PostProcessing {
  private PostProcessing() {
  }
  
  /**
   * Perform post-processing.
   * 
   * @param componentsMap Mapping from component name (absolute name of the CIF automaton) to CIF specification for
   *      that component. May be modified in-place, but should not be used after this call. Use the return value of
   *      this method instead.
   * @param relativeResolvePath The absolute path of the input file. During post-processing, all relative paths are
   *      to be resolved against the directory that contains this file.
   * @param operationsOptions The options for each of the post-processing operations to perform.
   * @param monitor The progress monitor.
   * @return Post-processing result, as mapping from component name (absolute name of the CIF automaton) to CIF
   *      specification for that component.
   */
  public static Map<String, Specification> postProcess(final Map<String, Specification> componentsMap, final Path relativeResolvePath, final List<PostProcessingOperationOptions> operationsOptions, final IProgressMonitor monitor) {
    int _size = operationsOptions.size();
    int _plus = (_size + 1);
    final SubMonitor subMonitor = SubMonitor.convert(monitor, _plus);
    subMonitor.split(1);
    boolean _isEmpty = operationsOptions.isEmpty();
    if (_isEmpty) {
      return componentsMap;
    }
    int _size_1 = componentsMap.size();
    final LinkedHashMap<String, PostProcessingModel> modelsToProcess = new LinkedHashMap<String, PostProcessingModel>(_size_1);
    Set<Map.Entry<String, Specification>> _entrySet = componentsMap.entrySet();
    for (final Map.Entry<String, Specification> componentEntry : _entrySet) {
      {
        final String name = componentEntry.getKey();
        final Specification spec = componentEntry.getValue();
        PostProcessingStatus _postProcessingStatus = new PostProcessingStatus(false, false);
        final PostProcessingModelCifSpec inputModel = new PostProcessingModelCifSpec(spec, name, _postProcessingStatus);
        modelsToProcess.put(name, inputModel);
      }
    }
    for (final PostProcessingOperationOptions operationOptions : operationsOptions) {
      {
        String _string = operationOptions.toString();
        String _plus_1 = ("Performing post-processing operation: " + _string);
        subMonitor.setTaskName(_plus_1);
        try {
          operationOptions.validate();
        } catch (final Throwable _t) {
          if (_t instanceof IllegalStateException) {
            final IllegalStateException e = (IllegalStateException)_t;
            throw new RuntimeException(("Options validation failed: " + operationOptions), e);
          } else {
            throw Exceptions.sneakyThrow(_t);
          }
        }
        final PostProcessingOperationProvider<?, ?> provider = operationOptions.<PostProcessingOperation<PostProcessingOperationOptions>, PostProcessingOperationOptions>getProvider();
        Set<String> selectedComponents = null;
        boolean _supportsFilteredComponentsAsInput = provider.supportsFilteredComponentsAsInput();
        if (_supportsFilteredComponentsAsInput) {
          Set<String> _switchResult = null;
          final PostProcessingFilterMode _switchValue = operationOptions.filterMode;
          if (_switchValue != null) {
            switch (_switchValue) {
              case NONE:
                _switchResult = modelsToProcess.keySet();
                break;
              case INCLUSION:
                Set<String> _xblockexpression = null;
                {
                  final Pattern filterPattern = Pattern.compile(operationOptions.filterPattern);
                  final Function1<String, Boolean> _function = (String k) -> {
                    return Boolean.valueOf(filterPattern.matcher(k).matches());
                  };
                  _xblockexpression = IterableExtensions.<String>toSet(IterableExtensions.<String>filter(modelsToProcess.keySet(), _function));
                }
                _switchResult = _xblockexpression;
                break;
              case EXCLUSION:
                Set<String> _xblockexpression_1 = null;
                {
                  final Pattern filterPattern = Pattern.compile(operationOptions.filterPattern);
                  final Function1<String, Boolean> _function = (String k) -> {
                    boolean _matches = filterPattern.matcher(k).matches();
                    return Boolean.valueOf((!_matches));
                  };
                  _xblockexpression_1 = IterableExtensions.<String>toSet(IterableExtensions.<String>filter(modelsToProcess.keySet(), _function));
                }
                _switchResult = _xblockexpression_1;
                break;
              default:
                break;
            }
          }
          selectedComponents = _switchResult;
        } else {
          selectedComponents = modelsToProcess.keySet();
        }
        final PostProcessingOperation<?> operation = provider.getOperationFromRawOptions(operationOptions);
        operation.applyOperation(modelsToProcess, selectedComponents, relativeResolvePath, subMonitor.split(1));
      }
    }
    int _size_2 = modelsToProcess.size();
    final LinkedHashMap<String, Specification> processedModels = new LinkedHashMap<String, Specification>(_size_2);
    Set<Map.Entry<String, PostProcessingModel>> _entrySet_1 = modelsToProcess.entrySet();
    for (final Map.Entry<String, PostProcessingModel> entry : _entrySet_1) {
      {
        final String name = entry.getKey();
        final PostProcessingModel model = entry.getValue();
        final Specification spec = model.getCifSpec();
        processedModels.put(name, spec);
      }
    }
    return processedModels;
  }
}
