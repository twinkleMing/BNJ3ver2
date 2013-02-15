package edu.ksu.cis.bnj.ver3.influence.nrar;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;

import edu.ksu.cis.bnj.ver3.core.BeliefNetwork;
import edu.ksu.cis.bnj.ver3.core.BeliefNode;
import edu.ksu.cis.bnj.ver3.plugin.IOPlugInLoader;
import edu.ksu.cis.bnj.ver3.streams.Importer;
import edu.ksu.cis.bnj.ver3.streams.OmniFormatV1_Reader;
import edu.ksu.cis.bnj.ver3.core.BeliefNetwork;
import edu.ksu.cis.bnj.ver3.core.BeliefNode;
import edu.ksu.cis.bnj.ver3.core.CPF;
import edu.ksu.cis.bnj.ver3.core.DiscreteEvidence;
import edu.ksu.cis.bnj.ver3.core.Value;
import edu.ksu.cis.bnj.ver3.core.values.Field;
import edu.ksu.cis.bnj.ver3.core.values.ValueDouble;
import edu.ksu.cis.bnj.ver3.core.values.ValueUnity;
import edu.ksu.cis.bnj.ver3.core.values.ValueZero;
import edu.ksu.cis.bnj.ver3.inference.Inference;
import edu.ksu.cis.bnj.ver3.inference.exact.LSonline;
import edu.ksu.cis.bnj.ver3.influence.Solver;
import edu.ksu.cis.bnj.ver3.influence.nfseq.Preconditions;
import edu.ksu.cis.bnj.ver3.plugin.IOPlugInLoader;
import edu.ksu.cis.bnj.ver3.streams.Importer;
import edu.ksu.cis.bnj.ver3.streams.OmniFormatV1_Reader;
import edu.ksu.cis.util.graph.core.*;

public class NodeRemovalArcReversal {
	private BeliefNetwork	_TransformedNetwork;
	private BeliefNetwork	_OriginalNetwork;
	
	public void Show() {
		System.out.println("======================================");
		BeliefNode[] OriginalNodes = _OriginalNetwork.getNodes();
		for (int i = 0; i < OriginalNodes.length; i++) {
			BeliefNode node = OriginalNodes[i];
			System.out.println(node.getName());
			System.out.println("children:");
			BeliefNode[] children = _OriginalNetwork.getChildren(node);
			for (int j = 0; j < children.length; j++) {
				System.out.println(children[j].getName());
			}
			System.out.println();
			
		}
		System.out.println("======================================");
	}
	

	
	
	public void UtilityFormatChange() {
		BeliefNode[] nodes = _OriginalNetwork.getNodes();
		for ( int i = 0; i < nodes.length; i++) {
			BeliefNode node = nodes[i];
			if (node.getType() == node.NODE_UTILITY) {
				CPF cpf = node.getCPF();
				for ( int j = 0; j < cpf.size(); j++) {
					Value v = cpf.get(j);
					ValueUtility d = new ValueUtility(v);
					cpf.put(j, d);
				}
				
			}
		}
	}
	
	public void cpfUtilityFormatChange(CPF cpf) {
		for ( int j = 0; j < cpf.size(); j++) {
			Value v = cpf.get(j);
			if ( v instanceof ValueZero)
				v= new ValueDouble(0.0);
			ValueUtility d = new ValueUtility(v);
			cpf.put(j, d);
		}		
	}
	public void BarrenNodesRemoval() {

		BeliefNode node;
		boolean flag= true;
		while (flag) {
			flag = false;
			BeliefNode[] OriginalNodes = _OriginalNetwork.getNodes();
			for ( int i = 0; i < OriginalNodes.length; i++) {
				node = OriginalNodes[i];
				if ( node.getType() == node.NODE_CHANCE || node.getType() == node.NODE_DECISION) {
					BeliefNode[] children = _OriginalNetwork.getChildren(node);
					if ( children.length == 0) {
						flag = true;
						System.out.println("deleted node:"+node.getName());
						_OriginalNetwork.deleteBeliefNode(node);
					}
				}
		
			}			
		}
		
	}
	
