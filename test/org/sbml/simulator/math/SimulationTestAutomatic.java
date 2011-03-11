/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.math;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.sbml.simulator.math.odes.AbstractDESSolver;
import org.sbml.simulator.math.odes.MultiBlockTable;
import org.sbml.simulator.math.odes.RKEventSolver;

import de.zbit.io.CSVReader;
import eva2.gui.Plot;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class SimulationTestAutomatic {

	static {
//		try {
//			System.loadLibrary("sbmlj");
//			// Extra check to be sure we have access to libSBML:
//			Class.forName("org.sbml.libsbml.libsbml");
//		} catch (Exception e) {
//			System.err.println("Error: could not load the libSBML library");
//			e.printStackTrace();
//			System.exit(1);
//		}
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String sbmlfile, csvfile, configfile;
		int start = 0;
		double duration = 0d, steps = 0d;

		for (int modelnr = 1; modelnr <= 2; modelnr++) {
			System.out.println("model " + modelnr);

			StringBuilder modelFile = new StringBuilder();
			modelFile.append(modelnr);
			while (modelFile.length() < 5)
				modelFile.insert(0, '0');
			String path = modelFile.toString();
			modelFile.append('/');
			modelFile.append(path);
			modelFile.insert(0, args[0]);
			path = modelFile.toString();
			sbmlfile = path + "-sbml-l2v4.xml";
			csvfile = path + "-results.csv";
			configfile = path + "-settings.txt";

			try {
				BufferedReader reader = new BufferedReader(new FileReader(
						configfile));
				String line = reader.readLine();
				start = Integer.valueOf(line
						.substring(line.lastIndexOf(' ') + 1));
				line = reader.readLine();
				duration = Double.valueOf(line
						.substring(line.lastIndexOf(' ') + 1));
				line = reader.readLine();
				steps = Double.valueOf(line
						.substring(line.lastIndexOf(' ') + 1));
				reader.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

//			SBMLio sbmlIo = new SBMLio(new LibSBMLReader(), new LibSBMLWriter());

			try {
				Model model = (new SBMLReader()).readSBML(sbmlfile).getModel();
					//sbmlIo.convert2Model(sbmlfile);

				// RKSolver rk = new RKSolver();
				AbstractDESSolver rk = new RKEventSolver();

				SBMLinterpreter interpreter = new SBMLinterpreter(model);
				double time = 0;

				// get timepoints
				CSVReader csvreader = new CSVReader(csvfile);
//				,					',', '\'', 1);
				String[][] input = csvreader.read();
				double[] timepoints = new double[input.length];
				for (int i = 0; i < timepoints.length; i++) {
					timepoints[i] = Double.valueOf(input[i][0]);
				}
				csvreader.close();

				// solve By StepSize
				// rk.setStepSize(duration / steps);
				// double solution[][] = rk.solveByStepSize(interpreter,
				// interpreter
				// .getInitialValues(), time, duration);

				// solve At StepSize

				MultiBlockTable solution = rk.solve(interpreter, interpreter
						.getInitialValues(), timepoints);
				Double[][] data = new Double[input[0].length - 1][input.length];
				Double[][] solutiontrans = new Double[input[0].length - 1][input.length];
				int from = model.getNumCompartments();
				// from = 0;
				int to = from + model.getNumSpecies();

				for (int i = 1; i < data.length + 1; i++) {
					for (int j = 0; j < input.length; j++) {
						data[i - 1][j] = Double.valueOf(input[j][i]);
						solutiontrans[i - 1][j] = solution.getValueAt(j, i
								+ from);
					}
				}

				Distance distance = new RSE();

				BufferedWriter writer = new BufferedWriter(new FileWriter(
						args[1] + modelnr + "-deviation.txt"));
				writer.write("relative distance for model-" + modelnr);
				writer.newLine();
				writer.write(String.valueOf(distance.distance(data,
						solutiontrans)));
				writer.close();

				if (rk.isUnstable())
					System.err.println("unstable!");
				else {
					Plot plot = new Plot("Simulation", "time", "value");

					for (int i = 0; i < solution.getRowCount(); i++) {
						for (int j = 0; j < solutiontrans.length; j++) {

							double sym = solutiontrans[j][i];
							double un = data[j][i];
							plot.setConnectedPoint(time, sym, j);
							plot.setUnconnectedPoint(time, un, 90 + j);

						}
						time += rk.getStepSize();

					}
					// save graph as jpg
					BufferedImage img = new BufferedImage(plot
							.getFunctionArea().getWidth(), plot
							.getFunctionArea().getHeight(),
							BufferedImage.TYPE_INT_RGB);
					plot.getFunctionArea().paint(img.createGraphics());
					ImageIO.write(img, "jpg", new File(args[1] + modelnr
							+ "-graph.jpg"));

					// plot.dispose();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
