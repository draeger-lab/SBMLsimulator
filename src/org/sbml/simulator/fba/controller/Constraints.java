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
	 * @throws Exception 
	 */
	public Constraints (SBMLDocument doc) throws Exception {
		this(doc, null, null);
	}


	/**
	 * Constructor that computes the GibbsEnergies from the incoming gibbs_eq
	 * @param doc
	 * @param gibbs_eq
	 * @param c_eq
	 * @throws Exception 
	 */
	public Constraints (SBMLDocument doc, double[] gibbs_eq, double[] c_eq) throws Exception {
		this.document = doc;
		equilibriumGibbsEnergies = gibbs_eq;
		equilibriumConcentrations = c_eq;
		computeGibbsEnergies(gibbs_eq);
	}

	/**
	 * Computes the Gibbs energies with the formula delta(Gibbs)_j = delta(Gibbs)_j_eq + R * T * ln(sum( N[j][i] * c_eq[j] ))
	 * @param steadyStateGibbs in kJ/mol
	 * @throws Exception 
	 */
	private double[] computeGibbsEnergies(double[] steadyStateGibbs) throws Exception {
		if (steadyStateGibbs != null && this.document != null && equilibriumConcentrations != null) {
			
			// initialize
			StoichiometricMatrix N = FluxMinimizationUtils.SBMLDocToStoichMatrix(document);
			SBMLDocument doc = FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(document);
			gibbsEnergies = new double[steadyStateGibbs.length];
			
			// compute delta(Gibbs)_j = delta(Gibbs)_j_eq + R * T * sum(N[j][i] * ln(c_eq[j]))
			for (int i=0; i< steadyStateGibbs.length; i++) {
				double sum = 0;
				// compute sum( N[i][j] * c_eq[j] )
				for (int j=0; j< N.getRowDimension(); j++) {
					if (!Double.isNaN(equilibriumConcentrations[j])) {
						sum += N.get(j, i) * Math.log(equilibriumConcentrations[j]);
					} 
				}
				// delta(Gibbs)_j = delta(Gibbs)_j_eq + R * T * sum( N[j][i] * ln(c_eq[j]) )
				// look if there is a reversible reaction and set the reverse reaction gibbs energie
				if (FluxMinimizationUtils.reversibleReactions.contains(doc.getModel().getReaction(i).getId())) {
					if (doc.getModel().containsReaction(doc.getModel().getReaction(i).getId() + "_rev")) {	
						gibbsEnergies[i] = (steadyStateGibbs[i]*1000) + R*T*sum;
						int index_of_rev_reac = doc.getModel().getListOfReactions().getIndex(doc.getModel().getReaction(doc.getModel().getReaction(i).getId() + "_rev"));
						// set the gibbs energie of the reverse reaction to the negtive 
						gibbsEnergies[index_of_rev_reac] = gibbsEnergies[i] * (-1);
					}
				} else {
					// it's no reversible reaction
					gibbsEnergies[i] = (steadyStateGibbs[i]*1000) + R*T*sum;
				}
			}
		}
		// return the computed Gibbs energies
		return gibbsEnergies;
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
	 * computed Gibbs energies by 
	 * delta(Gibbs)_j = delta(Gibbs)_j_eq + R * T * sum( N[j][i] * ln(c_eq[j]) )
	 * @return the computed gibbsEnergies
	 */
	public double[] getGibbsEnergies() {
		return gibbsEnergies;
	}

	/**
	 * Computes the maximum of J_i / G_i for every reaction i in the model
	 * @param fluxVector
	 * @return r_max
	 */
	public double computeR_max(double[] fluxVector) {
		double r_max = Double.MIN_NORMAL;
		if (gibbsEnergies != null) {
			if (fluxVector.length <= gibbsEnergies.length) {
				for(int i = 0; i < fluxVector.length; i++) {
					// if you divide by NaN, r_max will be NaN and if you divide by zero, you get r_max = infinity
					if (!Double.isNaN(gibbsEnergies[i]) && gibbsEnergies[i] != 0) {
						r_max = Math.max(r_max,(fluxVector[i]/gibbsEnergies[i]));
					}
				}
			} else {
				for (int i = 0; i < gibbsEnergies.length; i++) {
					if (!Double.isNaN(gibbsEnergies[i]) && gibbsEnergies[i] != 0) {
						r_max = Math.max(r_max,(fluxVector[i]/gibbsEnergies[i]));
					}
				}
			}
		}
		return r_max;
	}

}
