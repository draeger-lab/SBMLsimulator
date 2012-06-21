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

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.simulator.fba.controller.CSVDataConverter;
import org.sbml.simulator.fba.controller.Constraints;
import org.sbml.simulator.fba.controller.FluxBalanceAnalysis;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 17.06.2012
 * @since 1.0
 */
public class FluxBalanceAnalysisTest {


	static double[] c_eq = null;
	static double[] gibbs_eq = null;
	static String[] targetFluxes = null;
	static SBMLDocument sbml = null;
	static Constraints constraints = null;
	
	/**
	 * @param args[0] the SBMLDocument-File
	 * args[1] the Gibbs-File
	 * args[2] the concentration-File
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		sbml = (new SBMLReader()).readSBML(args[0]);

		//ConstraintsUtils:
		CSVDataConverter cu = new CSVDataConverter(sbml);
		File file_g = new File(args[1]);
		File file_c = new File(args[2]);
		
		// read gibbs and concentrations
		cu.readGibbsFromFile(file_g);

		while(cu.getGibbsArray() == null) {
			//wait
		}
		System.out.println("-> done gibbs");
		gibbs_eq = cu.getGibbsArray();

		System.out.println(cu.getReader().getState());
		cu.readConcentrationsFromFile(file_c);
		System.out.println(cu.getReader().getState());
		while(cu.getConcentrationsArray() == null) {
			//wait
		}
		System.out.println("-> done concentrations");
		c_eq = cu.getConcentrationsArray();

		//create FluxBalanceAnalysis object and solve it:
		constraints =  new Constraints(sbml, gibbs_eq, c_eq);
		FluxBalanceAnalysis fba = new FluxBalanceAnalysis(c_eq, constraints, sbml, targetFluxes);
		fba.solve();
		
		//print flux solution:
		double[] fluxsolution = fba.solution_fluxVector;
		String solution = "[ ";
		for (int i = 0; i < fluxsolution.length; i++) {
			solution += fluxsolution + " ";
		}
		System.out.println("solutions for the fluxes: " + solution + "]");
	}

}
