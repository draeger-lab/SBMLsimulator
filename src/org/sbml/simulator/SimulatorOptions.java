package org.sbml.simulator;

import de.zbit.gui.CSVKeys;
import de.zbit.gui.GUIKeys;

/**
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-10-22
 */
public interface SimulatorOptions extends CSVKeys, GUIKeys {
	/**
	 * SBML input file.
	 */
	public static final Option<File> SBML_FILE = new Option<File>("SBML_FILE", File.class, "SBML input file", new File(System.getProperty("user.dir")));
	/**
	 * When storing any GUI elements or other pictures into a JPEG graphics file
	 * the value associated to this key is used for the degree of compression.
	 * Expected is a float value between 0 and 1.
	 */
	public static final String JPEG_COMPRESSION_FACTOR = "JPEG_COMPRESSION_FACTOR";
	/**
	 * If not specified the value corresponding to this argument will be used to
	 * initialize the size of compartments.
	 */
	public static final String OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE = "OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE";
	/**
	 * If not specified the value corresponding to this argument will be used to
	 * initialize species depending on their hasOnlySubstanceUnits property as
	 * initial amount or initial concentration.
	 */
	public static final String OPT_DEFAULT_SPECIES_INITIAL_VALUE = "OPT_DEFAULT_SPECIES_INITIAL_VALUE";
	/**
	 * The value that is set for newly created parameters.
	 */
	public static final String OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS = "OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS";
	/**
	 * This field decides whether or not by default a logarithmic scale should
	 * be applied when plotting values in a two-dimensional figure.
	 */
	public static final String PLOT_LOG_SCALE = "PLOT_LOG_SCALE";
	/**
	 * With this key it can be specified whether a two-dimensional plot should
	 * display a grid to highlight the position of points and lines.
	 */
	public static final String PLOT_SHOW_GRID = "PLOT_SHOW_GRID";
	/**
	 * Determines whether or not a legend should be shown by default when
	 * plotting data into a two-dimensional figure.
	 */
	public static final String PLOT_SHOW_LEGEND = "PLOT_SHOW_LEGEND";
	/**
	 * Decides whether or not plots should display tooltips next to each curve.
	 */
	public static final String PLOT_SHOW_TOOLTIPS = "PLOT_SHOW_TOOLTIPS";
	/**
	 * The default save directory for graphics files as a result of a plot.
	 */
	public static final String PLOT_SAVE_DIR = "PLOT_SAVE_DIR";
	/**
	 * Path to a file with a time series of species/compartment/parameter
	 * values.
	 */
	public static final String TIME_SERIES_FILE = "TIME_SERIES_FILE";
	/**
	 * Boolean argument to specify if SBMLsqueezer should be used as a pure
	 * simulator for a given SBML file. In this case, the simulation control
	 * panel will be displayed when launching SBMLsqeeezer. This is only
	 * possible if the {@link GUI} is set to true and {@link SBML_FILE} is set
	 * to a valid input file.
	 */
	public static final String SIMULATION_MODE = "SIMULATION_MODE";
	/**
	 * This specifies the class name of the default distance function that
	 * evaluates the quality of a simulation with respect to given
	 * (experimental) data.
	 */
	public static final String SIM_DISTANCE_FUNCTION = "SIM_DISTANCE_FUNCTION";
	/**
	 * The default return value of a distance function that can be used if for
	 * some reason a distance cannot be computed.
	 */
	public static final String SIM_DISTANCE_DEFAULT_VALUE = "SIM_DISTANCE_DEFAULT_VALUE";
	/**
	 * The root parameter in the distance function: in case of the n-norm this
	 * is at the same time also the exponent. For instance, the Eulidean
	 * distance has a root value of two, whereas the Manhattan norm has a root
	 * of one. In the RSE, the default root is also two, but this value may be
	 * changed.
	 */
	public static final String SIM_DISTANCE_ROOT = "SIM_DISTANCE_ROOT";
	/**
	 * With the associated non-negative double number that has to be greater
	 * than 0 when simulating SBML models, it is possible to perform a
	 * simulation.
	 */
	public static final String SIM_END_TIME = "SIM_END_TIME";
	/**
	 * This is important for the graphical user interface as it defines the
	 * maximal possible value for compartiments within the input mask. Expected
	 * is a positive double value.
	 */
	public static final String SIM_MAX_COMPARTMENT_SIZE = "SIM_MAX_COMPARTMENT_SIZE";
	/**
	 * This is important for the graphical user interface as it defines the
	 * maximal possible value for parameters within the input mask. Expected is
	 * a positive double value.
	 */
	public static final String SIM_MAX_PARAMETER_VALUE = "SIM_MAX_PARAMETER_VALUE";
	/**
	 * This is important for the graphical user interface as it defines the
	 * maximal possible value for species within the input mask. Expected is a
	 * positive double value.
	 */
	public static final String SIM_MAX_SPECIES_VALUE = "SIM_MAX_SPECIES_VALUE";
	/**
	 * This key tells the graphical user interface the upper bound for the input
	 * mask of how many time steps per unit time can maximally be performed when
	 * simulating a model.
	 */
	public static final String SIM_MAX_STEPS_PER_UNIT_TIME = "SIM_MAX_STEPS_PER_UNIT_TIME";
	/**
	 * This is important for the graphical user interface as it defines the
	 * upper bound for the input mask for the simulation time.
	 */
	public static final String SIM_MAX_TIME = "SIM_MAX_TIME";
	/**
	 * This gives the class name of the default solver for ordinary differential
	 * equation systems. The associated class must implement
	 * {@link eva2.tools.math.des.AbstractDESSolver} and must have a constructor
	 * without any parameters.
	 */
	public static final String SIM_ODE_SOLVER = "SIM_ODE_SOLVER";
	/**
	 * The double value associated with this key must, in case of SBML equal to
	 * zero. Generally, any start time would be possible. This is why this key
	 * exists. But SBML is defined to start its simulation at the time zero.
	 */
	public static final String SIM_START_TIME = "SIM_START_TIME";
	/**
	 * The greater this value the longer the computation time, but the more
	 * accurate will be the result.
	 */
	public static final String SIM_STEP_SIZE = "SIM_STEP_SIZE";
	/**
	 * The minimal value of the initialization range in a parameter estimation
	 * procedure.
	 */
	public static final String EST_INIT_MIN_VALUE = "EST_INIT_MIN_VALUE";
	/**
	 * The maximal value of the initialization range in a parameter estimation
	 * procedure.
	 */
	public static final String EST_INIT_MAX_VALUE = "EST_INIT_MAX_VALUE";
	/**
	 * The minimal value in the absolute allowable range in a parameter
	 * estimation procedure.
	 */
	public static final String EST_MIN_VALUE = "EST_MIN_VALUE";
	/**
	 * The maximal value in the absolute allowable range in a parameter
	 * estimation procedure.
	 */
	public static final String EST_MAX_VALUE = "EST_MAX_VALUE";
	/**
	 * Boolean field to decide whether or not by default all local parameters in
	 * a model should be considered the target of an optimization, i.e., value
	 * estimation.
	 */
	public static final String EST_ALL_LOCAL_PARAMETERS = "EST_ALL_LOCAL_PARAMETERS";
	/**
	 * Boolean field to decide whether or not by default all global parameters
	 * in a model should be considered the target of an optimization, i.e.,
	 * value estimation.
	 */
	public static final String EST_ALL_GLOBAL_PARAMETERS = "EST_ALL_GLOBAL_PARAMETERS";
	/**
	 * Boolean field to decide whether or not by default all compartments in a
	 * model should be considered the target of an optimization, i.e., value
	 * estimation.
	 */
	public static final String EST_ALL_COMPARTMENTS = "EST_ALL_COMPARTMENTS";
	/**
	 * Boolean field to decide whether or not by default all species in a model
	 * should be considered the target of an optimization, i.e., value
	 * estimation.
	 */
	public static final String EST_ALL_SPECIES = "EST_ALL_SPECIES";
	/**
	 * Boolean field to decide whether a model calibration should be done using
	 * multiple shoot technique. This should be the default. The other
	 * possibility is the so-called single shoot technique. This means that only
	 * one initial value is taken to integrate the ordinary differential
	 * equation system, whereas the multiple shoot technique restarts the
	 * integration in each time step given the values in this step. The aim is
	 * then to come as close as possible to the start value in the next time
	 * step. In many cases the fitness landscape becomes much more friendly when
	 * using a multiple shoot strategy.
	 */
	public static final String EST_MULTI_SHOOT = "EST_MULTI_SHOOT";
}
