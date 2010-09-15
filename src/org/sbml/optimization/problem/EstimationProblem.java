/*
 *  SBMLsqueezer creates rate equations for reactions in SBML files
 *  (http://sbml.org).
 *  Copyright (C) 2009 ZBIT, University of Tübingen, Andreas Dräger
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.sbml.optimization.problem;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.Quantity;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.optimization.QuantityRange;
import org.sbml.simulator.math.Distance;
import org.sbml.simulator.math.SBMLinterpreter;
import org.sbml.simulator.math.odes.DESSolver;
import org.sbml.simulator.math.odes.MultiBlockTable;

import eva2.server.go.PopulationInterface;
import eva2.server.go.populations.Population;
import eva2.server.go.problems.AbstractProblemDouble;
import eva2.server.go.problems.InterfaceAdditionalPopulationInformer;
import eva2.server.go.problems.InterfaceHasInitRange;
import eva2.tools.ToolBox;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-08-24
 */
public class EstimationProblem extends AbstractProblemDouble implements
		InterfaceAdditionalPopulationInformer, InterfaceHasInitRange {

	/**
	 * 
	 */
	private MultiBlockTable bestPerGeneration = null;
	/**
	 * 
	 */
	private double bestPerGenerationDist = Double.POSITIVE_INFINITY;

	/**
	 * Generated version identifier.
	 */
	private static final long serialVersionUID = 8918650806005528506L;

	/**
	 * 
	 * @return
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	/**
	 * 
	 */
	private DESSolver solver = null;

	/**
	 * 
	 */
	private Distance distance = null;

	/**
	 * 
	 */
	private transient QuantityRange[] quantityRanges = null;

	/**
	 * Memorizes the original values of all given {@link Quantity}s to restore
	 * the original state.
	 */
	private double[] originalValues = null;

	/**
	 * 
	 */
	private transient SBMLinterpreter interpreter = null;

	/**
	 * Reference data used to judge the quality of a simulation result.
	 */
	private transient MultiBlockTable referenceData = null;

	/**
	 * An array to store the fitness of a parameter set to avoid multiple
	 * allocations
	 */
	private transient double[] fitness = new double[1];

	/**
	 * 
	 * @param solver
	 * @param distance
	 * @param model
	 * @param referenceData
	 * @param quantityRanges
	 * @throws ModelOverdeterminedException
	 * @throws SBMLException
	 */
	public EstimationProblem(DESSolver solver, Distance distance, Model model,
			MultiBlockTable referenceData, QuantityRange... quantityRanges)
			throws ModelOverdeterminedException, SBMLException {
		super();
		setSolver(solver);
		setDistance(distance);
		setModel(model);
		setReferenceData(referenceData);
		setQuantityRanges(quantityRanges);
	}

	/**
	 * @param problem
	 */
	public EstimationProblem(EstimationProblem problem) {
		super(problem);
		setSolver(problem.getSolver());
		setDistance(problem.getDistance());
		try {
			setModel(problem.getModel());
		} catch (Exception e) {
			// can never happen.
		}
		setQuantityRanges(problem.getQuantityRanges());
	}

	/**
	 * 
	 * @return
	 */
	public QuantityRange[] getQuantityRanges() {
		return quantityRanges;
	}

	/*
	 * (non-Javadoc)
	 * @see eva2.server.go.problems.AbstractProblemDouble#getRangeLowerBound(int)
	 */
	@Override
	public double getRangeLowerBound(int dim) {
		return quantityRanges[dim].getMinimum();
	}

	/*
	 * (non-Javadoc)
	 * @see eva2.server.go.problems.AbstractProblemDouble#getRangeUpperBound(int)
	 */
	@Override
	public double getRangeUpperBound(int dim) {
		return quantityRanges[dim].getMaximum();
	}

	/**
	 * Checks whether or not the given model contains all of the given
	 * quantities.
	 * 
	 * @param quantities
	 * @return
	 */
	private boolean check(QuantityRange... quantityRange) {
		for (int i = 0; i < quantityRange.length; i++) {
			if (!interpreter.getModel().containsQuantity(
					quantityRange[i].getQuantity())) {
				return false;
			}
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.server.go.problems.AbstractOptimizationProblem#clone()
	 */
	@Override
	public EstimationProblem clone() {
		return new EstimationProblem(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.server.go.problems.AbstractProblemDouble#eval(double[])
	 */
	@Override
	public double[] eval(double[] x) {
		for (int i = 0; i < x.length; i++) {
			quantityRanges[i].getQuantity().setValue(x[i]);
		}
		try {
			interpreter.init();
			MultiBlockTable solution = solver.solve(interpreter, interpreter
					.getInitialValues(), referenceData.getTimePoints());
			fitness[0] = distance.distance(solution.getBlock(0), referenceData
					.getBlock(0));
			if (bestPerGeneration == null
					|| (fitness[0] < bestPerGenerationDist)) {
				bestPerGenerationDist = fitness[0];
				bestPerGeneration = solution;
			}

		} catch (Exception e) {
			e.printStackTrace();
			fitness[0] = Double.POSITIVE_INFINITY;
		}
		return fitness;
	}

	@Override
	public void evaluatePopulationStart(Population population) {
		super.evaluatePopulationStart(population);
		bestPerGeneration = null;
		bestPerGenerationDist = Double.POSITIVE_INFINITY;
	}

	@Override
	public String[] getAdditionalFileStringHeader() {
		String[] superHead = super.getAdditionalFileStringHeader();
		return ToolBox.appendArrays(superHead, "simulationData");
	}

	@Override
	public String[] getAdditionalFileStringInfo() {
		String[] superInfo = super.getAdditionalFileStringInfo();
		return ToolBox.appendArrays(superInfo,
				"Result of the best per generation model simulation");
	}

	@Override
	public Object[] getAdditionalFileStringValue(PopulationInterface pop) {
		Object[] superVals = super.getAdditionalFileStringValue(pop);
		return ToolBox.appendArrays(superVals, bestPerGeneration);
	}

	/**
	 * 
	 * @return
	 */
	public Distance getDistance() {
		return distance;
	}

	/**
	 * 
	 * @return
	 */
	public Model getModel() {
		return interpreter.getModel();
	}

	/**
	 * 
	 * @return
	 */
	public double[] getOriginalValues() {
		return originalValues;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.server.go.problems.AbstractProblemDouble#getProblemDimension()
	 */
	@Override
	public int getProblemDimension() {
		return isSetQuantities() ? quantityRanges.length : 0;
	}

	/**
	 * 
	 * @return
	 */
	public Quantity[] getQuantities() {
		Quantity q[] = new Quantity[quantityRanges.length];
		for (int i = 0; i < quantityRanges.length; i++) {
			q[i] = quantityRanges[i].getQuantity();
		}
		return q;
	}

	/**
	 * The matrix of reference data containing the time points in the first
	 * column.
	 * 
	 * @return
	 */
	public MultiBlockTable getReferenceData() {
		return referenceData;
	}

	/**
	 * 
	 * @return
	 */
	public DESSolver getSolver() {
		return solver;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isSetQuantities() {
		return quantityRanges != null;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isSetReferenceData() {
		return referenceData != null;
	}

	/**
	 * Restores the original values of the model as before the model was
	 * modified during the optimization.
	 */
	public void restore() {
		for (int i = 0; i < quantityRanges.length; i++) {
			quantityRanges[i].getQuantity().setValue(originalValues[i]);
		}
	}

	/**
	 * 
	 * @param distance
	 */
	public void setDistance(Distance distance) {
		this.distance = distance;
	}

	/**
	 * 
	 * @param model
	 * @throws SBMLException
	 * @throws ModelOverdeterminedException
	 */
	private void setModel(Model model) throws ModelOverdeterminedException,
			SBMLException {
		interpreter = new SBMLinterpreter(model);
	}

	/**
	 * 
	 * @param quantityRanges
	 */
	public void setQuantityRanges(QuantityRange... quantRange) {
		if (check(quantRange)) {
			this.quantityRanges = quantRange;
			this.originalValues = new double[quantityRanges.length];
			for (int i = 0; i < originalValues.length; i++) {
				originalValues[i] = quantityRanges[i].getQuantity().getValue();
			}
		} else {
			throw new IllegalArgumentException(
					"cannot estimate the values of quantities that are not part of the given model.");
		}
	}

	/**
	 * 
	 * @param referenceData
	 */
	public void setReferenceData(MultiBlockTable referenceData) {
		if ((referenceData != null) && (referenceData.getColumnCount() <= 1)) {
			// time column */+ getModel().getNumSymbols())) {
			throw new IllegalArgumentException(
					"At least for one symbol reference data are required.");
		}
		this.referenceData = referenceData;
	}

	/**
	 * 
	 * @param solver
	 */
	public void setSolver(DESSolver solver) {
		this.solver = solver;
	}

	/**
	 * 
	 */
	public void unsetQuantities() {
		quantityRanges = null;
		originalValues = null;
	}

	/**
	 * 
	 */
	public void unsetReferenceData() {
		referenceData = null;
	}

	@Override
	public Object getInitRange() {
		double[][] initR = new double[getProblemDimension()][2];
		for (int i=0; i<getProblemDimension(); i++) {
			initR[i][0]=quantityRanges[i].getInitialMinimum();
			initR[i][1]=quantityRanges[i].getInitialMaximum();
		}
		return initR;
	}

}
