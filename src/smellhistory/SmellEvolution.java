package smellhistory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import parser.Parser;
import parser.factory.FactoryParser;
import sensitivityAnalysis.InterfaceSensitivityAnalysis;
import version.NaturalOrderComparator;
import version.Version;

public class SmellEvolution {

	String firstVersion;
	String lastVersion;

	Map<String,Version> versions; //only has sense when we had to compute everything, instead of loading it... so, it won't be available when we simply load the computed file
	List<String> sortedVersions;

	Map<String,Map<String,double[]>> smellEvolution; // <smell, feature, scores across versions []>
	//double [] .length == number of versions

	List<String> sortedFeatures; //has the features to analyse. 
	//Might not be the full set of generated features. Nonetheless, EVERY feature is saved so as to load the evolution without problems

	String indexUnderAnalysis;

	String highLevelRoot = null;

	static final Logger logger = LogManager.getLogger(SmellEvolution.class);

	//could use one constructor with variable parameters
	public SmellEvolution(String indexUnderAnalysis, String version1, String version2){

		this.indexUnderAnalysis = indexUnderAnalysis;
		this.firstVersion = version1;
		this.lastVersion = version2;

		smellEvolution = new HashMap<>();
		versions = new HashMap<>();
		sortedVersions = new ArrayList<>();

		sortedFeatures = new ArrayList<>();

		loadEvolution();

	}

	public SmellEvolution(String indexUnderAnalysis, String version1, String version2, String [] features){

		this.indexUnderAnalysis = indexUnderAnalysis;
		this.firstVersion = version1;
		this.lastVersion = version2;

		smellEvolution = new HashMap<>();
		versions = new HashMap<>();
		sortedVersions = new ArrayList<>();

		sortedFeatures = new ArrayList<>();
		for(String f : features)
			sortedFeatures.add(f.toLowerCase());		

		loadEvolution();
	}

	public List<String> getVersions(){
		return sortedVersions;
	}

	public boolean hasVersion(String version){
		return versions.containsKey(version);
	}

	public void addVersion(Version version){
		versions.put(version.getVersion(), version);

	}

	public Map<String,Map<String,double[]>> getEvolution(){
		return smellEvolution;
	}

	//only keeps those smells appearing in the last version
	public Map<String,Map<String,double[]>> filterSmells(){

		logger.info(" Filtering smells not appearing in the last version ");

		Map<String,Map<String,double[]>> filteredSmellEvolution = new HashMap<>();

		//		System.out.println(smellEvolution);

		for(Entry<String, Map<String,double[]>> e : smellEvolution.entrySet()){

			Map<String,double[]> values = e.getValue();
			boolean valued = true;
			for(String k : values.keySet()){
				//				System.out.println("Filter? "+e.getKey()+" "+Arrays.toString(values.get(k)));
				if(!Double.isFinite(values.get(k)[sortedVersions.size()-1]))
					valued = false;
			}


			if(valued) //if appeared in the last version	
				filteredSmellEvolution.put(e.getKey(), e.getValue());

		}

		return filteredSmellEvolution;
	}

