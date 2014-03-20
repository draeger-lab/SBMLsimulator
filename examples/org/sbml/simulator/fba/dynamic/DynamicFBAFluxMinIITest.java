/*
 * $$Id${file_name} ${time} ${user}$$
 * $$URL${file_name} $$
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
package org.sbml.simulator.fba.dynamic;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;
import org.sbml.simulator.io.CSVDataImporter;
import org.simulator.math.odes.MultiTable;
import org.simulator.math.odes.MultiTable.Block.Column;

import de.zbit.io.csv.CSVWriter;

/**
 * @author Stephanie Tscherneck, Robin F&auml;hnrich, Roland Keller
 * @version $Rev$
 * @since 1.0
 * ${tags}
 */

public class DynamicFBAFluxMinIITest {

	private static Model model;
	
	/**
	 * Test the methods of class {@link DynamicFBA}.
	 * args[0] = sbml document
	 * args[1] = concentration file as multitable
	 * args[2] = known fluxes as tab separated with headline or "noFile"
	 * args[3] = output multitable file
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		// Read SBML document file
		SBMLReader reader = new SBMLReader();
		SBMLDocument oriDocument = reader.readSBML(args[0]);
//		oriDocument.getModel().removeReaction("degPyr");
		model = oriDocument.getModel();
		double[][] transportfactors = FluxMinimizationUtils.calculateTransportFactors(oriDocument);
		double[] previousFactors = FluxMinimizationUtils.calculatePreviousFactors(oriDocument);
		
		System.out.println("SBML document read and splitted");
		
		// Read multi table file
		CSVDataImporter importer = new CSVDataImporter();
		MultiTable fullMT = importer.convert(oriDocument.getModel(), args[1]);
		fullMT.setTimeName("Time");
		
		System.out.println();
		
		
		System.out.println("Concentrations (and Fluxes) are read");
		
		
		Map<String, Integer> reactionIndices = new HashMap<String, Integer>();
		String[] reactionIds = new String[oriDocument.getModel().getReactionCount()];
		for (int i = 0; i < oriDocument.getModel().getReactionCount(); i++) {
			String id = oriDocument.getModel().getReaction(i).getId();
			reactionIndices.put(id , i);
			reactionIds[i] = id;
		}
		Map<String, Integer> speciesIndices = new HashMap<String, Integer>();
		String[] speciesIds = new String[oriDocument.getModel().getSpeciesCount()];
		for (int i = 0; i < oriDocument.getModel().getSpeciesCount(); i++) {
			String id = oriDocument.getModel().getSpecies(i).getId();
			speciesIndices.put(oriDocument.getModel().getSpecies(i).getId(), i);
			speciesIds[i] = id;
		}
		
//		String[] trFluxes = {
//				"r2526=HC00068_e", // Serine
//				"r2524=HC00048_e", // Alanine
//				"r2078=HC00177_e", // Lactate
//				"tr002=HC00266_e", // Isocitrate
//				"r1144=HC00034_e", // Glutamate
//				"r2525=HC00067_e", // Glutamine
//				"r1027=HC01472_e",  // GCDCA
//				"r1534,r1494=HC00863_e", // GCA
//				"r1535,r1496=HC01378_e"  // TCA
//		};
		
		//String[] transportFluxes = transportFluxesFromExtracellConcChanges(trFluxes, reactionIndices, speciesIndices);
		//double conversionFactor = splittedDocument.getModel().getCompartment("default").getSize();

		
		// constraint same fluxes
//		String[] sameFluxes = {
//				"+lr008-lr008_rev=+r1027-r1027_rev",
//				"+lr009-lr009_rev=+r1534-r1494",
//				"+lr010-lr010_rev=+r1535-r1496",
//				"+r1032-r1032_rev=+r0396-r0353",
//				"+r2526-r2526_rev=+r0060+r0160",
//				"+r2078-r2078_rev=+r0171_rev-r0171",
//				"+r2524-r2524_rev=+r0080-r0080_rev-r0160",
//				"+r2525-r2525_rev=+r0078-r0077"
//				};

//		String[] fluxPairs = getReactionPairIndices(reactionIndices, sameFluxes);
		
		// Read known fluxes file
//		String kf = args[2];
//		Map<Integer, Double> knownFluxes = null;
//		if (!kf.equalsIgnoreCase("noFile")) {
//			knownFluxes = readKnownFluxes(kf, true, reactionIndices);
//		}
		
		// Run a dynamic FBA
		
		// using splines
//		DynamicFBA dfba = new DynamicFBA(oriDocument, fullMT, 22);
		
		// using no splines
		DynamicFBA dfba = new DynamicFBA(oriDocument, fullMT);

		MultiTable smt =dfba.getDFBAStartingMultiTable();
		Column c = smt.getColumn(smt.findColumn("AS_c"));
		System.out.println(c.toString());

		
		
//		Map<Integer, double[]> knownFluxes = FluxMinimizationUtils.getKnownFluxesMap(dfba.getDFBAStartingMultiTable(), 1);
		
		FluxMinimizationIIa fm2 = new FluxMinimizationIIa();
		fm2.setFactors(previousFactors);
		fm2.setLambda2(100000);
		fm2.setCplexIterations(600);
		
		fm2.setConstraintJ0(true);
		fm2.setUsePreviousEstimations(true);
		fm2.setLittleFluxChanges(false);
		fm2.setConstraintZm(true);
		fm2.setFluxDynamic(false); // TODO test it
		fm2.setUseKnownFluxes(true);
//		fm2.setKnownFluxes(knownFluxes);
		fm2.setFluxLowerAndUpperBounds(-20, 300);
		fm2.setConcentrationLowerAndUpperBounds(0, 180000);
		
		dfba.runDynamicFBA(fm2);
		
		// Print solution MultiTable
		MultiTable solution = dfba.getFinalMultiTable();
		System.out.println(solution.toString());
		(new CSVWriter()).write(solution, ',', args[3]);
		
	}

	/**
	 * @param transportFluxes
	 * @param reactionIndices
	 * @param speciesIndices
	 * @return
	 */
	private static String[] transportFluxesFromExtracellConcChanges(
			String[] transportFluxes, Map<String, Integer> reactionIndices, Map<String, Integer> speciesIndices) {
		String[] tr = new String[transportFluxes.length];
		
		for (int i = 0; i < transportFluxes.length; i++) {
			String[] helper = transportFluxes[i].split("=");
			String r1, r2, ie;
			if (helper[0].split(",").length == 2) {
				r1 = helper[0].split(",")[0];
				r2 = helper[0].split(",")[1];
			}
			else {
				// get reversible if possible
				r1 = helper[0];
				r2 = helper[0] + FluxMinimizationUtils.endingForBackwardReaction;
			}
			int forward,backward;
			if (model.getReaction(r1).getListOfReactants().contains(helper[1])) {
				ie = "import";
				forward = reactionIndices.get(r1);
				backward = reactionIndices.get(r2);
			}
			else {
				ie = "export";
				backward = reactionIndices.get(r1);
				forward = reactionIndices.get(r2);
			}
			
			tr[i] = ie + ":" + forward + "," + backward + "=" + speciesIndices.get(helper[1]);
		}
		return tr;
	}

