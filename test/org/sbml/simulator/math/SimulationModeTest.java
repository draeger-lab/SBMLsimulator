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
import java.util.Properties;

import org.sbml.jsbml.resources.Resource;
import org.sbml.simulator.SimulationOptions;
import org.sbml.simulator.gui.SimulatorUI;
import org.sbml.simulator.io.SimulatorIOOptions;

import de.zbit.io.csv.CSVOptions;
import de.zbit.util.prefs.SBProperties;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class SimulationModeTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
	// try {
	// System.loadLibrary("sbmlj");
	// // Extra check to be sure we have access to libSBML:
	// Class.forName("org.sbml.libsbml.libsbml");
	// } catch (Exception e) {
	// System.err.println("Error: could not load the libSBML library");
	// e.printStackTrace();
	// System.exit(1);
	// }
	new SimulationModeTest(args[0]);
    }

    public SimulationModeTest(String testCasesDir) {

	String sbmlFile, csvfile, configfile;
	if (!testCasesDir.endsWith("/")) {
	    testCasesDir += "/";
	}

	SBProperties settings = new SBProperties();
	// SBMLsqueezer.getProperties();
	// settings.put(CfgKeys.CHECK_FOR_UPDATES, Boolean.valueOf(false));

	// SBMLio sbmlIo = new SBMLio(new LibSBMLReader(), new LibSBMLWriter());
	// SBMLsqueezerUI gui = new SBMLsqueezerUI(sbmlIo, settings);

	// 919
	for (int modelnr = 1; modelnr < 2; modelnr++)
	    try {
		StringBuilder modelFile = new StringBuilder();
		modelFile.append(modelnr);
		while (modelFile.length() < 5) {
		    modelFile.insert(0, '0');
		}
		String path = modelFile.toString();
		modelFile.append('/');
		modelFile.append(path);
		modelFile.insert(0, testCasesDir);
		path = modelFile.toString();
		sbmlFile = path + "-sbml-l2v4.xml";
		csvfile = path + "-results.csv";
		configfile = path + "-settings.txt";

		settings.put(SimulatorIOOptions.SBML_INPUT_FILE, sbmlFile);
		settings.put(CSVOptions.CSV_FILE, csvfile);

		Properties cfg = Resource.readProperties(configfile);
		double start = Double.parseDouble(cfg.get("start").toString());
		double end = start
			+ Double.parseDouble(cfg.get("duration").toString());
		double stepsize = (end - start)
			/ Double.parseDouble(cfg.get("steps").toString());

		settings.put(SimulationOptions.SIM_START_TIME, Double
			.valueOf(start));
		settings
			.put(SimulationOptions.SIM_END_TIME, Double.valueOf(end));
		settings.put(SimulationOptions.SIM_STEP_SIZE, Double
			.valueOf(stepsize));

		// sbmlIo.convert2Model(sbmlFile);

		if (sbmlFile != null) {
		  SimulatorUI d = new SimulatorUI();
		  d.open(new File(sbmlFile));
		  d.setSelectedQuantities(cfg.get("variables").toString()
			    .trim().split(", "));
		  if (csvfile != null) {
			  d.open(new File(csvfile));
		  }
		  d.simulate();
		  d.setVisible(true);
		}

	    } catch (Exception e) {
		e.printStackTrace();
	    }

	// gui.dispose();
	System.exit(0);
    }

}
