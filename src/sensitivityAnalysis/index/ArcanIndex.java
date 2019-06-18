package sensitivityAnalysis.index;

import java.util.ArrayList;
import java.util.Collection;

import smellhistory.smell.ArchSmell;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ArcanIndex implements IndexComputation {
	
	static final Logger logger = LogManager.getLogger(ArcanIndex.class);
	
	public static Collection<String> ADIFeatures;
	
	static {
		ADIFeatures = new ArrayList<String>();
		ADIFeatures.add(IndexComputation.SEVERITY_FEATURE);
		ADIFeatures.add(IndexComputation.PAGE_RANK_FEATURE);
		ADIFeatures.add(IndexComputation.TOTAL_DEPENDENCIES_FEATURE);
		ADIFeatures.add(IndexComputation.TOTAL_ELEMENTS_FEATURE);
	}
	
	private static double evaluateADI(double s, double pr, double nd, double ne) {
		double adi = s * pr * nd / ne;
		return (adi);
	}

	@Override
	public boolean checkFeatures(Collection<String> features) {

		return (features.containsAll(ADIFeatures));
	}
	
	@Override
	public double evaluate(ArchSmell s, String version) {
		
		double severity = s.getFeatureValue(IndexComputation.SEVERITY_FEATURE, version);
		double pageRank = s.getFeatureValue(IndexComputation.PAGE_RANK_FEATURE, version);
		double nDependencies = s.getFeatureValue(IndexComputation.TOTAL_DEPENDENCIES_FEATURE, version);
		double nElements = s.getFeatureValue(IndexComputation.TOTAL_ELEMENTS_FEATURE, version);
		
		logger.debug("    evaluate smell: "+s.getName()+"   "+severity+"  "+pageRank+"  "+nDependencies+" "+nElements);

		return (ArcanIndex.evaluateADI(severity, pageRank, nDependencies, nElements));
	}

	@Override
	public double getMaxValue(ArchSmell s) {
		
		double severity = s.getMaxValueForFeature(IndexComputation.SEVERITY_FEATURE);
		double pageRank = s.getMaxValueForFeature(IndexComputation.PAGE_RANK_FEATURE);
		double nDependencies = s.getMaxValueForFeature(IndexComputation.TOTAL_DEPENDENCIES_FEATURE);
		double nElements = s.getMaxValueForFeature(IndexComputation.TOTAL_ELEMENTS_FEATURE);
		
		logger.debug("    getMaxValue smell: "+s.getName()+"   "+severity+"  "+pageRank+"  "+nDependencies+" "+nElements);
		
		return (ArcanIndex.evaluateADI(severity, pageRank, nDependencies, nElements));
	}

	@Override
	public double getMinValue(ArchSmell s) {
		
		double severity = s.getMinValueForFeature(IndexComputation.SEVERITY_FEATURE);
		double pageRank = s.getMinValueForFeature(IndexComputation.PAGE_RANK_FEATURE);
		double nDependencies = s.getMinValueForFeature(IndexComputation.TOTAL_DEPENDENCIES_FEATURE);
		double nElements = s.getMinValueForFeature(IndexComputation.TOTAL_ELEMENTS_FEATURE);
		
		logger.debug("    getMinValue smell: "+s.getName()+"   "+severity+"  "+pageRank+"  "+nDependencies+" "+nElements);
		
		return (ArcanIndex.evaluateADI(severity, pageRank, nDependencies, nElements));
	}

}
