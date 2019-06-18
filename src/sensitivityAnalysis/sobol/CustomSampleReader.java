package sensitivityAnalysis.sobol;

import java.io.Reader;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.moeaframework.analysis.sensitivity.Parameter;
import org.moeaframework.analysis.sensitivity.ParameterFile;
import org.moeaframework.analysis.sensitivity.SampleReader;


public class CustomSampleReader extends SampleReader {
	
	static final Logger logger = LogManager.getLogger(CustomSampleReader.class);
	
	private ParameterFile myParameterFile = null;
	private MatrixReader myReader = null;
	
	public CustomSampleReader(Reader reader, ParameterFile parameterFile) {
		super(reader, parameterFile);
		myReader = new MatrixReader(reader, parameterFile.size());
		myParameterFile = parameterFile;
	}
	
	@Override
	public boolean hasNext() {
		return myReader.hasNext();
	}
	
	@Override
	public Properties next() {
		double[] values = myReader.next();
		Properties parameters = new Properties();

		for (int i = 0; i < values.length; i++) {
			Parameter parameter = myParameterFile.get(i);

			if ((values[i] < parameter.getLowerBound()) || (values[i] > parameter.getUpperBound())) {
				//throw new FrameworkException("parameter out of bounds");	
			}

			parameters.setProperty(myParameterFile.get(i).getName(),
					Double.toString(values[i]));
		}
		
		return parameters;
	}

}
