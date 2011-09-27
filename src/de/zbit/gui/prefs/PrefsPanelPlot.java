/*
 * $Id$
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
package de.zbit.gui.prefs;

import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.sbml.simulator.gui.plot.PlotOptions;

import de.zbit.gui.LayoutHelper;
import de.zbit.util.prefs.Option;

/**
 * For configuration of how to save images of plotted diagrams.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-09-14
 * @version $Rev$
 * @since 1.0
 */
public class PrefsPanelPlot extends PreferencesPanelForKeyProvider {

    /**
     * Generated serial version identifier.
     */
    private static final long serialVersionUID = -3432535613230251853L;

    /**
     * 
     * @throws IOException
     */
    public PrefsPanelPlot() throws IOException {
	super(PlotOptions.class);
    }

//    /*
//     * (non-Javadoc)
//     * @see de.zbit.gui.prefs.PreferencesPanelForKeyProvider#init()
//     */
//    @Override
//    public void init() {
//	FileSelector chooser = new FileSelector(null, properties.get(
//	    PlotOptions.PLOT_SAVE_DIR).toString());
//	chooser.setBorder(BorderFactory
//		.createTitledBorder(" Output directory for images "));
//
//	String names[] = { "Logarithmic scale", "Show grid", "Include legend",
//		"Display tooltips" };
//	Option<?> keys[] = { PlotOptions.PLOT_LOG_SCALE,
//		PlotOptions.SHOW_PLOT_GRID,
//		PlotOptions.SHOW_PLOT_LEGEND,
//		PlotOptions.SHOW_PLOT_TOOLTIPS };
//	JCheckBox check[] = new JCheckBox[names.length];
//	JPanel layout = new JPanel();
//	LayoutHelper lh = new LayoutHelper(layout);
//	for (int i = 0; i < check.length; i++) {
//	    check[i] = new JCheckBox(names[i], Boolean.parseBoolean(properties
//		    .get(keys[i])));
//	    check[i].setName(keys[i].toString());
//	    check[i].addItemListener(this);
//	    lh.add(check[i]);
//	}
//	layout.setBorder(BorderFactory.createTitledBorder(" Layout "));
//
//	double stepSize = 1E-3;
//	JSpinner compression = new JSpinner(new SpinnerNumberModel(Double
//		.parseDouble(properties
//			.get(PlotOptions.JPEG_COMPRESSION_FACTOR)), 0d,
//	    1d, stepSize));
//	compression.addChangeListener(this);
//	JPanel image = new JPanel();
//	lh = new LayoutHelper(image);
//	lh.add(new JLabel("JPEG compression factor"), 0, 0, 1, 1, 0, 0);
//	lh.add(new JPanel(), 1, 0, 1, 1, 0, 0);
//	lh.add(compression, 2, 0, 5, 1, 1, 0);
//	image.setBorder(BorderFactory.createTitledBorder(" Image "));
//
//	lh = new LayoutHelper(this);
//	lh.add(chooser, 0, 0, 2, 1, 1, 0);
//	lh.add(layout, 0, 1, 1, 1, 1, 0);
//	lh.add(image, 1, 1, 1, 1, 1, 0);
//    }

}
