/**
 * 
 */
package de.zbit.gui.prefs;

import java.awt.event.ItemEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.sbml.jsbml.util.StringTools;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.SimulatorOptions;
import org.sbml.simulator.gui.GUITools;
import org.sbml.simulator.math.Distance;
import org.sbml.simulator.math.odes.AbstractDESSolver;

import de.zbit.gui.LayoutHelper;
import de.zbit.util.SBProperties;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-14
 */
public class SettingsPanelComputation extends PreferencesPanel {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 4463577603729708633L;

	/**
	 * @param properties
	 * @param defaultProperties
	 */
	public SettingsPanelComputation(SBProperties properties) {
		super(properties);
	}

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
	@Override
	public String getTitle() {
		return "Computation";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#init()
	 */
	@Override
	public void init() {
		JComboBox solverBox = new JComboBox();
		JComboBox distanceBox = new JComboBox();
		int i;
		String keys[] = { SimulatorOptions.SIM_DISTANCE_DEFAULT_VALUE,
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
		double value = ((Number) properties.get(key)).doubleValue();
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(value,
				spinnerMinValue, spinnerMaxValue, spinnerStepSize));
		spinner.setName(key.toString());
		spinner.setToolTipText(GUITools.toHTML(String.format(toolTip,
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
	private void initComboBox(JComboBox combo, Class<?>[] classes, String key)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.zbit.gui.cfg.SettingsPanel#itemStateChanged(java.awt.event.ItemEvent)
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof JComboBox) {
			JComboBox combo = (JComboBox) e.getSource();
			if (combo.getName() != null) {
				try {
					String key = combo.getName();
					if (key.equals(SimulatorOptions.SIM_DISTANCE_FUNCTION)) {
						properties.put(key, SBMLsimulator
								.getAvailableDistances()[combo
								.getSelectedIndex()]);
					} else if (key.equals(SimulatorOptions.SIM_ODE_SOLVER)) {
						properties.put(key,
								SBMLsimulator.getAvailableSolvers()[combo
										.getSelectedIndex()]);
					}
				} catch (Throwable t) {
				}
			}
		}
		super.itemStateChanged(e);
	}

	/**
	 * 
	 * @param lh
	 * @param spinner
	 * @param cfgKeys
	 * @param string
	 */
	private void addSpinner(LayoutHelper lh, JSpinner spinner, String cfgKeys,
			String label) {
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
	 * @see
	 * de.zbit.gui.cfg.SettingsPanel#initConstantFields(java.util.Properties)
	 */
	@Override
	public void initConstantFields(Properties properties) {
		spinnerMaxValue = ((Number) properties
				.get(SimulatorCfgKeys.SimulatorOptions)).doubleValue();
		spinnerMinValue = ((Number) properties
				.get(SimulatorCfgKeys.SimulatorOptions)).doubleValue();
		spinnerStepSize = ((Number) properties
				.get(SimulatorCfgKeys.SimulatorOptions)).doubleValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.zbit.gui.cfg.SettingsPanel#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() instanceof JSpinner) {
			JSpinner spinner = (JSpinner) e.getSource();
			if (spinner.getName() != null) {
				try {
					properties.put(spinner.getName(), (Double) spinner
							.getValue());
				} catch (Throwable t) {
				}
			}
		}
		super.stateChanged(e);
	}
}
