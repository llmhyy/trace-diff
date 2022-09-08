# trace-diff
Trace alignment code extracted out from tregression project. <br/>
Use this project to obtain the PairList for a non-buggy and buggy trace.<br/>
On tregression, the branch `feature/refactor-trace-diff` is available that makes use of the trace-diff project.

## Setup
Have maven and java 11 (or above) installed.<br/>
Run ./setup.bat
This should generate a jar file for trace-diff in the ./target directory

## Usage
Call the `tracediff.TraceDiff#getTraceAlignment` method to generate the PairList.<br/>
An example usage is provided in the TraceManger project (See tracemanager.TraceManager in https://github.com/bchenghi/trace-manager
