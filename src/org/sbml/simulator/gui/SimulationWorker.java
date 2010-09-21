/**
 * 
 */
package org.sbml.simulator.gui;

import java.awt.Component;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.simulator.math.Distance;
import org.sbml.simulator.math.SBMLinterpreter;
import org.sbml.simulator.math.odes.AbstractDESSolver;
import org.sbml.simulator.math.odes.DESSolver;
import org.sbml.simulator.math.odes.IntegrationException;
import org.sbml.simulator.math.odes.MultiBlockTable;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-17
 */
public class SimulationWorker extends SwingWorker<MultiBlockTable, Object> {

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
	public static MultiBlockTable solveByStepSize(DESSolver solver,
			Model model, double t1, double t2, double stepSize,
			boolean includeReactions, Component parent)
			throws ModelOverdeterminedException, SBMLException,
			IntegrationException {
		SBMLinterpreter interpreter = new SBMLinterpreter(model);
		solver.setStepSize(stepSize);
		if (solver instanceof AbstractDESSolver) {
			((AbstractDESSolver) solver)
					.setIncludeIntermediates(includeReactions);
		}
		MultiBlockTable solution = solver.solve(interpreter, interpreter
				.getInitialValues(), t1, t2);
		if (solver.isUnstable()) {
			JOptionPane.showMessageDialog(parent, "Unstable!",
					"Simulation not possible", JOptionPane.WARNING_MESSAGE);
		}
		return solution;
	}

	/**
	 * Pointer to experimental data
	 */
	private MultiBlockTable data;
	/**
	 * The currently used distance function.
	 */
	private Distance distance;
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

	/**
	 * 
	 * @param model
	 */
	public SimulationWorker(Model model) {
		this.model = model;
	}

	/**
	 * 
	 * @return
	 * @throws SBMLException
	 * @throws IntegrationException
	 * @throws ModelOverdeterminedException
	 */
	public double computeDistance() throws SBMLException, IntegrationException,
			ModelOverdeterminedException {
		return distance.distance(solveAtTimePoints(solver, model, data
				.getTimePoints(), solver.getStepSize(), parent), data);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.SwingWorker#doInBackground()
	 */
	@Override
	protected MultiBlockTable doInBackground() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.SwingWorker#done()
	 */
	@Override
	protected void done() {
		// TODO
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
	public Distance getDistance() {
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
	public void setDistance(Distance distance) {
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

}
