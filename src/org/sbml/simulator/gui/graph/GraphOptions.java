/*
 * $Id$
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
package org.sbml.simulator.gui.graph;

import java.awt.Color;
import java.util.ResourceBundle;

import de.zbit.gui.ColorPalette;
import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;


/**
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public interface GraphOptions extends KeyProvider{
    /**
     * The bundle for the user's current language.
     */
    public static final ResourceBundle bundle = ResourceManager
            .getBundle("org.sbml.simulator.gui.graph.DynamicGraph");

    /**
     * In case of dynamic node size change, it defines the minimum node size.
     */
    public static final Option<Double> MIN_NODE_SIZE = new Option<Double>(
            "MIN_NODE_SIZE", Double.class, bundle, new Range<Double>(
                    Double.class, "{(0, 1E3]}"), Double.valueOf(8d));
    
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
     * Settings in case of dynamic change of node size.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final OptionGroup NODESIZE_GROUP = new OptionGroup(
        "NODESIZE_GROUP", bundle, USE_UNIFORM_NODE_COLOR, UNIFORM_NODE_COLOR, MIN_NODE_SIZE, MAX_NODE_SIZE);
    
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
     * Settings in case of dynamic change of node color.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final OptionGroup NODECOLOR_GROUP = new OptionGroup(
        "NODECOLOR_GROUP", bundle, COLOR1, COLOR2, COLOR3, COLOR_NODE_SIZE);
    
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
     * Settings for dynamic reaction visualization.
     */
    @SuppressWarnings("unchecked")
    public static final OptionGroup<Double> REACTION_GROUP = new OptionGroup<Double>(
        "REACTION_GROUP", bundle, MIN_LINE_WIDTH, MAX_LINE_WIDTH);
    
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
     * Labeling switches.
     */
    @SuppressWarnings("unchecked")
    public static final OptionGroup<Boolean> GRAPH_LABELS = new OptionGroup<Boolean>(
        "GRAPH_LABELS", bundle, SHOW_NODE_LABELS, SHOW_REACTION_LABELS);
    
    /**
     * Sets the fast simulation speed.
     */
    public static final Option<Double> SIM_SPEED_FAST = new Option<Double>(
            "SIM_SPEED_FAST", Double.class, bundle, new Range<Double>(
                    Double.class, "{[1, 1E3]}"), Double.valueOf(5d));
    
    //TODO integer options?
    
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
     * Options for simulation speed.
     */
    @SuppressWarnings("unchecked")
    public static final OptionGroup<Double> SIM_SPEED_GROUP = new OptionGroup<Double>(
        "SIM_SPEED_GROUP", bundle, SIM_SPEED_FAST, SIM_SPEED_NORMAL, SIM_SPEED_SLOW);
    
    //TODO options for graphmanipulator
}
