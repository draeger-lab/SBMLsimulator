/*
 * $Id:  ConstraintsUtils.java 12:50:53 Meike Aichele$
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
package org.sbml.simulator.fba.controller;

import java.beans.EventHandler;
import java.beans.PropertyChangeListener;
import java.io.File;

import org.sbml.jsbml.SBMLDocument;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 21.05.2012
 * @since 1.0
 */
public class ConstraintsUtils {

	boolean gibbs;
	private SBMLDocument document;
	private double[] gibbsArray;

	/**
	 * Constructor
	 * @param doc
	 */
	public ConstraintsUtils(SBMLDocument doc) {
		this.document = doc;
	}
	
	/**
	 * Method to read equilibrium concentrations from a file.
	 * 
	 * @param files
	 * @return
	 */
	public double[] readConcentrationsFromFile (File files) {
		readFromFile(files, false);
		return null;
	}


	/**
	 * Reads out the Gibbs energies from a given file and writes it in the array gibbsEnergies
	 *
	 * @param files
	 * @return
	 */
	public double[] readGibbsFromFile(File files) {
		readFromFile(files, true);
		return gibbsArray;
		

		//		BufferedReader read = new BufferedReader(new FileReader(file));
		//		gibbsEnergies = new double[document.getModel().getReactionCount()];
		//		String line;
		//		String k[] = new String[2];
		//		
		//		// While there is a line to read, this method writes the gibbs-energy, to the
		//		// corresponding reaction in the model, in the same array-index, so that the indices match
		//		// and the reaction document.getModel().getReaction(3) has the gibbs-energy gibbsEnergies[3].
		//		while ((line = read.readLine()) != null) {
		//			k = line.split("\t");
		//			for (int i = 0; i < document.getModel().getReactionCount(); i++){
		//				if (k[0].equals(document.getModel().getReaction(i).getId())) {
		//					gibbsEnergies[i] = Double.valueOf(k[1]);
		//				}
		//			}
		//		}
	}

	/**
	 * Reads out the Gibbs energies or concentrations from a given file and writes it in an array, that the method returns.
	 * 
	 * @param files
	 * @param gibbs
	 */
	private void readFromFile(File files, Boolean gibbs) {
		this.gibbs = gibbs;
		CSVDataReader reader = new CSVDataReader(files);
		reader.addPropertyChangeListener(EventHandler.create(PropertyChangeListener.class, this, "writeDataInArray", "newValue"));

	}

	/**
	 * Writes the incoming data from the csv-file in {@link# solutionArray}.
	 * @param obj
	 */
	public void writeDataInArray(Object obj) {
		if (obj instanceof String[][]) {
            String[][] data = (String[][]) obj;
            String[] values = data[1];
			String[] keys = data[0];
			gibbsArray = new double[values.length-1];
			if (gibbs) {
				for(int i = 0; i< values.length; i++) {
					gibbsArray[i] = Double.parseDouble(values[i]);
					if (document.getModel().getReaction(keys[i]) != null) {
						document.getModel().getReaction(keys[i]).putUserObject("gibbs", gibbsArray[i]);
					}
				}
			} else {
				for(int i = 0; i< values.length; i++) {
					gibbsArray[i] = Double.parseDouble(values[i]);
					if (document.getModel().getSpecies(keys[i]) != null) {
						document.getModel().getSpecies(keys[i]).putUserObject("concentration", gibbsArray[i]);
					}
				}
			}
		}
	}

	/**
	 * @return the solutionArray
	 */
	public double[] getGibbsArray() {
		return gibbsArray;
	}

}