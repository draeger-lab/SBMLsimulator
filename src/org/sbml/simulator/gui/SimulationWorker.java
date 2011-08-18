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
package org.sbml.simulator.gui;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.simulator.math.QualityMeasure;
import org.sbml.simulator.math.SBMLinterpreter;
import org.sbml.simulator.math.odes.AbstractDESSolver;
import org.sbml.simulator.math.odes.DESSolver;
import org.sbml.simulator.math.odes.IntegrationException;
import org.sbml.simulator.math.odes.MultiBlockTable;

/**
 * @author Andreas Dr&auml;ger
 * @author Philip Stevens
 * @author Max Zwießele
 * @date 2010-09-17
 * @version $Rev$
 * @since 1.0
 */
public class SimulationWorker extends SwingWorker<Void, Void> implements PropertyChangeListener{

	/**
	 * Pointer to experimental data
	 */
	private MultiBlockTable data;
	/**
	 * The currently used distance function.
	 */
	private QualityMeasure distance;
	/**
	 * 
	 */
	private boolean includeReactions;

	/**
	 * Pointer to a {@link Model} that is to be simulated.
	 */
	private Model model;

	/**
	 * 
	 */
	private Component parent;

	/**
	 * The integrator for the simulation
	 */
	private DESSolver solver;

	private double stepSize;
	private double timeEnd;
	private double timeStart;
	private SimulationPanel simulationPanel;

	/**
	 * constructor
	 * @param solver2
	 * @param model2
	 * @param t1val
	 * @param t2val
	 * @param stepSize
	 * @param includeReactions2
	 * @param simulationPanel
	 */
	public SimulationWorker(DESSolver solver, Model model, double t1val,
			double t2val, double stepSize, boolean includeReactions,
			SimulationPanel simulationPanel) {
		this.solver =  solver;
		this.model = model;
		this.timeStart = t1val;
		this.timeEnd = t2val;
		this.stepSize = stepSize;
		this.includeReactions = includeReactions;
		this.simulationPanel = simulationPanel;
	}
	
	/**
	 * 
	 * @param model
	 * @param timePoints
	 * @param stepSize
	 * @return
	 * @throws SBMLException
	 * @throws IntegrationException
	 * @throws ModelOverdeterminedException
	 */
	public static MultiBlockTable solveAtTimePoints(DESSolver solver,
			Model model, double times[], double stepSize, Component parent)
			throws SBMLException, IntegrationException,
			ModelOverdeterminedException {
		SBMLinterpreter interpreter = new SBMLinterpreter(model);
		solver.setStepSize(stepSize);
		if (solver instanceof AbstractDESSolver) {
			((AbstractDESSolver) solver).setIncludeIntermediates(false);
		}
		MultiBlockTable solution = solver.solve(interpreter, interpreter
				.getInitialValues(), times);
		if (solver.isUnstable()) {
			JOptionPane.showMessageDialog(parent, "Unstable!",
					"Simulation not possible", JOptionPane.WARNING_MESSAGE);
		}
		return solution;
	}

	/**
	 * 
	 * @param solver
	 * @param model
	 * @param t1
	 *            Begin time
	 * @param t2
	 *            End time.
	 * @param stepSize
	 * @param includeReactions
	 * @param parent
	 * @return
	 * @throws ModelOverdeterminedException
	 * @throws SBMLException
	 * @throws IntegrationException
	 */
	public MultiBlockTable solveByStepSize()
			throws ModelOverdeterminedException, SBMLException,
			IntegrationException {
		
		SBMLinterpreter interpreter = new SBMLinterpreter(model);
		solver.addPropertyChangeListener(this);
		solver.setStepSize(stepSize);
		if (solver instanceof AbstractDESSolver) {
			((AbstractDESSolver) solver)
					.setIncludeIntermediates(includeReactions);
		}
		MultiBlockTable solution = solver.solve(interpreter, interpreter
				.getInitialValues(), timeStart, timeEnd);
		if (solver.isUnstable()) {
			JOptionPane.showMessageDialog(parent, "Unstable!",
					"Simulation not possible", JOptionPane.WARNING_MESSAGE);
		}
		solver.removePropertyChangeListener(this);
		return solution;
	}

	
	/**
	 * 
	 * @param model
	 */
	public SimulationWorker(Model model) {
		if (model == null) {
			throw new NullPointerException("Model is null");
		}
		this.model = model;
	}


	/**
	 * 
	 * @return
	 * @throws SBMLException
	 * @throws IntegrationException
	 * @throws ModelOverdeterminedException
	 */
	public double computeQuality() throws SBMLException, IntegrationException,
			ModelOverdeterminedException {
		return distance.distance(solveAtTimePoints(solver, model, data
				.getTimePoints(), solver.getStepSize(), parent), data);
	}
	
	/**
	 * 
	 * @param stepSize
	 */
	public void setStepSize(double stepSize) {
		solver.setStepSize(stepSize);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.SwingWorker#doInBackground()
	 */
	@Override
	protected Void doInBackground() throws Exception {
		data = solveByStepSize();
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.SwingWorker#done()
	 */
	@Override
	protected void done() {
		firePropertyChange("done", 0, data);
	}

	/**
	 * @return the data
	 */
	public MultiBlockTable getData() {
		return data;
	}

	/**
	 * 
	 * @return
	 */
	public DESSolver getDESSolver() {
		return solver;
	}

	/**
	 * @return the distance
	 */
	public QualityMeasure getQualityMeasure() {
		return distance;
	}

	/**
	 * @return the model
	 */
	public Model getModel() {
		return model;
	}

	/**
	 * @return the parent
	 */
	public Component getParent() {
		return parent;
	}

	/**
	 * @return the includeReactions
	 */
	public boolean isIncludeReactions() {
		return includeReactions;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isSetData() {
		return data != null;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(MultiBlockTable data) {
		this.data = data;
	}

	/**
	 * 
	 * @param solver
	 */
	public void setDESSolver(AbstractDESSolver solver) {
		this.solver = solver;
	}

	/**
	 * @param distance
	 *            the distance to set
	 */
	public void setQualityMeasure(QualityMeasure distance) {
		this.distance = distance;
	}

	/**
	 * @param includeReactions
	 *            the includeReactions to set
	 */
	public void setIncludeReactions(boolean includeReactions) {
		this.includeReactions = includeReactions;
	}

	/**
	 * @param model
	 *            the model to set
	 */
	public void setModel(Model model) {
		this.model = model;
	}

	/**
	 * @param parent
	 *            the parent to set
	 */
	public void setParent(Component parent) {
		this.parent = parent;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isSetModel() {
		return model != null;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isSetSolver() {
	    return solver != null;
	}
	
  /*
   * (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
	public void propertyChange(PropertyChangeEvent evt) {
		getPropertyChangeSupport().firePropertyChange(evt);
	}

}
