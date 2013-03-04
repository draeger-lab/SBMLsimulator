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
		SBMLDocument expandedDocument = FluxMinimizationUtils.splitAllReversibleReactions(oriDocument);
		double[][] transportfactors = FluxMinimizationUtils.calculateTransportFactors(expandedDocument);
		 
		System.out.println("SBML document read and splitted");
		
		// Read concentration file
		CSVDataImporter importer = new CSVDataImporter();
		MultiTable concMT = importer.convert(oriDocument.getModel(), args[1]);
		
		System.out.println("Concentrations read");
		
	
		
		// Read known fluxes file
		Map<String, Double> reaction2Fluxes = readKnownFluxes(args[2], true);
		HashMap<Integer, Double> knownFluxes = new HashMap<Integer, Double>();
		//------------0nM Atorvastatin---------------------
//		reaction2Fluxes.put("lr003_rev", 18.9);
//		reaction2Fluxes.put("r0171_rev", 45.5);
//		reaction2Fluxes.put("r0317", 12.1);
//		reaction2Fluxes.put("r0426_rev", 12.1);
//		reaction2Fluxes.put("lr014", 12.1);
//		reaction2Fluxes.put("r0104", 92.2);
//		reaction2Fluxes.put("r0080", 12.0);
//		reaction2Fluxes.put("r0323_rev", 4.1);
//		reaction2Fluxes.put("r0123", 81.2);
//		reaction2Fluxes.put("r0334_rev", 0.06);
		//------------50nM Atorvastatin---------------------
//		reaction2Fluxes.put("lr003_rev", 18.9);
//		reaction2Fluxes.put("r0171_rev", 46.6);
//		reaction2Fluxes.put("r0317", 10.8);
//		reaction2Fluxes.put("r0426_rev", 10.8);
//		reaction2Fluxes.put("lr014", 10.8);
//		reaction2Fluxes.put("r0104", 88.2);
//		reaction2Fluxes.put("r0080", 11.6);
//		reaction2Fluxes.put("r0323_rev", 4.9);
//		reaction2Fluxes.put("r0123", 79.3);
//		reaction2Fluxes.put("r0334_rev", 0.04);
		
		for (int i = 0; i < expandedDocument.getModel().getReactionCount(); i++) {
			Double d = reaction2Fluxes.get(expandedDocument.getModel().getReaction(i).getId());
			System.out.println(expandedDocument.getModel().getReaction(i));
			if (d != null) {
				knownFluxes.put(i, d);
			}
		}
		
		// Run a dynamic FBA
		DynamicFBA dfba = new DynamicFBA(oriDocument, concMT, 50);
		
		FluxMinimizationIIa fm2 = new FluxMinimizationIIa();
		fm2.setTransportFactors(transportfactors);
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
	 * @param string
	 * @throws IOException 
	 */
	private static Map<String, Double> readKnownFluxes(String knownFluxes, boolean header) throws IOException {
		Map<String, Double> reaction2Fluxes = new HashMap<String, Double>();
		String line;

		BufferedReader input = new BufferedReader(new FileReader(knownFluxes));
		while ((line = input.readLine()) != null) {
			if (!header) {
				String[] helper = line.split("\t");
				String reactionId = helper[0];
				Double d = Double.valueOf(helper[1]);
				if (d < 0) {
					reactionId.concat(FluxMinimizationUtils.endingForBackwardReaction);
					Double neg = d;
					d = neg * -1;
				}
				reaction2Fluxes.put(reactionId, d);
			}
			else {header = false;}
		}
		
		return reaction2Fluxes;
		
	}
}
