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

import org.sbml.optimization.problem.EstimationOptions;
import org.sbml.simulator.gui.GUITools;

import de.zbit.gui.LayoutHelper;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.Option;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-14
 * @version $Rev$
 * @since 1.0
 */
public class PrefsPanelOptimization extends PreferencesPanelForKeyProvider {

    /**
     * Generated serial version identifier.
     */
    private static final long serialVersionUID = 4753517737576120353L;
    /**
     * The user-defined bounds of {@link JSpinner}s
     */
    private double spinnerMaxValue, spinnerMinValue, spinnerStepSize;

    /**
     * @param properties
     * @param defaultProperties
     * @throws IOException
     */
    public PrefsPanelOptimization() throws IOException {
	super(EstimationOptions.class);
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.cfg.SettingsPanel#init()
     */
    @Override
    public void init() {
	spinnerMaxValue = 2000;
	spinnerMinValue = 0d;
	spinnerStepSize = .1d;

	JCheckBox check[] = new JCheckBox[4];
	String names[] = { "Local parameters", "Global parameters",
		"Compartments", "Species" };
	Option<?> keys[] = { EstimationOptions.EST_ALL_LOCAL_PARAMETERS,
		EstimationOptions.EST_ALL_GLOBAL_PARAMETERS,
		EstimationOptions.EST_ALL_COMPARTMENTS,
		EstimationOptions.EST_ALL_SPECIES };
	JPanel panelWhatToEstimate = new JPanel();
	LayoutHelper lh = new LayoutHelper(panelWhatToEstimate);
	int i;
	for (i = 0; i < check.length; i++) {
	    check[i] = new JCheckBox(names[i], properties
		    .getBooleanProperty(keys[i]));
	    check[i].setName(keys[i].toString());
	    check[i].setToolTipText(StringUtil.toHTML(String.format(
		keys[i].getDescription()),
		60));
	    check[i].addItemListener(this);
	    lh.add(check[i]);
	}
	panelWhatToEstimate.setBorder(BorderFactory
		.createTitledBorder(" Default targets "));

	JPanel panelRanges = new JPanel();
	lh = new LayoutHelper(panelRanges);
	JSpinner spinner[] = new JSpinner[4];
	names = new String[] { "Lower initialization bound:",
		"Upper initialization bound:", "Minimal allowable value:",
		"Maximal allowable value:" };
	keys = new Option<?>[] { EstimationOptions.EST_INIT_MIN_VALUE,
		EstimationOptions.EST_INIT_MAX_VALUE,
		EstimationOptions.EST_MIN_VALUE,
		EstimationOptions.EST_MAX_VALUE };
	int row = 0;
	for (i = 0; i < spinner.length; i++) {
	    spinner[i] = new JSpinner(new SpinnerNumberModel(properties
		    .getDoubleProperty(keys[i]), spinnerMinValue,
		spinnerMaxValue, spinnerStepSize));
	    spinner[i].setName(keys[i].toString());
	    spinner[i].setToolTipText(StringUtil.toHTML(String.format(
		keys[i].getDescription()),
			60));
	    spinner[i].addChangeListener(this);
	    lh.add(new JLabel(names[i]), 0, row, 1, 1, 0, 0);
	    if (i == 0) {
		lh.add(new JPanel(), 1, 0, 1, 1, 0, 0);
	    }
	    lh.add(spinner[i], 2, row++, 1, 1, 1, 0);
	    lh.add(new JPanel(), 0, row++, 3, 1, 0, 0);
	}
	panelRanges.setBorder(BorderFactory
		.createTitledBorder(" Ranges of all optimization targets "));

	JPanel panelIntegrationStrategy = new JPanel();
	lh = new LayoutHelper(panelIntegrationStrategy);
	lh.add(GUITools.createJCheckBox("Use multiple shooting strategy",
	    properties.getBooleanProperty(EstimationOptions.EST_MULTI_SHOOT),
	    EstimationOptions.EST_MULTI_SHOOT,
	    EstimationOptions.EST_MULTI_SHOOT.getDescription(),
	    this), 0, 0, 1, 1, 1, 0);
	panelIntegrationStrategy.setBorder(BorderFactory
		.createTitledBorder(" Integration strategy "));

	lh = new LayoutHelper(this);
	lh.add(panelWhatToEstimate, 0, 0, 1, 1, 1, 0);
	lh.add(panelRanges, 1, 0, 1, 1, 1, 0);
	lh.add(panelIntegrationStrategy, 0, 1, 2, 1, 1, 0);
    }
    
}
