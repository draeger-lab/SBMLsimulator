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
package org.sbml.optimization.problem;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.apache.commons.math.ode.DerivativeException;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Quantity;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.optimization.QuantityRange;
import org.sbml.simulator.math.PearsonCorrelation;
import org.sbml.simulator.math.QualityMeasure;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.DESSolver;
import org.simulator.math.odes.MultiTable;
import org.simulator.sbml.SBMLinterpreter;

import de.zbit.util.ResourceManager;
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
	
	private static final ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");
	
	/**
	 * 
	 */
	private MultiTable bestPerGeneration = null;
	
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
	private double originalValues[] = null;

	/**
	 * 
	 */
	private transient SBMLinterpreter interpreter = null;

	/**
	 * Reference data used to judge the quality of a simulation result.
	 */
	private transient MultiTable referenceData[] = null;

	/**
	 * An array to store the fitness of a parameter set to avoid multiple
	 * allocations
	 */
	private transient double fitness[] = new double[1];
	
	/**
	 * To save computation time during the initialization, the initial ranges
	 * are stored in this 2-dimensional double array when setting the
	 * {@link QuantityRange} field.
	 */
	private double initRanges[][];
	
	/**
	 * Switch to decide whether or not to use a multiple shooting strategy.
	 */
	private boolean multishoot;

	/**
	 * 
	 */
	private Map<String,Integer> id2Index;

	/**
	 * 
	 */
	private boolean negationOfDistance;
	
	/**
	 * 
	 */
	private double defaultValue;
	
	/**
	 * 
	 */
	public static final String SIMULATION_DATA = "simulation data";
	
	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(EstimationProblem.class.getName());

	/**
	 * 
	 * @param solver
	 * @param distance
	 * @param model
	 * @param list
	 * @param multishoot
	 * @param quantityRanges
	 * @throws ModelOverdeterminedException
	 * @throws SBMLException
	 */
	public EstimationProblem(DESSolver solver, QualityMeasure distance, Model model,
			List<MultiTable> list, boolean multishoot,
			QuantityRange... quantityRanges)
			throws ModelOverdeterminedException, SBMLException {
		this(solver, distance, model, list, quantityRanges);
		this.multishoot = multishoot;
		logger.info(MessageFormat.format(bundle.getString("KIND_OF_SHOOTING"),
			multishoot ? bundle.getString("MULTIPLE") : bundle.getString("SINGLE")));
	}

	/**
	 * 
	 * @param solver
	 * @param distance
	 * @param model
	 * @param list
	 * @param quantityRanges
	 * @throws ModelOverdeterminedException
	 * @throws SBMLException
	 */
	public EstimationProblem(DESSolver solver, QualityMeasure distance,
		Model model, List<MultiTable> list, QuantityRange... quantityRanges)
		throws ModelOverdeterminedException, SBMLException {
		super();
		defaultValue = 10000;
		negationOfDistance=false;
		multishoot = false;
		setSolver(solver);
		setDistance(distance);
		if(distance instanceof PearsonCorrelation) {
			negationOfDistance=true;
		}
		setModel(model);
		setReferenceData(list.toArray(new MultiTable[0]));
		setQuantityRanges(quantityRanges);
		if (id2Index == null) {
			id2Index = new HashMap<String, Integer>();
			String ids[] = interpreter.getIdentifiers();
			for (int i = 0; i < ids.length; i++) {
				id2Index.put(ids[i], Integer.valueOf(i));
			}
			for (int i = 0; i < quantityRanges.length; i++) {
				id2Index.remove(quantityRanges[i].getQuantity().getId());
			}
		}
	}

	/**
	 * @param problem
	 */
	public EstimationProblem(EstimationProblem problem) {
		super(problem);
		defaultValue = 10000;
		setSolver(problem.getSolver());
		negationOfDistance=false;
		setDistance(problem.getDistance());
		if(distance instanceof PearsonCorrelation) {
			negationOfDistance=true;
		}
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

	/* (non-Javadoc)
	 * @see eva2.server.go.problems.AbstractOptimizationProblem#clone()
	 */
	public EstimationProblem clone() {
		return new EstimationProblem(this);
	}

	/* (non-Javadoc)
	 * @see eva2.server.go.problems.AbstractProblemDouble#eval(double[])
	 */
	public double[] eval(double[] x) {
	  
	  for (int i = 0; i < x.length; i++) {
			quantityRanges[i].getQuantity().setValue(x[i]);
		}
		
		try {
		  interpreter.init(false);
			
		  double[] initialValues = interpreter.getInitialValues();
			MultiTable reference = getInitialConditions();
			for (int col = 0; col < reference.getColumnCount(); col++) {
			  String name = reference.getColumnIdentifier(col);
			  Integer pos = id2Index.get(name);
			  if (pos != null) {
			    initialValues[pos] = ((Double) reference.getValueAt(0, col)).doubleValue();
			  }
			}
			MultiTable solution = null; 
			try {
				if (multishoot) {
					solution = solver.solve(interpreter,
						getInitialConditions().getBlock(0), initialValues);
				} else {
					solution = solver.solve(interpreter, initialValues, getTimePoints());
				}
			} catch (DerivativeException e) {
				logger.warning(e.getLocalizedMessage());
			}
			
			fitness[0] = 0d;
			for (MultiTable data : referenceData) {
				if (solution == null) {
					fitness[0] = defaultValue;
				} else {
					// equal weight for each reference data set
					if (negationOfDistance) {
						fitness[0] += -1
								* distance.distance(solution.getBlock(0), data.getBlock(0))
								/ referenceData.length;
					} else {
						fitness[0] += distance.distance(solution.getBlock(0),
							data.getBlock(0))
								/ referenceData.length;
					}
				}
				
			}
			if (bestPerGeneration == null
					|| (fitness[0] < bestPerGenerationDist)) {
				bestPerGenerationDist = fitness[0];
				bestPerGeneration = solution;
				if(solution != null) {
					bestPerGeneration.setName(SIMULATION_DATA);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			fitness[0] = Double.POSITIVE_INFINITY;
		}
		double fitnessClone[] = new double[fitness.length];
		System.arraycopy(fitness, 0, fitnessClone, 0, fitness.length);
		return fitnessClone;
	}

	/**
	 * 
	 * @return
	 */
	public MultiTable getInitialConditions() {
		if ((initConditions == null) && (referenceData.length > 0)) {
			if (referenceData.length == 1) {
				initConditions = referenceData[0];
			} else {
				// merge all identifiers
				ArrayList<String> identifiers = new ArrayList<String>();
				for (String s : referenceData[0].getBlock(0).getIdentifiers()) {
					identifiers.add(s);
				}
				int i, j, index;
				for (i = 1; i < referenceData.length; i++) {
					for (j = 0; j < referenceData[i].getBlock(0).getColumnCount(); j++) {
						index = Arrays.binarySearch((String[]) identifiers.toArray(),
							referenceData[i].getBlock(0).getIdentifiers()[j]);
						if (index < 0) {
							identifiers.add(-index + 1, referenceData[i].getBlock(0).getIdentifiers()[j]);
						}
					}
				}
				// determine the merged time points
				double tp[] = getTimePoints();
				// merge all data points (using a simple mean calculation)
				double data[][] = new double[tp.length][identifiers.size()];
				MultiTable.Block block;
				for (i = 0; i < tp.length; i++) {
					for (j = 0; j < identifiers.size(); j++) {
						for (index = 0; index < referenceData.length; index++) {
							block = referenceData[index].getBlock(0);
							if (block.containsColumn(identifiers.get(j))) {
								data[j][j] += block.getColumn(j).getValue(i) / referenceData.length;
							} else {
								// If one value is not available, this data point will become null! 
								data[j][j] = Double.NaN;
							}
						}
					}
				}
				initConditions = new MultiTable(tp, data, identifiers.toArray(new String[0]));
			}
		}
		return initConditions;
	}

	/**
	 * Memorizes the time points at which measurements are taken, i.e., at which
	 * the integration system must be evaluated.
	 */
	private double timePoints[] = null;
	
	/**
	 * Memorizes the merged initial conditions for multiple data sets in order to 
	 * avoid a multiple creation of it in the {@link #eval(double[])} method. 
	 */
	private MultiTable initConditions = null;
	
	/**
	 * 
	 * @return
	 */
	public double[] getTimePoints() {
		if ((timePoints == null) && (referenceData.length > 0)) {
			// Merge all measurement time points into one double array
			if (referenceData.length == 1) {
				this.timePoints = referenceData[0].getTimePoints();
			} else {
				ArrayList<Double> tp = new ArrayList<Double>();
				for (double d : referenceData[0].getTimePoints()) {
					tp.add(Double.valueOf(d));
				}
				int i, j, index;
				double t;
				for (i = 1; i < referenceData.length; i++) {
					for (j = 0; j < referenceData[i].getTimePoints().length; j++) {
						t = referenceData[i].getTimePoint(j);
						index = Arrays.binarySearch((Double[]) tp.toArray(), t);
						if (index < 0) {
							tp.add(-index + 1, Double.valueOf(t));
						}
					}
				}
				i = 0;
				this.timePoints = new double[tp.size()];
				for (Double d : tp) {
					this.timePoints[i++] = d.doubleValue();
				}
			}
		}
		return timePoints;
	}

	/* (non-Javadoc)
	 * @see eva2.server.go.problems.AbstractOptimizationProblem#evaluatePopulationStart(eva2.server.go.populations.Population)
	 */
	@Override
	public void evaluatePopulationStart(Population population) {
		super.evaluatePopulationStart(population);
		bestPerGeneration = null;
		bestPerGenerationDist = Double.POSITIVE_INFINITY;
	}

	/* (non-Javadoc)
	 * @see eva2.server.go.problems.AbstractProblemDouble#getAdditionalDataHeader()
	 */
	@Override
	public String[] getAdditionalDataHeader() {
		String[] superHead = super.getAdditionalDataHeader();
		return ToolBox.appendArrays(superHead, SIMULATION_DATA);
	}

	/* (non-Javadoc)
	 * @see eva2.server.go.problems.AbstractProblemDouble#getAdditionalDataInfo()
	 */
	@Override
	public String[] getAdditionalDataInfo() {
		String[] superInfo = super.getAdditionalDataInfo();
		return ToolBox.appendArrays(superInfo,
			bundle.getString("RESULT_OF_BEST_PER_GENERATION"));
	}

	/* (non-Javadoc)
	 * @see eva2.server.go.problems.AbstractProblemDouble#getAdditionalDataValue(eva2.server.go.PopulationInterface)
	 */
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

	/* (non-Javadoc)
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

	/* (non-Javadoc)
	 * @see eva2.server.go.problems.AbstractProblemDouble#getProblemDimension()
	 */
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

	/* (non-Javadoc)
	 * @see eva2.server.go.problems.AbstractProblemDouble#getRangeLowerBound(int)
	 */
	@Override
	public double getRangeLowerBound(int dim) {
		return quantityRanges[dim].getMinimum();
	}

	/* (non-Javadoc)
	 * @see eva2.server.go.problems.AbstractProblemDouble#getRangeUpperBound(int)
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
	public MultiTable[] getReferenceData() {
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
				bundle.getString("OPTIMIZATION_TARGET_IS_NOT_PART_OF_THE_MODEL"));
		}
	}

	/**
	 * 
	 * @param referenceData
	 */
	public void setReferenceData(MultiTable... referenceData) {
		if ((referenceData == null) || (referenceData.length < 1)
				|| (referenceData[0].getBlockCount() == 0)
				|| (referenceData[0].getBlock(0).getColumnCount() == 0)) {
			// time column */+ getModel().getNumSymbols())) {
			throw new IllegalArgumentException(
				bundle.getString("MISSING_REFERENCE_DATA"));
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
