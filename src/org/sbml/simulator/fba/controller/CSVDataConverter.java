/*
 * $Id:  ConstraintsUtils.java 12:50:53 Meike Aichele$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2013 by the University of Tuebingen, Germany.
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

import de.zbit.io.csv.CSVWriter;

/**
 * Class for reading the concentrations and Gibbs energies in steady state from a file.
 * 
 * @author Meike Aichele
 * @version $Rev$
 * @date 21.05.2012
 * @since 1.0
 */
public class CSVDataConverter {

	/**
	 * key string for concentration values
	 */
	public static final String KEY_CONCENTRATIONS = "concentration";

	/**
	 * key string for Gibbs values
	 */
	public static final String KEY_GIBBS = "gibbs";

	/**
	 * Containing the read concentrations.
	 */
	private double[] concentrationsArray;

	/**
	 * Containing the read Gibbs energies.
	 */
	private double[] gibbsArray;

	/**
	 * true if a concentration file is given
	 */
	private Boolean isConcentrationFile;

	/**
	 * true if a gibbs file is given
	 */
	private Boolean isGibbsFile;

	/**
	 * true if a system boundaries file is given
	 */
	private Boolean isSystemBoundariesFile;

	/**
	 * Containing the corresponding {@link SBMLDocument}.
	 */
	private SBMLDocument modifiedDocument;

	/**
	 * Containing the reader to read the csv files
	 */
	private CSVDataReader reader;

	/**
	 * Containing the species indices of species at the system boundaries
	 */
	private double[] systemBoundariesArray;

	/**
	 * Call this constructor if no system boundaries file is given, 
	 * for the automated creation of the system boundaries.
	 * @param originalDoc
	 * @throws Exception 
	 */
	public CSVDataConverter(SBMLDocument originalDoc) throws Exception {
		this.modifiedDocument = FluxMinimizationUtils.getExpandedDocument(originalDoc);
		this.systemBoundariesArray = FluxMinimizationUtils.getSystemBoundaries(originalDoc);
	}

	/**
	 * Call this constructor for the creation of a the systems boundaries array.
	 * @param originalDoc
	 * @param systemBoundariesFile
	 */
	public CSVDataConverter(SBMLDocument originalDoc, boolean systemBoundariesFile) {
		this.modifiedDocument = FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(originalDoc);
	}

	/**
	 * Call this constructor for the creation of the gibbs and the concentration array.
	 * @param originalDoc
	 * @param systemBoundaries
	 * @throws Exception
	 */
	public CSVDataConverter(SBMLDocument originalDoc, double[] systemBoundaries) throws Exception {
		this.modifiedDocument = FluxMinimizationUtils.getExpandedDocument(originalDoc, systemBoundaries);
	}

	/**
	 * @return the concentrationsArray
	 */
	public double[] getConcentrationsArray() {
		return concentrationsArray;
	}

	/**
	 * @return the gibbsArray
	 */
	public double[] getGibbsArray() {
		return gibbsArray;
	}

	/**
	 * 
	 * @return the csv-Reader
	 */
	public CSVDataReader getReader(){
		return reader;
	}

	/**
	 * 
	 * @return the systemBoundariesArray
	 */
	public double[] getSystemBoundariesArray() {
		return systemBoundariesArray;
	}

	/**
	 * Initializes the Concentration-array, so that every cell is filled with 10^(-11),
	 * because that is the normal minimal amount in nature, and when there is
	 * read a file with concentrations, the content will be overwritten.
	 */
	private void initializeConcentrationArray() {
		concentrationsArray = new double[modifiedDocument.getModel().getSpeciesCount()];
		for (int i = 0; i < concentrationsArray.length; i++) {
			concentrationsArray[i] = Math.pow(10, -11);
		}
	}

