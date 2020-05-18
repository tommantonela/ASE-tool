package main;

import sensitivityAnalysis.InterfaceSensitivityAnalysis;

public class Main {

	public static void main(String[] args) {
		
		args = new String[]{
			
			"-path",
//			"E:"+java.io.File.separator+"test_arcanSA"+java.io.File.separator+"apache-camel",
//			"E:/test_arcanSA/apache-camel",
//			"E:/test_arcanSA",
//			"E:/test_arcanSA/sensitivity-analysis-pipeline/test-input-sonargraph",
//			"E:/test_arcanSA/sensitivity-analysis-pipeline/test-input-arcan",
			
			"C:/Users/Anto/Desktop/esem/apache-camel-sa",
			
			"-index",
//			"arcan",
			"sonargraph",
			
			"-sobol",
			
			"-versions",
			"apache-camel-2.0.0", "apache-camel-2.17.0",
//			"hadoop-0.14.0-core_2019-06-18_21-33-29", "hadoop-0.15.0-core_2019-06-18_21-35-10",
			
//			"apache-cxf-2.2.0","apache-cxf-2.7.0",
			
//			"camel-core-2.2.0","camel-core-2.17.0",
			
//			"-features","pagerank",
			
			"-level", 
			"package-group",
//			"smell-instance"
//			"smell-type",
			
//			"-logger", "all"
			
			
		};
		
		InterfaceSensitivityAnalysis.main(args);
		
	}
	
}
