package tracediff;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;

import tracediff.util.Pair;

import java.util.List;

public class StepChangeType {
	public static int IDT = 0;
	public static int SRC = 1;
	public static int DAT = 2;
	public static int CTL = 3;
	
	private int type;
	private TraceNode matchingStep;
	private List<Pair<VarValue, VarValue>> wrongVariableList;
	
	public StepChangeType(int type, TraceNode matchedStep) {
		super();
		this.type = type;
		this.matchingStep = matchedStep;
	}

	public StepChangeType(int type, TraceNode matchingStep, List<Pair<VarValue, VarValue>> wrongVariableList) {
		super();
		this.type = type;
		this.matchingStep = matchingStep;
		this.wrongVariableList = wrongVariableList;
	}
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public List<Pair<VarValue, VarValue>> getWrongVariableList() {
		return wrongVariableList;
	}

	public TraceNode getMatchingStep() {
		return matchingStep;
	}


}
