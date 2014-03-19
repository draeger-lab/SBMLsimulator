import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Symbol;

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
	public static double[][] parameterAnalysis(Model originalModel, Map<Model, Double> modelMap, List<Symbol> parameters, Map<Integer,List<Model>> numbers) {
		double[][] result = new double[parameters.size()][];
		
		double percentageToConsider = 0.5;
		int repetitions = 10;
		int numberToConsider = (int) (repetitions * percentageToConsider);
		
		List<Model> models = new LinkedList<Model>();
		Map<Symbol, Double> maxStdDevMap = new HashMap<Symbol,Double>();
		Map<Symbol, Double> maxMedianDevMap = new HashMap<Symbol,Double>();
		
		for(int experimentNumber: numbers.keySet()) {
			List<Model>  modelsForExperiment = numbers.get(experimentNumber);
			List<Model>  modelsForExperimentAdded = new LinkedList<Model>();
			double[] fitnesses = new double[modelsForExperiment.size()];
					
			for(int i=0; i!=modelsForExperiment.size(); i++) {
				fitnesses[i] = modelMap.get(modelsForExperiment.get(i));
			}
			Arrays.sort(fitnesses);
			double limit = fitnesses[Math.min(numberToConsider-1, modelsForExperiment.size()-1)];
			if(modelsForExperiment.size() != 10) {
				System.out.println();
			}
			
			int added = 0;
			int modelCounter = 0;
			for(Model m: modelsForExperiment) {
				if(modelMap.get(m) < limit) {
					models.add(m);
					modelsForExperimentAdded.add(m);
					added++;
				}
				modelCounter++;
			}
			modelCounter = 0;
			for(Model m: models) {
				if(modelMap.get(m) == limit) {
					models.add(m);
					modelsForExperimentAdded.add(m);
					added++; 
					if(added >= numberToConsider) {
						break;
					}
				}
				modelCounter++;
			}
			
			
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
				
				
				
				
				double[] par = new double[modelsForExperimentAdded.size()];
				modelCounter = 0;
				for(Model m: modelsForExperimentAdded) {
					Parameter p2 = m.getParameter(s.getId());
					if(p2 != null) {
						par[modelCounter] = p2.getValue();
					}
					else {
							par[modelCounter] = m.getSpecies(s.getId()).getValue();
					}
					modelCounter++;
				}
				
			double median = MathUtils.median(par);
			double stddev = MathUtils.standardDeviation(par);
			double varCoeff = (stddev/median)*100;
			if(Double.isInfinite(varCoeff)) {
				varCoeff = stddev * 100;
			}
			if(Double.isNaN(varCoeff)) {
				varCoeff = 0d;
			}
			
				System.out.println(experimentNumber);
				System.out.println("Parameter: " + s.getId());
				for(double parameter: par) {
					//System.out.println(parameter);
				}
				System.out.println("Value in original model: " + originalValue);
				System.out.println("Mean: " + median);
				System.out.println("Standarddeviation: " + stddev);
				System.out.println("Variation coefficient: " + varCoeff + "%");
				System.out.println("Median deviation: " + (Math.abs(median - originalValue)/median)*100);
				System.out.println();
				
				Double currentStdDevMaxValue = maxStdDevMap.get(s);
				if(currentStdDevMaxValue != null) {
					currentStdDevMaxValue = Math.max(currentStdDevMaxValue, varCoeff);
				}
				else {
					currentStdDevMaxValue =varCoeff;
				}
				maxStdDevMap.put(s, currentStdDevMaxValue);
				
				
				
				Double currentMedianDevMaxValue = maxMedianDevMap.get(s);
				if(currentMedianDevMaxValue != null) {
					if(median != 0) {
						currentMedianDevMaxValue = Math.max(currentMedianDevMaxValue, Math.abs((median - originalValue)/median)*100);
					}
					else {
						currentMedianDevMaxValue = Math.max(currentMedianDevMaxValue, 100d);
					}
				}
				else {
					if(median != 0) {
						currentMedianDevMaxValue = Math.abs((median - originalValue)/median)*100;
					}
					else {
						currentMedianDevMaxValue = 100d;
					}
				}
				maxMedianDevMap.put(s, currentMedianDevMaxValue);
				
			}
		}
		
		
		
		
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
			
			
			
			
			double[] par = new double[models.size()];
			
			int modelCounter = 0;
			for(Model m: models) {
				Parameter p2 = m.getParameter(s.getId());
				if(p2 != null) {
					par[modelCounter] = p2.getValue();
				}
				else {
						par[modelCounter] = m.getSpecies(s.getId()).getValue();
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
//			System.out.println("Parameter: " + s.getId());
			for(double parameter: par) {
				//System.out.println(parameter);
			}
//			System.out.println("Value in original model: " + originalValue);
//			System.out.println("Mean: " + median);
//			System.out.println("Standarddeviation: " + stddev);
//			System.out.println("Variation coefficient: " + varCoeff + "%");
//			System.out.println();
//			result[index][0] = median;
//			result[index][1] = varCoeff;
			
			
			result[index][0] = maxMedianDevMap.get(s);
			result[index][1] = maxStdDevMap.get(s);
			
			index++; 
		}
		return result;

	}

	public static void main(String args[]) throws XMLStreamException, IOException {
		
		Model m = (new SBMLReader()).readSBML(args[0]).getModel();
        
		File f = new File(args[1]);

		File[] modelFiles = f.listFiles();
		Map<Model,Double> modelMap = new HashMap<Model,Double>();
		Map<Integer,List<Model>> numberMap = new HashMap<Integer,List<Model>>();
		
		for (File modelFile : modelFiles) {
			if (SBFileFilter.isSBMLFile(modelFile)) {
				SBMLDocument doc = SBMLReader.read(modelFile);
				double fitness = Double.parseDouble(modelFile.getAbsolutePath()
						.replaceAll(".*Fitness_", "").replace(".xml", ""));
				int number = Integer.parseInt(modelFile.getAbsolutePath()
					.replaceAll(".*Experiment_", "").replace("1_Rep.*", ""));
				modelMap.put(doc.getModel(), fitness);
				List<Model> models = numberMap.get(number);
				if(models == null) {
					models = new LinkedList<Model>();
				}
				models.add(doc.getModel());
				numberMap.put(number, models);
				
			}
		}
		String parameterFilePath = args[2];
		CSVReader reader = new CSVReader(parameterFilePath);
		String[][] data = reader.getData();

		List<Symbol> parameters = new LinkedList<Symbol>();
		for(int row=0; row != data.length; row++) {
			parameters.add(new Parameter(data[row][0]));
		}

		parameterAnalysis(m, modelMap, parameters, numberMap);
		
		
	}
	
}
