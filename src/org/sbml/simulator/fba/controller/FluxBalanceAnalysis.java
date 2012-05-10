/*
 * $Id:  FluxBalanceAnalysis.java 16:07:08 Meike Aichele$
 * $URL: FluxBalanceAnalysis.java $
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
public class FluxBalanceAnalysis {

	/**
	 * FluxBalanceAnalysis has two attributes:
	 * - the target function targetFunc, which can be a FluxMinimization-object 
	 *   or an other function for which the network has to be optimized
	 * - the constraints, which contains e.g. Gibbs-energies
	 * 
	 */
	public TargetFunction targetFunc;
	public Constraints constraints;
	
	public FluxBalanceAnalysis(TargetFunction target, Constraints constraints) {
		this.targetFunc = target;
		this.constraints = constraints;
		
		// TODO Auto-generated constructor stub
	}
	
	

}
