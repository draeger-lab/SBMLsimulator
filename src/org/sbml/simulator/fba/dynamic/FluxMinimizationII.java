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
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;
import org.sbml.simulator.stability.math.StoichiometricMatrix;
import org.simulator.math.odes.MultiTable;

import com.sun.org.apache.xpath.internal.axes.ReverseAxesWalker;

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
	protected double lambda_1 = 1.0;
	
	protected double lambda_2 = 1000.0;
	
	/**
	 * The array contains the complete interpolated concentrations
	 */
	protected double[][] completeConcentrations;
	
	/**
	 * The array contains the read system boundaries
	 */
	private double[] readSystemBoundaries;
	
	/**
	 * If the system boundaries are read from file, set system boundaries and
	 * <CODE>true</CODE>
	 */
	private boolean isSystemBoundaries = false;
	
	/**
	 * This object saves the optimized solution, in fact:
	 * - the optimized fluxes in a double[]
	 * - the optimized concentrations in a double[]
	 */
	private double[][] optimizedSolution;
	
	/**
	 * Lower bounds for CPLEX variables saved in a double array
	 */
	protected double[] lowerBounds;
	
	/**
	 * Upper bounds for CPLEX variables saved in a double array
	 */
	protected double[] upperBounds;
	
	/**
	 * Constraint J_j >= 0
	 */
	protected boolean constraintJ0 = true;
	
	/**
	 * Constraint z_m (t_i+1) >= 0
	 */
	protected boolean constraintZm = true;
	
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
	
	/**
	 * Set the read system boundaries.
	 * 
	 * @param systemBoundaries
	 */
	public void setReadSystemBoundaries(double[] systemBoundaries) {
		this.readSystemBoundaries = systemBoundaries;
		this.isSystemBoundaries = true;
	}
	
	/**
	 * contains the previous factors for reactions
	 */
	protected double[][] factors = null;
	
	/**
	 * @param factors
	 */
	public void setFactors(double[][] factors) {
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
	 * maps the j-th reaction of the {@param splittedDocument} to the known fluxes
	 */
	public Map<Integer, Double> knownFluxes = new HashMap<Integer, Double>();
	
	/**
	 * 
	 * @param map containing the known fluxes
	 */
	public void setKnownFluxes(Map<Integer, Double> map) {
		knownFluxes = map;
	}
	
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
	
	/**
	 * Enable/Disable constraint J_j >= 0
	 * 
	 * @param constraintJ0
	 */
	public void setConstraintJ0(boolean constraintJ0) {
		this.constraintJ0 = constraintJ0;
	}
	
	/**
	 * Enable/Disable constraint z_m (t_i+1) >= 0
	 * 
	 * @param constraintZm
	 */
	public void setConstraintZm(boolean constraintZm) {
		this.constraintZm = constraintZm;
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
	 * Set default bounds for each part of the target function that will be optimized.
	 */
	public void setDefaultBounds() {
		int fullVariableLength = 0;
		for (int i = 0; i < getTargetVariablesLengths().length; i++) {
			fullVariableLength += getTargetVariablesLengths()[i];
		}
		
		this.lowerBounds = new double[fullVariableLength];
		this.upperBounds = new double[fullVariableLength];
		
		// 1. Flux value bounds
		int fluxPosition = 0;
		for (int i = fluxPosition; i < fluxPosition + getTargetVariablesLengths()[0]; i++) {
			this.lowerBounds[i] = 0.0;
			this.upperBounds[i] = 1000.0;
		}
		
		// 2. Concentration bounds
		int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
		for (int i = concentrationPosition; i < concentrationPosition + getTargetVariablesLengths()[1]; i++) {
			this.lowerBounds[i] = Math.pow(10, -10);
			this.upperBounds[i] = Math.pow(10, -1);
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
			flux = cplex.sum(flux, cplex.prod(cplex.constant(this.lambda_1), cplex.abs(getVariables()[fluxPosition + j])));
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
			
			concentrations = cplex.sum(concentrations, cplex.prod(cplex.constant(this.lambda_2), optimizingConcentration));
		}
		
		// Sum up each term
		function = cplex.sum(flux, concentrations);
		
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
			if (knownFluxes.containsKey(j)) {
				double eightyPercent = 0.8 * knownFluxes.get(j);
				
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
					cplex.addGe(j_j_min, eightyPercent);
				} else {
					cplex.addLe(j_j_min, eightyPercent);
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

		for (int n = 0; n < getTargetVariablesLengths()[1]; n++) {

			if (this.getTimePointStep() == 0) {
				// In the first time point step 0, there is no c_m (t_i+1).
				// t_i+1 would be time point step 1, not 0!
				if (!Double.isNaN(this.completeConcentrations[this.getTimePointStep()][n])) {
					cplex.addEq(getVariables()[concentrationPosition + n], cplex.constant(this.completeConcentrations[this.getTimePointStep()][n]));
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
						NJ = cplex.sum(NJ, cplex.prod(cplex.constant(currentN_row[col]), getVariables()[fluxPosition + col]));
					}
					
					computedConcentration = cplex.sum(cplex.constant(this.completeConcentrations[this.getTimePointStep()-1][n]), cplex.prod(NJ, cplex.constant(delta_t)));
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
			for (int i = 0; i < getTargetVariablesLengths()[0]; i++) {
				optimizedFluxVector[i] = solution[fluxPosition + i];
				fluxMap.put(listOfReactions.get(i).getId(), optimizedFluxVector[i]);
			}
			System.out.println(fluxMap.toString());
			this.optimizedSolution[1] = optimizedFluxVector; // 2nd position: flux vector
			
			// 2. Concentration vector assignment
			int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
			HashMap<String, Number> concMap = new HashMap<String, Number>();
			ListOf<Species> listOfSpecies = this.splittedDocument.getModel().getListOfSpecies();
			double[] optimizedConcentrations = new double[getTargetVariablesLengths()[1]];
			for (int i = 0; i < getTargetVariablesLengths()[1]; i++) {
				optimizedConcentrations[i] = solution[concentrationPosition + i];
				concMap.put(listOfSpecies.get(i).getId(), optimizedConcentrations[i]);
			}
			System.out.println(concMap.toString());
			this.optimizedSolution[0] = optimizedConcentrations; // 1st position: concentrations
		}
		
		return this.optimizedSolution;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#saveValuesForCurrentTimePoint(org.simulator.math.odes.MultiTable)
	 */
	public void saveValuesForCurrentTimePoint(MultiTable solutionMultiTable) {
		for (int block = 0; block < solutionMultiTable.getBlockCount(); block++) {
				double[] currentSpecificSolution = optimizedSolution[block];
				if(block == 0) {
					solutionMultiTable.getBlock(block).setRowData(this.getTimePointStep(), currentSpecificSolution);
				}
				else if((block == 1) && (this.getTimePointStep() > 0)){
					solutionMultiTable.getBlock(block).setRowData(this.getTimePointStep() - 1, currentSpecificSolution);
				}
				
				if((block == 1) && (this.getTimePointStep() == solutionMultiTable.getRowCount() - 1)) {
					double[] nanArray = new double[optimizedSolution[1].length];
					Arrays.fill(nanArray, Double.NaN);
					solutionMultiTable.getBlock(block).setRowData(this.getTimePointStep(), nanArray);
				}
		}
	}
	
}
