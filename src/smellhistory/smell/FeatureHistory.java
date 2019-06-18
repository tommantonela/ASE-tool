package smellhistory.smell;

import java.util.HashMap;

public class FeatureHistory {
	
	private HashMap<String,Double> versionsToValues = null;
	private String feature = null;
	
	public FeatureHistory(String name) {
		feature = name;
		versionsToValues = new HashMap<String,Double>();
	}
	
	public int size() {
		return (versionsToValues.size());
	}
	
	public boolean hasVersion(String version) {
		return (versionsToValues.containsKey(version) && (versionsToValues.get(version) != null));
	}

	public double getValueAt(String version) {
		if (versionsToValues.get(version) == null)
			return (Double.NEGATIVE_INFINITY);
		
		return (versionsToValues.get(version));
	}
	
	public Double addValueAt(String version, double param) {
		return (versionsToValues.put(version, param));
	}
	
	public double getMinValue() {
		double min = Double.MAX_VALUE;
		for (double d: versionsToValues.values()) {
			if (d <= min)
				min = d;
		}
		return (min);
	}

	public double getMaxValue() {
		double max = 0.0;
		for (double d: versionsToValues.values()) {
			if (d >= max)
				max = d;
		}	
		return (max);
	}
	
	public String getName() {
		return (feature);
	}
}
