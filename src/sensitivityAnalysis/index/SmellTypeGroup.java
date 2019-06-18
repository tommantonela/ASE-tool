package sensitivityAnalysis.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import smellhistory.SmellFactory;
import smellhistory.smell.ArchSmell;
import smellhistory.smell.CDSmell;
import smellhistory.smell.HLSmell;
import smellhistory.smell.UDSmell;

//import parser.SmellFactory;

public class SmellTypeGroup extends SmellGroup {

	public static Collection<ArchSmell> filterSmells(String smellType, Collection<ArchSmell> smells) {
		
		ArrayList<ArchSmell> array = new ArrayList<ArchSmell>();
		
		if (smellType.equals(SmellFactory.CD_SMELL)) {
			for (ArchSmell s: smells) {
				if (s instanceof CDSmell)
					array.add(s);
			}
			return (array);
		}
		if (smellType.equals(SmellFactory.HL_SMELL)) {
			for (ArchSmell s: smells) {
				if (s instanceof HLSmell)
					array.add(s);
			}
			return (array);
		}	
		if (smellType.equals(SmellFactory.UD_SMELL)) {
			for (ArchSmell s: smells) {
				if (s instanceof UDSmell)
					array.add(s);
			}
			return (array);
		}
		
		return (array);

	}
	
	public static Collection<SmellGroup> generateGroups(Collection<ArchSmell> smells) {
		
		HashMap<String,SmellGroup> groups = new HashMap<String,SmellGroup>();
		
		SmellGroup cdGroup = new SmellTypeGroup(SmellFactory.CD_SMELL);
		groups.put(SmellFactory.CD_SMELL, cdGroup);
		cdGroup.initialize(SmellFactory.getSmells());
		
		SmellGroup udGroup = new SmellTypeGroup(SmellFactory.UD_SMELL);
		groups.put(SmellFactory.UD_SMELL, udGroup);
		udGroup.initialize(SmellFactory.getSmells());
				
		SmellGroup hlGroup = new SmellTypeGroup(SmellFactory.HL_SMELL);
		groups.put(SmellFactory.HL_SMELL, hlGroup);
		hlGroup.initialize(SmellFactory.getSmells());
		
		return (groups.values());
	}
	
	private String smellType = null;
	private Collection<ArchSmell> smellsByType = null;
	
	public SmellTypeGroup(String type) {
		super();
		smellType = type;
	}
	
	public String getType() {
		return (smellType);
	}
	
	public void initialize(Collection<ArchSmell> smells) {
		smellsByType = filterSmells(smellType, smells);
		return;
	}

	public double computeIndex(String version) {
		double index = 0;
		for (ArchSmell s: smellsByType) {
			index = index + s.computeIndex(version);
		}

		return (index);
	}
	
	@Override
	public String getName() {
		return (this.getType());
	}

	@Override
	public int getSize() {
		if (smellsByType == null)
			return (0);
		return (smellsByType.size());
	}

	@Override
	public double getMinForIndex() {
		double min = Double.MAX_VALUE;
		double smin = 0.0;
		for (ArchSmell s: smellsByType) {
			smin = s.getMinForIndex();
			if (smin < min)
				min = smin;
		}
		return (min);
	}

	@Override
	public double getMaxForIndex() {
		double max = Double.MIN_VALUE;
		double smax = 0.0;
		for (ArchSmell s: smellsByType) {
			smax = s.getMaxForIndex();
			if (smax > max)
				max = smax;
		}
		return (max);
	}
	
}

