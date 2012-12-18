/*
 * $Id:  DynamicFBATest.java 12:14:48 faehnrich$
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
package org.sbml.simulator.fba.dynamic;

import java.io.File;
import java.util.Arrays;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.simulator.fba.controller.CSVDataConverter;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;
import org.sbml.simulator.io.CSVDataImporter;
import org.simulator.math.odes.MultiTable;

/**
 * 
 * @author Robin F&auml;hnrich
 * @version $Rev$
 * @since 1.0
 */
public class DynamicFBATest {

	/**
	 * Test the methods of class {@link DynamicFBA}.
	 * 
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		// Read SBML document file
		SBMLReader reader = new SBMLReader();
		SBMLDocument testDocument = reader.readSBML(args[0]);
		
		System.out.println("SBML document read");
		
		// Read concentration file
		String concFile = args[1];
		CSVDataImporter importer = new CSVDataImporter();
		MultiTable concMT = importer.convert(testDocument.getModel(), concFile);
		
		System.out.println("Concentrations read");
		
		// Read system boundary file
		File sysBoundFile = new File(args[2]);
		CSVDataConverter sysBoundConverter = new CSVDataConverter(testDocument, true);
		sysBoundConverter.readSystemBoundariesFromFile(sysBoundFile);
		while (sysBoundConverter.getSystemBoundariesArray() == null) {
			//wait
		}
		double[] sysBounds = sysBoundConverter.getSystemBoundariesArray();
		// very unusual!
		double[] correctedSysBounds = FluxMinimizationUtils.getCorrectedSystemBoundaries(testDocument, sysBounds);
		
		System.out.println("System boundaries read. Count: " + correctedSysBounds.length);
		System.out.println(Arrays.toString(correctedSysBounds));
		
		// Read gibbs energy file
		File gibbsFile = new File(args[3]);
		CSVDataConverter gibbsConverter = new CSVDataConverter(testDocument, correctedSysBounds);
		gibbsConverter.readGibbsFromFile(gibbsFile);
		while(gibbsConverter.getGibbsArray() == null) {
			//wait TODO check gibbs reading process, maybe wrong!
			Thread.sleep(1000);
		}
		double[] gibbsEnergies = gibbsConverter.getGibbsArray();
		
		System.out.println("Gibbs energies read. Count: " + gibbsEnergies.length);
		System.out.println(Arrays.toString(gibbsEnergies));
		
		// Run a dynamic FBA without linear interpolation
		DynamicFBA dfba = new DynamicFBA(testDocument, concMT);
		dfba.setGibbsEnergies(gibbsEnergies);
		dfba.setSystemBoundaries(correctedSysBounds);
		
		FluxMinimization fm = new FluxMinimization();
		dfba.prepareDynamicFBA(fm);
		dfba.runDynamicFBA(fm);
		
		// Print solution MultiTable
		MultiTable solution = dfba.getSolutionMultiTable();
		System.out.println(solution.toString());
		
	}

}
