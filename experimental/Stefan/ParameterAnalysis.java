import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Symbol;
import org.sbml.jsbml.util.Maths;


import de.zbit.io.csv.CSVReader;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.math.MathUtils;

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
	public static double[][] parameterAnalysis(Model originalModel, List<Model> models, List<Double> fitnesses, List<Symbol> parameters) {
		double[][] result = new double[parameters.size()][];
		int index = 0;
		for(Symbol s: parameters) {
			
			
			// the value of the parameter in the original model without errors
			double originalValue = Double.NaN;
			Parameter p = originalModel.getParameter(s.getId());
			if(p != null) {
				originalValue = p.getValue();
			}
			else {
				originalValue = originalModel.getSpecies(s.getId()).getValue();
			}
			result[index] = new double[2];
			
			double[] fitnessClone = new double[fitnesses.size()];
			int counter = 0;
			for(double fitness: fitnesses) {
				fitnessClone[counter] = fitness;
				counter++;
			}
			
			Arrays.sort(fitnessClone);
			
			int numberToConsider = Math.min(fitnesses.size(), 10);
			double limit = fitnessClone[numberToConsider-1];
			double[] par = new double[numberToConsider];
			
			int modelCounter = 0;
			int arrayIndex = 0;
			for(Model m: models) {
				if(fitnesses.get(modelCounter) < limit) {
					Parameter p2 = m.getParameter(s.getId());
					if(p2 != null) {
						par[arrayIndex] = p2.getValue();
					}
					else {
						par[arrayIndex] = m.getSpecies(s.getId()).getValue();
					}
					arrayIndex++;
				}
				modelCounter++;
			}
			modelCounter = 0;
			for(Model m: models) {
				if(fitnesses.get(modelCounter) == limit) {
					Parameter p2 = m.getParameter(s.getId());
					if(p2 != null) {
						par[arrayIndex] = p2.getValue();
					}
					else {
						par[arrayIndex] = originalModel.getSpecies(s.getId()).getValue();
					}
					arrayIndex++; 
					if(arrayIndex >= numberToConsider) {
						break;
					}
				}
				modelCounter++;
			}
		double median = MathUtils.median(par);
			double stddev = MathUtils.standardDeviation(par);
			double varCoeff = (stddev/median)*100;
			if(Double.isInfinite(varCoeff)) {
				varCoeff = (stddev/MathUtils.mean(par));
			}
			double value = 0;
			// calculate mean, standard deviation and variation coefficient of the current parameter
			for(int i=0;i<par.length;i++) {
				value += par[i];
			}
			double mean = value/par.length;
			double sum = 0;
			for(int i=0;i<par.length;i++) {
				sum += ((par[i] - mean)*(par[i] - mean));
			}
			
			System.out.println();
			System.out.println("Parameter: " + s.getId());
			for(double parameter: par) {
				//System.out.println(parameter);
			}
			System.out.println("Value in original model: " + originalValue);
			System.out.println("Mean: " + median);
			System.out.println("Standarddeviation: " + stddev);
			System.out.println("Variation coefficient: " + varCoeff + "%");
			System.out.println();
			result[index][0] = median;
			result[index][1] = varCoeff;
			
			index++; 
		}
		return result;

	}

	public static void main(String args[]) throws XMLStreamException, IOException {
		
		Model m = (new SBMLReader()).readSBML(args[0]).getModel();
        
		File f = new File(args[1]);

		File[] modelFiles = f.listFiles();
		List<Model> models = new LinkedList<Model>();
		List<Double> fitnesses = new LinkedList<Double>();
		for(File modelFile: modelFiles) {
			if(SBFileFilter.isSBMLFile(modelFile)) {
				SBMLDocument doc = SBMLReader.read(modelFile);
				models.add(doc.getModel());
				fitnesses.add(Double.parseDouble(modelFile.getAbsolutePath().replaceAll(".*Fitness_", "").replace(".xml", "")));
			}
		}
		String parameterFilePath = args[2];
		CSVReader reader = new CSVReader(parameterFilePath);
		String[][] data = reader.getData();

		List<Symbol> parameters = new LinkedList<Symbol>();
		for(int row=0; row != data.length; row++) {
			parameters.add(new Parameter(data[row][0]));
		}

		parameterAnalysis(m, models, fitnesses, parameters);
		
		
	}
	
}
