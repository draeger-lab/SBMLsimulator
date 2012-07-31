/*
 * $Id:  FluxBalanceAnalysisTest.java 19:04:44 Meike Aichele$
 * $URL: FluxBalanceAnalysisTest.java $
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
package org.sbml.simulator.fba;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.simulator.fba.controller.CSVDataConverter;
import org.sbml.simulator.fba.controller.Constraints;
import org.sbml.simulator.fba.controller.FluxBalanceAnalysis;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 17.06.2012
 * @since 1.0
 */
public class FluxBalanceAnalysisTest {

	private static final transient Logger logger = Logger.getLogger(FluxBalanceAnalysisTest.class.getName());
	
	static double[] equilibriumsConcentrations = null;
	static double[] equilibriumsGibbsEnergies = null;
	static String[] targetFluxes = null;
	static SBMLDocument originalSBMLDoc = null;
	static Constraints constraints = null;
	
	/**
	 * @param args[0] the SBMLDocument-File
	 * args[1] the Gibbs-File
	 * args[2] the concentration-File
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		originalSBMLDoc = (new SBMLReader()).readSBML(args[0]);
		
		logger.info("document read");
		//ConstraintsUtils:
		CSVDataConverter cu1 = new CSVDataConverter(originalSBMLDoc);
		CSVDataConverter cu2 = new CSVDataConverter(originalSBMLDoc);
		
		
		
		File gibbsFile = new File(args[1]);
		File concFile = new File(args[2]);
		
		logger.info("will read gibbs");
		// read gibbs and concentrations
		cu1.readGibbsFromFile(gibbsFile);
		while(cu1.getGibbsArray() == null) {
			//wait
			System.out.print(".");
//			logger.info("cu1.getGibbsArray() == null");
		}
		equilibriumsGibbsEnergies = cu1.getGibbsArray();

		
		
		cu2.readConcentrationsFromFile(concFile);
		while(cu2.getConcentrationsArray() == null) {
			//wait
			System.out.print(".");
//			logger.info("cu2.getConcentrationsArray() == null");
		}
		equilibriumsConcentrations = cu2.getConcentrationsArray();


		//create FluxBalanceAnalysis object and solve it:
		constraints =  new Constraints(originalSBMLDoc, equilibriumsGibbsEnergies, equilibriumsConcentrations, true);
		FluxBalanceAnalysis fba = new FluxBalanceAnalysis(originalSBMLDoc, constraints, targetFluxes);
//		fba.setLambda1(0);
//		fba.setLambda2(0);
//		fba.setLambda3(0);
//		fba.setLambda4(0);
		fba.setConstraintJG(true);
		fba.setConstraintJ_rmaxG(true);
		fba.setCplexIterations(4000);
		fba.solve();
		
		//print flux solution:
		double[] fluxSolution = fba.solutionFluxVector;
		System.out.println("solutions for the fluxes: ");
		SBMLDocument doc = FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(originalSBMLDoc);
		Model model = doc.getModel();
		for (int i = 0; i < fluxSolution.length; i++) {
			System.out.println(model.getReaction(i).getId() + "   " + fluxSolution[i]);
		}
		
		Map<Species, Double> fluxSum = new HashMap<Species, Double>();
		// TODO is sum of all in- and outgoing fluxes are 0?
		for (int i = 0; i< model.getSpeciesCount(); i++ ) {
			fluxSum.put(model.getSpecies(i), 0.0);
		}
		for (int j = 0; j < model.getReactionCount(); j++) {
			Reaction r = model.getReaction(j);
			ListOf<SpeciesReference> substrateList = r.getListOfReactants();
			for (SpeciesReference sr : substrateList) {
				double helper = fluxSum.get(sr.getSpeciesInstance());
				helper += (-1 * sr.getStoichiometry() * fluxSolution[j]);
//				System.out.println(
//						sr.getSpeciesInstance() + ": -" 
//						+ sr.getStoichiometry() + " * " 
//						+ fluxSolution[j] + " = " 
//						+ (-1 * sr.getStoichiometry() * fluxSolution[j]));
				fluxSum.put(sr.getSpeciesInstance(), helper);
			}
			ListOf<SpeciesReference> productList = r.getListOfProducts();
			for (SpeciesReference sr : productList) {
				double helper = fluxSum.get(sr.getSpeciesInstance());
				helper += (sr.getStoichiometry() * fluxSolution[j]);
//				System.out.println(
//						sr.getSpeciesInstance() + ": +" 
//						+ sr.getStoichiometry() + " * " 
//						+ fluxSolution[j] + " = " 
//						+ (sr.getStoichiometry() * fluxSolution[j]));
				fluxSum.put(sr.getSpeciesInstance(), helper);
			}
		}
		System.out.println("\n#####################################");
		System.out.println("sum of the in- and outgoing fluxes (incl. stoichiometry):");
		for (int i = 0; i< model.getSpeciesCount(); i++ ) {
			System.out.println(model.getSpecies(i) + " : " + fluxSum.get(model.getSpecies(i)));
		}
		
		
//		System.out.println("-----------------");
//		//print conc solution:
//		double[] concSolution = fba.solution_concentrations;
//		System.out.println("solutions for the concs: ");
//		for (int i = 0; i < concSolution.length; i++) {
//			System.out.println(sbml.getModel().getSpecies(i).getId() + "   " + concSolution[i]);
//		}
		
	}

}
