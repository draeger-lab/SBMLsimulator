package org.sbml.simulator;

import java.io.File;

import org.sbml.simulator.math.Distance;
import org.sbml.simulator.math.RSE;
import org.sbml.simulator.math.odes.AbstractDESSolver;
import org.sbml.simulator.math.odes.RKEventSolver;

import de.zbit.io.SBFileFilter;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.Range;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-10-22
 */
public interface SimulatorOptions extends KeyProvider {
	/**
	 * SBML input file.
	 */
	public static final Option<File> SBML_FILE = new Option<File>("SBML_FILE",
			File.class, "SBML input file", new Range<File>(File.class,
					SBFileFilter.createSBMLFileFilter()), new File(System
					.getProperty("user.dir")));
	/**
	 * When storing any GUI elements or other pictures into a JPEG graphics file
	 * the value associated to this key is used for the degree of compression.
	 * Expected is a float value between 0 and 1.
	 */
	public static final Option<Float> JPEG_COMPRESSION_FACTOR = new Option<Float>(
			"JPEG_COMPRESSION_FACTOR",
			Float.class,
			"When storing any GUI elements or other pictures into a JPEG graphics file the value associated to this key is used for the degree of compression. Expected is a float value between 0 and 1.",
			new Range<Float>(Float.class, "{[0.0,1.0]}"), Float.valueOf(.7f));
	/**
	 * If not specified the value corresponding to this argument will be used to
	 * initialize the size of compartments.
	 */
	public static final Option<Double> OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE = new Option<Double>(
			"OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE",
			Double.class,
			"If not specified the value corresponding to this argument will be used to initialize the size of compartments.",
			new Range<Double>(Double.class, "{(0.0, 9E9]}"), Double.valueOf(1d));
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
	 * This field decides whether or not by default a logarithmic scale should
	 * be applied when plotting values in a two-dimensional figure.
	 */
	public static final Option<Boolean> PLOT_LOG_SCALE = new Option<Boolean>(
			"PLOT_LOG_SCALE",
			Boolean.class,
			" This field decides whether or not by default a logarithmic scale should be applied when plotting values in a two-dimensional figure.",
			Boolean.FALSE);
	/**
	 * With this key it can be specified whether a two-dimensional plot should
	 * display a grid to highlight the position of points and lines.
	 */
	public static final Option<Boolean> PLOT_SHOW_GRID = new Option<Boolean>(
			"PLOT_SHOW_GRID",
			Boolean.class,
			"With this key it can be specified whether a two-dimensional plot should display a grid to highlight the position of points and lines.",
			Boolean.FALSE);
	/**
	 * Determines whether or not a legend should be shown by default when
	 * plotting data into a two-dimensional figure.
	 */
	public static final Option<Boolean> PLOT_SHOW_LEGEND = new Option<Boolean>(
			"PLOT_SHOW_LEGEND",
			Boolean.class,
			"Determines whether or not a legend should be shown by default when plotting data into a two-dimensional figure.",
			Boolean.TRUE);
	/**
	 * Decides whether or not plots should display tool tips next to each curve.
	 */
	public static final Option<Boolean> PLOT_SHOW_TOOLTIPS = new Option<Boolean>(
			"PLOT_SHOW_TOOLTIPS",
			Boolean.class,
			"Decides whether or not plots should display tool tips next to each curve.",
			Boolean.FALSE);
	/**
	 * The default save directory for graphics files as a result of a plot.
	 */
	public static final Option<File> PLOT_SAVE_DIR = new Option<File>(
			"PLOT_SAVE_DIR",
			File.class,
			"The default save directory for graphics files as a result of a plot.",
			new Range<File>(File.class, SBFileFilter.createDirectoryFilter()),
			new File(System.getProperty("user.home")));
	/**
	 * Path to a file with a time series of species/compartment/parameter
	 * values.
	 */
	public static final Option<File> TIME_SERIES_FILE = new Option<File>(
			"TIME_SERIES_FILE",
			File.class,
			"Path to a file with a time series of species/compartment/parameter values.",
			new Range<File>(File.class, SBFileFilter.createCSVFileFilter()),
			new File(System.getProperty("user.home")));
	/**
	 * Boolean argument to specify if SBMLsqueezer should be used as a pure
	 * simulator for a given SBML file. In this case, the simulation control
	 * panel will be displayed when launching SBMLsqeeezer. This is only
	 * possible if the {@link GUI} is set to true and {@link SBML_FILE} is set
	 * to a valid input file.
	 */
	public static final Option<Boolean> SIMULATION_MODE = new Option<Boolean>(
			"SIMULATION_MODE",
			Boolean.class,
			"Argument to specify if SBMLsqueezer should be used as a pure simulator for a given SBML file. In this case, the simulation control panel will be displayed when launching SBMLsqeeezer. This is only possible if the {@link GUI} is set to true and {@link SBML_FILE} is set to a valid input file.",
			Boolean.FALSE);
	/**
	 * This specifies the class name of the default distance function that
	 * evaluates the quality of a simulation with respect to given
	 * (experimental) data.
	 */
	public static final Option<Distance> SIM_DISTANCE_FUNCTION = new Option<Distance>(
			"SIM_DISTANCE_FUNCTION",
			Distance.class,
			"This specifies the class name of the default distance function that evaluates the quality of a simulation with respect to given (experimental) data.",
			new RSE());
	/**
	 * The default return value of a distance function that can be used if for
	 * some reason a distance cannot be computed.
	 */
	public static final Option<Double> SIM_DISTANCE_DEFAULT_VALUE = new Option<Double>(
			"SIM_DISTANCE_DEFAULT_VALUE",
			Double.class,
			"The default return value of a distance function that can be used if for some reason a distance cannot be computed.",
			Double.valueOf(1E3));
	/**
	 * The root parameter in the distance function: in case of the n-norm this
	 * is at the same time also the exponent. For instance, the Eulidean
	 * distance has a root value of two, whereas the Manhattan norm has a root
	 * of one. In the RSE, the default root is also two, but this value may be
	 * changed.
	 */
	public static final Option<Double> SIM_DISTANCE_ROOT = new Option<Double>(
			"SIM_DISTANCE_ROOT",
			Double.class,
			"The root parameter in the distance function: in case of the n-norm this is at the same time also the exponent. For instance, the Eulidean distance has a root value of two, whereas the Manhattan norm has a root of one. In the RSE, the default root is also two, but this value may be changed.",
			Double.valueOf(2d));
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
	 * The maximal value of the initialization range in a parameter estimation
	 * procedure.
	 */
	public static final Option<Double> EST_INIT_MAX_VALUE = new Option<Double>(
			"EST_INIT_MAX_VALUE",
			Double.class,
			"The maximal value of the initialization range in a parameter estimation procedure.",
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
	 * The maximal value in the absolute allowable range in a parameter
	 * estimation procedure.
	 */
	public static final Option<Double> EST_MAX_VALUE = new Option<Double>(
			"EST_MAX_VALUE",
			Double.class,
			"The maximal value in the absolute allowable range in a parameter estimation procedure.",
			Double.valueOf(1E3));
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
	 * Decide whether or not by default all global parameters in a model should
	 * be considered the target of an optimization, i.e., value estimation.
	 */
	public static final Option<Boolean> EST_ALL_GLOBAL_PARAMETERS = new Option<Boolean>(
			"EST_ALL_GLOBAL_PARAMETERS",
			Boolean.class,
			"Decide whether or not by default all global parameters in a model should be considered the target of an optimization, i.e., value estimation.",
			Boolean.TRUE);
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
	 * Decide whether or not by default all species in a model should be
	 * considered the target of an optimization, i.e., value estimation.
	 */
	public static final Option<Boolean> EST_ALL_SPECIES = new Option<Boolean>(
			"EST_ALL_SPECIES",
			Boolean.class,
			"Decide whether or not by default all species in a model should be considered the target of an optimization, i.e., value estimation.",
			Boolean.FALSE);
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
			Boolean.FALSE);
}
