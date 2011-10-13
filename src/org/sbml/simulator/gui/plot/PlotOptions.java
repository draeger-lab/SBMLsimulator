/*
 * $Id:  PlotOptions.java 17:29:23 draeger$
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
package org.sbml.simulator.gui.plot;

import java.awt.Color;
import java.io.File;
import java.util.ResourceBundle;

import de.zbit.io.SBFileFilter;
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
public interface PlotOptions extends KeyProvider {
    
    /**
     * The resource where to obtain the names of all the options
     */
    public static ResourceBundle bundle = ResourceManager
	    .getBundle("org.sbml.simulator.locales.Simulator");
    
    /**
     * 
     */
    public static final Option<Color> PLOT_BACKGROUND_COLOR = new Option<Color>(
    		"PLOT_BACKGROUND_COLOR", Color.class, "The background color of the plot", Color.WHITE);
    /**
     * 
     */
    public static final Option<Color> PLOT_GRID_COLOR = new Option<Color>(
    		"PLOT_GRID_COLOR", Color.class, "The color of the plot's grid", Color.DARK_GRAY);
    /**
     * 
     */
    public static final Option<String> PLOT_TITLE = new Option<String>(
    		"PLOT_TITLE", String.class, "Set the title of the plot", "");
    

//    /**
//     * When storing any GUI elements or other pictures into a JPEG graphics file
//     * the value associated to this key is used for the degree of compression.
//     * Expected is a float value between 0 and 1.
//     */
//    public static final Option<Float> JPEG_COMPRESSION_FACTOR = new Option<Float>(
//	"JPEG_COMPRESSION_FACTOR",
//	Float.class,
//	"When storing any GUI elements or other pictures into a JPEG graphics file the value associated to this key is used for the degree of compression. Expected is a float value between 0 and 1.",
//	new Range<Float>(Float.class, "{[0.0,1.0]}"), Float.valueOf(.7f));
//    /**
//     * This field decides whether or not by default a logarithmic scale should
//     * be applied when plotting values in a two-dimensional figure.
//     */
//    public static final Option<Boolean> PLOT_LOG_SCALE = new Option<Boolean>(
//	"PLOT_LOG_SCALE", Boolean.class, bundle.getString("SIM:0000006"),
//	Boolean.FALSE, bundle.getString("SIM:0000002"));
//    /**
//     * The default save directory for graphics files as a result of a plot.
//     */
//    public static final Option<File> PLOT_SAVE_DIR = new Option<File>(
//	"PLOT_SAVE_DIR", File.class,
//	"The default save directory for graphics files as a result of a plot.",
//	new Range<File>(File.class, SBFileFilter.createDirectoryFilter()),
//	new File(System.getProperty("user.home")));
    
    
    /**
     * With this key it can be specified whether a two-dimensional plot should
     * display a grid to highlight the position of points and lines.
     */
    public static final Option<Boolean> SHOW_PLOT_GRID = new Option<Boolean>(
	"SHOW_PLOT_GRID", Boolean.class, bundle.getString("SIM:0000007"),
	Boolean.FALSE, bundle.getString("SIM:0000003"));

    /**
     * Determines whether or not a legend should be shown by default when
     * plotting data into a two-dimensional figure.
     */
    public static final Option<Boolean> SHOW_PLOT_LEGEND = new Option<Boolean>(
	"SHOW_PLOT_LEGEND", Boolean.class, bundle.getString("SIM:0000008"),
	Boolean.TRUE, bundle.getString("SIM:0000004"));
    /**
     * Decides whether or not plots should display tool tips next to each curve.
     */
    public static final Option<Boolean> SHOW_PLOT_TOOLTIPS = new Option<Boolean>(
	"SHOW_PLOT_TOOLTIPS", Boolean.class, bundle.getString("SIM:0000009"),
	Boolean.FALSE, bundle.getString("SIM:0000005"));
        
    public static final OptionGroup TITLE_OPTIONS = new OptionGroup("Adjust the tile for the plot", "Plot title", PLOT_TITLE);
    public static final OptionGroup BASE_OPTIONS = new OptionGroup("Plotting panel options", "Options for the visible rect of the plot panel", SHOW_PLOT_GRID, SHOW_PLOT_LEGEND, SHOW_PLOT_TOOLTIPS);
    public static final OptionGroup PLOT_APPEARANCE = new OptionGroup("Options for appearence of the Plot Area", "Options for appearence of the Plot Area", PLOT_BACKGROUND_COLOR, PLOT_GRID_COLOR);

}
