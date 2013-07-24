/*
 * $Id:  FluxMinimization.java 16:15:22 Meike Aichele$
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

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
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
public class FluxMinimization extends TargetFunction {

	/**
	 * Contains the computed gibbs energies in the model.
	 */
	private double[] computedGibbsEnergies;

	/**
	 * counts the lengths of the components in the target array
	 */
	private int[] counterArray;

	/**
	 * Contains the concentrations in the model in steady-state.
	 */
	private double[] equilibriumsConcentrations;

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
	private double[] initialConcentrations;

	/**
	 * Vector that is made up of the transposed null space matrix K and
	 * the corresponding gibbs energies.
	 */
	private double[] L = {};

	/**
	 * Contains the computed stoichiometric matrix N.
	 */
	private StoichiometricMatrix N;

	/**
	 * Current {@link SBMLDocument}
	 */
	private SBMLDocument oriDocument;

	/**
	 * Constructor, that gets a {@link StoichiometricMatrix} and computes 
	 * the fluxVector, the Gibbs energies, the concentrations and the errorArray itself.
	 *
	 * @param originalDocument
	 * @param constraints
	 * @param N_int (stoichiometric matrix)
	 * @param targetFluxes, important for computing the flux vector
	 * @throws Exception 
	 */
	public FluxMinimization(SBMLDocument originalDocument, Constraints constraints, StoichiometricMatrix N_int, String[] targetFluxes) throws Exception {

		// set the fields of this object
		this.oriDocument = originalDocument;
		this.fluxVector = FluxMinimizationUtils.computeFluxVector(N_int, targetFluxes, originalDocument);
		setC_eq(constraints.getEquilibriumConcentrations());

		this.N = N_int;

		// get the computed Gibbs energies for the incoming Gibbs energies in steady state
		this.computedGibbsEnergies = constraints.getComputedGibbsEnergies();

		// get the error array
		if (computedGibbsEnergies != null) {
			this.errorArray = FluxMinimizationUtils.computeError(computedGibbsEnergies.length);
		} else {
			this.errorArray = new double[0];
		}

		// compute the initial concentrations
		this.initialConcentrations = extractConcentrations(oriDocument);

		// compute L or let it be null if the Gibbs energies couldn't be computed
		if(computedGibbsEnergies != null && oriDocument != null) {
			this.L = computeL(oriDocument);
		} else {
			L = new double[0];
		}

		counterArray = new int[4];
		if (fluxVector != null && errorArray != null && L != null) {
			fillCounterArray(fluxVector.length,		// J
					errorArray.length, 				// E
					L.length);						// L
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
	 * @throws Exception 
	 */
	public FluxMinimization(SBMLDocument doc, double[] c_eq, double[] gibbs_eq, String[] targetFluxes) throws Exception {
		this(doc,new Constraints(doc, gibbs_eq, c_eq, null),FluxMinimizationUtils.getExpandedStoichiometricMatrix(doc), targetFluxes);
	}

	/**
	 * 
	 * @param doc
	 * @param constraints
	 * @param targetFluxes
	 * @throws Exception
	 */
	public FluxMinimization(SBMLDocument doc, Constraints constraints, String[] targetFluxes) throws Exception {
		this(doc, constraints, FluxMinimizationUtils.getExpandedStoichiometricMatrix(doc), targetFluxes);
	}

	/**
	 * Computes the transposed kernel matrix of the reduced stoichiometric matrix N
	 * multiplied with the reaction Gibbs energy values for the internal reactions of the system.
	 * 
	 * @param originalDocument
	 * @return L = (K_int^T) * (Delta_r(gibbs))_int
	 * @throws Exception 
	 */
	private double[] computeL(SBMLDocument originalDocument) throws Exception {
		// get the kernel (K_int) of this StoichiometricMatrix and transpose it
		StoichiometricMatrix N_int_sys = FluxMinimizationUtils.getExpandedStoichiometricMatrix(originalDocument);
		Matrix transposedK_int = new StoichiometricMatrix(N_int_sys.transpose().getArray(), N_int_sys.getColumnDimension(), N_int_sys.getRowDimension()).getConservationRelations();
		double[] vectorL = new double[computedGibbsEnergies.length];
//		int k = 0;
		for (int i = 0; i < vectorL.length; i++) {
//			k = FluxMinimizationUtils.remainingList.get(i); // k-th element of the computed Gibbs energies
			for (int j = 0; j < transposedK_int.getRowDimension(); j++) {
				// compute L = (K_int^T) * (Delta_r(gibbs))_int
				vectorL[i] += transposedK_int.get(j,i) * computedGibbsEnergies[i];
			}
		}
		return vectorL;
	}


	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#computeTargetFunctionForQuadraticProgramming()
	 */
	@Override
	public double[] computeTargetFunctionForQuadraticProgramming() {
		// the function to minimize is: ||J|| + lambda1*sum((c_i - c_eq)^2) + lambda2*||L|| + lambda3*||E|| + lambda4*||deltaG||
	
		// create the target vector 
		double[] target = new double[fluxVector.length + 				// ||J||
		                             L.length +							// ||L||
		                             errorArray.length + 				// ||E||
		                             computedGibbsEnergies.length];		// ||deltaG||
	
		// this is a pointer, which counts in the target vector the actually position
	
		int counter = 0;
		
	
		// fill the target with the flux vector: ||J|| 
		for (int i=0; i< this.fluxVector.length; i++) {
			target[i] = Math.abs(fluxVector[i]);
			counter++;
		}
	
		/*
		 *  concentrations left out because they are quadratic and must be computed in 
		 *  FluxBalanceAnalysis
		 *  
		 */
	
	
		// ||L||: lambda2*||L||
		for (int h = 0; h < this.L.length; h++) {
			if (!Double.isNaN(L[h])) {
				target[counter] = lambda2 * Math.abs(L[h]);
			} else {
				target[counter] = lambda2;
			}
			counter++;
		}
	
		// the weighted error: lambda3*||E||
		for (int k = 0; k < this.errorArray.length; k++) {
			target[counter] = lambda3 * Math.abs(errorArray[k]);
			counter++;
		}
	
		// the weighted Gibbs energy: lambda4*||G||
		for (int l = 0; l < this.computedGibbsEnergies.length; l++) {
			if (!Double.isNaN(computedGibbsEnergies[l]) && !Double.isInfinite(computedGibbsEnergies[l])) {
				target[counter] = lambda4 * Math.abs(computedGibbsEnergies[l]);
			} else {
				target[counter] = lambda4;
			}
			counter++;
		}
		return target;
	}


	/**
	 * Fills the concentrations-array with the initial concentrations/amounts of
	 * the {@link Species} in this {@link Model}.
	 * 
	 * @param document
	 * @return concentrations-array
	 */
	private double[] extractConcentrations(SBMLDocument document) {
		Model model = document.getModel();
		initialConcentrations = new double[model.getSpeciesCount()];
		Species currentSpecies;
		for (int i = 0; i < model.getSpeciesCount(); i++) {
			currentSpecies = model.getSpecies(i);
			if (currentSpecies.hasOnlySubstanceUnits()) {
				if (currentSpecies.isSetInitialConcentration()) {
					// multiply with the volume of the compartment
					initialConcentrations[i] = currentSpecies.getInitialConcentration()* currentSpecies.getCompartmentInstance().getSize();
				} else if (currentSpecies.isSetInitialAmount()) {
					// do nothing
					initialConcentrations[i] = currentSpecies.getInitialAmount();
				}
				else {
					// TODO no initialConcentration and no initialAmount isSet --> unknown
				}
			} else {
				if (currentSpecies.isSetInitialConcentration()) {
					// do nothing
					initialConcentrations[i] = currentSpecies.getInitialConcentration();
				} else if (currentSpecies.isSetInitialAmount()) {
					// divide through the volume of the compartment
					if (currentSpecies.getCompartmentInstance().isSetSize() && currentSpecies.getCompartmentInstance().getSize() != 0) {
						initialConcentrations[i] = currentSpecies.getInitialAmount() / currentSpecies.getCompartmentInstance().getSize();
					}
				}
				else {
					// TODO no initialConcentration and no initialAmount isSet --> unknown
				}
			}
		}
		return initialConcentrations;
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
		counterArray[2] = counterArray[1] + length2;
		counterArray[3] = counterArray[2] + length3;
	}


	/**
	 * @return the equilibriumsConcentrations
	 */
	public double[] getC_eq() {
		return equilibriumsConcentrations;
	}


	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#getConcentrations()
	 */
	public double[] getConcentrations() {
		return initialConcentrations;
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


	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#getFluxVector()
	 */
	public double[] getFluxVector() {
		return this.fluxVector;
	}


	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#getGibbs()
	 */
	public double[] getGibbs() {
		return computedGibbsEnergies;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#getStoichiometricMatrix()
	 */
	public double[][] getStoichiometricMatrix() {
		return N.getArray();
	}


	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.fba.controller.TargetFunction#isMinProblem()
	 */
	public boolean isMinProblem() {
		return true;
	}


	/**
	 * If the incoming array is not null, then set it, 
	 * else set the content to 1, so that the variables for this array are later
	 * not weighted.
	 * @param c_eq
	 */
	public void setC_eq(double[] c_eq) {
		if (c_eq != null) {
			this.equilibriumsConcentrations = c_eq;
		} else {
			this.equilibriumsConcentrations = new double[oriDocument.getModel().getSpeciesCount()];
			for (int i = 0; i < this.equilibriumsConcentrations.length; i++) {
				this.equilibriumsConcentrations[i] = 1;
			}
		}
	}


	/**
	 * @param concentrations the concentrations to set
	 */
	public void setConcentrations(double[] concentrations) {
		this.initialConcentrations = concentrations;
	}


	/**
	 * @param gibbs the gibbs to set
	 */
	public void setGibbs(double[] gibbs) {
		this.computedGibbsEnergies = gibbs;
	}

}
