package version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.paukov.combinatorics.CombinatoricsVector;
import org.paukov.combinatorics.ICombinatoricsVector;
import org.paukov.combinatorics.permutations.PermutationGenerator;

import it.unimib.disco.essere.arcan.TerminalExecutor;
import parser.Parser;
import parser.factory.FactoryParser;
import sensitivityAnalysis.InterfaceSensitivityAnalysis;

public class VersionADI extends Version{

	static final Logger logger = LogManager.getLogger(VersionADI.class);

	static Map<String,Map<String,double[]>> normalisationValues;

	public VersionADI(String path, String versionName) {
		super(path, versionName);
	}

	@Override
	protected void processVersion(String path) {

		Map<Integer,String> smellMapping = new HashMap<>();

		process(path);
		String dir = path.replace(".jar", "_output");

		try {
			smellMapping.putAll(loadUD(dir));
		} catch (IOException e) {
			logger.error("  Error loading UD ");
		}

		try {
			smellMapping.putAll(loadHL(dir));
		} catch (IOException e) {
			logger.error("  Error loading UD ");
		}

		try {
			smellMapping.putAll(loadCD(dir));
		} catch (IOException e) {
			logger.error("  Error loading UD ");
		}

		try {
			scores = loadADI(dir,smellMapping);
		} catch (IOException e) {
			logger.error("  Error loading ADI Scores ");
		}

		smellMapping.clear();
		smellMapping = null;

	}

	private void process(String path) {

		logger.info("  Starting computation for "+versionName+" ");

		System.setSecurityManager(MySecurityManager.getMySecurityManager(System.getSecurityManager()));

		String output_path = path.replace(".jar","_output");
		
		if(new File(output_path).exists() && correctRun(output_path))
			return;

		String [] args = new String[]{
				"-jar",
				"-p", path,
				"-all",
				"-outputDir",output_path,
				"-cm",
				"-pm",
				"-adi",
		};

		try{
			TerminalExecutor.main(args);
		} catch(RuntimeException e){

		}
		System.setSecurityManager(MySecurityManager.baseSecurityManager);

		logger.info("  Finished computation for "+path);
	}

	private Map<Integer,String> loadUD(String path) throws IOException{

		Map<Integer,String> uds = new HashMap<>();

		String name = path+File.separator+"UD.csv";

		if(new File(name).exists()){

			BufferedReader io = new BufferedReader(new InputStreamReader(new FileInputStream(name)));
			io.readLine();
			String l = io.readLine();
			while(l!=null){

				int index = l.indexOf(",");
				int as = Integer.parseInt(l.substring(0, index));

				l = l.substring(index+1);
				index = l.indexOf(",");

				String s = l.substring(0, index);

				s = s.replace("_", "*");

				uds.put(as,"ud_"+s);

				l = io.readLine();
			}
			io.close();
		}

		if(!uds.isEmpty())
			smellTypes.add("ud");

		return uds;
	}

	private Map<Integer,String> loadHL(String path) throws IOException{

		Map<Integer,String> hls = new HashMap<>();

		String name = path+File.separator+"pkHL.csv";


		if(new File(name).exists()){
			BufferedReader io = new BufferedReader(new InputStreamReader(new FileInputStream(name)));
			io.readLine();
			String l = io.readLine();
			while(l!=null){

				int index = l.indexOf(",");
				int as = Integer.parseInt(l.substring(0, index));

				l = l.substring(index+1);
				index = l.indexOf(",");

				String s = l.substring(0, index);

				s = s.replace("_", "*");

				hls.put(as,"hl_"+s);

				l = io.readLine();
			}
			io.close();
		}

		if(!hls.isEmpty())
			smellTypes.add("hl");

		return hls;
	}

