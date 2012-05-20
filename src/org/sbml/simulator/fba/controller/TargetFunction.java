/*
 * $Id:  TargetFunction.java 16:08:51 Meike Aichele$
 * $URL: TargetFunction.java $
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
public abstract class TargetFunction {
	
	public TargetFunction() {
		//TODO
	}
	
	//TODO: fill this class with methods for all target-functions
	
	/**
	 * This method gives an array back, which contains the different components for quadratic programming 
	 * @return double[]
	 */
	public abstract double[][] computeTargetFunctionForQuadraticProgramming();
	
	/**
	 * This method gives an array back, which contains the different components for linear programming 
	 * @return double[]
	 */
	public abstract double[][] computeTargetFunctionForLinearProgramming();

}
