package sensitivityAnalysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sensitivityAnalysis.index.PackageSmellGroup;
import sensitivityAnalysis.index.SingleSmellGroup;
import sensitivityAnalysis.index.SmellGroup;
import sensitivityAnalysis.index.SmellTypeGroup;
import sensitivityAnalysis.sobol.SAnalysis;
import smellhistory.SmellEvolution;
import smellhistory.SmellFactory;
import version.FactoryVersion;
import version.MySecurityManager;
import version.NaturalOrderComparator;
import version.Version;

public class InterfaceSensitivityAnalysis {

	static final Logger logger = LogManager.getLogger(InterfaceSensitivityAnalysis.class);

	private static String OPTION_HELP = "help";

	private static String OPTION_INPUT_PATH = "path";

	private static String OPTION_INDEX = "index";

	private static String OPTION_MORRIS = "morris";

	private static String OPTION_SOBOL = "sobol";

	private static String OPTION_VERSIONS = "versions";

//	private static String OPTION_FEATURES = "features";

	private static String OPTION_LEVEL = "level";

	public static String STRING_SEPARATOR = ",";

	public static String INPUT_PATH = null; 
	public static String LEVEL;
	public static String INDEX;
	public static String SENSITIVITY;

	//////////////////////////////////////////////////////////////////////////////////////////

	public static double SA_SCALING_FACTOR 	= 100000;
	public static int SALTELLI_SAMPLES 		= 3000;

	//////////////////////////////////////////////////////////////////////////////////////////

	private static Set<String> option_indices = new HashSet<>();
	private static Map<String,String> indices_suffixes = new HashMap<>();

	private static Options getOptions(){

		Options options = new Options();

		Option path = new Option(OPTION_INPUT_PATH, "Directory with the input data.");
		path.setArgs(1);
		path.setOptionalArg(true); //to avoid parsing errors
		options.addOption(path);

		Option index = new Option(OPTION_INDEX,"Specifies the index over which run the sensitivity analysis. "
				+"Possible options: arcan, sonargraph. "
				+ "In the case of Arcan, input must include the .jar corresponding to the selected versions."
				+ "In the case of Sonargraph input must include the .xml reports corresponding to the selected versions.");
		index.setArgs(1);
		index.setOptionalArg(true); //to avoid parsing errors
		options.addOption(index);

		Option versions = new Option(OPTION_VERSIONS, "The span of versions to analyse.");
		versions.setArgs(2);
		versions.setOptionalArg(true); //to avoid parsing errors
		options.addOption(versions);

		//		Option features = new Option(OPTION_FEATURES, "The features to include in the analysis. "
		//				+ "If it not specified, all features are considered.");
		//		features.setArgs(Option.UNLIMITED_VALUES);
		//		features.setOptionalArg(true); //to avoid parsing errors
		//		options.addOption(features);

		Option level = new Option(OPTION_LEVEL,"Specifies the abstraction level at to which perform the sensitivity analysis. "
				+ "The availability of this option depends on the index under analysis."
				+"Possible options: smell-instance, smell-type, package-group.");
		level.setArgs(1);
		level.setOptionalArg(true); //to avoid parsing errors
		options.addOption(level);

		//		options.addOption(OPTION_MORRIS, false,"Runs the Morris analysis.");
		options.addOption(OPTION_SOBOL, false,"Runs the Sobol analysis.");

		Option logger = new Option("logger",true,"The default logger is ALL. It can be changed to WARNING, SEVERE, INFO or OFF");
		options.addOption(logger);

		options.addOption(OPTION_HELP,false,"Prints this message");

		return options;

	}

	private static void loadAlternatives(){
		option_indices.add("arcan");
		option_indices.add("sonargraph");

		indices_suffixes.put("arcan", ".jar");
		indices_suffixes.put("sonargraph", ".xml");

	}


