/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2014 by the University of Tuebingen, Germany.
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
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math.ode.DerivativeException;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.optimization.problem.EstimationProblem;
import org.sbml.simulator.gui.SimulationWorker;
import org.simulator.math.odes.MultiTable;
import org.simulator.sbml.SBMLinterpreter;

import de.zbit.util.ResourceManager;
import eva2.tools.math.Mathematics;

/**
 * This class stores the simulation configuration and the quality measurement
 * for a simulation including the solution and the distance values if computed.
 * 
 * @author Alexander D&ouml;rr
 * @version $Rev$
 * @since 1.0
 */
public class SimulationManager implements PropertyChangeListener {

	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(SimulationManager.class.getName());

	/**
	 * The distance values to the experimental data sets.
	 */
	private double[] distanceValues;

	/**
	 * The mean distance value over all experimental data sets.
	 */
	private double meanDistanceValue;

	/**
	 * The PropertyChangeSupport class for the event handling.
	 */
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	/**
	 * The quality measurement for current simulation.
	 */
	private QualityMeasurement qualityMeasurement;

	/**
	 * The simulation configuration for current simulation.
	 */
	private SimulationConfiguration simulationConfiguration;

	/**
	 * The worker for the simulation.
	 */
	private SimulationWorker simworker;

	/**
	 * The solution computed as a result of a simulation.
	 */
	private MultiTable solution;

	/**
	 * The problem to estimate parameters for.
	 */
	private EstimationProblem estimationProblem;

	/**
	 * @return the estimationProblem
	 */
	public EstimationProblem getEstimationProblem() {
		return estimationProblem;
	}

	/**
	 * @param estimationProblem the estimationProblem to set
	 */
	public void setEstimationProblem(EstimationProblem estimationProblem) {
		this.estimationProblem = estimationProblem;
	}

