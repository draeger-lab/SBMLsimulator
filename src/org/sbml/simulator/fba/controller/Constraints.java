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

import org.sbml.jsbml.Model;
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
	private double[] computedGibbsEnergies;

	/**
	 * Contains the opened {@link SBMLDocument}
	 */
	public SBMLDocument originalDocument;

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
	 * @throws Exception 
	 */
	public Constraints (SBMLDocument doc) throws Exception {
		this(doc, null, null);
	}


	/**
	 * Constructor that computes the GibbsEnergies from the incoming gibbs_eq
	 * @param originalDoc
	 * @param gibbs_eq
	 * @param c_eq
	 * @throws Exception 
	 */
	public Constraints (SBMLDocument originalDoc, double[] gibbs_eq, double[] c_eq) throws Exception {
		this(originalDoc, gibbs_eq, c_eq, false);
	}
	
	/**
	 * Constructor that computes the GibbsEnergies from the incoming gibbs_eq and c_eq.
	 * 
	 * @param originalDoc
	 * @param gibbs_eq
	 * @param c_eq
	 * @param kiloJoule (true, if gibbsEnergies are given in [kJ/mol])
	 * @throws Exception
	 */
	public Constraints (SBMLDocument originalDoc, double[] gibbs_eq, double[] c_eq, boolean kiloJoule) throws Exception {
		originalDocument = originalDoc;
		equilibriumConcentrations = c_eq;
			
		if (kiloJoule) {
			equilibriumGibbsEnergies = getEquillibriumGibbsEnergiesfromkKiloJoule(gibbs_eq);
		}
		else {
			equilibriumGibbsEnergies = gibbs_eq;
		}
		computeGibbsEnergies(equilibriumGibbsEnergies);
	}

	/**
	 * Converts Gibbs values given in [kJ/mol] to [J/mol].
	 * 
	 * @param gibbs_eq
	 * @return adapted Gibbs energies
	 */
	private double[] getEquillibriumGibbsEnergiesfromkKiloJoule(double[] gibbs_eq) {
		double[] adaptedGibbsEnergies = new double[gibbs_eq.length];
		for (int i = 0; i < gibbs_eq.length; i++) {
			adaptedGibbsEnergies[i] = gibbs_eq[i] / 1000;
		}
		return adaptedGibbsEnergies;
	}


	/**
	 * Computes the Gibbs energies with the formula delta(Gibbs)_j = delta(Gibbs)_j_eq + R * T * ln(sum( N[j][i] * c_eq[j] )).
	 * @param equilibriumsGibbs in kJ/mol
	 * @throws Exception 
	 */
	private double[] computeGibbsEnergies(double[] equilibriumsGibbs) throws Exception {
		if ((equilibriumsGibbs != null) && (this.originalDocument != null) && (equilibriumConcentrations != null)) {
			
			// initialize
			StoichiometricMatrix N = FluxMinimizationUtils.SBMLDocToStoichMatrix(originalDocument);
			SBMLDocument modifiedDocument = FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(originalDocument);
			computedGibbsEnergies = new double[equilibriumsGibbs.length];
			Model modModel = modifiedDocument.getModel();
			
			// compute delta(Gibbs)_j = delta(Gibbs)_j_eq + R * T * sum(N[i][j] * ln(S[i])) 
			for (int j = 0; j < modModel.getReactionCount(); j++) {
				double sum = 0;
				// compute sum( N[i][j] * ln(S_eq[i]) ) equals to the sum( N[i][j] * c_eq[i] ) 
				for (int i = 0; i < modModel.getSpeciesCount(); i++) {
					if (!Double.isNaN(equilibriumConcentrations[i])) {
						sum += N.get(i, j) * Math.log(equilibriumConcentrations[i]);
					} 
				}
				// delta(Gibbs)_j = delta(Gibbs)_j_eq + R * T * sum( N[i][j] * ln(c_eq[i]) )

				computedGibbsEnergies[j] = (equilibriumsGibbs[j]) + (R*T*sum);
			}
		}
		// return the computed Gibbs energies
		return computedGibbsEnergies;
	}

	/**
	 * @param gibbsEnergies the gibbsEnergies to set
	 * @throws Exception 
	 */
	public void setEquilibriumGibbsEnergies(double[] gibbsEnergies) throws Exception {
		this.equilibriumGibbsEnergies = gibbsEnergies;
		computeGibbsEnergies(gibbsEnergies);
	}

	/**
	 * @return the gibbsEnergies
	 */
	public double[] getEquilibriumGibbsEnergies() {
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
	 * Computed Gibbs energies by 
	 * delta(Gibbs)_j = delta(Gibbs)_j_eq + R * T * sum( N[i][j] * ln(S[i]) )
	 * @return the computed gibbsEnergies
	 */
	public double[] getComputedGibbsEnergies() {
		return computedGibbsEnergies;
	}

	
	/**
	 * Computes the maximum of J_j / G_j for every reaction j in the model
	 * @param fluxVector
	 * @return r_max
	 */
	public double[] computeR_max(double[] fluxVector) {
		double[] r_max = new double[fluxVector.length]; 
		
		// initialization
		for (int i = 0; i < r_max.length; i++) {
			r_max[i] = Double.MIN_NORMAL; 
		}
		
		if (computedGibbsEnergies != null) {
			for (int i = 0; i < r_max.length; i++) {
				// if you divide by NaN, r_max will be NaN and if you divide by zero, you get r_max = infinity
				if (!Double.isNaN(computedGibbsEnergies[i]) && computedGibbsEnergies[i] != 0) {
					r_max[i] = Math.max(r_max[i],(fluxVector[i]/computedGibbsEnergies[i]));
				}
			}
		}
		return r_max;
	}

}