	public static void main(String[] args) {

		//		BasicConfigurator.configure();
		//		LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
		//		File file = new File(InterfaceSensitivityAnalysis.class.getResource("/resources/quantili-ds-norm.csv").getFile());
		//		context.setConfigLocation(file.toURI());
		org.apache.logging.log4j.core.config.Configurator.setRootLevel(Level.INFO);

		loadAlternatives();

		//		System.setProperty("log4j.skipJansi", "true"); //to remove a WARN

		Options options = getOptions();
		CommandLineParser parser = new DefaultParser();
		try {

			CommandLine line = parser.parse(options, args);

			if(line.hasOption("help") || line.getOptions().length == 0){
				HelpFormatter formatter = new HelpFormatter();
				formatter.setOptionComparator(null);
				formatter.printHelp("Architectural Debt Index Sensitivity Analysis", "", options, "", true);
				System.exit(0);
			}

			if(line.hasOption("logger")){
				String l = line.getOptionValue("logger");
				if(l != null)
					switch (l.toLowerCase()) {
					case "warning":
						logger.info("The log level was changed to: WARNING.");
						org.apache.logging.log4j.core.config.Configurator.setRootLevel(Level.WARN);
						break;
					case "all":
						logger.info("The log level was changed to: ALL.");
						org.apache.logging.log4j.core.config.Configurator.setRootLevel(Level.ALL);
						break;
					case "severe":
						logger.info("The log level was changed to: SEVERE.");
						org.apache.logging.log4j.core.config.Configurator.setRootLevel(Level.ERROR);
						break;
					case "info":
						logger.info("The log level was changed to: INFO.");
						org.apache.logging.log4j.core.config.Configurator.setRootLevel(Level.INFO);
						break;
					case "off":
						logger.info("The log level was changed to: OFF.");
						org.apache.logging.log4j.core.config.Configurator.setRootLevel(Level.OFF);
						break;
					default:
						logger.info("The option was not recognise. The log level was not changed.");
						break;
					}
			}

			if(!line.hasOption(OPTION_INPUT_PATH)){
				logger.error(" The input path is missing. ");
				System.exit(0);
			}

			if(!line.hasOption(OPTION_INDEX)){
				logger.error(" The Index to analyse is missing. ");
				System.exit(0);
			}

			if(line.hasOption(OPTION_INPUT_PATH)){
				INPUT_PATH = line.getOptionValue(OPTION_INPUT_PATH);
				if(INPUT_PATH == null || INPUT_PATH.length() < 2 || !new File(INPUT_PATH).exists()){
					logger.error(" Input path is missing or does not exists. ");
					System.exit(0);
				}
			}

			if(line.hasOption(OPTION_INDEX)){

				option_indices.add("arcan");
				option_indices.add("sonargraph");

				INDEX = line.getOptionValue(OPTION_INDEX).toLowerCase();
				if(INDEX == null || INDEX.length() < 2){
					logger.warn(" The Index to analyse is missing. The default Arcan ADI was selected. ");
					INDEX = "arcan";
				}
				else
					if(!option_indices.contains(INDEX)){
						logger.warn(" The Index to analyse was not recognised. The default Arcan ADI was selected. ");
						INDEX = "arcan";
					}

				if(INDEX.equals("sonargraph"))
					SA_SCALING_FACTOR = 0.1;

			}

			String firstVersion = null;
			String lastVersion = null;

			if(line.hasOption(OPTION_VERSIONS)){

				String [] versions = line.getOptionValues(OPTION_VERSIONS);
				if(versions == null || versions.length != 2){
					logger.error("Versions are missing");
					System.exit(0);
				}

				//				System.out.println(java.util.Arrays.toString(versions));

				if(new NaturalOrderComparator().compare(versions[0], versions[1]) > 0){ //first one is higher
					logger.warn(" "+versions[1]+" and "+versions[0]+" were interchaged. ");
					firstVersion = versions[1];
					lastVersion = versions[0];
				}
				else{
					firstVersion = versions[0];
					lastVersion = versions[1];
				}
			}


			//			SmellEvolution evolution = null;

			List<String> versionsInFolder = getVersionsInFolder();

			if(firstVersion == null && !versionsInFolder.isEmpty()){
				firstVersion = versionsInFolder.get(0);
				lastVersion = versionsInFolder.get(versionsInFolder.size()-1);
			}

			//			if(!line.hasOption(OPTION_FEATURES)){
			//				logger.info(" No features were selected. All features will be included in the analysis. ");
			//				evolution = new SmellEvolution(INDEX, firstVersion, lastVersion);
			//			}
			//			else{
			//				String [] features = line.getOptionValues(OPTION_FEATURES);
			//				evolution = new SmellEvolution(INDEX, firstVersion, lastVersion, features);
			//			}

			SmellEvolution evolution = new SmellEvolution(INDEX, firstVersion, lastVersion);

			if(evolution.getVersions().isEmpty()){

				int firstVersionIndex = versionsInFolder.indexOf(firstVersion);
				int lastVersionIndex = versionsInFolder.indexOf(lastVersion);

				if(firstVersionIndex < 0 || lastVersionIndex < 0){
					logger.error(" Versions "+firstVersion+" or "+lastVersion+" do not exist ");
					System.exit(0);
				}

				if(firstVersionIndex > lastVersionIndex){

					logger.warn(" "+lastVersion+" and "+firstVersion+" were interchaged. ");
					int aux = lastVersionIndex;
					lastVersionIndex = firstVersionIndex;
					firstVersionIndex = aux;

				}

				versionsInFolder = versionsInFolder.subList(firstVersionIndex, lastVersionIndex+1);
				logger.info(" Selected versions to analyse: "+versionsInFolder+" ");

				firstVersion = versionsInFolder.get(0);
				lastVersion = versionsInFolder.get(versionsInFolder.size()-1);

				evolution.setVersions(firstVersion, lastVersion);

				List<Version> versions = buildVersions(versionsInFolder);

				for(Version v : versions)
					evolution.addVersion(v);

				evolution.buildSmellEvolution();
				evolution.saveEvolution();

			}

			//			System.out.println("  -- Final Filtered smells: "+evolution.filterSmells().size());
			//
			//			Map<String,Map<String,double[]>> evol = evolution.getEvolution();
			//			for(String s : evol.keySet()){
			//				Map<String,double[]> aux = evol.get(s);
			//				for(String fe : aux.keySet())
			//					System.out.println(s+"_"+fe+"_"+java.util.Arrays.toString(aux.get(fe)));
			//
			//			}

			//			System.out.println(evolution.getFeatures());

			//			System.out.println("-------------------------------");

			//						Map<String,Map<String,double[]>> evol_filt = evolution.filterSmells();
			//						System.out.println(evol_filt.size());
			//						for(String s : evol_filt.keySet()){
			//							Map<String,double[]> aux = evol_filt.get(s);
			//							for(String fe : aux.keySet())
			//								System.out.println(s+"_"+fe+"_"+Arrays.toString(aux.get(fe)));
			//							
			//						}

			if(!line.hasOption(OPTION_LEVEL)){
				logger.warn(" The level to analyse is missing. The default smell-instance was selected. ");
				LEVEL = "smell-instance";
			}
			else{
				LEVEL = line.getOptionValue(OPTION_LEVEL);
				if(LEVEL == null || (!LEVEL.equalsIgnoreCase("smell-instance") && !LEVEL.equalsIgnoreCase("smell-type") && !LEVEL.equalsIgnoreCase("package-group"))){
					logger.warn(" The level to analyse was not recognised. The default smell-instance was selected. ");
					LEVEL = "smell-instance";
				}
				else
					logger.info(" The level to analyse was set to "+LEVEL+" ");

			}

			if(!line.hasOption(OPTION_SOBOL) && !line.hasOption(OPTION_MORRIS)){
				logger.info(" No sensitivity analysis was selected. Default Sobol was selected ");
				SENSITIVITY = "sobol";
			}
			else{
				if(line.hasOption(OPTION_SOBOL))
					SENSITIVITY = "sobol";
				else
					if(line.hasOption(OPTION_MORRIS))
						SENSITIVITY = "morris";
			}

			performSensitivityAnalysis(evolution);

		} catch(Throwable exp ) {
			logger.error("Parsing failed. Reason: " + exp.getMessage());


		}
		finally{
			System.setSecurityManager(MySecurityManager.baseSecurityManager);
			System.exit(0);
		}
	}