	//once all versions are loaded, builds the evolution path of smells... T
	public void buildSmellEvolution() {

		logger.info(" Building smell evolution ");

		if(!smellEvolution.isEmpty()){ //it has been already loaded
			logger.info(" Finished building smell evolution ");
			return;
		}

		List<String> allFeatures = new ArrayList<>(versions.get(lastVersion).getFeatures());
		Collections.sort(allFeatures);

//				System.out.println(allFeatures);

		sortedVersions.addAll(versions.keySet());
		Collections.sort(sortedVersions,new NaturalOrderComparator());
		
		Set<String> smellsInThisVersion = new HashSet<>(); //could be replaced by simply considering those that do not have a score for the current version

		//		Map<String,Set<String>> smellMatchings = new HashMap<>();

		for(int currentVersion=0;currentVersion<sortedVersions.size();currentVersion++){ //for each version to analyse

			smellsInThisVersion.clear();

			//			System.out.println("------------------------------");
			//			smellMatchings.clear();

			Version version = versions.get(sortedVersions.get(currentVersion));

			Set<String> smells = version.getSmells();

			for(String smell : smells){ //for each smell in the version...

//				System.out.println(smell);
				
				double [] currentScores = version.getScoresForSmell(smell);

				Map<String,double[]> previousScores = smellEvolution.get(smell);

				int foundVersion = currentVersion;

//				System.out.println(Arrays.toString(currentScores));
//				System.out.println(previousScores);
				
				if(previousScores == null && currentVersion > 0){ // i > 0 means that there was another version and we can try to match it

//					System.out.println("previous scores null & not first version");
					
					String alternative = null;
					for(int vant = currentVersion-1;vant>=0 && alternative == null;vant--){
						alternative = versions.get(sortedVersions.get(vant)).findSmell(smell); 
						if(alternative != null){
							previousScores = smellEvolution.get(alternative);
							smellsInThisVersion.add(alternative);
							foundVersion = vant;

						}
					}

				}

				if(previousScores != null){ //updates scores

//					System.out.println("encontré con qué matchear");
					
					for(int j=0;j<allFeatures.size();j++){
						double [] sc = previousScores.get(allFeatures.get(j));
						sc[currentVersion] = currentScores[version.indexOfFeature(allFeatures.get(j))];

						for(int k=foundVersion+1;k<currentVersion;k++) //for previous versions up to the in which it was found
							sc[k] = Double.NEGATIVE_INFINITY; //discernible!
					}

					smellsInThisVersion.add(smell);

				}
				else{ //no exact matching was found... we need to check for other types of matchings

					//					Set<String> potentialMatches = new HashSet<>();
					//					for(int vant = currentVersion-1;vant>=0 && potentialMatches.isEmpty();vant--){
					//						potentialMatches.addAll(versions.get(sortedVersions.get(vant)).findPotentiallyMatchingSmells(smell)); 
					//						if(!potentialMatches.isEmpty())
					//							foundVersion = vant;
					//					}
					//					
					//					if(potentialMatches.isEmpty()){ //if we could not find any potential matching smell

					previousScores = new HashMap<String,double[]>();

					for(int j=0;j<allFeatures.size();j++){

						double [] sc = new double[versions.size()];
						sc[currentVersion] = currentScores[version.indexOfFeature(allFeatures.get(j))];

						for(int k=0;k<currentVersion;k++) //for previous versions up to the current one
							sc[k] = Double.NEGATIVE_INFINITY; //discernible!

						previousScores.put(allFeatures.get(j), sc);

					}
					smellsInThisVersion.add(smell);
					smellEvolution.put(smell, previousScores);
					//						
					//					}
					//					else
					//						smellMatchings.put(smell,potentialMatches);

				}
			}

			//once we know every smell in this version and the potential matches, it has to be analysed whether the potential matchings could actually we considered
			//should keep control of smells that are used as mappings so as to add them to the smellsInThisVersion
			//			Map<String,Integer> cantMappings = new HashMap<>();
			//			for(Entry<String,Set<String>> e : smellMatchings.entrySet()){
			//				
			//				Set<String> matches = e.getValue();
			//				for(String m : matches){
			//					if(!smellsInThisVersion.contains(m)){ //if the matching is not in this version
			//						
			//						Map<String,double[]> scores = smellEvolution.get(m);
			//						
			//						//TODO!!!! has to use currentScores
			//						
			//					}
			//				}
			//				
			//			}


			//this can be avoided if we check later whether for a certain version, a smell has all zero values
			for(String smelly : smellEvolution.keySet())
				if(!smellsInThisVersion.contains(smelly)){ //not was found in this version

					Map<String,double[]> smellyScores = smellEvolution.get(smelly);
					//					boolean allZeros = true;
					//					for(String k : smellyScores.keySet())
					//						if(smellyScores.get(k)[currentVersion] != 0) //to avoid the need of adding the matched ones to the structure
					//							allZeros = false;
					//					if(allZeros)
					for(String k : smellyScores.keySet())
						smellyScores.get(k)[currentVersion] = Double.NEGATIVE_INFINITY;

				}

			//			for(String k : smellEvolution.keySet()){
			//				Map<String,double[]> aux = smellEvolution.get(k);
			////				for(String f : aux.keySet()){
			//					String f = aux.keySet().iterator().next();
			//					System.out.println(k+"-"+f+"-"+Arrays.toString(aux.get(f)));
			////				}
			//					
			//			}
			//			try {
			//				System.in.read();
			//			} catch (IOException e) {
			//				e.printStackTrace();
			//			}
		}

		if(sortedFeatures.isEmpty())
			sortedFeatures.addAll(smellEvolution.get(smellEvolution.keySet().iterator().next()).keySet());

		//		System.out.println("  -- SORTED FEATURES: "+sortedFeatures);

		logger.info(" Finish building smell evolution ");

	}

