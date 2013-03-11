package edu.ksu.cis.bnj.ver3.influence.uid;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;

import edu.ksu.cis.bnj.ver3.core.*;
import edu.ksu.cis.bnj.ver3.influence.nrar.NodeRemovalArcReversal;
import edu.ksu.cis.bnj.ver3.plugin.IOPlugInLoader;
import edu.ksu.cis.bnj.ver3.streams.Importer;
import edu.ksu.cis.bnj.ver3.streams.OmniFormatV1_Reader;


public class UIDSimpleSolver {
	private class SDAG{
		BeliefNode decisionNode;
		BeliefNode[] chanceNodes;
		SDAG[] parents;
		
		public SDAG(BeliefNode n) {
			decisionNode = n;
			chanceNodes = null;
			parents = null;
		}
		
		public void printOut() {
			if ( decisionNode != null)
				System.out.println("decision node: "+decisionNode.getName());
			else
				System.out.println("decision node: sink");
			System.out.println("parent nodes: ");
			for (int i = 0; i < parents.length; i++)
				System.out.print(parents[i].decisionNode.getName()+" ");
			System.out.println();
			for (int i = 0; i < parents.length; i++)
				parents[i].printOut();
		}
	}
	
	private BeliefNetwork _OriginalNetwork;
	private BeliefNetwork _TransformedNetwork;
	private SDAG sink;
	HashSet<BeliefNode> decisionNodesSet;
	HashSet<BeliefNode> chanceNodesSet;
	
	public UIDSimpleSolver(BeliefNetwork _bn) {
		_OriginalNetwork = _bn;
		BeliefNode[] nodes = _OriginalNetwork.getNodes();
		decisionNodesSet = new HashSet<BeliefNode>();
		chanceNodesSet = new HashSet<BeliefNode>();		
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i].getType() == nodes[i].NODE_DECISION)
				decisionNodesSet.add(nodes[i]);
			if (nodes[i].getType() == nodes[i].NODE_CHANCE)
				chanceNodesSet.add(nodes[i]);
		}
		sink = new SDAG(null);
		HashSet<BeliefNode> future = new HashSet<BeliefNode>();
		SDAGGenRec(future, sink);
		sink.printOut();

	}
	
	public void SDAGGenRec(HashSet<BeliefNode> future, SDAG current) {
		
		HashSet<BeliefNode> candidates = (HashSet<BeliefNode>)decisionNodesSet.clone();
		candidates.removeAll(future);
		Hashtable<BeliefNode, HashSet<BeliefNode>> table = new Hashtable<BeliefNode, HashSet<BeliefNode>>();

		for (BeliefNode n: candidates) {
			BeliefNode[] descendants = _OriginalNetwork.descendants(n);
			HashSet<BeliefNode> de = new HashSet<BeliefNode> (Arrays.asList(descendants));
			de.removeAll(future);
			table.put(n, de);
		}
		HashSet<BeliefNode> parents = new HashSet<BeliefNode>();
		HashSet<BeliefNode> min_de = new HashSet<BeliefNode>();
		for (BeliefNode n: table.keySet()) {
			if (parents.isEmpty()) {
				parents.add(n);
				min_de = table.get(n);
			}
			else {
				if (table.get(n).equals(min_de))
					parents.add(n);
				else 
					if (min_de.containsAll(table.get(n))) {
						parents.clear();
						parents.add(n);
						min_de = table.get(n);
					}
						
			}
		}
		
		current.parents = new SDAG[parents.size()];
		if (current.decisionNode != null)
			System.out.println(current.decisionNode.getName());
		System.out.println("parents: "+parents.toString());
		System.out.println("future: "+future.toString());
		int i = 0;
		for (BeliefNode n: parents) {
			SDAG p = new SDAG(n);
			current.parents[i] = p;
			i++;
			HashSet<BeliefNode> futurenew = (HashSet<BeliefNode> )future.clone();
			futurenew.add(n);

			SDAGGenRec(futurenew, p);
		}
		
		
	}
	
	public static void main(String[] args) {
		String f = "/home/ming/git/BNJ3ver2/bnjv3/Prisoner's Dilemma.xml";
		try {
			FileInputStream FIS = new FileInputStream(f);
			IOPlugInLoader pil = IOPlugInLoader.getInstance();
			Importer IMP = pil.GetImporterByExt(pil.GetExt(f));
			OmniFormatV1_Reader ofv1w = new OmniFormatV1_Reader();
			IMP.load(FIS, ofv1w);
			BeliefNetwork _bn = ofv1w.GetBeliefNetwork(0);
			UIDSimpleSolver uid = new UIDSimpleSolver(_bn);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//System.out.println("Successfully execute this function!");
	}
	
	
	
}