	private static List<String> getVersionsInFolder(){
		File f = new File(INPUT_PATH);
		List<String> versionsInFolder = new ArrayList<>();

		for(File ff : f.listFiles())
			if(ff.isFile() && ff.getName().endsWith(indices_suffixes.get(INDEX)))
				versionsInFolder.add(ff.getName().replace(indices_suffixes.get(INDEX), ""));

		if(versionsInFolder.isEmpty()){
			logger.warn(" No systems to analyse in input folder. ");
			//			System.exit(0);
			return versionsInFolder;
		}

		Collections.sort(versionsInFolder,new NaturalOrderComparator());

		return versionsInFolder;
	}

	private static List<Version> buildVersions(List<String> selectedVersions) {

		List<Version> versions = new ArrayList<>();

		//		File f = new File(INPUT_PATH);
		//		for(File ff : f.listFiles()){
		//			if(ff.isFile() && ff.getName().endsWith(indices_suffixes.get(INDEX))){
		//				String name = ff.getName().replaceAll(indices_suffixes.get(INDEX), "");
		//				if(selectedVersions.contains(name))
		//					versions.add(FactoryVersion.createVersion(INDEX,ff.getAbsolutePath(),name));
		//			}
		//		}

		String suffix = indices_suffixes.get(INDEX);
		for(String sv : selectedVersions){
			String path = INPUT_PATH+File.separator+sv+suffix;
			if(new File(path).exists())
				versions.add(FactoryVersion.createVersion(INDEX,path,sv));
		}

		return versions;

	}

