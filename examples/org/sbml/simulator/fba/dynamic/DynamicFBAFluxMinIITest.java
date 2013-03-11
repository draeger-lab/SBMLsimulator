/*
 * $$Id:  ${file_name} ${time} ${user}$$
 * $$URL: ${file_name} $$
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBO;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.simulator.fba.controller.CSVDataConverter;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;
import org.sbml.simulator.io.CSVDataImporter;
import org.simulator.math.odes.MultiTable;

import de.zbit.io.csv.CSVWriter;

/**
 * @author Stephanie Tscherneck, Robin F&auml;hnrich
 * @version $$Rev$$
 * @since 1.0
 * ${tags}
 */

public class DynamicFBAFluxMinIITest {

	/**
	 * Test the methods of class {@link DynamicFBA}.
	 * args[0] = sbml document
	 * args[1] = concentration file as multitable
	 * args[2] = known fluxes as tab separated with headline
	 * args[3] = output multitable file
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		// Read SBML document file
		SBMLReader reader = new SBMLReader();
		SBMLDocument oriDocument = reader.readSBML(args[0]);
		SBMLDocument splittedDocument = FluxMinimizationUtils.getSplittedDocument(oriDocument);
		double[][] transportfactors = FluxMinimizationUtils.calculateTransportFactors(splittedDocument);
		double[][] previousFactors = FluxMinimizationUtils.calculatePreviousFactors(splittedDocument);
		
		System.out.println("SBML document read and splitted");
		
		// Read concentration file
		CSVDataImporter importer = new CSVDataImporter();
		MultiTable concMT = importer.convert(oriDocument.getModel(), args[1]);
		
		System.out.println("Concentrations read");
		
		Map<String, Integer> reactionIndices = new HashMap<String, Integer>();
		for (int i = 0; i < splittedDocument.getModel().getReactionCount(); i++) {
			reactionIndices.put(splittedDocument.getModel().getReaction(i).getId(), i);
		}
		
		// constraint same fluxes
		String[] sameFluxes = {
				"test=test2",
				"lr008=+r1027",
				"lr009=+r1534-r1494",
				"lr010=+r1535-r1496",
				"r1032=+r0396-r0353",
				"r2526=+r0060+r0160",
				"r2078=-r0171",
				"r2524=+r0080-r0160",
				"r2525=+r0078-r0077"
				};
		String[] fluxPairs = getReactionPairIndices(reactionIndices, sameFluxes);
		
		// Read known fluxes file
		Map<Integer, Double> knownFluxes = readKnownFluxes(args[2], true, reactionIndices);
		
		// Run a dynamic FBA
		// using splines
		DynamicFBA dfba = new DynamicFBA(oriDocument, concMT, 100);
		// using no splines
		//DynamicFBA dfba = new DynamicFBA(oriDocument, concMT);
		
		FluxMinimizationIIa fm2 = new FluxMinimizationIIa();
		fm2.setFluxPairs(fluxPairs);
//		fm2.setTransportFactors(transportfactors);
		fm2.setFactors(previousFactors);
		fm2.setKnownFluxes(knownFluxes);
		fm2.setLambda2(1000);
		fm2.setCplexIterations(1000000);

		dfba.runDynamicFBA(fm2);
		
		// Print solution MultiTable
		MultiTable solution = dfba.getSolutionMultiTable();
		System.out.println(solution.toString());
		
		(new CSVWriter()).write(solution, ',', args[3]);
		
	}

	/**
	 * 
	 * @param reactionIndices
	 * @param fluxPairs
	 * @return
	 */
	private static String[] getReactionPairIndices(Map<String, Integer> reactionIndices, String[] fluxPairs) {
		String[] pairIndices = new String[fluxPairs.length];

		for (Map.Entry<String, Integer> entry : reactionIndices.entrySet()) {
			for (int i = 0; i < fluxPairs.length; i++) {
				fluxPairs[i] = fluxPairs[i].replaceAll(entry.getKey(), entry.getValue().toString());
			}
		}
		
		
		return fluxPairs;
	}


	/**
	 * 
	 * @param knownFluxes
	 * @param header
	 * @param reactionIndices
	 * @return
	 * @throws IOException
	 */
	private static Map<Integer, Double> readKnownFluxes(String knownFluxes, boolean header, Map<String, Integer> reactionIndices) throws IOException {
		Map<Integer, Double> fluxes = new HashMap<Integer, Double>();
		String line;

		BufferedReader input = new BufferedReader(new FileReader(knownFluxes));
		while ((line = input.readLine()) != null) {
			if (!header) {
				String[] helper = line.split("\t");
				String reactionId = helper[0];
				// FIXME size of compartiment volume and conversion factor is fixed at the moment
				Double d = (Double.valueOf(helper[1]) * 4.248) / 1E3;
				fluxes.put(reactionIndices.get(reactionId), d);
			}
			else {header = false;}
		}
		
		return fluxes;
		
	}
}
