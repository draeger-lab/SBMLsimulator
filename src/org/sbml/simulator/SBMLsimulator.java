/**
 * 
 */
package org.sbml.simulator;

import java.io.File;

import org.sbml.simulator.math.Distance;
import org.sbml.simulator.math.odes.AbstractDESSolver;

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
	public static final String SOLVER_PACKAGE = "org.sbml.simulator.math.des";
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
	 */
	public static void main(String[] args) {
		new SBMLsimulator();
	}

	/**
	 * 
	 */
	public SBMLsimulator() {
	}

}
