package smellhistory;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sensitivityAnalysis.index.ArcanIndex;
import sensitivityAnalysis.index.IndexComputation;
import sensitivityAnalysis.index.SmellGroup;
import sensitivityAnalysis.index.SmellTypeGroup;
import sensitivityAnalysis.index.SonargraphIndex;
import smellhistory.smell.ArchSmell;
import smellhistory.smell.CDSmell;
import smellhistory.smell.HLSmell;
import smellhistory.smell.UDSmell;

public class SmellFactory {

	static final Logger logger = LogManager.getLogger(SmellFactory.class);

	public static final String INVALID_VALUE = "-Infinity";

	private static HashMap<String, ArchSmell> CurrentSmells = null;
	public static HashMap<String, SmellGroup> CurrentSmellPackages = null;

	private static List<String> AllVersions = null;

	private static LinkedHashMap<String, List<String>> CurrentPackages = null;

	private static IndexComputation CurrentIndex = null;

	public static final String HL_SMELL = "hl";
	public static final String CD_SMELL = "cd";
	public static final String UD_SMELL = "ud";

	public static void initialize(List<String> versions) {
		CurrentSmells = new HashMap<String,ArchSmell>();
		AllVersions = versions;
		CurrentPackages = new LinkedHashMap<>();

		return;
	}

	public static void clear() {
		CurrentSmells = new HashMap<String,ArchSmell>();
		AllVersions.isEmpty();
		CurrentPackages = new LinkedHashMap<>();

		return;
	}

	public static int getMaxVersions() {
		return (AllVersions.size());
	}

	public static String getLastVersion() {
		return (AllVersions.get(AllVersions.size()-1));
	}

	public static String getVersion(int i) {
		return (AllVersions.get(i));
	}

	public static int getVersionId(String v) {
		for (int i=0; i < AllVersions.size(); i++) {
			if (AllVersions.get(i).equals(v))
				return (i);
		}
		return (-1);
	}

	public static boolean setPackagesForVersion(String version, List<String> packages) {
		return (CurrentPackages.put(version, packages) != null);
	}

	public static List<String> getPackagesForVersion(String version) {
		return (CurrentPackages.get(version));
	}

//	public static boolean reduceToTopLevelPackages(String defaultPrefix) {
//
//		if (CurrentPackages == null) 
//			return (false);
//
//		logger.debug("  Updating package lists to top-level packages (for all versions)  ");
//
//		List<String> reducedList = null;
//		List<String> packageForVersion = null;
//		for (String k: CurrentPackages.keySet()) {
//			packageForVersion = CurrentPackages.get(k);
////			Collections.sort(packageForVersion);
////			System.out.println("ANTES: "+packageForVersion.size()+" "+packageForVersion);
//			reducedList = reduceListOfPackages(packageForVersion, defaultPrefix);
////			Collections.sort(reducedList);
////			System.out.println("DESPUES: "+reducedList.size()+" "+reducedList);
////			packageForVersion.removeAll(reducedList);
////			System.out.println("DIFF: "+packageForVersion.size()+" "+packageForVersion);
//			CurrentPackages.put(k, reducedList); // Updates the structure to top-level packages only
//		}
//
//		return (true);
//	}
//
//	private static List<String> reduceListOfPackages(List<String> packages, String defaultPrefix) {
//
//		Set<String> list = new TreeSet<String>(packages);	
//		Set<String> tobeAdded = new TreeSet<String>();		
//
//		boolean remove = false;
//		String toplevel = null;
//		for (int i = 0; i < packages.size(); i++) {
//			remove = false;
//			toplevel = packages.get(i);
//			for (String pp: list) {
//				if (toplevel.startsWith(pp) && (toplevel.length() > pp.length()) 
//						&& !pp.equals(defaultPrefix)) // pp is prefix of toplevel
//					remove = true;
//			}
//			if (toplevel.contains("impl")) //TODO
//				remove = true;
//
//			if (!remove)
//				tobeAdded.add(toplevel);
//		}
//
//		return new ArrayList<String>(tobeAdded);
//	}

	public static String getPreviousVersion(String version) {
		for (int i = 0; i < AllVersions.size(); i++) {
			if (AllVersions.get(i).equals(version)) {
				if (i > 0)
					return (AllVersions.get(i-1));
			}
		}
		return (null);
	}

	public static String getNextVersion(String version) {
		for (int i = 0; i < AllVersions.size(); i++) {
			if (AllVersions.get(i).equals(version)) {
				if (i < AllVersions.size()-1)
					return (AllVersions.get(i+1));
			}
		}
		return (null);
	}	

