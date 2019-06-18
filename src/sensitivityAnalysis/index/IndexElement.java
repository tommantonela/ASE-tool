package sensitivityAnalysis.index;

public interface IndexElement {
	
	public static final String MORRIS_METHOD = "morris";
	public static final String SOBOL_METHOD = "sobol";
	
	public double computeIndex(String version);
	
	public double getMinForIndex();
	
	public double getMaxForIndex();
	
	public String getName();
	
	public int getSize();
	
	public double getSensitivityScore(String method);
		
}
