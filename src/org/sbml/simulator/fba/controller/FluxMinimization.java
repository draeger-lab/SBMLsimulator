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

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.simulator.stability.math.ConservationRelations;
import org.sbml.simulator.stability.math.StoichiometricMatrix;

import eva2.tools.math.Jama.Matrix;


/**
 * Computes and contains all components you need for a flux minimization target function for FBA (flux balance analysis),
 * like the error, the flux vector, the concentrations (in steady-state and at the beginning) and the gibbs energies
 * for the reactions in the model. 
 * 
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class FluxMinimization implements TargetFunction {

	/**
	 * To compute a flux minimization, we need an error-value-array 
	 * that contains the computed errors, which will be minimized in FBA. 
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

	/**
	 * Contains the concentrations in the model in steady-state.
	 */
	private double[] c_eq;

	/**
	 * Contains the computed gibbs energies in the model.
	 */
	private double[] gibbs;

	/**
	 * Vector that is made up of the transposed null space matrix K and
	 * the corresponding gibbs energies.
	 */
	private double[] L;

	/**
	 * counts the lengths of the components in the target array
	 */
	private int[] counterArray;

	/**
	 * Contains the computed stoichiometric matrix N.
	 */
	private StoichiometricMatrix N;
	
	/**
	 * Constructor, that gets a {@link StoichiometricMatrix} and computes 
	 * the fluxVector an the errorArray itself.
	 * 
	 * @param doc
	 * @param N
	 * @param c_eq
	 * @param gibbs_eq
	 * @param targetFluxes
	 */
	public FluxMinimization(SBMLDocument doc, StoichiometricMatrix N, double[] c_eq, double[] gibbs_eq, String[] targetFluxes) {
		this.fluxVector = FluxMinimizationUtils.computeFluxVector(N, targetFluxes, doc);
		this.c_eq = c_eq;
		this.N = N;
		Constraints c = new Constraints(doc, gibbs_eq, c_eq);
		this.gibbs = c.getGibbsEnergies();
		if (gibbs != null) {
			this.errorArray = FluxMinimizationUtils.computeError(gibbs.length);
		}
		this.concentrations = computeConcentrations(doc);
		if(gibbs != null && doc != null) {
			this.L = computeL(doc);
		} else {
			this.L = null;
		}
	}


	/**
	 * Constructor, that gets a {@link SBMLDocument} and computes the
	 *  {@link StoichiometricMatrix} from the given document, the fluxVector
	 *  from the computed StoichiometricMatrix and the errorArray itself.
	 * 
	 * @param doc
	 * @param c_eq
	 * @param gibbs_eq
	 * @param targetFluxes
	 */
	public FluxMinimization(SBMLDocument doc, double[] c_eq, double[] gibbs_eq, String[] targetFluxes) {
		this(doc,FluxMinimizationUtils.SBMLDocToStoichMatrix(doc), c_eq, gibbs_eq, targetFluxes);
	}
	

	/**
	 * Computes the transposed kernel matrix of the reduced stoichiometric matrix N
	 * multiplied with the reaction Gibbs energy values for the internal reactions of the system.
	 * @param doc
	 * @return L = (K_int^T) * (Delta_r(gibbs))_int
	 */
	private double[] computeL(SBMLDocument doc) {
		Matrix K_int_t = ConservationRelations.calculateConsRelations(FluxMinimizationUtils.SBMLDocToStoichMatrix(doc)).transpose();
		double[] erg = new double[gibbs.length];
		for (int i = 0; i< gibbs.length; i++) {
			for (int j = 0; j < K_int_t.getColumnDimension(); j++) {
				erg[i] += K_int_t.get(i, j)*gibbs[i];
			}
		}
		return erg;
	}


	/**
	 * Fills the concentrations-array with the initial concentrations/amounts of
	 * the {@link Species} in this {@link Model}.
	 * @param document
	 * @return concentrations-array
	 */
	private double[] computeConcentrations(SBMLDocument document) {
		Model model = document.getModel();
		concentrations = new double[model.getSpeciesCount()];
		Species currentSpecies;
		for (int i = 0; i < model.getSpeciesCount(); i++) {
			currentSpecies = model.getSpecies(i);
			if (currentSpecies.hasOnlySubstanceUnits()) {
				if (currentSpecies.isSetInitialConcentration()){
					// multiply with the volume of the compartment
					concentrations[i] = currentSpecies.getInitialConcentration()* document.getModel().getCompartment(currentSpecies.getCompartment()).getSize();
				} else if (currentSpecies.isSetInitialAmount()){
					// do nothing
					concentrations[i] = currentSpecies.getInitialAmount();
				}
			} else {
				if (currentSpecies.isSetInitialConcentration()){
					// do nothing
					concentrations[i] = currentSpecies.getInitialConcentration();
				} else if (currentSpecies.isSetInitialAmount()){
					// divide through the volume of the compartment
					concentrations[i] = currentSpecies.getInitialAmount() / currentSpecies.getCompartmentInstance().getSize();
				}
			}
		}
		return concentrations;
	}


	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#computeTargetFunctionForQuadraticProgramming()
	 */
	@Override
	public double[] computeTargetFunctionForQuadraticProgramming() {
		// the function to minimize is: ||J|| + lambda1*sum((c_i - c_eq)^2) + lambda2*||L|| + lambda3*||E|| + lambda4*||G||

		// create the target vector 
		double[] target = new double[fluxVector.length + 
		                             concentrations.length + 
		                             errorArray.length + L.length +
		                             gibbs.length];
		// this is a pointer, which counts in the target vector the actually position
		counterArray = new int[4];
		fillCounterArray(fluxVector.length, errorArray.length, L.length);
		int counter = 0;
		
		// fill it with the flux vector: ||J||
		for (int i=0; i< this.fluxVector.length; i++) {
			target[counter] = fluxVector[i];
			counter++;
		}
		
		// the weighted error: lambda3*||E||
		for (int k = 0; k < this.errorArray.length; k++) {
			target[counter] = lambda1 * errorArray[k];
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
			counter++;
		}

		// concentrations left out because they are quadratic and must be computed in 
		// FluxBalanceAnalysis
		return target;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#getConcentrations()
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
	public double[] getC_eq() {
		return c_eq;
	}

	/**
	 * @param c_eq the c_eq to set
	 */
	public void setC_eq(double[] c_eq) {
		this.c_eq = c_eq;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#getGibbs()
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

	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#isMinProblem()
	 */
	@Override
	public boolean isMinProblem() {
		return true;
	}


	/**
	 * Fills the {@link# counterArray} to save the indices of the components in the target array.
	 * 
	 * @param length1: fluxVector length
	 * @param length2: errorArray length
	 * @param length3: L array length
	 */
	private void fillCounterArray(int length1, int length2, int length3) {
		counterArray[0] = 0;
		counterArray[1] = length1;
		counterArray[2] = length1 + length2;
		counterArray[3] = counterArray[2] + length3;
	}


	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#getFluxVector()
	 */
	@Override
	public double[] getFluxVector() {
		return this.fluxVector;
	}


	/**
	 * counterArray[0] = fluxvector index;
	 * counterArray[1] = errorArray index;
	 * counterArray[2] = L array index;
	 * counterArray[3] = Gibbs array index;
	 * 
	 * @return the counterArray
	 */
	public int[] getCounterArray() {
		return counterArray;
	}


	@Override
	public double[][] getStoichiometricMatrix() {
		return N.getArray();
	}

}
