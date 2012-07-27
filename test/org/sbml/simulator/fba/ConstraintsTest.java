/*
 * $Id:  ConstraintsTest.java 19:03:59 Meike Aichele$
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
import org.sbml.simulator.fba.controller.Constraints;
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
	
	public static void main(String[] args) throws Exception {
		sbml = (new SBMLReader()).readSBML(args[0]);
		
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
		gibbs_eq = converter1.getGibbsArray();
		System.out.println(converter1.getReader().getState());
		while(converter2.getConcentrationsArray() == null) {
			//wait
		}
		c_eq = converter2.getConcentrationsArray();
		System.out.println("done reading");
		
		System.out.print("read gibbs array: [ ");
		for (int i = 0; i < gibbs_eq.length; i++) {
			System.out.print(gibbs_eq[i] + " ");
		}
		System.out.print("]");
		System.out.println();
		
		
		//Constraints:
		Constraints constraints = new Constraints(sbml);
		constraints.setEquilibriumConcentrations(c_eq);
		constraints.setEquilibriumGibbsEnergies(gibbs_eq);
		
		//testing if computing r_max works
		double[] fluxVector = FluxMinimizationUtils.computeFluxVector(null, sbml);
		double r_max[] = constraints.computeR_max(fluxVector);
		for ( int i = 0; i < r_max.length; i++) {
			System.out.println(r_max[i]);
		}
		
		System.out.println("the computed Gibbs energies:");
		for (int i = 0; i < constraints.getGibbsEnergies().length; i++) {
			System.out.print(FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(sbml).getModel().getReaction(i).getId() + "  ");
			System.out.print(constraints.getGibbsEnergies()[i]);
			System.out.println();
		}
	}

}
