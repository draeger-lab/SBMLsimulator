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

import org.sbml.jsbml.SBMLDocument;

import scpsolver.lpsolver.LinearProgramSolver;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.problems.LinearProgram;

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

	/**
	 * Constructor that get's a {@link Constraints}-Object and a {@link SBMLDocument} 
	 * and that set's the {@link# linearProgramming} true.
	 * 
	 * @param constraints
	 * @param doc
	 */
	public FluxBalanceAnalysis(double[] c_eq, Constraints constraints, SBMLDocument doc) {
		this(new FluxMinimization(doc, c_eq, constraints.getGibbsEnergies()), constraints, true);
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
	}

	/**
	 * Calls a method to solve the problem with linear programming if the boolean
	 * {@link# linearProgramming} is set and true, else it calls a method for quadratic programming.
	 * @return double[]
	 */
	public double[] solve() {
		if (this.isSetLinearProgramming()) {
			if (this.isLinearProgramming()) {
				return solveWithLinearProgramming();
			} else {
				return solveWithQuadraticProgramming();
			}
		}
		return null;
	}


	/**
	 * Calls SCPsolver to solve the problem with linear programming
	 */
	private double[] solveWithLinearProgramming() {
		double[] target = targetFunc.computeTargetFunctionForLinearProgramming();
		// TODO: add the constraints
		LinearProgramSolver solver  = SolverFactory.newDefault();

		LinearProgram lp = new LinearProgram(target);
		lp.setMinProblem(targetFunc.isMinProblem());

		double[] erg = solver.solve(lp);
		return erg;
	}

	/**
	 * Calls CPLEX to solve the problem with quadratic programming
	 */
	private double[] solveWithQuadraticProgramming() {
		double[] target = targetFunc.computeTargetFunctionForQuadraticProgramming();
		// TODO: call CPLEX
		return null;
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
}
