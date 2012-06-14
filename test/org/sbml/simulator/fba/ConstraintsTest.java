/*
 * $Id:  ConstraintsTest.java 19:03:59 Meike Aichele$
 * $URL: ConstraintsTest.java $
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
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.simulator.fba.controller.Constraints;
import org.sbml.simulator.fba.controller.ConstraintsUtils;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 13.06.2012
 * @since 1.0
 */
public class ConstraintsTest {

	/**
	 * @param args
	 */
	static double[] c_eq = null;
	static double[] gibbs_eq = null;
	static String[] targetFluxes = null;
	static SBMLDocument sbml = null;
	
	public static void main(String[] args) throws XMLStreamException, IOException {
		sbml = (new SBMLReader()).readSBML(args[0]);
		
		//ConstraintsUtils:
		ConstraintsUtils cu = new ConstraintsUtils(sbml);
		File file_g = new File(args[1]);
		File file_c = new File(args[2]);
		gibbs_eq = cu.readGibbsFromFile(file_g);
		c_eq = cu.readConcentrationsFromFile(file_c);
		
		//TODO: no gibbs-values read after this method-call...
		System.out.println(gibbs_eq);
		
//		for (int i = 0; i < gibbs_eq.length; i++) {
//			System.out.print(gibbs_eq[i] + " ");
//		}
		
		
		//Constraints:
		Constraints con = new Constraints(sbml);
		con.setEquilibriumConcentrations(c_eq);
		con.setEquilibriumGibbsEnergies(gibbs_eq);
		
		//testing if computing r_max works
		double[] fluxVector = FluxMinimizationUtils.computeFluxVector(null, sbml);
		double r_max = con.computeR_max(fluxVector);
		System.out.println("r_max = " + r_max);
	}

}
