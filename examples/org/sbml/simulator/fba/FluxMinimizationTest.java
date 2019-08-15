/*
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2016 by the University of Tuebingen, Germany.
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
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.simulator.fba.controller.CSVDataConverter;
import org.sbml.simulator.fba.controller.FluxMinimization;

/**
 * @author Meike Aichele
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
		CSVDataConverter converter1 = new CSVDataConverter(sbml);
		CSVDataConverter converter2 = new CSVDataConverter(sbml);
		converter1.readGibbsFromFile(file_g);
		converter2.readConcentrationsFromFile(file_c);
		System.out.println(converter1.getReader().getState());
		while(converter1.getGibbsArray() == null) {
			//wait
		}
		System.out.println(converter1.getReader().getState());
		while(converter2.getConcentrationsArray() == null) {
			//wait
		}
		System.out.println("done reading");

		// set gibbs and concentrations
		gibbs_eq = converter1.getGibbsArray();
		c_eq = converter2.getConcentrationsArray();
		//test and create an FluxMinimization object:
		FluxMinimization fm = new FluxMinimization(sbml, c_eq, gibbs_eq, targetFluxes);
		double[] flux = fm.getFluxVector();
		
		System.out.println("The computed flux vector: ");
		System.out.print("[ ");
		for (int i = 0; i< fm.getFluxVector().length; i++) {
			System.out.print(flux[i] + " ");
		}
		System.out.print("]");
		System.out.println();
		
		double[] conc = fm.getConcentrations();
		System.out.println("The computed concentrations: ");
		System.out.print("[ ");
		for (int i = 0; i< conc.length; i++) {
			System.out.print(conc[i] + " ");
		}
		System.out.print("]");
		
	}

}
