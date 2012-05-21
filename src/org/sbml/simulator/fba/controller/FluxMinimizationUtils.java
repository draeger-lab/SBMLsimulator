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
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ListOf.Type;
import org.sbml.simulator.stability.math.StoichiometricMatrix;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class FluxMinimizationUtils {

	/**
	 * Gets a {@link StoichiometricMatrix} and gives back the corresponding flux-vector.
	 * @param N
	 * @return
	 */
	public static double[] computeFluxVector(StoichiometricMatrix N) {
		N.getSteadyStateFluxes();
		double[] fluxVector = new double[N.getColumnDimension()];
		// TODO: fill the fluxVector
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
		for (int i = 0; i< metaboliteCount; i++) {
			for (int j = 0; j< reactionCount; j++) {
				SpeciesReference specRef = searchCorrespondingObjectInReaction(doc.getModel().getReaction(j), doc.getModel().getSpecies(i).getId());
				if (specRef == null){
					// if specRef is not in the reaction j
					sMatrix.set(i, j, 0);
				} else if (specRef.isSetParentSBMLObject()) {
					// check if specRef is a product or a reactant in reaction j
					if (((ListOf<?>)specRef.getParentSBMLObject()).getSBaseListType().equals(Type.listOfProducts)) {
						// the stoichiometry of products is positive in the stoichiometricMatrix
						if (specRef.isSetStoichiometry()) {
							sMatrix.set(i, j, specRef.getStoichiometry());
							// else the entry i,j stays 0 
						}
					}
					else if (((ListOf<?>)specRef.getParentSBMLObject()).getSBaseListType().equals(Type.listOfReactants)) {
						// the stoichiometry of reactants is negative in the stoichiometricMatrix
						if (specRef.isSetStoichiometry()) {
							sMatrix.set(i, j, - specRef.getStoichiometry());
							// else the entry i,j stays 0 
						}
					}
				}
			}
		}
		return sMatrix;
	}
	
	/**
	 * Gives the {@link SpeciesReference} back, when there is one with the given id in the given {@link Reaction}
	 * else this method returns null.
	 * 
	 * @param reac
	 * @param id
	 * @return {@link SpeciesReference}
	 */
	private static SpeciesReference searchCorrespondingObjectInReaction(Reaction reac, String id) {
		// look if the given Species-ID is corresponding to a product in the given Reaction r
		// or a reactant or not in the reaction
		for (SpeciesReference specRef: reac.getListOfProducts()) {
			if (specRef.getId().equals(id)){
				// it's a product
				return specRef;
			}
		}
		for (SpeciesReference specRef: reac.getListOfReactants()) {
			if (specRef.getId().equals(id)){
				// it's a reactant
				return specRef;
			}
		}
		// it's not in this reaction
		return null;
	}
}
