package tracediff;

import microbat.model.trace.Trace;
import tracediff.model.PairList;
import tracediff.separatesnapshots.DiffMatcher;
import tracediff.tracematch.ControlPathBasedTraceMatcher;

// API class for calling trace diff
public class TraceDiff {
    public static PairList getTraceAlignment(String srcFolderPath, String testFolderPath, String buggyPath,
                                             String fixPath, Trace buggyTrace, Trace originalTrace) {
        DiffMatcher diffMatcher = new DiffMatcher(srcFolderPath, testFolderPath, buggyPath, fixPath);
        diffMatcher.matchCode();

        ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
        PairList pairList = traceMatcher.matchTraceNodePair(buggyTrace, originalTrace,
                diffMatcher);
        return pairList;
    }
}
