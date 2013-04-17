package edu.ksu.cis.bnj.ver3.influence.uid;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;

import edu.ksu.cis.bnj.ver3.core.*;
import edu.ksu.cis.bnj.ver3.influence.nrar.VariableElimination;
import edu.ksu.cis.bnj.ver3.plugin.IOPlugInLoader;
import edu.ksu.cis.bnj.ver3.streams.Importer;
import edu.ksu.cis.bnj.ver3.streams.OmniFormatV1_Reader;



public class UIDSolver {
	private class GSskeleton {
		ArrayList<Node> nodes;
		ArrayList<HashSet<Integer>> adj;
		
		public GSskeleton() {
			nodes = new ArrayList<Node>();
			adj = new ArrayList<HashSet<Integer>>();
		}
		public int contains(HashSet<BeliefNode> label) {
			for (Node n : nodes) {
				if (n.label.equals(label))
					return nodes.indexOf(n);
			}
			return -1;
		}
		public int addNode(Node n) {
			nodes.add(n);
			adj.add(new HashSet<Integer>());
			return nodes.indexOf(n);
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < nodes.size(); i++) {
				sb.append("node "+i+": "+ nodes.get(i).toString()+"\n");
			}
			sb.append("------------------------\n");
			for (int i = 0; i < adj.size(); i++) {
				sb.append("adj "+i+": "+adj.get(i).toString()+"\n");
			}
			sb.append("=========================\n");
			return sb.toString();
		}

	}
	private class Node {
		HashSet<BeliefNode> label;
		HashSet<BeliefNode> futureminuslabel;
		public Node(HashSet<BeliefNode> l, HashSet<BeliefNode> f) {
			label = l;
			futureminuslabel = f;
		}
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("label: "+label+"\tfuture: "+future());
			return sb.toString();
		}
		public HashSet<BeliefNode> future() {
			HashSet<BeliefNode> f = new HashSet<BeliefNode>();
			f.addAll(label);
			f.addAll(futureminuslabel);
			return f;
		}
	}
	
	private BeliefNetwork _OriginalNetwork;
	private BeliefNetwork _TransformedNetwork;
	private HashSet<BeliefNode> decisionNodesSet;
	private HashSet<BeliefNode> observableNodesSet;
	private HashSet<BeliefNode> nonobservableNodesSet;
	private GSskeleton G;
	
	public UIDSolver(BeliefNetwork bn) {
		_OriginalNetwork = bn;
		BeliefNode[] nodes = _OriginalNetwork.getNodes();
		decisionNodesSet = new HashSet<BeliefNode>();
		observableNodesSet = new HashSet<BeliefNode>();	 // first assume all chance nodes are observable	
		nonobservableNodesSet = new HashSet<BeliefNode>();	
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i].getType() == nodes[i].NODE_DECISION)
				decisionNodesSet.add(nodes[i]);
			if (nodes[i].getType() == nodes[i].NODE_CHANCE)
				observableNodesSet.add(nodes[i]);
		}
	}
	
	public void GSskeletonGen() {
		G = new GSskeleton();
		ArrayList<Integer> process = new ArrayList<Integer> ();
		HashSet<BeliefNode> free = new HashSet<BeliefNode>();
		
		mainloop:
		for (BeliefNode n: decisionNodesSet) {
			BeliefNode[] children = _OriginalNetwork.getChildren(n);
			for (int j = 0; j < children.length; j++) {
				if (observableNodesSet.contains(children[j]))
					continue mainloop;
			}
			free.add(n);
		}
		
		Node start = new Node(free, new HashSet<BeliefNode> ());
		process.add(G.addNode(start));
		
		while (!process.isEmpty()) {
			int pNodeindex = process.remove(0);
			Node pNode = G.nodes.get(pNodeindex);
			if (!pNode.future().containsAll(decisionNodesSet)) {
				Collection<HashSet<BeliefNode>> parents = findParents(pNode);
				for (HashSet<BeliefNode> parent : parents) {
					int index = G.contains(parent);
					if (index == -1) {
						index = G.addNode(new Node(parent, pNode.future()));
						process.add(index);
					}
					G.adj.get(index).add(pNodeindex);
				}
			}
		}
		
		System.out.println(G.toString());
	}
		
	public Collection<HashSet<BeliefNode>> findParents(Node pNode) {
		Hashtable<HashSet<BeliefNode>, BeliefNode> deTable = new Hashtable<HashSet<BeliefNode>, BeliefNode>();
		HashSet<BeliefNode> decisions = decisionNodesSet;
		decisions.removeAll(pNode.future());
		for (BeliefNode node : decisions) {
			HashSet<BeliefNode> de = new HashSet<BeliefNode>();
			de.addAll(Arrays.asList(_OriginalNetwork.descendants(node)));
			de.retainAll(observableNodesSet);
			de.removeAll(pNode.future());
			deTable.put(de,node);
			System.out.println(node+": "+ de);
		}
		
		
		Hashtable<HashSet<BeliefNode>, HashSet<BeliefNode>> eqTable = new Hashtable<HashSet<BeliefNode>, HashSet<BeliefNode>>();
		
		mainloop:
		for (HashSet<BeliefNode> de : deTable.keySet()) {
			for (HashSet<BeliefNode> cand_de : eqTable.keySet()) {
				if (cand_de.equals(de)) {
					eqTable.get(cand_de).add(deTable.get(de));
					continue mainloop;
				}
				if (cand_de.containsAll(de)) continue mainloop;
				if (de.containsAll(cand_de)) eqTable.remove(cand_de);
			}
			HashSet<BeliefNode> eq = new HashSet<BeliefNode> ();
			eq.add(deTable.get(de));
			eqTable.put(de, eq);
		}
		return eqTable.values();
	}
	
	public static void main(String[] args) {
		String f = "thinkboxx.xml";
		try {
			FileInputStream FIS = new FileInputStream(f);
			IOPlugInLoader pil = IOPlugInLoader.getInstance();
			Importer IMP = pil.GetImporterByExt(pil.GetExt(f));
			OmniFormatV1_Reader ofv1w = new OmniFormatV1_Reader();
			IMP.load(FIS, ofv1w);
			BeliefNetwork _bn = ofv1w.GetBeliefNetwork(0);
			UIDSolver solver = new UIDSolver(_bn);
			solver.GSskeletonGen();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//System.out.println("Successfully execute this function!");
	}
}