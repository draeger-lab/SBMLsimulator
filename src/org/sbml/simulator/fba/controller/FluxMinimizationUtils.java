/*
 * $Id:  FluxVector.java 16:25:58 Meike Aichele$
 * $URL: FluxVector.java $
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

import ilog.concert.IloException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBO;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.simulator.stability.math.ConservationRelations;
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
	 * contains prefix for a degradation reaction
	 */
	protected static final String degradationPrefix = "deg_";

	/**
	 * List contains the eliminated reactions (the transport reactions).
	 */
	public static List<String> eliminatedReactions = new ArrayList<String>();

	/**
	 * contains the suffix for integrated backward reactions of reversible reactions.
	 */
	public static final String endingForBackwardReaction = "_rev";
	
	/**
	 * contains prefix for the metaID
	 */
	protected static final String metaIdPrefix = "meta_";
	
	/**
	 * contains the {@link StoichiometricMatrix} with added system boundaries
	 */
	private static StoichiometricMatrix N_int_sys;
	
	/**
	 * contains the {@link StoichiometricMatrix} incl. all reactions and transports
	 */
	private static StoichiometricMatrix N_all;

	/**
	 * contains the indices of the remaining rows, which are not only zeros
	 */
	public static List<Integer> remainingList = new LinkedList<Integer>();

	/**
	 * List contains the reversible reactions.
	 */
	public static Set<String> reversibleReactions = new HashSet<String>();
	
	/**
	 * contains the corresponding index of the backward reaction in the modified sbml document 
	 * to the i-th reaction in the original sbml document for all reactions that are reversible
	 */
	public static Map<Integer, Integer> reverseReaction = new HashMap<Integer, Integer>(); 

	/**
	 * contains the steadyStateMatrix
	 */
	private static StabilityMatrix steadyStateMatrix;

	/**
	 * contains the system boundaries array
	 */
	private static double[] systemBoundaries;

	/**
	 * contains the {@link SBMLDocument} with added systems boundaries
	 */
	private static SBMLDocument systemBoundaryDocument;
	
	/**
	 * contains all factors for fluxes in transport reactions
	 */
	private static double[][] transportFactors = null;
	
	/**
	 * contains all factors for fluxes in reactions
	 */
	private static double[][] previousFactors = null;

	/**
		 * 
		 * @param indexOfSpecies
		 * @param doc
		 * @param stoichiometry
		 * @return
		 */
		private static SBMLDocument addNewReactionEntriesToDoc(double[] systemBoundaries, SBMLDocument doc) {
			SBMLDocument modified = doc.clone();
	
			for (int index = 0; index < systemBoundaries.length; index++) {
				
				if (!Double.isNaN(systemBoundaries[index])){
					// get species
					Species species = modified.getModel().getSpecies(index);
					String speciesId = species.getId();
	
					String inOut = "";
					// new reaction
					if (Math.signum(systemBoundaries[index]) == -1) {
						inOut = "out";
					}
					else if (Math.signum(systemBoundaries[index]) == 1) {
						inOut = "in";
					}
					String reactionId = degradationPrefix + inOut + speciesId;
					Reaction systemBoundaryReaction = new Reaction(reactionId, doc.getModel().getLevel(), doc.getModel().getVersion());
					systemBoundaryReaction.setMetaId(metaIdPrefix + reactionId);
	
					SpeciesReference specRef =  new SpeciesReference(species);
					specRef.setMetaId(metaIdPrefix + speciesId + "_" + reactionId);
					// TODO choose which is better (give stoichiometry or 1)
	//				specRef.setStoichiometry(Math.abs(systemBoundaries[index]));
					specRef.setStoichiometry(1);
					
					if (Math.signum(systemBoundaries[index]) == -1) {
						systemBoundaryReaction.addReactant(specRef);
					} 
					else if (Math.signum(systemBoundaries[index]) == +1) {
						systemBoundaryReaction.addProduct(specRef);
					}
					systemBoundaryReaction.setReversible(false);
					modified.getModel().addReaction(systemBoundaryReaction);
				}
			}
			return modified;
		}

	/**
	 * 
	 * @param modDoc
	 * @param systemBoundaries
	 * @return
	 * @throws Exception 
	 */
	private static SBMLDocument addSystemBoundaries(SBMLDocument modDoc, double[] systemBoundaries) throws Exception {
		SBMLDocument doc = modDoc.clone();
		doc = addNewReactionEntriesToDoc(systemBoundaries, doc);
		return doc;
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
		double[] systemBoundaries = new double[n_int.getRowDimension()];
		for (int row = 0; row < n_int.getRowDimension(); row++) {
			if (sumOfRowOrCol(n_int.getRow(row)) != 0) {
				systemBoundaries[row] = -(sumOfRowOrCol(n_int.getRow(row)));
			}
			else {
				systemBoundaries[row] = Double.NaN;
			}
		}
		FluxMinimizationUtils.systemBoundaries = systemBoundaries;
		doc = addNewReactionEntriesToDoc(systemBoundaries, doc);
		return doc;
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
		 * Gets a {@link StoichiometricMatrix} and gives back the corresponding flux-vector in Manhattan-Norm, without the given 
		 * target fluxes.
		 * 
		 * @param N_int_sys
		 * @param targetFluxes 
		 * @param doc 
		 * @return double[] flux vector
		 * @throws IOException 
		 * @throws IloException 
		 */
		public static double[] computeFluxVector(StoichiometricMatrix N_int_sys, String[] targetFluxes, SBMLDocument doc) throws IOException, IloException {
			double[][] NwithoutZeroRows = eliminateZeroRows(N_int_sys).getArray();
			// pre-processing: sort the matrix to speed up the computation of the steady state matrix
			double[][] sortedMatrixArray = getSortedArray(NwithoutZeroRows);
			
			StoichiometricMatrix sortedMatrix = new StoichiometricMatrix(sortedMatrixArray, sortedMatrixArray.length, sortedMatrixArray[0].length);
			steadyStateMatrix = ConservationRelations.calculateConsRelations(sortedMatrix.transpose());
	
			// post-processing: delete rows with only forward and backward of a reaction
			steadyStateMatrix = getCorrectedSteadyStateMatrix(steadyStateMatrix);
	
			double[] fluxVector = new double[steadyStateMatrix.getColumnDimension()];
			
			// fill the fluxVector
			for (int column = 0; column < steadyStateMatrix.getColumnDimension(); column++) {
				if (steadyStateMatrix.getRowDimension() > 0) {
					if (((targetFluxes == null) || isNoTargetFlux(column, targetFluxes, doc))) {
						//TODO: save all possible fluxes and pick that one, that gets the best fba-solution, 
	//					fluxVector[column] = steadyStateMatrix.get(0,column);
						
						// or get the linear combination of all steady state fluxes
						fluxVector[column] = sumOfRowOrCol(steadyStateMatrix.getColumn(column));
	
						if (Math.abs(fluxVector[column]) < Math.pow(10, -15)) {
							//then the value is similar to 0 and the flux is 0
							fluxVector[column] = 0d;
						}
	
					} else {
						// TODO use the targetflux ...
					}
				}
			}
			for (int i = 0; i < fluxVector.length; i++) {
				System.out.println(systemBoundaryDocument.getModel().getReaction(i).getId() + ": " + fluxVector[i]);
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
	 * @throws Exception 
	 */
	public static double[] computeFluxVector(String[] targetFluxes, SBMLDocument doc) throws Exception {
		return computeFluxVector(getExpandedStoichiometricMatrix(doc), targetFluxes, doc);
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
	 * Gets a {@link SBMLDocument} and searches the reversible reactions. Than it creates
	 * a new SBMLDocument and splits the reversible reactions in two
	 * irreversible reaction to both sides.
	 * @param document
	 * @return {@link SBMLDocument}
	 */
	public static SBMLDocument splitAllReversibleReactions(SBMLDocument document) {
		SBMLDocument revReacDoc = document.clone();
		//split the reversible reactions
		for (int i = 0; i < document.getModel().getReactionCount(); i++) {
			Reaction reversibleReac = revReacDoc.getModel().getReaction(document.getModel().getReaction(i).getId());
			if (reversibleReac.isSetReversible() && reversibleReac.isReversible()) { 
				reversibleReactions.add(reversibleReac.getId());
				Reaction backwardReac = reversibleReac.clone();
				backwardReac.setKineticLaw(null);
				backwardReac.setSBOTerm(reversibleReac.getSBOTerm());
				backwardReac.setId(reversibleReac.getId() + endingForBackwardReaction);
				backwardReac.setMetaId(reversibleReac.getMetaId() + endingForBackwardReaction);
				backwardReac.setName(reversibleReac.getName() + endingForBackwardReaction);
				backwardReac.getListOfProducts().clear();
				backwardReac.getListOfReactants().clear();
				backwardReac.getListOfModifiers().clear();
				for (int j = 0; j < reversibleReac.getReactantCount(); j++) {
					SpeciesReference sr = reversibleReac.getReactant(j).clone();
					sr.setMetaId(reversibleReac.getReactant(j).getMetaId() + endingForBackwardReaction);
					backwardReac.addProduct(sr);
				}
	
				for (int k = 0; k < reversibleReac.getProductCount(); k++) {
					SpeciesReference sr = reversibleReac.getProduct(k).clone();
					sr.setMetaId(reversibleReac.getProduct(k).getMetaId() + endingForBackwardReaction);
					backwardReac.addReactant(sr);
				}
				for (int l = 0; l < reversibleReac.getModifierCount(); l++) {
					ModifierSpeciesReference sr = reversibleReac.getModifier(l).clone();
					sr.setMetaId(reversibleReac.getModifier(l).getMetaId() + endingForBackwardReaction);
					backwardReac.addModifier(sr);
				} 
				backwardReac.setReversible(false);
				revReacDoc.getModel().addReaction(backwardReac);
				reversibleReac.setReversible(false);
				reverseReaction.put(i, revReacDoc.getModel().getReactionCount() - 1);
//				System.out.println("added: " + backwardReac.getId());
			}
		}
		// return the new document
		return revReacDoc;
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
		return splitAllReversibleReactions(doc);
	}

	/**
	 * Method to eliminate the rows in the {@link StoichiometricMatrix} that contains only zeros.
	 * @param {@link StoichiometricMatrix} N
	 * @return {@link StoichiometricMatrix} without the zero-rows.
	 */
	private static StoichiometricMatrix eliminateZeroRows(StoichiometricMatrix N) {
		// initialize the new StoichiometricMatrix (without the zero-rows)
		StoichiometricMatrix S;
	
		for (int row = 0; row <N.getRowDimension(); row++){
			//flagZeros is true if this row contains only zeros
			boolean flagZeros = true;
			for (int col = 0; col <N.getColumnDimension(); col++) {
				if (N.get(row, col) != 0.0) {
					flagZeros = false;
				}
			}
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
	 * Expands the {@link SBMLDocument} and computes itself the system boundaries.
	 * @param originalDocument
	 * @throws Exception
	 */
	private static void expandDocument(SBMLDocument originalDocument) throws Exception {
		SBMLDocument modifiedDocument = eliminateTransportsAndSplitReversibleReactions(originalDocument);
		StoichiometricMatrix N_int = SBMLDocToStoichMatrix(modifiedDocument);
		systemBoundaryDocument = addSystemBoundaries(N_int, modifiedDocument);
		N_int_sys = SBMLDocToStoichMatrix(systemBoundaryDocument);
	}

	/**
	 * Expands the {@link SBMLDocument} with the given system boundaries.
	 * @param originalDocument
	 * @param systemBoundaries
	 * @throws Exception
	 */
	private static void expandDocument(SBMLDocument originalDocument, double[] systemBoundaries) throws Exception {
		SBMLDocument modifiedDocument = eliminateTransportsAndSplitReversibleReactions(originalDocument);
		systemBoundaryDocument = addSystemBoundaries(modifiedDocument, systemBoundaries);
		N_int_sys = SBMLDocToStoichMatrix(systemBoundaryDocument);
		System.out.println();
	}

	/**
	 * delete the rows of the steadyStateMatrix with conservation relations of a reversible reactions
	 * (rows with a conservation relation of only the forward and backward flux of a splitted reversible reaction)
	 * 
	 * @param ssMatrix
	 * @return the corrected SteadyStateMatrix
	 */
	private static StabilityMatrix getCorrectedSteadyStateMatrix(StabilityMatrix ssMatrix) {
		boolean[] deleting = new boolean[ssMatrix.getRowDimension()];
		int toDeleteCnt = 0;
		for (int i = 0; i < ssMatrix.getRowDimension(); i++) {
			if (isForwardBackwardConservation(ssMatrix.getRow(i))) {
				deleting[i] = true;
				toDeleteCnt++;
			}
		}
		StabilityMatrix helperMatrix = ssMatrix.clone();
		System.out.println(toDeleteCnt);
		ssMatrix = new StabilityMatrix((helperMatrix.getRowDimension() - toDeleteCnt), helperMatrix.getColumnDimension());
		int i = 0;
		for (int j = 0; j < helperMatrix.getRowDimension(); j++) {
			if (!deleting[j]) {
				ssMatrix.setRow(i, helperMatrix.getRow(j));
				i++;
			}
		}
		return ssMatrix;
	}

	/**
	 * @param oriDoc
	 * @param rawSystemBoundaries
	 * @return the corrected system boundaries
	 * @throws Exception
	 */
	public static double[] getCorrectedSystemBoundaries(SBMLDocument oriDoc, double[] rawSystemBoundaries) throws Exception {
		SBMLDocument modifiedDocument = eliminateTransportsAndSplitReversibleReactions(oriDoc);
		StoichiometricMatrix n_int = SBMLDocToStoichMatrix(modifiedDocument);
		double[] sb = getCorrectedSystemBoundaries(n_int, rawSystemBoundaries);
		systemBoundaries = sb;
		return sb;
	}

	/**
	 * Corrects the incoming system boundaries and the corresponding {@link StoichiometricMatrix}.
	 * @param n_int
	 * @param rawSystemBoundaries
	 * @return
	 */
	private static double[] getCorrectedSystemBoundaries(
			StoichiometricMatrix n_int, double[] rawSystemBoundaries) {
		double[] sb = rawSystemBoundaries.clone();
		
		for (int row = 0; row < rawSystemBoundaries.length; row++) {
			if (rawSystemBoundaries[row] == 0) {
				if (sumOfRowOrCol(n_int.getRow(row)) != 0) {
					sb[row] = -(sumOfRowOrCol(n_int.getRow(row)));
				}
				else {
					sb[row] = Double.NaN;
				}
			}
		}
		return sb;
	}

	/**
	 * Converts Gibbs values given in [kJ/mol] to [J/mol].
	 * 
	 * @param gibbs_eq
	 * @return adapted Gibbs energies
	 */
	public static double[] getEquillibriumGibbsEnergiesfromkKiloJoule(double[] gibbs_eq) {
		double[] adaptedGibbsEnergies = new double[gibbs_eq.length];
		for (int i = 0; i < gibbs_eq.length; i++) {
			adaptedGibbsEnergies[i] = gibbs_eq[i] * 1000;
		}
		return adaptedGibbsEnergies;
	}

	/**
	 * returns the expanded {@link SBMLDocument} with the computed system boundaries.
	 * @param originalDocument
	 * @return SBMLDocument with added system boundaries
	 * @throws Exception
	 */
	public static SBMLDocument getExpandedDocument(SBMLDocument originalDocument) throws Exception {
		if (systemBoundaryDocument == null) {
			expandDocument(originalDocument);
		}
		return systemBoundaryDocument;
	}

	/**
	 * returns the expanded {@link SBMLDocument} with the added incoming system boundaries.
	 * @param originalDocument
	 * @param systemBoundaries
	 * @return SBMLDocument with added system boundaries
	 * @throws Exception
	 */
	public static SBMLDocument getExpandedDocument(SBMLDocument originalDocument, double[] systemBoundaries) throws Exception {
		if (systemBoundaryDocument == null) {
			expandDocument(originalDocument, systemBoundaries);
		}
		return systemBoundaryDocument;
	}

	/**
	 * @param originalDocument
	 * @return StoichiometricMatrix with added system boundaries
	 * @throws Exception 
	 */
	public static StoichiometricMatrix getExpandedStoichiometricMatrix(SBMLDocument originalDocument) throws Exception {
		if (N_int_sys == null) {
			getExpandedDocument(originalDocument);
		}
		return N_int_sys;
	}

	/**
	 * @param originalDocument
	 * @param systemBoundaries
	 * @return StoichiometricMatrix with added system boundaries
	 * @throws Exception 
	 */
	public static StoichiometricMatrix getExpandedStoichiometricMatrix(SBMLDocument originalDocument, double[] systemBoundaries) throws Exception {
		if (N_int_sys == null) {
			getExpandedDocument(originalDocument, systemBoundaries);
		}
		return N_int_sys;
	}

	/**
	 * 
	 * @param SBMLDocument
	 * @return the {@link StoichiometricMatrix} of all reactions and transports
	 * @throws Exception
	 */
	public static StoichiometricMatrix getStoichiometricMatrix(SBMLDocument doc) throws Exception {
		if (N_all == null) {
			N_all = SBMLDocToStoichMatrix(doc);
		}
		return N_all;
	}
	
	/**
	 * @param array
	 * @return the number of non-zero entries
	 */
	private static int getParticipatingCount(double[] array) {
		int counter = 0;
		for (int i = 0; i < array.length; i++) {
			if (array[i] != 0) {
				counter++;
			}
		}
		return counter;
	}

	/**
	 * sort the matrix:
	 * rows of metabolites, which occur in many reactions (e.g. ATP) should moved to the end of the matrix and vice versa
	 * this results in a faster computation of the tableau algorithm, which searches for combinations of rows.
	 * applying this preprocessing we reduce the number of possible combinations in the beginning 
	 * and therefore speed up the following computation of the steadyStateMatrix
	 * 
	 * @param arrayToSort
	 * @return the sorted matrix as double[][]
	 */
	private static double[][] getSortedArray(double[][] arrayToSort) {
		double[][] sortedMatrixArray = new double[arrayToSort.length][arrayToSort[0].length];
		int[] counter = new int[arrayToSort.length];
		for (int i = 0; i < arrayToSort.length; i++) {
			counter[i] = getParticipatingCount(arrayToSort[i]);
		}
		int[] sortedCounter = counter.clone();
		Arrays.sort(sortedCounter);

		int[] indices = new int[counter.length]; // contains the new indices after sorting
		for (int i = 0; i < counter.length; i++) {
			for (int j = 0; j < sortedCounter.length; j++) {
				if ((counter[i] == sortedCounter[j])) {
					indices[i] = j;
					sortedCounter[j] = -1; // set the used index to a never again matching value
					j = sortedCounter.length-1; // set j to the last iteration-index, so that next iteration of i starts
				}
			}
		}
		for (int i = 0; i < indices.length; i++) {
			sortedMatrixArray[indices[i]] = arrayToSort[i];
		}
		return sortedMatrixArray;
	}

	/**
	 * 
	 * Returns K_int^T
	 * @return the steady state matrix
	 */
	public static StabilityMatrix getSteadyStateMatrix() {
		return steadyStateMatrix;
	}

	/**
	 * @return the system boundaries
	 */
	public static double[] getSystemBoundaries() {
		return systemBoundaries;
	}
	
	/**
	 * 
	 * @return the factors for fluxes in transport reactions
	 */
	public static double[][] getTransportFactors() {
		return transportFactors;
	}
	
	/**
	 * 
	 * @param doc
	 * @return the factors for fluxes in transport reactions
	 */
	public static double[][] calculateTransportFactors(SBMLDocument doc) {
		Model m = doc.getModel();
		//init the array
		int reacCnt = m.getReactionCount();
		int specCnt = m.getSpeciesCount();
		transportFactors = new double[specCnt][reacCnt];
		
		for (int j = 0; j < reacCnt; j++) {
			for (int i = 0; i < specCnt; i++) {
			double d = 1.0;
			if ((m != null) && (SBO.isChildOf(m.getReaction(j).getSBOTerm(), SBO.getTransport()))) {
				Reaction r = m.getReaction(j);
				Species s = m.getSpecies(i);
				if (r.hasReactant(s) || r.hasProduct(s)) {
					d = 1 / (m.getCompartment(s.getCompartment()).getSize());
//					System.out.println("r " + r + " s " + s + " size: " + (m.getCompartment(s.getCompartment()).getSize()) + " d " + d);
				}
			}
			transportFactors[i][j] = d;
		}
		}
		
		return transportFactors;
	}

	/**
	 * 
	 * @return the factors for fluxes in all reactions
	 */
	public static double[][] getPreviousFactors() {
		return previousFactors;
	}
	
	/**
	 * 
	 * @param doc
	 * @return
	 */
	public static double[][] calculatePreviousFactors(SBMLDocument doc) {
		Model m = doc.getModel();
		//init the array
		int reacCnt = m.getReactionCount();
		int specCnt = m.getSpeciesCount();
		previousFactors = new double[specCnt][reacCnt];
		
		for (int j = 0; j < reacCnt; j++) {
			for (int i = 0; i < specCnt; i++) {
			double d = 1.0;
			if (m != null) {
				Reaction r = m.getReaction(j);
				Species s = m.getSpecies(i);
				if (r.hasReactant(s) || r.hasProduct(s)) {
					d = 1 / (m.getCompartment(s.getCompartment()).getSize());
//					System.out.println("r " + r + " s " + s + " size: " + (m.getCompartment(s.getCompartment()).getSize()) + " d " + d);
				}
			}
			previousFactors[i][j] = d;
		}
		}
		
		return previousFactors;
	}
	
	/**
	 * @param doc
	 * @return the system boundaries created from the stoichiometric matrix
	 * @throws Exception 
	 */
	public static double[] getSystemBoundaries(SBMLDocument doc) throws Exception {
		if (systemBoundaries == null) {
			expandDocument(doc);
		}
		return systemBoundaries;
	}

	/**
	 * checks whether one possible conservation only consists of a splitted reversible reaction
	 * @param row
	 * @return true if the row only contains a forward and the corresponding backward direction of a reversible reaction
	 */
	private static boolean isForwardBackwardConservation(double[] row) {
		int counter = 0;
		int indexCounter = 0;
		int[] index = new int[2];
		for (int i = 0; i < row.length; i++) {
			if (row[i] != 0) {
				if (counter < 2) {
					index[indexCounter] = i;
					indexCounter++;
					counter++;
				}
				else return false;
			}
		}
		if (counter == 2) {
			String r1 = systemBoundaryDocument.getModel().getReaction(index[0]).getId();
			String r2 = systemBoundaryDocument.getModel().getReaction(index[1]).getId();
			if (r2.startsWith(r1) || r1.startsWith(r2)) {
				return true;
			}
			else return false;
		}
		else return false;
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
	 * Gets the {@link SBMLDocument} and gives back the corresponding {@link StoichiometricMatrix}.
	 * @param doc
	 * @return {@link StoichiometricMatrix}
	 * @throws Exception 
	 */
	public static StoichiometricMatrix SBMLDocToStoichMatrix(SBMLDocument doc) throws Exception{

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
	 * Computes the sum of all values in the incoming column- or row-array.
	 * @param indices
	 * @return
	 */
	private static double sumOfRowOrCol(double[] indices) {
		double sum = 0;
		for(int column = 0; column < indices.length; column++) {
			sum += indices[column];
		}
		return sum;
	}
	
}
