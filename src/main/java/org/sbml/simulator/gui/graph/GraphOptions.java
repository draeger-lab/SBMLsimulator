/*
 * $Id$
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
package org.sbml.simulator.gui.graph;

import java.awt.Color;
import java.util.ResourceBundle;

import org.sbml.simulator.gui.graph.DynamicControlPanel.Items;
import org.sbml.simulator.gui.graph.DynamicView.Manipulators;

import de.zbit.gui.ColorPalette;
import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;


/**
 * Options for dynamic visualization.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public interface GraphOptions extends KeyProvider {
    /**
     * The bundle for the user's current language.
     */
    public static final ResourceBundle bundle = ResourceManager
            .getBundle("org.sbml.simulator.gui.graph.DynamicGraph");
    
    /**
     * Choose visualization-style.
     */
    public static final Option<String> VISUALIZATION_STYLE = new Option<String>(
            "VISUALIZATION_STYLE", String.class, bundle, new Range<String>(
                    String.class, Manipulators.getAllManipulators()),
            Manipulators.NODESIZE.getName());
    
    /**
     * Switch for node labels.
     */
    public static final Option<Boolean> SHOW_NODE_LABELS = new Option<Boolean>(
        "SHOW_NODE_LABELS", Boolean.class, bundle, Boolean.FALSE);
    
    /**
     * Switch for reaction labels.
     */
    public static final Option<Boolean> SHOW_REACTION_LABELS = new Option<Boolean>(
        "SHOW_REACTION_LABELS", Boolean.class, bundle, Boolean.FALSE);
    
    /**
     * Enable/Disable relative change of concentrations.
     */
    public static final Option<Boolean> RELATIVE_CONCENTRATION_CHANGES = new Option<Boolean>(
        "RELATIVE_CONCENTRATION_CHANGES", Boolean.class, bundle, Boolean.FALSE);
    
    /**
     * Enable/Disable interpolation of added experimental data.
     */
    public static final Option<Boolean> INTERPOLATE_EXP_DATA = new Option<Boolean>(
        "INTERPOLATE_EXP_DATA", Boolean.class, bundle, Boolean.TRUE);

    /**
     * In case of dynamic node size change, it defines the minimum node size.
     */
    public static final Option<Double> MIN_NODE_SIZE = new Option<Double>(
            "MIN_NODE_SIZE", Double.class, bundle, new Range<Double>(
                    Double.class, "{(0, 1E3]}"), Double.valueOf(15d));
    
    /**
     * In case of dynamic node size change, it defines the maximum node size.
     */
    public static final Option<Double> MAX_NODE_SIZE = new Option<Double>(
            "MAX_NODE_SIZE", Double.class, bundle, new Range<Double>(
                    Double.class, "{(0, 1E3]}"), Double.valueOf(50d));
    
    /**
     * Switch for uniform node colors.
     */
    public static final Option<Boolean> USE_UNIFORM_NODE_COLOR = new Option<Boolean>(
        "USE_UNIFORM_NODE_COLOR", Boolean.class, bundle, Boolean.FALSE);
    
    /**
     * Color for uniform node color.
     */
    public static final Option<Color> UNIFORM_NODE_COLOR = new Option<Color>(
        "UNIFORM_NODE_COLOR", Color.class, bundle, ColorPalette.SECOND_292);
    
    /**
     * Color for high concentration.
     */
    public static final Option<Color> COLOR1 = new Option<Color>(
        "COLOR1", Color.class, bundle, ColorPalette.SECOND_180);
    
    /**
     * Color for middle concentration
     */
    public static final Option<Color> COLOR2 = new Option<Color>(
        "COLOR2", Color.class, bundle, Color.WHITE);
    
    /**
     * Color for low concentration
     */
    public static final Option<Color> COLOR3 = new Option<Color>(
        "COLOR3", Color.class, bundle, ColorPalette.SECOND_3015);
    
    /**
     * Node size while color interpolation.
     */
    public static final Option<Double> COLOR_NODE_SIZE = new Option<Double>(
            "COLOR_NODE_SIZE", Double.class, bundle, new Range<Double>(
                    Double.class, "{(0, 1E3]}"), Double.valueOf(30d));
    
    /**
     * Determines the minimum line width of dynamic reaction visualization.
     */
    public static final Option<Double> MIN_LINE_WIDTH = new Option<Double>(
            "MIN_LINE_WIDTH", Double.class, bundle, new Range<Double>(
                    Double.class, "{(0, 1E2]}"), Double.valueOf(1d));
    
    /**
     * Determines the maximum line width of dynamic reaction visualization.
     */
    public static final Option<Double> MAX_LINE_WIDTH = new Option<Double>(
            "MAX_LINE_WIDTH", Double.class, bundle, new Range<Double>(
                    Double.class, "{(0, 1E3]}"), Double.valueOf(6d));
    
    /**
     * Sets the fast simulation speed.
     */
    public static final Option<Double> SIM_SPEED_FAST = new Option<Double>(
            "SIM_SPEED_FAST", Double.class, bundle, new Range<Double>(
                    Double.class, "{[1, 1E3]}"), Double.valueOf(5d));
    
    /**
     * Sets the normal simulation speed.
     */
    public static final Option<Double> SIM_SPEED_NORMAL = new Option<Double>(
            "SIM_SPEED_NORMAL", Double.class, bundle, new Range<Double>(
                    Double.class, "{[1, 1E3]}"), Double.valueOf(25d));
  
    /**
     * Sets the slow simulation speed.
     */
    public static final Option<Double> SIM_SPEED_SLOW = new Option<Double>(
            "SIM_SPEED_SLOW", Double.class, bundle, new Range<Double>(
                    Double.class, "{[1, 1E3]}"), Double.valueOf(80d));
    
    /**
     * Choose simulation speed.
     */
  public static final Option<String> SIM_SPEED_CHOOSER = new Option<String>(
    "SIM_SPEED_CHOOSER", String.class, bundle, new Range<String>(String.class,
      Items.getAllSpeedItems()), Items.NORMAL.getName());
    
