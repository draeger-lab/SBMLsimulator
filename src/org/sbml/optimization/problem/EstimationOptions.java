/*
 * $Id:  EstimationOptions.java 16:12:42 draeger$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
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

import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;

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
     * Decide whether or not by default all compartments in a model should be
     * considered the target of an optimization, i.e., value estimation.
     */
    public static final Option<Boolean> EST_ALL_COMPARTMENTS = new Option<Boolean>(
	"EST_ALL_COMPARTMENTS",
	Boolean.class,
	"Decide whether or not by default all compartments in a model should be considered the target of an optimization, i.e., value estimation.",
	Boolean.FALSE);
    /**
     * Decide whether or not by default all global parameters in a model should
     * be considered the target of an optimization, i.e., value estimation.
     */
    public static final Option<Boolean> EST_ALL_GLOBAL_PARAMETERS = new Option<Boolean>(
	"EST_ALL_GLOBAL_PARAMETERS",
	Boolean.class,
	"Decide whether or not by default all global parameters in a model should be considered the target of an optimization, i.e., value estimation.",
	Boolean.TRUE);
    /**
     * Decide whether or not by default all local parameters in a model should
     * be considered the target of an optimization, i.e., value estimation.
     */
    public static final Option<Boolean> EST_ALL_LOCAL_PARAMETERS = new Option<Boolean>(
	"EST_ALL_LOCAL_PARAMETERS",
	Boolean.class,
	"Decide whether or not by default all local parameters in a model should be considered the target of an optimization, i.e., value estimation.",
	Boolean.TRUE);
    /**
     * Decide whether or not by default all species in a model should be
     * considered the target of an optimization, i.e., value estimation.
     */
    public static final Option<Boolean> EST_ALL_SPECIES = new Option<Boolean>(
	"EST_ALL_SPECIES",
	Boolean.class,
	"Decide whether or not by default all species in a model should be considered the target of an optimization, i.e., value estimation.",
	Boolean.FALSE);
    /**
     * The maximal value of the initialization range in a parameter estimation
     * procedure.
     */
    public static final Option<Double> EST_INIT_MAX_VALUE = new Option<Double>(
	"EST_INIT_MAX_VALUE",
	Double.class,
	"The maximal value of the initialization range in a parameter estimation procedure.",
	Double.valueOf(1E1));
    /**
     * The minimal value of the initialization range in a parameter estimation
     * procedure.
     */
    public static final Option<Double> EST_INIT_MIN_VALUE = new Option<Double>(
	"EST_INIT_MIN_VALUE",
	Double.class,
	"The minimal value of the initialization range in a parameter estimation procedure.",
	Double.valueOf(0d));
    /**
     * The maximal value in the absolute allowable range in a parameter
     * estimation procedure.
     */
    public static final Option<Double> EST_MAX_VALUE = new Option<Double>(
	"EST_MAX_VALUE",
	Double.class,
	"The maximal value in the absolute allowable range in a parameter estimation procedure.",
	Double.valueOf(1E3));
    /**
     * The minimal value in the absolute allowable range in a parameter
     * estimation procedure.
     */
    public static final Option<Double> EST_MIN_VALUE = new Option<Double>(
	"EST_MIN_VALUE",
	Double.class,
	"The minimal value in the absolute allowable range in a parameter estimation procedure.",
	Double.valueOf(0d));
    /**
     * Decide whether a model calibration should be done using multiple shoot
     * technique. This should be the default. The other possibility is the
     * so-called single shoot technique. This means that only one initial value
     * is taken to integrate the ordinary differential equation system, whereas
     * the multiple shoot technique restarts the integration in each time step
     * given the values in this step. The aim is then to come as close as
     * possible to the start value in the next time step. In many cases the
     * fitness landscape becomes much more friendly when using a multiple shoot
     * strategy.
     */
    public static final Option<Boolean> EST_MULTI_SHOOT = new Option<Boolean>(
	"EST_MULTI_SHOOT",
	Boolean.class,
	"Decide whether a model calibration should be done using multiple shoot technique. This should be the default. The other possibility is the so-called single shoot technique. This means that only one initial value is taken to integrate the ordinary differential equation system, whereas the multiple shoot technique restarts the integration in each time step given the values in this step. The aim is then to come as close as possible to the start value in the next time step. In many cases the fitness landscape becomes much more friendly when using a multiple shoot strategy.",
	Boolean.TRUE);
}