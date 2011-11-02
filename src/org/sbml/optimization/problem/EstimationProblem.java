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
package org.sbml.optimization.problem;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.Quantity;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.optimization.QuantityRange;
import org.sbml.simulator.math.QualityMeasure;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.DESSolver;
import org.simulator.math.odes.MultiBlockTable;
import org.simulator.sbml.SBMLinterpreter;

import eva2.server.go.PopulationInterface;
import eva2.server.go.populations.Population;
import eva2.server.go.problems.AbstractProblemDouble;
import eva2.server.go.problems.InterfaceAdditionalPopulationInformer;
import eva2.server.go.problems.InterfaceHasInitRange;
import eva2.tools.ToolBox;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-08-24
 * @version $Rev$
 * @since 1.0
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
	private transient DESSolver solver = null;

	/**
	 * 
	 */
	private QualityMeasure distance = null;

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
	 * To save computation time during the initialization, the initial ranges
	 * are stored in this 2-dimensional double array when setting the
	 * {@link QuantityRange} field.
	 */
	private double[][] initRanges;
	/**
	 * Switch to decide whether or not to use a multiple shooting strategy.
	 */
	private boolean multishoot;

	/**
	 * 
	 */
	public static final String SIMULATION_DATA = "simulation data";

	/**
	 * 
	 * @param solver
	 * @param distance
	 * @param model
	 * @param referenceData
	 * @param multishoot
	 * @param quantityRanges
	 * @throws ModelOverdeterminedException
	 * @throws SBMLException
	 */
	public EstimationProblem(DESSolver solver, QualityMeasure distance, Model model,
			MultiBlockTable referenceData, boolean multishoot,
			QuantityRange... quantityRanges)
			throws ModelOverdeterminedException, SBMLException {
		this(solver, distance, model, referenceData, quantityRanges);
		this.multishoot = multishoot;
		if (multishoot) {
			System.out.println("Using multiple shooting!");
		} else {
			System.out.println("Using single shooting!");
		}
	}

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
	public EstimationProblem(DESSolver solver, QualityMeasure distance, Model model,
			MultiBlockTable referenceData, QuantityRange... quantityRanges)
			throws ModelOverdeterminedException, SBMLException {
		super();
		multishoot = false;
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
			interpreter.init(false);
			MultiBlockTable solution = multishoot ? solver.solve(interpreter,
					referenceData.getBlock(0), interpreter.getInitialValues())
					: solver.solve(interpreter, interpreter.getInitialValues(),
							referenceData.getTimePoints());
			fitness[0] = distance.distance(solution.getBlock(0), referenceData
					.getBlock(0));
			if (bestPerGeneration == null
					|| (fitness[0] < bestPerGenerationDist)) {
				bestPerGenerationDist = fitness[0];
				bestPerGeneration = solution;
				bestPerGeneration.setName(SIMULATION_DATA);
			}

		} catch (Exception e) {
			e.printStackTrace();
			fitness[0] = Double.POSITIVE_INFINITY;
		}
		double fitnessClone[] = new double[fitness.length];
		System.arraycopy(fitness, 0, fitnessClone, 0, fitness.length);
		return fitnessClone;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eva2.server.go.problems.AbstractOptimizationProblem#evaluatePopulationStart
	 * (eva2.server.go.populations.Population)
	 */
	@Override
	public void evaluatePopulationStart(Population population) {
		super.evaluatePopulationStart(population);
		bestPerGeneration = null;
		bestPerGenerationDist = Double.POSITIVE_INFINITY;
	}

	@Override
	public String[] getAdditionalDataHeader() {
		String[] superHead = super.getAdditionalDataHeader();
		return ToolBox.appendArrays(superHead, SIMULATION_DATA);
	}

	@Override
	public String[] getAdditionalDataInfo() {
		String[] superInfo = super.getAdditionalDataInfo();
		return ToolBox.appendArrays(superInfo,
				"Result of the best per generation model simulation");
	}

	@Override
	public Object[] getAdditionalDataValue(PopulationInterface pop) {
		Object[] superVals = super.getAdditionalDataValue(pop);
		return ToolBox.appendArrays(superVals, bestPerGeneration);
	}

	/**
	 * 
	 * @return
	 */
	public QualityMeasure getDistance() {
		return distance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.server.go.problems.InterfaceHasInitRange#getInitRange()
	 */
	public double[][] getInitRange() {
		return initRanges;
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
	 * 
	 * @return
	 */
	public QuantityRange[] getQuantityRanges() {
		return quantityRanges;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eva2.server.go.problems.AbstractProblemDouble#getRangeLowerBound(int)
	 */
	@Override
	public double getRangeLowerBound(int dim) {
		return quantityRanges[dim].getMinimum();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eva2.server.go.problems.AbstractProblemDouble#getRangeUpperBound(int)
	 */
	@Override
	public double getRangeUpperBound(int dim) {
		return quantityRanges[dim].getMaximum();
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
	 * @return the multishoot
	 */
	public boolean isMultishoot() {
		return multishoot;
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
	public void setDistance(QualityMeasure distance) {
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
	 * @param multishoot
	 *            the multishoot to set
	 */
	public void setMultishoot(boolean multishoot) {
		this.multishoot = multishoot;
	}

	/**
	 * 
	 * @param quantityRanges
	 */
	public void setQuantityRanges(QuantityRange... quantRange) {
		if (check(quantRange)) {
			this.quantityRanges = quantRange;
			this.originalValues = new double[quantityRanges.length];
			initRanges = new double[quantityRanges.length][2];
			for (int i = 0; i < originalValues.length; i++) {
				originalValues[i] = quantityRanges[i].getQuantity().getValue();
				initRanges[i][0] = quantityRanges[i].getInitialMinimum();
				initRanges[i][1] = quantityRanges[i].getInitialMaximum();
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
		this.solver = solver.clone();
		if (solver instanceof AbstractDESSolver) {
			((AbstractDESSolver) this.solver).setIncludeIntermediates(false);
		}
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

}
