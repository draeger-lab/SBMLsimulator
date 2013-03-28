import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Symbol;


import de.zbit.io.csv.CSVReader;
import de.zbit.io.filefilter.SBFileFilter;

/**
 * This class analyzes the values of the parameters for multiple optimization runs
 * @author Stef
 *
 */
public class ParameterAnalysis {
	
	/**
	 * Executes the parameter analysis
	 * 
	 * @param models
	 * @param parameters
	 */
	public static void parameterAnalysis(Model originalModel, List<Model> models, List<Symbol> parameters) {

		for(Symbol s: parameters) {
	
			double value = 0;
			int modelCounter = 0;
			double[] par = new double[models.size()];
			
			// the value of the parameter in the original model without errors
			double originalValue = Double.NaN;
			Parameter p = originalModel.getParameter(s.getId());
			if(p != null) {
				originalValue = p.getValue();
			}
			else {
				originalValue = originalModel.getSpecies(s.getId()).getValue();
			}
			
			
			for(Model m: models) {
				
				Parameter p2 = m.getParameter(s.getId());
				if(p2 != null) {
					par[modelCounter] = p2.getValue();
				}
				else {
					par[modelCounter] = originalModel.getSpecies(s.getId()).getValue();
				}
				modelCounter++;
			}
			// calculate mean, standard deviation and variation coefficient of the current parameter
			for(int i=0;i<par.length;i++){
				value += par[i];
			}
			double mean = value/par.length;
			double sum = 0;
			for(int i=0;i<par.length;i++){
				sum += ((par[i] - mean)*(par[i] - mean));
			}
			double stddev = Math.sqrt((sum/(par.length - 1.0)));
			double varCoeff = (stddev/mean)*100;
				
			System.out.println();
			System.out.println("Parameter: " + s.getId());
			System.out.println();
			System.out.println("Value in original model: " + originalValue);
			System.out.println("Mean: " + mean);
			System.out.println("Standarddeviation: " + stddev);
			System.out.println("Variation coefficient: " + varCoeff + "%");
			System.out.println();
			
		}

	}

	public static void main(String args[]) throws XMLStreamException, IOException {
		
		Model m = (new SBMLReader()).readSBML(args[0]).getModel();
        
		File f = new File(args[1]);

		File[] modelFiles = f.listFiles();
		List<Model> models = new LinkedList<Model>();
		for(File modelFile: modelFiles) {
			if(SBFileFilter.isSBMLFile(modelFile)) {
				SBMLDocument doc = SBMLReader.read(modelFile);
				models.add(doc.getModel());
			}
		}
		String parameterFilePath = args[2];
		CSVReader reader = new CSVReader(parameterFilePath);
		String[][] data = reader.getData();

		List<Symbol> parameters = new LinkedList<Symbol>();
		for(int row=0; row != data.length; row++) {
			parameters.add(new Parameter(data[row][0]));
		}

		parameterAnalysis(m, models, parameters);
	}
	
}