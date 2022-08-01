package tracediff.model;

import microbat.model.trace.TraceNode;


public class TraceNodePair {

	private TraceNode beforeNode;
	private TraceNode afterdNode;
	
	private boolean isExactlySame;
	
	public TraceNodePair(TraceNode beforeNode, TraceNode afterNode) {
		this.beforeNode = beforeNode;
		this.afterdNode = afterNode;
	}

	public TraceNode getBeforeNode() {
		return beforeNode;
	}

	public TraceNode getAfterNode() {
		return afterdNode;
	}

	public void setExactSame(boolean b) {
		this.isExactlySame = b;
	}

	@Override
	public String toString() {
		return "TraceNodePair [originalNode=" + beforeNode + ", mutatedNode="
				+ afterdNode + ", isExactlySame=" + isExactlySame + "]";
	}
}
