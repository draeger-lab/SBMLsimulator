/*
 * $Id:  FluxMinimization.java 16:15:22 Meike Aichele$
 * $URL: FluxMinimization.java $
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

import org.sbml.simulator.stability.math.StoichiometricMatrix;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class FluxMinimization extends TargetFunction {
	
	/**
	 * to compute a flux minimization, we need a flux vector 
	 * in an array fluxVector and an error-value-array errorArray, 
	 * which will be computed in Errorobject-class
	 */
	private double[] errorArray;
	private double[] fluxVector;
	private StoichiometricMatrix N;
	
	/*
	 * default constructor
	 */
	public FluxMinimization(StoichiometricMatrix N) {
		super();
		this.errorArray = computeError();
		this.fluxVector = FluxMinimizationUtils.computeFluxVector(N);
		this.N = N;
	}
	
	private double[] computeError() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * constructor that gets an error-array and a FluxVector-Array fluxvec.
	 * 
	 * @param error
	 * @param fluxvec
	 */
	public FluxMinimization(double[] error,double[] fluxvec) {
	}
}
