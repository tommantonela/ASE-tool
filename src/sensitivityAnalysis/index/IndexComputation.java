package sensitivityAnalysis.index;

import java.util.Collection;

import smellhistory.smell.ArchSmell;

public interface IndexComputation {
	
	// ADI features
	public static final String SEVERITY_FEATURE				= "severityscore";
	public static final String PAGE_RANK_FEATURE 			= "pagerank";
	public static final String TOTAL_DEPENDENCIES_FEATURE 	= "numsetofelement";
	public static final String TOTAL_ELEMENTS_FEATURE		= "totnumofsetofelement";
	
	// Sonargraph features
	public static final String CDEPENDENCIES_TOREMOVE_FEATURE	= "componentdependenciestoremove";
	public static final String PDEPENDENCIES_TOREMOVE_FEATURE 	= "parserdependenciestoremove";
	public static final String STRUCTURAL_DEBT_FEATURE 			= "structuraldebtindex";
	
	public boolean checkFeatures(Collection<String> features);
	
	public double evaluate(ArchSmell s, String version);
	
	public double getMaxValue(ArchSmell s);
	
	public double getMinValue(ArchSmell s);

}
