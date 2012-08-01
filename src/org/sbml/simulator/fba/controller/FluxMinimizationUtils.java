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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBO;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
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
	 * contains the suffix for integrated backward reactions of reversible reactions.
	 */
	protected static final String endingForBackwardReaction = "_rev";
	
	/**
	 * List contains the eliminated reactions (the transport reactions).
	 */
	public static List<String> eliminatedReactions = new ArrayList<String>();
	
	/**
	 * List contains the reversible reactions.
	 */
	public static List<String> reversibleReactions = new ArrayList<String>();

	/**
	 * 
	 */
	private static SBMLDocument systemBoundaryDocument;

	/**
	 * 
	 */
	private static StoichiometricMatrix N_int_sys;
	
	
	
	/**
	 * Gets a {@link StoichiometricMatrix} and gives back the corresponding flux-vector in Manhattan-Norm, without the given 
	 * target fluxes.
	 * 
	 * @param N_int
	 * @param targetFluxes 
	 * @param doc 
	 * @return double[] flux vector
	 */
	public static double[] computeFluxVector(StoichiometricMatrix N_int, String[] targetFluxes, SBMLDocument doc) {
		StoichiometricMatrix eliminateZeroRows = eliminateZeroRows(N_int);
		StoichiometricMatrix NwithoutZeroRows = new StoichiometricMatrix(eliminateZeroRows.getArray(), eliminateZeroRows.getRowDimension(), eliminateZeroRows.getColumnDimension());
		NwithoutZeroRows.getReducedMatrix();
//		StabilityMatrix steadyStateMatrix = ConservationRelations.calculateConsRelations(new StoichiometricMatrix(NwithoutZeroRows.transpose().getArray(),NwithoutZeroRows.getColumnDimension(),NwithoutZeroRows.getRowDimension()));
		StabilityMatrix steadyStateMatrix = (new StoichiometricMatrix(NwithoutZeroRows.transpose().getArray(),NwithoutZeroRows.getColumnDimension(),NwithoutZeroRows.getRowDimension())).getConservationRelations();
		double[] fluxVector = new double[steadyStateMatrix.getColumnDimension()];

		//TODO
		System.out.println();
		System.out.println("N.getColumnDimension() " + N_int.getColumnDimension() + "   N.getRowDimension() " + N_int.getRowDimension() + ", steadyStateMatrix.getColumnDimension() " + steadyStateMatrix.getColumnDimension() + "   steadyStateMatrix.getRowDimension() " +  steadyStateMatrix.getRowDimension());
		
		System.out.println("_------_");
		System.out.println(steadyStateMatrix.toString());
		
		// fill the fluxVector
		for (int column = 0; column < steadyStateMatrix.getColumnDimension(); column++) {
			if (steadyStateMatrix.getRowDimension() > 0) {
				if (((targetFluxes == null) || isNoTargetFlux(column, targetFluxes, doc))) {
					//TODO: save all possible fluxes and pick that one, that gets the best fba-solution. 
					fluxVector[column] = steadyStateMatrix.get(0,column);
					if (Math.abs(fluxVector[column]) < Math.pow(10, -15)) {
						//then the value is similar to 0 and the flux is 0
						fluxVector[column] = 0d;
					}
				} else {
					// TODO use the targetflux ...
				}
			}
		}
		return fluxVector;
	}

	/**
	 * Method to eliminate the rows in the {@link StoichiometricMatrix} that contains only zeros.
	 * @param {@link StoichiometricMatrix} N
	 * @return {@link StoichiometricMatrix} without the zero-rows.
	 */
	private static StoichiometricMatrix eliminateZeroRows(StoichiometricMatrix N) {
		// initialize the new StoichiometricMatrix (without the zero-rows)
		StoichiometricMatrix S;
		
		// list which contains the indices of the remaining rows
		List<Integer> remainingList = new LinkedList<Integer>();
		
		for (int row = 0; row <N.getRowDimension(); row++){
			//flagZeros is true if this row contains only zeros
			boolean flagZeros = true;
			int i = 0;
			for (int col = 0; col <N.getColumnDimension(); col++) {
				if (N.get(row, col) != 0.0) {
					flagZeros = false;
					i++;
				}
			}
//			if (!flagZeros && i > 1) {
			if (!flagZeros) {
				remainingList.add(row);
			}
			
		}
		
		S = new StoichiometricMatrix(remainingList.size(), N.getColumnDimension());
		for (int j = 0; j < S.getRowDimension(); j++) {
			S.setRow(j, N.getRow(remainingList.get(j)));
		}
		return S;
	}

	/**
	 * Gets a {@link StoichiometricMatrix} and gives back the corresponding flux-vector in Manhattan-Norm, without the given 
	 * target fluxes.
	 * 
	 * @param targetFluxes
	 * @param doc
	 * @return
	 * @throws Exception 
	 */
	public static double[] computeFluxVector(String[] targetFluxes, SBMLDocument doc) throws Exception {
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
	 * Gets the original {@link SBMLDocument} and gives back the corresponding {@link StoichiometricMatrix}.
	 * @param doc
	 * @return {@link StoichiometricMatrix}
	 * @throws Exception 
	 */
	public static StoichiometricMatrix SBMLDocToStoichMatrix(SBMLDocument doc) throws Exception{
		doc = eliminateTransportsAndSplitReversibleReactions(doc);

		// build a new StoichiometricMatrix with the number of species as the dimension of rows
		// and the number of reactions as the dimension of columns
		int speciesCount = doc.getModel().getSpeciesCount();
		int reactionCount = doc.getModel().getReactionCount();
		StoichiometricMatrix sMatrix = new StoichiometricMatrix (speciesCount,reactionCount);

		//fill the matrix with the stoichiometry of each reaction
		for (int j = 0; j < reactionCount; j++) { //j is the column (reaction)
			for (int i = 0; i < speciesCount; i++) { // i is the row (species)
				Reaction reac = doc.getModel().getReaction(j);
				Species species = doc.getModel().getSpecies(i);
				if (reac.hasProduct(species)) {
					// the stoichiometry of products is positive in the stoichiometricMatrix
					if (reac.getProductForSpecies(species.getId()).getStoichiometry() != Double.NaN) {
						sMatrix.set(i, j, reac.getProductForSpecies(species.getId()).getStoichiometry());
					}
					else {
						throw new Exception("Stoichiometry of species: " + reac.getProductForSpecies(species.getId()) + " is unknown. Can't create the full stoichiometric matrix of the model.");
					}
				} else if (reac.hasReactant(species)) {
					// the stoichiometry of reactants is negative in the stoichiometricMatrix
					if (reac.getReactantForSpecies(species.getId()).getStoichiometry() != Double.NaN) {
						sMatrix.set(i, j, - reac.getReactantForSpecies(species.getId()).getStoichiometry());
					}
					else {
						throw new Exception("Stoichiometry of species: " + reac.getReactantForSpecies(species.getId()) + " is unknown. Can't create the full stoichiometric matrix of the model.");
					}
				}
			}
		}
		
		for (int i = 0; i< sMatrix.getRowDimension(); i++) {
			double[] d = sMatrix.getRow(i);
			if (d.equals(sMatrix.getRowShallow(i)))
			System.out.println(d);
		}
		return sMatrix;
	}


	/**
	 * Gets a {@link SBMLDocument} and searches the reversible reactions. Than it creates
	 * a new SBMLDocument without the transport reactions and the reversible reactions split in two
	 * irreversible reaction to both sides.
	 * @param document
	 * @return {@link SBMLDocument}
	 */
	public static SBMLDocument eliminateTransportsAndSplitReversibleReactions(
			SBMLDocument document) {
		// first eliminate the transports
		SBMLDocument doc = eliminateTransports(document);
		SBMLDocument revReacDoc = doc.clone();
		//split the reversible reactions
		int metaid = 0;
		for (int i = 0; i < doc.getModel().getReactionCount(); i++) {
			Reaction reversibleReac = revReacDoc.getModel().getReaction(doc.getModel().getReaction(i).getId());
			if (reversibleReac.isSetReversible() && reversibleReac.isReversible()) {
				reversibleReactions.add(reversibleReac.getId());
				reversibleReactions.add(reversibleReac.getId() + endingForBackwardReaction);
				Reaction backwardReac = reversibleReac.clone();
				backwardReac.setId(reversibleReac.getId() + endingForBackwardReaction);
				backwardReac.setMetaId(reversibleReac.getMetaId() + endingForBackwardReaction);
				backwardReac.setName(reversibleReac.getName() + endingForBackwardReaction);
				backwardReac.getListOfProducts().clear();
				backwardReac.getListOfReactants().clear();
				for (int j = 0; j < reversibleReac.getReactantCount(); j++) {
					SpeciesReference sr = reversibleReac.getReactant(j).clone();
					sr.setMetaId(metaid + endingForBackwardReaction);
					metaid++;
					backwardReac.addProduct(sr);
				}

				for (int k = 0; k < reversibleReac.getProductCount(); k++) {
					SpeciesReference sr = reversibleReac.getProduct(k).clone();
					sr.setMetaId(metaid + endingForBackwardReaction);
					metaid++;
					backwardReac.addReactant(sr);
				}
				backwardReac.setReversible(false);
				revReacDoc.getModel().addReaction(backwardReac);
				reversibleReac.setReversible(false);
			}
		}
		// return the new document
		return revReacDoc;
	}


	/**
	 * Eliminates the transport-reactions and gives back the new {@link SBMLDocument}.
	 * @param doc
	 * @return a new SBMLDocument without transport reactions
	 */
	public static SBMLDocument eliminateTransports(SBMLDocument doc) {
		SBMLDocument newDoc = doc.clone();
		for (int i = 0; i < doc.getModel().getReactionCount(); i++) {
			String id = doc.getModel().getReaction(i).getId();
			if (SBO.isChildOf(doc.getModel().getReaction(i).getSBOTerm(), SBO.getTransport())) {
				// reaction i is a transport reaction: remove it
				newDoc.getModel().removeReaction(id);
				eliminatedReactions.add(id);
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
			error[i] = 1.0;
		}
		return error;
	}
	
	
	/**
	 * 
	 * @param originalDoc
	 * @return SBMLDocument with added system boundaries
	 * @throws Exception
	 */
	public static SBMLDocument expandDocument(SBMLDocument originalDoc) throws Exception {
		SBMLDocument modifiedDocument = eliminateTransportsAndSplitReversibleReactions(originalDoc);
		StoichiometricMatrix N_int = SBMLDocToStoichMatrix(modifiedDocument);
		systemBoundaryDocument = addSystemBoundaries(N_int, modifiedDocument);
		N_int_sys = SBMLDocToStoichMatrix(systemBoundaryDocument);
		return systemBoundaryDocument;
	}
	
	/**
	 * @return StoichiometricMatrix with added system boundaries
	 */
	public static StoichiometricMatrix expandStoichiometricMatrix() {
		return N_int_sys;
	}

	/**
	 * 
	 * @param n_int
	 * @param modDoc
	 * @return
	 */
	private static SBMLDocument addSystemBoundaries(StoichiometricMatrix n_int,
			SBMLDocument modDoc) {
		SBMLDocument doc = modDoc.clone();
		for (int row = 0; row < n_int.getRowDimension(); row++) {
			if (sumOfRow(n_int.getRow(row)) > 0.0) {
				//species and reactions adden, stoich = -1
				int indexOfSpecies = row;
				double stoichiometry = -1;
				doc = addNewSpeciesAndReactionEntriesToDoc(indexOfSpecies, doc, stoichiometry);
			} else if (sumOfRow(n_int.getRow(row)) < 0.0) {
				//species and reactions adden, stoich = +1
				int indexOfSpecies = row;
				double stoichiometry = 1;
				doc = addNewSpeciesAndReactionEntriesToDoc(indexOfSpecies, doc, stoichiometry);
			}
		}
		return doc;
	}

	/**
	 * 
	 * @param row
	 * @return
	 */
	private static double sumOfRow(double[] row) {
		double sum = 0;
		for(int column = 0; column < row.length; column++) {
			sum += row[column];
		}
		return sum;
	}
	
	
	/**
	 * 
	 * @param indexOfSpecies
	 * @param doc
	 * @param stoichiometry
	 * @return
	 */
	private static SBMLDocument addNewSpeciesAndReactionEntriesToDoc(int indexOfSpecies, SBMLDocument doc, double stoichiometry) {
		SBMLDocument mod = doc.clone();
		// get species
		Species systemBoundarySpec = mod.getModel().getSpecies(indexOfSpecies);
		String sbs_id = systemBoundarySpec.getId();
		
		// new reaction
		String sbr_id = "deg_r_" + indexOfSpecies;
		Reaction systemBoundaryReaction = new Reaction(sbr_id);
		systemBoundaryReaction.setMetaId(sbr_id);
		
		SpeciesReference specRef =  new SpeciesReference(systemBoundarySpec);
		specRef.setMetaId("meta_" + sbs_id);
		specRef.setStoichiometry(stoichiometry);
		if (Math.signum(stoichiometry) == -1) {
			systemBoundaryReaction.addReactant(specRef);
		} else {
			systemBoundaryReaction.addProduct(specRef);
		}
		systemBoundaryReaction.setReversible(false);
		mod.getModel().addReaction(systemBoundaryReaction);
		
		return mod;
	}
	
}
