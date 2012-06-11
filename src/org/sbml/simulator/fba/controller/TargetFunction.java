/*
 * $Id:  TargetFunction.java 16:08:51 Meike Aichele$
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

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public interface TargetFunction {
	
	public static double lambda1 = 10;
	public static double lambda2 = 10;
	public static double lambda3 = 0.01;
	public static double lambda4 = 1.0;
	
	/**
	 * This method gives an array back, which contains the target function for quadratic programming 
	 * @return double[]
	 */
	public double[] computeTargetFunctionForQuadraticProgramming();
	
	/**
	 * This method gives an array back, which contains the target function for linear programming 
	 * @return double[]
	 */
	public double[] computeTargetFunctionForLinearProgramming();

	/**
	 * @return true, if the target function belongs to a minimization problem
	 */
	public boolean isMinProblem();
	
	/**
	 * @return the computed flux vector
	 */
	public double[] getFluxVector();
	
	/**
	 * @return the computed Gibbs energies
	 */
	public double[] getGibbs();

	/**
	 * @return the computed concentrations
	 */
	public double[] getConcentrations();
	
	/**
	 * @return the counter array to see where in the target array the different components 
	 * like the flux vector are.
	 */
	public int[] getCounterArray();
}
