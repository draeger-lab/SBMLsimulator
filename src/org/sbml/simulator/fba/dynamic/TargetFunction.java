/*
 * $Id:  TargetFunction.java 11:31:26 faehnrich$
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

import java.util.logging.Logger;


import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

/**
 * 
 * @author Robin F&auml;hnrich
 * @version $Rev$
 * @since 1.0
 */
public abstract class TargetFunction {
	
	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(TargetFunction.class.getName());
	
	/*
	 * Alternatively (for use when needed):
	 * Save the target coefficients of the CPLEX object in a double array
	 */
	private double[] coefficients;
	
	/*
	 * Save the variables of the CPLEX object in a {@link IloNumVar[]}
	 */
	private IloNumVar[] variables;
	
	/*
	 * Save the variables that are solved by CPLEX in a double array
	 */
	private double[] solution;
	
	
	/**
	 * Alternatively (for use when needed).
	 * 
	 * @return The target coefficients of the CPLEX object
	 */
	public double[] getCoefficients() {
		return this.coefficients;
	}
	
	/**
	 * Alternatively (for use when needed):
	 * Set the target coefficients of the CPLEX object.
	 * 
	 * @param coefficients
	 */
	public void setCoefficients(double[] coefficients) {
		this.coefficients = coefficients;
	}
	
	/**
	 * @return The variables of the CPLEX object
	 */
	public IloNumVar[] getVariables() {
		return this.variables;
	}
	
	/**
	 * Set the variables of the CPLEX object.
	 * 
	 * @param variables
	 */
	public void setVariables(IloNumVar[] variables) {
		this.variables = variables;
	}
	
	/**
	 * @return The solution to the variables of the optimization problem
	 */
	public double[] getSolution() {
		return this.solution;
	}
	
	/**
	 * Set the current interpolated concentrations for this step of optimization
	 * by CPLEX
	 * 
	 * @param currentConcentrations
	 */
	public abstract void setCurrentConcentrations(double[] currentConcentrations);
	
	/**
	 * @return <CODE>true</CODE> if the concentrations are supposed to be optimized
	 */
	public abstract boolean isConcentrationsOptimization();
	
	/**
	 * @return The computed concentrations optimized by the target function
	 */
	public abstract double[] getOptimizedConcentrations();
	
	/**
	 * @return The computed flux vector optimized by the target function
	 */
	public abstract double[] getOptimizedFluxVector();
	
	/**
	 * @return <CODE>true</CODE> if the gibbs energies are supposed to be optimized
	 */
	public abstract boolean isGibbsEnergiesOptimization();
	
	/**
	 * @return The computed gibbs energies optimized by the target function
	 */
	public abstract double[] getOptimizedGibbsEnergies();
	
	/**
	 * @return <CODE>true</CODE> if the target function belongs to a minimization problem
	 */
	public abstract boolean isMinProblem();
	
	/**
	 * @return <CODE>true</CODE> if the target function belongs to a maximization problem
	 */
	public abstract boolean isMaxProblem();
	
	/**
	 * Prepare CPLEX by setting the variables with lower and upper bounds and
	 * alternatively the target coefficients.
	 * 
	 * @param cplex
	 * @throws IloException
	 */
	public abstract void prepareCplex(IloCplex cplex) throws IloException;
	
	/**
	 * Create a new target function that will be solved by CPLEX.
	 * 
	 * @param cplex
	 * @return The target function in a {@link IloNumExpr}
	 * @throws IloException
	 */
	public abstract IloNumExpr createTargetFunction(IloCplex cplex) throws IloException;
	
	/**
	 * If the target function belongs to a minimization problem, minimize it, if
	 * the target function belongs to a maximization problem, maximize it.
	 * 
	 * @param cplex
	 * @param expr The target function in a {@link IloNumExpr}
	 * @throws IloException
	 */
	public void optimizeTargetFunction(IloCplex cplex, IloNumExpr expr) throws IloException {
		if (isMinProblem()) {
			cplex.addMinimize(expr);
		} else if (isMaxProblem()) {
			cplex.addMaximize(expr);
		} else {
			logger.warning("No minimization or maximization problem!");
		}
	}
	
	/**
	 * Add constraints to the CPLEX target function.
	 * 
	 * @param cplex
	 * @throws IloException
	 */
	public abstract void addConstraintsToTargetFunction(IloCplex cplex) throws IloException;
	
	/**
	 * Let CPLEX solve the optimization problem (if necessary check the CPLEX
	 * iterations) and set the solution of the variables.
	 * 
	 * @param cplex
	 * @throws IloException
	 */
	public void solveCplex(IloCplex cplex) throws IloException {
		// TODO write method to set cplexIterations (now: 600)
		cplex.setParam(IloCplex.IntParam.BarItLim, 600);
		
		if (cplex.solve()) {
			this.solution = cplex.getValues(this.variables);
		} else {
			this.solution = null;
			logger.warning("No feasible solution found!");
		}
	}
	
	/**
	 * CPLEX solves the optimization problem.
	 * 
	 * @param cplex
	 * @throws IloException
	 */
	public void optimizeProblem(IloCplex cplex) throws IloException {
		// Prepare CPLEX
		prepareCplex(cplex);
		// Create target function
		IloNumExpr targetFunction = createTargetFunction(cplex);
		// Optimize target function
		optimizeTargetFunction(cplex, targetFunction);
		// Add constraints to target function
		addConstraintsToTargetFunction(cplex);
		// Solve the optimization problem
		solveCplex(cplex);
	}

	/**
	 * Assign the solved values to the actual arrays; at least the flux values
	 * to the optimized flux vector.
	 */
	public abstract void assignOptimizedSolution();
	
}
