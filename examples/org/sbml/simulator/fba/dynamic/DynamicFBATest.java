/*
 * $Id$URL:
 * https://rarepos.cs.uni-tuebingen
 * .de/svn-path/SBMLsimulator/trunk/examples/org/
 * sbml/simulator/fba/dynamic/DynamicFBATest.java $
 * --------------------------------------------------------------------- This
 * file is part of SBMLsimulator, a Java-based simulator for models of
 * biochemical processes encoded in the modeling language SBML.
 * 
 * Copyright (C) 2007-2014 by the University of Tuebingen, Germany.
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation. A copy of the license agreement is provided in the file
 * named "LICENSE.txt" included with this software distribution and also
 * available online as <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.fba.dynamic;

import java.util.Arrays;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.simulator.fba.controller.CSVDataConverter;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;
import org.sbml.simulator.io.CSVDataImporter;
import org.simulator.math.odes.MultiTable;

import de.zbit.io.csv.CSVWriter;

/**
 * Test a dynamic flux balance analysis.
 * 
 * @author Robin F&auml;hnrich
 * @version $Rev$
 * @since 1.0
 */
public class DynamicFBATest {
	
	/**
	 * Test the methods of class {@link DynamicFBA}.
	 * args[0] = sbml document
	 * args[1] = concentration file as multitable
	 * args[2] = system boundaries 
	 * args[3] = gibbs energies as tab separated with headline
	 * args[4] = output multitable file
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
		CSVDataImporter importer = new CSVDataImporter();
		MultiTable concMT = importer.convert(testDocument.getModel(), args[1]);
		
		System.out.println("Concentrations read");
		
		// Read system boundary file
		CSVDataConverter sysBoundConverter = new CSVDataConverter(testDocument,
			true);
		double[] sysBounds = sysBoundConverter.readSystemBoundaries(args[2]);
		System.out.println();
		
		// Remember: get corrected system boundaries after reading the sysBound file
		double[] correctedSysBounds = FluxMinimizationUtils
				.getCorrectedSystemBoundaries(testDocument, sysBounds);
		System.out.println("System boundaries read. Count: "
				+ correctedSysBounds.length);
		System.out.println(Arrays.toString(correctedSysBounds));
		
		// Read gibbs energy file
		CSVDataConverter gibbsConverter = new CSVDataConverter(testDocument,
			correctedSysBounds);
		double[] gibbsEnergies = gibbsConverter.readGibbs(args[3]);
		System.out.println();
		
		// Remember: get corrected gibbs energies after reading the gibbs file 
		double[] correctedGibbsEnergies = FluxMinimizationUtils
				.getEquillibriumGibbsEnergiesfromkKiloJoule(gibbsEnergies);
		System.out.println("Gibbs energies read. Count: "
				+ correctedGibbsEnergies.length);
		System.out.println(Arrays.toString(correctedGibbsEnergies));
		
		// Run a dynamic FBA
		DynamicFBA dfba = new DynamicFBA(testDocument, concMT, 1000);
		
		FluxMinimization fm = new FluxMinimization();
		fm.setCplexIterations(1000);
		fm.setReadGibbsEnergies(correctedGibbsEnergies);
		fm.setReadSystemBoundaries(correctedSysBounds);
		dfba.runDynamicFBA(fm);
			
		// Print solution MultiTable
		MultiTable solution = dfba.getWorkingSolutionMultiTable();
		System.out.println(solution.toString());
		
		(new CSVWriter()).write(solution, ',', args[4]);
		
	}
	
}
