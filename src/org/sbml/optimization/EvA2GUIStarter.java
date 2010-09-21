package org.sbml.optimization;

import java.awt.Window;
import java.util.List;

import eva2.client.EvAClient;
import eva2.gui.BeanInspector;
import eva2.gui.GenericObjectEditor;
import eva2.server.go.enums.PSOTopologyEnum;
import eva2.server.go.operators.terminators.EvaluationTerminator;
import eva2.server.go.problems.InterfaceOptimizationProblem;
import eva2.server.go.strategies.ParticleSwarmOptimization;
import eva2.server.modules.GOParameters;
import eva2.server.stat.InterfaceStatisticsListener;

/**
 * 
 * @author Marcel Kronfeld
 * @date 2010-09-01
 */
public class EvA2GUIStarter {

	/**
	 * 
	 */
	private EvAClient evaClient = null;

	/**
	 * 
	 * @param problem
	 * @param parentWindow
	 * @param listener
	 */
	public static void init(InterfaceOptimizationProblem problem,
			final Window parentWindow, InterfaceStatisticsListener listener) {
		EvA2GUIStarter evaBP = new EvA2GUIStarter();
		GOParameters goParams = new GOParameters(); // Instance for the general
													// Genetic Optimization
													// parameterization

		goParams.setOptimizer(new ParticleSwarmOptimization(50, 2.05, 2.05,
				PSOTopologyEnum.grid, 2));
		goParams.setTerminator(new EvaluationTerminator(3000));

		// set the initial EvA problem here
		goParams.setProblem(problem);

		// hide some properties which should not be shown
		GenericObjectEditor.setHideProperty(goParams.getClass(), "problem",
				true);
		GenericObjectEditor.setHideProperty(goParams.getClass(),
				"postProcessParams", true);
		// public EvAClient(final String hostName, final Window parent, final
		// String paramsFile, final InterfaceGOParameters goParams, final
		// boolean autorun, final boolean noSplash, final boolean noGui) {
		evaBP.evaClient = new EvAClient(null, parentWindow, null, goParams,
				false, true, false); // initializes GUI in the background
		// important: wait for GUI initialization before accessing any internal
		// settings:
		evaBP.evaClient.awaitGuiInitialized(); // this returns as soon as the
												// GUI is ready

		// modify initial settings:
		evaBP.evaClient.getStatistics().getStatisticsParameter()
				.setOutputAllFieldsAsText(true); // activate output of all data
													// fields

		evaBP.evaClient.refreshMainPanels(); // GUI update due to the changes
												// made through the API

		evaBP.evaClient.getStatistics().addDataListener(listener); // add a data
																	// listener
																	// instance
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eva2.server.stat.InterfaceStatisticsListener#finalMultiRunResults(java
	 * .lang.String[], java.util.List)
	 */
	public void finalMultiRunResults(String[] header,
			List<Object[]> multiRunFinalObjectData) {
		System.out.println("Last data line of multi runs: "
				+ BeanInspector.toString(multiRunFinalObjectData));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eva2.server.stat.InterfaceStatisticsListener#notifyGenerationPerformed
	 * (java.lang.String[], java.lang.Object[], java.lang.Double[])
	 */
	public void notifyGenerationPerformed(String[] header,
			Object[] statObjects, Double[] statDoubles) {
		// statDoubles only contains double representations of statObject where
		// possible (so its redundant)
		System.out.println("Received data "
				+ BeanInspector.toString(statObjects));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.server.stat.InterfaceStatisticsListener#notifyRunStarted(int,
	 * int, java.lang.String[], java.lang.String[])
	 */
	public void notifyRunStarted(int runNumber, int plannedMultiRuns,
			String[] header, String[] metaInfo) {
		System.out.println("notifyRunStarted, " + runNumber + " of "
				+ plannedMultiRuns);
		System.out.println("Headers: " + BeanInspector.toString(header));
		System.out.println("Meta-info: " + BeanInspector.toString(metaInfo));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.server.stat.InterfaceStatisticsListener#notifyRunStopped(int,
	 * boolean)
	 */
	public void notifyRunStopped(int runsPerformed, boolean completedLastRun) {
		System.out.println("notifyRunStopped, " + runsPerformed
				+ ", last finished normally: " + completedLastRun);
	}
}