	private Map<Integer,String> loadCD(String path) throws IOException{

		Map<Integer,String> cds = new HashMap<>();

		String name = path+File.separator+"packageCyclicDependencyTable.csv";

		if(new File(name).exists()){

			parser = FactoryParser.getParser(path.replace("_output", ".jar"),"package");

			BufferedReader io = new BufferedReader(new InputStreamReader(new FileInputStream(name)));
			String l = io.readLine();

			l = l.replace("_", "*");

			String [] sp = l.split(",");

			l = io.readLine();


			while(l!=null){

				List<String> tentative_cycle = new ArrayList<String>();

				String [] sl = l.split(",");
				for(int i=2;i<sl.length;i++)
					if(Integer.parseInt(sl[i]) == 1)
						tentative_cycle.add(sp[i]);

				if(tentative_cycle.size() <= 10){ 
					List<String> aux = getCycle(tentative_cycle,parser);

					if(aux != null){ //shouldn't happen... but just in case
						String s = aux.toString().replace("[","").replace("]","").replace(", ",";");
						cds.put(Integer.parseInt(sl[0]),"cd_"+s);

					}

				}

				l = io.readLine();
			}

			io.close();
		}

		if(!cds.isEmpty())
			smellTypes.add("cd");

		return cds;
	}

	private List<String> getCycle(List<String> tentative_cycle, Parser parser) {

		PermutationGenerator<String> permutations = new PermutationGenerator<>(new CombinatoricsVector<>(tentative_cycle));

		Iterator<ICombinatoricsVector<String>> it = permutations.iterator();
		while(it.hasNext()){

			List<String> pot = it.next().getVector();
			if(parser.dependsOn(pot.get(pot.size()-1), pot.get(0))){ //si el �ltimo con el primero...
				boolean depends = true;
				for(int i=0;i<pot.size()-1 && depends;i++)
					if(!parser.dependsOn(pot.get(i), pot.get(i+1)))
						depends = false;
				if(depends)
					return pot;
			}
		}

		return null;
	}

	private Map<String,double[]> loadADI(String path,Map<Integer,String> smellMapping) throws IOException{

		String path_results = path+File.separator+"testADIResults.csv";

		if(!new File(path_results).exists())
			return null;

		if(normalisationValues == null){
			normalisationValues = loadNormalisationValues(VersionADI.class.getResourceAsStream("/resources/quantili-ds-norm.csv"));
//			normalisationValues = loadNormalisationValues(VersionADI.class.getClassLoader().getResource("/resources/quantili-ds-norm.csv").getFile());
		}
			
		//TODO: See why resource file is not found!
		
		int granularity = 2;
		//		if(level.equalsIgnoreCase("package"))
		//			granularity = 2;
		
		Map<String,double[]> smells = new HashMap<>();

		BufferedReader io = new BufferedReader(new InputStreamReader(new FileInputStream(path_results)));
		String l = io.readLine();

		List<String> head = Arrays.asList(l.split(","));
		for(int i=0;i<head.size();i++)
			features.put(head.get(i), i);

		int granularity_index = features.get("GranularityValue");
		int AS_index = features.get("AS");
		int smellType = features.get("Type");

		l = io.readLine();
		while(l != null){

			String [] sl = l.split(",");

			if(Integer.parseInt(sl[granularity_index]) == granularity){ //if it is the granularity I am looking for

				String map = smellMapping.get(Integer.parseInt(sl[AS_index]));

				if(map != null){
					double [] fes = new double[features.size()-4];
					for(int i=4;i<head.size();i++){ 
//						fes[i-4] = Double.parseDouble(sl[features.get(head.get(i))]);
						fes[i-4] = normalize(Double.parseDouble(sl[features.get(head.get(i))]),sl[smellType],head.get(i));
					}

					smells.put(map,fes);
				}

			}

			l = io.readLine();
		}

		features.clear();
		for(int i=4;i<head.size();i++){
			features.put(head.get(i).toLowerCase(), i-4);//only the relevant ones
			sortedFeatures.add(head.get(i).toLowerCase());
		}

		io.close();

		return smells;
	}

	private double normalize(double value, String smellType, String feature) {
		
		Map<String,double[]> scorePerType = normalisationValues.get(smellType);
		if(scorePerType == null)
			return value;
		
		double [] quantiles = scorePerType.get(feature);
		if(quantiles == null)
			return value;
		
		for (int i=0; i < quantiles.length; i++) {
			if (value < quantiles[i])
				return (i * 10 / 200.0);
		}
		
		return 1.0;
	}

