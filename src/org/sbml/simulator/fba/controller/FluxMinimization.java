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

import eva2.tools.math.Jama.Matrix;


/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class FluxMinimization implements TargetFunction {

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
	 * An array of the concentrations in the model.
	 */
	private double[] concentrations;

	private double c_eq;

	private double[] gibbs;

	private double[] L;

	/**
	 * Constructor, that gets a {@link StoichiometricMatrix} and computes 
	 * the fluxVector an the errorArray itself.
	 * 
	 * @param N
	 */
	public FluxMinimization(StoichiometricMatrix N, double c_eq, double[] gibbs) {
		this.errorArray = FluxMinimizationUtils.computeError();
		this.fluxVector = FluxMinimizationUtils.computeFluxVector(N);

		if(gibbs != null && N != null) {
			this.L = computeL(N,gibbs);
		} else {
			this.L = null;
		}
		this.c_eq = c_eq;
		this.gibbs = gibbs;
		this.concentrations = computeConcentrations(N);
	}


	/**
	 *  Constructor, that gets a {@link SBMLDocument} and computes the
	 *  {@link StoichiometricMatrix} from the given document, the fluxVector
	 *  from the computed StoichiometricMatrix and the errorArray itself.
	 *  
	 * @param doc
	 */
	public FluxMinimization(SBMLDocument doc) {
		this(FluxMinimizationUtils.SBMLDocToStoichMatrix(doc), 0, null);
	}

	/**
	 * computes the transposed kernel matrix of the reduced stoichiometric matrix N
	 * multiplied with the reaction Gibbs energy values for the internal reactions of the system.
	 * @param n
	 * @param gibbs2
	 * @return L = (K_int^T) * (Delta_r(gibbs))_int
	 */
	private double[] computeL(StoichiometricMatrix n, double[] gibbs2) {
		// TODO: L = (K_int^T) * (Delta_r(gibbs))_int
		Matrix K_int = n.getReducedMatrix().svd().getV().transpose();
		double[] erg = new double[gibbs.length];
		for (int i = 0; i< gibbs.length; i++) {
			for (int j = 0; j < K_int.getColumnDimension(); j++) {
				erg[i] += K_int.get(i, j)*gibbs[j];
			}
		}
		return erg;
	}

	/**
	 * 
	 * @param n
	 * @return
	 */
	private double[] computeConcentrations(StoichiometricMatrix n) {
		// TODO Auto-generated method stub
		SBMLDocument doc= null;
		doc.getModel().getSpecies(0).getInitialConcentration(); //??
		return null;
	}
	

	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#computeTargetFunctionForQuadraticProgramming()
	 */
	@Override
	public double[] computeTargetFunctionForQuadraticProgramming() {
		// the function to minimize is: ||J|| + lambda1*sum((c_i - c_eq)^2) + lambda2*||L|| + lambda3*||E|| + lambda4*||G||

		// create the target vector 
		double[] target = new double[(fluxVector.length-1) + 
		                             (concentrations.length-1) + 
		                             (L.length -1) + (errorArray.length -1) + 
		                             (gibbs.length -1)];
		// this is a pointer, which counts in the target vector the actually position
		int counter = 0;

		// the quadratic target function is like the linear one with two additional vectors: 
		// the L vector and the gibbs vector: therefore we can call first the linear method and add
		// the additional ones
		double[] lineartarget = computeTargetFunctionForLinearProgramming();
		for (int i = 0; i < lineartarget.length; i++) {
			target[counter] = lineartarget[i];
			counter++;
		}

		// ||L||: lambda2*||L||
		for (int h = 0; h < this.L.length; h++) {
			target[counter] = lambda2 * L[h];
			counter++;
		}
		// the weighted gibbs energy: lambda4*||G||
		for (int l = 0; l < this.gibbs.length; l++) {
			target[counter] = lambda4 * gibbs[l];
		}

		return target;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#computeTargetFunctionForLinearProgramming()
	 */
	@Override
	public double[] computeTargetFunctionForLinearProgramming() {
		// the target function is: ||J|| + lambda1*sum((c_i - c_eq)^2) + lambda3*||E||

		double[] target = new double[(fluxVector.length-1) + 
		                             (concentrations.length-1) + 
		                             (errorArray.length -1)];
		int counter = 0;
		// fill it with the flux vector: ||J||
		for (int i=0; i< this.fluxVector.length; i++) {
			target[counter] = fluxVector[i];
			counter++;
		}
		// then the weighted concentrations: lambda1*sum((c_i - c_eq)^2)
		for (int j = 0; j < this.concentrations.length; j++) {
			target[counter] = lambda3 * Math.pow((concentrations[j] - c_eq),2);
			counter++;
		}
		// the weighted error: lambda3*||E||
		for (int k = 0; k < this.errorArray.length; k++) {
			target[counter] = lambda1 * errorArray[k];
			counter++;
		}

		return target;
	}

	/**
	 * @return the concentrations
	 */
	public double[] getConcentrations() {
		return concentrations;
	}

	/**
	 * @param concentrations the concentrations to set
	 */
	public void setConcentrations(double[] concentrations) {
		this.concentrations = concentrations;
	}

	/**
	 * @return the c_eq
	 */
	public double getC_eq() {
		return c_eq;
	}

	/**
	 * @param c_eq the c_eq to set
	 */
	public void setC_eq(double c_eq) {
		this.c_eq = c_eq;
	}

	/**
	 * @return the gibbs
	 */
	public double[] getGibbs() {
		return gibbs;
	}

	/**
	 * @param gibbs the gibbs to set
	 */
	public void setGibbs(double[] gibbs) {
		this.gibbs = gibbs;
	}



}
