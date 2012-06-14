/*
 * $Id:  FluxBalanceAnalysis.java 16:07:08 Meike Aichele$
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
package org.sbml.simulator.fba.controller;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import org.sbml.jsbml.SBMLDocument;

/**
 * Contains all components for flux balance analysis and solves the optimization problem
 * for the incoming target function and constraints with linear or quadratic programming.
 * 
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class FluxBalanceAnalysis {
	/**
	 * Can be a {@link FluxMinimization}-object 
	 * or an other function for which the network has to be optimized
	 */
	public TargetFunction targetFunc;

	/**
	 * Contains e.g. Gibbs-energies
	 */
	public Constraints constraints;

	/**
	 * lower bound for cplex variables
	 */
	public double[] lb;
	
	/**
	 * upper bounds for cplex variables
	 */
	public double[] ub;
	
	/**
	 * Contains the target array for cplex.
	 */
	private double[] target;

	/**
	 * Contains the concentrations computed in the targetFunc.
	 */
	private double[] concentrations;

	/**
	 * Contains the solutions of cplex for the concentrations.
	 */
	public double[] solution_concentrations;

	/**
	 * Contains the solutions of cplex for the fluxes.
	 */
	public double[] solution_fluxVector;

	/**
	 * Constructor that get's a {@link Constraints}-Object and a {@link SBMLDocument} 
	 * and that set's the {@link# linearProgramming} true.
	 * 
	 * @param constraints
	 * @param doc
	 */
	public FluxBalanceAnalysis(double[] c_eq, Constraints constraints, SBMLDocument doc, String[] targetFluxes) {
		this(new FluxMinimization(doc, c_eq, constraints.getGibbsEnergies(), targetFluxes), constraints);
	}

	/**
	 * Constructor that get's {@link TargetFunction}, {@link Constraints} and a Boolean, which
	 * contains the information for linearProgramming (true) or quadraticProgramming (false).
	 * 
	 * @param target
	 * @param constraints
	 */
	public FluxBalanceAnalysis(TargetFunction target, Constraints constraints) {
		super();
		this.targetFunc = target;
		this.constraints = constraints;
		int length = targetFunc.computeTargetFunctionForQuadraticProgramming().length;
		lb = new double[length];
		ub = new double[length];
		this.solution_fluxVector = new double[target.getFluxVector().length];
	}

	/**
	 * Calls a method to solve the problem with linear programming if the boolean
	 * {@link# linearProgramming} is set and true, else it calls a method for quadratic programming.
	 * @return double[]
	 * @throws IloException 
	 */
	public double[] solve() throws IloException {
		target = targetFunc.computeTargetFunctionForQuadraticProgramming();
		concentrations = targetFunc.getConcentrations();
		return solveWithQuadraticProgramming();
	}


	/**
	 * Calls CPLEX to solve the problem with quadratic programming
	 * @throws IloException 
	 */
	private double[] solveWithQuadraticProgramming() throws IloException {
		// create the cplex solver
		IloCplex cplex = new IloCplex();

		// TARGET
		// create upper bounds (ub) and lower bounds (lb) for the variables x
		int[] counter = targetFunc.getCounterArray();
		// counter[1] contains the length of the flux vector
		for (int i = counter[1]; i< target.length; i++) {
			lb[i]= 0.0; 
			ub[i] = Double.MAX_VALUE;
		}
		// create variables with upper bounds and lower bounds
		IloNumVar[] x = cplex.numVarArray(target.length, lb, ub);

		//compute the target function for cplex with the scalar product (lin)
		IloNumExpr lin = cplex.scalProd(target, x);
		
		
		//lambda1*sum((c_i - c_eq)^2)
		double[] c_eq = constraints.getEquilibriumConcentrations();
		IloNumVar[] c = cplex.numVarArray(concentrations.length, lb, ub);
		IloNumExpr sum = null;
		for (int i = 0; i< c.length; i++) {
			IloNumExpr temp = sum;
			IloNumExpr c_i = cplex.prod(c[i], concentrations[i]);
			sum = cplex.sum(temp, cplex.prod(cplex.sum(c_i, (-1 * c_eq[i])), cplex.sum(c_i, (-1 * c_eq[i]))));
		}
	
		// now put the variables together  
		IloNumExpr cplex_target = cplex.sum(lin, cplex.prod(TargetFunction.lambda1, sum));
		

		// only for FluxMinimization has the target function be minimized
		if (targetFunc instanceof FluxMinimization) {
			cplex.addMinimize(cplex_target);
		} else {
			cplex.addMaximize(cplex_target);
		}
		
		
		//CONTRAINTS

		double[] flux = targetFunc.getFluxVector();
		double[] gibbs = constraints.getGibbsEnergies();
		double r_max = constraints.computeR_max(flux);
		for (int i = 0; i< counter[1]; i++) {
			//jg is the expression for J_i * G_i
			//and jr_maxg the expression for |J_i| - r_max * |G_i|
			for (int k = 0; k < counter[counter.length-1]; k++) {
				// contraint |J_i| - r_max * |G_i| < 0
				IloNumExpr j_i = cplex.abs(cplex.prod(flux[i], x[i]));
				IloNumExpr g_i = cplex.abs(cplex.prod(gibbs[i], x[k]));
				IloNumExpr jr_maxg = cplex.sum(j_i, (cplex.prod(-1, cplex.prod(r_max, g_i))));
				cplex.addLe(jr_maxg, 0);

				// contraint J_i * G_i < 0
				IloNumExpr jg = cplex.prod(cplex.prod(flux[i], x[i]),cplex.prod(gibbs[i],x[k]));
				cplex.addLe(jg, 0);
			}
		}
		

		// now solve the problem and get the solution array for the variables x
		double[] solution = null;
		if (cplex.solve()) {
			// get the from cplex computed values for the variables x and c
			solution = cplex.getValues(x);
			for (int i = 0; i < counter[1]; i++) {
				solution_fluxVector[i] = solution[i];
			}
			solution_concentrations = cplex.getValues(c);
		}
		cplex.end();
		return solution;

	}

	public boolean setUbOfReactionJ(double ubValue, int j) {
		if (j < targetFunc.getFluxVector().length) {
			ub[j] = ubValue;
			return true;
		} else {
			return false;
		}
	}


	/**
	 * Puts for reaction j the lower bound on the given lbValue
	 * @param lbValue
	 * @param j (index of reaction)
	 * @return true if lbValue was set successfully
	 */
	public boolean setLbOfReactionJ(double lbValue, int j) {
		if (j < targetFunc.getFluxVector().length) {
			lb[j] = lbValue;
			return true;
		} else {
			return false;
		}
	}
}
