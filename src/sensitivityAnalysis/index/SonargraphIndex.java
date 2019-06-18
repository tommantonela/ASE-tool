package sensitivityAnalysis.index;


import java.util.ArrayList;
import java.util.Collection;

import smellhistory.smell.ArchSmell;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SonargraphIndex implements IndexComputation {
	
	static final Logger logger = LogManager.getLogger(SonargraphIndex.class);
	
	public static Collection<String> SDIFeatures;
	
	static {
		SDIFeatures = new ArrayList<String>();
		SDIFeatures.add(IndexComputation.CDEPENDENCIES_TOREMOVE_FEATURE);
		SDIFeatures.add(IndexComputation.PDEPENDENCIES_TOREMOVE_FEATURE);
		SDIFeatures.add(IndexComputation.STRUCTURAL_DEBT_FEATURE);
	}
	
	private static double evaluateSDI(double remove, double c) {
		double sdi = remove * c;
		return (sdi);
	}

	@Override
	public boolean checkFeatures(Collection<String> features) {

		return (features.containsAll(SDIFeatures));
	}
	
	@Override
	public double evaluate(ArchSmell s, String version) {
		
		double cdependenciesToRemove = s.getFeatureValue(IndexComputation.CDEPENDENCIES_TOREMOVE_FEATURE, version);
		double cycleLength = s.getSize();
		if (cycleLength == 1)
			cdependenciesToRemove = 0.0;
		
		//logger.debug("    smell: "+s.getName()+"   "+cdependenciesToRemove+"  "+cycleLength);

		return (SonargraphIndex.evaluateSDI(cdependenciesToRemove, cdependenciesToRemove));
	}

	@Override
	public double getMaxValue(ArchSmell s) {
		
		double cdependenciesToRemove = s.getMaxValueForFeature(IndexComputation.CDEPENDENCIES_TOREMOVE_FEATURE);
		double cycleLength = s.getSize();
		if (cycleLength == 1)
			cdependenciesToRemove = 0.0;
		
		//logger.debug("    smell: "+s.getName()+"   "+cdependenciesToRemove+"  "+cycleLength);

		return (SonargraphIndex.evaluateSDI(cdependenciesToRemove, cdependenciesToRemove));
	}

	@Override
	public double getMinValue(ArchSmell s) {
		
		double cdependenciesToRemove = s.getMinValueForFeature(IndexComputation.CDEPENDENCIES_TOREMOVE_FEATURE);
		double cycleLength = s.getSize();
		if (cycleLength == 1)
			cdependenciesToRemove = 0.0;
		
		//logger.debug("    smell: "+s.getName()+"   "+cdependenciesToRemove+"  "+cycleLength);

		return (SonargraphIndex.evaluateSDI(cdependenciesToRemove, cdependenciesToRemove));
	}

}
