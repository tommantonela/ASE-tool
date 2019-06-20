package parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Parser {

	Set<String> clases; 
	Map<String,Map<String,Float>> efferents;
	Map<String,Map<String,Float>> afferents;
	Map<String,String> classNamespace;
	Map<String,Set<String>> namespaces;
	Map<String,String> containerAttributes;

	Map<String,Integer> hierarchy_levels;

	int totalDependencies = -1;

	String highLevelRoot;

	public Parser(String path,DependencyExtractor extractor) {

		clases = new HashSet<>();
		efferents = new HashMap<>();
		afferents = new HashMap<>();
		classNamespace = new HashMap<>();
		namespaces = new HashMap<>();
		containerAttributes = new HashMap<>();

		extractor.set(clases,efferents,afferents,classNamespace,namespaces,containerAttributes);

		extractor.parseArchive(path);

		hierarchy_levels = new HashMap<String,Integer>();

	}

	public Set<String> getNameSpaces(){
		return namespaces.keySet();
	}

	public String getNameSpace(String clas){
		return classNamespace.get(clas);
	}

	public Map<String,Set<String>> getPackageDistribution(){
		return namespaces;
	}

	public Map<String,Float> getafferents(String clas){
		Map<String,Float> r = afferents.get(clas);
		if(r==null)
			r = new HashMap<String,Float>();
		return r;
	}

	public Map<String,Float> getefferents(String clas){
		Map<String,Float> r = efferents.get(clas);
		if(r==null)
			r = new HashMap<String,Float>();
		return r;
	}

	public String toString(){
		return "class";
	}

	//true si class1 depende de class2
	public Boolean dependsOn(String class1,String class2){
		Map<String,Float> outClass1 = efferents.get(class1);
		if(outClass1 == null)
			return false;
		return outClass1.containsKey(class2);
	}

	public float getMeanIngoing(){

		List<Integer> values = new ArrayList<Integer>();
		for(String c : afferents.keySet())
			values.add(afferents.get(c).size());
		Collections.sort(values);

		if(values.size() % 2 == 0){
			int mid = values.size()/2;
			return (values.get(mid)+values.get(mid-1))/2.0f;
		}

		return values.get(values.size()/2);

	}

	public float getMeanOutgoing(){

		List<Integer> values = new ArrayList<Integer>();
		for(String c : efferents.keySet())
			values.add(efferents.get(c).size());
		Collections.sort(values);

		if(values.size() % 2 == 0){
			int mid = values.size()/2;
			return (values.get(mid)+values.get(mid-1))/2.0f;
		}

		return values.get(values.size()/2);
	}

	public float getTotalDependencies() {	

		if(totalDependencies < 0){

			Set<String> deps = new HashSet<String>();

			for(String c : efferents.keySet()){
				Map<String,Float> ff = efferents.get(c);
				for(String cc : ff.keySet()){
					if(cc.compareTo(c) < 0)
						deps.add(cc+"-"+c);
					else
						deps.add(c+"-"+cc);
				}
			}
			totalDependencies = deps.size();
		}

		return totalDependencies;
	}

	public String getHighLevelRootPackage() {
		return highLevelRoot;
	}

	//needs to check the packages to whether they have classes or not. It stops on the first package with classes
	public Set<String> getTopLevelPackages() {

		if(highLevelRoot == null){
			highLevelRoot = org.apache.commons.lang3.StringUtils.getCommonPrefix(namespaces.keySet().toArray(new String[]{}));
			if(highLevelRoot.endsWith("."))
				highLevelRoot = highLevelRoot.substring(0, highLevelRoot.length()-1);
		}

		if(highLevelRoot.length() > 0)//there was some common root
			return getPackages(highLevelRoot);

		//there was no common root!
		Set<String> packages = getPackages("");

		Set<String> finalPackages = new HashSet<>();
		for(String p : packages)
			finalPackages.addAll(getPackages(p)); 

		return finalPackages;
	}

	//	private Set<String> getPackages(String localHighLevel) {
	//		
	//		System.out.println(" --------- Starting with: "+localHighLevel);
	//		
	//		Set<String> packages = new HashSet<>();
	//
	//		for(String p : namespaces.keySet()){ //for each package!
	//
	//			if(p.startsWith(localHighLevel)){
	//				
	//				p = p.replace(localHighLevel, "");
	//
	//				if(p.startsWith("."))
	//					p = p.substring(1);
	//				
	//				String [] pa = p.split("\\.");
	//
	//				System.out.println(p+" :: "+pa.length+" "+Arrays.toString(pa));
	//
	//				StringBuilder sb = new StringBuilder();
	//				sb.append(localHighLevel);
	//
	//				System.out.println("sb "+sb);
	//				
	//				boolean hasClasses = false;
	//				
	//				for(int i=0;i<pa.length && !hasClasses;i++){
	//
	//					if(pa[i].length() > 0){
	//						if(sb.length() > 0)
	//							sb.append(".");
	//
	//						sb.append(pa[i]);
	//					}
	//						
	//					System.out.println("Analysing: "+sb+" "+namespaces.containsKey(sb.toString()));
	//
	//					if(namespaces.containsKey(sb.toString()))
	//						hasClasses = true;
	//
	//				}
	//				if(namespaces.containsKey(sb.toString()))
	//					packages.add(sb.toString());
	//
	//			}
	//
	//		}
	//		return packages;
	//	}

	private Set<String> getPackages(String highLevel) {
		Set<String> packages = new HashSet<>();

		int hlr = highLevel.length() - highLevel.replace(".", "").length(); //how many packages in the common root
		if(hlr > 0)
			hlr++;

		for(String p : namespaces.keySet()){ //for each package!

			if(p.startsWith(highLevel)){
				String [] pa = p.split("\\.");

				StringBuilder sb = new StringBuilder();
				sb.append(highLevel);

				boolean hasClasses = false;
				for(int i=hlr;i<pa.length && !hasClasses;i++){

					if(pa[i].length() > 0){
						if(sb.length() > 0)
							sb.append(".");

						sb.append(pa[i]);
					}

					if(namespaces.containsKey(sb.toString()))
						hasClasses = true;

				}

				if(namespaces.containsKey(sb.toString()))
					packages.add(sb.toString());

			}


		}
		return packages;
	}

	private Set<String> getNaiveTopLevelPackage(){

		highLevelRoot = org.apache.commons.lang3.StringUtils.getCommonPrefix(namespaces.keySet().toArray(new String[]{}));
		if(highLevelRoot.endsWith("."))
			highLevelRoot = highLevelRoot.substring(0, highLevelRoot.length()-1);

		if(highLevelRoot.length() != 0)
			return getNaivePackages(highLevelRoot,namespaces.keySet());
		
		//we have no common root - we need to find the potential commons!
		Map<String,Set<String>> potentialRoots = new HashMap<>();
		for(String a : namespaces.keySet()){
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

	private Set<String> getNaivePackages(String topLevel,Set<String> packagesToSearch) {
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

	public static void main(String[] args) {

		String path = "E:/test_arcanSA/apache-camel/apache-camel-1.6.0.jar";

		path = "E:/test_arcanSA/sensitivity-analysis-pipeline/test-input-arcan/camel-core-2.6.0.jar";

		DependencyExtractor extractor = new CDAExtractor();

		Parser parser = new ParserPackage(path,extractor);

		//		System.out.println(parser.clases);
		System.out.println(parser.namespaces);
		//		for(String p : parser.namespaces.keySet())
		//			System.out.println(" -- "+p+" "+parser.namespaces.get(p).size());

		//		parser.namespaces.remove("org.eclipse.tycho.compiler.jdt");
		//		parser.namespaces.remove("org.apache.camel.language"); 


		//		String pp = "META-INF.services.org.apache.camel.component";
		//		System.out.println(parser.namespaces.get(pp));
		//		System.out.println(parser.efferents.get(pp));
		//		System.out.println(parser.afferents.get(pp));
		//		System.out.println();

		parser.namespaces.remove("META-INF.services.org.apache.camel.component");

		//		Set<String> ss = parser.getTopLevelPackages();

		Set<String> ss = parser.getNaiveTopLevelPackage();

		System.out.println(" -- Top Level packages: "+ss.size()+" :: "+ss);
		for(String s : ss)
			System.out.println(s);

	}

}