	//Save every feature, not just the ones potentially selected by the user
	public void saveEvolution(){

		logger.info(" Saving smell_evolution ");

		if(new File(InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"smell_evolution_"+indexUnderAnalysis+"__"+firstVersion+"_"+lastVersion+".csv").exists()){
			logger.info(" File already exists ");
			return;
		}

		StringBuffer sb = new StringBuffer();

		sb.append("type");sb.append(InterfaceSensitivityAnalysis.STRING_SEPARATOR);sb.append("smell");sb.append(InterfaceSensitivityAnalysis.STRING_SEPARATOR);
		sb.append(sortedVersions.toString().replace("[", "").replace("]", "").replace(", ", ","));
		sb.append(System.lineSeparator());

		for(String s : smellEvolution.keySet()){

			Map<String,double[]> smellScores = smellEvolution.get(s);

			for(String k : smellScores.keySet()){
				sb.append(s.replace("_", ","));sb.append("_");sb.append(k);sb.append(InterfaceSensitivityAnalysis.STRING_SEPARATOR);

				double [] scoresFeature = smellScores.get(k);

				for(int j=0;j<scoresFeature.length;j++){	
					sb.append(scoresFeature[j]);
					sb.append(InterfaceSensitivityAnalysis.STRING_SEPARATOR);
				}
				sb.append(System.lineSeparator());
			}

		}

		//writing...
		BufferedWriter out;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"smell_evolution_"+indexUnderAnalysis+"__"+firstVersion+"_"+lastVersion+".csv", false), "UTF8"));
			out.write(sb.toString());
			out.close();
		} catch (IOException e) {
			logger.error(" Error while writing smell_evolution ");
		}

		logger.info(" Finished saving smell_evolution ");


		reduceFeatureSet();

	}


	//once it is saved, we only keep those features that were actually selected by the user
	private void reduceFeatureSet(){

		for(Entry<String, Map<String, double[]>> e : smellEvolution.entrySet()){ //for each smell, only keep those features that were previously selected
			e.getValue().keySet().retainAll(sortedFeatures);
		}

	}

	private void loadEvolution(){

		String fileName = findFile();
		
		if(fileName == null){
			logger.warn(" Smell Evolution file does not exist ");
			return;
		}
		
		BufferedReader io = null;

		try {
			io = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF8"));
		} catch (IOException e) {
			logger.warn(" Problem opening evolution file ");
		}

		if(io == null)
			return;

		logger.info(" Loading Smell Evolution file "+fileName);

		try {
			String l = io.readLine();
			String [] sp = l.split(InterfaceSensitivityAnalysis.STRING_SEPARATOR);
			int versions = sp.length - 2;

			for(int i=2;i<sp.length;i++)
				sortedVersions.add(sp[i]);

			l = io.readLine();
			while(l != null){

				sp = l.split(InterfaceSensitivityAnalysis.STRING_SEPARATOR);

				double [] scores = new double[versions];

				for(int i=2;i<sp.length;i++)
					scores[i-2] = Double.parseDouble(sp[i]);

				String [] spsf = sp[1].split("_");

				String smell = sp[0]+"_"+spsf[0];
				Map<String,double[]> fscores = smellEvolution.get(smell);
				if(fscores == null){
					fscores = new HashMap<>();
					smellEvolution.put(smell, fscores);
				}
				fscores.put(spsf[1].toLowerCase(), scores);

				l = io.readLine();
			}
			io.close();

			if(sortedFeatures.isEmpty())
				sortedFeatures.addAll(smellEvolution.get(smellEvolution.keySet().iterator().next()).keySet());

			reduceFeatureSet();

			//			for(String k : smellEvolution.keySet()){
			//				Map<String,double[]> aux = smellEvolution.get(k);
			////				for(String f : aux.keySet())
			////					System.out.println(k+"-"+f+"-"+Arrays.toString(aux.get(f)));
			//				String f = aux.keySet().iterator().next();
			//				System.out.println(k+"-"+f+"-"+Arrays.toString(aux.get(f)));
			//			}

		} catch (IOException e) {
			logger.error(" Error reading Smell Evolution file. ");
		}

	}

	private String findFile() {
		
		if(firstVersion != null && lastVersion != null){ //if actual versions were passed as parameters, we look for that file
			String name = InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"smell_evolution_"+indexUnderAnalysis+"__"+firstVersion+"_"+lastVersion+".csv";
			if(new File(name).exists())
				return name;
			return null;
		}
		
		//if not, we look for the "last" file, meaning the file with the largest span of versions as defined in the comparator
		
		logger.warn("No versions were selected. Searching for the evolution file with the largest span of analysed versions...");
		
		Set<String> names = new HashSet<>();
		
		Set<String> versions = new HashSet<>();
		
		File f = new File(InterfaceSensitivityAnalysis.INPUT_PATH);
		for(File ff : f.listFiles())
			if(ff.isFile() && ff.getName().startsWith("smell_evolution_"+indexUnderAnalysis+"__")){
				names.add(ff.getAbsolutePath());
				versions.add(ff.getName().substring(ff.getName().lastIndexOf("__")+2,ff.getName().lastIndexOf("_")));
				versions.add(ff.getName().substring(ff.getName().lastIndexOf("_")+1,ff.getName().lastIndexOf(".")));
			}
				
		if(names.isEmpty())
			return null;
		
		List<String> versionsSorted = new ArrayList<>(versions);
		Collections.sort(versionsSorted,new NaturalOrderComparator());
		
		String modifiedPath = null;
		if(!InterfaceSensitivityAnalysis.INPUT_PATH.contains(File.separator))
			modifiedPath = InterfaceSensitivityAnalysis.INPUT_PATH.replace("\\", File.separator).replace("/", File.separator);
		else
			modifiedPath = InterfaceSensitivityAnalysis.INPUT_PATH;
		
		modifiedPath = modifiedPath + File.separator+"smell_evolution_"+indexUnderAnalysis+"__";
		
		String potentialPath = null;
		
		for(int i=versionsSorted.size()-1;i>=0;i--){ //from the last one!
			
			String lv = versionsSorted.get(i);
			
			for(int j=0;j<i;j++){
				potentialPath = modifiedPath+versionsSorted.get(j)+"_"+lv+".csv";
				if(names.contains(potentialPath))
					return potentialPath;
			
			}
				
		}
		
		return null;
		
	}

	public List<String> getFeatures() {
		return sortedFeatures;
	}


	public List<String> getTopLevelPackagesForVersion(String version){

		logger.info("Getting top level packages for version: "+version);
		
		//if the version does not exist, we cannot find packages
		if(!sortedVersions.contains(version)){
			logger.error("Version "+version+" was not found. Top level packages are empty. ");
			return new ArrayList<>();
		}
			

		//if by any chance we needed to create the versions, they might already have access to the .jar, so we can get the packages with classes inside
		Version v = versions.get(version);
		if(v != null){ 
			logger.info("Getting top level packages with classes inside for: "+version);
			List<String> pp = v.getTopLevelPackages();
			highLevelRoot = Version.getHighLevelRoot();
			logger.info("Top level packages for: "+version+" "+pp.size());
			return pp;
		}

		//we did not create the Version, but the .jar might still be available, so we can get the packages with the classes inside
		String fileName = InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+version+".jar";
		if(new File(fileName).exists()){
			logger.info("Getting top level packages with classes inside for: "+version);
			Parser parser = FactoryParser.getParser(fileName,"package");
			List<String> aux = new ArrayList<String> (parser.getTopLevelPackages());
			highLevelRoot = parser.getHighLevelRootPackage();
			logger.info("Top level packages for: "+version+" "+aux.size());
			return aux;
		}				

		//if we are here, we do not have neither the version nor the .jar, so we can only naïvely check for the first level packages ... re using Version code
		
		logger.info("Getting naïve top level packages for: "+version);
		int versionN = sortedVersions.indexOf(version);
		//we first need to find which smells appear in the selected version
		Set<String> smellsInVersion = new HashSet<>();
		for(Entry<String,Map<String,double[]>> e : smellEvolution.entrySet()){
			Map<String,double[]> scores = e.getValue();
			if(Double.isFinite(scores.get(scores.keySet().iterator().next())[versionN])) //if the score is finite
				smellsInVersion.add(e.getKey());
		}
		
		Set<String> packages = Version.getTopLevelPackages(smellsInVersion);
		highLevelRoot = Version.getHighLevelRoot();
		logger.info("Top level packages for: "+version+" "+packages.size());
		return new ArrayList<>(packages);
	}

	public Map<String,List<String>> getTopLevelPackages(){
		Map<String,List<String>> packages = new HashMap<>();
		for(String v : sortedVersions)
			packages.put(v, getTopLevelPackagesForVersion(v));
				
		return packages;
	}

	public String getHighLevelRoot(){
		return highLevelRoot;
	}

	public void setVersions(String v1, String v2){
		firstVersion = v1;
		lastVersion = v2;
	}

	public String getFirstVersion() {
		return firstVersion;
	}
	
	public String getLastVersion() {
		return lastVersion;
	}
	
}
