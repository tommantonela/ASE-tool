package smellhistory;


import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import smellhistory.smell.ArchSmell;

public class TimedGraph { // Only necessary to trace the evolution of cycles (so far)
	
	static final Logger logger = LogManager.getLogger(TimedGraph.class);
	
	private DirectedGraph<SmellNode, String> historyTree = null;
	private Collection<ArchSmell> cycles = null;
	
	private String[][] matrix = null;
	
	public TimedGraph() {
		historyTree = new DirectedSparseGraph<SmellNode, String>();
		cycles = SmellFactory.filterSmells(SmellFactory.CD_SMELL);
		this.buildTree();
	}
	
	public static final String SAME_SMELL_EDGE = "-sameSmell->";
	public static final String SMELL_DECREASE_EDGE = "-decrease->";
	public static final String SMELL_INCREASE_EDGE = "-increase->";
	
	public void clear() {
		matrix = null;
		historyTree = null;
		cycles = null;
		return;
	}
	
	private static boolean isSmallerThan(ArchSmell smell1, ArchSmell smell2) {
		String[] s1 = smell1.getDescription().split(";");
		String[] s2 = smell2.getDescription().split(";");
		
		List<String> listS1 = Arrays.asList(s1);
		List<String> listS2 = Arrays.asList(s2);
		
		return (TimedGraph.isSmaller(listS1, listS2));
	}
	
	public void checkSmellMatrix() {
		
		cycles = SmellFactory.filterSmells(SmellFactory.CD_SMELL);
		ArchSmell[] array = cycles.toArray(new ArchSmell[cycles.size()]);
		int nVersions = SmellFactory.getMaxVersions();
		
		matrix = new String[nVersions][cycles.size()];
		
		String version = null;
		ArchSmell cd = null;
		for (int i = 0; i < nVersions; i++) {
			version = SmellFactory.getVersion(i);
			for (int j =0; j < array.length; j++) {
				cd = array[j];
				if (cd.existsForVersion(version)) 
					matrix[i][j] = Integer.toString(cd.getSize());
				else
					matrix[i][j] = "X";
			}
		}	
		
//		System.out.println();	
//		System.out.println("Checking for blanks ...");
		logger.info("Checking for blanks ...");
		int blankCounter = 0;
		int patchedCounter = 0;
		for (int i = 0; i < array.length; i++) {
			if (this.hasBlanks(i)) {
				//System.out.println("\t smell with blanks: "+array[i].getName()+"  non-blanks= "+this.getNonBlanks(i).size());
				blankCounter++;
			}
			else { // non-blanks = 1
				Point seq = this.getNonBlanks(i).iterator().next();
				if (seq.getY() < nVersions-1) {
					//System.out.println("\t can be patched: "+array[i].getName());
					patchedCounter++;
				}
			}
		}
//		System.out.println("blanks= "+blankCounter);
//		System.out.println("patcheable= "+patchedCounter);		
		logger.info("blanks= "+blankCounter);
		logger.info("patcheable= "+patchedCounter);	
		
		return;
	}
	
//	public void prinMatrix() {
//		ArchSmell[] array = cycles.toArray(new ArchSmell[cycles.size()]);
//		int nVersions = SmellFactory.getMaxVersions();
//		
//		System.out.println("Printing matrix (CDs only) ...");
//		String version;
//		for (int i = 0; i < nVersions; i++) {
//			version = SmellFactory.getVersion(i);
//			System.out.print("\t"+i);
//		}
//		System.out.println();
//		for (int i = 0; i < array.length; i++) {
//			for (int j = -1; j < nVersions; j++) {
//				if (j == -1)
//					System.out.print(array[i].getName());
//				else
//					System.out.print("\t"+ matrix[j][i]);
//			}
//			if (array[i].patched())
//				System.out.println("\t-p");
//			else
//				System.out.println();
//		}
//		System.out.println();	
//		
//		return;
//	}
	
//	public static void prinADIMatrixAllSmells() {
//		
//		Collection<ArchSmell> allSmells = SmellFactory.getSmells();
//		ArchSmell[] arrayAllSmells = allSmells.toArray(new ArchSmell[allSmells.size()]);
//		int nVersions = SmellFactory.getMaxVersions();
//		
//		//String[][] matrixAllSmells = new String[nVersions][allSmells.size()];
//		
//		System.out.println("Printing matrix of ADI scores (for all smells)...");
//		System.out.println();
//		String version;
//		System.out.print("smell, ");
//		for (int i = 0; i < nVersions; i++) {
//			version = SmellFactory.getVersion(i);
//			if (i == nVersions-1)
//				System.out.print(version);
//			else
//				System.out.print(version + ", ");
//		}
//		System.out.println();
//		ArchSmell smell = null;
//		DecimalFormat df = new DecimalFormat("#0.00");
//		double adi = 0;
//		for (int i = 0; i < arrayAllSmells.length; i++) {
//			for (int j = -1; j < nVersions; j++) {
//				smell = arrayAllSmells[i];
//				if (j == -1)
//					System.out.print(smell.getName()+", ");
//				else {
//					version = SmellFactory.getVersion(j);
//					adi = smell.computeIndex(version);
//					if (j == nVersions-1) {
//						//System.out.print(adi);
//						System.out.print(df.format(adi));
//					}
//					else {
//						//System.out.print(adi+", ");
//						System.out.print(df.format(adi)+", ");
//					}
//				}
//			}
////			if (arrayAllSmells[i].patched())
////				System.out.println("\t-p");
////			else
//				System.out.println();
//		}
//		System.out.println();	
//		
//		return;
//	}
	
