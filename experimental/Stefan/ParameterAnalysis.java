import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;


import de.zbit.io.csv.CSVReader;

/**
 * This class analyzes the values of the parameters for multiple optimization-runs
 * @author Stefan Fischer
 *
 */
public class ParameterAnalysis {
	
	/**
	 * Executes the parameter-analysis
	 * 
	 * @param models
	 * @param parameters
	 */
	public static void parameterAnalysis(List<Model> models, List<Parameter> parameters) {

		for(Parameter p: parameters) {
	
			double value = 0;
			int modelCounter = 0;
			double[] par = new double[models.size()];
			
			for(Model m: models) {
				
				par[modelCounter] = m.getParameter(p.getId()).getValue();
				modelCounter++;
			}
			//calculate mean, standard deviation and variation coefficient of the current parameter
			for(int i=0;i<par.length;i++){
				value += par[i];
			}
			double mean = value/modelCounter;
			double sum = 0;
			for(int i=0;i<par.length;i++){
				sum += (Math.pow((par[i]-mean), 2));	
			}
			double stddev = ((1/(double)models.size())*sum);
			double varCoeff = stddev/mean;
				
			System.out.println(p.getId());
			System.out.println("Mean: " + mean);
			System.out.println("Standarddeviation: " + stddev);
			System.out.println("Variation coefficient: " + varCoeff);
			System.out.println();
			
		}

	}

	public static void main(String args[]) throws XMLStreamException, IOException {
		File f = new File(args[0]);

		File[] modelFiles = f.listFiles();
		List<Model> models = new LinkedList<Model>();
		for(File modelFile: modelFiles) {
			SBMLDocument doc = SBMLReader.read(modelFile);
			models.add(doc.getModel());
		}
		String parameterFilePath = args[1];
		CSVReader reader = new CSVReader(parameterFilePath);
		String[][] data = reader.getData();

		List<Parameter> parameters = new LinkedList<Parameter>();
		for(int row=0; row != data.length; row++) {
			parameters.add(new Parameter(data[row][0]));
		}

		parameterAnalysis(models, parameters);
	}
}