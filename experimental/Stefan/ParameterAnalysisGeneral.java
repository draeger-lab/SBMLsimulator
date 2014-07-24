import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

public class ParameterAnalysisGeneral {
	
	public static void main(String args[]) throws XMLStreamException, IOException {
		SBMLReader sbmlReader = new SBMLReader();
		Model m = sbmlReader.readSBML(args[0]).getModel();
		
		String parameterFilePath = args[2];
		CSVReader reader = new CSVReader(parameterFilePath);
		String[][] data = reader.getData();
		List<Symbol> parameters = new LinkedList<Symbol>();
		boolean replicates = false;
		
//		int[] errorValues = new int[] { 0, 5, 10, 15, 20, 25, 30 };
		int[] errorValues = new int[] { 0, 5};
//		int[] errorValues = new int[] { 0};
		Map<String, double[][][]> medians = new HashMap<String, double[][][]>();
		Map<String, double[][][]> stddevs = new HashMap<String, double[][][]>();
		for (int row = 0; row != data.length; row++) {
			Parameter p = new Parameter(data[row][0]);
			parameters.add(p);
			medians
					.put(
						p.getId(),
						new double[errorValues.length][errorValues.length][errorValues.length]);
			stddevs
					.put(
						p.getId(),
						new double[errorValues.length][errorValues.length][errorValues.length]);
		}
		
		for (int i = 0; i != errorValues.length; i++) {
			for (int j = 0; j != errorValues.length; j++) {
				for (int k = 0; k != errorValues.length; k++) {
					int precision = errorValues[i];
					int systematicError = errorValues[j];
					int baselineError = errorValues[k];
					File folder = new File(args[1] + "/Errors_" + precision + "_"
							+ systematicError + "_" + baselineError);
					File[] modelFiles = folder.listFiles();
					
					if (modelFiles == null) {
						System.out.println();
					}
					
					Map<Model,Double> modelMap = new HashMap<Model,Double>();
					Map<Integer,List<Model>> numberMap = new HashMap<Integer,List<Model>>();
					File modelFile;
					for (int n=0; n!=modelFiles.length; n++) {
						modelFile = modelFiles[n];
						if (SBFileFilter.isSBMLFile(modelFile)) {
							FileInputStream stream = new FileInputStream(modelFile);
							SBMLDocument doc = new SBMLReader().readSBMLFromStream(stream, new SimpleTreeNodeChangeListener());
							stream.close();
							double fitness = Double.parseDouble(modelFile.getAbsolutePath()
									.replaceAll(".*Fitness_", "").replace(".xml", ""));
							int number = Integer.parseInt(modelFile.getAbsolutePath()
								.replaceAll(".*Exp_", "").replaceAll("_1_Rep_.*", "").replaceAll(".*_", ""));
							if(replicates && (modelFile.getAbsolutePath().contains("["))) {
								modelMap.put(doc.getModel(), fitness);
								List<Model> models = numberMap.get(number);
								if(models == null) {
									models = new LinkedList<Model>();
								}
								models.add(doc.getModel());
								numberMap.put(number, models);
							}
							else if(!replicates && !(modelFile.getAbsolutePath().contains("["))) {
								modelMap.put(doc.getModel(), fitness);
								List<Model> models = numberMap.get(number);
								if(models == null) {
									models = new LinkedList<Model>();
								}
								models.add(doc.getModel());
								numberMap.put(number, models);
							}
							
						}
					}
					System.out.println(precision + " " + systematicError + " " + baselineError);
					double[][] currentResult = ParameterAnalysis.parameterAnalysis(m,
						modelMap, parameters, numberMap);
					for (int l = 0; l != currentResult.length; l++) {
						double[][][] resultTableMedians = medians.get(parameters.get(l)
								.getId());
						resultTableMedians[i][j][k] = currentResult[l][0];
						double[][][] resultTableStdDev = stddevs.get(parameters.get(l)
								.getId());
						resultTableStdDev[i][j][k] = currentResult[l][1];
					}
				}
			}
		}
		
		for (Symbol p : parameters) {
			double[][][] resultMedians = medians.get(p.getId());
			double[][][] resultStdDev = stddevs.get(p.getId());
			for (int num = 0; num != resultMedians[0][0].length; num++) {
				CSVWriter writer = new CSVWriter();
				
				Object[][] objectsMedians = new Object[resultMedians.length][resultMedians[0].length];
				
				for (int i = 0; i != resultMedians.length; i++) {
					for (int j = 0; j != resultMedians[0].length; j++) {
						objectsMedians[i][j] = new Double(resultMedians[i][j][num]);
					}
				}
				if(replicates) {
					writer.write(objectsMedians, new File(args[1] + "/medians_"
						+ errorValues[num] + "_" + p.getId() + "_rep.csv"));
				}
				else {
					writer.write(objectsMedians, new File(args[1] + "/medians_"
							+ errorValues[num] + "_" + p.getId() + ".csv"));
				}
				Object[][] objectsStdDev = new Object[resultStdDev.length][resultStdDev[0].length];
				
				for (int i = 0; i != resultStdDev.length; i++) {
					for (int j = 0; j != resultStdDev[0].length; j++) {
						objectsStdDev[i][j] = new Double(resultStdDev[i][j][num]);
					}
				}
				
				if(replicates) {
					writer.write(objectsStdDev, new File(args[1] + "/stddev_"
						+ errorValues[num] + "_" + p.getId() + "_rep.csv"));
				}
				else {
					writer.write(objectsStdDev, new File(args[1] + "/stddev_"
							+ errorValues[num] + "_" + p.getId() + ".csv"));
				}
			}
		}
		
	}
	
}
