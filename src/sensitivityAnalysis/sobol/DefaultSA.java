package sensitivityAnalysis.sobol;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.moeaframework.analysis.sensitivity.ParameterFile;
import org.moeaframework.analysis.sensitivity.SampleReader;

import sensitivityAnalysis.InterfaceSensitivityAnalysis;
import sensitivityAnalysis.index.SmellGroup;

public abstract class DefaultSA extends SAnalysis {

	static final Logger logger = LogManager.getLogger(DefaultSA.class);

	public static void generateIndicesFile(String myFile, String[] contents) {

		try {	
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(myFile, false), "UTF8"));

			for (int i = 0; i < contents.length; i++) {
				writer.write(contents[i]);
				writer.newLine();
			}

			writer.close();

		} catch (IOException e1) {
			logger.error("Error writing Indeces file: "+e1.getMessage());
		}

		return;
	}

	public DefaultSA() {
		super();
	}

	public boolean execute(Collection<SmellGroup> groups, boolean parameters) {

		TimeWatch watch = TimeWatch.start();

		logger.info("SENSITIVITY ANALYSIS - default");
		logger.info("===============================");

		if (parameters) 		
			this.generateParametersFile(groups);

		try {
			File outputFile = new File(InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"sa"+ File.separator + "parameterValues.txt");

			outputFile.createNewFile();

			FileReader fileReader = new FileReader(outputFile);

			ParameterFile parameterFile = new ParameterFile(new File(InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"sa" + File.separator + "parameters.txt"));

			SampleReader reader = new CustomSampleReader(fileReader, parameterFile);

			if(this.doSampling()){ // Sobol or Morris implementation
				//long nSamples = generator.getNSamples(); 

				if(reader.hasNext()){
					//this.fitSamplesToDistribution(reader); // Experimental feature -- so far, normal distribution
					this.runModel(reader/*, nSamples*/);	
					this.doAnalysis(); // Sobol or Morris implementation
				}
				else{
					logger.info("No parameters found. Cannot run sensitivity analysis.");
					reader.close();	
					return false;
				}
				
				long passedTimeInSeconds = watch.time(TimeUnit.SECONDS);
				logger.info("Elapsed time: "+passedTimeInSeconds+" seconds.");
				return true;
			}
			else{
				logger.info("No sampling can be done.");
				reader.close();	
				return false;
			}
		

		} catch (IOException e) {
			logger.error("Error while executing sensitivity analysis: "+e.getMessage());
			return false;
		}
	}

	protected abstract boolean doSampling();

	protected abstract void doAnalysis();

}

