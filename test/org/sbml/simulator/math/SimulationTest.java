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

import java.io.File;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.sbml.simulator.io.CSVDataImporter;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.MultiTable;
import org.simulator.math.odes.RosenbrockSolver;
import org.simulator.sbml.SBMLinterpreter;

import eva2.gui.Plot;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class SimulationTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		try {
//			System.loadLibrary("sbmlj");
//			// Extra check to be sure we have access to libSBML:
//			Class.forName("org.sbml.libsbml.libsbml");
//		} catch (Exception e) {
//			System.err.println("Error: could not load the libSBML library");
//			e.printStackTrace();
//			System.exit(1);
//		}
		// path =
		// "C:/Dokumente und Einstellungen/radbarbeit11/Desktop/tst suite/sbml-test-cases-2009-09-05/cases/semantic/";
		// path += "00204/00204-sbml-l2v4.xml";
//		SBMLio sbmlIo = new SBMLio(new LibSBMLReader(), new LibSBMLWriter());
		// System.out.println(args[0]);
		// System.out.println(path);
		try {
			Model model = (new SBMLReader()).readSBML(args[0]).getModel(); 
				//sbmlIo.convert2Model(args[0]);
			// Model model = sbmlIo.readModel(path);
			AbstractDESSolver solver = new RosenbrockSolver();
			SBMLinterpreter interpreter = new SBMLinterpreter(model);
			double time = 0;
			
			CSVDataImporter importer = new CSVDataImporter();
	    MultiTable experimentalData = importer.convert(model,
	            (new File(args[1])).getAbsolutePath());
	    
			solver.setStepSize(0.1);
			// TODO: Rel-Tolerance, Abs-Tolerance.
			MultiTable solution = solver.solve(interpreter, interpreter.getInitialValues(), 0, 10);
			
			MultiTable solution1 = solver.solve(interpreter,
        experimentalData.getBlock(0), interpreter.getInitialValues());
      
			
			MultiTable solution2= solver.solve(interpreter, interpreter.getInitialValues(),
        experimentalData.getTimePoints());
			
			QualityMeasure qm = new EuclideanDistance();
			System.out.println(qm.distance(solution, experimentalData));
			System.out.println(qm.distance(solution1, experimentalData));
			System.out.println(qm.distance(solution2, experimentalData));
			
			System.out.println(solution.getColumnCount() - 1);
			
			if (solver.isUnstable()) {
				System.err.println("unstable!");
			} else {
				int from = model.getNumCompartments();
				int to = from + model.getNumSpecies();
				Plot plot = new Plot("Simulation", "time", "value");
				for (int i = 0; i < solution.getRowCount(); i++) {
					double[] symbol = solution.getBlock(0).getRow(i);
					for (int j = from; j < to; j++) {

						double sym = symbol[j];
						plot.setConnectedPoint(time, sym, j);

					}
					time += solver.getStepSize();

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
