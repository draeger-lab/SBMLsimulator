/*
 * $Id:  EstimationOptions.java 16:12:42 draeger$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Simulation Core Library, a Java-based library for
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
package org.sbml.optimization.problem;

import java.util.ResourceBundle;

import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.math.EuclideanDistance;
import org.sbml.simulator.math.ManhattanDistance;
import org.sbml.simulator.math.N_Metric;
import org.sbml.simulator.math.RelativeEuclideanDistance;
import org.sbml.simulator.math.RelativeManhattanDistance;
import org.sbml.simulator.math.RelativeSquaredError;
import org.sbml.simulator.math.Relative_N_Metric;

import de.zbit.util.ResourceManager;
import de.zbit.util.objectwrapper.ValuePairUncomparable;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;

/**
 * A collection of {@link Option}s to configure the parameter estimation
 * process.
 * 
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 * @date 2011-03-23
 */
public interface EstimationOptions extends KeyProvider {
	
	/**
	 * Resource bundle
	 */
	static ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");
	
	/**
	 * Decide whether or not by default all compartments in a model should be
	 * considered the target of an optimization, i.e., value estimation.
	 */
	public static final Option<Boolean> EST_ALL_COMPARTMENTS = new Option<Boolean>(
		"EST_ALL_COMPARTMENTS", Boolean.class, bundle, Boolean.FALSE);
	
	/**
	 * Decide whether or not by default all global parameters in a model should be
	 * considered the target of an optimization, i.e., value estimation.
	 */
	public static final Option<Boolean> EST_ALL_GLOBAL_PARAMETERS = new Option<Boolean>(
		"EST_ALL_GLOBAL_PARAMETERS", Boolean.class, bundle, Boolean.TRUE);
	
	/**
	 * Decide whether or not by default all local parameters in a model should be
	 * considered the target of an optimization, i.e., value estimation.
	 */
	public static final Option<Boolean> EST_ALL_LOCAL_PARAMETERS = new Option<Boolean>(
		"EST_ALL_LOCAL_PARAMETERS", Boolean.class, bundle, Boolean.TRUE);
	
	/**
	 * Decide whether or not by default all species in a model should be
	 * considered the target of an optimization, i.e., value estimation.
	 */
	public static final Option<Boolean> EST_ALL_SPECIES = new Option<Boolean>(
		"EST_ALL_SPECIES", Boolean.class, bundle, Boolean.FALSE);
	
	/**
	 * The maximal value of the initialization range in a parameter estimation
	 * procedure.
	 */
	public static final Option<Double> EST_INIT_MAX_VALUE = new Option<Double>(
		"EST_INIT_MAX_VALUE", Double.class, bundle, Double.valueOf(1E1));
	
	/**
	 * The minimal value of the initialization range in a parameter estimation
	 * procedure.
	 */
	public static final Option<Double> EST_INIT_MIN_VALUE = new Option<Double>(
		"EST_INIT_MIN_VALUE", Double.class, bundle, Double.valueOf(0d));
	
	/**
	 * The maximal value in the absolute allowable range in a parameter estimation
	 * procedure.
	 */
	public static final Option<Double> EST_MAX_VALUE = new Option<Double>(
		"EST_MAX_VALUE", Double.class, bundle, Double.valueOf(1E3));
	
	/**
	 * The minimal value in the absolute allowable range in a parameter estimation
	 * procedure.
	 */
	public static final Option<Double> EST_MIN_VALUE = new Option<Double>(
		"EST_MIN_VALUE", Double.class, bundle, Double.valueOf(0d));
	
	/**
	 * Decide whether a model calibration should be done using multiple shoot
	 * technique. This should be the default. The other possibility is the
	 * so-called single shoot technique. This means that only one initial value is
	 * taken to integrate the ordinary differential equation system, whereas the
	 * multiple shoot technique restarts the integration in each time step given
	 * the values in this step. The aim is then to come as close as possible to
	 * the start value in the next time step. In many cases the fitness landscape
	 * becomes much more friendly when using a multiple shoot strategy.
	 */
	public static final Option<Boolean> EST_MULTI_SHOOT = new Option<Boolean>(
		"EST_MULTI_SHOOT", Boolean.class, bundle, Boolean.TRUE);
	
	/**
	 * If this is selected, splines will be calculated from given experimental data
	 * and the parameter estimation procedure will fit the system to the splines instead
	 * of the original values. The advantage of this procedure is that the amount of available
	 * data is increased due to this form of interpolation, also ensuring that the shape of
	 * the resulting curves comes close to what could be expected. The disadvantage is that
	 * the influence of potential outliers on the overall fitness is increased. 
	 */
	public static final Option<Boolean> FIT_TO_SPLINES = new Option<Boolean>(
		"FIT_TO_SPLINES", Boolean.class, bundle, Boolean.FALSE);

	/**
	 * Range that is used to decide whether or not spline fitting is enabled.
	 */
	public static final ValuePairUncomparable<Option<Boolean>, Range<Boolean>> SPLINE_FITTING_SELECTED = new ValuePairUncomparable<Option<Boolean>, Range<Boolean>>(
		FIT_TO_SPLINES, new Range<Boolean>(Boolean.class, Boolean.TRUE));

	/**
	 * This defines the number of additional spline sampling points between the
	 * measurement data. If you select zero, only the real sampling points will be
	 * used.
	 */
	@SuppressWarnings("unchecked")
	public static final Option<Integer> NUMBER_OF_SPLINE_SAMPLES = new Option<Integer>(
		"NUMBER_OF_SPLINE_SAMPLES", Integer.class, bundle, Integer.valueOf(50),
		SPLINE_FITTING_SELECTED);

	/**
	 * These options allow you to estimate parameters with respect to spline
	 * interpolation values between given measurement data and to configure how to
	 * calculate these splines.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static final OptionGroup SPLINE_FITTING = new OptionGroup(
		"SPLINE_FITTING", bundle, FIT_TO_SPLINES, NUMBER_OF_SPLINE_SAMPLES);
	
	/**
   * 
   */
	@SuppressWarnings("unchecked")
	public static final OptionGroup<Boolean> OPTIMIZATION_TARGETS = new OptionGroup<Boolean>(
		"OPTIMIZATION_TARGETS", bundle, EST_ALL_COMPARTMENTS,
		EST_ALL_GLOBAL_PARAMETERS, EST_ALL_LOCAL_PARAMETERS, EST_ALL_SPECIES);
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public static final OptionGroup<Double> OPTIMIZATION_RANGES = new OptionGroup<Double>(
		"OPTIMIZATION_RANGES", bundle, EST_INIT_MIN_VALUE, EST_INIT_MAX_VALUE,
		EST_MIN_VALUE, EST_MAX_VALUE);
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public static final OptionGroup<Boolean> INTEGRATION_STRATEGY = new OptionGroup<Boolean>(
		"INTEGRATION_STRATEGY", bundle, EST_MULTI_SHOOT);
	
	/**
	 * This specifies the class name of the default distance function that
	 * evaluates the quality of a simulation with respect to given (experimental)
	 * data.
	 */
	@SuppressWarnings({ "rawtypes" })
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
	
	
	/*
	 * TODO: Select Optimization algorithm and Termination criterion and SBML-output file
	 */

}