	public void ChanceNodeRemoval() {
		BeliefNode[] OriginalNodes = _OriginalNetwork.getNodes();
		BeliefNode node;
		boolean flag;
		ArrayList<Integer>delete = new ArrayList<Integer>();
		Show() ;

		for ( int i = 0; i < OriginalNodes.length; i++) {
			node = OriginalNodes[i];
			System.out.println(node.getName());
			node.getCPF().Print();
			if ( node.getType() == node.NODE_CHANCE ){
				BeliefNode[] children = _OriginalNetwork.getChildren(node);
				flag = true;
				for (int j = 0; j < children.length; j++) {
					if (children[j].getType() != node.NODE_UTILITY) 
						flag = false;		
						
				}
				if (flag) delete.add(i);
			}
			
		}
		for (int i = 0; i < delete.size(); i++) {
			node = OriginalNodes[delete.get(i)];
			System.out.println("delete node: "+node.getName());
			BeliefNode[] children = _OriginalNetwork.getChildren(node);
			for (int j = 0; j < children.length; j++) {
				nodeDisconnect(node, children[j]);
			}
			_OriginalNetwork.deleteBeliefNode(node);
		}
		Show();
	}
	
	public boolean  decisionDisconnect(BeliefNode from, BeliefNode to) {
		if (from.getType() != from.NODE_DECISION || to.getType() != to.NODE_UTILITY)
				return false;
		HashSet<BeliefNode>  from_parents = new HashSet<BeliefNode>(Arrays.asList(_OriginalNetwork.getParents(from)));
		HashSet<BeliefNode>  to_parents = new HashSet<BeliefNode>(Arrays.asList(_OriginalNetwork.getParents(to)));
		HashSet<BeliefNode> intersect = new HashSet<BeliefNode>(from_parents);
		intersect.retainAll(to_parents);
		from_parents.removeAll(intersect);
		BeliefNode[] X = new BeliefNode[from_parents.size()];
		BeliefNode[] Y = new BeliefNode[intersect.size()];		
		BeliefNode[] Yi = new BeliefNode[to_parents.size()];
		from_parents.toArray(X);
		intersect.toArray(Y);
		to_parents.toArray(Yi);
		
		CPF to_cpf = to.getCPF();
		CPF cpf = new CPF(Y);
		cpfUtilityFormatChange(cpf);
		int index_from = to_cpf.getNodeIndex(from);
		for ( int i = 0; i < to_cpf.size(); i++) {
			int[] addr = to_cpf.realaddr2addr(i);
			int v_from = addr[index_from];
			ValueUtility v = (ValueUtility)to_cpf.get(i);
			int[] Yaddr = cpf.getSubQuery(addr, Yi);
			Value vDecision = cpf.get(Yaddr);
			if (vDecision instanceof ValueZero) {
				vDecision = new ValueUtility(v.getUtility(), null, null, from, v_from);
				cpf.put(Yaddr, vDecision);
			}
			else {
				ValueUtility voldDecision = (ValueUtility) vDecision;
				double vold = ((ValueDouble) voldDecision.getUtility()).getValue();
				double vcurr = ((ValueDouble) v.getUtility()).getValue();
				if (vold < vcurr) {
					vDecision = new ValueUtility(v.getUtility(), v.getNodes(), v.getValues(), from, v_from);
					cpf.put(Yaddr, vDecision);
				}
				
			}
			
		}
		cpf.Print();
		_OriginalNetwork.disconnect(from, to);
		return true;

	}
	
	
	
