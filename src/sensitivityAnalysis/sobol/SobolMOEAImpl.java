package sensitivityAnalysis.sobol;


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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.moeaframework.analysis.sensitivity.SampleGenerator;
import org.moeaframework.analysis.sensitivity.SobolAnalysis;

import sensitivityAnalysis.InterfaceSensitivityAnalysis;
import smellhistory.SmellFactory;
import smellhistory.smell.ArchSmell;

//http://waterprogramming.wordpress.com/2012/08/13/running-sobol-sensitivity-analysis-using-moeaframework/
//http://waterprogramming.wordpress.com/2012/01/18/running-sobol-sensitivity-analysis/	

//https://waterprogramming.wordpress.com/category/sensitivity-analysis/

public class SobolMOEAImpl extends DefaultSA {

	static final Logger logger = LogManager.getLogger(SobolMOEAImpl.class);

	final SampleGenerator generator = new SampleGenerator();

	static final CommandLineParser parser = new DefaultParser();

	static Options optionsSampling = new Options();
	static Options optionsAnalysis = new Options();

	static String[] argsSampling = null;
	static String[] argsAnalysis = null;

	public SobolMOEAImpl() {
		super();
		this.initialize();
	}

	protected void initialize() {

		optionsSampling = optionsSampling.addOption("m", "method", true, "");
		optionsSampling = optionsSampling.addOption("p", "parameterFile", true, "");
		optionsSampling = optionsSampling.addOption("n", "numberOfSamples", true, "");
		optionsSampling = optionsSampling.addOption("o", "output", true, "");	    

		optionsAnalysis = optionsAnalysis.addOption("i", "input", true, "calculated objective files");
		optionsAnalysis = optionsAnalysis.addOption("p", "parameterFile", true, "");
		optionsAnalysis = optionsAnalysis.addOption("m", "metric", true, "");
		optionsAnalysis = optionsAnalysis.addOption("o", "output", true, "");
		optionsAnalysis = optionsAnalysis.addOption("r", "resamples", true, "");

		argsAnalysis = new String[]{
				"-p", InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"sa"  + File.separator + "parameters.txt", 
				"-r", "1000", 
				"-m", "0", 
				//	    		"-m", "1", 
				"-i", InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"sa" + File.separator + "objectiveValues.txt", 
				"-o", InterfaceSensitivityAnalysis.INPUT_PATH + File.separator+"sa"+ File.separator + "sobolIndices.txt"};

		argsSampling = new String[] {
				"-m", "saltelli", 
				"-n", Integer.toString(InterfaceSensitivityAnalysis.SALTELLI_SAMPLES), 
				"-p", InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"sa"  + File.separator + "parameters.txt", 
				"-o", InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"sa"+ File.separator + "parameterValues.txt"};

		return;
	}

	protected void doSampling() {

		CommandLine cmd = null;
		try {

			logger.info("2. Sampling parameters (MOEA)...");

			cmd = parser.parse(optionsSampling, argsSampling);

			String file1 = cmd.getOptionValue("parameterFile");
			logger.info("\tinput file-->  "+ file1);

			String file2 = cmd.getOptionValue("output");
			
			logger.info("\toutput file-->  "+ file2);

			generator.run(cmd);

		} catch (ParseException | IOException e) {
			logger.error("Error while sampling: "+e.getMessage());
		}

		return;
	}

	protected void doAnalysis() {

		CommandLine cmd = null;
		try {

			logger.info("4. Running Sobol (MOEA) ...");

			cmd = parser.parse(optionsAnalysis, argsAnalysis);			

			String file1 = cmd.getOptionValue("parameterFile");

			logger.info("\tparameters file-->  "+ file1);

			String file2 = cmd.getOptionValue("input");

			logger.info("\tinput file-->  "+ file2);

			String file3 = cmd.getOptionValue("output");

			logger.info("\toutput file-->  "+ file3);

			SobolAnalysis sobol = new SobolAnalysis();
			sobol.run(cmd);	
			
			logger.info("== Done!");

		} catch (Exception e) {
			logger.error("Error while running sensitivity analysis: "+e.getMessage());
		}
		return;
	}

