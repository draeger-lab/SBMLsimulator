/**
 * 
 */
package org.sbml.simulator;

import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.JFileChooser;
import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.sbml.simulator.gui.SimulationDialog;
import org.sbml.simulator.math.Distance;
import org.sbml.simulator.math.odes.AbstractDESSolver;
import org.sbml.squeezer.io.SBFileFilter;

import de.zbit.gui.GUITools;
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
	 * @param args
	 * @throws XMLStreamException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException,
			XMLStreamException {
		if (args.length == 1) {
			new SBMLsimulator(args[0]);
		} else {
			new SBMLsimulator();
		}
	}

	/**
	 * 
	 * @param pathname
	 *            path to an SBML model
	 * @throws XMLStreamException
	 * @throws FileNotFoundException
	 */
	public SBMLsimulator(String pathname) throws FileNotFoundException,
			XMLStreamException {
		System.out.println("reading model");
		SBMLDocument doc = SBMLReader.readSBML(pathname);
		System.out.println("starting simulator");
		if ((doc != null) && (doc.isSetModel())) {
			SimulationDialog d = new SimulationDialog(null, doc.getModel());
			d.setVisible(true);
		}
	}

	public SBMLsimulator() {
		JFileChooser chooser = GUITools.createJFileChooser(System
				.getProperty("user.home"), false, false,
				JFileChooser.FILES_ONLY, SBFileFilter.SBML_FILE_FILTER);
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			try {
				SBMLDocument doc = SBMLReader.readSBML(chooser.getSelectedFile());
				if ((doc != null) && (doc.isSetModel())) {
					new SimulationDialog(null, doc.getModel());
				}
			} catch (FileNotFoundException exc) {
				GUITools.showErrorMessage(null, exc);
				exc.printStackTrace();
			} catch (XMLStreamException exc) {
				GUITools.showErrorMessage(null, exc);
				exc.printStackTrace();
			}
		}
	}

}
