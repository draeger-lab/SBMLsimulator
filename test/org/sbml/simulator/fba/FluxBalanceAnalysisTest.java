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
import java.util.Arrays;
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
import org.sbml.simulator.io.CSVDataImporter;
import org.simulator.math.odes.MultiTable;

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
	static double[] systemBoundaries = null;
	static String[] targetFluxes = null;
	static SBMLDocument originalSBMLDoc = null;
	static Constraints constraints = null;
	
	private static long start;
	private static long end;
	
	/**
	 * @param args[0] the SBMLDocument-File
	 * args[1] the Gibbs-File
	 * args[2] the concentration-File
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		start = System.currentTimeMillis();
		originalSBMLDoc = (new SBMLReader()).readSBML(args[0]);
		logger.info("document read: " + args[0]);

		// read system boundaries:
		CSVDataConverter cu0;

		File systemBoundariesFile = null;		
		if (args.length >= 4) {
			systemBoundariesFile = new File(args[3]);
			cu0 = new CSVDataConverter(originalSBMLDoc,true);
			cu0.readSystemBoundariesFromFile(systemBoundariesFile);
			System.out.println("sysbound file vorhanden");
		}
		else {
			cu0 = new CSVDataConverter(originalSBMLDoc);
			System.out.println("sysbound file nicht vorhanden");
		}
		
		while (cu0.getSystemBoundariesArray() == null){
			//wait
			System.out.print(".");
		}
		System.out.println();
		systemBoundaries = cu0.getSystemBoundariesArray();
		System.out.println();
		// get the corrected systems boundaries
		systemBoundaries = FluxMinimizationUtils.getCorrectedSystemBoundaries(originalSBMLDoc, systemBoundaries);
		
		logger.info("system boundaries are read: " + systemBoundariesFile);
		System.out.println();
		for (int i = 0; i < systemBoundaries.length; i++) {
			System.out.println("SB:" + originalSBMLDoc.getModel().getSpecies(i).getId() + " : " + systemBoundaries[i]);
		}
		
		CSVDataConverter cu1 = new CSVDataConverter(originalSBMLDoc, systemBoundaries);
		CSVDataConverter cu2 = new CSVDataConverter(originalSBMLDoc, systemBoundaries);
		File gibbsFile = new File(args[1]);
		File concFile = new File(args[2]);
		
		// read gibbs energies
		cu1.readGibbsFromFile(gibbsFile);
		while(cu1.getGibbsArray() == null) {
			//wait
			System.out.print(".");
//			logger.info("cu1.getGibbsArray() == null");
		}
		equilibriumsGibbsEnergies = cu1.getGibbsArray();
		logger.info("gibbs are read: " + gibbsFile.getName());
		
		
		// read concentrations
		CSVDataImporter importer = new CSVDataImporter();
		MultiTable concentrationMT = importer.convert(concFile.getAbsolutePath());
		int speciesCount = concentrationMT.getColumnCount()-1;
		equilibriumsConcentrations = new double[speciesCount];
		for(int i=0; i<speciesCount; i++) {
			equilibriumsConcentrations[i] = concentrationMT.getValueAt(0, i+1);
		}
		
		/*cu2.readConcentrationsFromFile(concFile);
		while(cu2.getConcentrationsArray() == null) {
			//wait
			System.out.print(".");
//			logger.info("cu2.getConcentrationsArray() == null");
		}
		equilibriumsConcentrations = cu2.getConcentrationsArray();*/
		logger.info("concentrations are read: " + concFile.getName());
		
		System.out.println("gibbs...");
		System.out.println(Arrays.toString(equilibriumsGibbsEnergies));
		
		System.out.println("konz...");
		System.out.println(Arrays.toString(equilibriumsConcentrations));


		//create FluxBalanceAnalysis object and solve it:
		constraints =  new Constraints(originalSBMLDoc, equilibriumsGibbsEnergies, equilibriumsConcentrations, systemBoundaries, true);
		FluxBalanceAnalysis fba = new FluxBalanceAnalysis(originalSBMLDoc, constraints, targetFluxes);
