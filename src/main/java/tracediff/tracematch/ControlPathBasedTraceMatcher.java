package tracediff.tracematch;

import tracediff.graphdiff.GraphDiff;
import tracediff.graphdiff.HierarchyGraphDiffer;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import tracediff.TraceNodePairReverseOrderComparator;
import tracediff.model.PairList;
import tracediff.model.TraceNodePair;
import tracediff.separatesnapshots.DiffMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControlPathBasedTraceMatcher {

	public PairList matchTraceNodePair(Trace mutatedTrace, Trace correctTrace, DiffMatcher matcher) {

		IndexTreeNode beforeRoot = initVirtualRootWrapper(mutatedTrace);
		IndexTreeNode afterRoot = initVirtualRootWrapper(correctTrace);

		HierarchyGraphDiffer differ = new HierarchyGraphDiffer();
		differ.diff(beforeRoot, afterRoot, false, new HierarchicalIndexTreeMatcher(matcher), -1);

		List<GraphDiff> diffList = differ.getDiffs();
		List<TraceNodePair> pList = new ArrayList<>();
		for (GraphDiff diff : diffList) {
			if (diff.getDiffType().equals(GraphDiff.UPDATE)) {
				IndexTreeNode wrapperBefore = (IndexTreeNode) diff.getNodeBefore();
				IndexTreeNode wrapperAfter = (IndexTreeNode) diff.getNodeAfter();

				TraceNodePair pair = new TraceNodePair(wrapperBefore.getTraceNode(), wrapperAfter.getTraceNode());
				pair.setExactSame(false);
				pList.add(pair);
			}
		}

		for (GraphDiff common : differ.getCommons()) {
			IndexTreeNode wrapperBefore = (IndexTreeNode) common.getNodeBefore();
			IndexTreeNode wrapperAfter = (IndexTreeNode) common.getNodeAfter();

			TraceNodePair pair = new TraceNodePair(wrapperBefore.getTraceNode(), wrapperAfter.getTraceNode());
			pair.setExactSame(true);
			pList.add(pair);
		}

		Collections.sort(pList, new TraceNodePairReverseOrderComparator());
		PairList pairList = new PairList(pList);
		return pairList;
	}

	private IndexTreeNode initVirtualRootWrapper(Trace trace) {
		TraceNode virtualNode = new TraceNode(null, null, -1, trace, "");
		List<TraceNode> topList = trace.getTopMethodLevelNodes();
		virtualNode.setInvocationChildren(topList);

		Map<TraceNode, IndexTreeNode> linkMap = new HashMap<TraceNode, IndexTreeNode>();

		IndexTreeNode root = new IndexTreeNode(virtualNode, linkMap);

		return root;
	}
}