	private static int SMELL_ID = 0;

	public static int retainSmellsBetweenVersions(String[] versions) {

		HashMap<String, ArchSmell> smellsForVersions = new HashMap<String, ArchSmell>();

		boolean exists = false;
		for (ArchSmell s: SmellFactory.getSmells()) {
			exists = false;
			//System.out.println("Smell "+s.getName());
			for (int i = 0; i < versions.length; i++) {
				if (s.existsForVersion(versions[i])) {
					//System.out.println("      exists in version= "+versions[i]+"  "+s.existsForVersion(versions[i]));
					exists = true;
				}
			}
			if (exists)
				smellsForVersions.put(s.getDescription(), s);
		}

		int n = smellsForVersions.size();
		CurrentSmells = smellsForVersions;

		return (n);
	}

	public static int retainSmellsAtVersion(String version) {

		HashMap<String, ArchSmell> smellsForVersion = new HashMap<String, ArchSmell>();

		for (ArchSmell s: SmellFactory.getSmells()) {
			if (s.existsForVersion(version)) {
				smellsForVersion.put(s.getDescription(), s);
			}
		}

		int n = smellsForVersion.size();
		CurrentSmells = smellsForVersion;

		return (n);
	}

	public static boolean isCyclicPackage(String p, String version) {
		int count = 0;
		for (ArchSmell s: SmellFactory.getSmells()) {
			if (s instanceof CDSmell) {
				if (s.existsForVersion(version) && s.getDescription().contains(p))
					count++;
			}
		}

		return (count > 0);
	}

	public static ArchSmell createSmell(String smellName, int nVersions) { 

		// Assuming that each description is unique for a given smell instance 
		// (different smell types with the same description/name are not considered)

		
		
		ArchSmell smell = CurrentSmells.get(smellName);

		if (smell == null) {
			
			String [] sp = smellName.split("_"); 
			String smellType = sp[0];
			String description = sp[1];
			
			if (smellType.equals(CD_SMELL)) {
				SMELL_ID++;
				CDSmell cd = new CDSmell(CD_SMELL+SMELL_ID, nVersions);
				cd.setDescription(description);
				cd.configureIndex(CurrentIndex);
				CurrentSmells.put(smellType+description, cd);
				return (cd);

			}
			if (smellType.equals(HL_SMELL)) {
				SMELL_ID++;
				HLSmell hl = new HLSmell(HL_SMELL+SMELL_ID, nVersions);
				hl.setDescription(description);
				hl.configureIndex(CurrentIndex);
				CurrentSmells.put(smellType+description, hl);
				return (hl);

			}		

			if (smellType.equals(UD_SMELL)) {
				SMELL_ID++;
				UDSmell ud = new UDSmell(UD_SMELL+SMELL_ID, nVersions);
				ud.setDescription(description);
				ud.configureIndex(CurrentIndex);
				CurrentSmells.put(smellType+description, ud);
				return (ud);

			}		
		}



		return (smell);
	}

	public static boolean addFeature(ArchSmell smellObj, String feature, CSVRecord record, List<String> versions) {

		double[] values = new double[versions.size()];
		String versionValue;
		for (int v = 0; v < versions.size(); v++) {
			versionValue = record.get(v + 2);
			if (versionValue.equals(SmellFactory.INVALID_VALUE)) {
				//logger.error("Feature "+feature+" with invalid value: "+versionValue);
				values[v] = Double.NEGATIVE_INFINITY;
			}
			else
				values[v] = Double.valueOf(versionValue);
		}

		return (SmellFactory.addFeature(smellObj, feature, values, versions));
	}

	public static boolean addFeature(ArchSmell smellObj, String feature, double values[], List<String> versions) {

		double value = 0.0;
		String versionName = null;

		for (int v = 0; v < versions.size(); v++) {
			versionName = versions.get(v);
			value = values[v];

			//System.out.println(value);
			if (value != Double.NEGATIVE_INFINITY) { // -Infinite is considered an erroneous feature value
				smellObj.defineFeature(feature);
				smellObj.addFeatureValueForVersion(feature, value, versionName);
			}
			//			else
			//				logger.error("Feature "+feature+" with invalid value: "+value);
		}

		return (true);
	}

	public static int numberOfSmells() {
		return (CurrentSmells.size());
	}

	public static Collection<ArchSmell> getSmells() {
		return (CurrentSmells.values());
	}

