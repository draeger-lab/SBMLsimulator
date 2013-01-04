/*
 * $Id:  ContraintsUtilsTest.java 18:43:52 Meike Aichele$
 * $URL$
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
package org.sbml.simulator.fba;

import java.io.File;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.simulator.fba.controller.CSVDataConverter;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 13.06.2012
 * @since 1.0
 */
public class CSVDataConverterTest {

	static double[] c_eq = null;
	static double[] gibbs_eq = null;
	static String[] targetFluxes = null;
	static SBMLDocument sbml = null;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		sbml = (new SBMLReader()).readSBML(args[0]);

		// read the gibbs file
		CSVDataConverter cu = new CSVDataConverter(sbml);
		File file = new File(args[1]);
		cu.readGibbsFromFile(file);

		while(cu.getGibbsArray() == null){
			//wait
		}
		System.out.println("done reading");
		
		// test if it is written in an array
		gibbs_eq = cu.getGibbsArray();
		for (int i = 0; i < gibbs_eq.length; i++) {
			System.out.print(gibbs_eq[i] + " ");
		}
	}

}
