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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.sbml.simulator.gui.CSVDataImporter;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.MultiBlockTable;
import org.simulator.math.odes.RosenbrockSolver;
import org.simulator.sbml.SBMLinterpreter;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class SimulationTestAutomatic {
	private static final Logger logger = Logger.getLogger(SimulationTestAutomatic.class.getName());
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
		for (int modelnr = 1; modelnr <= 400; modelnr++) {
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
			sbmlfile = path + "-sbml-l3v1.xml";
			csvfile = path + "-results.csv";
			configfile = path + "-settings.txt";

			Properties props = new Properties();
			props.load(new BufferedReader(new FileReader(configfile)));
//			int start = Integer.valueOf(props.getProperty("start"));
			double duration = Double.valueOf(props.getProperty("duration"));
			double steps = Double.valueOf(props.getProperty("steps"));
//			double absolute = Double.valueOf(props.getProperty("absolute"));
//			double relative = Double.valueOf(props.getProperty("relative"));
			/*
			 * Other variables:
			 * variables: S1, S2
			 * amount:
			 * concentration:
			 */
			try {
				Model model = (new SBMLReader()).readSBML(sbmlfile).getModel();
					//sbmlIo.convert2Model(sbmlfile);

				AbstractDESSolver solver = new RosenbrockSolver();
				SBMLinterpreter interpreter = new SBMLinterpreter(model);
				// get timepoints
				CSVDataImporter csvimporter = new CSVDataImporter();
				MultiBlockTable inputData = csvimporter.convertWithoutWindows(model, csvfile);
				
				double[] timepoints = inputData.getTimePoints();

				
				duration=timepoints[timepoints.length-1]-timepoints[0];
				solver.setStepSize(duration/steps);
				MultiBlockTable solution=solver.solve(interpreter, interpreter.getInitialValues(), timepoints);
				
				
				QualityMeasure distance = new Euclidean();

				double dist=distance.distance(solution, inputData);
				
				BufferedWriter writer = new BufferedWriter(new FileWriter(
						args[1] + modelnr + "-deviation.txt"));
				writer.write("relative distance for model-" + modelnr);
				writer.newLine();
				/*
				writer.write(String.valueOf());
				*/
				writer.write(String.valueOf(dist));
				writer.close();
				
				if(dist>0.1) {
					logger.log(Level.INFO, "relative distance for model-" + modelnr);
					logger.log(Level.INFO,String.valueOf(dist));
				}
				if (solver.isUnstable()) {
					logger.warning("unstable!");
				} 
//					else {
//					(new Thread(new Plotter(solution, inputData, args[1]
//							+ modelnr + "-graph.jpg"))).start();
//				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
