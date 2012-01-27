/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
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
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.sbml.simulator.gui.SimulatorUI;
import org.sbml.simulator.io.SimulatorIOOptions;
import org.sbml.simulator.math.N_Metric;
import org.sbml.simulator.math.QualityMeasure;
import org.sbml.simulator.math.Relative_N_Metric;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.MultiTable;

import de.zbit.AppConf;
import de.zbit.io.CSVOptions;
import de.zbit.io.CSVWriter;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;

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
	String openFile;
	String timeSeriesFile;
	AppConf appConf;

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
			if(props.containsKey(CSVOptions.CSV_FILES_SEPARATOR_CHAR)) {
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
		AbstractDESSolver solver;
		SBPreferences prefs = SBPreferences
				.getPreferencesFor(SimulationOptions.class);
		QualityMeasure qualityMeasure;
		SimulationConfiguration simulationConfiguration;
		double simEndTime, simStepSize;
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

		try {
			double defaultQualityValue;
			if (props.containsKey(SimulationOptions.QUALITY_DEFAULT_VALUE)) {
				defaultQualityValue = Double.valueOf(props
						.get(SimulationOptions.QUALITY_DEFAULT_VALUE));
			} else {
				defaultQualityValue = prefs.getDouble(SimulationOptions.QUALITY_DEFAULT_VALUE);
			}
			
			double root;
			if (props.containsKey(SimulationOptions.QUALITY_N_METRIC_ROOT)) {
				root = Double.valueOf(props
						.get(SimulationOptions.QUALITY_N_METRIC_ROOT));
			} else {
				root = prefs.getDouble(SimulationOptions.QUALITY_N_METRIC_ROOT);
			}
			
			qualityMeasure = (QualityMeasure) Class.forName(
					prefs.getString(SimulationOptions.QUALITY_MEASURE))
					.newInstance();
			qualityMeasure.setDefaultValue(defaultQualityValue);
			
			if(qualityMeasure instanceof N_Metric) {
				((N_Metric)qualityMeasure).setRoot(root);
			}
			else if(qualityMeasure instanceof Relative_N_Metric) {
				((Relative_N_Metric)qualityMeasure).setRoot(root);
			}
			
			

			Model model = (new SBMLReader()).readSBML(openFile).getModel();

			solver = (AbstractDESSolver) Class.forName(
					prefs.getString(SimulationOptions.ODE_SOLVER))
					.newInstance();

			double defaultCompartmentValue, defaultSpeciesValue, defaultParameterValue;
	      
	    if(props.containsKey(SimulationOptions.DEFAULT_INIT_COMPARTMENT_SIZE)) {
				defaultCompartmentValue=Double.valueOf(props.get(SimulationOptions.DEFAULT_INIT_COMPARTMENT_SIZE));
			}
			else {
				defaultCompartmentValue=Double.valueOf(prefs.get(SimulationOptions.DEFAULT_INIT_COMPARTMENT_SIZE));
			}
			
			if(props.containsKey(SimulationOptions.DEFAULT_INIT_SPECIES_VALUE)) {
				defaultSpeciesValue=Double.valueOf(props.get(SimulationOptions.DEFAULT_INIT_SPECIES_VALUE));
			}
			else {
				defaultSpeciesValue=Double.valueOf(prefs.get(SimulationOptions.DEFAULT_INIT_SPECIES_VALUE));
			}
			
			if(props.containsKey(SimulationOptions.DEFAULT_INIT_PARAMETER_VALUE)) {
				defaultParameterValue=Double.valueOf(props.get(SimulationOptions.DEFAULT_INIT_PARAMETER_VALUE));
			}
			else {
				defaultParameterValue=Double.valueOf(prefs.get(SimulationOptions.DEFAULT_INIT_PARAMETER_VALUE));
			}
			
			simulationConfiguration = new SimulationConfiguration(model,
					solver, 0, simEndTime, simStepSize, false);

			simulationManager = new SimulationManager(new QualityMeasurement(qualityMeasure),
					simulationConfiguration);
			simulationManager.addPropertyChangeListener(this);
			
			//TODO estimation

		} catch (Exception e) {
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

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			simulate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
