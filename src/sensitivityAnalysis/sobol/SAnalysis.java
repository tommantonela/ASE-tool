package sensitivityAnalysis.sobol;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.moeaframework.analysis.sensitivity.SampleReader;

import sensitivityAnalysis.InterfaceSensitivityAnalysis;
import sensitivityAnalysis.index.SmellGroup;

//http://waterprogramming.wordpress.com/2012/08/13/running-sobol-sensitivity-analysis-using-moeaframework/
//http://waterprogramming.wordpress.com/2012/01/18/running-sobol-sensitivity-analysis/	

//https://waterprogramming.wordpress.com/category/sensitivity-analysis/

public abstract class SAnalysis {
	
	static final Logger logger = LogManager.getLogger(SAnalysis.class);
	
	public static SAnalysis getSA(String sensitivity) {
		//return (new MorrisSALibImpl());
		//return (new SobolSALibImpl());
		
		switch(sensitivity){
		case "sobol":
			return new SobolMOEAImpl();
		default :
			return new SobolMOEAImpl();
		}
	
	}

	abstract public void execute(Collection<SmellGroup> groups, boolean parameters);
	
	protected void generateParametersFile(Collection<SmellGroup> elements) {
		
		logger.info("1. Generating parameters ("+ elements.size() +") and ranges ... ");
		logger.info("\toutput file-->  "+ InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"sa"  + File.separator + "parameters.txt");
		
		BufferedWriter bw = null;	
		int skipped = 0;
		try {
			
	        //Specify the file name and path here
			File file = new File(InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"sa"  + File.separator + "parameters.txt");
			// This logic will make sure that the file gets created if it is not present at the specified location
			if (!file.exists()) 
				file.createNewFile();
			
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file,false),"UTF8"));
			
			double minIndex, maxIndex;
			String entry = null;
			for (SmellGroup g: elements) {
						
				minIndex = g.getMinForIndex() * InterfaceSensitivityAnalysis.SA_SCALING_FACTOR;
			
				maxIndex = g.getMaxForIndex() * InterfaceSensitivityAnalysis.SA_SCALING_FACTOR;
			
				entry = g.getName() + " " + minIndex +" "+ maxIndex;

				logger.debug("\trange for " + entry);
				
				if (minIndex >= maxIndex) {
					//logger.warn("==> Warning: parameter range error, parameter not written ");
					skipped++;
				}
				else{
					bw.write(entry);
					bw.newLine();
				}
					
			}
	    } 
		catch (IOException ioe) {
			logger.error("Error writing the parameters file: "+ ioe.getMessage());
		}
		finally { 
			try {
		    	if (bw != null)
		    		bw.close();
			}
			catch(Exception ex) {
				logger.error("Error in closing the parameters file: "+ ex.getMessage());
			}
		}

		logger.debug("\tskipped: "+skipped);
		
	}

	protected void runModel(SampleReader reader /*,long nSamples*/) {
		
		BufferedWriter bw = null;	
		try {
			
			logger.info("3. Reading (and evaluating) sampled parameters ...");	
			logger.info("\tinput file-->  "+ InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"sa" + File.separator + "parameterValues.txt");
			logger.info("\toutput file-->  "+ InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"sa"  + File.separator + "objectiveValues.txt");
			
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(InterfaceSensitivityAnalysis.INPUT_PATH+File.separator+"sa"  + File.separator + "objectiveValues.txt",false),"UTF8"));
			
			Iterator<Properties> it = reader.iterator();
			Properties p = null;
			double index, result;
			String pName;
//			int i = 0;
			while (it.hasNext()) {
				p = it.next(); // This is a list of parameters to input to the model
				Enumeration<?> itNames = p.propertyNames();
				result = 0;
				pName = null;
				while (itNames.hasMoreElements()) {
					pName = (String)(itNames.nextElement());
					//System.out.println(pName+"  "+Math.round(Double.parseDouble(p.getProperty(pName))));
					result = result +  Math.round(Double.parseDouble(p.getProperty(pName)));
				}				
//				i++;
//				System.out.println("\tinstance: "+i+" (out of "+nSamples+")");
				index = result / InterfaceSensitivityAnalysis.SA_SCALING_FACTOR;
				//System.out.println("\tobjective " + ADI);		
				bw.write(Double.toString(index));
				bw.newLine();
			}	

	    } 
		catch (IOException ioe) {
			logger.error("  Error in closing the BufferedWriter"+ ioe.getMessage()+"  ");
		}
		finally
		{ 
			try {
		    	if (bw != null)
		    		bw.close();
			}
			catch(Exception ex) {
				logger.error("  Error in closing the BufferedWriter"+ ex.getMessage()+"  ");
			}
		}
	
		logger.info("  End of model runs ");
		
		return;
	}
	
	public abstract List<String> generateElementRanking(String filename);
	
}