	public boolean ArcReversal(BeliefNode from, BeliefNode to) {
		if (from.getType() != from.NODE_CHANCE || to.getType() != to.NODE_CHANCE)
			return false;
		
		HashSet<BeliefNode>  from_parents = new HashSet<BeliefNode>(Arrays.asList(_OriginalNetwork.getParents(from)));
		HashSet<BeliefNode>  to_parents = new HashSet<BeliefNode>(Arrays.asList(_OriginalNetwork.getParents(to)));
		HashSet<BeliefNode> intersect = new HashSet<BeliefNode>(from_parents);
		intersect.retainAll(to_parents);
		from_parents.removeAll(intersect);
		to_parents.removeAll(intersect);
		to_parents.remove(from);
		

		BeliefNode[] X = new BeliefNode[from_parents.size()];
		BeliefNode[] Y = new BeliefNode[intersect.size()];
		BeliefNode[] Z = new BeliefNode[to_parents.size()];
		from_parents.toArray(X);
		intersect.toArray(Y);
		to_parents.toArray(Z);
		
		
		
		
		return true;
	}
	public void DecisionNodeRemoval() {

		
		
	}
	public void nodeDisconnect(BeliefNode from, BeliefNode to) {
		CPF from_cpf = from.getCPF();
		CPF to_cpf = to.getCPF();
		HashSet<BeliefNode>  from_parents = new HashSet<BeliefNode>(Arrays.asList(_OriginalNetwork.getParents(from)));
		HashSet<BeliefNode>  to_parents = new HashSet<BeliefNode>(Arrays.asList(_OriginalNetwork.getParents(to)));
		HashSet<BeliefNode> intersect = new HashSet<BeliefNode>(from_parents);
		intersect.retainAll(to_parents);
		from_parents.removeAll(intersect);
		to_parents.removeAll(intersect);
		to_parents.remove(from);
		

		BeliefNode[] X = new BeliefNode[from_parents.size()];
		BeliefNode[] Y = new BeliefNode[intersect.size()];
		BeliefNode[] Z = new BeliefNode[to_parents.size()];
		from_parents.toArray(X);
		intersect.toArray(Y);
		to_parents.toArray(Z);
		BeliefNode[] Whole = new BeliefNode[X.length+Y.length+Z.length+1];
		Whole[0] = to;
		System.arraycopy(X, 0, Whole, 1, X.length);
		System.arraycopy(Y, 0, Whole, X.length+1, Y.length);
		System.arraycopy(Z, 0, Whole, X.length+Y.length+1, Z.length);

		BeliefNode[] Whole_iter = new BeliefNode[Whole.length+1];
		System.arraycopy(Whole, 0, Whole_iter, 0, Whole.length);
		Whole_iter[Whole.length] = from;
		
		CPF cpf = new CPF(Whole);
		cpfUtilityFormatChange(cpf);
		CPF cpf_iter = new CPF(Whole_iter);
		for ( int i = 0; i < cpf_iter.size(); i++) {
			int[] addr = cpf_iter.realaddr2addr(i);
			int[] XYi = from_cpf.getSubQuery(addr, Whole_iter);
			Value pr = from_cpf.get(XYi);
			int[] YZiv = to_cpf.getSubQuery(addr, Whole_iter);
			Value v = (ValueDouble) ((ValueUtility)to_cpf.get(YZiv)).getUtility();
			int[] XYZv = cpf.getSubQuery(addr, Whole_iter);
			ValueUtility vv_u = (ValueUtility)cpf.get(XYZv);
			Value vv = (ValueDouble) vv_u.getUtility();
			Value result = Field.add(vv, Field.mult(pr, v));
			ValueUtility vnew = new ValueUtility(result, vv_u.getNodes(), vv_u.getValues());
			cpf.put(XYZv, vnew);

		}
		cpf.Print();
		_OriginalNetwork.disconnect(from, to);
		BeliefNode[] parents = _OriginalNetwork.getParents(from);
		for ( int i = 0; i < parents.length; i++) {
			_OriginalNetwork.connect(parents[i], to);
		}
		to.setCPF(cpf);
	}
	
	
	
	
	public void run(BeliefNetwork _bn) {
		_OriginalNetwork = _bn;
		UtilityFormatChange();
		BarrenNodesRemoval();
		ChanceNodeRemoval();
		BeliefNode[] Nodes = _OriginalNetwork.getNodes();
		decisionDisconnect(Nodes[0], Nodes[1]);
		Show();
		decisionDisconnect(Nodes[2], Nodes[1]);
		Show();
		/*
		BeliefNode[] Nodes = _OriginalNetwork.getNodes();
		nodeDisconnect(Nodes[0], Nodes[2]);
		Nodes[2].getCPF().Print();
		Show();
		*/
	}
	
	
	
	
	
	
	
	
	public static void main(String[] args) {
		String f = "/home/ming/git/BNJ3ver2/bnjv3/testid2.xml";
		try {
			FileInputStream FIS = new FileInputStream(f);
			IOPlugInLoader pil = IOPlugInLoader.getInstance();
			Importer IMP = pil.GetImporterByExt(pil.GetExt(f));
			OmniFormatV1_Reader ofv1w = new OmniFormatV1_Reader();
			IMP.load(FIS, ofv1w);
			BeliefNetwork _bn = ofv1w.GetBeliefNetwork(0);
			NodeRemovalArcReversal nrar = new NodeRemovalArcReversal();
			nrar.run(_bn);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//System.out.println("Successfully execute this function!");
	}
}