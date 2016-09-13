/*
 * $Id:  FBAProperties.java 15:29:01 Meike Aichele$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2016 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.fba.gui;

import java.io.File;
import java.util.ResourceBundle;

import org.sbml.simulator.fba.controller.FluxBalanceAnalysis;
import org.sbml.simulator.fba.controller.TargetFunction;

import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 17.07.2012
 * @since 1.0
 */
public interface FBAOptions extends KeyProvider {

	/**
	 * Contains the choice for the target function in {@link FluxBalanceAnalysis}
	 * 
	 * @author Meike Aichele
	 * @version $Rev$
	 * @date 17.07.2012
	 * @since 1.0
	 */
	public static enum Manipulators {
		;

		/**
		 * @return all target functions
		 */
		public static String[] getAllManipulators() {
			return new String[] { "Flux Minimization" , "Biomass Optimization"};
		}
	}


	/**
	 * The bundle for the user's current language.
	 */
	public static final ResourceBundle bundle = ResourceManager
	.getBundle("org.sbml.simulator.locales.Simulator");


	/**
	 * Choose to activate the constraint J*G < 0
	 */
	public static final Option<Boolean> ACTIVATE_CONSTRAINT_JG_LESS_THAN_0 = new Option<Boolean>(
			"ACTIVATE_CONSTRAINT_JG_LESS_THAN_0", Boolean.class, bundle, Boolean.TRUE);


	/**
	 * Choose to activate the constraint |J|-r_max*|G| < 0
	 */
	public static final Option<Boolean> ACTIVATE_CONSTRAINT_J_R_MAX_G_LESS_THAN_0 = new Option<Boolean>(
			"ACTIVATE_CONSTRAINT_J_R_MAX_G_LESS_THAN_0", Boolean.class, bundle, Boolean.TRUE);


	/**
	 * Choose to activate the constraint J>0
	 */
	public static final Option<Boolean> ACTIVATE_CONSTRAINT_J_GREATER_THAN_0 = new Option<Boolean>(
			"ACTIVATE_CONSTRAINT_J_GREATER_THAN_0", Boolean.class, bundle, Boolean.TRUE);
	
	/**
	 * Choose to activate the constraint to compute the errors
	 */
	public static final Option<Boolean> ACTIVATE_CONSTRAINT_ERROR = new Option<Boolean>(
			"ACTIVATE_CONSTRAINT_ERROR", Boolean.class, bundle, Boolean.TRUE);

	
	/**
	 * Choose a target function
	 */
	public static final Option<String> DEFAULT_TARGET_FUNCTION = new Option<String>(
			"DEFAULT_TARGET_FUNCTION", String.class, bundle, new Range<String>(String.class, Manipulators.getAllManipulators()), "Flux Minimization");
	
	/**
	 * Change lambda1 to weight the concentrations in the target function
	 */
	public static final Option<Double> LAMBDA1 = new Option<Double>(
			"LAMBDA1", Double.class, bundle, Double.valueOf(TargetFunction.lambda1));
		
	/**
	 * Change lambda2 to weight the errors in the target function
	 */
	public static final Option<Double> LAMBDA2 = new Option<Double>(
			"LAMBDA2", Double.class, bundle, Double.valueOf(TargetFunction.lambda2));
		
	/**
	 * Change lambda3 to weight the L-Matrix in the target function
	 */
	public static final Option<Double> LAMBDA3 = new Option<Double>(
			"LAMBDA3", Double.class, bundle, Double.valueOf(TargetFunction.lambda3));
	
	/**
	 * Change lambda4 to weight the Gibbs energies in the target function
	 */
	public static final Option<Double> LAMBDA4 = new Option<Double>(
			"LAMBDA4", Double.class, bundle, Double.valueOf(TargetFunction.lambda4));
		
	/**
	 * Load a concentration file
	 */
	public static final Option<File> LOAD_CONCENTRATION_FILE = new Option<File>(
			"LOAD_CONCENTRATION_FILE", File.class, bundle, new Range<File>(File.class,
					SBFileFilter.createCSVFileFilter()), new File(
					System.getProperty("user.dir")));


	/**
	 * Load a Gibbs file
	 */
	public static final Option<File> LOAD_GIBBS_FILE = new Option<File>(
			"LOAD_GIBBS_FILE", File.class, bundle, new Range<File>(File.class,
					SBFileFilter.createCSVFileFilter()), new File(
					System.getProperty("user.dir")));


	/**
	 * Load a System Boundaries file
	 */
	public static final Option<File> LOAD_SYSTEM_BOUNDARIES_FILE = new Option<File>(
			"LOAD_SYSTEM_BOUNDARIES_FILE", File.class, bundle, new Range<File>(File.class,
					SBFileFilter.createTextFileFilter()), new File(
					System.getProperty("user.dir")));


	/**
	 * Group to active some constraints
	 */
	@SuppressWarnings("unchecked")
	public static final OptionGroup<Boolean> SET_CONSTRAINTS = new OptionGroup<Boolean>(
			"SET_CONSTRAINTS", bundle, ACTIVATE_CONSTRAINT_JG_LESS_THAN_0, ACTIVATE_CONSTRAINT_J_R_MAX_G_LESS_THAN_0, ACTIVATE_CONSTRAINT_J_GREATER_THAN_0, ACTIVATE_CONSTRAINT_ERROR);


	/**
	 * Set the iterations for the CPLEX algorithm
	 */
	public static final Option<Integer> SET_ITERATIONS = new Option<Integer>(
			"SET_ITERATIONS", Integer.class, bundle, new Range<Integer>(
	                Integer.class, "{(1, 100000]}"), Integer.valueOf(600));


	/**
	 * Group to load files
	 */
	@SuppressWarnings("unchecked")
	public static final OptionGroup<File> SET_FILES_GROUP = new OptionGroup<File>(
			"SET_FILES_GROUP", bundle, LOAD_GIBBS_FILE, LOAD_CONCENTRATION_FILE, LOAD_SYSTEM_BOUNDARIES_FILE);


	/**
	 * Group of lambdas
	 */
	@SuppressWarnings("unchecked")
	public static final OptionGroup<Double> SET_LAMBDAS = new OptionGroup<Double>(
			"SET_LAMBDAS", bundle, LAMBDA1,
			LAMBDA2, LAMBDA3, LAMBDA4);
		
	/**
	 * Group to set the target function and the iterations
	 */
	@SuppressWarnings("unchecked")
	public static final OptionGroup<?> SET_TARGET_AND_ITERATIONS = new OptionGroup<Object>(
			"SET_TARGET_AND_ITERATIONS", bundle, DEFAULT_TARGET_FUNCTION, SET_ITERATIONS);

	
}
