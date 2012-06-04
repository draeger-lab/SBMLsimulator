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
import java.io.IOException;
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

	//TODO: method to read equilibrium concentrations
	// should be similar to gibbs-file-reader, perhaps write one method for both tasks
	
	/**
	 * Reads out the Gibbs energies from a given file and writes it in the array gibbsEnergies
	 * @param file
	 * @throws IOException
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public static double[] readGibbsFromFile(SimulatorUI ui, File[] files) throws IOException, InterruptedException, ExecutionException {
		SerialWorker worker = new SerialWorker();
		double[] gibbsEnergies = null;

		if ((files != null) && (files.length > 0)){
			CSVReadingTask task = new CSVReadingTask(ui, files);
			task.addPropertyChangeListener(EventHandler.create(PropertyChangeListener.class, ui, "addExperimentalData", "newValue"));
			worker.add(task);
			
			if(task.isDone()){
				String[] values = task.get().values().toArray(new String[ui.getModel().getReactionCount()]);
				String[] keys = task.get().keySet().toArray(new String[ui.getModel().getReactionCount()]);

				gibbsEnergies = new double[values.length-1];
				for(int i = 0; i< values.length; i++) {
					gibbsEnergies[i] = Double.parseDouble(values[i]);
					if (ui.getModel().getReaction(keys[i]) != null) {
						ui.getModel().getReaction(keys[i]).putUserObject("gibbs", gibbsEnergies[i]);
					}
				}
			}
		}

		worker.execute();

		return gibbsEnergies;

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

}