	/**
	 * Creates a new simulation manager with the given simulation configuration
	 * and quality measurement.
	 * 
	 * @param qualityMeasurement
	 * @param simlationConfiguration
	 */
	public SimulationManager(QualityMeasurement qualityMeasurement,
			SimulationConfiguration simlationConfiguration) {
		this.qualityMeasurement = qualityMeasurement;
		simulationConfiguration = simlationConfiguration;
		meanDistanceValue = Double.NaN;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param simulationManager
	 */
	public SimulationManager(SimulationManager simulationManager) {
		this(simulationManager.getQualityMeasurement(), simulationManager.getSimulationConfiguration());
		distanceValues = simulationManager.getDistanceValues();
		meanDistanceValue = simulationManager.getMeanDistanceValue();
	}

	/**
	 * Adds a PropertyChangeListener to the list of listeners.
	 * 
	 * @param listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new SimulationManager(this);
	}

	/**
	 * Computes the model quality with the values stored in the quality
	 * measurement class.
	 * 
	 * @throws SBMLException
	 * @throws IntegrationException
	 * @throws ModelOverdeterminedException
	 */
	public void computeModelQuality() throws SBMLException, DerivativeException,
	ModelOverdeterminedException {

		distanceValues = new double[qualityMeasurement.getMeasurements().size()];
		meanDistanceValue = 0d;

		if ((solution != null) && (qualityMeasurement.getMeasurements().size() > 0)) {
			for (int i = 0; i != qualityMeasurement.getMeasurements().size(); i++) {
				distanceValues[i] = qualityMeasurement.getDistance().distance(solution, qualityMeasurement.getMeasurements().get(i));
			}
			meanDistanceValue = Mathematics.mean(distanceValues);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object object) {
		// Check if the given object is a pointer to precisely the same object:
		if (super.equals(object)) {
			return true;
		}
		// Check if the given object is of identical class and not null:
		if ((object == null) || (!getClass().equals(object.getClass()))) {
			return false;
		}
		if (object instanceof SimulationManager) {
			SimulationManager manager = (SimulationManager) object;
			boolean equal = true;
			equal &= Arrays.equals(manager.getDistanceValues(), getDistanceValues());
			equal &= isSetQualityMeasurement() == manager.isSetQualityMeasurement();
			if (equal && isSetQualityMeasurement()) {
				equal &= manager.getQualityMeasurement()
						.equals(getQualityMeasurement());
			}
			equal &= isSetSimulationConfiguration() == manager
					.isSetSimulationConfiguration();
			if (equal && isSetSimulationConfiguration()) {
				equal &= getSimulationConfiguration().equals(
						manager.getSimulationConfiguration());
			}
			return equal;
		}
		return false;
	}

	/**
	 * @return the distanceValues
	 */
	public double[] getDistanceValues() {
		return distanceValues;
	}

	/**
	 * @return the meanDistanceValue
	 */
	public double getMeanDistanceValue() {
		return meanDistanceValue;
	}

	/**
	 * @return the qualityMeasurement
	 */
	public QualityMeasurement getQualityMeasurement() {
		return qualityMeasurement;
	}

	/**
	 * @return the simlationConfiguration
	 */
	public SimulationConfiguration getSimulationConfiguration() {
		return simulationConfiguration;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 769;
		int hashCode = getClass().getName().hashCode();
		if (distanceValues.length > 0) {
			hashCode += prime * Arrays.hashCode(distanceValues);
		}
		if (isSetQualityMeasurement()) {
			hashCode += prime * qualityMeasurement.hashCode();
		}
		if (isSetSimulationConfiguration()) {
			hashCode += prime * simulationConfiguration.hashCode();
		}
		return hashCode;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isSetQualityMeasurement() {
		return qualityMeasurement != null;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isSetSimulationConfiguration() {
		return simulationConfiguration != null;
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("progress")) {
			pcs.firePropertyChange(evt);
		} else if (evt.getPropertyName().equals("done") && (simworker != null)
				&& !simworker.isCancelled()) {
			try {
				solution = simworker.get();
				computeModelQuality();
			} catch (Exception e) {
				logger.log(Level.WARNING, e.getLocalizedMessage());
			}
		}
		pcs.firePropertyChange(evt);
	}

	/**
	 * Removes a PropertyChangeListener from the list of listeners.
	 * 
	 * @param listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	/**
	 * Performs a simulation with the values stored in the simulation
	 * configuration class.
	 * 
	 * @throws Exception
	 */
	public void simulate() throws Exception {
		simworker = new SimulationWorker(simulationConfiguration);
		simworker.addPropertyChangeListener(this);
		simworker.execute();
	}

	/**
	 * Performs a simulation with the values stored in the simulation
	 * configuration class.
	 * 
	 * @throws Exception
	 */
	public void simulateWithoutGUI(double defaultCompartmentValue, double defaultSpeciesValue, double defaultParameterValue) throws Exception {
		// TODO: The purpose of the SimulationManager is to be independent from any GUI!
		SBMLinterpreter interpreter = new SBMLinterpreter(
				simulationConfiguration.getModel(), defaultCompartmentValue, defaultSpeciesValue, defaultParameterValue);
		solution = SimulationWorker.solveByStepSize(
				simulationConfiguration.getSolver(), interpreter,
				interpreter.getInitialValues(), simulationConfiguration.getStart(),
				simulationConfiguration.getEnd(), simulationConfiguration.getStepSize(),
				simulationConfiguration.isIncludeReactions(),
				simulationConfiguration.getAbsTol(), simulationConfiguration.getRelTol());
		pcs.firePropertyChange("done", null, solution);
	}

	/**
	 * 
	 * @return
	 */
	public boolean stopSimulation() {
		ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");
		if (simworker != null) {
			if (simworker.cancel(true)) {
				return true;
			}
			logger.warning(bundle.getString("SIMULATION_COULD_NOT_BE_CANCELED"));
		} else {
			logger.warning(bundle.getString("NO_RUNNING_SIMULATION"));
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append('[');
		sb.append("simulationConfiguration=[");
		sb.append(isSetSimulationConfiguration() ? simulationConfiguration.toString() : "null");
		sb.append(']');
		sb.append(",qualityMeasurement=[");
		sb.append(isSetQualityMeasurement() ? qualityMeasurement.toString() : "null");
		sb.append(']');
		sb.append(",distanceValues=[");
		sb.append(Arrays.toString(distanceValues));
		sb.append("]]");
		return sb.toString();
	}

}
