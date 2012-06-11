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
import ilog.concert.IloLinearNumExpr;
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
	 * Is true for the request to solve the problem with linear programming and false for quadratic programming.
	 */
	private Boolean linearProgramming;

	public double[] lb;
	public double[] ub;
	private double[] target;

	/**
	 * Constructor that get's a {@link Constraints}-Object and a {@link SBMLDocument} 
	 * and that set's the {@link# linearProgramming} true.
	 * 
	 * @param constraints
	 * @param doc
	 */
	public FluxBalanceAnalysis(double[] c_eq, Constraints constraints, SBMLDocument doc, String[] targetFluxes) {
		this(new FluxMinimization(doc, c_eq, constraints.getGibbsEnergies(), targetFluxes), constraints, true);
	}

	/**
	 * Constructor that get's {@link TargetFunction}, {@link Constraints} and a Boolean, which
	 * contains the information for linearProgramming (true) or quadraticProgramming (false).
	 * 
	 * @param target
	 * @param constraints
	 * @param linearProgramming
	 */
	public FluxBalanceAnalysis(TargetFunction target, Constraints constraints, Boolean linearProgramming) {
		super();
		this.targetFunc = target;
		this.constraints = constraints;
		this.setLinearProgramming(linearProgramming);
		if (linearProgramming) {
			int length = targetFunc.computeTargetFunctionForLinearProgramming().length;
			lb = new double[length];
			ub = new double[length];
		} else {
			int length = targetFunc.computeTargetFunctionForQuadraticProgramming().length;
			lb = new double[length];
			ub = new double[length];
		}
	}

	/**
	 * Calls a method to solve the problem with linear programming if the boolean
	 * {@link# linearProgramming} is set and true, else it calls a method for quadratic programming.
	 * @return double[]
	 * @throws IloException 
	 */
	public double[] solve() throws IloException {
		if (this.isSetLinearProgramming()) {
			if (this.isLinearProgramming()) {
				target = targetFunc.computeTargetFunctionForLinearProgramming();
				return solveWithLinearProgramming();
			} else {
				target = targetFunc.computeTargetFunctionForQuadraticProgramming();
				return solveWithQuadraticProgramming();
			}
		}
		return null;
	}


	/**
	 * Calls SCPsolver to solve the problem with linear programming
	 * @throws IloException 
	 */
	private double[] solveWithLinearProgramming() throws IloException {
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
		IloLinearNumExpr lin = cplex.scalProd(target, x);

		// only for FluxMinimization has the target function be minimized
		if (targetFunc instanceof FluxMinimization) {
			cplex.addMinimize(lin);
		} else {
			cplex.addMaximize(lin);
		}

		//CONTRAINTS
		//TODO

		// contraint J_i * G_i < 0
		double[] flux = targetFunc.getFluxVector();
		double[] gibbs = constraints.getGibbsEnergies();
		for (int i = 0; i< counter[1]; i++) {
			//jg is the expression for J_i * G_i
			IloNumExpr jg = cplex.prod(cplex.prod(flux[i], x[i]),gibbs[i]);
			cplex.addLe(jg, 0);
		}
		
		//contraint gibbs errors: delta_r(G_j^0) - E_j + R * T * sum(n_ij * ln[S_i]) = delta_r(G_j)
		//TODO
		
		
		// now solve the problem and get the solution array for the variables x
		double[] solution = null;
		if (cplex.solve()) {
			// get the from cplex computed values for the variables x
			solution = cplex.getValues(x);
		}
		cplex.end();
		return solution;
	}

	/**
	 * Calls CPLEX to solve the problem with quadratic programming
	 * @throws IloException 
	 */
	private double[] solveWithQuadraticProgramming() throws IloException {
		IloCplex cplex = new IloCplex();

		// TARGET
		// create upper bounds (ub) and lower bounds (lb) for the variables x
		int[] count = targetFunc.getCounterArray();
		// counter[1] contains the length of the flux vector
		for (int i = count[1]; i< target.length; i++) {
			lb[i]= 0.0; 
			ub [i] = Double.MAX_VALUE;
		}
		// create variables with upper bounds and lower bounds
		IloNumVar[] x = cplex.numVarArray(target.length, lb, ub);
		
		
		
		// TODO concentrations are now quadratic!!


		//TODO CONSTRAINTS

		
		// now solve the problem and get the solution array for the variables x
		double[] solution = null;
		if (cplex.solve()) {
			solution = cplex.getValues(x);
		}
		cplex.end();
		return solution;
	}

	/**
	 * @param linearProgramming the linearProgramming to set
	 */
	public void setLinearProgramming(boolean linearProgramming) {
		this.linearProgramming = Boolean.valueOf(linearProgramming);
	}

	/**
	 * @return the linearProgramming
	 */
	public Boolean isLinearProgramming() {
		return linearProgramming;
	}


	/**
	 * true if the boolean linearProgramming is set, else false
	 * @return
	 */
	public boolean isSetLinearProgramming() {
		return (isLinearProgramming() != null);
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