	/**
	 * Initializes the Gibbs-array, so that every cell is filled with NaN and when there is
	 * read a file with Gibbs energies, the content will be overwritten.
	 */
	private void initializeGibbsArray() {
		gibbsArray = new double[modifiedDocument.getModel().getReactionCount()];
		for (int i = 0; i < gibbsArray.length; i++) {
			gibbsArray[i] = Double.NaN;
		}
	}

	private void initializeSystemBoundariesArray() {
		systemBoundariesArray = new double[modifiedDocument.getModel().getSpeciesCount()];
		for (int i =0; i < systemBoundariesArray.length; i++) {
			systemBoundariesArray[i] = Double.NaN;
		}
	}

	/**
	 * Method to read equilibrium concentrations from a file.
	 * 
	 * @param file
	 * @return concentrationsArray
	 * @throws Exception 
	 */
	public double[] readConcentrationsFromFile (File file) throws Exception {
		this.isConcentrationFile = true;
		this.isGibbsFile = null;
		this.isSystemBoundariesFile = null;
		readFromFile(file);
		return concentrationsArray;
	}

	/**
	 * Reads out the Gibbs energies or concentrations from a given file and writes it in an array, that the method returns.
	 * 
	 * @param file
	 * @throws Exception 
	 */
	private void readFromFile(File file) throws Exception {
		reader = new CSVDataReader(file,null);
		reader.addPropertyChangeListener(EventHandler.create(PropertyChangeListener.class, this, "writeDataInArray", "newValue"));
		reader.cancel(true);
	}

	/**
	 * Reads out the Gibbs energies from a given file and writes it in the array gibbsEnergies
	 *
	 * @param file
	 * @return gibbsArray
	 * @throws Exception 
	 */
	public double[] readGibbsFromFile(File file) throws Exception {
		this.isGibbsFile = true;
		this.isConcentrationFile = null;
		this.isSystemBoundariesFile = null;
		readFromFile(file);
		return gibbsArray;
	}

	/**
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public double[] readSystemBoundariesFromFile(File file) throws Exception {
		this.isSystemBoundariesFile = true;
		this.isGibbsFile = null;
		this.isConcentrationFile = null;
		readFromFile(file);
		return systemBoundariesArray;
	}

	//TODO: call this method in the gui --> create a button for that
	/**
	 * Writes the computed values of the flux balance analysis in the given csv-File, which has 2 columns and for every reaction/ species a line.
	 * The first line contains the heading "Reaction_id" or "Species_id" (first column) and "steady_state_value" (second column).
	 * @param computed_solution
	 * @param file
	 * @throws Exception 
	 */
	public void writeComputedValuesInCSV(double[] computed_solution, File file) throws Exception {
		modifiedDocument = FluxMinimizationUtils.getExpandedDocument(modifiedDocument);
		CSVWriter writer = new CSVWriter();
		String[][] data = null;
		Boolean isFluxOrErrorSolution = null;
		Boolean isConcSolution = null;
		if (computed_solution.length == modifiedDocument.getModel().getReactionCount()) {
			data = new String[modifiedDocument.getModel().getReactionCount()+1][2];
			data[0][0] = "Reaction_id";
			isFluxOrErrorSolution = true;
		} else if (computed_solution.length == modifiedDocument.getModel().getSpeciesCount()) {
			data = new String[modifiedDocument.getModel().getSpeciesCount()+1][2];
			data[0][0] = "Species_id";
			isConcSolution = true;
		} 
		if (isConcSolution == null && isFluxOrErrorSolution == null) {
			// then the array has the wrong length
			throw new IllegalArgumentException("Solution is neither corressponding to the reactions in this model nor to " +
			"the species.");
		}
		data[0][1] = "steady_state_value";
		if (isFluxOrErrorSolution != null && isFluxOrErrorSolution) {
			// write the reactions ids
			for (int j = 1; j < modifiedDocument.getModel().getReactionCount(); j++) {
				data[j][0] = modifiedDocument.getModel().getReaction(j).getId();
				data[j][1] = Double.toString(computed_solution[j]);
			}
		} else if (isConcSolution != null && isConcSolution) {
			// write the species ids
			for (int i = 1; i < modifiedDocument.getModel().getSpeciesCount(); i++) {
				data[i][0] = modifiedDocument.getModel().getSpecies(i).getId();
				data[i][1] = Double.toString(computed_solution[i]);
			}
		}
		// write the solution
		writer.write(data, file);
	}

