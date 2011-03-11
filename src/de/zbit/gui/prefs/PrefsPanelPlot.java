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

import org.sbml.simulator.SimulatorOptions;

import de.zbit.gui.LayoutHelper;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.SBPreferences;

/**
 * For configuration of how to save images of plotted diagrams.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-09-14
 * @version $Rev$
 * @since 1.0
 */
public class PrefsPanelPlot extends PreferencesPanel {

    /**
     * Generated serial version identifier.
     */
    private static final long serialVersionUID = -3432535613230251853L;

    /**
     * 
     */
    private FileSelector chooser;
    /**
     * Settings for {@link JSpinner}s to be applied here.
     */
    private double stepSize;
    /**
     * 
     * @throws IOException
     */
    public PrefsPanelPlot() throws IOException {
	super();
    }


    /*
     * (non-Javadoc)
     * @see de.zbit.gui.cfg.SettingsPanel#accepts(java.lang.Object)
     */
    @Override
    public boolean accepts(Object key) {
	String k = key.toString();
	return k.startsWith("PLOT_") || k.equals("JPEG_COMPRESSION_FACTOR");
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.cfg.SettingsPanel#getTitle()
     */
    @Override
    public String getTitle() {
	return "Plot";
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.cfg.SettingsPanel#init()
     */
    @Override
    public void init() {
	chooser = new FileSelector(null, properties.get(
	    SimulatorOptions.PLOT_SAVE_DIR).toString());
	chooser.setBorder(BorderFactory
		.createTitledBorder(" Output directory for images "));

	String names[] = { "Logarithmic scale", "Show grid", "Include legend",
		"Display tooltips" };
	Option<?> keys[] = { SimulatorOptions.PLOT_LOG_SCALE,
		SimulatorOptions.PLOT_SHOW_GRID,
		SimulatorOptions.PLOT_SHOW_LEGEND,
		SimulatorOptions.PLOT_SHOW_TOOLTIPS };
	JCheckBox check[] = new JCheckBox[names.length];
	JPanel layout = new JPanel();
	LayoutHelper lh = new LayoutHelper(layout);
	for (int i = 0; i < check.length; i++) {
	    check[i] = new JCheckBox(names[i], Boolean.parseBoolean(properties
		    .get(keys[i])));
	    check[i].setName(keys[i].toString());
	    check[i].addItemListener(this);
	    lh.add(check[i]);
	}
	layout.setBorder(BorderFactory.createTitledBorder(" Layout "));

	JSpinner compression = new JSpinner(new SpinnerNumberModel(Double
		.parseDouble(properties
			.get(SimulatorOptions.JPEG_COMPRESSION_FACTOR)), 0d,
	    1d, stepSize));
	compression.addChangeListener(this);
	JPanel image = new JPanel();
	lh = new LayoutHelper(image);
	lh.add(new JLabel("JPEG compression factor"), 0, 0, 1, 1, 0, 0);
	lh.add(new JPanel(), 1, 0, 1, 1, 0, 0);
	lh.add(compression, 2, 0, 5, 1, 1, 0);
	image.setBorder(BorderFactory.createTitledBorder(" Image "));

	lh = new LayoutHelper(this);
	lh.add(chooser, 0, 0, 2, 1, 1, 0);
	lh.add(layout, 0, 1, 1, 1, 1, 0);
	lh.add(image, 1, 1, 1, 1, 1, 0);
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.prefs.PreferencesPanel#loadPreferences()
     */
    protected SBPreferences loadPreferences() throws IOException {
	return SBPreferences.getPreferencesFor(SimulatorOptions.class);
    }

}
