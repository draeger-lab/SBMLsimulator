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

import org.simulator.math.odes.MultiTable;

import de.zbit.sbml.util.CellDesignerAnnotationParser;

/**
 * 
 * @author Robin F&auml;hnrich
 * @version $Rev$
 * @since 1.0
 */
public abstract class TargetFunction {
	
	/**
	 * Logger
	 */
	private static final transient Logger logger = Logger.getLogger(TargetFunction.class.getName());
	
	/**
	 * @return The computed concentrations optimized by the target function
	 */
	public abstract MultiTable getOptimizedConcentrations();
	
	/**
	 * @return The computed flux vector optimized by the target function
	 */
	public abstract double[] getOptimizedFluxVector();
	
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
	 * 
	 * @return
	 * @throws IloException
	 */
	public IloCplex prepareCplex() throws IloException {
		IloCplex cplex = new IloCplex();
		// TODO implement method
		return cplex;
	}
	
	/**
	 * If the target function belongs to a minimization problem, minimize it,
	 * if the target function belongs to a maximization problem, maximize it.
	 * @param cplex
	 * @param expr
	 * @throws IloException 
	 */
	public void minOrMaxTargetFunction(IloCplex cplex, IloNumExpr expr) throws IloException {
		if (isMinProblem()) {
			cplex.addMinimize(expr);
		} else if (isMaxProblem()) {
			cplex.addMaximize(expr);
		} else {
			System.err.println("No minimization or maximization problem!");
		}
	}
	
	/**
	 * Let CPLEX solve the optimization problem (if necessary check the CPLEX 
	 * iterations) and get the solution of the variables in a double array.
	 * @param cplex
	 * @param vars
	 * @return The array with the solution variables optimized by CPLEX
	 * @throws IloException
	 */
	public double[] solveAndFinishCplex(IloCplex cplex, IloNumVar[] vars) throws IloException {
		// TODO write method to set cplexIterations (now: 600)
		cplex.setParam(IloCplex.IntParam.BarItLim, 600);
		
		double[] solution;
		
		if (cplex.solve()) {
			solution = cplex.getValues(vars);
		} else {
			solution = null;
//			System.err.println("No feasible solution found!");
			logger.warning("No feasible solution found!");
		}
		
		cplex.end();
		
		return solution;
	}
	
}
