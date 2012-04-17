package org.sbml.simulator.fba;

import scpsolver.constraints.LinearBiggerThanEqualsConstraint;
import scpsolver.constraints.LinearSmallerThanEqualsConstraint;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.qpsolver.QuadraticProgram;
import scpsolver.qpsolver.QuadraticProgramSolver;

public class Scptest {
	public static void main(String[] args) {
		QuadraticProgram qp = new QuadraticProgram(new double[][] { { 1, 0,0 },
				{ 0, 1, 0 } }, new double[] { 0.0, 0.0, 0.0 });
		qp.addConstraint(new LinearSmallerThanEqualsConstraint(new double[] {
				3.0, 16.0 , 0.0 }, 20, "A"));
				
		qp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[] {
				1.0, 0.0, 0.0 }, 2, "B"));
		qp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[] {
				1.0, 0.0, 0.0 }, 2, "B"));
		qp.setMinProblem(true);
		QuadraticProgramSolver solver = (QuadraticProgramSolver) SolverFactory
				.getSolver("CPLEX");
		double[] sol = solver.solve(qp);
		for (int i = 0; i < sol.length; i++) {
			System.out.println(i + " " + sol[i]);
		}

	}
}
