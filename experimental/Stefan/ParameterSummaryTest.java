import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

public class ParameterSummaryTest {
	
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
		
		int[] precisionValues = new int[] {0};
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
					
					Map<Model, Double> modelMap = new HashMap<Model, Double>();
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
							modelMap.put(doc.getModel(), fitness);
							
						}
					}
					System.out.println(precision + " " + systematicError + " "
							+ baselineError);
					parameterSummary(m, modelMap, parameters, parameterValues);
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
					writer.write(objectsResult, new File(args[1] + "/values_"
								+ precision + "_" + systematicError + "_" + baselineError
								+ ".csv"));
					
				}
			}
		}
		
	}
	
	private static void parameterSummary(Model originalModel,
		Map<Model, Double> modelMap, List<Symbol> parameters,
		Map<String, double[]> parameterValues) {
		
		int numberToConsider = 50;
		List<Model> models = new LinkedList<Model>();
		for(Model m: modelMap.keySet()) {
			models.add(m);
		}
		
		List<Model> modelsAdded = new LinkedList<Model>();

		double[] fitnesses = new double[models.size()];
			
			for (int i = 0; i != models.size(); i++) {
				fitnesses[i] = modelMap.get(models.get(i));
			}
			Arrays.sort(fitnesses);
			double limit = fitnesses[Math.min(numberToConsider - 1,
				models.size() - 1)];
			int added = 0;
			for (Model m : models) {
				if (modelMap.get(m) < limit) {
					modelsAdded.add(m);
					added++;
				}
			}
			for (Model m : models) {
				if (modelMap.get(m) == limit) {
					modelsAdded.add(m);
					added++;
					if (added >= numberToConsider) {
						break;
					}
				}
			}
			
			for (Symbol s : parameters) {
				
				double[] par = new double[numberToConsider];
				int modelCounter = 0;
				for (Model m : modelsAdded) {
					Parameter p2 = m.getParameter(s.getId());
					if (p2 != null) {
						par[modelCounter] = p2.getValue();
					} else {
						par[modelCounter] = m.getSpecies(s.getId()).getValue();
					}
					modelCounter++;
				}
				
				
					
//				values[experimentNumber - 1] = bestModel.get;
				parameterValues.put(s.getId(), par);
		}
	}
	
}
