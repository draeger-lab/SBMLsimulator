/*
 * $Id:  FluxMinimization.java 11:34:24 faehnrich$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2013 by the University of Tuebingen, Germany.
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

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;
import org.sbml.simulator.stability.math.StabilityMatrix;
import org.sbml.simulator.stability.math.StoichiometricMatrix;

/**
 * FluxMinimization target function:
 * minimize (||J|| +
 * lambda_1 * Sum_i^m (c_i - c_eq)&sup2; +
 * lambda_2 * ||L|| +
 * lambda_3 * ||E|| +
 * lambda_4 * ||delta_r G||)
 * 
 * @author Robin F&auml;hnrich
 * @version $Rev$
 * @since 1.0
 */
public class FluxMinimization extends TargetFunction {
	
	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(FluxMinimization.class.getName());
	
	/*
	 * The expanded SBML document, in fact:
	 * - transport reactions eliminated and reversible reactions split,
	 * - compensated reactions added (computed from system boundaries)
	 */
	private SBMLDocument expandedDocument;
	
	/*
	 * The complete intern {@link StoichiometricMatrix} N (with system
	 * boundaries) that is essential for the Tableau algorithm
	 */
	private StoichiometricMatrix N_int_sys;
	
	/*
	 * The internal, reduced and transposed {@link StoichiometricMatrix}
	 * K_int^T for the computation of the L vector and the flux vector
	 */
	private StabilityMatrix K_intTransposed;
	
	/*
	 * Ideal Gas Constant (unit J/mol*K)
	 */
	private static final double R = 8.3144621;
	
	/*
	 * Standard Ambient Temperature (unit Kelvin)
	 */
	private static final double T = 298.15;
	
	/*
	 * Contains the maximum of J_j / delta_r G_tilde_j for each reaction j in the model
	 */
	private double r_max;
	
	/*
	 * These numbers (lambda_i, i is el. of {1, ..., 4}) weight the contributions
	 * of each term in the optimization problem. According to Ziller (2009), set:
	 */
	private double lambda_1 = 10.0;
	
	private double lambda_2 = 10.0;
	
	private double lambda_3 = 0.01;
	
	private double lambda_4 = 1.0;
	
	/*
	 * This vector contains the fluxes that are computed by the Tableau algorithm
	 * applied to N_int_sys
	 */
	private double[] computedFluxVector;
	
	/*
	 * The array contains the complete interpolated concentrations
	 */
	private double[][] completeConcentrations;
	
	/*
	 * The array contains the read gibbs energies in unit J/mol
	 */
	private double[] readGibbsEnergies;
	
	/*
	 * The array contains the read system boundaries
	 */
	private double[] readSystemBoundaries;
	
	/*
	 * If the system boundaries are read from file, set system boundaries and
	 * <CODE>true</CODE>
	 */
	private boolean isSystemBoundaries = false;
	
	/*
	 * This object saves the optimized solution, in fact:
	 * - the optimized fluxes in a double[]
	 * - the optimized concentrations in a double[]
	 * (- NOT the optimized error vector in a double[] -> Sysout)
	 * (- NOT the optimized L vector in a double[] -> Sysout)
	 * - the optimized gibbs energies in a double[]
	 */
	private double[][] optimizedSolution;
	
	/*
	 * Lower bounds for CPLEX variables saved in a double array
	 */
	private double[] lowerBounds;
	
	/*
	 * Upper bounds for CPLEX variables saved in a double array
	 */
	private double[] upperBounds;
	
	/*
	 * Constraint J_j * G_j < 0
	 */
	private boolean constraintJG = true;
	
	/*
	 * Constraint |J_j| - r_max * |G_j| < 0
	 */
	private boolean constraintJ_rmaxG = true;
	
	/*
	 * Constraint J_j >= 0
	 */
	private boolean constraintJ0 = true;
	
	/*
	 * Constraint delta_r G_j = delta_r G^0_j - E_j + RT...
	 */
	private boolean constraintError = true;
	
