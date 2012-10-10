/*
 * $Id$
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
package org.sbml.simulator;

import java.awt.HeadlessException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sbml.optimization.problem.EstimationOptions;
import org.sbml.simulator.fba.gui.FBAOptions;
import org.sbml.simulator.gui.SimulatorUI;
import org.sbml.simulator.gui.graph.GraphOptions;
import org.sbml.simulator.gui.plot.PlotOptions;
import org.sbml.simulator.io.SimulatorIOOptions;
import org.simulator.math.QualityMeasure;
import org.simulator.math.odes.AbstractDESSolver;

import de.zbit.AppConf;
import de.zbit.Launcher;
import de.zbit.gui.BaseFrame;
import de.zbit.gui.GUIOptions;
import de.zbit.io.csv.CSVOptions;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.SBProperties;

/**
 * Start program for {@link SBMLsimulator}.
 * 
 * @author Andreas Dr&auml;ger
 * @author Roland Keller
 * @date 2010-09-01
 * @version $Rev$
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public class SBMLsimulator extends Launcher {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = -6519145035944241806L;

	/**
	 * The logger for this class.
	 */
	public static final transient Logger logger = Logger
			.getLogger(SBMLsimulator.class.getName());

//	/**
//	 * The possible location of this class in a jar file if used in plug-in
//	 * mode.
//	 */
//	public static final String JAR_LOCATION = "plugin"
//			+ StringUtil.fileSeparator();

	/**
	 * The package where all mathematical functions, in particular distance
	 * functions, are located.
	 */
	public static final String MATH_PACKAGE = "org.sbml.simulator.math";

	/**
	 * The package where all ODE solvers are assumed to be located.
	 */
	public static final String SOLVER_PACKAGE = "org.simulator.math.odes";

	/**
	 * An array of all available implementations of distance functions to judge
	 * the quality of a simulation based on parameter and initial value settings.
	 */
	private static final Class<QualityMeasure> AVAILABLE_QUALITY_MEASURES[];
	
	/**
	 * An array of all available ordinary differential equation solvers.
	 */
	private static final Class<AbstractDESSolver> AVAILABLE_SOLVERS[];
	
	static {
		int i;
		//	AVAILABLE_QUALITY_MEASURES = Reflect.getAllClassesInPackage(MATH_PACKAGE,
		//	true, true, QualityMeasure.class, JAR_LOCATION, true);
		String classes[] = new String[] {
				"org.simulator.math.EuclideanDistance",
				"org.simulator.math.ManhattanDistance",
				"org.simulator.math.N_Metric",
				"org.simulator.math.PearsonCorrelation",
				"org.simulator.math.RelativeEuclideanDistance",
				"org.simulator.math.RelativeManhattanDistance",
				"org.simulator.math.RelativeSquaredError",
				"org.simulator.math.Relative_N_Metric" };
		AVAILABLE_QUALITY_MEASURES = new Class[classes.length];
		for (i = 0; i < classes.length; i++) {
			try {
				AVAILABLE_QUALITY_MEASURES[i] = (Class<QualityMeasure>) Class.forName(classes[i]);
			} catch (ClassNotFoundException exc) {
				logger.severe(exc.getLocalizedMessage());
			}
		}
		
		// AVAILABLE_SOLVERS = Reflect.getAllClassesInPackage(SOLVER_PACKAGE, true,
		//   true, AbstractDESSolver.class, JAR_LOCATION, true);
		classes = new String[] { 
				"org.simulator.math.odes.AdamsBashforthSolver",
				"org.simulator.math.odes.AdamsMoultonSolver",
				"org.simulator.math.odes.DormandPrince54Solver",
				"org.simulator.math.odes.DormandPrince853Solver",
				"org.simulator.math.odes.EulerMethod",
				"org.simulator.math.odes.GraggBulirschStoerSolver",
				"org.simulator.math.odes.HighamHall54Solver",
				"org.simulator.math.odes.RosenbrockSolver",
				"org.simulator.math.odes.RungeKutta_EventSolver" };
		AVAILABLE_SOLVERS = new Class[classes.length];
		for (i = 0; i < classes.length; i++) {
			try {
				AVAILABLE_SOLVERS[i] = (Class<AbstractDESSolver>) Class.forName(classes[i]);
			} catch (ClassNotFoundException exc) {
				logger.severe(exc.getLocalizedMessage());
			}
		}
	}

	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static List<Class> getAvailableQualityMeasureClasses() {
		List<Class> qualityList = new ArrayList<Class>(
				AVAILABLE_QUALITY_MEASURES.length);
		for (Class<? extends QualityMeasure> qualityMeasureClass : AVAILABLE_QUALITY_MEASURES) {
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
	public static List<String> getAvailableQualityMeasureClassNames() {
		List<String> qualityList = new ArrayList<String>(
				AVAILABLE_QUALITY_MEASURES.length);
		for (Class<?> qualityMeasureClass : AVAILABLE_QUALITY_MEASURES) {
			try {
				qualityList.add(qualityMeasureClass.getName());
			} catch (Exception e) {
				logger.warning(e.getLocalizedMessage());
			}
		}
		return qualityList;
	}

	/**
	 * @return
	 */
	public static final Class<QualityMeasure>[] getAvailableQualityMeasures() {
		return AVAILABLE_QUALITY_MEASURES;
	}

	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static List<Class> getAvailableSolverClasses() {
		List<Class> solverList = new ArrayList<Class>(AVAILABLE_SOLVERS.length);
		for (Class<AbstractDESSolver> solverClass : AVAILABLE_SOLVERS) {
			try {
				solverList.add(solverClass);
			} catch (Exception e) {
				logger.warning(e.getLocalizedMessage());
			}
		}
		return solverList;
	}

	/**
	 * 
	 * @return
	 */
	public static Class<AbstractDESSolver>[] getAvailableSolvers() {
		return AVAILABLE_SOLVERS;
	}

	/**
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws HeadlessException 
	 */
	public static void main(String args[]) {
		new SBMLsimulator(args);
	}

	/**
	 * 
	 * @param args
	 */
	public SBMLsimulator(String[] args) {
		super(args);
	}

	/* (non-Javadoc)
	 * @see de.zbit.Launcher#commandLineMode(de.zbit.AppConf)
	 */
	public void commandLineMode(AppConf appConf) {
		String openFile = null;
		SBProperties props = appConf.getCmdArgs();
		if (props.containsKey(SimulatorIOOptions.SBML_INPUT_FILE)) {
			openFile = props.get(SimulatorIOOptions.SBML_INPUT_FILE).toString();
		}
		String timeSeriesFile = null;
		if (props.containsKey(SimulatorIOOptions.TIME_SERIES_FILE)) {
			timeSeriesFile = props.get(SimulatorIOOptions.TIME_SERIES_FILE)
					.toString();
		}

		if (openFile != null) {
			CommandLineManager commandLineManager = new CommandLineManager(openFile,
				timeSeriesFile, appConf);
			commandLineManager.run();
		} else {
			logger.fine(String.format(
					getResources().getString("INCOMPLETE_CMD_ARG_LIST"),
					getAppName(), getVersionNumber()));

		}
	}

	/* (non-Javadoc)
	 * @see de.zbit.Launcher#getAppName()
	 */
	public String getAppName() {
		return getClass().getSimpleName();
	}

	/* (non-Javadoc)
	 * @see de.zbit.Launcher#getCmdLineOptions()
	 */
	public List<Class<? extends KeyProvider>> getCmdLineOptions() {
		List<Class<? extends KeyProvider>> defAndKeys = new ArrayList<Class<? extends KeyProvider>>(6);
		defAndKeys.add(SimulatorIOOptions.class);
		defAndKeys.add(SimulationOptions.class);
		defAndKeys.add(EstimationOptions.class);
		defAndKeys.add(GUIOptions.class);
		defAndKeys.add(PlotOptions.class);
		defAndKeys.add(CSVOptions.class);
		return defAndKeys;
	}

	/* (non-Javadoc)
	 * @see de.zbit.Launcher#getInteractiveOptions()
	 */
	public List<Class<? extends KeyProvider>> getInteractiveOptions() {
		List<Class<? extends KeyProvider>> defAndKeys = new ArrayList<Class<? extends KeyProvider>>(5);
		defAndKeys.add(SimulationOptions.class);
		defAndKeys.add(EstimationOptions.class);
		defAndKeys.add(PlotOptions.class);
		defAndKeys.add(GraphOptions.class);
		defAndKeys.add(FBAOptions.class);
		return defAndKeys;
	}

	/* (non-Javadoc)
	 * @see de.zbit.Launcher#getLogPackages()
	 */
	@Override
	public String[] getLogPackages() {
		return new String[] { "de.zbit", "org.sbml", "org.simulator", "eva" };
	}

	/* (non-Javadoc)
	 * @see de.zbit.Launcher#getURLlicenseFile()
	 */
	public URL getURLlicenseFile() {
		URL url = null;
		try {
			url = new URL("http://www.gnu.org/licenses/lgpl-3.0-standalone.html");
		} catch (MalformedURLException exc) {
			logger.log(Level.FINER, exc.getLocalizedMessage(), exc);
		}
		return url;
	}

	/* (non-Javadoc)
	 * @see de.zbit.Launcher#getURLOnlineUpdate()
	 */
	public URL getURLOnlineUpdate() {
		URL url = null;
		try {
			url = new URL("http://www.cogsys.cs.uni-tuebingen.de/software/SBMLsimulator/downloads/");
		} catch (MalformedURLException exc) {
			logger.log(Level.FINER, exc.getLocalizedMessage(), exc);
		}
		return url;
	}

	/* (non-Javadoc)
	 * @see de.zbit.Launcher#getVersionNumber()
	 */
	public String getVersionNumber() {
		return "1.0";
	}

	/* (non-Javadoc)
	 * @see de.zbit.Launcher#getYearOfProgramRelease()
	 */
	public short getYearOfProgramRelease() {
		return (short) 2012;
	}

	/* (non-Javadoc)
	 * @see de.zbit.Launcher#getYearWhenProjectWasStarted()
	 */
	public short getYearWhenProjectWasStarted() {
		return 2007;
	}

	/* (non-Javadoc)
	 * @see de.zbit.Launcher#initGUI(de.zbit.AppConf)
	 */
	public BaseFrame initGUI(AppConf appConf) {
		return new SimulatorUI(appConf);
	}

}
