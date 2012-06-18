/*
 * $Id:  FluxMinimizationTest.java 12:30:10 Meike Aichele$
 * $URL$
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

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.simulator.fba.controller.CSVDataConverter;
import org.sbml.simulator.fba.controller.FluxMinimization;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 09.06.2012
 * @since 1.0
 */
public class FluxMinimizationTest {

	static double[] c_eq = null;
	static double[] gibbs_eq = null;
	static String[] targetFluxes = null;
	static SBMLDocument sbml = null;
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws XMLStreamException 
	 */
	public static void main(String[] args) throws Exception {

		// read
		sbml = (new SBMLReader()).readSBML(args[0]);
		File file_g = new File(args[1]);
		File file_c = new File(args[2]);
		CSVDataConverter converter = new CSVDataConverter(sbml);
		converter.readGibbsFromFile(file_g);
		converter.readConcentrationsFromFile(file_c);
		while(converter.getGibbsArray() == null) {
			//wait
		}
		while(converter.getConcentrationsArray() == null) {
			//wait
		}
		System.out.println("done reading");

		// set gibbs and concentrations
		gibbs_eq = converter.getGibbsArray();
		c_eq = converter.getConcentrationsArray();
		
		//test and create an FluxMinimization object:
		FluxMinimization fm = new FluxMinimization(sbml, c_eq, gibbs_eq, targetFluxes);
		double[] flux = fm.getFluxVector();
		for (int i = 0; i< fm.getFluxVector().length; i++) {
			System.out.println(flux[i]);
		}
		
	}

}