	//given a path, loads the normalisation scores -- <smell type , <feature, scores>>
//	private static Map<String, Map<String,double[]>> loadNormalisationValues(String path) {
		private static Map<String, Map<String,double[]>> loadNormalisationValues(InputStream path) {

		Map<String,Map<String,double[]>> normalization = new HashMap<>();

		for(String st : smellTypes)
			normalization.put(st, new HashMap<>());
		
		BufferedReader io = null;

//		try {
//			io = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF8"));
			io = new BufferedReader(new InputStreamReader(path));
//		} catch (IOException e) {
//			logger.warn(" The normalisation file does not exist ");
//		}

//		if(io == null){
//			logger.warn(" The normalisation file does not exist ");
//			return normalization;
//		}
				
		try {
			String l = io.readLine();
			l = io.readLine();
			while(l != null){
						
				String [] sp = l.split(",");
				String st = null;
				String feature = null;

				switch(sp[0].substring(1, sp[0].length()-1)){
				case "quantile.ds.cd.package.noc":
					st = "cd";
					feature = "NumOfCycle";
					break;
				case "quantile.ds.cd.package.nov":
					st = "cd";
					feature = "NumberOfVerteces";
					break;
				case "quantile.ds.cd.package.pr":
					st = "cd";
					feature = "PageRank";
					break;
				case "quantile.ds.cd.package.sv":
					st = "cd";
					feature = "SeverityScore";
					break;
				case "quantile.ds.hl.package.pr":
					st = "hl";
					feature = "PageRank";
					break;
				case "quantile.ds.hl.package.sv":
					st = "hl";
					feature = "SeverityScore";
					break;
				case "quantile.ds.hl.package.td":
					st = "hl";
					feature = "TotalDep";
					break;
				case "quantile.ds.ud.nbd":
					st = "ud";
					feature = "NumBadDep";
					break;
				case "quantile.ds.ud.pr":
					st = "ud";
					feature = "PageRank";
					break;
				case "quantile.ds.ud.sv":
					st = "ud";
					feature = "SeverityScore";
					break;
				}
				
				if(st != null){
					double [] values = new double[sp.length-1];
					for(int i=1;i<sp.length;i++){
						values[i-1] = Double.parseDouble(sp[i]);
					}
					
					Map<String,double[]> aux = normalization.get(st);
					if(aux == null){
						aux = new HashMap<>();
						normalization.put(st, aux);
					}
					aux.put(feature, values);
				}
			
				
				l = io.readLine();
			}
			logger.info(" Normalisation file loaded");
//			System.out.println(normalization);
		} catch (IOException e) {
			logger.error(" Error reading the normalisation file ");
		}
		
		return normalization;
	}

	@Override
	public String findSmell(String smell) {

		if(scores.containsKey(smell))
			return smell;

		if(!smell.startsWith("cd_"))
			return null;

		Set<String> cycles = smells.get("cd");

		List<String> smell_cycle = Arrays.asList(smell.replace("cd_", "").split(";"));

		List<String> cycle_aux = null;		

		for(String cycle : cycles){

			cycle_aux = Arrays.asList(cycle.replace("cd_", "").split(";"));

			if(cycle_aux.size() == smell_cycle.size()){ //if we take this out, we can find any type of correspondence between cycles

				if(isAMatch(smell_cycle, cycle_aux))
					return cycle;
				
//				int firstIndex = cycle_aux.indexOf(smell_cycle[0]);
//				if(firstIndex < 0)
//					continue; 
//
//				int index = firstIndex;
//
//				int currentIndex = 0;
//				int numberDecreased = 0;
//				boolean potential = true;
//				for(int i=1;i<smell_cycle.length && potential;i++){
//
//					currentIndex = cycle_aux.indexOf(smell_cycle[i]);
//					if(currentIndex < 0){
//						potential = false;
//						break;
//					}
//
//					if(numberDecreased == 0){ //si todav�a no baj�, puede bajar uno
//						if(currentIndex < index)
//							numberDecreased++;
//					}
//					else{//si ya baj�, no puede volver a bajar
//
//						if(currentIndex < index || currentIndex > firstIndex){
//							potential = false;
//							break;
//						}
//
//					}
//
//					index = currentIndex;
//
//				}
//
//				if(potential)
//					return cycle;

			}

		}
		return null;
	}

