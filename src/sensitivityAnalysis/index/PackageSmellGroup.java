package sensitivityAnalysis.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import smellhistory.SmellFactory;
import smellhistory.smell.ArchSmell;
import smellhistory.smell.CDSmell;
import smellhistory.smell.HLSmell;
import smellhistory.smell.UDSmell;

public class PackageSmellGroup extends SmellGroup {
	
	static final Logger logger = LogManager.getLogger(PackageSmellGroup.class);
	
	public static Collection<SmellGroup> generateGroups(Collection<ArchSmell> smells) {
		
		String latestVersion = SmellFactory.getLastVersion();
		List<String> packagesLatestVersion = SmellFactory.getPackagesForVersion(latestVersion);
		logger.info("High-level packages for last version= "+latestVersion+"  "
				+packagesLatestVersion);
		
		HashMap<String,SmellGroup> groups = new HashMap<String,SmellGroup>();
		
		PackageSmellGroup g = null;
		for (int i=0; i < packagesLatestVersion.size(); i++) {
			g = new PackageSmellGroup(packagesLatestVersion.get(i));
			groups.put(packagesLatestVersion.get(i), g);
		}
				
		String p = null;
		ArrayList<ArchSmell> gsmells = null;
		int c = 0;
		for (int i=0; i < packagesLatestVersion.size(); i++) {
			
			p = packagesLatestVersion.get(i);
			g = (PackageSmellGroup)(groups.get(p));
			gsmells = new ArrayList<ArchSmell>();
//			boolean added = false;
			for (ArchSmell s: smells) {	

//				added = false;
				if (s instanceof HLSmell) {
					if (s.getDescription().contains(p)) {
//						System.out.println(p+" :: "+s.getDescription());
//						added = 
						gsmells.add(s);
					}
				}
				if (s instanceof UDSmell) {
					if (s.getDescription().contains(p)) {
//						added = 
						gsmells.add(s);
					}
				}
				if (s instanceof CDSmell) {
					if (s.getDescription().contains(p)) {
						System.out.println(p+" :: "+s.getDescription());
						gsmells.add(s);
					}
				}
			
			}
			
			c = c + gsmells.size();
			g.initialize(gsmells);
		}
		
		logger.info("Groups by packages: "+ groups.size() +  " ("+c+ ")");
		int count = 0;
		for (SmellGroup sg: groups.values()) {
			count = count + sg.getSize();
		}

		logger.info("Smells inside packages: "+ count);

		// Check for smells not asigned to any package
		int total = 0;
		String[] containers = null;
		for (ArchSmell s: smells) {
			containers = isInPackages(s, groups.values());
			//if ((container == null) && !(s instanceof CDSmell))
			if (containers == null) {
				//System.out.println("Smell not included --> "+s.getName()+" "+s.getDescription());
				logger.debug("Smell not included --> "+s.getName()+" "+s.getDescription());
				//System.out.println(s.getName()+" in packages= "+containers);
			}
			else {
				total = total + containers.length;
				//System.out.println("smell "+s.getName()+" in "+containers.length+" packages");
				for (int i = 0; i < containers.length; i++) {
					g = (PackageSmellGroup)(groups.get(containers[i]));
					if (g == null)
						logger.debug("NULL "+g+" "+containers[i]);
					//System.out.println((1.0/containers.length)+"  "+s+"  "+g);
					g.setRatioForSmell(s.getName(), (1.0/containers.length));
				}
			}
		}		
		logger.info("Total elements (with overlapping): "+total);
		
		SmellFactory.CurrentSmellPackages = groups;
		
		for(String k : groups.keySet())
			System.out.println(k+" :: "+groups.get(k).getName());
		
		return (groups.values());
	}
	
	private static String[] isInPackages(ArchSmell smell, Collection<SmellGroup> groups) {
		
		PackageSmellGroup sp = null;
		ArrayList<String> packagesForSmell = new ArrayList<String>();
		int count = 0;
		for (SmellGroup g: groups) {
			//if (smell.getDescription().contains(g.getName()))
			sp = (PackageSmellGroup)g;
			if (sp.containsSmell(smell)) {
				count++;
				packagesForSmell.add(sp.getName());
			}
		}
		String[] results = null;
		if (count > 0) {
			results = new String[count-1];
			results = packagesForSmell.toArray(results);
		}
		return (results);
	}
	
	private String myPackage = null;
	private Collection<ArchSmell> smellsByPackage = null;
	private HashMap<String, Double> ratiosForSmells = null;
	
	public PackageSmellGroup(String packageName) {
		super();
		myPackage = packageName;
	}

	@Override
	public double computeIndex(String version) {
		double index = 0;
		//double maxNe = Double.MIN_VALUE;
		
//		int validSmells = 0;
		Double ratio = 1.0;
		for (ArchSmell s: smellsByPackage) {
			if (s.existsForVersion(version)) {
				ratio = ratiosForSmells.get(s.getName());
				if (ratio == null)
					ratio = 1.0;
				index = index + s.computeIndex(version)*ratio;
//				validSmells++;
				
				//maxNe = s.getNumberOfElements(version);
			}
		}
		
		//return (ADI  /maxNe);
		return (index);
	}

	@Override
	public String getName() {
		return (myPackage);
	}

	@Override
	public void initialize(Collection<ArchSmell> smells) {
		smellsByPackage = new ArrayList<ArchSmell>();
		smellsByPackage.addAll(smells);
		ratiosForSmells = new HashMap<String,Double>();
		return;
	}
	
	public void setRatioForSmell(String id, double ratio) {
		ratiosForSmells.put(id, ratio);
		return;
	}
	
	@Override
	public int getSize() {
		if (smellsByPackage == null)
			return (0);
		return (smellsByPackage.size());
	}
	
	public boolean containsSmell(ArchSmell smell) {
		for (ArchSmell s: smellsByPackage) {
			if (s.getName().equals(smell.getName()))
				return (true);
		}
		
		return (false);
	}
	
	@Override
	public double getMinForIndex() {
		double min = Double.MAX_VALUE;
		double smin = 0.0;
		for (ArchSmell s: smellsByPackage) {
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
		for (ArchSmell s: smellsByPackage) {
			smax = s.getMaxForIndex();
			if (smax > max)
				max = smax;
		}
		return (max);
	}

}

