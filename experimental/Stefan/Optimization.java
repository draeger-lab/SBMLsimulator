import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.optimization.QuantityRange;
import org.sbml.optimization.problem.EstimationProblem;
import org.sbml.simulator.QualityMeasurement;
import org.sbml.simulator.SimulationConfiguration;
import org.sbml.simulator.SimulationManager;
import org.sbml.simulator.io.CSVDataImporter;
import org.simulator.math.QualityMeasure;
import org.simulator.math.RelativeEuclideanDistance;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.MultiTable;
import org.simulator.math.odes.RosenbrockSolver;

import de.zbit.io.csv.CSVReader;

import eva2.server.go.individuals.ESIndividualDoubleData;
import eva2.server.go.operators.terminators.EvaluationTerminator;
import eva2.server.go.strategies.DifferentialEvolution;
import eva2.server.go.strategies.InterfaceOptimizer;
import eva2.server.modules.GOParameters;

/**
 * This class enables you to perform parameter-optimization-runs for a model
 * @author Stefan Fischer
 *
 */
public class Optimization implements PropertyChangeListener{

	/**
	 * The simulation manager for the current simulation.
	 */
	private SimulationManager simulationManager;


	private EstimationProblem estimationProblem;



	public Optimization(String openFile, String timeSeriesFile, String parameterFile) {
		loadPreferences(openFile, timeSeriesFile, parameterFile);
	}


	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress".equals(evt.getPropertyName())) {
		} else if ("done".equals(evt.getPropertyName())) {
			MultiTable data = (MultiTable) evt.getNewValue();
		}

	}


	/**
	 * 
	 */
	private void loadPreferences(String openFile, String timeSeriesFile, String parameterFile) {
		AbstractDESSolver solver = null;
		QualityMeasure qualityMeasure = null;
		SimulationConfiguration simulationConfiguration;
		double simEndTime, simStepSize, absoluteTolerance, relativeTolerance;

		//TODO
		simEndTime = 600;//10;
		simStepSize = 10;//0.1;
		absoluteTolerance = 1E-10;
		relativeTolerance = 1E-6;
		solver = new RosenbrockSolver();
		Model model = null;

		simulationConfiguration = new SimulationConfiguration(model, solver, 0,
				simEndTime, simStepSize, false, absoluteTolerance, relativeTolerance);

		qualityMeasure = new RelativeEuclideanDistance();

		qualityMeasure.setDefaultValue(100000);

		QualityMeasurement measurement = null;
		if (timeSeriesFile != null) {
			CSVDataImporter csvimporter = new CSVDataImporter();
			MultiTable experimentalData = null;
			try {
				experimentalData = csvimporter.convert(model, timeSeriesFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			List<MultiTable> measurements = new LinkedList<MultiTable>();
			if (experimentalData != null) {
				measurements.add(experimentalData);
			}
			measurement = new QualityMeasurement(qualityMeasure, measurements);
		} else {
			measurement = new QualityMeasurement(qualityMeasure);
		}
		simulationManager = new SimulationManager(measurement,
				simulationConfiguration);
		simulationManager.addPropertyChangeListener(this);

		if (timeSeriesFile != null) {
			initializeEstimationPreferences(parameterFile);
		}

	}

	/**
	 * 
	 */
	private void initializeEstimationPreferences(String parameterFile) {
		boolean multiShoot=false;

		SBMLDocument clonedDocument = simulationManager.getSimulationConfiguration().getModel().getSBMLDocument().clone();
		Model clonedModel = clonedDocument.getModel();
		//Create quantity ranges from file or with standard preferences
		QuantityRange[] quantityRanges = null;
		try {
			quantityRanges = EstimationProblem.readQuantityRangesFromFile(parameterFile, clonedModel);
		} catch (IOException e1) {
			e1.printStackTrace();
		}


		if ((quantityRanges != null) && (quantityRanges.length >= 0)) {
			try {
				estimationProblem = new EstimationProblem(simulationManager.getSimulationConfiguration().getSolver(), simulationManager.getQualityMeasurement().getDistance(), clonedModel, simulationManager.getQualityMeasurement().getMeasurements(),
						multiShoot, quantityRanges);
			} catch (SBMLException e) {
				e.printStackTrace();
			} catch (ModelOverdeterminedException e) {
				e.printStackTrace();
			}
		}
	}


	/**
	 * 
	 * @param openFile
	 * @param timeSeriesFile
	 * @param props
	 */
	private void performOptimization(int evaluations, int repetitions, String directory) {

		for(int j = 0; j < repetitions; j++){

			//Set initial values to values given in experimental data.
			String[] quantityIds = new String[estimationProblem.getQuantities().length];
			for (int i = 0; i < estimationProblem.getQuantities().length; i++) {
				quantityIds[i] = estimationProblem.getQuantities()[i].getId();
			}
			MultiTable reference = estimationProblem.getReferenceData()[0];
			for (int col = 0; col < reference.getColumnCount(); col++) {
				String id = reference.getColumnIdentifier(col);
				if (Arrays.binarySearch(quantityIds, id) < 0) {
					double value = ((Double) reference.getValueAt(0, col))
					.doubleValue();
					if (!Double.isNaN(value)) {
						Species sp = estimationProblem.getModel().getSpecies(id);
						if (sp != null) {
							sp.setValue(value);
							continue;
						}
						Compartment c = estimationProblem.getModel().getCompartment(id);
						if (c != null) {
							c.setValue(value);
							continue;
						}
						Parameter p = estimationProblem.getModel().getParameter(id);
						if (p != null) {
							p.setValue(value);
							continue;
						}

					}
				}
			}


			GOParameters goParams = new GOParameters(); // Instance for the general
			// Genetic Optimization
			// parameterization

			// set the initial EvA problem here
			goParams.setProblem(estimationProblem);
			goParams.setOptimizer(new DifferentialEvolution());
			goParams.setTerminator(new EvaluationTerminator(evaluations));

			InterfaceOptimizer optimizer = goParams.getOptimizer();
			optimizer.init();
			while (!goParams.getTerminator().isTerminated(optimizer.getPopulation()))  {
				optimizer.optimize();
			}

			ESIndividualDoubleData best = (ESIndividualDoubleData)optimizer.getPopulation().get(0);
			double[] estimations = best.getDoubleData();
			double fitness = best.getFitness()[0];
			System.out.println("Fitness: " + fitness);
			for (int i=0; i!=estimations.length; i++) {
				System.out.println(estimationProblem.getQuantityRanges()[i].getQuantity().getName() + ": " + estimations[i]);
			}

			//Refresh model
			for(int i=0; i!=estimations.length; i++) {
				estimationProblem.getQuantities()[i].setValue(estimations[i]);
			}

			//Name of the new model
			String outSBMLFile = directory + "/" + estimationProblem.getModel().getId()+ j + "_Fitness_ " + fitness + ".xml";

			//Save model to file
			if (outSBMLFile != null) {
				try {
					(new SBMLWriter()).write(estimationProblem.getModel().getSBMLDocument(), outSBMLFile);
				} catch (SBMLException e) {
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (XMLStreamException e) {
					e.printStackTrace();
				}
			}
		}
	}



	public static void main(String args[]) throws XMLStreamException, IOException {

		// model
		String modelFile = args[0];
		// experimental data
		String experimentalFile = args[1];
		// parameters that are being optimized
		String parameterFile = args[2];
		// declares how often an optimization is performed for the model
		int repetitions = Integer.parseInt(args[3]);
		// declares how many optimization-steps are performed per optimization (evaluationTerminator)
		int fitnessEvaluations = Integer.parseInt(args[4]);
		// data path
		String directory = args[5];



		// optimization	
		//(new File(directory)).mkdir();

		Optimization opt = new Optimization(modelFile, experimentalFile, parameterFile);

		opt.performOptimization(fitnessEvaluations, repetitions, directory);


	}

}