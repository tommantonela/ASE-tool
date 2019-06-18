package smellhistory.smell;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sensitivityAnalysis.index.IndexComputation;
import sensitivityAnalysis.index.IndexElement;
import smellhistory.SmellFactory;

public abstract class ArchSmell implements IndexElement, Comparable<Object> {
	
	static final Logger logger = LogManager.getLogger(ArchSmell.class);
	
	protected Map<String, FeatureHistory> features = null;

	private String description; // the packages affected by the smell
	private String name; // an Id like "ud21"
	
	private double morrisMuStarScore = 0.0;
	private double sobolS1Score = 0.0;	
	private boolean patched = false;
	private int numberOfVersions = -1;
	
	protected IndexComputation myIndex = null;
	
	protected ArchSmell(String description, int nVersions) {
		name = description;
		features = new HashMap<String, FeatureHistory>();
		morrisMuStarScore = 0.0;
		sobolS1Score = 0.0;
		patched = false;
		numberOfVersions = nVersions;
	}
	
	public boolean defineFeature(String fname) {
		if (!features.containsKey(fname)) {
			FeatureHistory fh = new FeatureHistory(fname);
			features.put(fname, fh);
			return (true);
		}
		
		return (false);
	}
	
	public void configureIndex(IndexComputation ic) {
		myIndex = ic;
	}
	
	public int getSize() {
		return (1);
	}
	
	public double getSensitivityScore(String method) {
		if (method.equals(MORRIS_METHOD))
			return (morrisMuStarScore);
		if (method.equals(SOBOL_METHOD))
			return (sobolS1Score);
		
		return (-1);
	}
	
	public boolean setSensitivityScore(double score, String method) {
		if (method.equals(MORRIS_METHOD)) {
			morrisMuStarScore = score;
			return (true);
		}
		if (method.equals(SOBOL_METHOD)) {
			sobolS1Score = score;
			return (true);
		}
		
		return (false);
	}
	
	public boolean patched() {
		return (patched);
	}
	
//	public void copyParameters(ArchSmell copy, String version) {
//		
//		double pr  = copy.getPageRank(version);
//		double sc = copy.getSeverityScore(version);
//		double nd = copy.getNumberOfDependencies(version);
//		double ne = copy.getNumberOfElements(version);
//		
//		this.addPageRankForVersion(version, pr);
//		this.addSeverityScoreForVersion(version, sc);
//		this.addTotalDependenciesForVersion(version, nd);
//		this.addTotalElementsForVersion(version, ne);
//		
//		patched = true;
//		
//		return;
//	}
	
	public void setDescription(String d) {
		description = d;
	}
	
	public String getDescription() {
		return (description);
	}
	
	public double getMinValueForFeature(String fname) {
		if (features.containsKey(fname)) {
			return (features.get(fname).getMinValue());
		}

		return (-1);
	}
	
	public double getMaxValueForFeature(String fname) {
		if (features.containsKey(fname)) {
			return (features.get(fname).getMaxValue());
		}

		return (Double.MAX_VALUE);
	}

	public boolean addFeatureValueForVersion(String fname, double value, String version) {
		if (features.containsKey(fname)) {
			if (value == Double.NEGATIVE_INFINITY) {
				//logger.error("Feature "+fname+" with value: "+value);
				return (false);
			}
			else {
				features.get(fname).addValueAt(version, value); 
				return (true);
			}
		}
		
		return (false);
	}
	
	public int numberOfVersions() {
		return (numberOfVersions);
	}

	public double getFeatureValue(String fname, String version) {
		if (features.containsKey(fname)) {
			return (features.get(fname).getValueAt(version));
		}
		
		return (-1);
	}
	
	public String getName() {
		return (name);	
	}
	
	public boolean existsForVersion(String version) {
		for (FeatureHistory fh: features.values()) {
			if (fh.hasVersion(version))
				return (true);
		} 
		// It´s assumed that the version does not exist for any of the parameters
		
		return (false);
	}
	
	public final double computeIndex(String version) {
		// TODO: Invoke the IndexComputation strategy with the required features
		if (!this.existsForVersion(version))
			return (0.0);
		
		Collection<String> myFeatures = features.keySet();
		double index = Double.MAX_VALUE;
		if (myIndex.checkFeatures(myFeatures)) {
			index = myIndex.evaluate(this, version);
		}
		else {
			logger.error("Smell with wrong features: "+this.getName());
		}

		return (index);
	}
	
	@Override
	public double getMinForIndex() {
		return (myIndex.getMinValue(this));
	}

	@Override
	public double getMaxForIndex() {
		return (myIndex.getMaxValue(this));
	}
	
//	public boolean involvesTopLevelPackages(String version) {
//
//		if (!this.existsForVersion(version))
//			return (false);
//		
//		// For now, it should only work for cycles
//		String[] smellPackages = this.getDescription().split(";");
//		String[] packages = SmellFactory.getPackagesForVersion(version);
//		
//		boolean smellInList = false;
//		for (int i = 0; i < smellPackages.length; i++) {
//			smellInList = false;
//			for (int j = 0; j < packages.length; j++) {
//				if (smellPackages[i].compareTo(packages[j]) == 0) // The smell is in the list
//					smellInList = true;
//			}
//			if (!smellInList)
//				return (false);
//		}
//		
//		return (true);
//	}
//	
//	public boolean isPredictable(String version) {
//		
//		String previousVersion  = SmellFactory.getPreviousVersion(version);
//		
//		if (previousVersion != null)
//			if (!this.existsForVersion(previousVersion))
//					return (this.involvesTopLevelPackages(version));
//		
//			return (false);
//	}
//	
//	public boolean existedBefore(String version) {
//		
//		String previousVersion  = SmellFactory.getPreviousVersion(version);
//		
//		if (previousVersion != null)
//			return (this.existsForVersion(previousVersion));
//		else
//			return (false);
//	}
//	
//	public boolean existsAfter(String version) {
//		
//		String nextVersion  = SmellFactory.getNextVersion(version);
//		//System.out.println("test-next-version: "+version+"  "+nextVersion);
//		
//		if (nextVersion != null)
//			return (this.existsForVersion(nextVersion));
//		else
//			return (false);
//	}
	
	@Override
	public int compareTo(Object obj) {
		if (obj instanceof ArchSmell) {
			ArchSmell as = (ArchSmell)obj;
			if (this.getName().equals(as.getName()))
				return (0);
		}
		return 1;
	}
	
	public void copyParameters(ArchSmell copy, String version) {
		
		//logger.debug("Copying parameters for "+this.getName());
		
		double fv = -1;
		for (String f: features.keySet()) {
			fv = copy.getFeatureValue(f, version);
			this.addFeatureValueForVersion(f, fv, version);
		}
		
		patched = true;
		
		return;
	}
	
	public boolean existedBefore(String version) {
		
		String previousVersion  = SmellFactory.getPreviousVersion(version);
		
		if (previousVersion != null)
			return (this.existsForVersion(previousVersion));
		else
			return (false);
	}
	
	public boolean existsAfter(String version) {
		
		String nextVersion  = SmellFactory.getNextVersion(version);
		
		if (nextVersion != null)
			return (this.existsForVersion(nextVersion));
		else
			return (false);
	}
	
}