	public int patchSmells() {
		ArchSmell[] array = cycles.toArray(new ArchSmell[cycles.size()]);
		int nVersions = SmellFactory.getMaxVersions();
		
		ArchSmell s = null;
//		Point seqs = null;
		int patched = 0;
		for (int i = 0; i < array.length; i++) {
			if (!this.hasBlanks(i)) {
				Point seq = this.getNonBlanks(i).iterator().next();
				if (seq.getY() < nVersions-1) { // It´s patcheable
					s = array[i];
					if (this.tryPatchBlankSmell(s, seq, nVersions))
						patched++;
				}
			}

		}	
		
		return (patched);
	}
	
	private boolean tryPatchBlankSmell(ArchSmell smell, Point seq, int maxVersions) { 
		// Warning: the smells is assumed to be patcheable, 
		// and it´s not a general solution, only 1 blank allowed in the end
		
		ArchSmell[] array = cycles.toArray(new ArchSmell[cycles.size()]);
		
		ArchSmell s = null;
		Point seqs = null;
		boolean patched = false;
		double match = -1;
		double minMatch = Double.MAX_VALUE;
		int bestPatch = -1;
		for (int i = 0; i < array.length; i++) {
			s = array[i];
			if (!s.getName().equals(smell.getName())) {
				seqs = this.getNonBlanks(i).iterator().next();
				match = this.isSuccessor(smell, seq, s, seqs, maxVersions);
				if (match >= 0) {
					//System.out.println("patch for "+smell.getName()+" found!  "+s.getName());
					patched = true;
					if (match < minMatch) {
						minMatch = match;
						bestPatch = i;
					}
				}
			}
		}
		
		if (patched) {
			//System.out.println("best patch for "+smell.getName()+" found!  "+array[bestPatch].getName()+"  match= "+minMatch);
			seqs = this.getNonBlanks(bestPatch).iterator().next();
			
			// Patch here!! (i.e., copy parameters)
			String version = null;
			//System.out.println(seq.getY()+"  -->  "+seqs.getY());
			for (int j = (int)(seq.getY()); j <= (int)(seqs.getY()); j++) {
				version = SmellFactory.getVersion(j-1);
				smell.copyParameters(array[bestPatch], version);
				//System.out.println("\t... patching version: "+j+"  "+version);
			}
		}
		
		return (patched);
	}
	
