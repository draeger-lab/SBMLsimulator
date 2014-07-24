import java.io.File;
import java.io.FileInputStream;
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
import org.sbml.jsbml.util.SimpleTreeNodeChangeListener;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.sbml.jsbml.Symbol;

import de.zbit.io.csv.CSVReader;
import de.zbit.io.csv.CSVWriter;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.math.MathUtils;

public class ParameterSummary {
	
	public static void main(String args[]) throws XMLStreamException, IOException {
		SBMLReader sbmlReader = new SBMLReader();
		Model m = sbmlReader.readSBML(args[0]).getModel();
		
		String parameterFilePath = args[2];
		CSVReader reader = new CSVReader(parameterFilePath);
		String[][] data = reader.getData();
		List<Symbol> parameters = new LinkedList<Symbol>();
		for (int row = 0; row != data.length; row++) {
			Parameter p = new Parameter(data[row][0]);
			parameters.add(p);
		}
		boolean replicates = true;
		
		int[] precisionValues = new int[] {5};
		int[] systematicValues = new int[] {0};
		int[] baselineValues = new int[] {0};
		
		for (int i = 0; i != precisionValues.length; i++) {
			for (int j = 0; j != systematicValues.length; j++) {
				for (int k = 0; k != baselineValues.length; k++) {
					Map<String, double[]> parameterValues = new HashMap<String, double[]>();
					
					
					int precision = precisionValues[i];
					int systematicError = systematicValues[j];
					int baselineError = baselineValues[k];
					File folder = new File(args[1] + "/Errors_" + precision + "_"
							+ systematicError + "_" + baselineError);
					File[] modelFiles = folder.listFiles();
					
					if (modelFiles == null) {
						System.out.println();
					}
					
					Map<Model, Double> modelMap = new HashMap<Model, Double>();
					Map<Integer, List<Model>> numberMap = new HashMap<Integer, List<Model>>();
					File modelFile;
					for (int n = 0; n != modelFiles.length; n++) {
						modelFile = modelFiles[n];
						if (SBFileFilter.isSBMLFile(modelFile)) {
							FileInputStream stream = new FileInputStream(modelFile);
							SBMLDocument doc = new SBMLReader().readSBMLFromStream(stream,
								new SimpleTreeNodeChangeListener());
							stream.close();
							double fitness = Double.parseDouble(modelFile.getAbsolutePath()
									.replaceAll(".*Fitness_", "").replace(".xml", ""));
							int number = Integer.parseInt(modelFile.getAbsolutePath()
									.replaceAll(".*Exp_", "").replaceAll("_1_Rep_.*", "")
									.replaceAll(".*_", ""));
							if (replicates && (modelFile.getAbsolutePath().contains("["))) {
								modelMap.put(doc.getModel(), fitness);
								List<Model> models = numberMap.get(number);
								if (models == null) {
									models = new LinkedList<Model>();
								}
								models.add(doc.getModel());
								numberMap.put(number, models);
							} else if (!replicates
									&& !(modelFile.getAbsolutePath().contains("["))) {
								modelMap.put(doc.getModel(), fitness);
								List<Model> models = numberMap.get(number);
								if (models == null) {
									models = new LinkedList<Model>();
								}
								models.add(doc.getModel());
								numberMap.put(number, models);
							}
							
						}
					}
					System.out.println(precision + " " + systematicError + " "
							+ baselineError);
					parameterSummary(m, modelMap, parameters, numberMap, parameterValues);
					Object[][] objectsResult = new Object[parameterValues.get(parameters
							.get(0).getId()).length + 1][parameters.size()];
					
					for (int parameterNum = 0; parameterNum != parameters.size(); parameterNum++) {
						objectsResult[0][parameterNum] = new String(parameters.get(parameterNum).getId());
						for (int rep = 0; rep != parameterValues.get(parameters.get(0).getId()).length; rep++) {
							double[] values = parameterValues.get(parameters
									.get(parameterNum).getId());
							objectsResult[rep + 1][parameterNum] = new Double(values[rep]);
						}
					}
					
					CSVWriter writer = new CSVWriter();
					if (replicates) {
						writer.write(objectsResult, new File(args[1] + "/values_"
								+ precision + "_" + systematicError + "_" + baselineError
								+ "_rep.csv"));
					} else {
						writer.write(objectsResult, new File(args[1] + "/values_"
								+ precision + "_" + systematicError + "_" + baselineError
								+ ".csv"));
					}
					
				}
			}
		}
		
	}
	
	private static void parameterSummary(Model originalModel,
		Map<Model, Double> modelMap, List<Symbol> parameters,
		Map<Integer, List<Model>> numbers, Map<String, double[]> parameterValues) {
		
		double percentageToConsider = 0.5;
		int repetitions = 20;
		int numberToConsider = (int) (repetitions * percentageToConsider);
		
		List<Model> models = new LinkedList<Model>();
		
		for (int experimentNumber : numbers.keySet()) {
			List<Model> modelsForExperiment = numbers.get(experimentNumber);
			List<Model> modelsForExperimentAdded = new LinkedList<Model>();
			double[] fitnesses = new double[modelsForExperiment.size()];
			
			for (int i = 0; i != modelsForExperiment.size(); i++) {
				fitnesses[i] = modelMap.get(modelsForExperiment.get(i));
			}
			Arrays.sort(fitnesses);
			double limit = fitnesses[Math.min(numberToConsider - 1,
				modelsForExperiment.size() - 1)];
			if (modelsForExperiment.size() != 10) {
				System.out.println();
			}
			
			int added = 0;
			int modelCounter = 0;
			for (Model m : modelsForExperiment) {
				if (modelMap.get(m) < limit) {
					models.add(m);
					modelsForExperimentAdded.add(m);
					added++;
				}
				modelCounter++;
			}
			modelCounter = 0;
			for (Model m : models) {
				if (modelMap.get(m) == limit) {
					models.add(m);
					modelsForExperimentAdded.add(m);
					added++;
					if (added >= numberToConsider) {
						break;
					}
				}
				modelCounter++;
			}
			
			for (Symbol s : parameters) {
				
				// the value of the parameter in the original model without errors
				double originalValue = Double.NaN;
				Parameter p = originalModel.getParameter(s.getId());
				if (p != null) {
					originalValue = p.getValue();
				} else {
					originalValue = originalModel.getSpecies(s.getId()).getValue();
				}
				
				double[] par = new double[modelsForExperimentAdded.size()];
				modelCounter = 0;
				for (Model m : modelsForExperimentAdded) {
					Parameter p2 = m.getParameter(s.getId());
					if (p2 != null) {
						par[modelCounter] = p2.getValue();
					} else {
						par[modelCounter] = m.getSpecies(s.getId()).getValue();
					}
					modelCounter++;
				}
				
				double[] values = parameterValues.get(s.getId());
				
				if (values == null) {
					values = new double[numbers.keySet().size()];
				}
				parameterValues.put(s.getId(), values);
				double median = MathUtils.median(par);
				values[experimentNumber - 1] = median;
				
				double stddev = MathUtils.standardDeviation(par);
				double varCoeff = (stddev / median) * 100;
				if (Double.isInfinite(varCoeff)) {
					varCoeff = stddev * 100;
				}
				if (Double.isNaN(varCoeff)) {
					varCoeff = 0d;
				}
			}
		}
	}
	
}
