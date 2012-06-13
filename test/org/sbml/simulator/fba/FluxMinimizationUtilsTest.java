/*
 * $Id:  FluxMinimizationUtilsTest.java 17:35:33 Meike Aichele$
 * $URL: FluxMinimizationUtilsTest.java $
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

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;
import org.sbml.simulator.stability.math.StoichiometricMatrix;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 13.06.2012
 * @since 1.0
 */
public class FluxMinimizationUtilsTest {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws XMLStreamException 
	 */
	public static void main(String[] args) throws XMLStreamException, IOException {
		final SBMLDocument sbml = (new SBMLReader()).readSBML(args[0]);

		System.out.println("Manhattan-Norm of the vector (1,-1,2):");
		double[] vector = {1, -1, 2};
		System.out.println(FluxMinimizationUtils.computeManhattenNorm(vector));


		System.out.println("stoichiometric matrix:");
		StoichiometricMatrix testMatrix = FluxMinimizationUtils.SBMLDocToStoichMatrix(sbml);
		for(int i = 0 ; i < testMatrix.getColumnDimension(); i++) {
			for (int j = 0 ; j < testMatrix.getRowDimension(); j++) {
				System.out.print(testMatrix.get(j, i) + " ");
			}
			System.out.println();
		}

		System.out.println();
		System.out.println("Flux vector:");
		double[] fluxvector = FluxMinimizationUtils.computeFluxVector(null, sbml);
		for (int i= 0; i < fluxvector.length; i++) {
			System.out.print(fluxvector[i] + " ");
		}

		System.out.println();
		System.out.println();
		System.out.println("Test eliminating target reactions");
		SBMLDocument new_Doc = FluxMinimizationUtils.eliminateTransports(sbml);
		System.out.println(new_Doc.equals(sbml));

	}

}
