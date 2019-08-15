/*
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
package org.sbml.simulator;

import java.util.ResourceBundle;

import org.simulator.math.odes.RungeKutta_EventSolver;

import de.zbit.util.ResourceManager;
import de.zbit.util.objectwrapper.ValuePairUncomparable;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;

/**
 * A collection of user-preferences for the numerical simulation of quantitative
 * systems biology models.
 * 
 * @author Andreas Dr&auml;ger
 * @since 1.0
 * @date 2011-03-23
 */
public interface SimulationOptions extends KeyProvider {

  /**
   * Location of the translation files for multiple language support.
   */
  public static final String BUNDLE_LOCATION = "org.sbml.simulator.locales.Simulator";

  /**
   * The bundle for the user's current language.
   */
  public static final ResourceBundle bundle = ResourceManager
      .getBundle(BUNDLE_LOCATION);

  /**
   * If not specified the value corresponding to this argument will be used to
   * initialize the size of compartments.
   */
  public static final Option<Double> DEFAULT_INIT_COMPARTMENT_SIZE = new Option<Double>(
      "DEFAULT_INIT_COMPARTMENT_SIZE", Double.class, bundle, new Range<Double>(
          Double.class, "{(0.0, 9E9]}"), Double.valueOf(1d));

  /**
   * 
   * If not specified the value corresponding to this argument will be used to
   * initialize species depending on their hasOnlySubstanceUnits property as
   * initial amount or initial concentration.
   */
  public static final Option<Double> DEFAULT_INIT_SPECIES_VALUE = new Option<Double>(
      "DEFAULT_INIT_SPECIES_VALUE", Double.class, bundle, new Range<Double>(
          Double.class, "{(0.0, 9E9]}"), Double.valueOf(1d));

  /**
   * The value that is set for newly created parameters.
   */
  public static final Option<Double> DEFAULT_INIT_PARAMETER_VALUE = new Option<Double>(
      "DEFAULT_INIT_PARAMETER_VALUE", Double.class, bundle, new Range<Double>(
          Double.class, "{(0.0, 9E9]}"), Double.valueOf(1d));

  /**
   * Decide how to treat the values of compartments, species, and parameters if
   * no initial value has been defined in the model.
   */
  @SuppressWarnings("unchecked")
  public static final OptionGroup<Double> DEFAULT_VALUES = new OptionGroup<Double>(
      "DEFAULT_VALUES", bundle, DEFAULT_INIT_COMPARTMENT_SIZE,
      DEFAULT_INIT_SPECIES_VALUE, DEFAULT_INIT_PARAMETER_VALUE);

  /**
   * This gives the class name of the default solver for ordinary differential
   * equation systems. The associated class must implement
   * {@link AbstractDESSolver} and must have a constructor without any
   * parameters.
   */
  @SuppressWarnings({ "rawtypes" })
  public static final Option<Class> ODE_SOLVER = new Option<Class>(
      "ODE_SOLVER", Class.class, bundle, new Range<Class>(Class.class,
          SBMLsimulator.getAvailableSolverClasses()), RungeKutta_EventSolver.class);

  /**
   * 
   */
  @SuppressWarnings("rawtypes")
  public static final ValuePairUncomparable<Option<Class>, Range<Class>> ADAPTIVE_STEP_SIZE_SOLVERS = new ValuePairUncomparable<Option<Class>, Range<Class>>(
      ODE_SOLVER, new Range<Class>(Class.class, org.simulator.math.odes.AdaptiveStepsizeIntegrator.class));

  /**
   * 
   */
  @SuppressWarnings("unchecked")
  public static final Option<Double> ABS_TOL = new Option<Double>("ABS_TOL",
      Double.class, bundle, Double.valueOf(1E-10d), ADAPTIVE_STEP_SIZE_SOLVERS);

  /**
   * 
   */
  @SuppressWarnings("unchecked")
  public static final Option<Double> REL_TOL = new Option<Double>("REL_TOL",
      Double.class, bundle, Double.valueOf(1E-6d), ADAPTIVE_STEP_SIZE_SOLVERS);

  /**
   * With the associated non-negative double number that has to be greater than
   * 0 when simulating SBML models, it is possible to perform a simulation.
   */
  public static final Option<Double> SIM_END_TIME = new Option<Double>(
      "SIM_END_TIME", Double.class, bundle, new Range<Double>(Double.class,
          "{(0, 1E5]}"), Double.valueOf(5d));

  /**
   * The double value associated with this key must, in case of SBML equal to
   * zero. Generally, any start time would be possible. This is why this key
   * exists. But SBML is defined to start its simulation at the time zero.
   */
  public static final Option<Double> SIM_START_TIME = new Option<Double>(
      "SIM_START_TIME", Double.class, bundle, new Range<Double>(Double.class,
          "{[0, 1E5]}"), Double.valueOf(0d));

  /**
   * The greater this value the longer the computation time, but the more
   * accurate will be the result.
   */
  public static final Option<Double> SIM_STEP_SIZE = new Option<Double>(
      "SIM_STEP_SIZE", Double.class, bundle, new Range<Double>(Double.class,
          "{(0, 1E5]}"), Double.valueOf(.01d));

  /**
   * Parameters for the simulation
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static final OptionGroup SIMULATION_CONFIGURATION = new OptionGroup(
    "SIMULATION_CONFIGURATION", bundle, ODE_SOLVER, ABS_TOL, REL_TOL,
    SIM_START_TIME, SIM_END_TIME, SIM_STEP_SIZE);

}
