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
import java.lang.reflect.InvocationTargetException;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

import org.sbml.jsbml.util.StringTools;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.SimulatorOptions;
import org.sbml.simulator.gui.GUITools;
import org.sbml.simulator.math.Distance;
import org.sbml.simulator.math.odes.AbstractDESSolver;

import de.zbit.gui.LayoutHelper;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.SBPreferences;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-14
 * @version $Rev$
 * @since 1.0 
 */
public class PrefsPanelComputation extends PreferencesPanel {

	/**
	 * @throws IOException
	 */
	public PrefsPanelComputation() throws IOException {
		super();
	}

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 4463577603729708633L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#accepts(java.lang.Object)
	 */
	@Override
	public boolean accepts(Object key) {
		String k = key.toString();
		return k.startsWith("SIM_") || k.startsWith("OPT_DEFAULT_");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#getTitle()
	 */
	public String getTitle() {
		return "Computation";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#init()
	 */
	public void init() {
		spinnerMaxValue = 2000;
		spinnerMinValue = 0d;
		spinnerStepSize = .1d;
		JComboBox solverBox = new JComboBox();
		JComboBox distanceBox = new JComboBox();
		int i;
		Option<?> keys[] = { SimulatorOptions.SIM_DISTANCE_DEFAULT_VALUE,
				SimulatorOptions.SIM_DISTANCE_ROOT,
				SimulatorOptions.SIM_MAX_TIME, SimulatorOptions.SIM_START_TIME,
				SimulatorOptions.SIM_END_TIME,
				SimulatorOptions.SIM_MAX_STEPS_PER_UNIT_TIME,
				SimulatorOptions.SIM_STEP_SIZE,
				SimulatorOptions.OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE,
				SimulatorOptions.OPT_DEFAULT_SPECIES_INITIAL_VALUE,
				SimulatorOptions.OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS };
		String names[] = { "Default value:", "Root:",
				"Maximal simulation time:", "Simulation start time:",
				"Simulation end time:",
				"Maximal number of steps per time unit:",
				"Simulation step size:", "Default compartment size:",
				"Default species value:", "Default parameter value:" };
		String toolTips[] = { "", "", "", "", "", "", "", "", "", "" };
		JPanel panelDistance = new JPanel(), panelTimes = new JPanel(), panelDefaults = new JPanel();
		LayoutHelper lDistance = new LayoutHelper(panelDistance);
		LayoutHelper lTimes = new LayoutHelper(panelTimes);
		LayoutHelper lDefaults = new LayoutHelper(panelDefaults);

		lTimes.add(new JLabel("Default ODE solver:"), new JPanel(), solverBox);
		lTimes.add(new JPanel());

		try {
			initComboBox(solverBox, SBMLsimulator.getAvailableSolvers(),
					SimulatorOptions.SIM_ODE_SOLVER);
			initComboBox(distanceBox, SBMLsimulator.getAvailableDistances(),
					SimulatorOptions.SIM_DISTANCE_DEFAULT_VALUE);
		} catch (Exception exc) {
			GUITools.showErrorMessage(this, exc);
		}

		lDistance.add(new JLabel("Default distance function:"), new JPanel(),
				distanceBox);
		lDistance.add(new JPanel());

		JSpinner spinner[] = new JSpinner[keys.length];
		for (i = 0; i < spinner.length; i++) {
			spinner[i] = createJSpinner(keys[i], names[i], toolTips[i]);
			if (keys[i].equals(SimulatorOptions.SIM_START_TIME)) {
				spinner[i].setEnabled(false);
			}
			if (keys[i].toString().contains("DISTANCE")) {
				addSpinner(lDistance, spinner[i], keys[i], names[i]);
			} else if (keys[i].toString().startsWith("OPT_DEFAULT_")) {
				addSpinner(lDefaults, spinner[i], keys[i], names[i]);
			} else {
				addSpinner(lTimes, spinner[i], keys[i], names[i]);
			}
		}

		panelDistance.setBorder(BorderFactory
				.createTitledBorder(" Distance function "));
		panelTimes.setBorder(BorderFactory
				.createTitledBorder(" Numerical integration "));
		panelDefaults.setBorder(BorderFactory
				.createTitledBorder(" Missing values "));

		LayoutHelper lh = new LayoutHelper(this);
		lh.add(panelTimes, 0, 0, 2, 1, 1, 0);
		lh.add(panelDistance, 0, 1, 1, 1, 1, 0);
		lh.add(panelDefaults, 1, 1, 1, 1, 1, 0);
	}

	/**
	 * Creates a new {@link JSpinner} with all necessary properties and adds
	 * this as a {@link ChangeListener}.
	 * 
	 * @param key
	 * @param name
	 * @param toolTip
	 * @return
	 */
	private JSpinner createJSpinner(Object key, String name, String toolTip) {
		return createJSpinner(key, name, toolTip, spinnerMinValue,
				spinnerMaxValue, spinnerStepSize);
	}

	/**
	 * Creates a new {@link JSpinner} with all necessary properties and adds
	 * this as a {@link ChangeListener}.
	 * 
	 * @param key
	 * @param name
	 * @param toolTip
	 * @param spinnerMinValue
	 * @param spinnerMaxValue
	 * @param spinnerStepSize
	 * @return
	 */
	private JSpinner createJSpinner(Object key, String name, String toolTip,
			double spinnerMinValue, double spinnerMaxValue,
			double spinnerStepSize) {
		double value = Double.parseDouble(properties.get(key));
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(value,
				spinnerMinValue, spinnerMaxValue, spinnerStepSize));
		spinner.setName(key.toString());
		spinner.setToolTipText(StringUtil.toHTML(String.format(toolTip,
				StringTools.firstLetterLowerCase(name.substring(0, name
						.length() - 1))), 60));
		spinner.addChangeListener(this);
		return spinner;
	}

	/**
	 * Just to shorten the source code. Ugly!
	 * 
	 * @param combo
	 * @param classes
	 * @param key
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	private void initComboBox(JComboBox combo, Class<?>[] classes, Option<?> key)
			throws IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		int selectedIndex = 0;
		String name;
		for (int i = 0; i < classes.length; i++) {
			name = properties.get(key).toString();
			if (classes[i].getSimpleName().equals(
					name.substring(name.lastIndexOf('.') + 1))) {
				selectedIndex = i;
			}
			if (key.equals(SimulatorOptions.SIM_ODE_SOLVER)) {
				AbstractDESSolver solver = (AbstractDESSolver) classes[i]
						.getConstructor().newInstance();
				combo.addItem(solver.getName());
			} else {
				Distance distance = (Distance) classes[i].getConstructor()
						.newInstance();
				combo.addItem(distance.getName());
			}
		}
		combo.setName(key.toString());
		combo.setSelectedIndex(selectedIndex);
		combo.addItemListener(this);
	}

	/**
	 * @param lh
	 * @param spinner
	 * @param cfgKeys
	 * @param string
	 */
	private void addSpinner(LayoutHelper lh, JSpinner spinner,
			Option<?> cfgKeys, String label) {
		// lh.add(new JLabel(label), 0, lh.getRow()+1, 1, 1, 0, 0);
		// if (lh.getRow() == 1) {
		// lh.add(new JPanel(), 1, lh.getRow(), 1, 1, 0, 0);
		// }
		// lh.add(spinner, 2, lh.getRow(), 1, 1, 1, 0);
		// lh.add(spinner, 0, lh.getRow() + 1, 3, 1, 1, 0);
		lh.add(new JLabel(label), new JPanel(), spinner);
		lh.add(new JPanel());
	}

	/**
	 * The user-defined bounds of {@link JSpinner}s
	 */
	private double spinnerMaxValue, spinnerMinValue, spinnerStepSize;

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.prefs.PreferencesPanel#loadPreferences()
	 */
	protected SBPreferences loadPreferences() throws IOException {
		return SBPreferences.getPreferencesFor(SimulatorOptions.class);
	}
}
