/*
 * $Id:  FluxMinimization.java 16:15:22 Meike Aichele$
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

import org.sbml.jsbml.SBMLDocument;
import org.sbml.simulator.stability.math.StoichiometricMatrix;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class FluxMinimization extends TargetFunction {
	
	/**
	 * To compute a flux minimization, we need an error-value-array 
	 * that contains the computed errors. 
	 */
	private double[] errorArray;
	
	/**
	 * To compute a flux minimization, we need a flux-vector, that
	 * contains the steady-state-fluxes in the model.
	 */
	private double[] fluxVector;
	
	/**
	 * Contains the stoichiometry of the model.
	 */
	private StoichiometricMatrix N;
	
	/**
	 * Constructor, that gets a {@link StoichiometricMatrix} and computes 
	 * the fluxVector an the errorArray itself.
	 * 
	 * @param N
	 */
	public FluxMinimization(StoichiometricMatrix N) {
		super();
		this.errorArray = computeError();
		this.fluxVector = FluxMinimizationUtils.computeFluxVector(N);
		this.N = N;
	}
	
	/**
	 *  Constructor, that gets a {@link SBMLDocument} and computes the
	 *  {@link StoichiometricMatrix} from the given document, the fluxVector
	 *  from the computed StoichiometricMatrix and the errorArray itself.
	 *  
	 * @param doc
	 */
	public FluxMinimization(SBMLDocument doc) {
		this(FluxMinimizationUtils.SBMLDocToStoichMatrix(doc));
	}
	
	/**
	 * Constructor that gets an error-array and a FluxVector-Array fluxvec.
	 * 
	 * @param error
	 * @param fluxvec
	 */
	public FluxMinimization(double[] error,double[] fluxvec) {
		this.errorArray = error;
		this.fluxVector = fluxvec;
	}
	
	private double[] computeError() {
		// TODO: compute the error
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#computeTargetFunctionForQuadraticProgramming()
	 */
	@Override
	public double[][] computeTargetFunctionForQuadraticProgramming() {
		// TODO fill the matrix with the needed vectors
		double[][] target = new double[6][];
		
		target[0] = this.fluxVector;
		target[1] = this.errorArray;
		
		return target;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#computeTargetFunctionForLinearProgramming()
	 */
	@Override
	public double[][] computeTargetFunctionForLinearProgramming() {
		// TODO fill the matrix with the needed vectors
		return null;
	}
}