//    /**
//     * Sets framerate.
//     */
//    public static final Option<Double> VIDEO_TIMESTAMP = new Option<Double>(
//            "VIDEO_TIMESTAMP", Double.class, bundle, new Range<Double>(
//                    Double.class, "{[1, 1E2]}"), Double.valueOf(25d));
    
    /**
     * Sets step size for image taking.
     */
    public static final Option<Double> VIDEO_IMAGE_STEPSIZE = new Option<Double>(
            "VIDEO_IMAGE_STEPSIZE", Double.class, bundle, new Range<Double>(
                    Double.class, "{[1, 1E2]}"), Double.valueOf(5d));
    
    /**
     * Sets resolution multiplier.
     */
    public static final Option<Double> VIDEO_RESOLUTION_MULTIPLIER = new Option<Double>(
            "VIDEO_RESOLUTION_MULTIPLIER", Double.class, bundle, new Range<Double>(
                    Double.class, "{[1, 1E2]}"), Double.valueOf(3d));
    
    /**
     * Option to force resolution multiplier, even if the output resolution is
     * greater than some threshold.
     */
    public static final Option<Boolean> VIDEO_FORCE_RESOLUTION_MULTIPLIER = new Option<Boolean>(
        "VIDEO_FORCE_RESOLUTION_MULTIPLIER", Boolean.class, bundle, Boolean.FALSE);
    
    /**
     * Option to use the current whole graph to generate videos and images
     * or the current view graph.
     */
    public static final Option<Boolean> VIDEO_DISPLAY_WINDOW = new Option<Boolean>(
            "VIDEO_DISPLAY_WINDOW", Boolean.class, bundle, Boolean.TRUE);
    
    /**
     * Options for visualization.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final OptionGroup GROUP_VISUALIZATION = new OptionGroup(
        "GROUP_VISUALIZATION", bundle, INTERPOLATE_EXP_DATA, RELATIVE_CONCENTRATION_CHANGES, SHOW_NODE_LABELS, SHOW_REACTION_LABELS, VISUALIZATION_STYLE);
    
    /**
     * Settings in case of dynamic change of node size.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final OptionGroup GROUP_NODESIZE = new OptionGroup(
        "GROUP_NODESIZE", bundle, USE_UNIFORM_NODE_COLOR, UNIFORM_NODE_COLOR, MIN_NODE_SIZE, MAX_NODE_SIZE);
    
    /**
     * Settings in case of dynamic change of node color.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final OptionGroup GROUP_NODECOLOR = new OptionGroup(
        "GROUP_NODECOLOR", bundle, COLOR1, COLOR2, COLOR3, COLOR_NODE_SIZE);
    
    /**
     * Settings for dynamic reaction visualization.
     */
    @SuppressWarnings("unchecked")
    public static final OptionGroup<Double> GROUP_REACTION = new OptionGroup<Double>(
        "GROUP_REACTION", bundle, MIN_LINE_WIDTH, MAX_LINE_WIDTH);
    
    /**
     * Options for simulation speed.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final OptionGroup GROUP_SIM_SPEED = new OptionGroup(
        "GROUP_SIM_SPEED", bundle, SIM_SPEED_FAST, SIM_SPEED_NORMAL, SIM_SPEED_SLOW, SIM_SPEED_CHOOSER);
    
    /**
     * Settings for video encoding.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final OptionGroup GROUP_VIDEO_ENCODING = new OptionGroup(
        "GROUP_VIDEO_ENCODING", bundle, VIDEO_DISPLAY_WINDOW, VIDEO_FORCE_RESOLUTION_MULTIPLIER, VIDEO_RESOLUTION_MULTIPLIER, VIDEO_IMAGE_STEPSIZE);
    
}
