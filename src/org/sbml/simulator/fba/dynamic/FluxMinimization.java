/*
 * $Id:  FluxMinimization.java 11:34:24 faehnrich$
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
package org.sbml.simulator.fba.dynamic;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.logging.Logger;

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
	 * The complete intern {@link StoichiometricMatrix} N (with system
	 * boundaries) that is essential for the Tableau algorithm
	 */
	private StoichiometricMatrix N_int_sys;
	
	/*
	 * The internal, reduced and transposed {@link StoichiometricMatrix}
	 * K_int^T for the computation of the L vector
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
	 * Contains the maximum of J_j / G_j for each reaction j in the model
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
	 * The array contains the current interpolated concentrations for this step
	 * of optimization by CPLEX
	 */
	private double[] currentConcentrations;
	
	/*
	 * The computed L vector (L = K_int^T * delta_r G_int, where T is the
	 * transposed matrix)
	 */
	private double[] computedLVector;
	
	/*
	 * The array contains the read gibbs energies in unit J/mol
	 */
	private double[] readGibbsEnergies;

	/*
	 * Save the optimized flux vector in a double array
	 */
	private double[] optimizedFluxVector;
	
	/*
	 * Save the optimized concentrations vector in a double array
	 */
	private double[] optimizedConcentrations;
	
	/*
	 * Save the optimized L vector in a double array
	 */
	private double[] optimizedLVector;
	
	/*
	 * Save the optimized error values in a double array
	 */
	private double[] optimizedErrors;
	
	/*
	 * Save the optimized gibbs energies vector in a double array
	 */
	private double[] optimizedGibbsEnergies;
	
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
	 * Constraint delta_G = delta_G - error
	 */
	private boolean constraintError = true;
	
	
	/**
	 * Set the expanded {@link StoichiometricMatrix} N_int_sys of the
	 * corresponding expanded SBML document.
	 * 
	 * @param N
	 */
	public void setNIntSys(StoichiometricMatrix N) {
		this.N_int_sys = N;
	}
	
	/**
	 * Set the internal, reduced and transposed {@link StoichiometricMatrix}
	 * K_int^T for the computation of the L vector.
	 * 
	 * @param K_intT
	 */
	public void setKIntTransposed(StabilityMatrix K_intT) {
		this.K_intTransposed = K_intT;
	}
	
	/**
	 * Set the maximum J_j / G_j for each reaction j in the model.
	 * 
	 * @param rmax
	 */
	public void setRmax(double rmax) {
		this.r_max = rmax;
	}
	
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
	
	/**
	 * Set the computed flux vector.
	 * 
	 * @param fluxVector
	 */
	public void setComputedFluxVector(double[] fluxVector) {
		this.computedFluxVector = fluxVector;
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#setCurrenConcentrations(double[])
	 */
	@Override
	public void setCurrentConcentrations(double[] concentrations) {
		this.currentConcentrations = concentrations;
	}
	
	/**
	 * Set the computed L vector.
	 * 
	 * @param LVector
	 */
	public void setComputedLVector(double[] LVector) {
		this.computedLVector = LVector;
	}
	
	/**
	 * Set the read gibbs energies (unit J/mol)
	 * 
	 * @param gibbsEnergies
	 */
	public void setReadGibbsEnergies(double[] gibbsEnergies) {
		this.readGibbsEnergies = gibbsEnergies;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getOptimizedFluxVector()
	 */
	@Override
	public double[] getOptimizedFluxVector() {
		return this.optimizedFluxVector;
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getOptimizedConcentrations()
	 */
	@Override
	public double[] getOptimizedConcentrations() {
		return this.optimizedConcentrations;
	}
	
	/**
	 * @return The computed L vector optimized by the target function
	 */
	public double[] getOptimizedLVector() {
		return this.optimizedLVector;
	}
	
	/**
	 * @return The computed error values optimized by the target function
	 */
	public double[] getOptimizedErrors() {
		return this.optimizedErrors;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#getOptimizedGibbsEnergies()
	 */
	@Override
	public double[] getOptimizedGibbsEnergies() {
		return this.optimizedGibbsEnergies;
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#isConcentrationsOptimization()
	 */
	@Override
	public boolean isConcentrationsOptimization() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#isGibbsEnergiesOptimization()
	 */
	@Override
	public boolean isGibbsEnergiesOptimization() {
		return true;
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
	 * @param constraintJG
	 */
	public void setConstraintJG(boolean constraintJG) {
		this.constraintJG = constraintJG;
	}

	/**
	 * Enable/Disable constraint |J_j| - r_max * |G_j| < 0
	 * @param constraintJ_rmaxG
	 */
	public void setConstraintJ_rmaxG(boolean constraintJ_rmaxG) {
		this.constraintJ_rmaxG = constraintJ_rmaxG;
	}

	/**
	 * Enable/Disable constraint J_j >= 0
	 * @param constraintJ0
	 */
	public void setConstraintJ0(boolean constraintJ0) {
		this.constraintJ0 = constraintJ0;
	}
	
	/**
	 * Enable/Disable constraint delta_G = delta_G - error
	 * @param constraintError
	 */
	public void setConstraintError(boolean constraintError) {
		this.constraintError = constraintError;
	}
	
	/**
	 * @return The length of each variable vector in an int array (attend the order!)
	 */
	public int[] getTargetVariablesLengths() {
		int speciesCount = DynamicFBA.expandedDocument.getModel().getSpeciesCount();
		int reactionCount = DynamicFBA.expandedDocument.getModel().getReactionCount();
		
		int[] targetVariablesLength = new int[5];
		targetVariablesLength[0] = 1; //1 variable for the flux vector
		targetVariablesLength[1] = speciesCount; //variables for the concentrations
		targetVariablesLength[2] = reactionCount; //variables for the L vector
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
		int fluxPosition = getTargetVariablesLengths()[0] - 1;
		this.lowerBounds[fluxPosition] = 1.0;
		this.upperBounds[fluxPosition] = 10.0;
		
		// 2. Concentration bounds
		int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
		for (int i = concentrationPosition; i < concentrationPosition + getTargetVariablesLengths()[1]; i++) {
			this.lowerBounds[i] = Math.pow(10, -10);
			this.upperBounds[i] = Math.pow(10, -1);
		}
		
		// 3. L vector bounds
		int lPosition = concentrationPosition + getTargetVariablesLengths()[1];
		for (int i = lPosition; i < lPosition + getTargetVariablesLengths()[2]; i++) {
			this.lowerBounds[i] = 0.0;
			this.upperBounds[i] = 100000.0;
		}
		
		// 4. Error bounds
		int errorPosition = lPosition + getTargetVariablesLengths()[2];
		for (int i = errorPosition; i < errorPosition + getTargetVariablesLengths()[3]; i++) {
			this.lowerBounds[i] = 0.0;
			this.upperBounds[i] = 100000.0;
		}
		
		// 5. Gibbs energy bounds
		int gibbsPosition = errorPosition + getTargetVariablesLengths()[3];
		for (int i = gibbsPosition; i < gibbsPosition + getTargetVariablesLengths()[4]; i++) {
			this.lowerBounds[i] = -100000;
			this.upperBounds[i] = 100;
		}
	}
	
	//TODO write for each bound part a new set-method to set bounds!

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#prepareCplex(ilog.cplex.IloCplex)
	 */
	@Override
	public void initCplexVariables(IloCplex cplex) throws IloException {
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
		int fluxPosition = getTargetVariablesLengths()[0] - 1;
		// Manhattan norm included
		for (int i = 0; i < this.computedFluxVector.length; i++) {
			flux = cplex.sum(flux, cplex.abs(cplex.prod(this.computedFluxVector[i], getVariables()[0])));
		}
		
		// Concentrations 
		IloNumExpr conc = cplex.numExpr();
		int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
		double[] c_eq = this.currentConcentrations;
		for (int i = 0; i < getTargetVariablesLengths()[1]; i++) {
			IloNumExpr c_i = getVariables()[i + concentrationPosition];
			if (!Double.isNaN(c_eq[i])) {
				double logConc = Math.log(c_eq[i]);
				// TODO ?
				// if(Double.isInfinite(logConc)) {
				// logConc = - Double.MAX_VALUE;
				// }
				conc = cplex.sum(conc, cplex.square(c_i), cplex.sum(cplex.prod(c_i, (-2) * logConc), cplex.square(cplex.constant(logConc))));
			} else {
				double logcmin = Math.log(this.lowerBounds[i + concentrationPosition]);
				double logcmax = Math.log(this.upperBounds[i + concentrationPosition]);
				conc = cplex.sum(conc, cplex.max(cplex.max(0, cplex.diff(logcmin, c_i)), cplex.diff(c_i, logcmax)));
			}
		}
		conc = cplex.prod(cplex.constant(this.lambda_1), conc);
		
		// L vector
		IloNumExpr l = cplex.numExpr();
		int lPosition = concentrationPosition + getTargetVariablesLengths()[1];
		// Manhattan norm included
		for (int i = 0; i < getTargetVariablesLengths()[2]; i++) {
			l = cplex.sum(l, cplex.abs(cplex.prod(this.computedLVector[i], getVariables()[i + lPosition])));
			//l = cplex.sum(l, cplex.abs(getVariables()[i + lPosition]));
		}
		l = cplex.prod(cplex.constant(this.lambda_2), l);
		
		// Error values
		IloNumExpr error = cplex.numExpr();
		int errorPosition = lPosition + getTargetVariablesLengths()[2];
		// Manhattan norm included
		for (int i = 0; i < getTargetVariablesLengths()[3]; i++) {
			error = cplex.sum(error, cplex.abs(getVariables()[i + errorPosition]));
		}
		error = cplex.prod(cplex.constant(this.lambda_3), error);
		
		// Gibbs energies
		IloNumExpr gibbs = cplex.numExpr();
		int gibbsPosition = errorPosition + getTargetVariablesLengths()[3];
		// Manhattan norm included
		for (int i = 0; i < getTargetVariablesLengths()[4]; i++) {
			gibbs = cplex.sum(gibbs, cplex.abs(getVariables()[i + gibbsPosition]));
		}
		gibbs = cplex.prod(cplex.constant(this.lambda_4), gibbs);
		
		// Sum up each term
		function = cplex.sum(flux, conc, l, error, gibbs);
		
		return function;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#addConstraintsToTargetFunction(ilog.cplex.IloCplex)
	 */
	@Override
	public void addConstraintsToTargetFunction(IloCplex cplex) throws IloException {
		int reactionCount = DynamicFBA.expandedDocument.getModel().getReactionCount();
		
		//TODO rmax
		
		int fluxPosition = getTargetVariablesLengths()[0] - 1;
		int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
		int lPosition = concentrationPosition + getTargetVariablesLengths()[1];
		int errorPosition = lPosition + getTargetVariablesLengths()[2];
		int gibbsPosition = errorPosition + getTargetVariablesLengths()[3];
		
		for (int j = 0; j < reactionCount; j++) {
			// Flux J_j TODO omit Math.abs()?
			IloNumExpr j_j = cplex.abs(cplex.prod(Math.abs(this.computedFluxVector[j]), getVariables()[fluxPosition]));
			
			// Constraint J_j * G_j < 0
			if (this.constraintJG == true) {
				IloNumExpr jg = cplex.numExpr();
				jg = cplex.prod(cplex.prod(this.computedFluxVector[j], getVariables()[fluxPosition]), getVariables()[j + gibbsPosition]);
				cplex.addLe(jg, 0);
			}
			
			// Constraint |J_j| - r_max * |G_j| < 0
			if (this.constraintJ_rmaxG == true) {
				IloNumExpr rmaxG = cplex.numExpr();
				rmaxG = cplex.prod(this.r_max, cplex.abs(getVariables()[j + gibbsPosition]));
				cplex.addLe(cplex.diff(j_j, rmaxG), 0);
			}

			// Constraint J_j >= 0
			if (this.constraintJ0 == true) {
				cplex.addGe(cplex.prod(this.computedFluxVector[j], getVariables()[fluxPosition]), 0);
			}
			
			// Constraint delta_G = delta_G - error
			if (this.constraintError == true) {
				IloNumExpr sumConcentrations = cplex.numExpr();
				for (int i = 0; i < getTargetVariablesLengths()[1]; i++) {
					sumConcentrations = cplex.sum(sumConcentrations, cplex.prod(this.N_int_sys.get(i, j), getVariables()[i + concentrationPosition]));
				}
				IloNumExpr delta_G_computation = cplex.sum(cplex.prod(R, cplex.prod(T, sumConcentrations)), cplex.diff(this.readGibbsEnergies[j], getVariables()[j + errorPosition]));
				cplex.addEq(delta_G_computation, getVariables()[j + gibbsPosition]);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.simulator.fba.dynamic.TargetFunction#assignOptimizedSolution(org.sbml.jsbml.SBMLDocument)
	 */
	@Override
	public void assignOptimizedSolution() {
		double[] solution = getSolution();
		
		if (solution == null) {
			logger.warning("Solution doesn't exist, no assignment possible!");
		} else {
			// 1. Flux vector assignment
			int fluxPosition = getTargetVariablesLengths()[0] - 1;
			this.optimizedFluxVector = new double[this.computedFluxVector.length];
			// solution[0] contains the optimized value for the flux vector
			for (int i = 0; i < this.computedFluxVector.length; i++) {
				this.optimizedFluxVector[i] = solution[fluxPosition] * this.computedFluxVector[i];
			}
			
			// 2. Concentration vector assignment
			int concentrationPosition = fluxPosition + getTargetVariablesLengths()[0];
			this.optimizedConcentrations = new double[getTargetVariablesLengths()[1]];
			for (int i = 0; i < getTargetVariablesLengths()[1]; i++) {
				this.optimizedConcentrations[i] = Math.pow(Math.E, solution[i + concentrationPosition]);
			}
			
			// 3. L vector assignment
			int lPosition = concentrationPosition + getTargetVariablesLengths()[1];
			this.optimizedLVector = new double[getTargetVariablesLengths()[2]];
			for (int i = 0; i < getTargetVariablesLengths()[2]; i++) {
				this.optimizedLVector[i] = solution[i + lPosition];
			}
			
			// 4. Error vector assignment
			int errorPosition = lPosition + getTargetVariablesLengths()[2];
			this.optimizedErrors = new double[getTargetVariablesLengths()[3]];
			for (int i = 0; i < getTargetVariablesLengths()[3]; i++) {
				this.optimizedErrors[i] = solution[i + errorPosition];
			}
			
			// 5. Gibbs energy vector assignment
			int gibbsPosition = errorPosition + getTargetVariablesLengths()[3];
			this.optimizedGibbsEnergies = new double[getTargetVariablesLengths()[4]];
			for (int i = 0; i < getTargetVariablesLengths()[4]; i++) {
				this.optimizedGibbsEnergies[i] = solution[i + gibbsPosition];
			}
		}
	}
	
}
