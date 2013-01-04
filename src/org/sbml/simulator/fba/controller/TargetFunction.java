/*
 * $Id:  TargetFunction.java 16:08:51 Meike Aichele$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2013 by the University of Tuebingen, Germany.
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
public abstract class TargetFunction {
	
	/**
	 * weighting factor for the optimization problem, 
	 * default value according to Ziller et al. (2009): 
	 * lambda1*sum((c_i - c_eq)^2)
	 */
	public static double lambda1 = 10;
	
	/**
	 * weighting factor for the optimization problem,
	 * default value according to Ziller et al. (2009):
	 * lambda2*||L||
	 */
	public static double lambda2 = 10;
	
	/**
	 * weighting factor for the optimization problem,
	 * default value according to Ziller et al. (2009):
	 * lambda3*||E||
	 */
	public static double lambda3 = 0.01;
	
	/**
	 * weighting factor for the optimization problem,
	 * default value according to Ziller et al. (2009):
	 * lambda4*||deltaG||
	 */
	public static double lambda4 = 1.0;
	
	/**
	 * This method gives an array back, which contains the target function for quadratic programming 
	 * @return double[]
	 */
	public abstract double[] computeTargetFunctionForQuadraticProgramming();

	/**
	 * @return the computed concentrations
	 */
	public abstract double[] getConcentrations();

	/**
	 * @return the counter array to see where in the target array the different components 
	 * like the flux vector are.
	 */
	public abstract int[] getCounterArray();

	/**
	 * @return the computed flux vector
	 */
	public abstract double[] getFluxVector();
	
	/**
	 * @return the computed Gibbs energies
	 */
	public abstract double[] getGibbs();

	/**
	 * @return the computed stoichiometric matrix N.
	 */
	public abstract double[][] getStoichiometricMatrix();
	
	/**
	 * @return true, if the target function belongs to a minimization problem
	 */
	public abstract boolean isMinProblem();

}
