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
import java.util.concurrent.ExecutionException;

import org.sbml.simulator.gui.SimulatorUI;
import org.sbml.simulator.io.CSVReadingTask;

import de.zbit.gui.SerialWorker;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 21.05.2012
 * @since 1.0
 */
public class ConstraintsUtils {

	/**
	 * Method to read equilibrium concentrations from a file.
	 * @param ui
	 * @param files
	 * @return double[] concentrations
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static double[] readConcentrationsFromFile (SimulatorUI ui, File files) throws InterruptedException, ExecutionException {
		return readFromFile(ui, files, false);
	}


	/**
	 * Reads out the Gibbs energies from a given file and writes it in the array gibbsEnergies
	 * @param ui
	 * @param file
	 * @return double[] gibbs
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public static double[] readGibbsFromFile(SimulatorUI ui, File files) throws InterruptedException, ExecutionException {

		return readFromFile(ui, files, true);

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
		//			for (int i = 0; i < document.getModel().getReactionCount(); i++) {
		//				if (k[0].equals(document.getModel().getReaction(i).getId())) {
		//					gibbsEnergies[i] = Double.valueOf(k[1]);
		//				}
		//			}
		//		}
	}

	/**
	 * Reads out the Gibbs energies or concentrations from a given file and writes it in an array, that the method returns.
	 * 
	 * @param ui
	 * @param files
	 * @param gibbs
	 * @return double[]
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private static double[] readFromFile(SimulatorUI ui, File files, boolean gibbs) throws InterruptedException, ExecutionException {
		SerialWorker worker = new SerialWorker();
		double[] erg = null;
		if (files != null) {
			CSVReadingTask task = new CSVReadingTask(ui, files);
			task.addPropertyChangeListener(EventHandler.create(PropertyChangeListener.class, ui, "addExperimentalData", "newValue"));
			worker.add(task);

			if (task.isDone()) {
				if (gibbs) {
				String[] values = task.get().values().toArray(new String[ui.getModel().getReactionCount()]);
				String[] keys = task.get().keySet().toArray(new String[ui.getModel().getReactionCount()]);

				erg = new double[values.length-1];
				
					for(int i = 0; i< values.length; i++) {
						erg[i] = Double.parseDouble(values[i]);
						if (ui.getModel().getReaction(keys[i]) != null) {
							ui.getModel().getReaction(keys[i]).putUserObject("gibbs", erg[i]);
						}
					}
				} else {
					String[] values = task.get().values().toArray(new String[ui.getModel().getSpeciesCount()]);
					String[] keys = task.get().keySet().toArray(new String[ui.getModel().getSpeciesCount()]);

					erg = new double[values.length-1];
					
					for(int i = 0; i< values.length; i++) {
						erg[i] = Double.parseDouble(values[i]);
						if (ui.getModel().getSpecies(keys[i]) != null) {
							ui.getModel().getSpecies(keys[i]).putUserObject("concentration", erg[i]);
						}
					}
				}
			}
		}

		worker.execute();
		return erg;
	}

}
