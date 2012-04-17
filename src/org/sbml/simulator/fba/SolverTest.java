package org.sbml.simulator.fba;

import java.util.Arrays;

import scpsolver.constraints.LinearBiggerThanEqualsConstraint;
import scpsolver.lpsolver.LinearProgramSolver;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.problems.LinearProgram;


public class SolverTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LinearProgram lp = new LinearProgram(new double[]{5.0,10.0});
		lp.addConstraint(new LinearBiggerThanEqualsConstraint(new double[]{1.0,1.0}, 8.0, "c1"));
		lp.setMinProblem(true);
		lp.setLowerbound(new double[]{0.0,0.0} );
		lp.setUpperbound(new double[]{10.0,10.0});
	
		LinearProgramSolver solver  = SolverFactory.newDefault();
		double[] sol = solver.solve(lp);
		System.out.println(Arrays.toString(sol));
	}

}