	private double isSuccessor(ArchSmell s, Point seq, ArchSmell patch, Point patchseq, int maxVersions) {
		
		if (TimedGraph.isSmallerThan(s, patch)) {
//			if ((patchseq.getX() <= seq.getX()+1) && (patchseq.getY() == maxVersions-1))
			if ((patchseq.getX() <= seq.getX()+1) && (patchseq.getY() > seq.getY())) //Shouldn´t be patchseq.getY() == maxVersions-1?
					return (maxVersions-patchseq.getY());
		}

		if (TimedGraph.isSmallerThan(patch, s)) {
//			if ((patchseq.getX() <= seq.getX()+1) && (patchseq.getY() == maxVersions-1))
			if ((patchseq.getX() <= seq.getX()+1) && (patchseq.getY() > seq.getY())) //Shouldn´t be patchseq.getY() == maxVersions-1?
				return (maxVersions-patchseq.getY());
		}
		
		return (-1);
	}
	
	public int removeBlanks() {
		
		ArchSmell[] array = cycles.toArray(new ArchSmell[cycles.size()]);
//		int nVersions = SmellFactory.getMaxVersions();
		
		int removed = 0;
		for (int i = 0; i < array.length; i++) {
			if (this.hasBlanks(i)) {
				//System.out.println("\t smell with blanks: "+array[i].getName()+"  non-blanks= "+this.getNonBlanks(i).size());
				if (SmellFactory.removeSmell(array[i].getName()) != null)
					removed++;
			}
		}
		
		return (removed);
	}
	
	public boolean hasBlanks(int smell) {
		
		//int nVersions = SmellFactory.getMaxVersions();
		Collection<Point> c = this.getNonBlanks(smell);
		if (c.size() > 1) {
			return (true);
		}
		
		return (false);
	}
	
//	public boolean hasBlanks(int smell) {
//		
//		int nVersions = SmellFactory.getMaxVersions();
//		Collection<Point> c = this.getBlanks(smell);
//		if (c.size() > 2)
//			return (true);
//		else if (c.size() == 1) {
//			Point p = c.iterator().next();
//			if ((p.getX() > 0) && (p.getY() < nVersions-1))
//				return (true);
//		}
//		else if (c.size() == 2) {
//			Iterator<Point> it = c.iterator();
//			Point p1 = it.next();
//			Point p2 = it.next();
//			if ((p1.getX() == 0) && (p1.getY() < nVersions-1)) {
//				if ((p2.getX() > 1) && (p2.getY() < nVersions-1))
//					return (false);
//			}
//			return (true);
//		}
//		
//		return (false);
//	}
	
	public Collection<Point> getNonBlanks(int smell) {
		
		ArrayList<Point> result = new ArrayList<Point>();
		Point p = null;
		
		int nVersions = SmellFactory.getMaxVersions();
//		boolean number = false;
		int beginBlank = -1;
		int offsetBlank = -1;
		int i = 0; 
		while (i < nVersions) {
			while ((i < nVersions) && (matrix[i][smell].equals("X"))) {
				i++;
			}
			if (i == nVersions) {
				return (result);
			}
			else { // It found a sequence beginning with X
				beginBlank = i;
				offsetBlank = 0;
				while ((i < nVersions) && (!matrix[i][smell].equals("X"))) {
					i++;
					offsetBlank++;
				}
				p = new Point(beginBlank, beginBlank + offsetBlank);
				result.add(p);
				if (i == nVersions) {
					return (result);
				}
			}
		}

		return (result);
	}
	
