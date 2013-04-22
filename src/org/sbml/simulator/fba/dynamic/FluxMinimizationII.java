/*
 * $Id:  FluxMinimizationII.java 00:24:09 faehnrich$
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
package org.sbml.simulator.fba.dynamic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.util.compilers.FindUnitsCompiler;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;
import org.sbml.simulator.stability.math.StoichiometricMatrix;
import org.simulator.math.odes.MultiTable;


import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

/**
 * FluxMinimizationII target function:
 * minimize (lambda_1 * ||J|| +
 * lambda_2 * Sum_k^m [c_k(t_i+1) -(c_k(t_i) + N*J*delta_t)])
 * 
 * @author Robin F&auml;hnrich
 * @version $Rev$
 * @since 1.0
 */
public class FluxMinimizationII extends TargetFunction {
	
	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(FluxMinimizationII.class.getName());
	
//	/**
//	 * The expanded SBML document, in fact:
//	 * - transport reactions eliminated and reversible reactions split,
//	 * - compensated reactions added (computed from system boundaries)
//	 */
//	protected SBMLDocument expandedDocument;
	
	/**
	 * contains the {@link SBMLDocument} with all reaction including transports, 
	 * which are splitted in case of reversibility
	 */
	protected SBMLDocument splittedDocument;
	
//	/**
//	 * The complete internal {@link StoichiometricMatrix} N (with system
//	 * boundaries)
//	 */
//	protected StoichiometricMatrix N_int_sys;

	/**
	 * The complete {@link StoichiometricMatrix} N including transport reactions
	 */
	protected StoichiometricMatrix N_all;

	/**
	 * These numbers (lambda_i, i is el. of {1, 2}) weight the contributions
	 * of each term in the optimization problem.
	 */
	protected double lambda_1 = 1;
	
	protected double lambda_2 = 1000;
	
	/**
	 * The array contains the complete interpolated concentrations
	 */
	protected double[][] completeConcentrations;
		
	/**
	 * The array contains the complete interpolated fluxes
	 */
	protected double[][] completeNetFluxes;
	
	/**
	 * The estimated concentrations for the previous time point.
	 */
	protected double[] previousEstimatedConcentrations;
		
	/**
	 * This object saves the optimized solution, in fact:
	 * - the optimized fluxes in a double[]
	 * - the optimized concentrations in a double[]
	 */
	protected double[][] optimizedSolution;
	
	/**
	 * Lower bounds for CPLEX variables saved in a double array
	 */
	protected double[] lowerBounds = null;
	
	/**
	 * Upper bounds for CPLEX variables saved in a double array
	 */
	protected double[] upperBounds = null;
	
	/**
	 * flux lower bound
	 */
	protected double fluxLow = 0;
	
	/**
	 * flux upper bound
	 */
	protected double fluxUp = 200;
	
	/**
	 * concentration lower bound
	 */
	protected double concLow = 0;
	
	/**
	 * concentration upper bound
	 */
	protected double concUp = 3000;
	
	/**
	 * Constraint J_j >= 0
	 */
	protected boolean constraintJ0 = true;
	
	/**
	 * 
	 * @return true if constraint J_j >= 0 is enabled
	 */
	protected boolean isConstraintJ0() {
		return constraintJ0;
	}
	
	/**
	 * Enable/Disable constraint J_j >= 0
	 * 
	 * @param b
	 */
	public void setConstraintJ0(boolean b) {
		this.constraintJ0 = b;
	}
	
	
	/**
	 * Constraint z_m (t_i+1) >= 0
	 */
	protected boolean constraintZm = true;
	
	/**
	 * 
	 * @return true if constraint z_m (t_i+1) >= 0 is enabled
	 */
	protected boolean isConstraintZm() {
		return constraintZm;
	}
	
	/**
	 * Enable/Disable constraint z_m (t_i+1) >= 0
	 * 
	 * @param b
	 */
	public void setConstraintZm(boolean b) {
		this.constraintZm = b;
	}
	
	/**
	 * constraint supporting non-zero fluxes
	 */
	protected boolean fluxDynamic = true;
	
	/**
	 * 
	 * @return true if constraint of a dynamic flux is enabled
	 */
	protected boolean isFluxDynamic() {
		return fluxDynamic;
	}
	
