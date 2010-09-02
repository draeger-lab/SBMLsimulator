package org.sbml.simulator.gui;

import java.util.List;

import eva2.client.EvAClient;
import eva2.gui.BeanInspector;
import eva2.gui.GenericObjectEditor;
import eva2.server.go.enums.PSOTopologyEnum;
import eva2.server.go.operators.terminators.EvaluationTerminator;
import eva2.server.go.problems.F2Problem;
import eva2.server.go.strategies.ParticleSwarmOptimization;
import eva2.server.modules.GOParameters;
import eva2.server.stat.InterfaceStatisticsListener;

public class EvA2GUIStarter implements InterfaceStatisticsListener {
	EvAClient evaClient = null;

	public static void main(String[] args) {
		EvA2GUIStarter evaBP = new EvA2GUIStarter();
		GOParameters goParams = new GOParameters();
		goParams.setOptimizer(new ParticleSwarmOptimization(50, 2.05, 2.05, PSOTopologyEnum.grid, 2));
		
		// set the initial EvA problem here
		goParams.setProblem(new F2Problem(15));
		 
		goParams.setTerminator(new EvaluationTerminator(300));
		GenericObjectEditor.setHideProperty(goParams.getClass(), "problem", true);
		GenericObjectEditor.setHideProperty(goParams.getClass(), "postProcessParams", true);

		evaBP.evaClient = new EvAClient(null, goParams, false, true, false); // initializes GUI in the background
		// important: wait for GUI initialization before accessing any internal settings:
		evaBP.evaClient.awaitGuiInitialized(); // this returns as soon as the GUI is ready
		
		// modify initial settings:
		evaBP.evaClient.getStatistics().getStatisticsParameter().setOutputAllFieldsAsText(true); // activate output of all data fields
		evaBP.evaClient.refreshMainPanels();
		evaBP.evaClient.getStatistics().addDataListener(evaBP); // add a data listener instance
	}

	public void finalMultiRunResults(String[] header,
			List<Object[]> multiRunFinalObjectData) {
		System.out.println("Last data line of multi runs: " + BeanInspector.toString(multiRunFinalObjectData));
	}

	public void notifyGenerationPerformed(String[] header,
			Object[] statObjects, Double[] statDoubles) {
		// statDoubles only contains double representations of statObject where possible (so its redundant)
		System.out.println("Received data " + BeanInspector.toString(statObjects));
	}
	
	public void notifyRunStarted(int runNumber, int plannedMultiRuns, String[] header, String[] metaInfo) {
		System.out.println("notifyRunStarted, " + runNumber + " of "  + plannedMultiRuns);
		System.out.println("Headers: " + BeanInspector.toString(header));
		System.out.println("Meta-info: " + BeanInspector.toString(metaInfo));
	}

	public void notifyRunStopped(int runsPerformed, boolean completedLastRun) {
		System.out.println("notifyRunStopped, " + runsPerformed + ", last finished normally: "  + completedLastRun);
	}
}
