/*
 * $Id:  Constraints.java 16:17:07 Meike Aichele$
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
 * Containing the read equilibrium Gibbs energies and the computed Gibbs energies 
 * and the concentrations in steady state. Also it contains the temperature in Kelvin under standard conditions (T)
 * and ideal gas constant R in J/(mol * K).
 * 
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class Constraints {

	/**
	 * Contains the Gibbs energies in steady state
	 */
	private double[] equilibriumGibbsEnergies;

	/**
	 * Contains the computed Gibbs energies
	 */
	private double[] gibbsEnergies;

	/**
	 * Contains the opened {@link SBMLDocument}
	 */
	public SBMLDocument document;

	/**
	 * Contains the concentrations in equilibrium / steady state
	 */
	private double[] equilibriumConcentrations;

	/**
	 * Contains the temperature in Kelvin under standard conditions (25 degree Celsius)
	 */
	private double T = 298.15;

	/**
	 * The ideal gas constant R in J/(mol * K)
	 */
	private double R = 8.3144621;

	/**
	 * Constructor, that gets a {@link SBMLDocument} and creates a new
	 * array of Gibbs energies, that stays empty if the user doesn't check
	 * in a Gibbs energy file.
	 * 
	 * @param doc
	 */
	public Constraints (SBMLDocument doc) {
		this(doc, null, null);
	}


	/**
	 * Constructor that computes the GibbsEnergies from the incoming gibbs_eq
	 * @param doc
	 * @param gibbs_eq
	 * @param c_eq
	 */
	public Constraints (SBMLDocument doc, double[] gibbs_eq, double[] c_eq) {
		this.document = doc;
		equilibriumConcentrations = gibbs_eq;
		equilibriumConcentrations = c_eq;
		computeGibbsEnergies(gibbs_eq);
	}

	/**
	 * Computes the Gibbs energies with the formula delta(Gibbs)_j = delta(Gibbs)_j_eq + R * T * ln(sum( N[j][i] * c_eq[j] ))
	 * @param steadyStateGibbs
	 */
	private double[] computeGibbsEnergies(double[] steadyStateGibbs) {

		if (steadyStateGibbs != null && this.document != null && equilibriumConcentrations != null) {
			StoichiometricMatrix N = FluxMinimizationUtils.SBMLDocToStoichMatrix(document);
			gibbsEnergies = new double[steadyStateGibbs.length];
			for (int i=0; i< steadyStateGibbs.length; i++) {
				double sum = 0;
				// compute sum( N[j][i] * c_eq[j] )
				for (int j=0; j< N.getColumnDimension(); j++) {
					sum += N.get(i, j) * equilibriumConcentrations[j];
				}
				// delta(Gibbs)_j = delta(Gibbs)_j_eq + R * T * ln(sum( N[j][i] * c_eq[j] ))
				gibbsEnergies[i] = steadyStateGibbs[i] + R*T*Math.log(sum);
			}
		}
		// return the computed Gibbs energies
		return gibbsEnergies;
	}

	/**
	 * @param gibbsEnergies the gibbsEnergies to set
	 */
	public void setEquilibriumGibbsEnergies(double[] gibbsEnergies) {
		this.equilibriumGibbsEnergies = gibbsEnergies;
		computeGibbsEnergies(gibbsEnergies);
	}

	/**
	 * @return the gibbsEnergies
	 */
	public double[] getEquilibriumGibbsEnergiesGibbsEnergies() {
		return equilibriumGibbsEnergies;
	}

	/**
	 * @param c_eq the equilibrium concentrations to set
	 */
	public void setEquilibriumConcentrations(double[] c_eq) {
		this.equilibriumConcentrations = c_eq;
	}

	/**
	 * @return the equilibrium concentrations
	 */
	public double[] getEquilibriumConcentrations() {
		return equilibriumConcentrations;
	}

	/**
	 * @return the computed gibbsEnergies
	 */
	public double[] getGibbsEnergies() {
		return gibbsEnergies;
	}

	/**
	 * Computes the maximum of J_i / G_i for every reaction i in the model
	 * @param fluxVector
	 * @return
	 */
	public double computeR_max(double[] fluxVector) {
		double r_max = 0;
		if (gibbsEnergies != null) {
			if (fluxVector.length <= gibbsEnergies.length) {
				for(int i = 0; i < fluxVector.length; i++) {
					r_max = Math.max(r_max,(fluxVector[i]/gibbsEnergies[i]));
				}
			} else {
				for (int i = 0; i < gibbsEnergies.length; i++) {
					r_max = Math.max(r_max,(fluxVector[i]/gibbsEnergies[i]));
				}
			}
		}
		return r_max;
	}

}
