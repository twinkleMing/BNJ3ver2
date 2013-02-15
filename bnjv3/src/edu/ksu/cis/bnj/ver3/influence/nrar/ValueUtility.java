package edu.ksu.cis.bnj.ver3.influence.nrar;

import java.util.ArrayList;

import edu.ksu.cis.bnj.ver3.core.BeliefNode;
import edu.ksu.cis.bnj.ver3.core.Value;

public class ValueUtility implements Value {
	Value Utility;
	BeliefNode[] DecisionNodes;
	int[] DecisionValues;
	
	public ValueUtility(Value u) {
		Utility = u;
		DecisionNodes = null;
		DecisionValues = null;
		
	}
	public ValueUtility (Value u, BeliefNode[] oldnodes, int[] oldv) {
		Utility = u;
		DecisionNodes =oldnodes;
		DecisionValues  = oldv;
	}	
	public ValueUtility (Value u, BeliefNode[] oldnodes, int[] oldv, BeliefNode node, int v) {
		Utility = u;
		int l = (oldnodes == null ? 1 : oldnodes.length+1);
		DecisionNodes = new BeliefNode[l];
		DecisionValues = new int[l];
		int i = 0;
		for (i = 0; i < l-1; i++) {
			DecisionNodes[i] = oldnodes[i];
			DecisionValues[i] = oldv[i];
		}
		DecisionNodes[i] = node;
		DecisionValues[i] = v;
	}	
	
	
	
	public Value getUtility() {
		return Utility;
	}
	public BeliefNode[] getNodes() {
		return DecisionNodes;
	}
	public int[] getValues() {
		return DecisionValues;
	}
	@Override
	public String getExpr() {
		String str = "";
		if (DecisionNodes != null) {
			for ( int i = 0; i <DecisionNodes.length; i++) {
				str += DecisionNodes[i].getName() + " = " + DecisionValues[i] + ",";
			}
		}

		// TODO Auto-generated method stub
		return ""+Utility.getExpr()+" ( "+ str + " )";
	}

}