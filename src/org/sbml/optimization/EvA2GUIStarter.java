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
package org.sbml.optimization;

import java.awt.Window;
import java.awt.event.WindowListener;
import java.util.logging.Logger;

import eva2.client.EvAClient;
import eva2.gui.BeanInspector;
import eva2.gui.GenericObjectEditor;
import eva2.server.go.operators.terminators.EvaluationTerminator;
import eva2.server.go.problems.InterfaceOptimizationProblem;
import eva2.server.go.strategies.DifferentialEvolution;
import eva2.server.modules.GOParameters;
import eva2.server.stat.InterfaceStatisticsListener;

/**
 * 
 * @author Marcel Kronfeld
 * @date 2010-09-01
 * @version $Rev$
 * @since 1.0
 */
public class EvA2GUIStarter {

	/**
	 * 
	 */
	private EvAClient evaClient = null;
	
	/**
	 * Private in order to prohibit initialization of this class.
	 */
	private EvA2GUIStarter() {
		super();
	}

	/**
	 * 
	 * @param problem
	 * @param parentWindow
	 * @param statisticsListener
	 * @param windowListener
	 */
	public static void init(InterfaceOptimizationProblem problem,
			final Window parentWindow,
			InterfaceStatisticsListener statisticsListener,
			WindowListener windowListener) {
		EvA2GUIStarter evaBP = new EvA2GUIStarter();
		logger.fine("created EvA2 GUI starter");
		GOParameters goParams = new GOParameters(); // Instance for the general
		logger.fine("created GO parameters");
		// Genetic Optimization
		// parameterization

//		goParams.setOptimizer(new ParticleSwarmOptimization(50, 2.05, 2.05,
//				PSOTopologyEnum.grid, 2));
		DifferentialEvolution optimizer = new DifferentialEvolution();
		goParams.setOptimizer(optimizer);
		goParams.setTerminator(new EvaluationTerminator(3000));
		logger.fine("created optimizer Differential Evolution");

		// set the initial EvA problem here
		goParams.setProblem(problem);
		logger.fine("Set problem");

		// hide some properties which should not be shown
		logger.fine("hiding unnecessary GUI options");
		GenericObjectEditor.setHideProperty(goParams.getClass(), "problem", true);
		GenericObjectEditor.setHideProperty(goParams.getClass(), "postProcessParams", true);
		// public EvAClient(final String hostName, final Window parent, final
		// String paramsFile, final InterfaceGOParameters goParams, final
		// boolean autorun, final boolean noSplash, final boolean noGui) {
		logger.fine("launching the actual EvA Client");
		evaBP.evaClient = new EvAClient(null, parentWindow, null, goParams, false, true, false); // initializes GUI in the background
		// important: wait for GUI initialization before accessing any internal
		// settings:
		logger.fine("waiting for EvA Client to get ready");
		evaBP.evaClient.awaitClientInitialized(); // this returns as soon as the
		// GUI is ready
		logger.fine("adding WindowListener to the EvA Client");
		evaBP.evaClient.addWindowListener(windowListener);

		// modify initial settings:
		logger.fine("defining statistics properties");
		evaBP.evaClient.getStatistics().getStatisticsParameter()
				.setOutputAllFieldsAsText(true); // activate output of all data
		// fields

		logger.fine("updating main EvA2 main panel");
		evaBP.evaClient.refreshMainPanels(); // GUI update due to the changes
		// made through the API

		// add a data listener instance:
		logger.fine("adding data listener to EvA2");
		evaBP.evaClient.getStatistics().addDataListener(statisticsListener);
		logger.fine("EvA2 should now be ready.");
	}

	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(EvA2GUIStarter.class.getName());

	/* (non-Javadoc)
	 * @see eva2.server.stat.InterfaceStatisticsListener#notifyGenerationPerformed(java.lang.String[], java.lang.Object[], java.lang.Double[])
	 */
	public void notifyGenerationPerformed(String[] header,
			Object[] statObjects, Double[] statDoubles) {
		// statDoubles only contains double representations of statObject where
		// possible (so its redundant)
		logger.info("Received data " + BeanInspector.toString(statObjects));
	}

	/* (non-Javadoc)
	 * @see eva2.server.stat.InterfaceStatisticsListener#notifyRunStarted(int, int, java.lang.String[], java.lang.String[])
	 */
	public void notifyRunStarted(int runNumber, int plannedMultiRuns,
			String[] header, String[] metaInfo) {
		logger.info("notifyRunStarted, " + runNumber + " of "	+ plannedMultiRuns);
		logger.info("Headers: " + BeanInspector.toString(header));
		logger.info("Meta-info: " + BeanInspector.toString(metaInfo));
	}

	/* (non-Javadoc)
	 * @see eva2.server.stat.InterfaceStatisticsListener#notifyRunStopped(int, boolean)
	 */
	public void notifyRunStopped(int runsPerformed, boolean completedLastRun) {
		logger.info("notifyRunStopped, " + runsPerformed + ", last finished normally: " + completedLastRun);
	}
}
