/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2012 by the University of Tuebingen, Germany.
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
import java.io.IOException;
import java.util.ArrayList;
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
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.sbml.optimization.QuantityRange;
import org.sbml.optimization.problem.EstimationOptions;
import org.sbml.optimization.problem.EstimationProblem;
import org.sbml.simulator.io.CSVDataImporter;
import org.sbml.simulator.io.SimulatorIOOptions;
import org.sbml.simulator.math.N_Metric;
import org.sbml.simulator.math.QualityMeasure;
import org.sbml.simulator.math.Relative_N_Metric;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.MultiTable;

import de.zbit.AppConf;
import de.zbit.io.csv.CSVOptions;
import de.zbit.io.csv.CSVWriter;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;
import eva2.server.go.individuals.ESIndividualDoubleData;
import eva2.server.go.operators.terminators.EvaluationTerminator;
import eva2.server.go.strategies.DifferentialEvolution;
import eva2.server.go.strategies.InterfaceOptimizer;
import eva2.server.modules.GOParameters;

/**
 * This class handels
 * 
 * @author Alexander D&ouml;rr
 * @version $Rev$
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
			simEndTime = Double.valueOf(props
					.get(SimulationOptions.SIM_END_TIME));
		} else {
			simEndTime = prefs.getDouble(SimulationOptions.SIM_END_TIME);
		}

		if (props.containsKey(SimulationOptions.SIM_STEP_SIZE)) {
			simStepSize = Double.valueOf(props
					.get(SimulationOptions.SIM_STEP_SIZE));
		} else {
			simStepSize = prefs.getDouble(SimulationOptions.SIM_STEP_SIZE);
		}

		double defaultQualityValue;
		if (props.containsKey(EstimationOptions.QUALITY_DEFAULT_VALUE)) {
				defaultQualityValue = Double.valueOf(props
						.get(EstimationOptions.QUALITY_DEFAULT_VALUE));
			} else {
				defaultQualityValue = prefs.getDouble(EstimationOptions.QUALITY_DEFAULT_VALUE);
			}
			
			double root;
			if (props.containsKey(EstimationOptions.QUALITY_N_METRIC_ROOT)) {
				root = Double.valueOf(props
						.get(EstimationOptions.QUALITY_N_METRIC_ROOT));
			} else {
				root = prefs.getDouble(EstimationOptions.QUALITY_N_METRIC_ROOT);
			}
			
			try {
				qualityMeasure = (QualityMeasure) Class.forName(
						prefs.getString(EstimationOptions.QUALITY_MEASURE))
						.newInstance();
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
					((N_Metric)qualityMeasure).setRoot(root);
				}
				else if (qualityMeasure instanceof Relative_N_Metric) {
					((Relative_N_Metric)qualityMeasure).setRoot(root);
				}
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
				solver = (AbstractDESSolver) Class.forName(
						prefs.getString(SimulationOptions.ODE_SOLVER))
						.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			double defaultCompartmentValue, defaultSpeciesValue, defaultParameterValue;
	      
	    if (props.containsKey(SimulationOptions.DEFAULT_INIT_COMPARTMENT_SIZE)) {
				defaultCompartmentValue=Double.valueOf(props.get(SimulationOptions.DEFAULT_INIT_COMPARTMENT_SIZE));
			}
			else {
				defaultCompartmentValue=Double.valueOf(prefs.get(SimulationOptions.DEFAULT_INIT_COMPARTMENT_SIZE));
			}
			
			if (props.containsKey(SimulationOptions.DEFAULT_INIT_SPECIES_VALUE)) {
				defaultSpeciesValue=Double.valueOf(props.get(SimulationOptions.DEFAULT_INIT_SPECIES_VALUE));
			}
			else {
				defaultSpeciesValue=Double.valueOf(prefs.get(SimulationOptions.DEFAULT_INIT_SPECIES_VALUE));
			}
			
			if (props.containsKey(SimulationOptions.DEFAULT_INIT_PARAMETER_VALUE)) {
				defaultParameterValue=Double.valueOf(props.get(SimulationOptions.DEFAULT_INIT_PARAMETER_VALUE));
			}
			else {
				defaultParameterValue=Double.valueOf(prefs.get(SimulationOptions.DEFAULT_INIT_PARAMETER_VALUE));
			}
			
			if (props.containsKey(SimulationOptions.ABS_TOL)) {
				absTol = Double.valueOf(props.get(SimulationOptions.ABS_TOL));
			}
			else {
				absTol = Double.valueOf(prefs.get(SimulationOptions.ABS_TOL));
			}
			
			if (props.containsKey(SimulationOptions.REL_TOL)) {
				relTol = Double.valueOf(props.get(SimulationOptions.REL_TOL));
			}
			else {
				relTol = Double.valueOf(prefs.get(SimulationOptions.REL_TOL));
			}
			
			simulationConfiguration = new SimulationConfiguration(model,
					solver, 0, simEndTime, simStepSize, false, absTol, relTol);

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
			}
			else {
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
		
		boolean multiShoot;
		if (props.containsKey(EstimationOptions.EST_MULTI_SHOOT)) {
			multiShoot = Boolean.valueOf(props.get(EstimationOptions.EST_MULTI_SHOOT));
		}
		else {
			multiShoot = Boolean.valueOf(prefsEst.get(EstimationOptions.EST_MULTI_SHOOT));
		}
		
		boolean allGlobalParameters;
		if (props.containsKey(EstimationOptions.EST_ALL_GLOBAL_PARAMETERS)) {
			allGlobalParameters = Boolean.valueOf(props.get(EstimationOptions.EST_ALL_GLOBAL_PARAMETERS));
		}
		else {
			allGlobalParameters = Boolean.valueOf(prefsEst.get(EstimationOptions.EST_ALL_GLOBAL_PARAMETERS));
		}
		
		boolean allLocalParameters;
		if (props.containsKey(EstimationOptions.EST_ALL_LOCAL_PARAMETERS)) {
			allLocalParameters = Boolean.valueOf(props.get(EstimationOptions.EST_ALL_LOCAL_PARAMETERS));
		}
		else {
			allLocalParameters = Boolean.valueOf(prefsEst.get(EstimationOptions.EST_ALL_LOCAL_PARAMETERS));
		}
		
		boolean allSpecies=false;
		if (props.containsKey(EstimationOptions.EST_ALL_SPECIES)) {
			allSpecies = Boolean.valueOf(props.get(EstimationOptions.EST_ALL_SPECIES));
		}
		else {
			allSpecies = Boolean.valueOf(prefsEst.get(EstimationOptions.EST_ALL_SPECIES));
		}
		
		boolean allCompartments = false;
		if (props.containsKey(EstimationOptions.EST_ALL_COMPARTMENTS)) {
			allCompartments = Boolean.valueOf(props.get(EstimationOptions.EST_ALL_COMPARTMENTS));
		}
		else {
			allCompartments = Boolean.valueOf(prefsEst.get(EstimationOptions.EST_ALL_COMPARTMENTS));
		}
  		boolean allUndefinedQuantities = false;
  		if (props.containsKey(EstimationOptions.EST_ALL_UNDEFINED_QUANTITIES)) {
  			allUndefinedQuantities = props.getBoolean(EstimationOptions.EST_ALL_UNDEFINED_QUANTITIES);
  		} else {
  			allUndefinedQuantities = prefsEst.getBoolean(EstimationOptions.EST_ALL_UNDEFINED_QUANTITIES);
  		}
		
		Model clonedModel = simulationManager.getSimulationConfiguration().getModel().clone();
		try {
			estimationProblem = new EstimationProblem(simulationManager.getSimulationConfiguration().getSolver(), simulationManager.getQualityMeasurement().getDistance(), clonedModel, simulationManager.getQualityMeasurement().getMeasurements(),
				multiShoot, createQuantityRanges(clonedModel, allGlobalParameters, allLocalParameters, allSpecies, allCompartments, allUndefinedQuantities, initMin, initMax, min, max));
		} catch (SBMLException e) {
			e.printStackTrace();
		} catch (ModelOverdeterminedException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Conducts the simulation.
	 * 
	 * @throws Exception
	 */
	private void simulate() throws Exception {
		simulationManager.simulateWithoutGUI();
	}
	
	/**
	 * 
	 * @param openFile
	 * @param timeSeriesFile
	 * @param props
	 */
	private void performOptimization() {
		
		GOParameters goParams = new GOParameters(); // Instance for the general
		// Genetic Optimization
		// parameterization

		// set the initial EvA problem here
		goParams.setProblem(estimationProblem);
		goParams.setOptimizer(new DifferentialEvolution());
		goParams.setTerminator(new EvaluationTerminator(3000));
		
		InterfaceOptimizer optimizer = goParams.getOptimizer();
		optimizer.init();
		while (!goParams.getTerminator().isTerminated(optimizer.getPopulation()))  {
			optimizer.optimize();
		}

		ESIndividualDoubleData best = (ESIndividualDoubleData)optimizer.getPopulation().get(0);
		double[] estimations = best.getDoubleData();
		double fitness = best.getFitness()[0];
		logger.info("Fitness: " + fitness);
		for (int i=0; i!=estimations.length; i++) {
			logger.info(estimationProblem.getQuantityRanges()[i].getQuantity().getName() + ": " + estimations[i]);
		}
		
  		//TODO add listener
  		//TODO refresh model
			//TODO output to file
		
	}

	/**
	 * @param model
	 * @param allGlobalParameters
	 * @param allLocalParameters
	 * @param allSpecies
	 * @param allCompartments
	 * @return
	 */
	private QuantityRange[] createQuantityRanges(Model model,
		boolean allGlobalParameters, boolean allLocalParameters,
		boolean allSpecies, boolean allCompartments, boolean allUndefinedQuantities, double initMin, double initMax, double min, double max) {
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
	public void run() {
		try {
			if (estimationProblem != null) {
				performOptimization();				
			}	
			else {
				simulate();
			}
		} catch (Exception e) {
			e.printStackTrace();				
		}
		
	}

}