	/**
	 * 
	 * @param reactionIndices
	 * @param fluxPairs
	 * @return
	 */
	private static String[] getReactionPairIndices(Map<String, Integer> reactionIndices, String[] fluxPairs) {
		for (int i = 0; i < fluxPairs.length; i++) {
			System.out.println(fluxPairs[i]);
			for (Map.Entry<String, Integer> entry : reactionIndices.entrySet()) {
				String forward = entry.getKey();
				String reverse = forward + "_rev";
				if (fluxPairs[i].contains(reverse)) {
					continue;
				}
				else if (fluxPairs[i].contains(forward)) {
					fluxPairs[i] = fluxPairs[i].replaceAll(entry.getKey(), entry.getValue().toString());
				}
			}
		}
		return fluxPairs;
	}


	/**
	 * 
	 * @param knownFluxes
	 * @param header
	 * @param reactionIndices
	 * @param length 
	 * @return
	 * @throws IOException
	 */
	private static Map<Integer, double[]> readKnownFluxes(String knownFluxes, boolean header, Map<String, Integer> reactionIndices, int nTimepoints) throws IOException {
		Map<Integer, double[]> fluxes = new HashMap<Integer, double[]>();
		String line;

		BufferedReader input = new BufferedReader(new FileReader(knownFluxes));
		while ((line = input.readLine()) != null) {
			if (!header) {
				String[] helper = line.split("\t");
				String reactionId = helper[0];
				// FIXME size of compartiment volume and conversion factor is fixed at the moment
				Double d = Double.valueOf(helper[1]);
				double[] values = new double[nTimepoints];
				Arrays.fill(values, d);
				fluxes.put(reactionIndices.get(reactionId), values);
			}
			else {header = false;}
		}
		
		return fluxes;
		
	}
}
