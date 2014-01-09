/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2014 by the University of Tuebingen, Germany.
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

import org.sbml.simulator.stability.math.StoichiometricMatrix;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

/**
 * @author Stephanie Tscherneck
 * @version $Rev$
 */
public class CalculateNullSpace {

	/**
	 * 
	 * @param N
	 * @return
	 * @throws IloException
	 */
	public static double[] calculateNullSpace(StoichiometricMatrix N) throws IloException {
		// create the cplex solver
		IloCplex cplex = new IloCplex();
		double[] solution = new double[N.getColumnDimension()];
		double[] lb = new double[N.getColumnDimension()];
		double[] ub = new double[N.getColumnDimension()];
		for (int i = 0; i < ub.length; i++) {
			lb[i] = 0;
			ub[i] = 1;
		}
		
		IloNumVar[] x = cplex.numVarArray(N.getColumnDimension(), lb, ub);

		IloNumExpr cplex_target = cplex.sum(x);
		cplex.addMaximize(cplex_target);
		
		IloNumExpr expr = cplex.numExpr();
		for (int i = 0; i < N.getRowDimension(); i++) {
			expr = cplex.scalProd(x, N.getRow(i));
			System.out.println(i + ": " + expr);
			cplex.addEq(expr, 0);
		}
		
		if (cplex.solve()) {
			solution = cplex.getValues(x);
		}
		
		return solution;
	}

}