	public static ArchSmell removeSmell(String id) {
		for (ArchSmell s: CurrentSmells.values()) {
			if (s.getName().equals(id)) {
				return (CurrentSmells.remove(s.getDescription()));
			}
		}

		return (null);
	}

	public static ArchSmell findSmell(String id) {
		for (ArchSmell s: CurrentSmells.values()) {
			if (s.getName().equals(id))
				return (s);
		}

		return (null);
	}

	public static SmellGroup findSmellGroup(String name) {

		if(CurrentSmellPackages != null)
			for (SmellGroup s: CurrentSmellPackages.values()) {
				if (s.getName().equals(name))
					return (s);
			}

		return (null);
	}

	public static Collection<ArchSmell> filterSmells(String smellType) {

		Collection<ArchSmell> array = SmellTypeGroup.filterSmells(smellType, CurrentSmells.values());
		return (array);

	}

	public static int numberOfSmells(String smellType) {
		int counter = filterSmells(smellType).size();
		return (counter);

	}

	public static double computeIndex(String version) {
		double globalAdi = 0.0;

		double smellAdi = 0.0;
		for (ArchSmell s: CurrentSmells.values()) {
			smellAdi = s.computeIndex(version);
			globalAdi = globalAdi + smellAdi;
		}

		return (globalAdi);
	}

	public static IndexComputation setIndex(String option) {

		if (option.equals("arcan")) {
			if (CurrentIndex == null) {
				CurrentIndex = new ArcanIndex();
			}
			else if (CurrentIndex instanceof SonargraphIndex)
				CurrentIndex = new ArcanIndex();
		}
		else
			if (option.equals("sonargraph")) {
				if (CurrentIndex == null) {
					CurrentIndex = new SonargraphIndex();
				}
				else if (CurrentIndex instanceof ArcanIndex)
					CurrentIndex = new SonargraphIndex();
			}

		return (CurrentIndex);
	}

	public static TimedGraph buildTimedGraph() {

		TimedGraph g = new TimedGraph();
		g.checkSmellMatrix();
		int r = g.removeBlanks();
		logger.debug("  removed = "+r+" ");


		//g.checkSmellMatrix();

		int p = g.patchSmells(); // Experimental
		logger.debug("  patched = "+p+" ");

		//g.checkSmellMatrix();
		//g.prinMatrix();

		//TimedGraph.prinADIMatrixAllSmells();

		return (g);
	}

	public static void convertToArchitecturalSmells(Map<String,Map<String,double[]>> smellEvolution, List<String> sortedVersions) {

		logger.info(" Preparing smells for Sensitivity Analysis ...");

		logger.info("  Versions to analyse: "+ sortedVersions + " ");

		SmellFactory.initialize(sortedVersions);

		int nVersions = sortedVersions.size();

		String description;
		String type; 
		Map<String, double[]> smellFeatures;
		double[] featureValues;
		boolean ok;
		for (String s: smellEvolution.keySet()) {

			ArchSmell smell = SmellFactory.createSmell(s, nVersions); // Ojo, esto le asigna un nuevo ID al smell

			smellFeatures = smellEvolution.get(s);
			for (String f: smellFeatures.keySet()) {
				featureValues = smellFeatures.get(f);
				ok = SmellFactory.addFeature(smell, f, featureValues, sortedVersions);
				if (!ok)
					logger.error(" Problems when adding feature.  "+f);
			}

		}

		int total = SmellFactory.numberOfSmells();		

		logger.info("Smells read: "+ total
				+ "\n    CD: "+ SmellFactory.numberOfSmells(SmellFactory.CD_SMELL)
				+ "\n    UD: "+ SmellFactory.numberOfSmells(SmellFactory.UD_SMELL)
				+ "\n    HL: "+ SmellFactory.numberOfSmells(SmellFactory.HL_SMELL));

		//		if (smellsLastVersionOnly) {
		//			String lastVersion = myVersions[myVersions.length-1];
		//			int n = SmellFactory.retainSmellsAtVersion(lastVersion);
		//			logger.info("Keeping only smells for version= "+lastVersion+"  retained smells: "+ n);
		//			total = SmellFactory.numberOfSmells();
		//			System.out.println("Smells read: "+total);
		//			System.out.println("    CD: "+ SmellFactory.numberOfSmells(SmellFactory.CD_SMELL));
		//			System.out.println("    UD: "+ SmellFactory.numberOfSmells(SmellFactory.UD_SMELL));
		//			System.out.println("    HL: "+ SmellFactory.numberOfSmells(SmellFactory.HL_SMELL));	
		//		}

		SmellFactory.buildTimedGraph();

		return; 
	}

}

