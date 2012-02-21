/*
 * $Id:  SimulationOptions.java 17:03:54 draeger$
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

import java.util.ResourceBundle;

import org.sbml.simulator.math.EuclideanDistance;
import org.sbml.simulator.math.ManhattanDistance;
import org.sbml.simulator.math.N_Metric;
import org.sbml.simulator.math.RelativeEuclideanDistance;
import org.sbml.simulator.math.RelativeManhattanDistance;
import org.sbml.simulator.math.RelativeSquaredError;
import org.sbml.simulator.math.Relative_N_Metric;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.RungeKutta_EventSolver;

import de.zbit.util.ResourceManager;
import de.zbit.util.ValuePairUncomparable;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
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
   * This specifies the class name of the default distance function that
   * evaluates the quality of a simulation with respect to given (experimental)
   * data.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static final Option<Class> QUALITY_MEASURE = new Option<Class>(
    "QUALITY_MEASURE", Class.class, bundle, new Range<Class>(Class.class,
      SBMLsimulator.getAvailableQualityMeasureClasses()),
    RelativeSquaredError.class);
	
	/**
	 * The default return value of a distance function that can be used if for
	 * some reason a distance cannot be computed.
	 */
  @SuppressWarnings({ "unchecked", "rawtypes"} )
  public static final Option<Double> QUALITY_DEFAULT_VALUE = new Option<Double>(
    "QUALITY_DEFAULT_VALUE", Double.class, bundle, Double.valueOf(1E3),
    new ValuePairUncomparable<Option<Class>, Range<Class>>(
      QUALITY_MEASURE,
      new Range<Class>(Class.class, Relative_N_Metric.class)),
    new ValuePairUncomparable<Option<Class>, Range<Class>>(
      QUALITY_MEASURE,
      new Range<Class>(Class.class, RelativeSquaredError.class)),
    new ValuePairUncomparable<Option<Class>, Range<Class>>(
      QUALITY_MEASURE,
      new Range<Class>(Class.class, RelativeEuclideanDistance.class)),
    new ValuePairUncomparable<Option<Class>, Range<Class>>(
      QUALITY_MEASURE,
      new Range<Class>(Class.class, RelativeManhattanDistance.class))
  );
  
  /**
   * The root parameter in the distance function: in case of the n-norm this is
   * at the same time also the exponent. For instance, the {@link EuclideanDistance}
   * distance has a root value of two, whereas the {@link ManhattanDistance} norm has a
   * root of one. In the {@link RelativeSquaredError}, the default root is also two, but this
   * value may be changed.
   */
	@SuppressWarnings({ "unchecked", "rawtypes" })
  public static final Option<Double> QUALITY_N_METRIC_ROOT = new Option<Double>(
    "QUALITY_N_METRIC_ROOT", Double.class, bundle, Double.valueOf(3d),
    new ValuePairUncomparable<Option<Class>, Range<Class>>(QUALITY_MEASURE,
      new Range<Class>(Class.class, N_Metric.class)),
    new ValuePairUncomparable<Option<Class>, Range<Class>>(QUALITY_MEASURE,
      new Range<Class>(Class.class, RelativeSquaredError.class)),
    new ValuePairUncomparable<Option<Class>, Range<Class>>(QUALITY_MEASURE,
      new Range<Class>(Class.class, Relative_N_Metric.class)));

	/**
	 * Here you can specify how to evaluate the quality of a parameter set with
	 * respect to given experimental data.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static final OptionGroup QUALITY_MEASURES = new OptionGroup(
		"QUALITY_MEASURES", bundle, QUALITY_MEASURE, QUALITY_DEFAULT_VALUE,
		QUALITY_N_METRIC_ROOT);

	/**
	 * This gives the class name of the default solver for ordinary differential
	 * equation systems. The associated class must implement
	 * {@link AbstractDESSolver} and must have a constructor without any
	 * parameters.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
  public static final Option<Class> ODE_SOLVER = new Option<Class>(
    "ODE_SOLVER", Class.class, bundle, new Range<Class>(Class.class,
      SBMLsimulator.getAvailableSolverClasses()), RungeKutta_EventSolver.class);
	
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
  @SuppressWarnings("unchecked")
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
		"SIMULATION_CONFIGURATION", bundle, ODE_SOLVER, SIM_START_TIME,
		SIM_END_TIME, SIM_STEP_SIZE);

}
