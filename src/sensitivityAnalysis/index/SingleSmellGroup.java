package sensitivityAnalysis.index;

import java.util.Collection;
import java.util.HashMap;

import smellhistory.SmellFactory;
import smellhistory.smell.ArchSmell;

//import parser.SmellFactory;

public class SingleSmellGroup extends SmellGroup {
	
	public static Collection<SmellGroup> generateGroups(Collection<ArchSmell> smells) {
		
		HashMap<String,SmellGroup> groups = new HashMap<String,SmellGroup>();
		
		SingleSmellGroup ssGroup = null;
		for (ArchSmell s: SmellFactory.getSmells()) {
			ssGroup = new SingleSmellGroup(s);
			groups.put(s.getName(), ssGroup);
		}
		
		return (groups.values());
	}
	
	private ArchSmell mySmell = null;
	
	public SingleSmellGroup(ArchSmell smell) {
		super();
		mySmell = smell;
	}

	@Override
	public double computeIndex(String version) {
		return (mySmell.computeIndex(version));
	}

	@Override
	public String getName() {
		return (mySmell.getName());
	}

	@Override
	public void initialize(Collection<ArchSmell> smells) {
		return;
	}

	@Override
	public int getSize() {
		if (mySmell == null)
			return (0);
		return (1);
	}

	@Override
	public double getMinForIndex() {
		return (mySmell.getMinForIndex());
	}

	@Override
	public double getMaxForIndex() {
		return (mySmell.getMaxForIndex());
	}

}