	private static void performSensitivityAnalysis(SmellEvolution evolution){

		String outputSA = InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"sa";

		new File(outputSA).mkdirs(); //creating the folder to store the files needed for the sensitivity analysis

		SmellFactory.setIndex(INDEX);
		SmellFactory.convertToArchitecturalSmells(evolution.filterSmells(),evolution.getVersions());

		Collection<SmellGroup> groups = null;

		switch(LEVEL){
		case "smell-instance":
			logger.info("Generating groups by individual smells ...");
			groups = SingleSmellGroup.generateGroups(SmellFactory.getSmells());
			logger.info("    Individual smell groups: "+groups.size());
			break;
		case "smell-type":
			logger.info("Generating groups by smell type ...");
			groups = SmellTypeGroup.generateGroups(SmellFactory.getSmells());
			logger.info("    Type smell groups: "+groups.size());
			break;
		case "package-group":
			logger.info("Generating groups by packages (with smells) ...");

			Map<String,List<String>> topLevelPackages = evolution.getTopLevelPackages();
			for(String v : topLevelPackages.keySet()){
//				System.out.println(v+" "+topLevelPackages.get(v).size()+" "+topLevelPackages.get(v));
				SmellFactory.setPackagesForVersion(v, topLevelPackages.get(v));
			}

			groups = PackageSmellGroup.generateGroups(SmellFactory.getSmells()); // For the packages in the latest version
			logger.info("	Package smell groups: "+groups.size());		

		}

		SAnalysis sa = SAnalysis.getSA(SENSITIVITY);
		if(sa.execute(groups, true)){

			List<String> rankedSmells = sa.generateElementRanking(InterfaceSensitivityAnalysis.INPUT_PATH + File.separator + "smellRanking__"+evolution.getFirstVersion()+"_"+evolution.getLastVersion()+"_"+LEVEL+".csv");

			if(rankedSmells != null){

				System.out.println();
				System.out.println("======================================");
				System.out.println();

				int max = 10;
				if(rankedSmells.size() < max)
					max = rankedSmells.size();


				System.out.println("Top "+max+" Ranked List of smells ");
				for(int i=0;i<max;i++){

					String name = null;

					if(LEVEL.equals("smell-instance"))
						name = rankedSmells.get(i)+" "+SmellFactory.findSmell(rankedSmells.get(i)).getDescription();
					else
						if(LEVEL.equals("package-group"))
							name = SmellFactory.findSmellGroup(rankedSmells.get(i)).getName();
						else
							name = rankedSmells.get(i);

					System.out.println((i+1)+". "+name);

				}

				System.out.println();
			}
		}

		deleteRecursive(new File(outputSA));

	}

	public static void deleteRecursive(File path){
		logger.info("Cleaning out folder:" + path.toString());
		for (File file : path.listFiles()){
			if (file.isDirectory()){
				logger.debug("Deleting folder:" + file.toString());
				deleteRecursive(file);
				try{
					Files.delete(file.toPath());
				}catch(IOException e){
					logger.error("Error deleting folder:" + file.toString()+" "+e.getMessage());
				}
			} else {
				logger.debug("Deleting file:" + file.toString());
				try{
					Files.delete(file.toPath());
				}catch(IOException e){
					logger.error("Error deleting file:" + file.toString()+" "+e.getMessage());
				}
			}
		}
		path.delete();
	}

}
