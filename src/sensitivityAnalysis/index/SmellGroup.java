package sensitivityAnalysis.index;

import java.util.Collection;

import smellhistory.smell.ArchSmell;

public abstract class SmellGroup implements IndexElement {
	
	private double morrisMuStarScore = 0.0;
	private double sobolS1Score = 0.0;	
	
	protected SmellGroup() {
		morrisMuStarScore = 0.0;
		sobolS1Score = 0.0;
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
	
	public abstract void initialize(Collection<ArchSmell> smells);
		
}
