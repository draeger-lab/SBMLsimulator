/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.math;

import java.util.HashMap;
import java.util.Map;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.simulator.sbml.SBMLinterpreter;

/**
 * This Class represents a tool to get the information needed to do a stability
 * analysis out of a SBML file
 * 
 * @author Alexander D&ouml;rr
 * @date 2009-12-18
 * @version $Rev$
 * @since 1.0
 */
public class SBMLMatrixParser {

	/**
	 * 
	 */
	private Model model;
	
	/**
	 * 
	 */
	private Map<String, Integer> hashReactions, hashSpecies;
	
	/**
	 * 
	 */
	private int numSpecies, numReactions;
	
	/**
	 * 
	 */
	private Map<String, Map<Integer, Integer>> sBOTerms;
	
	/**
	 * 
	 */
	private static int initvalue = 1;
	
	/**
	 * 
	 */
	private String[] reactions, species;

	/**
	 * Creates a new SBMLParser for the file in the given path
	 * 
	 * @param path
	 */
	public SBMLMatrixParser(Model model) {
		this.model = model;
		this.hashReactions = new HashMap<String, Integer>();
		this.hashSpecies = new HashMap<String, Integer>();
		this.numSpecies = (int) model.getListOfSpecies().size();
		this.numReactions = (int) model.getListOfReactions().size();
		this.reactions = new String[numReactions];
		this.species = new String[numSpecies];
		hashSpezies();
		hashReactions();
		sBOTerms = new HashMap<String, Map<Integer, Integer>>();
	}

	/**
	 * Creates a HashMap of the available species and an array with the ids in
	 * the same order as they are hashed to the species indeces used to build
	 * the stoichiometric matrix
	 */
	private void hashSpezies() {
		for (int i = 0; i < numSpecies; i++) {
			hashSpecies.put(model.getSpecies(i).getId(), Integer.valueOf(i));
			species[i] = model.getSpecies(i).getId();
		}
	}

	/**
	 * Creates a HashMap of the available reactions and an array with the ids in
	 * the same order as they are hashed to the reaction indeces used to build
	 * the stoichiometric matrix
	 */
	private void hashReactions() {
		for (int i = 0; i < numReactions; i++) {
			hashReactions.put(model.getReaction(i).getId(), i);
			reactions[i] = model.getReaction(i).getId();
		}
	}

	/**
	 * Returns an array with the ids of all participating reactions in the same
	 * order as they are used in the stoichiometric matrix
	 * 
	 * @return
	 */
	public String[] getReactionNames() {
		return this.reactions;
	}

	/**
	 * Returns an array with the ids of all participating species in the same
	 * order as they are used in the stoichiometric matrix
	 * 
	 * @return
	 */
	public String[] getSpeciesNames() {
		return this.species;
	}

	/**
	 * Builds the stoichiometric matrix for the current model
	 * 
	 * @return
	 */
	public StoichiometricMatrix getStoichiometric() {
		StoichiometricMatrix matrixN = new StoichiometricMatrix(numSpecies,
				numReactions, 0);

		ListOf<SpeciesReference> losr;
		Reaction reac;
		SpeciesReference speciesRef;
		for (int n = 0; n < numReactions; n++) {
			reac = model.getReaction(n);
			// reactants
			losr = reac.getListOfReactants();

			for (int m = 0; m < losr.size(); m++) {
				speciesRef = reac.getReactant(m);

				matrixN.set(hashSpecies.get(speciesRef.getSpecies()),
						hashReactions.get(reac.getId()), -speciesRef
								.getStoichiometry());
			}

			// products
			losr = model.getReaction(n).getListOfProducts();

			for (int m = 0; m < losr.size(); m++) {
				speciesRef = reac.getProduct(m);

				matrixN.set(hashSpecies.get(speciesRef.getSpecies()),
						hashReactions.get(reac.getId()), speciesRef
								.getStoichiometry());
			}
		}
		return matrixN;
	}

	/**
	 * Builds the modulation matrix for the current model
	 * 
	 * @return
	 * @throws SBMLException 
	 */
	public StabilityMatrix getModulation() throws SBMLException {

		StabilityMatrix matrixW = new StabilityMatrix(numSpecies, numReactions,
				0);

		ListOf<ModifierSpeciesReference> losr;
		Reaction reac;
		ModifierSpeciesReference modSpeciesRef;

		int classification = 0;
		for (int n = 0; n < numReactions; n++) {
			reac = model.getReaction(n);
			// modifier
			losr = reac.getListOfModifiers();
			if (losr.size() > 0) {
				sBOTerms.put(reac.getId(), new HashMap<Integer, Integer>());
			}
			for (int m = 0; m < losr.size(); m++) {
				modSpeciesRef = reac.getModifier(m);

				if (modSpeciesRef.getSBOTerm() == -1) {
					classification = classifyModifier(reac, modSpeciesRef);
				}

				// potentiator
				if (classification > 0 || modSpeciesRef.getSBOTerm() == 21) {
					matrixW.set(hashSpecies.get(modSpeciesRef.getSpecies())
							.intValue(), hashReactions.get(reac.getId())
							.intValue(), 1);

					if (modSpeciesRef.getSBOTerm() == -1) {
						sBOTerms.get(reac.getId()).put(Integer.valueOf(m), 21);
					}
				}
				// inhibitor
				else if ((classification < 0) || (modSpeciesRef.getSBOTerm() == 20)) {
					matrixW.set(hashSpecies.get(modSpeciesRef.getSpecies()),
							hashReactions.get(reac.getId()), -1);
					if (modSpeciesRef.getSBOTerm() == -1) {
						sBOTerms.get(reac.getId()).put(Integer.valueOf(m), 20);
					}
				}
				// catalyst
				else {
					matrixW.set(hashSpecies.get(modSpeciesRef.getSpecies()),
							hashReactions.get(reac.getId()), 0);
					if (modSpeciesRef.getSBOTerm() == -1) {
						sBOTerms.get(reac.getId()).put(Integer.valueOf(m), 13);
					// modSpeciesRef.setSBOTerm(13);
					}
				}
			}
		}
		return matrixW;
	}

