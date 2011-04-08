/*
 * $Id$ $URL:
 * svn://rarepos/SBMLsimulator/trunk/src/org/sbml/simulator/SBMLsimulator.java $
 * --------------------------------------------------------------------- This
 * file is part of SBMLsimulator, a Java-based simulator for models of
 * biochemical processes encoded in the modeling language SBML.
 * 
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation. A copy of the license agreement is provided in the file
 * named "LICENSE.txt" included with this software distribution and also
 * available online as <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.optimization.PlotOptions;
import org.sbml.optimization.problem.EstimationOptions;
import org.sbml.simulator.gui.SimulatorUI;
import org.sbml.simulator.math.QualityMeasure;
import org.sbml.simulator.math.odes.AbstractDESSolver;
import org.sbml.simulator.math.odes.SimulationOptions;

import de.zbit.gui.GUIOptions;
import de.zbit.io.CSVOptions;
import de.zbit.util.Reflect;
import de.zbit.util.logging.LogUtil;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;

/**
 * @author Andreas Dr&auml;ger
 * @author Roland Keller
 * @date 2010-09-01
 * @version $Rev$
 * @since 1.0
 */
public class SBMLsimulator {
	
	/**
	 * The logger for this class.
	 */
	public static final Logger logger = Logger.getLogger(SBMLsimulator.class
			.getName());
	
	/**
	 * The possible location of this class in a jar file if used in plug-in mode.
	 */
	public static final String JAR_LOCATION = "plugin" + File.separatorChar;
	/**
	 * The package where all mathematical functions, in particular distance
	 * functions, are located.
	 */
	public static final String MATH_PACKAGE = "org.sbml.simulator.math";
	
	/**
	 * The package where all ODE solvers are assumed to be located.
	 */
	public static final String SOLVER_PACKAGE = "org.sbml.simulator.math.odes";
	
	/**
	 * The version number of this program.
	 */
	private static final String VERSION_NUMBER = "0.7";
	
	/**
	 * An array of all available implementations of distance functions to judge
	 * the quality of a simulation based on parameter and initial value settings.
	 */
	private static final Class<QualityMeasure> AVAILABLE_QUALITY_MEASURES[] = Reflect
			.getAllClassesInPackage(MATH_PACKAGE, true, true, QualityMeasure.class,
				JAR_LOCATION, true);
	/**
	 * An array of all available ordinary differential equation solvers.
	 */
	private static final Class<AbstractDESSolver> AVAILABLE_SOLVERS[] = Reflect
			.getAllClassesInPackage(SOLVER_PACKAGE, true, true,
				AbstractDESSolver.class, JAR_LOCATION, true);
	
	/**
	 * @return
	 */
	public static final Class<QualityMeasure>[] getAvailableQualityMeasures() {
		return AVAILABLE_QUALITY_MEASURES;
	}
	
	/**
	 * @return
	 */
	public static Class<AbstractDESSolver>[] getAvailableSolvers() {
		return AVAILABLE_SOLVERS;
	}
	
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Class<? extends KeyProvider>[] getCommandLineOptions() {
		return new Class[] { SimulatorOptions.class, SimulationOptions.class,
				EstimationOptions.class, GUIOptions.class, PlotOptions.class,
				CSVOptions.class };
	}
	
	/**
	 * 
	 * @return
	 * @throws MalformedURLException
	 */
	public static URL getURLOnlineUpdate() throws MalformedURLException {
		return new URL(
			"http://www.ra.cs.uni-tuebingen.de/software/SBMLsimulator/downloads/");
	}
	
	/**
	 * Returns the version number of this program.
	 * 
	 * @return
	 */
	public static String getVersionNumber() {
		return VERSION_NUMBER;
	}
	
	/**
	 * @param args
	 * @throws XMLStreamException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException,
		XMLStreamException {
		
		LogUtil.initializeLogging(Level.FINE, "org.sbml", "de.zbit");
		
		SBProperties p = SBPreferences.analyzeCommandLineArguments(
			getCommandLineOptions(), args);
		String openFile = null;
		if (p.containsKey(SimulatorOptions.SBML_FILE)) {
			openFile = p.get(SimulatorOptions.SBML_FILE).toString();
		}
		String timeSeriesFile = null;
		if (p.containsKey(SimulatorOptions.TIME_SERIES_FILE)) {
			timeSeriesFile = p.get(SimulatorOptions.TIME_SERIES_FILE).toString();
		}
		if ((openFile == null) || (openFile.length() == 0)) {
			new SBMLsimulator();
		} else {
			if ((timeSeriesFile == null) || (timeSeriesFile.length() == 0)) {
				new SBMLsimulator(openFile);
			} else {
				new SBMLsimulator(openFile, timeSeriesFile);
			}
		}
	}
	
	/**
     * 
     */
	public SBMLsimulator() {
		this(null);
	}
	
	/**
	 * @param pathname
	 *        path to an SBML model
	 */
	public SBMLsimulator(String pathname) {
		this(pathname, null);
	}
	
	/**
	 * @param sbmlFile
	 * @param timeSeriesFile
	 * @throws XMLStreamException
	 * @throws FileNotFoundException
	 */
	public SBMLsimulator(String sbmlFile, String timeSeriesFile) {
		super();
		SimulatorUI simulatorUI = new SimulatorUI();
		if (sbmlFile != null) {
			simulatorUI.openModel(new File(sbmlFile));
		}
		simulatorUI.setLocationRelativeTo(null);
		simulatorUI.setVisible(true);
		if (timeSeriesFile != null) {
			simulatorUI.openExperimentalData(timeSeriesFile);
		}
	}
	
	/**
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static List<Class> getAvailableQualityMeasureClasses() {
		List<Class> qualityList = new LinkedList<Class>();
		for (Class qualityMeasureClass : AVAILABLE_QUALITY_MEASURES) {
			try {
				qualityList.add(qualityMeasureClass);
			} catch (Exception e) {
				logger.warning(e.getLocalizedMessage());
			}
		}
		return qualityList;
	}
	
	/**
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static List<Class> getAvailableSolverClasses() {
		List<Class> solverList = new LinkedList<Class>();
		for (Class<AbstractDESSolver> solverClass : AVAILABLE_SOLVERS) {
			try {
				solverList.add(solverClass);
			} catch (Exception e) {
				logger.warning(e.getLocalizedMessage());
			}
			
		}
		return solverList;
	}
}
