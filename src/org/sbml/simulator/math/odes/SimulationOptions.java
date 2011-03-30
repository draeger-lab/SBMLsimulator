/*
 * $Id:  SimulationOptions.java 17:03:54 draeger$
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
package org.sbml.simulator.math.odes;

import java.util.ResourceBundle;

import org.sbml.simulator.math.QualityMeasure;
import org.sbml.simulator.math.RSE;

import de.zbit.util.ResourceManager;
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
    public static final ResourceBundle bundle = ResourceManager.getBundle(BUNDLE_LOCATION);
    
    /**
     * If not specified the value corresponding to this argument will be used to
     * initialize the size of compartments.
     */
    public static final Option<Double> OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE = new Option<Double>(
	"OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE", Double.class, bundle
		.getString("SIM:0000001"), new Range<Double>(Double.class,
	    "{(0.0, 9E9]}"), Double.valueOf(1d));
    /**
     * If not specified the value corresponding to this argument will be used to
     * initialize species depending on their hasOnlySubstanceUnits property as
     * initial amount or initial concentration.
     */
    public static final Option<Double> OPT_DEFAULT_SPECIES_INITIAL_VALUE = new Option<Double>(
	"OPT_DEFAULT_SPECIES_INITIAL_VALUE",
	Double.class,
	" If not specified the value corresponding to this argument will be used to initialize species depending on their hasOnlySubstanceUnits property as initial amount or initial concentration.",
	new Range<Double>(Double.class, "{(0.0, 9E9]}"), Double.valueOf(1d));
    /**
     * The value that is set for newly created parameters.
     */
    public static final Option<Double> OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS = new Option<Double>(
	"OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS", Double.class,
	"The value that is set for newly created parameters.",
	new Range<Double>(Double.class, "{(0.0, 9E9]}"), Double.valueOf(1d));

    /**
     * Decide how to treat the values of compartments, species, and parameters
     * if no initial value has been defined in the model.
     */
    @SuppressWarnings("unchecked")
    public static final OptionGroup<Double> DEFAULT_VALUES = new OptionGroup<Double>(
	"Missing values",
	"Decide how to treat the values of compartments, species, and parameters if no initial value has been defined in the model.",
	OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE,
	OPT_DEFAULT_SPECIES_INITIAL_VALUE, OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS);  
    
    /**
     * The default return value of a distance function that can be used if for
     * some reason a distance cannot be computed.
     */
    public static final Option<Double> SIM_QUALITY_DEFAULT_VALUE = new Option<Double>(
	"SIM_QUALITY_DEFAULT_VALUE",
	Double.class,
	"The default return value of a quality function that can be used if for some reason a quality cannot be computed.",
	Double.valueOf(1E3));
    
    /**
     * This specifies the class name of the default distance function that
     * evaluates the quality of a simulation with respect to given
     * (experimental) data.
     */
    public static final Option<QualityMeasure> SIM_QUALITY_FUNCTION = new Option<QualityMeasure>(
	"SIM_QUALITY_FUNCTION",
	QualityMeasure.class,
	"This specifies the class name of the default quality function that evaluates the quality of a simulation with respect to given (experimental) data.",
	new RSE());
    
    /**
     * The root parameter in the distance function: in case of the n-norm this
     * is at the same time also the exponent. For instance, the Eulidean
     * distance has a root value of two, whereas the Manhattan norm has a root
     * of one. In the RSE, the default root is also two, but this value may be
     * changed.
     */
    public static final Option<Double> SIM_QUALITY_N_METRIC_ROOT = new Option<Double>(
	"SIM_QUALITY_N_METRIC_ROOT",
	Double.class,
	"The root parameter in the distance function for n-metrics: in case of the n-norm this is at the same time also the exponent. For instance, the Eulidean distance has a root value of two, whereas the Manhattan norm has a root of one. In the RSE, the default root is also two, but this value may be changed.",
	Double.valueOf(3d));
        
    /**
     * With the associated non-negative double number that has to be greater
     * than 0 when simulating SBML models, it is possible to perform a
     * simulation.
     */
    public static final Option<Double> SIM_END_TIME = new Option<Double>(
	"SIM_END_TIME",
	Double.class,
	"With the associated non-negative double number that has to be greater than 0 when simulating SBML models, it is possible to perform a simulation.",
	Double.valueOf(5d));
    
    /**
     * 
     */
    @SuppressWarnings("unchecked")
    public static final OptionGroup MODEL_QUALITY = new OptionGroup(bundle
	    .getString("SIM:0000010"), bundle.getString("SIM:0000011"),
	SIM_QUALITY_FUNCTION, SIM_QUALITY_DEFAULT_VALUE,
	SIM_QUALITY_N_METRIC_ROOT);
    
    /**
     * This is important for the graphical user interface as it defines the
     * maximal possible value for compartments within the input mask. Expected
     * is a positive double value.
     */
    public static final Option<Double> SIM_MAX_COMPARTMENT_SIZE = new Option<Double>(
	"SIM_MAX_COMPARTMENT_SIZE",
	Double.class,
	" This is important for the graphical user interface as it defines the maximal possible value for compartments within the input mask. Expected is a positive double value.",
	Double.valueOf(1E3));
    /**
     * This is important for the graphical user interface as it defines the
     * maximal possible value for parameters within the input mask. Expected is
     * a positive double value.
     */
    public static final Option<Double> SIM_MAX_PARAMETER_VALUE = new Option<Double>(
	"SIM_MAX_PARAMETER_VALUE",
	Double.class,
	"This is important for the graphical user interface as it defines the maximal possible value for parameters within the input mask. Expected is a positive double value.",
	Double.valueOf(1E3));
    /**
     * This is important for the graphical user interface as it defines the
     * maximal possible value for species within the input mask. Expected is a
     * positive double value.
     */
    public static final Option<Double> SIM_MAX_SPECIES_VALUE = new Option<Double>(
	"SIM_MAX_SPECIES_VALUE",
	Double.class,
	"This is important for the graphical user interface as it defines the maximal possible value for species within the input mask. Expected is a positive double value.",
	Double.valueOf(1E3));
    /**
     * This key tells the graphical user interface the upper bound for the input
     * mask of how many time steps per unit time can maximally be performed when
     * simulating a model.
     */
    public static final Option<Integer> SIM_MAX_STEPS_PER_UNIT_TIME = new Option<Integer>(
	"SIM_MAX_STEPS_PER_UNIT_TIME",
	Integer.class,
	"This key tells the graphical user interface the upper bound for the input mask of how many time steps per unit time can maximally be performed when simulating a model.",
	Integer.valueOf(100));
    /**
     * This is important for the graphical user interface as it defines the
     * upper bound for the input mask for the simulation time.
     */
    public static final Option<Double> SIM_MAX_TIME = new Option<Double>(
	"SIM_MAX_TIME",
	Double.class,
	"This is important for the graphical user interface as it defines the upper bound for the input mask for the simulation time.",
	Double.valueOf(1E3));
    /**
     * This gives the class name of the default solver for ordinary differential
     * equation systems. The associated class must implement
     * {@link AbstractDESSolver} and must have a constructor without any
     * parameters.
     */
    public static final Option<AbstractDESSolver> SIM_ODE_SOLVER = new Option<AbstractDESSolver>(
	"SIM_ODE_SOLVER",
	AbstractDESSolver.class,
	"This gives the class name of the default solver for ordinary differential equation systems. The associated class must implement AbstractDESSolver and must have a constructor without any parameters.",
	new RKEventSolver());
    /**
     * The double value associated with this key must, in case of SBML equal to
     * zero. Generally, any start time would be possible. This is why this key
     * exists. But SBML is defined to start its simulation at the time zero.
     */
    public static final Option<Double> SIM_START_TIME = new Option<Double>(
	"SIM_START_TIME",
	Double.class,
	"The double value associated with this key must, in case of SBML equal to zero. Generally, any start time would be possible. This is why this key exists. But SBML is defined to start its simulation at the time zero.",
	Double.valueOf(0d));
    /**
     * The greater this value the longer the computation time, but the more
     * accurate will be the result.
     */
    public static final Option<Double> SIM_STEP_SIZE = new Option<Double>(
	"SIM_STEP_SIZE",
	Double.class,
	"The greater this value the longer the computation time, but the more accurate will be the result.",
	Double.valueOf(.01));

}