	private void buildTree() {
		
		//System.out.println("Creating timed graph ... for CDs");
		logger.info("Creating timed graph ... for CDs (only)");
		
		//cycles = SmellFactory.filterSmells(SmellFactory.CD_SMELL);
		//System.out.println(cycles.size());
		int nVersions = SmellFactory.getMaxVersions();		
		
		SmellNode n = null;
		String version = null;
//		boolean added = false;
		int isFinal = 0;
		// Creates all nodes for all smells and versions in which the smells exist
		for (int i = 0; i < nVersions; i++) {
			version = SmellFactory.getVersion(i);
			//System.out.println(version);
			int cdsPerVersion = 0;
			for (ArchSmell cd: cycles) {
				if (cd.existsForVersion(version)) {
					cdsPerVersion++;
					n = new SmellNode(cd);
					boolean b = n.setVersion(version, i);
					if (!b)
						logger.debug("Warning: "+cd+" was not configured with version+time");
					historyTree.addVertex(n);
					
					if (i == 0) { //Roots
						n.setInitial();
						//roots.add(n);
					}
					
					if (!cd.existedBefore(version)) {
						n.setNew();
					}
					
					if (!cd.existsAfter(version)) {// && (i < nVersions-1)) {
						//System.out.println(i+"  Setting final ... "+cd.getName());
						if (!n.isFinal())
							isFinal++;
						n.setFinal();
					}
				}
			}			
			logger.debug(version+"  smells that existed: "+cdsPerVersion);
			
		}
		logger.debug("\tNODES created for "+cycles.size()+" CDs and "+nVersions+" versions: "+historyTree.getVertexCount()+"  isFinal= "+isFinal);
		
		// Engancha los nodos que corresponden al mismo smell
		int nEdge = 0;
		for (int i = 0; i < (nVersions-1); i++) {
			
			for (SmellNode pred: this.getNodesAtTime(i)) {
				for (SmellNode succ: this.getNodesAtTime(i+1)) {
//					added = false;

					//System.out.print("\tconsidering "+pred.getName()+" --> "+succ.getName()+"  ");
					if (pred.isEqualSmell(succ)) {
//						added = 
						historyTree.addEdge(SAME_SMELL_EDGE+nEdge, pred, succ);
						nEdge++;
					}	
					
					//System.out.println(added);
				}
			}
		}
		
		// Engancha los nodos
		int temp = nEdge;
		for (int i = 0; i < (nVersions-1); i++) {
			
			for (SmellNode pred: this.getNodesAtTime(i)) {
				for (SmellNode succ: this.getNodesAtTime(i+1)) {
//					added = false;

					if (pred.isFinal()) {
						if (TimedGraph.isSmallerThan(succ.getSmell(), pred.getSmell())) {
						//if (pred.isLargerThan(succ)) {
//							added = 
							historyTree.addEdge(SMELL_DECREASE_EDGE+nEdge, pred, succ);
							nEdge++;
						}
						else if (TimedGraph.isSmallerThan(succ.getSmell(), pred.getSmell())) {
						//else if (pred.isSmallerThan(succ)) {
//							added = 
							historyTree.addEdge(SMELL_INCREASE_EDGE+nEdge, pred, succ);		
							nEdge++;
						}
			
					}
					
					//System.out.println(added);
				}
			}
		}
		logger.debug("\tincreaseDecrease edges "+(nEdge-temp));
		logger.debug("\tEDGES created for "+cycles.size()+" CDs and "+nVersions+" : "+historyTree.getEdgeCount());
		
		logger.debug("==========================");
		logger.debug("\tinitial smells: "+this.countInitialSmells());
		logger.debug("\temerging smells: "+this.countEmergingSmell());		
		logger.debug("Checking for consistency ok= "+this.checkSuccessors());
				
		return;
	}
	
	public Collection<SmellNode> getNodesAtTime(int time) {
		ArrayList<SmellNode> result = new ArrayList<SmellNode>();
		
		for (SmellNode n: historyTree.getVertices()) {
			if (n.getTime() == time)
				result.add(n);
		}
		
		return (result);
	}
	
	public int countInitialSmells() {
		int counter = 0;
		//System.out.println(this.getNodesAtTime(0).size());
		for (SmellNode sn: this.getNodesAtTime(0)) {
			if (sn.isInitial())
				counter++;
		}
		
		return (counter);
	}
	