	/**
	 * Classifies the given modifier in the given reaction by reference to its
	 * impact on the result of the reaction. Returning a 0 for catalyst, -1 for
	 * inhibitor and 1 for potentiator
	 * 
	 * @param reac
	 * @param msr
	 * @return
	 * @throws SBMLException
	 */
	private int classifyModifier(Reaction reac, ModifierSpeciesReference msr)
			throws SBMLException {
		SBMLinterpreter sbmli;
		int result = 0;
		double normal, half, twice;
		double iA;

		initLocalParameter(reac.getKineticLaw().getListOfLocalParameters());
		initGlobalParameter(reac.getKineticLaw().getMath());
		try {
			sbmli = new SBMLinterpreter(model);
			iA = model.getSpecies(msr.getSpecies()).getInitialAmount();
			// evaluate reaction with normal amount
			normal = reac.getKineticLaw().getMath().compile(sbmli.getASTNodeInterpreter()).toDouble();

			// evaluate reaction with half of the normal amount
			model.getSpecies(msr.getSpecies()).setInitialAmount(iA / 2);
			sbmli = new SBMLinterpreter(model);
			half = reac.getKineticLaw().getMath().compile(sbmli.getASTNodeInterpreter()).toDouble();

			// evaluate reaction with twice the normal amount
			model.getSpecies(msr.getSpecies()).setInitialAmount(iA * 2);
			sbmli = new SBMLinterpreter(model);
			twice = reac.getKineticLaw().getMath().compile(sbmli.getASTNodeInterpreter()).toDouble();

			if ((half < normal) && (normal < twice)) {
				result = 1;
			}	else if ((half > normal) && (normal > twice)) {
				result = -1;
			}

			model.getSpecies(msr.getSpecies()).setInitialAmount(iA);

		} catch (ModelOverdeterminedException e) {

		}
		return result;
	}

	/**
	 * Adds SBOTerms to all modifiers hashed in the HashMap sBOTerms because
	 * their SBOTerm hasn't been set yet
	 */
	// TODO still necessary?
	private void setSBOTerms() {
		Map<Integer, Integer> sBOReaction;
		for (String rid : sBOTerms.keySet()) {
			sBOReaction = sBOTerms.get(rid);
			for (Integer mid : sBOReaction.keySet()) {
				model.getReaction(rid).getModifier(mid.intValue()).setSBOTerm(
						sBOReaction.get(mid));
			}
		}
	}

	/**
	 * Initializes all parameters in the list of local parameter that haven't
	 * been initialized yet
	 */
	private void initLocalParameter(ListOf<LocalParameter> lop) {
		for (int i = 0; i < lop.size(); i++) {
			if (!lop.get(i).isSetValue()) {
				lop.get(i).setValue(initvalue);
			}
		}
	}

	/**
	 * Initializes all parameters in a kinetic law that haven't been initialized
	 * yet
	 */
	private void initGlobalParameter(ASTNode astnode) {
		ListOf<Species> los = model.getListOfSpecies();
		ListOf<Parameter> lop;
		ListOf<Compartment> loc;
		boolean found = false;
		String nodename = new String();
		int i;

		if (astnode.isString()) {
			nodename = astnode.getName();
		}

		for (i = 0; i < los.size() && !found; i++) {
			if (los.get(i).getName() == nodename) {
				found = true;
				Species s = los.get(i);
				if (!s.isSetInitialAmount() && !s.isSetInitialConcentration()) {
					los.get(i).setInitialAmount(Double.valueOf(initvalue));
				}
			}
		}
		lop = model.getListOfParameters();
		for (i = 0; i < lop.size() && !found; i++) {
			if (lop.get(i).getName() == nodename) {
				found = true;
				if (!lop.get(i).isSetValue()) {
					lop.get(i).setValue(initvalue);
				}
			}
		}

		loc = model.getListOfCompartments();
		for (i = 0; i < los.size() && !found; i++) {
			if (loc.get(i).getName() == nodename) {
				found = true;
				loc.get(i).setSize(initvalue);
			}

		}

		if (astnode.getRightChild() != null) {
			initGlobalParameter(astnode.getRightChild());
		}
		if (astnode.getLeftChild() != null) {
			initGlobalParameter(astnode.getLeftChild());
		}
	}

}
