/*
 * $Id:  FluxMinimizationUtilsTest.java 17:35:33 Meike Aichele$
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

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;
import org.sbml.simulator.stability.math.StabilityMatrix;
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

		// test the method to compute the manhattan norm of a vector:
		System.out.println("Manhattan-Norm of the vector (1,-1,2):");
		double[] vector = {1, -1, 2};
		System.out.println(FluxMinimizationUtils.computeManhattenNorm(vector));


		// test the method to compute the stoichiometric matrix for the SBMLDocument:
		System.out.println("stoichiometric matrix:");
		StoichiometricMatrix testMatrix;
		try {
			testMatrix = FluxMinimizationUtils.SBMLDocToStoichMatrix(sbml);
			for (int i = 0; i < FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(sbml).getModel().getReactionCount(); i++) {
//				if(FluxMinimizationUtils.reversibleReactions.contains(FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(sbml).getModel().getReaction(i).getId())){
				System.out.print(FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(sbml).getModel().getReaction(i).getId()+" ");
//				}
			}
			System.out.println();
			for(int i = 0 ; i < testMatrix.getRowDimension(); i++) {
				System.out.print(FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(sbml).getModel().getSpecies(i).getId() + ": ");
				for (int j = 0 ; j < testMatrix.getColumnDimension(); j++) {
//					if(FluxMinimizationUtils.reversibleReactions.contains(FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(sbml).getModel().getReaction(j).getId())){
						System.out.print(testMatrix.get(i, j) + " ");
//					}
				}
				System.out.println();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}


		// test the method to compute the flux vector:
		System.out.println();
		System.out.println("Flux vector:");
		double[] fluxvector;
		System.out.println(FluxMinimizationUtils.eliminatedReactions.toString());
		try {
			fluxvector = FluxMinimizationUtils.computeFluxVector(null, sbml);
			for (int i= 0; i < fluxvector.length; i++) {
				System.out.print(FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(sbml).getModel().getReaction(i).getId() + "  ");
				System.out.println(fluxvector[i] + " ");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// test the method to eliminate transport reactions:
		System.out.println();
		System.out.println();
		System.out.println("Test eliminating transport reactions in the given document: ");
		SBMLDocument new_Doc = FluxMinimizationUtils.eliminateTransports(sbml);
		System.out.println(!new_Doc.equals(sbml));
		
		
		System.out.println();
		System.out.println("Test eliminate and split: new document reactions:");
		SBMLDocument new_Doc2 = FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(sbml);
		for(int i =0; i < new_Doc2.getModel().getReactionCount(); i++) {
			System.out.println(new_Doc2.getModel().getReaction(i).getId());
		}

		System.out.println();
		System.out.println("Test if the flux is right computed: (it should be (1,1,1,1))");
		double[][] sMatrix = {{-1,0,0,1}, {1,-1,0,0}, {0,1,-1,0}, {0,0,1,-1}};
		StoichiometricMatrix S = new StoichiometricMatrix(sMatrix, 4, 4);
		
		StabilityMatrix steadyStateMatrix = S.getSteadyStateFluxes();
		double[] fluxVector = new double[steadyStateMatrix.getRowDimension()];
		System.out.println("Flux of S:");
		for (int row = 0; row < S.getColumnDimension(); row++) {
			fluxVector[row] = steadyStateMatrix.get(row, steadyStateMatrix.getColumnDimension()-1);
			System.out.print(fluxVector[row] + " ");
		}
		System.out.println();
		System.out.println("-----");
		System.out.println("S:");
		System.out.println(S.toString());


	}

}
