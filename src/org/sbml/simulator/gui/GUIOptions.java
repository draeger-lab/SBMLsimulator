/**
 * 
 */
package org.sbml.simulator.gui;

import de.zbit.util.prefs.Option;

/**
 * @author draeger
 * @date 2010-10-28
 */
public interface GUIOptions extends de.zbit.gui.GUIOptions {
    /**
     * The minimal value for JSpinners in the GUI.
     */
    public static final Option<Double> SPINNER_MIN_VALUE = new Option<Double>(
	"SPINNER_MIN_VALUE", Double.class,
	"The minimal value for JSpinners in the GUI.", Double.valueOf(0d));
    /**
     * The maximal value for JSpinners in the GUI.
     */
    public static final Option<Double> SPINNER_MAX_VALUE = new Option<Double>(
	"SPINNER_MAX_VALUE", Double.class,
	"The maximal value for JSpinners in the GUI.", Double.valueOf(1E3));
    /**
     * This is important for the graphical user interface as it defines the step
     * size between two values in input masks.
     */
    public static final Option<Double> SPINNER_STEP_SIZE = new Option<Double>(
	"SPINNER_STEP_SIZE",
	Double.class,
	"This is important for the graphical user interface as it defines the step size between two values in input masks.",
	Double.valueOf(.1d));
}
