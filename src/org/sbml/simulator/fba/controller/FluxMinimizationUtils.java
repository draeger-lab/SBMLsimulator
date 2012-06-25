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

import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBO;
import org.sbml.jsbml.Species;
import org.sbml.simulator.stability.math.StabilityMatrix;
import org.sbml.simulator.stability.math.StoichiometricMatrix;

/**
 * Contains methods to compute the flux vector for FBA and a method to get the {@link StoichiometricMatrix} of an incoming {@link SBMLDocument}.
 * It also contains a method to compute the Error array for FluxMinimization.
 * 
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class FluxMinimizationUtils {

	/**
	 * Gets a {@link StoichiometricMatrix} and gives back the corresponding flux-vector in Manhattan-Norm, without the given 
	 * target fluxes.
	 * 
	 * @param N
	 * @param targetFluxes 
	 * @param doc 
	 * @return double[] flux vector
	 */
	public static double[] computeFluxVector(StoichiometricMatrix N, String[] targetFluxes, SBMLDocument doc) {
		StabilityMatrix steadyStateMatrix = N.getSteadyStateFluxes();
		double[] fluxVector = new double[steadyStateMatrix.getColumnDimension()];
		// fill the fluxVector
		for (int column=0; column < N.getSteadyStateFluxes().getColumnDimension(); column++) {
			if (targetFluxes == null || isNoTargetFlux(column, targetFluxes, doc)) {
				fluxVector[column] = computeManhattenNorm(steadyStateMatrix.getColumn(column));
			}
		}
		return fluxVector;
	}
	
	/**
	 * Gets a {@link StoichiometricMatrix} and gives back the corresponding flux-vector in Manhattan-Norm, without the given 
	 * target fluxes.
	 * 
	 * @param targetFluxes
	 * @param doc
	 * @return
	 */
	public static double[] computeFluxVector(String[] targetFluxes, SBMLDocument doc) {
		return computeFluxVector(SBMLDocToStoichMatrix(doc), targetFluxes, doc);
	}



	/**
	 * Looks if the reaction with the index column is one of the target fluxes. Therefore
	 * this method looks if the id of the reaction is in the String-array targetFluxes.
	 * 
	 * @param column
	 * @param targetFluxes
	 * @param doc
	 * @return true if the reaction with the index column is a target flux, false if not.
	 */
	private static boolean isNoTargetFlux(int column, String[] targetFluxes, SBMLDocument doc) {
		Reaction r = doc.getModel().getReaction(column);
			for (int i = 0; i < targetFluxes.length; i++) {
				if (r.getId().equals(targetFluxes[i])) {
					return false;
				}
			}
		return true;
	}



	/**
	 * Gets a {@link SBMLDocument} and gives back the corresponding {@link StoichiometricMatrix}.
	 * @param doc
	 * @return {@link StoichiometricMatrix}
	 */
	public static StoichiometricMatrix SBMLDocToStoichMatrix(SBMLDocument document){
		SBMLDocument doc = eliminateTransports(document);
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
					if (reac.getProductForSpecies(metabolit.getId()).isSetStoichiometry()) {
						sMatrix.set(j, i, reac.getProductForSpecies(metabolit.getId()).getStoichiometry());
					}
					else sMatrix.set(j, i, 1);
				} else if (reac.hasReactant(metabolit)) {
					// the stoichiometry of reactants is negative in the stoichiometricMatrix
					if (reac.getReactantForSpecies(metabolit.getId()).isSetStoichiometry()) {
						sMatrix.set(j, i, - reac.getReactantForSpecies(metabolit.getId()).getStoichiometry());
					}
					else sMatrix.set(j, i, - 1);
				}
			}
		}

		return sMatrix;
	}

	/**
	 * Eliminates the transport-reactions and gives back the new {@link SBMLDocument}.
	 * @param doc
	 * @return a new SBMLDocument without transport reactions
	 */
	public static SBMLDocument eliminateTransports(SBMLDocument doc) {
		SBMLDocument newDoc = doc;
		for (int i = 0; i < doc.getModel().getReactionCount(); i++) {
			if (SBO.isChildOf(doc.getModel().getReaction(i).getSBOTerm(), SBO.getTransport())) {
				// reaction i is a transport reaction: remove it
				newDoc.getModel().removeReaction(i);
			}
		}
		return newDoc;
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
	 * Computes the error for the target function with the given dimension
	 * @param length 
	 * @return double[]
	 */
	public static double[] computeError(int length) {
		double[] error = new double[length];
		// fill the error-vector with ones, so that it only consists of the variables
		for (int i = 0; i < error.length; i++) {
			error[i] = 1;
		}
		return error;
	}
}
