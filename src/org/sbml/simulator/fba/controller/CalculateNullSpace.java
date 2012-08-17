/**
 * 
 */
package org.sbml.simulator.fba.controller;

import org.sbml.simulator.stability.math.StoichiometricMatrix;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

/**
 * @author tscherneck
 *
 */
public class CalculateNullSpace {

	public static double[] CalculateNullSpace(StoichiometricMatrix N) throws IloException {
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
		
		if(cplex.solve()) {
			solution = cplex.getValues(x);
		}
		
		return solution;

	}
	


}
