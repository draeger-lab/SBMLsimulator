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
 * Class for reading the concentrations and Gibbs energies in steady state from a file.
 * 
 * @author Meike Aichele
 * @version $Rev$
 * @date 21.05.2012
 * @since 1.0
 */
public class CSVDataConverter {

	private static final String KEY_CONCENTRATIONS = "concentration";

	private static final String KEY_GIBBS = "gibbs";

	/**
	 * Contains the information about the sort of the file:
	 * true if it is a Gibbs-file, false if it's not.
	 */
	boolean isGibbsFile;
	
	/**
	 * Containing the corresponding {@link SBMLDocument}.
	 */
	private SBMLDocument document;
	
	/**
	 * Containing the read Gibbs energies.
	 */
	private double[] gibbsArray;
	
	/**
	 * Containing the read concentrations.
	 */
	private double[] concentrationsArray;


	/**
	 * Containing the reader to read the csv files
	 */
	private CSVDataReader reader;

	/**
	 * Constructor
	 * @param doc
	 */
	public CSVDataConverter(SBMLDocument doc) {
		this.document = doc;
	}
	
	/**
	 * Method to read equilibrium concentrations from a file.
	 * 
	 * @param files
	 * @return
	 * @throws Exception 
	 */
	public double[] readConcentrationsFromFile (File files) throws Exception {
		readFromFile(files, false);
		return concentrationsArray;
	}


	/**
	 * Reads out the Gibbs energies from a given file and writes it in the array gibbsEnergies
	 *
	 * @param files
	 * @return
	 * @throws Exception 
	 */
	public double[] readGibbsFromFile(File files) throws Exception {
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
	 * @throws Exception 
	 */
	private void readFromFile(File files, Boolean isGibbs) throws Exception {
		this.isGibbsFile = isGibbs;
		reader = new CSVDataReader(files,null);
		reader.addPropertyChangeListener(EventHandler.create(PropertyChangeListener.class, this, "writeDataInArray", "newValue"));
		reader.cancel(true);
	}

	/**
	 * Writes the incoming data from the csv-file in {@link# solutionArray}.
	 * @param obj
	 */
	public void writeDataInArray(Object obj) {
		if (obj instanceof String[][]) {
            String[][] data = (String[][]) obj;
            String[] values = new String[data.length];
			String[] keys = new String[data.length];
			// values are in the second column and keys in the first column
			for(int i = 0; i < data.length; i++) {
				values[i] = data[i][1];
				keys[i] = data[i][0];
			}
			
			if (isGibbsFile) {
				gibbsArray = new double[values.length];
				for(int i = 0; i< values.length; i++) {
					gibbsArray[i] = Double.parseDouble(values[i]);
					if (document.getModel().getReaction(keys[i]) != null) {
						document.getModel().getReaction(keys[i]).putUserObject(KEY_GIBBS, gibbsArray[i]);
					}
				}
			} else {
				concentrationsArray = new double[values.length];
				for(int i = 0; i< values.length; i++) {
					concentrationsArray[i] = Double.parseDouble(values[i]);
					if (document.getModel().getSpecies(keys[i]) != null) {
						document.getModel().getSpecies(keys[i]).putUserObject(KEY_CONCENTRATIONS, concentrationsArray[i]);
					}
				}
			}
		}
	}

	/**
	 * @return the gibbsArray
	 */
	public double[] getGibbsArray() {
		return gibbsArray;
	}

	/**
	 * @return the concentrationsArray
	 */
	public double[] getConcentrationsArray() {
		return concentrationsArray;
	}
	

	public CSVDataReader getReader(){
		return reader;
	}
}