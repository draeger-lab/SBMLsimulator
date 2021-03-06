/*
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2016 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Quantity;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.sbml.optimization.QuantityRange;
import org.sbml.optimization.problem.EstimationOptions;
import org.sbml.optimization.problem.EstimationProblem;
import org.sbml.simulator.io.CSVDataImporter;
import org.sbml.simulator.io.SimulatorIOOptions;
import org.sbml.simulator.math.SplineCalculation;
import org.simulator.math.N_Metric;
import org.simulator.math.QualityMeasure;
import org.simulator.math.Relative_N_Metric;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.MultiTable;

import de.zbit.AppConf;
import de.zbit.io.csv.CSVOptions;
import de.zbit.io.csv.CSVWriter;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;
import eva2.optimization.individuals.ESIndividualDoubleData;
import eva2.optimization.operator.terminators.EvaluationTerminator;
import eva2.optimization.strategies.DifferentialEvolution;
import eva2.optimization.strategies.InterfaceOptimizer;
import eva2.optimization.modules.OptimizationParameters;

/**
 * This class handles the execution of SBMLsimulator from the command line.
 * 
 * @author Alexander D&ouml;rr
 * @since 1.0
 */
public class CommandLineManager implements PropertyChangeListener, Runnable {

  /**
   * The simulation manager for the current simulation.
   */
  private SimulationManager simulationManager;

  /**
   * A {@link Logger} for this class.
   */
  private static final Logger logger = Logger.getLogger(CommandLineManager.class.getName());

  /**
   * 
   */
  private String openFile;

  /**
   * 
   */
  private String timeSeriesFile;

  /**
   * 
   */
  private AppConf appConf;

  /**
   * 
   */
  private double defaultSpeciesValue;

  /**
   * 
   */
  private double defaultParameterValue;

  /**
   * 
   */
  private double defaultCompartmentValue;
  /**
   * 
   */
  private String outSBMLFile;

  private EstimationProblem estimationProblem;

