package main;

import sensitivityAnalysis.InterfaceSensitivityAnalysis;

public class Main {

	public static void main(String[] args) {
		
		args = new String[]{
			
//			"-path",
////			"E:"+java.io.File.separator+"test_arcanSA"+java.io.File.separator+"apache-camel",
//			"E:/test_arcanSA/apache-camel",
////			"E:/test_arcanSA",
//			
//			"-index",
//			"arcan",
////			"sonargraph",
//			
//			"-sobol",
//			
////			"-versions","apache-camel-1.6.0", "apache-camel-2.0.0",
//			
////			"-features","pagerank",
//			
//			"-level", "package-group",
//			
//			"-logger", "info"
//			
			
		};
		
		InterfaceSensitivityAnalysis.main(args);
		
	}
	
}