	/**
	 * Writes the incoming data from the CSV-file in an array. When it was a Gibbs data file
	 * the data will be written in the {@link# gibbsArray} else it was a concentration data file
	 * and the data will be written in the {@link# concentrationsArray}.
	 * @param obj
	 * @throws Exception 
	 */
	public void writeDataInArray(Object obj) throws Exception {
		if (obj instanceof String[][]) {
			String[][] data = (String[][]) obj;
			String[] values = new String[data.length];
			String[] keys = new String[data.length];
			int fileMatchToDocument = 0;
			// values are in the second column and keys in the first column
			for(int i = 0; i < data.length; i++) {
				values[i] = data[i][1];
				keys[i] = data[i][0];
			}

			if (isGibbsFile != null && isGibbsFile) {
				initializeGibbsArray();
				for(int i = 0; i < values.length; i++) {
					if (modifiedDocument.getModel().containsReaction(keys[i])) {
						if (!FluxMinimizationUtils.eliminatedReactions.contains(keys[i])) {
							modifiedDocument.getModel().getReaction(keys[i]).putUserObject(KEY_GIBBS, Double.parseDouble(values[i]));
							int index = modifiedDocument.getModel().getListOfReactions().getIndex(modifiedDocument.getModel().getReaction(keys[i]));
							gibbsArray[index] = Double.parseDouble(values[i]);
							fileMatchToDocument++;
							if (modifiedDocument.getModel().containsReaction(keys[i] + FluxMinimizationUtils.endingForBackwardReaction)){
								modifiedDocument.getModel().getReaction(keys[i] + FluxMinimizationUtils.endingForBackwardReaction).putUserObject(KEY_GIBBS, "isReverse");
								int index2 = modifiedDocument.getModel().getListOfReactions().getIndex(modifiedDocument.getModel().getReaction(keys[i] + FluxMinimizationUtils.endingForBackwardReaction));
								gibbsArray[index2] = -Double.parseDouble(values[i]);
							}
						}
					}
				}
			} else if(isConcentrationFile != null && isConcentrationFile){
				initializeConcentrationArray();
				for(int i = 0; i < values.length; i++) {
					if (modifiedDocument.getModel().containsSpecies(keys[i])) {
						modifiedDocument.getModel().getSpecies(keys[i]).putUserObject(KEY_CONCENTRATIONS, values[i]);
						int index = modifiedDocument.getModel().getListOfSpecies().getIndex(modifiedDocument.getModel().getSpecies(keys[i]));
						concentrationsArray[index] = Double.parseDouble(values[i]);
						fileMatchToDocument++;
					}
				}
			} else if (isSystemBoundariesFile != null && isSystemBoundariesFile) { 
				initializeSystemBoundariesArray();
				for (int i = 0; i < values.length; i++) {
					if (modifiedDocument.getModel().containsSpecies(keys[i])) {
						int index = modifiedDocument.getModel().getListOfSpecies().getIndex(modifiedDocument.getModel().getSpecies(keys[i]));
						if (values[i].equals("-")){
							systemBoundariesArray[index] = -1;
						}
						else if (values[i].equals("+")) {
							systemBoundariesArray[index] = +1;
						}
						else if (values[i].equals("=")) {
							systemBoundariesArray[index] = 0;
						}
						fileMatchToDocument++;
					}
				}
			}
			if (fileMatchToDocument == 0) {
				throw new Exception("given file does not match with opened SBMLDocument");				
			}
		}
	}
}