	/**
	 * Set the concentrations weighting factor lambda1 (default: 10.0).
	 * 
	 * @param lambda1
	 */
	public void setLambda1(double lambda1) {
		this.lambda_1 = lambda1;
	}
	
	/**
	 * Set the L vector weighting factor lambda2 (default: 10.0).
	 * 
	 * @param lambda2
	 */
	public void setLambda2(double lambda2) {
		this.lambda_2 = lambda2;
	}
	
	/**
	 * Set the error weighting factor lambda 3 (default: 0.01).
	 * 
	 * @param lambda3
	 */
	public void setLambda3(double lambda3) {
		this.lambda_3 = lambda3;
	}
	
	/**
	 * Set the gibbs energy weighting factor lambda4 (default: 1.0).
	 * 
	 * @param lambda4
	 */
	public void setLambda4(double lambda4) {
		this.lambda_4 = lambda4;
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#setInterpolatedConcentrations(double[][])
	 */
	@Override
	public void setInterpolatedConcentrations(double[][] concentrations) {
		this.completeConcentrations = concentrations;
	}
	
	/**
	 * Set the read gibbs energies (unit J/mol)
	 * 
	 * @param gibbsEnergies
	 */
	public void setReadGibbsEnergies(double[] gibbsEnergies) {
		this.readGibbsEnergies = gibbsEnergies;
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
	 * Prepare the FluxMinimization by setting the:
	 * - expanded SBML document
	 * - matrix N_int_sys
	 * - computed flux vector (Tableau algorithm)
	 * - matrix K_int_T
	 * all from the (read) system boundaries.
	 * 
	 * @throws Exception
	 */
	private void prepareFluxMinimization() throws Exception {
		// If system boundaries are read from file...
		if (this.isSystemBoundaries) {
			this.expandedDocument = FluxMinimizationUtils.getExpandedDocument(DynamicFBA.originalDocument, this.readSystemBoundaries);
			this.N_int_sys = FluxMinimizationUtils.getExpandedStoichiometricMatrix(DynamicFBA.originalDocument, this.readSystemBoundaries);
			// Compute (with Tableau algorithm) and set flux vector
			this.computedFluxVector = FluxMinimizationUtils.computeFluxVector(this.N_int_sys, null, this.expandedDocument);
			// Set K_int_T
			this.K_intTransposed = FluxMinimizationUtils.getSteadyStateMatrix();
		} else {
			//... or aren't available
			this.expandedDocument = FluxMinimizationUtils.getExpandedDocument(DynamicFBA.originalDocument);
			this.N_int_sys = FluxMinimizationUtils.getExpandedStoichiometricMatrix(DynamicFBA.originalDocument);
			// Compute (with Tableau algorithm) and set flux vector
			this.computedFluxVector = FluxMinimizationUtils.computeFluxVector(this.N_int_sys, null, this.expandedDocument);
			// Set K_int_T
			this.K_intTransposed = FluxMinimizationUtils.getSteadyStateMatrix();
		}
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
	 * Enable/Disable constraint J_j * G_j < 0
	 * 
	 * @param constraintJG
	 */
	public void setConstraintJG(boolean constraintJG) {
		this.constraintJG = constraintJG;
	}

	/**
	 * Enable/Disable constraint |J_j| - r_max * |G_j| < 0
	 * 
	 * @param constraintJ_rmaxG
	 */
	public void setConstraintJ_rmaxG(boolean constraintJ_rmaxG) {
		this.constraintJ_rmaxG = constraintJ_rmaxG;
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
	 * Enable/Disable constraint delta_r G_j = delta_r G^0_j - E_j + RT...
	 * 
	 * @param constraintError
	 */
	public void setConstraintError(boolean constraintError) {
		this.constraintError = constraintError;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getTargetVariablesIds()
	 */
	@Override
	public String[][] getTargetVariablesIds() {		
		// Species Ids for the concentrations block
		ListOf<Species> listOfSpecies = this.expandedDocument.getModel().getListOfSpecies();
		int speciesCount = this.expandedDocument.getModel().getSpeciesCount();
		String[] speciesIds = new String[speciesCount];
		for (int i = 0; i < speciesCount; i++) {
			speciesIds[i] = listOfSpecies.get(i).getId();
		}

		// Reaction Ids for the fluxes and gibbs energies block
		ListOf<Reaction> listOfReactions = this.expandedDocument.getModel().getListOfReactions();
		int reactionCount = this.expandedDocument.getModel().getReactionCount();
		String[] reactionIds = new String[reactionCount];
		for (int i = 0; i < reactionCount; i++) {
			reactionIds[i] = listOfReactions.get(i).getId();
		}
		
		String[][] targetVariablesIds = new String[3][];
		targetVariablesIds[0] = speciesIds; // The concentration ID's
		targetVariablesIds[1] = reactionIds; // The flux vector ID's
		// Now: no assignment useful! There are no L vector ID's for the MultiTable
		//targetVariablesIds[2] = reactionIds; // The error vector ID's
		targetVariablesIds[2] = reactionIds; // The gibbs energies ID's

		return targetVariablesIds;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getTargetVariablesLengths()
	 */
	@Override
	public int[] getTargetVariablesLengths() {
		int speciesCount = this.expandedDocument.getModel().getSpeciesCount();
		int reactionCount = this.expandedDocument.getModel().getReactionCount();
		int K_intT_rowDim = this.K_intTransposed.getRowDimension();
		
		int[] targetVariablesLength = new int[5];
		targetVariablesLength[0] = K_intT_rowDim; //variables for the flux vector
		targetVariablesLength[1] = speciesCount; //variables for the concentrations
		targetVariablesLength[2] = K_intT_rowDim; //variables for the L vector
		targetVariablesLength[3] = reactionCount; //variables for the error vector
		targetVariablesLength[4] = reactionCount; //variables for the gibbs energies
		
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
			this.lowerBounds[i] = 0.01;
			this.upperBounds[i] = 10.0;
		}
		
		// 2. Concentration bounds
		int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
		for (int i = concentrationPosition; i < concentrationPosition + getTargetVariablesLengths()[1]; i++) {
			this.lowerBounds[i] = Math.log(Math.pow(10, -10));
			this.upperBounds[i] = Math.log(Math.pow(10, -1));
		}
		
		// 3. L vector bounds
		int lPosition = concentrationPosition + getTargetVariablesLengths()[1];
		for (int i = lPosition; i < lPosition + getTargetVariablesLengths()[2]; i++) {
			this.lowerBounds[i] = -10000000;
			this.upperBounds[i] = 10000000;
		}
		
		// 4. Error bounds
		int errorPosition = lPosition + getTargetVariablesLengths()[2];
		for (int i = errorPosition; i < errorPosition + getTargetVariablesLengths()[3]; i++) {
			this.lowerBounds[i] = -10000000;
			this.upperBounds[i] = 10000000;
		}
		
		// 5. Gibbs energy bounds
		int gibbsPosition = errorPosition + getTargetVariablesLengths()[3];
		for (int i = gibbsPosition; i < gibbsPosition + getTargetVariablesLengths()[4]; i++) {
			this.lowerBounds[i] = -10000000;
			this.upperBounds[i] = 10000000;
		}
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#initCplexVariables(ilog.cplex.IloCplex)
	 */
	@Override
	public void initCplexVariables(IloCplex cplex) throws IloException {
		// Prepare the FluxMinimization
		try {
			prepareFluxMinimization();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		int fullVariableLength = 0;
		for (int i = 0; i < getTargetVariablesLengths().length; i++) {
			fullVariableLength += getTargetVariablesLengths()[i];
		}
		
		// Set default bounds
		setDefaultBounds();
		
		// Set the variables
		IloNumVar[] variables = cplex.numVarArray(fullVariableLength, this.lowerBounds, this.upperBounds);
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
		for (int k = 0; k < getTargetVariablesLengths()[0]; k++) {
			for (int i = 0; i < this.expandedDocument.getModel().getReactionCount(); i++) {
				IloNumExpr k_var = cplex.prod(this.K_intTransposed.getRow(k)[i], getVariables()[k]);
				flux = cplex.sum(flux, k_var);
			}
		}
		
		// Concentrations 
		IloNumExpr conc = cplex.numExpr();
		int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
		double[] c_eq = this.completeConcentrations[this.getTimePointStep()];
		for (int i = 0; i < getTargetVariablesLengths()[1]; i++) {
			IloNumExpr c_i = getVariables()[i + concentrationPosition];
			getVariables()[i + concentrationPosition].setName("conc" + i);
			if (!Double.isNaN(c_eq[i])) {
				double logConc = Math.log(c_eq[i]);
				if(Double.isInfinite(logConc)) {
					logConc = this.lowerBounds[i + concentrationPosition];
				}
				conc = cplex.sum(conc, cplex.prod(cplex.constant(this.lambda_1), cplex.sum(cplex.square(c_i), cplex.prod(c_i, (-2) * logConc), cplex.square(cplex.constant(logConc)))));
			} else {
				double logcmin = this.lowerBounds[i + concentrationPosition];
				double logcmax = this.upperBounds[i + concentrationPosition];
				conc = cplex.sum(conc, cplex.prod(cplex.constant(this.lambda_1), cplex.max(cplex.max(cplex.constant(0d), cplex.diff(logcmin, c_i)), cplex.diff(c_i, logcmax))));
			}
		}
		
		// L vector
		IloNumExpr l = cplex.numExpr();
		int lPosition = concentrationPosition + getTargetVariablesLengths()[1];
		// Manhattan norm included
		for (int i = 0; i < getTargetVariablesLengths()[2]; i++) {
			getVariables()[i + lPosition].setName("l" + i);
			l = cplex.sum(l, cplex.prod(cplex.constant(this.lambda_2), cplex.abs(getVariables()[i + lPosition])));
		}
		
		// Error values
		IloNumExpr error = cplex.numExpr();
		int errorPosition = lPosition + getTargetVariablesLengths()[2];
		// Manhattan norm included
		for (int i = 0; i < getTargetVariablesLengths()[3]; i++) {
			getVariables()[i + errorPosition].setName("e" + i);
			error = cplex.sum(error, cplex.prod(cplex.constant(this.lambda_3), cplex.abs(getVariables()[i + errorPosition])));
		}
		
		// Gibbs energies
		IloNumExpr gibbs = cplex.numExpr();
		int gibbsPosition = errorPosition + getTargetVariablesLengths()[3];
		// Manhattan norm included
		for (int i = 0; i < getTargetVariablesLengths()[4]; i++) {
			getVariables()[i + gibbsPosition].setName("g" + i);
			gibbs = cplex.sum(gibbs, cplex.prod(cplex.constant(this.lambda_4), cplex.abs(getVariables()[i + gibbsPosition])));
		}
		
		// Sum up each term
		function = cplex.sum(flux, conc, l, error, gibbs);
		//function = cplex.sum(flux, conc, l, error);
		
		return function;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#addConstraintsToTargetFunction(ilog.cplex.IloCplex)
	 */
	@Override
	public void addConstraintsToTargetFunction(IloCplex cplex) throws IloException {
		int reactionCount = this.expandedDocument.getModel().getReactionCount();
		int speciesCount = this.expandedDocument.getModel().getSpeciesCount();

		int fluxPosition = 0;
		int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
		int lPosition = concentrationPosition + getTargetVariablesLengths()[1];
		int errorPosition = lPosition + getTargetVariablesLengths()[2];
		int gibbsPosition = errorPosition + getTargetVariablesLengths()[3];
		
		// Constraint J_j * G_j < 0
		if (this.constraintJG == true) {
			for (int j = 0; j < reactionCount; j++) {
				// Flux J_j
				IloNumExpr j_j = cplex.numExpr();
				for (int k = 0; k < getTargetVariablesLengths()[0]; k++) {
					IloNumExpr k_var = cplex.prod(this.K_intTransposed.getRow(k)[j], getVariables()[k]);
					j_j = cplex.sum(j_j, k_var);
				}
				
				// J_j * G_j
				cplex.ifThen(cplex.not(cplex.eq(j_j, 0)),cplex.le(getVariables()[j + gibbsPosition],0));
			}
		}
		
		// Constraint delta_r G_j = delta_r G^0_j - E_j + RT...
		if (this.constraintError == true) {
			for (int j = 0; j < reactionCount; j++) {
				IloNumExpr sumConcentrations = cplex.numExpr();
				
				for (int i = 0; i < speciesCount; i++) {
					sumConcentrations = cplex.sum(sumConcentrations, cplex.prod(this.N_int_sys.get(i, j), getVariables()[i + concentrationPosition]));
				}
				// TODO if readGibbsEnergies[i] is NaN???
				if(!Double.isNaN(this.readGibbsEnergies[j])) {
					IloNumExpr delta_G_computation = cplex.diff(cplex.sum(cplex.prod(R, cplex.prod(T, sumConcentrations)), this.readGibbsEnergies[j]), getVariables()[j + errorPosition]);
					cplex.addEq(getVariables()[j + gibbsPosition], delta_G_computation);
				}
			}
		}
		
		// Computation r_max = max(J_j / delta_r G_tilde_j)
		double rmax = Double.MIN_NORMAL;
		
		for (int j = 0; j < reactionCount; j++) {
			// Division by NaN -> r_max = NaN, Division by zero -> r_max = +Infinity
			if (!Double.isNaN(this.readGibbsEnergies[j]) && this.readGibbsEnergies[j] != 0) {
				// TODO computedFluxVector <-> basis vectors of K_int_transposed??
				double current_r = Math.abs(this.computedFluxVector[j]) / Math.abs(this.readGibbsEnergies[j]);
				rmax = Math.max(rmax, current_r);
			}
		}
		this.r_max = rmax;
		
		// Constraint |J_j| - r_max * |G_j| < 0
		if (this.constraintJ_rmaxG == true) {
			for (int j = 0; j < reactionCount; j++) {
				// Flux J_j
				IloNumExpr j_j = cplex.numExpr();
				for (int k = 0; k < getTargetVariablesLengths()[0]; k++) {
					IloNumExpr k_var = cplex.prod(this.K_intTransposed.getRow(k)[j], getVariables()[k]);
					j_j = cplex.sum(j_j, k_var);
				}
				// |J_j| - r_max * |G_j|
				IloNumExpr rmaxG = cplex.prod(this.r_max, cplex.abs(getVariables()[j + gibbsPosition]));
				
				cplex.addLe(cplex.diff(cplex.abs(j_j), rmaxG), 0);
			}
		}
		
		// Constraint J_j >= 0
		if (this.constraintJ0 == true) {
			for (int j = 0; j < reactionCount; j++) {
				// Flux J_j
				IloNumExpr j_j = cplex.numExpr();
				for (int k = 0; k < getTargetVariablesLengths()[0]; k++) {
					IloNumExpr k_var = cplex.prod(this.K_intTransposed.getRow(k)[j], getVariables()[k]);
					j_j = cplex.sum(j_j, k_var);
				}
				
				cplex.addGe(j_j, 0);
			}
		}
		
		// Computation of the L vector
		for (int kRow = 0; kRow < this.K_intTransposed.getRowDimension(); kRow++) {

			double[] row = this.K_intTransposed.getRow(kRow);
			
			IloNumExpr l_row = cplex.numExpr(); // Remember: L is a vector!
			for (int kColumn = 0; kColumn < this.K_intTransposed.getColumnDimension(); kColumn++) {
				double kVal = row[kColumn];
				// TODO if readGibbsEnergies[i] is NaN???
				if(!Double.isNaN(this.readGibbsEnergies[kColumn])) {
					IloNumExpr delta_G_tilde_j = cplex.diff(this.readGibbsEnergies[kColumn], getVariables()[kColumn + errorPosition]);
				  l_row = cplex.sum(l_row, cplex.prod(kVal, delta_G_tilde_j)); 
				}
			}
			
			cplex.addEq(getVariables()[kRow + lPosition], l_row);
		}
		
		//Ensure that the gibbs values of a reaction and the corresponding backward reaction correspond to each other
		Map<Integer, Integer> reverseReaction = FluxMinimizationUtils.reverseReaction;
		
		
		
		for (Entry<Integer, Integer> map : reverseReaction.entrySet()) {
			int index = map.getKey();
			int revIndex= map.getValue();
			cplex.addEq(cplex.sum(getVariables()[revIndex + errorPosition], readGibbsEnergies[revIndex]), cplex.prod(-1, cplex.sum(getVariables()[index + errorPosition],readGibbsEnergies[index])));
			
		}
		
		
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getOptimizedSolution()
	 */
	@Override
	public double[][] getOptimizedSolution() {
		double[] solution = getSolution();
		this.optimizedSolution = new double[3][];
		
		if (solution == null) {
			logger.warning("Can't get optimized solution! Solution doesn't exist!");
		} else {
			// 1. Flux vector assignment
			int fluxPosition = 0;
			double[] optimizedFluxVector = new double[this.expandedDocument.getModel().getReactionCount()];
			// solution[k] contains the optimized values for the flux vector (row dimension of K_int^T)
			for (int i = 0; i < optimizedFluxVector.length; i++) {
				double fluxVector = 0;
				for (int k = 0; k < getTargetVariablesLengths()[0]; k++) {
					fluxVector += solution[k] * this.K_intTransposed.getRow(k)[i];
				}
				optimizedFluxVector[i] = fluxVector;
			}
			this.optimizedSolution[1] = optimizedFluxVector; // 2nd position: flux vector
			System.out.println("Optimized Flux Vector: " + Arrays.toString(optimizedFluxVector));
			
			// 2. Concentration vector assignment
			int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
			double[] optimizedConcentrations = new double[getTargetVariablesLengths()[1]];
			for (int i = 0; i < getTargetVariablesLengths()[1]; i++) {
				optimizedConcentrations[i] = Math.pow(Math.E, solution[i + concentrationPosition]);
			}
			this.optimizedSolution[0] = optimizedConcentrations; // 1st position: concentrations
			System.out.println("Optimized concentration: " + Arrays.toString(optimizedConcentrations));
			
			// 3. L vector assignment
			int lPosition = concentrationPosition + getTargetVariablesLengths()[1];
			double[] optimizedLVector = new double[getTargetVariablesLengths()[2]];
			for (int i = 0; i < getTargetVariablesLengths()[2]; i++) {
				optimizedLVector[i] = solution[i + lPosition];
			}
			System.out.println("Optimized L vector: " + Arrays.toString(optimizedLVector));
			// Now: no assignment useful! There are no L vector ID's for the MultiTable
			
			// 4. Error vector assignment
			int errorPosition = lPosition + getTargetVariablesLengths()[2];
			double[] optimizedErrors = new double[getTargetVariablesLengths()[3]];
			for (int i = 0; i < getTargetVariablesLengths()[3]; i++) {
				optimizedErrors[i] = solution[i + errorPosition];
			}
			System.out.println("Optimized error Vector: " + Arrays.toString(optimizedErrors));
			
			// 5. Gibbs energy vector assignment
			int gibbsPosition = errorPosition + getTargetVariablesLengths()[3];
			double[] optimizedGibbsEnergies = new double[getTargetVariablesLengths()[4]];
			for (int i = 0; i < getTargetVariablesLengths()[4]; i++) {
				optimizedGibbsEnergies[i] = solution[i + gibbsPosition];
			}
			this.optimizedSolution[2] = optimizedGibbsEnergies; // 3rd position: gibbs energies
			System.out.println("G0 Gibbs Vector: " + Arrays.toString(this.readGibbsEnergies));
			System.out.println("Optimized Gibbs Vector: " + Arrays.toString(optimizedGibbsEnergies));
		}
		
		return this.optimizedSolution;
	}
	
}
