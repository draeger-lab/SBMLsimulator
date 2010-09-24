/**
 * 
 */
package org.sbml.simulator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.prefs.BackingStoreException;

import javax.xml.stream.XMLStreamException;

import org.sbml.simulator.gui.SimulatorUI;
import org.sbml.simulator.math.Distance;
import org.sbml.simulator.math.odes.AbstractDESSolver;
import org.sbml.squeezer.CfgKeys;

import de.zbit.util.Reflect;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-01
 */
public class SBMLsimulator {
	/**
	 * The possible location of this class in a jar file if used in plug-in
	 * mode.
	 */
	public static final String JAR_LOCATION = "plugin" + File.separatorChar;
	/**
	 * The package where all ODE solvers are assumed to be located.
	 */
	public static final String SOLVER_PACKAGE = "org.sbml.simulator.math.odes";
	/**
	 * The package where all mathematical functions, in particular distance
	 * functions, are located.
	 */
	public static final String MATH_PACKAGE = "org.sbml.simulator.math";
	/**
	 * An array of all available ordinary differential equation solvers.
	 */
	private static final Class<AbstractDESSolver> AVAILABLE_SOLVERS[] = Reflect
			.getAllClassesInPackage(SOLVER_PACKAGE, true, true,
					AbstractDESSolver.class, JAR_LOCATION, true);

	/**
	 * An array of all available implementations of distance functions to judge
	 * the quality of a simulation based on parameter and initial value
	 * settings.
	 */
	private static final Class<Distance> AVAILABLE_DISTANCES[] = Reflect
			.getAllClassesInPackage(MATH_PACKAGE, true, true, Distance.class,
					JAR_LOCATION, true);

	/**
	 * The version number of this program.
	 */
	private static final String VERSION_NUMBER = "0.5";

	static {
		CfgKeys.setCommentCfgFile(String.format(
				"SBMLsimulator %s configuration. Do not change manually.",
				VERSION_NUMBER));
		CfgKeys
				.setDefaultsCfgFile("/org/sbml/simulator/resources/cfg/Configuration.xml");
		CfgKeys.setUserPrefNode("/org/sbml/simulator");
		try {
			CfgKeys.initProperties();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @return
	 */
	public static final Class<Distance>[] getAvailableDistances() {
		return AVAILABLE_DISTANCES;
	}

	/**
	 * 
	 * @return
	 */
	public static Class<AbstractDESSolver>[] getAvailableSolvers() {
		return AVAILABLE_SOLVERS;
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
		Properties p = CfgKeys.analyzeCommandLineArguments(args);
		String openFile = null;
		if (p.containsKey(CfgKeys.SBML_FILE)) {
			openFile = p.get(CfgKeys.SBML_FILE).toString();
		}
		String timeSeriesFile = null;
		if (p.containsKey(CfgKeys.TIME_SERIES_FILE)) {
			timeSeriesFile = p.get(CfgKeys.TIME_SERIES_FILE).toString();
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
	 * 
	 * @param pathname
	 *            path to an SBML model
	 */
	public SBMLsimulator(String pathname) {
		this(pathname, null);
	}

	/**
	 * 
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
}
