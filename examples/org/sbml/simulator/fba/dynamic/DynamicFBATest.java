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

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;
import org.sbml.simulator.io.CSVDataImporter;
import org.simulator.math.odes.MultiTable;
import org.simulator.math.odes.MultiTable.Block.Column;

/**
 * @author Robin F&auml;hnrich
 * @version $Rev$
 * @since 1.0
 */
public class DynamicFBATest {

	/**
	 * Test the methods of {@link DynamicFBA}.
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		// Read sbml document and concentration file
		SBMLReader reader = new SBMLReader();
		SBMLDocument testDocument = reader.readSBML(args[0]);
		String concFile = args[1];
		CSVDataImporter importer = new CSVDataImporter();
		MultiTable concMT = importer.convert(testDocument.getModel(), concFile);
		
		// Test multitable output
		DynamicFBA dfba = new DynamicFBA(testDocument, concMT, 44);
		System.out.println(dfba.getSolutionMultiTable().toString());
		System.out.println(dfba.getDFBAconcentrations().toString());
		
		// Test linear interpolation
		Column testColumn = dfba.getDFBAconcentrations().getColumn("HC00685_i");
		System.out.println(testColumn.toString());
		
		
	}

}
