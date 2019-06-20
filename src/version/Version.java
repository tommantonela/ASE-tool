package version;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parser.Parser;
import parser.factory.FactoryParser;
import sensitivityAnalysis.InterfaceSensitivityAnalysis;

public abstract class Version {

	String versionName;
	
	Map<String,Integer> features; //to search for the index?
	List<String> sortedFeatures;
	
	Map<String,double[]> scores;
	
	static Set<String> smellTypes;
	Map<String,Set<String>> smells;
	
	static String highLevelRoot = null;

	Parser parser; //to be used for the packages selection
	
	public Version(String path, String versionName){
		
		this.versionName = versionName;
		
		features = new HashMap<>();
		scores = new HashMap<>();
		smellTypes = new HashSet<>();
		sortedFeatures = new ArrayList<>();
		
		processVersion(path);
		
		smells = new HashMap<>();
		for(String st : smellTypes)
			smells.put(st, new HashSet<>());
		
		for(String s : scores.keySet())
			if(s.startsWith("ud_"))
				smells.get("ud").add(s);
			else
				if(s.startsWith("hl_"))
					smells.get("hl").add(s);
				else
					smells.get("cd").add(s);
		
	}
	
	
	public int indexOfFeature(String feature){
		
		return features.get(feature);
	}
	
	protected abstract void processVersion(String path);
	
	public List<String> getFeatures(){
		return sortedFeatures;
	}
	
	public Set<String> getSmellTypes(){
		return smellTypes;
	}
	
	public String getVersion(){
		return versionName;
	}
	
	public Set<String> getSmells(String type){
		Set<String> aux = smells.get(type);
		if(aux != null)
			return aux;
		return new HashSet<>();
	}
	
	public Set<String> getSmells(){
		return scores.keySet();
	}
	
	public double[] getScoresForSmell(String smell){
		double [] aux = scores.get(smell);
		if(aux != null)
			return aux;
		
		return new double[0];
	}

	public abstract String findSmell(String smell);

	public abstract Set<String> findPotentiallyMatchingSmells(String smell);
	
	//allows to find all matches between smells -- potential smaller can be a String []
		protected boolean isAMatch(List<String> potential_smaller,List<String> bigger) {

			int first_index = bigger.indexOf(potential_smaller.get(0));

			if (first_index < 0) //ya el primero no existe
				return false;

			int number_decreased = 0;
			int index = first_index;

			for (int i=1; i< potential_smaller.size();i++){ //por cada uno de los paquetes en el pequeÃ±o

				int current_index = bigger.indexOf(potential_smaller.get(i));

				if(current_index < 0) //si no lo encontró, no va a existir
					return false;

				if(number_decreased == 0){ //si todavía no bajó, puede bajar uno
					if(current_index < index)
						number_decreased++;
				}
				else{//si ya bajó, no puede volver a bajar

					if(current_index < index)
						return false;

					if(current_index > first_index)
						return false;
				}

				index = current_index;
			}
			return true;
		}


		public List<String> getTopLevelPackages() {
			

			if(parser == null){ //if we can find the required file, we do that of finding the packages with classes. If not, we cut the first package we found
				String fileName = InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+versionName+".jar";
				if(new File(fileName).exists()){
					parser = FactoryParser.getParser(fileName,"package");
					List<String> aux = new ArrayList<String> (parser.getTopLevelPackages());
					highLevelRoot = parser.getHighLevelRootPackage();
					return aux;
				}				
			}
						
			return new ArrayList<>(getTopLevelPackages(smells.keySet()));
		}


		public static Set<String> getTopLevelPackages(Set<String> smells) {
			
			Set<String> allPackage = new HashSet<>();
			
			for(String s : smells){
				String [] packs = s.substring(s.indexOf("_")+1).split(";");
				for(String p : packs)
					allPackage.add(p);
			}
			
			highLevelRoot = org.apache.commons.lang3.StringUtils.getCommonPrefix(allPackage.toArray(new String[]{}));
			if(highLevelRoot.endsWith("."))
				highLevelRoot = highLevelRoot.substring(0, highLevelRoot.length()-1);

			if(highLevelRoot.length() != 0)
				return getNaivePackages(highLevelRoot,allPackage);
			
			//we have no common root - we need to find the potential commons!
			Map<String,Set<String>> potentialRoots = new HashMap<>();
			for(String a : allPackage){
				int index = a.indexOf(".");
				String a_edited = a;
				if(index > 0)
					a_edited = a.substring(0,index);

				Set<String> aux = potentialRoots.get(a_edited);
				if(aux == null){
					aux = new HashSet<>();
					potentialRoots.put(a_edited, aux);
				}

				aux.add(a);
			}

			Set<String> packages = new HashSet<>();
			for(String pr : potentialRoots.keySet()){
				
				highLevelRoot = org.apache.commons.lang3.StringUtils.getCommonPrefix(potentialRoots.get(pr).toArray(new String[]{}));
				
				packages.addAll(getNaivePackages(highLevelRoot,potentialRoots.get(pr)));	
			}
			return packages;
		}

		private static Set<String> getNaivePackages(String topLevel,Set<String> packagesToSearch) {
			Set<String> packages = new HashSet<>();

			for(String a : packagesToSearch){ //for every package we got here

				a = a.replace(topLevel, "");

				if(a.length() == 0)
					packages.add(topLevel);
				else{
					if(a.startsWith("."))
						a = a.substring(1);
					
					int index = a.indexOf(".");
					if(index > 0)
						a = a.substring(0,index);
					
					packages.add(topLevel+"."+a);
				}
			}
			return packages;
		}
		
		public static String getHighLevelRoot() {
			return highLevelRoot;
		}
	
}