	@Override //When no exact matching is possible, we should check whether this smell is an expansion or reduction of other smells TODO
	public Set<String> findPotentiallyMatchingSmells(String smell){
		Set<String> matchings = new HashSet<>();
		
		if(scores.containsKey(smell)){
			matchings.add(smell);
			return matchings;
		}
	
		if(!smell.startsWith("cd_"))
			return matchings;

		Set<String> cycles = smells.get("cd");

		List<String> smell_cycle = Arrays.asList(smell.replace("cd_", "").split(";"));

		List<String> cycle_aux = null;		

		for(String cycle : cycles){

			cycle_aux = Arrays.asList(cycle.replace("cd_", "").split(";"));

			if(cycle_aux.size() < smell_cycle.size()) //at this point, we are not interested in exact matchings
				if(isAMatch(cycle_aux, smell_cycle))
					matchings.add(cycle);
			else
				if(cycle_aux.size() > smell_cycle.size())
					if(isAMatch(smell_cycle, cycle_aux))
						matchings.add(cycle);
		}
		
		return matchings;
	}
	
	
	
	private boolean correctRun(String out) {

		File f = new File(out+File.separator+"classCyclesShapeTable.csv");
		if(!f.exists()){
			InterfaceSensitivityAnalysis.deleteRecursive(new File(out));
			return false;
		}
		f = new File(out+File.separator+"classCyclicDependencyMatrix.csv");
		if(!f.exists()){
			InterfaceSensitivityAnalysis.deleteRecursive(new File(out));
			return false;
		}
		f = new File(out+File.separator+"classCyclicDependencyTable.csv");
		if(!f.exists()){
			InterfaceSensitivityAnalysis.deleteRecursive(new File(out));
			return false;
		}
		f = new File(out+File.separator+"CM.csv");
		if(!f.exists()){
			InterfaceSensitivityAnalysis.deleteRecursive(new File(out));
			return false;
		}
		f = new File(out+File.separator+"mas.csv");
		if(!f.exists()){
			InterfaceSensitivityAnalysis.deleteRecursive(new File(out));
			return false;
		}
		f = new File(out+File.separator+"packageCyclesShapeTable.csv");
		if(!f.exists()){
			InterfaceSensitivityAnalysis.deleteRecursive(new File(out));
			return false;
		}
		f = new File(out+File.separator+"packageCyclicDependencyMatrix.csv");
		if(!f.exists()){
			InterfaceSensitivityAnalysis.deleteRecursive(new File(out));
			return false;
		}
		f = new File(out+File.separator+"packageCyclicDependencyTable.csv");
		if(!f.exists()){
			InterfaceSensitivityAnalysis.deleteRecursive(new File(out));
			return false;
		}
		f = new File(out+File.separator+"pkHL.csv");
		if(!f.exists()){
			InterfaceSensitivityAnalysis.deleteRecursive(new File(out));
			return false;
		}
		f = new File(out+File.separator+"PM.csv");
		if(!f.exists()){
			InterfaceSensitivityAnalysis.deleteRecursive(new File(out));
			return false;
		}
		f = new File(out+File.separator+"testADIResults.csv");
		if(!f.exists()){
			InterfaceSensitivityAnalysis.deleteRecursive(new File(out));
			return false;
		}
		f = new File(out+File.separator+"UD.csv");
		if(!f.exists()){
			InterfaceSensitivityAnalysis.deleteRecursive(new File(out));
			return false;
		}
		f = new File(out+File.separator+"UD30.csv");
		if(!f.exists()){
			InterfaceSensitivityAnalysis.deleteRecursive(new File(out));
			return false;
		}

		return true;
	}




	public static void main(String[] args) {
		
//		Version v = new VersionADI("","");
//		v.smells = new HashMap<>();
//		v.smells.clear();
//		v.smells.put("cd", new HashSet<>());
//		v.smells.get("cd").add("A;B;C");
//		v.smells.get("cd").add("A;F;C");
//		v.smells.get("cd").add("A;D;C");
//
//		System.out.println(v.findSmell("cd_B;C;A"));
//		System.out.println(v.findSmell("cd_C;A;B"));
//		System.out.println(v.findSmell("cd_B;A;C"));
//		System.out.println(v.findSmell("cd_D;C;A"));

	}

}
