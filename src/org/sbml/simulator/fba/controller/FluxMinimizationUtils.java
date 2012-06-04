/*
 * $Id:  FluxVector.java 16:25:58 Meike Aichele$
 * $URL: FluxVector.java $
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

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ListOf.Type;
import org.sbml.simulator.stability.math.StabilityMatrix;
import org.sbml.simulator.stability.math.StoichiometricMatrix;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class FluxMinimizationUtils {

	/**
	 * Gets a {@link StoichiometricMatrix} and gives back the corresponding flux-vector in Manhattan-Norm.
	 * @param N
	 * @return double[] flux vector
	 */
	public static double[] computeFluxVector(StoichiometricMatrix N) {
		StabilityMatrix steadyStateMatrix = N.getSteadyStateFluxes();
		double[] fluxVector = new double[steadyStateMatrix.getColumnDimension()];
		// fill the fluxVector
		for (int column=0; column < N.getSteadyStateFluxes().getColumnDimension(); column++) {
			fluxVector[column] = computeManhattenNorm(steadyStateMatrix.getColumn(column));
		}
		return fluxVector;
	}

	/**
	 * Gets a {@link SBMLDocument} and gives back the corresponding {@link StoichiometricMatrix}.
	 * @param doc
	 * @return {@link StoichiometricMatrix}
	 */
	public static StoichiometricMatrix SBMLDocToStoichMatrix(SBMLDocument doc){
		// build a new StoichiometricMatrix with the number of metabolites as the dimension of rows
		// and the number of reactions as the dimension of columns
		int metaboliteCount = doc.getModel().getSpeciesCount();
		int reactionCount = doc.getModel().getReactionCount();
		StoichiometricMatrix sMatrix = new StoichiometricMatrix (metaboliteCount, reactionCount);

		//fill the matrix with the stoichiometry of each reaction
		for (int i = 0; i < reactionCount; i++) {
			for (int j = 0; j < metaboliteCount; j++) {
				Reaction reac = doc.getModel().getReaction(i);
				Species metabolit = doc.getModel().getSpecies(j);
				if (reac.hasProduct(metabolit)) {
					// the stoichiometry of products is positive in the stoichiometricMatrix
					if (reac.getProduct(metabolit.getId()).isSetStoichiometry()) {
						sMatrix.set(i, j, reac.getProduct(metabolit.getId()).getStoichiometry());
					}
				} else if (reac.hasReactant(metabolit)) {
					// the stoichiometry of reactants is negative in the stoichiometricMatrix
					if (reac.getReactant(metabolit.getId()).isSetStoichiometry()) {
						sMatrix.set(i, j, - reac.getReactant(metabolit.getId()).getStoichiometry());
					}
				}
			}
		}

		return sMatrix;
	}

	/**
	 * Computes the Manhattan-Norm ||vector|| = sum up from 1 to |vector|: |v_i|        
	 * e.g.:
	 * vector = (1,-2,3) than ||vector|| = ||(1,-2,3)|| = |1| + |-2| + |3| = 1 + 2 + 3 = 6
	 * 
	 * @param vector
	 * @return Manhattan-Norm of the vector
	 */
	public static double computeManhattenNorm(double[] vector) {
		double norm = 0;
		for (int i = 0; i < vector.length; i++) {
			norm += Math.abs(vector[i]);
		}
		return norm;
	}

	/**
	 * Computes the error for the target function
	 * @return double[]
	 */
	public static double[] computeError() {
		// TODO: compute the error
		return null;
	}
}
