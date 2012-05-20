/*
 * $Id:  Constraints.java 16:17:07 Meike Aichele$
 * $URL: Constraints.java $
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
package org.sbml.simulator.fba.controller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.sbml.jsbml.SBMLDocument;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class Constraints {
	
	/**
	 * Contains the gibbs-energies and can be filled with
	 * the method readGibbsFromFile(String file)
	 */
	public double[] gibbsEnergies;
	
	/**
	 * Contains the opened {@link SBMLDocument}
	 */
	public SBMLDocument document;
	
	
	/**
	 * Constructor, that gets a {@link SBMLDocument} and creates a new
	 * array of gibbs-energies, that stays empty if the user doesn't check
	 * in a gibbs-energy file.
	 * 
	 * @param doc
	 */
	public Constraints(SBMLDocument doc) {
		this.document = doc;
		this.gibbsEnergies = new double[doc.getModel().getReactionCount()];
	}
	
	/**
	 * Reads out the gibbs-energies from a given file and writes it in the array gibbsEnergies
	 * @param file
	 * @throws IOException
	 */
	public void readGibbsFromFile(String file) throws IOException {
		BufferedReader read = new BufferedReader(new FileReader(file));
		String line;
		String k[] = new String[2];
		
		// While there is a line to read, this method writes the gibbs-energy, to the
		// corresponding reaction in the model, in the same array-index, so that the indices match
		// and the reaction document.getModel().getReaction(3) has the gibbs-energy gibbsEnergies[3].
		while ((line = read.readLine()) != null) {
			k = line.split("\t");
			for (int i = 0; i < document.getModel().getReactionCount(); i++){
				if (k[0].equals(document.getModel().getReaction(i).getId())) {
					gibbsEnergies[i] = Double.valueOf(k[1]);
				}
			}
		}
	}
	
}