//		fba.setLambda1(0);
//		fba.setLambda2(0);
//		fba.setLambda3(0);
//		fba.setLambda4(0);
		fba.setConstraintJG(true);
		fba.setConstraintJ_rmaxG(true);
		fba.setConstraintJ0(true);
		fba.setConstraintError(true);
		fba.setCplexIterations(40000);
		
		
		

		SBMLDocument modifiedDocument = FluxMinimizationUtils.getExpandedDocument(originalSBMLDoc, systemBoundaries);
		Model modModel = modifiedDocument.getModel();

//		System.out.println();
//		System.out.println("--------steady state matrix--------");
		for (int i = 0; i < modModel.getReactionCount(); i++){
			System.out.print(modModel.getReaction(i) + " ");
		}
		System.out.println();
//		System.out.println(FluxMinimizationUtils.getSteadyStateMatrix().toString());
		
		fba.solve();
		
		//------------------------print stoichiometric matrix---------------
//		System.out.println();
//		double[][] sMatrix = FluxMinimizationUtils.getExpandedStoichiometricMatrix(originalSBMLDoc).getArray();
//		for (int i = 0; i < sMatrix.length; i++) {
//			System.out.println(modModel.getSpecies(i) + "\t" + Arrays.toString(sMatrix[i]));
//		}
		
		
		
		
		System.out.println();
		//print flux solution:
		double[] fluxSolution = fba.solutionFluxVector;
		System.out.println("--------solution for the fluxes:--------");
		for (int i = 0; i < fluxSolution.length; i++) {
			System.out.println(modModel.getReaction(i).getId() + " & " + fluxSolution[i]);
		}
		
		// print sum of all in- and outgoing fluxes are 0?
		Map<Species, Double> fluxSum = new HashMap<Species, Double>();
		for (int i = 0; i< modModel.getSpeciesCount(); i++ ) {
			fluxSum.put(modModel.getSpecies(i), 0.0);
		}
		for (int j = 0; j < modModel.getReactionCount(); j++) {
			Reaction r = modModel.getReaction(j);
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
		
		double[] gibbsSolution = fba.solutionGibbs;
		
		System.out.println();
		System.out.println("--------------gibbs-------------");
		for (int i = 0; i < gibbsSolution.length; i++) {
			System.out.println(modModel.getReaction(i).getId() + "    " + gibbsSolution[i]);
		}
		
		
//		System.out.println();
//		System.out.println("--------sum of the in- and outgoing fluxes (incl. stoichiometry)--------");
//		for (int i = 0; i< modModel.getSpeciesCount(); i++ ) {
//			System.out.println(modModel.getSpecies(i) + " : " + fluxSum.get(modModel.getSpecies(i)));
//		}
		
		System.out.println();
		//print conc solution:
		double[] concSolution = fba.solutionConcentrations;
		System.out.println("--------solution for the concentrations:--------");
		for (int i = 0; i < concSolution.length; i++) {
			System.out.println(originalSBMLDoc.getModel().getSpecies(i).getId() + "   " + concSolution[i]);
		}
//		
//		System.out.println();
//		System.out.println("--------species at the system boundaries----------");
//		double[] sb = constraints.getSystemBoundaries();
//		for (int i = 0; i < sb.length; i++){
//			if (!Double.isNaN(sb[i])) {
//				System.out.println(originalSBMLDoc.getModel().getSpecies(i));
//			}
//		}
		
//		System.out.println("time:----------------");
//		end = System.currentTimeMillis();
//		System.out.println((end - start) + " ms");
//		
		System.out.println("Error:-----------------");
		double[] errorSolutions = fba.solutionErrors;
		for (int i = 0; i < errorSolutions.length; i++) {
			System.out.println(modModel.getReaction(i).getId() + "    " + errorSolutions[i]);
		}
		
//		cu0.writeComputedValuesInCSV(fluxSolution, new File("C:/Users/Meike/Desktop/fluxSolutionFBATest2.csv"));
//		cu1.writeComputedValuesInCSV(fba.solutionConcentrations, new File("C:/Users/Meike/Desktop/concSolutionFBATest2.csv"));
//		cu2.writeComputedValuesInCSV(fba.solutionErrors, new File("C:/Users/Meike/Desktop/errorSolutionFBATest2.csv"));
	}

}
