# Simple first example

We will look at a simple first example of the use of MIDS:

* Get MIDS:
  * Downloads MIDS from the [MIDS download page](https://tno.github.io/MIDS/).
  * Extract MIDS and put its `bin` directory in your `PATH`.
  * Make sure you don't forget to install the [dependencies](https://tno.github.io/MIDS/#dependencies) and also make them available on your `PATH`.
* Get the input traces:
  * Download [example_simple1.tmsct](../examples/simple1.tmsct).
  * Download [example_simple2.tmsct](../examples/simple2.tmsct).
* Infer behavioral models:
  * Open a shell or command prompt in the directory where you downloaded the traces.
  * To infer models for the first trace, execute: `mids-cmi -input simple1.tmsct -yed -single-model` (use `mids-cmi.exe` instead of `mids-cmi` on Windows).
  * Open the generated 