	public boolean checkSuccessors() {
		boolean warnings = false;
		int n = -1;
		for (SmellNode sn: historyTree.getVertices()) {
			n = historyTree.getSuccessorCount(sn);
			if ((n > 1) && sn.isFinal()) {
				//System.out.println("Warning: many successors for node "+sn.getName()+" "+n+"  isFinal= "+sn.isFinal());
				//this.printSuccessors(sn);
				warnings = true;
			}
		}
		
		return (!warnings);
	} 
	
//	public void printSuccessors(SmellNode pred) {
//		
//		String edge = null;
//		for (SmellNode succ: historyTree.getSuccessors(pred)) {
//			edge = historyTree.findEdge(pred, succ);
//			//if (pred.isFinal())
//			System.out.println("\t"+pred.getName()+"  "+edge+"  "+succ.getName()+"  "+pred.isFinal()+"  "+succ.isFinal());
//		}
//		
//		return;
//	}

	public int countEmergingSmell() {
		int counter = 0;
		for (SmellNode sn: historyTree.getVertices()) {
//			if (!sn.isInitial()) {
				if (historyTree.getInEdges(sn).size() == 0)
					counter++;
//			}
		}
		
		return (counter);
	}
	
	public static boolean isSmaller(List<String> potential_smaller,List<String> bigger) {

		int first_index = bigger.indexOf(potential_smaller.get(0));

		if (first_index < 0) //ya el primero no existe
			return false;

		int number_decreased = 0;
		int index = first_index;

		for (int i=1; i< potential_smaller.size();i++){ //por cada uno de los paquetes en el pequeño

			int current_index = bigger.indexOf(potential_smaller.get(i));

			if(current_index < 0) //si no lo encontró, no va a existir
				return false;

			if(number_decreased == 0){ //si todavía no bajó, puede bajar uno
				if(current_index < index)
					number_decreased++;
			}
			else{//si ya bajó, no puede volver a bajar

				if(current_index < index)
					return false;

				if(current_index > first_index)
					return false;
			}

			index = current_index;
		}
		return true;
	}
	
	// For internal usage 
	class SmellNode {
		
		private ArchSmell mySmell = null;
		private int time = -1;
//		private String version = null;
		private boolean isNew = false;
		private boolean isInitial = false;
		private boolean isFinal = false;
		
		public SmellNode(ArchSmell s) {
			mySmell = s;
//			version = null;
			time = -1;
			isInitial = false;
			isNew = false;
			isFinal = false;
		}
		
		public void setFinal() {
			isFinal = true;
		}
		
		public boolean isFinal() {
			return (isFinal);
		}
		
		public void setInitial() {
			isInitial = true;
			isNew = false;
		}
		
		public void setNew() {
			isInitial = false;
			isNew = true;
		}
		
		public boolean isNew() {
			return (isNew);
		}
		
		public boolean isInitial() {
			return (isInitial);
		}

		public boolean setVersion(String v, int t) {
			if (mySmell.existsForVersion(v)) {
//				version = v;
				time = t;
				return (true);
			}
			
			return (false);
		}

		public boolean isEqualNode(SmellNode node) {
			return (this.getName().equals(node.getName()));
		}

		public boolean isEqualSmell(SmellNode node) {
			return (this.getSmell().getName().equals(node.getSmell().getName()));
		}

		public boolean isLargerThan(SmellNode node) {
			String s1 = this.getSmell().getDescription();
			String s2 = node.getSmell().getDescription();
			
			return (s1.startsWith(s2) && (s1.length() > s2.length()));
		}
		
		public boolean isSmallerThan(SmellNode node) {
			String s1 = this.getSmell().getDescription();
			String s2 = node.getSmell().getDescription();
			
			return (s2.startsWith(s1) && (s2.length() > s1.length()));
		}
		
		public ArchSmell getSmell() {
			return (mySmell);
		}
		
		public int getTime() {
			return (time);
		}
		
		public String getName() {
			return (mySmell.getName()+"-"+time);
		}
		
	}
}