	/**
	 * Disable/Enable constraint for supporting non-zero fluxes
	 * 
	 * @param b
	 */
	public void setFluxDynamic(boolean b) {
		this.fluxDynamic = b;
	}
	
	/**
	 * true if previous estimation should be used
	 */
	protected boolean usePreviousEstimations = false;
	
	/**
	 * 
	 * @param true if previous estimation should be used
	 */
	public void setUsePreviousEstimations(boolean b) {
		this.usePreviousEstimations = b;
	}
	/**
	 * Set the fluxes weighting factor lambda1 (default: 1.0).
	 * 
	 * @param lambda1
	 */
	public void setLambda1(double lambda1) {
		this.lambda_1 = lambda1;
	}
	
	/**
	 * Set the concentrations weighting factor lambda2 (default: 10.0).
	 * 
	 * @param lambda2
	 */
	public void setLambda2(double lambda2) {
		this.lambda_2 = lambda2;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#setInterpolatedConcentrations(double[][])
	 */
	@Override
	public void setInterpolatedConcentrations(double[][] concentrations) {
		this.completeConcentrations = concentrations;
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#setInterpolatedFluxes(double[][])
	 */
	@Override
	public void setInterpolatedFluxes(double[][] fluxes) {
		this.completeNetFluxes = fluxes;
		
	}
	
	/**
	 * contains the previous factors for reactions
	 */
	protected double[] factors = null;
	
	/**
	 * @param factors
	 */
	public void setFactors(double[] factors) {
		this.factors = factors;
	}
	
	/**
	 * contains the indices of paired fluxes
	 */
	protected String[] fluxPairs = null;
	
	/**
	 * @param fluxPairs
	 */
	public void setFluxPairs(String[] fluxPairs) {
		this.fluxPairs = fluxPairs;
	}
	
	/**
	 * contains the rules for transport fluxes
	 */
	protected String[] transportFluxes = null;
	
	/**
	 * @param transportFluxes
	 */
	public void setTransportFluxes(String[] transportFluxes) {
		this.transportFluxes = transportFluxes;
	}
	
	protected Double conversionFactor = null;
	
	/**
	 * @param conversionFactor
	 */
	public void setConversionFactor(double conversionFactor) {
		this.conversionFactor = conversionFactor;
	}
	
	/**
	 * true if little flux changes should be supported, but not to big changes
	 */
	protected boolean littleFluxChanges = true;
	
	/**
	 * boolean for supporting little flux changes
	 * @return true if little flux changes should be supported, but not to big changes
	 */
	public boolean isLittleFluxChanges() {
		return littleFluxChanges;
	}
	
	/**
	 * sets the boolean for supporting little flux changes
	 * @param littleFluxChanges
	 */
	public void setLittleFluxChanges(boolean littleFluxChanges) {
		this.littleFluxChanges = littleFluxChanges;
	}


	/**
	 * maps the j-th reaction of the {@param splittedDocument} to the known fluxes
	 */
	public Map<Integer, double[]> knownFluxes = new HashMap<Integer, double[]>();
	
	/**
	 * 
	 * @param map containing the known fluxes
	 */
	public void setKnownFluxes(Map<Integer, double[]> map) {
		knownFluxes = map;
	}
	
	/**
	 * boolean for supporting use of known fluxes in the given multitable
	 */
	protected boolean useKnownFluxes = true;
	
	/**
	 * Disable/Enable use of known fluxes from the given multitable
	 * @param b
	 */
	public void setUseKnownFluxes(boolean b) {
		this.useKnownFluxes = b;
	}
	
//	protected MultiTable workingSolutionMultiTable;
	
	/**
	 * Prepare the FluxMinimization by setting the:
	 * - splitted SBML document
	 * - matrix N_all
	 * 
	 * @throws Exception
	 */
	protected void prepareFluxMinimizationII() throws Exception {
			this.splittedDocument = FluxMinimizationUtils.getSplittedDocument(DynamicFBA.originalDocument);
			this.N_all = FluxMinimizationUtils.getStoichiometricMatrix(splittedDocument);
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#isMinProblem()
	 */
	@Override
	public boolean isMinProblem() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#isMaxProblem()
	 */
	@Override
	public boolean isMaxProblem() {
		return false;
	}


	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getTargetVariablesIds()
	 */
	@Override
	public String[][] getTargetVariablesIds() {
		// Species Ids for the concentrations block
		ListOf<Species> listOfSpecies = this.splittedDocument.getModel().getListOfSpecies();
		int speciesCount = this.splittedDocument.getModel().getSpeciesCount();
		String[] speciesIds = new String[speciesCount];
		for (int i = 0; i < speciesCount; i++) {
			speciesIds[i] = listOfSpecies.get(i).getId();
		}

		// Reaction Ids for the fluxes and gibbs energies block
		ListOf<Reaction> listOfReactions = this.splittedDocument.getModel().getListOfReactions();
		int reactionCount = this.splittedDocument.getModel().getReactionCount();
		String[] reactionIds = new String[reactionCount];
		for (int i = 0; i < reactionCount; i++) {
			reactionIds[i] = listOfReactions.get(i).getId();
		}

		String[][] targetVariablesIds = new String[2][];
		targetVariablesIds[0] = speciesIds; // The concentration ID's
		targetVariablesIds[1] = reactionIds; // The flux vector ID's

		return targetVariablesIds;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getTargetVariablesLengths()
	 */
	@Override
	public int[] getTargetVariablesLengths() {
		int reactionCount = this.splittedDocument.getModel().getReactionCount();
		int speciesCount = this.splittedDocument.getModel().getSpeciesCount();
		
		int[] targetVariablesLength = new int[2];
		targetVariablesLength[0] = reactionCount; //variables for the fluxes
		targetVariablesLength[1] = speciesCount; //variables for the concentrations
		
		return targetVariablesLength;
	}
	
	/**
	 * initializes the lower and upper bounds according the reaction and species count.
	 * This method does not check, if the bounds are already have been set. 
	 */
	public void initializeBounds() {
			int fullVariableLength = 0;
			for (int i = 0; i < getTargetVariablesLengths().length; i++) {
				fullVariableLength += getTargetVariablesLengths()[i];
			}

			this.lowerBounds = new double[fullVariableLength];
			this.upperBounds = new double[fullVariableLength];
	}
	
	/**
	 * Set default bounds for each part of the target function that will be optimized.
	 */
	public void setDefaultBounds() {
		initializeBounds();
		// 1. Flux value bounds
		setFluxBounds();
		// 2. Concentration bounds
		setConcentrationBounds();
	}
	
	/**
	 * set the lower and upper bounds of fluxes
	 * @param fluxLow
	 * @param fluxUp
	 */
	public void setFluxLowerAndUpperBounds(double fluxLow, double fluxUp) {
		this.fluxLow = fluxLow;
		this.fluxUp = fluxUp;
	}
	
	/**
	 * set the lower and upper bounds of concentrations
	 * @param concLow
	 * @param concUp
	 */
	public void setConcentrationLowerAndUpperBounds(double concLow, double concUp) {
		this.concLow = concLow;
		this.concUp = concUp;
	}
	
	/**
	 * set the default lower and upper flux bounds
	 */
	public void setFluxBounds() {
		int fluxCount = getTargetVariablesLengths()[0];
		for (int i = 0; i < fluxCount; i++) {
			this.lowerBounds[i] = this.fluxLow;
			this.upperBounds[i] = this.fluxUp;
		}
	}
	
	/**
	 * set the default lower and upper concentration bounds
	 */
	public void setConcentrationBounds() {
		int concentrationPosition = getTargetVariablesLengths()[0];
		int concCount = concentrationPosition + getTargetVariablesLengths()[1];
		for (int i = concentrationPosition; i < concCount; i++) {
			this.lowerBounds[i] = this.concLow;
			this.upperBounds[i] = this.concUp;
		}
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#initCplexVariables(ilog.cplex.IloCplex)
	 */
	@Override
	public void initCplexVariables(IloCplex cplex) throws IloException {
		// Prepare the FluxMinimization
		try {
			prepareFluxMinimizationII();
		} catch (Exception e) {
			e.printStackTrace();
		}

		int fullVariableLength = 0;
		for (int i = 0; i < getTargetVariablesLengths().length; i++) {
			fullVariableLength += getTargetVariablesLengths()[i];
		}

		// Set default bounds
		setDefaultBounds();

		IloNumVar[] variables = cplex.numVarArray(fullVariableLength, this.lowerBounds, this.upperBounds);

		// Set the variables
		setVariables(variables);
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#createTargetFunction(ilog.cplex.IloCplex)
	 */
	@Override
	public IloNumExpr createTargetFunction(IloCplex cplex) throws IloException {
		IloNumExpr function = cplex.numExpr();
		
		// One variable for all fluxes in the flux vector
		IloNumExpr flux = cplex.numExpr();
		int fluxPosition = 0;
		// Manhattan norm included
		for (int j = 0; j < getTargetVariablesLengths()[0]; j++) {
			flux = cplex.sum(flux, cplex.abs(getVariables()[fluxPosition + j]));
		}
		
		// Concentrations 
		IloNumExpr concentrations = cplex.numExpr();
		int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
		double[] c_k_ti = this.completeConcentrations[this.getTimePointStep()];
		
		for (int i = 0; i < getTargetVariablesLengths()[1]; i++) {
			IloNumExpr optimizingConcentration = cplex.numExpr();
			
			if (!Double.isNaN(c_k_ti[i])) {
				optimizingConcentration = cplex.abs(cplex.diff(c_k_ti[i], getVariables()[concentrationPosition + i]));
			} else {
				// TODO if c_m_measured[n] is NaN???
			}
			
			concentrations = cplex.sum(concentrations, optimizingConcentration);
		}
		
		// Sum up each term
		function = cplex.sum(cplex.prod(this.lambda_1, flux), cplex.prod(this.lambda_2, concentrations));
		return function;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#addConstraintsToTargetFunction(ilog.cplex.IloCplex)
	 */
	@Override
	public void addConstraintsToTargetFunction(IloCplex cplex) throws IloException {
		int speciesCount = this.splittedDocument.getModel().getSpeciesCount();

		int fluxPosition = 0;
		int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
		
		
		// Constraint J_j >= 0
		if (this.constraintJ0 == true) {
			for (int j = 0; j < getTargetVariablesLengths()[0]; j++) {
				cplex.addGe(getVariables()[fluxPosition + j], 0);
			}
		}
		
		for (int j = 0; j < getTargetVariablesLengths()[0]; j++) {
			// Flux J_j
			if ((knownFluxes.containsKey(j)) && (this.getTimePointStep() > 0)) {
				double eightyPercent = knownFluxes.get(j)[this.getTimePointStep()-1];
				
				IloNumExpr j_j_min = cplex.numExpr();
				if (FluxMinimizationUtils.reverseReaction.containsKey(j)) {
					j_j_min = cplex.diff(
						getVariables()[fluxPosition + j],
						getVariables()[fluxPosition
								+ FluxMinimizationUtils.reverseReaction.get(j)]);
				} else {
					j_j_min = getVariables()[fluxPosition + j];
				}
				
				if (eightyPercent >= 0) {
					cplex.addEq(j_j_min, eightyPercent);
				} else {
					cplex.addEq(j_j_min, eightyPercent);
				}
			}
		}
			
		// Constraint z_m (t_i+1) >= 0
		if (this.constraintZm == true) {
			for (int m = 0; m < speciesCount; m++) {
				// Concentration z_m (t_i+1)
				cplex.addGe(getVariables()[concentrationPosition + m], 0);
			}
		}
		
		// Computation of z_m (t_i+1)

		// Use this computation of delta_t:
		// Only if each timepoint has the same distance to its neighboring timepoint
		double delta_t = DynamicFBA.dFBATimePoints[1] - DynamicFBA.dFBATimePoints[0];
		if(this.getTimePointStep() > 0) {
			delta_t = DynamicFBA.dFBATimePoints[this.getTimePointStep()] - DynamicFBA.dFBATimePoints[this.getTimePointStep()-1];
		}

		for (int n = 0; n < getTargetVariablesLengths()[1]; n++) {

			if (this.getTimePointStep() == 0) {
				// In the first time point step 0, there is no c_m (t_i+1).
				// t_i+1 would be time point step 1, not 0!
				if (!Double.isNaN(this.completeConcentrations[this.getTimePointStep()][n])) {
					cplex.addEq(getVariables()[concentrationPosition + n], this.completeConcentrations[this.getTimePointStep()][n]);
				} else {
					// TODO if currentConcentrations[n] is NaN???
				}
			} else {
				// In all other cases
				if (!Double.isNaN(completeConcentrations[this.getTimePointStep()-1][n])) {
					IloNumExpr computedConcentration = cplex.numExpr();
					IloNumExpr NJ = cplex.numExpr();

					double[] currentN_row = this.N_all.getRow(n);
					for (int col = 0; col < this.N_all.getColumnDimension(); col++) {
						NJ = cplex.sum(NJ, cplex.prod(currentN_row[col], getVariables()[fluxPosition + col]));
					}
					
					computedConcentration = cplex.sum(this.completeConcentrations[this.getTimePointStep()-1][n], cplex.prod(NJ, delta_t));
					cplex.addEq(getVariables()[concentrationPosition + n], computedConcentration);
				} else {
					// TODO if last concentration (completeConcentrations[this.getTimePointStep()-1][n]) is NaN???
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getOptimizedSolution()
	 */
	@Override
	public double[][] getOptimizedSolution() {
		double[] solution = getSolution();
		this.optimizedSolution = new double[2][];
		
		if (solution == null) {
			logger.warning("Can't get optimized solution! Solution doesn't exist!");
		} else {
			// 1. Flux vector assignment
			int fluxPosition = 0;
			HashMap<String, Number> fluxMap = new HashMap<String, Number>();
			ListOf<Reaction> listOfReactions = this.splittedDocument.getModel().getListOfReactions();
			double[] optimizedFluxVector = new double[getTargetVariablesLengths()[0]];
			for (int j = 0; j < getTargetVariablesLengths()[0]; j++) {
				optimizedFluxVector[j] = solution[fluxPosition + j]; // J_j of the formula
				fluxMap.put(listOfReactions.get(j).getId(), optimizedFluxVector[j]);
			}
			System.out.println(fluxMap.toString());
			this.optimizedSolution[1] = optimizedFluxVector; // 2nd position: flux vector
			
			// 2. Concentration vector assignment
			int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
			HashMap<String, Number> concMap = new HashMap<String, Number>();
			HashMap<String, Number> concMap2 = new HashMap<String, Number>();
			ListOf<Species> listOfSpecies = this.splittedDocument.getModel().getListOfSpecies();
			double[] optimizedConcentrations = new double[getTargetVariablesLengths()[1]];  // z_m of the formula
			for (int i = 0; i < getTargetVariablesLengths()[1]; i++) {
				optimizedConcentrations[i] = solution[concentrationPosition + i];
				concMap.put(listOfSpecies.get(i).getId(), optimizedConcentrations[i]);
				concMap2.put(listOfSpecies.get(i).getId(), this.completeConcentrations[this.getTimePointStep()][i]);
			}
			this.optimizedSolution[0] = optimizedConcentrations; // 1st position: concentrations
		}
		return this.optimizedSolution;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#saveValuesForCurrentTimePoint(org.simulator.math.odes.MultiTable)
	 */
	public void saveValuesForCurrentTimePoint(MultiTable workingSolutionMultiTable) {
		// for concentration
		double[] currentSpecificSolution = optimizedSolution[0];
		for(double value: currentSpecificSolution) {
			if(value <= 0) {
				System.out.println();
			}
		}
		workingSolutionMultiTable.getBlock(0).setRowData(this.getTimePointStep(), currentSpecificSolution);
		// save previous estimated concentrations
		this.previousEstimatedConcentrations = currentSpecificSolution;
		
		// for fluxes
		double[] currentFluxSolution = optimizedSolution[1];
		if(this.getTimePointStep() > 0){
			workingSolutionMultiTable.getBlock(1).setRowData(this.getTimePointStep() - 1, currentFluxSolution);
		}
		// for the last time point
		if(this.getTimePointStep() == workingSolutionMultiTable.getRowCount() - 1) {
			double[] nanArray = new double[optimizedSolution[1].length];
			Arrays.fill(nanArray, Double.NaN);
			workingSolutionMultiTable.getBlock(1).setRowData(this.getTimePointStep(), nanArray);
		}
//		this.workingSolutionMultiTable = workingSolutionMultiTable;

	}
	
	

	
}