  public CommandLineManager(String openFile, String timeSeriesFile,
    AppConf appConf) {
    this.openFile = openFile;
    this.appConf = appConf;
    this.timeSeriesFile = timeSeriesFile;
    loadPreferences();
  }

  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if ("progress".equals(evt.getPropertyName())) {
    } else if ("done".equals(evt.getPropertyName())) {
      MultiTable data = (MultiTable) evt.getNewValue();
      if (data != null) {
        processResult(data);
      } else {

      }
    }

  }

  /**
   * 
   * @param data
   */
  private void processResult(MultiTable data) {
    SBProperties props = appConf.getCmdArgs();
    SBPreferences prefs = SBPreferences
        .getPreferencesFor(CSVOptions.class);
    String outCSVFile;
    if (props.containsKey(SimulatorIOOptions.SIMULATION_OUTPUT_FILE)) {
      outCSVFile = props.get(SimulatorIOOptions.SIMULATION_OUTPUT_FILE)
          .toString();
    } else {
      outCSVFile = openFile.substring(0, openFile.lastIndexOf('.'))
          + "_simulated.csv";
    }
    try {
      char separator;
      if (props.containsKey(CSVOptions.CSV_FILES_SEPARATOR_CHAR)) {
        separator = props.get(CSVOptions.CSV_FILES_SEPARATOR_CHAR).toString().charAt(0);
      }
      else {
        separator = prefs.get(CSVOptions.CSV_FILES_SEPARATOR_CHAR).toString().charAt(0);
      }

      (new CSVWriter()).write(data, separator, outCSVFile);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  /**
   * 
   */
  private void loadPreferences() {
    AbstractDESSolver solver = null;
    SBPreferences prefs = SBPreferences
        .getPreferencesFor(SimulationOptions.class);
    QualityMeasure qualityMeasure = null;
    SimulationConfiguration simulationConfiguration;
    double simEndTime, simStepSize, absTol, relTol;
    SBProperties props = appConf.getCmdArgs();

    if (props.containsKey(SimulationOptions.SIM_END_TIME)) {
      simEndTime = Double.valueOf(props.get(SimulationOptions.SIM_END_TIME));
    } else {
      simEndTime = prefs.getDouble(SimulationOptions.SIM_END_TIME);
    }

    if (props.containsKey(SimulationOptions.SIM_STEP_SIZE)) {
      simStepSize = Double.valueOf(props.get(SimulationOptions.SIM_STEP_SIZE));
    } else {
      simStepSize = prefs.getDouble(SimulationOptions.SIM_STEP_SIZE);
    }

    Model model = null;
    try {
      model = (new SBMLReader()).readSBML(openFile).getModel();
    } catch (XMLStreamException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      if (props.containsKey(SimulationOptions.ODE_SOLVER)) {
        solver = (AbstractDESSolver) Class.forName(
          props.get(SimulationOptions.ODE_SOLVER)).newInstance();
      } else {
        solver = (AbstractDESSolver)
            prefs.getClass(SimulationOptions.ODE_SOLVER).newInstance();
      }

    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    if (props.containsKey(SimulationOptions.DEFAULT_INIT_COMPARTMENT_SIZE)) {
      defaultCompartmentValue = Double.valueOf(props
        .get(SimulationOptions.DEFAULT_INIT_COMPARTMENT_SIZE));
    } else {
      defaultCompartmentValue = Double.valueOf(prefs
        .get(SimulationOptions.DEFAULT_INIT_COMPARTMENT_SIZE));
    }

    if (props.containsKey(SimulationOptions.DEFAULT_INIT_SPECIES_VALUE)) {
      defaultSpeciesValue = Double.valueOf(props
        .get(SimulationOptions.DEFAULT_INIT_SPECIES_VALUE));
    } else {
      defaultSpeciesValue = Double.valueOf(prefs
        .get(SimulationOptions.DEFAULT_INIT_SPECIES_VALUE));
    }

    if (props.containsKey(SimulationOptions.DEFAULT_INIT_PARAMETER_VALUE)) {
      defaultParameterValue = Double.valueOf(props
        .get(SimulationOptions.DEFAULT_INIT_PARAMETER_VALUE));
    } else {
      defaultParameterValue = Double.valueOf(prefs
        .get(SimulationOptions.DEFAULT_INIT_PARAMETER_VALUE));
    }

    if (props.containsKey(SimulationOptions.ABS_TOL)) {
      absTol = Double.valueOf(props.get(SimulationOptions.ABS_TOL));
    } else {
      absTol = Double.valueOf(prefs.get(SimulationOptions.ABS_TOL));
    }

    if (props.containsKey(SimulationOptions.REL_TOL)) {
      relTol = Double.valueOf(props.get(SimulationOptions.REL_TOL));
    } else {
      relTol = Double.valueOf(prefs.get(SimulationOptions.REL_TOL));
    }

    simulationConfiguration = new SimulationConfiguration(model, solver, 0,
      simEndTime, simStepSize, false, absTol, relTol);

    prefs = SBPreferences
        .getPreferencesFor(EstimationOptions.class);
    double defaultQualityValue;
    if (props.containsKey(EstimationOptions.QUALITY_DEFAULT_VALUE)) {
      defaultQualityValue = Double.valueOf(props
        .get(EstimationOptions.QUALITY_DEFAULT_VALUE));
    } else {
      defaultQualityValue = prefs
          .getDouble(EstimationOptions.QUALITY_DEFAULT_VALUE);
    }

    double root;
    if (props.containsKey(EstimationOptions.QUALITY_N_METRIC_ROOT)) {
      root = Double.valueOf(props.get(EstimationOptions.QUALITY_N_METRIC_ROOT));
    } else {
      root = prefs.getDouble(EstimationOptions.QUALITY_N_METRIC_ROOT);
    }

    try {
      qualityMeasure = (QualityMeasure) Class.forName(
        prefs.getString(EstimationOptions.QUALITY_MEASURE)).newInstance();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    if (qualityMeasure != null) {
      qualityMeasure.setDefaultValue(defaultQualityValue);

      if (qualityMeasure instanceof N_Metric) {
        ((N_Metric) qualityMeasure).setRoot(root);
      } else if (qualityMeasure instanceof Relative_N_Metric) {
        ((Relative_N_Metric) qualityMeasure).setRoot(root);
      }
    }

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
      initializeEstimationPreferences();
    }

  }

  /**
   * 
   */
  private void initializeEstimationPreferences() {
    SBProperties props = appConf.getCmdArgs();
    if (props.containsKey(SimulatorIOOptions.SBML_OUTPUT_FILE)) {
      outSBMLFile = props.get(SimulatorIOOptions.SBML_OUTPUT_FILE)
          .toString();
    } else {
      outSBMLFile = openFile.substring(0, openFile.lastIndexOf('.'))
          + "_optimized.xml";
    }
    SBPreferences prefsEst = SBPreferences.getPreferencesFor(EstimationOptions.class);

    boolean multiShoot;
    if (props.containsKey(EstimationOptions.EST_MULTI_SHOOT)) {
      multiShoot = props.getBoolean(EstimationOptions.EST_MULTI_SHOOT);
    }
    else {
      multiShoot = prefsEst.getBoolean(EstimationOptions.EST_MULTI_SHOOT);
    }
    SBMLDocument clonedDocument = simulationManager.getSimulationConfiguration().getModel().getSBMLDocument().clone();
    Model clonedModel = clonedDocument.getModel();
    //Create quantity ranges from file or with standard preferences
    QuantityRange[] quantityRanges = null;
    if (props.containsKey(EstimationOptions.EST_TARGETS)) {
      String file = String.valueOf(props.get(EstimationOptions.EST_TARGETS));
      try {
        quantityRanges = EstimationProblem.readQuantityRangesFromFile(file, clonedModel);

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    else {
      double initMax, initMin, min, max;
      if (props.containsKey(EstimationOptions.EST_INIT_MAX_VALUE)) {
        initMax = Double.valueOf(props.get(EstimationOptions.EST_INIT_MAX_VALUE));
      }
      else {
        initMax = Double.valueOf(prefsEst.get(EstimationOptions.EST_INIT_MAX_VALUE));
      }

      if (props.containsKey(EstimationOptions.EST_INIT_MIN_VALUE)) {
        initMin = Double.valueOf(props.get(EstimationOptions.EST_INIT_MIN_VALUE));
      }
      else {
        initMin = Double.valueOf(prefsEst.get(EstimationOptions.EST_INIT_MIN_VALUE));
      }

      if (props.containsKey(EstimationOptions.EST_MAX_VALUE)) {
        max = Double.valueOf(props.get(EstimationOptions.EST_MAX_VALUE));
      }
      else {
        max = Double.valueOf(prefsEst.get(EstimationOptions.EST_MAX_VALUE));
      }

      if (props.containsKey(EstimationOptions.EST_MIN_VALUE)) {
        min = Double.valueOf(props.get(EstimationOptions.EST_MIN_VALUE));
      }
      else {
        min = Double.valueOf(prefsEst.get(EstimationOptions.EST_MIN_VALUE));
      }



      boolean allGlobalParameters;
      if (props.containsKey(EstimationOptions.EST_ALL_GLOBAL_PARAMETERS)) {
        allGlobalParameters = props.getBoolean(EstimationOptions.EST_ALL_GLOBAL_PARAMETERS);
      }
      else {
        allGlobalParameters = prefsEst.getBoolean(EstimationOptions.EST_ALL_GLOBAL_PARAMETERS);
      }

      boolean allLocalParameters;
      if (props.containsKey(EstimationOptions.EST_ALL_LOCAL_PARAMETERS)) {
        allLocalParameters = props.getBoolean(EstimationOptions.EST_ALL_LOCAL_PARAMETERS);
      }
      else {
        allLocalParameters = prefsEst.getBoolean(EstimationOptions.EST_ALL_LOCAL_PARAMETERS);
      }

      boolean allSpecies = false;
      if (props.containsKey(EstimationOptions.EST_ALL_SPECIES)) {
        allSpecies = props.getBoolean(EstimationOptions.EST_ALL_SPECIES);
      } else {
        allSpecies = prefsEst.getBoolean(EstimationOptions.EST_ALL_SPECIES);
      }

      boolean allCompartments = false;
      if (props.containsKey(EstimationOptions.EST_ALL_COMPARTMENTS)) {
        allCompartments = props.getBoolean(EstimationOptions.EST_ALL_COMPARTMENTS);
      } else {
        allCompartments = prefsEst.getBoolean(EstimationOptions.EST_ALL_COMPARTMENTS);
      }
      boolean allUndefinedQuantities = false;
      if (props.containsKey(EstimationOptions.EST_ALL_UNDEFINED_QUANTITIES)) {
        allUndefinedQuantities = props.getBoolean(EstimationOptions.EST_ALL_UNDEFINED_QUANTITIES);
      } else {
        allUndefinedQuantities = prefsEst.getBoolean(EstimationOptions.EST_ALL_UNDEFINED_QUANTITIES);
      }
      try {
        quantityRanges = createQuantityRanges(clonedModel, allGlobalParameters, allLocalParameters, allSpecies, allCompartments, allUndefinedQuantities, initMin, initMax, min, max);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    boolean fitToSplines = false;
    if (props.containsKey(EstimationOptions.FIT_TO_SPLINES)) {
      fitToSplines = props.getBoolean(EstimationOptions.FIT_TO_SPLINES);
    }
    else {
      fitToSplines = prefsEst.getBoolean(EstimationOptions.FIT_TO_SPLINES);
    }

    int numSplineSamples = 0;
    if (props.containsKey(EstimationOptions.NUMBER_OF_SPLINE_SAMPLES)) {
      numSplineSamples = Integer.valueOf(props.get(EstimationOptions.NUMBER_OF_SPLINE_SAMPLES));
    }
    else {
      numSplineSamples = Integer.valueOf(prefsEst.get(EstimationOptions.NUMBER_OF_SPLINE_SAMPLES));
    }
    if (fitToSplines) {
      for (int i = simulationManager.getQualityMeasurement().getMeasurements().size() - 1; i >= 0; i--) {
        simulationManager.getQualityMeasurement().getMeasurements().set(i, SplineCalculation.calculateSplineValues(
          simulationManager.getQualityMeasurement().getMeasurements().get(i),
          numSplineSamples));
      }
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
   * Conducts the simulation.
   * 
   * @throws Exception
   */
  private void simulate(double defaultSpeciesValue, double defaultParameterValue, double defaultCompartmentValue) throws Exception {
    simulationManager.simulateWithoutGUI(defaultSpeciesValue, defaultParameterValue, defaultCompartmentValue);
  }

  /**
   *
   */
	private void performOptimization() {
		//Set initial values to values given in experimental data.
		String[] quantityIds = new String[estimationProblem.getQuantities().length];
		for (int i = 0; i < estimationProblem.getQuantities().length; i++) {
			quantityIds[i] = estimationProblem.getQuantities()[i].getId();
		}
		MultiTable reference = estimationProblem.getReferenceData()[0];
		if (reference.getTimePoints()[0] == 0d) {
			for (int col = 0; col < reference.getColumnCount(); col++) {
				String id = reference.getColumnIdentifier(col);
				if (Arrays.binarySearch(quantityIds, id) < 0) {
					double value = reference.getValueAt(0, col).doubleValue();
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
		}


    OptimizationParameters goParams = new OptimizationParameters(); // Instance for the general
    // Genetic Optimization
    // parameterization

    // set the initial EvA problem here
    goParams.setProblem(estimationProblem);
    goParams.setOptimizer(new DifferentialEvolution());
    goParams.setTerminator(new EvaluationTerminator(100000));

    InterfaceOptimizer optimizer = goParams.getOptimizer();
    optimizer.initialize();
    while (!goParams.getTerminator().isTerminated(optimizer.getPopulation()))  {
      optimizer.optimize();
    }

    ESIndividualDoubleData best = (ESIndividualDoubleData)optimizer.getPopulation().getBestEAIndividual();
    double[] estimations = best.getDoubleData();
    double fitness = best.getFitness()[0];
    logger.info("Fitness: " + fitness);
    for (int i = 0; i != estimations.length; i++) {
      logger.info(estimationProblem.getQuantityRanges()[i].getQuantity().getName() + ": " + estimations[i]);
    }

    //Refresh model
    for(int i=0; i!=estimations.length; i++) {
      estimationProblem.getQuantities()[i].setValue(estimations[i]);
    }

    //Save model to file
    if (outSBMLFile != null) {
      try {
        (new SBMLWriter()).write(
          estimationProblem.getModel().getSBMLDocument(), outSBMLFile);
      } catch (SBMLException e) {
        e.printStackTrace();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (XMLStreamException e) {
        e.printStackTrace();
      }
    }

  }

  /**
   * @param model
   * @param allGlobalParameters
   * @param allLocalParameters
   * @param allSpecies
   * @param allCompartments
   * @param allUndefinedQuantities
   * @return
   */
  private QuantityRange[] createQuantityRanges(Model model,
    boolean allGlobalParameters, boolean allLocalParameters,
    boolean allSpecies, boolean allCompartments,
    boolean allUndefinedQuantities, double initMin, double initMax, double min,
    double max) {
    ArrayList<QuantityRange> quantities = new ArrayList<QuantityRange>();

    if (allGlobalParameters) {
      for (Parameter p: model.getListOfParameters()) {
        quantities.add(new QuantityRange(p, true, initMin, initMax, min, max));
      }
    }

    if (allSpecies) {
      for (Species s: model.getListOfSpecies()) {
        quantities.add(new QuantityRange(s, true, initMin, initMax, min, max));
      }
    }

    if (allCompartments) {
      for (Compartment c: model.getListOfCompartments()) {
        quantities.add(new QuantityRange(c, true, initMin, initMax, min, max));
      }
    }

    if (allLocalParameters) {
      for (Reaction r : model.getListOfReactions()) {
        if (r.isSetKineticLaw()) {
          for (LocalParameter lp: r.getKineticLaw().getListOfLocalParameters()) {
            quantities.add(new QuantityRange(lp, true, initMin, initMax, min, max));
          }
        }
      }
    }

    if (allUndefinedQuantities) {
      addAllUndefinedQuantities(quantities, model.getListOfParameters(), initMin, initMax, min, max);
      addAllUndefinedQuantities(quantities, model.getListOfSpecies(), initMin, initMax, min, max);
      addAllUndefinedQuantities(quantities, model.getListOfCompartments(), initMin, initMax, min, max);
      for (Reaction r : model.getListOfReactions()) {
        if (r.isSetKineticLaw() && r.getKineticLaw().isSetListOfLocalParameters()) {
          addAllUndefinedQuantities(quantities, r.getKineticLaw().getListOfLocalParameters(), initMin, initMax, min, max);
        }
      }
    }

    return quantities.toArray(new QuantityRange[quantities.size()]);
  }

  /**
   * 
   * @param quantities
   * @param listOfQuantities
   * @param initMin
   * @param initMax
   * @param min
   * @param max
   */
  private void addAllUndefinedQuantities(List<QuantityRange> quantities, ListOf<? extends Quantity> listOfQuantities, double initMin, double initMax, double min, double max) {
    for (Quantity q : listOfQuantities) {
      if ((!q.isSetValue() || Double.isNaN(q.getValue())) && !quantities.contains(q)) {
        quantities.add(new QuantityRange(q, true, initMin, initMax, min, max));
      }
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    try {
      if (estimationProblem != null) {
        performOptimization();
      }
      else {
        simulate(defaultSpeciesValue, defaultParameterValue, defaultCompartmentValue);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}