	public List<String> generateElementRanking(String filename) {

		logger.info("Generating ranking of elements (Sobol)...");

		Map<String, double[]> firstOrderEffects = new HashMap<>();
		Map<String, double[]> totalOrderEffects = new HashMap<>();

		// Open the file
		FileInputStream fstream;
		
		try {
			fstream = new FileInputStream(InterfaceSensitivityAnalysis.INPUT_PATH + File.separator+"sa"+ File.separator + "sobolIndices.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

			String strLine;
			String[] tokens = null;
			//Read File Line By Line
			strLine = br.readLine(); // This removes the header

//			String smellId = null; 
//			double smellScore;

			double[] values = null;
			boolean parseFirstOrder = false;
			boolean parseTotalOrder = false;
			while ((strLine = br.readLine()) != null)   {
				tokens = strLine.split("\\s+");

				// Very ad-hoc parsing, should be revised to make it more robust!
				if (strLine.contains("First-Order")) {
					parseFirstOrder = true;
				}
				else if (strLine.contains("Total-Order")) {
					parseFirstOrder = false;
					parseTotalOrder = true;
				} 
				else if (tokens[0].length() > 1) {
					parseFirstOrder = false;
					parseTotalOrder = false;
				}
				else if (parseFirstOrder) {
					values = new double[2];
					values[0] = Double.parseDouble(tokens[2]);
					values[1] = Double.parseDouble(tokens[3].substring(1, tokens[3].length()-1));
					firstOrderEffects.put(tokens[1], values);
				}
				else if (parseTotalOrder) {
					values = new double[2];
					values[0] = Double.parseDouble(tokens[2]);
					values[1] = Double.parseDouble(tokens[3].substring(1, tokens[3].length()-1));
					totalOrderEffects.put(tokens[1], values);
				}

			}


			fstream.close();			

		} catch (IOException e) {
			logger.error("Error reading smell indeces: "+e.getMessage());
		}

		if(totalOrderEffects.isEmpty())
			return null;
		
		List<String> rankedSmells = new ArrayList<>(totalOrderEffects.keySet());
		
		Collections.sort(rankedSmells,new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				double value2 = totalOrderEffects.get(o2)[0];
				double value1 = totalOrderEffects.get(o1)[0];
				
				if(value1 > value2)
					return -1;
				if(value1 < value2)
					return 1;
				return 0;
				
			}
		});
		
		BufferedWriter out;
		try {
			double[] values = null;
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename,false),"UTF8"));
			out.write("element"+InterfaceSensitivityAnalysis.STRING_SEPARATOR+"description"+InterfaceSensitivityAnalysis.STRING_SEPARATOR+"firstOrder"
					+InterfaceSensitivityAnalysis.STRING_SEPARATOR+"confidenceFirstOrder"
					+InterfaceSensitivityAnalysis.STRING_SEPARATOR+"totalOrder"
					+InterfaceSensitivityAnalysis.STRING_SEPARATOR+"confidenceTotalOrder");
			out.newLine();
			ArchSmell s = null;
			String description = null;
			for (String fo: rankedSmells) {
//			for (String fo: firstOrderEffects.keySet()) {
				s = SmellFactory.findSmell(fo);
				if (s != null)
					description = s.getDescription();
				else
					description = null;
				out.write(fo + InterfaceSensitivityAnalysis.STRING_SEPARATOR + description + InterfaceSensitivityAnalysis.STRING_SEPARATOR);

				values = firstOrderEffects.get(fo);
				out.write(values[0] + InterfaceSensitivityAnalysis.STRING_SEPARATOR + values[1] + InterfaceSensitivityAnalysis.STRING_SEPARATOR);

				values = totalOrderEffects.get(fo);
				out.write(values[0] + InterfaceSensitivityAnalysis.STRING_SEPARATOR + values[1]);

				out.newLine();
			}

			out.close();
		} catch (IOException e) {
			logger.error("Error writing smell ranking: "+e.getMessage());
		}

		return rankedSmells;
	}

}
