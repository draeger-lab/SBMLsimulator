/**
 * 
 */
package de.zbit.gui.cfg;

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

import org.sbml.jsbml.util.StringTools;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.gui.GUITools;
import org.sbml.simulator.math.Distance;
import org.sbml.simulator.math.odes.AbstractDESSolver;
import org.sbml.squeezer.CfgKeys;

import de.zbit.gui.LayoutHelper;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-14
 */
public class SettingsPanelComputation extends SettingsPanel {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 4463577603729708633L;

	/**
	 * @param properties
	 * @param defaultProperties
	 */
	public SettingsPanelComputation(Properties properties,
			Properties defaultProperties) {
		super(properties, defaultProperties);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#accepts(java.lang.Object)
	 */
	@Override
	public boolean accepts(Object key) {
		return key.toString().startsWith("SIM_");
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
		CfgKeys keys[] = { CfgKeys.SIM_DISTANCE_DEFAULT_VALUE,
				CfgKeys.SIM_DISTANCE_ROOT, CfgKeys.SIM_MAX_TIME,
				CfgKeys.SIM_START_TIME, CfgKeys.SIM_END_TIME,
				CfgKeys.SIM_MAX_STEPS_PER_UNIT_TIME, CfgKeys.SIM_STEP_SIZE };
		String names[] = { "Default value:", "Root:",
				"Maximal simulation time:", "Simulation start time:",
				"Simulation end time:",
				"Maximal number of steps per time unit:",
				"Simulation step size:" };
		String toolTips[] = { "", "", "", "", "", "", "" };
		JPanel panelSolver = new JPanel(), panelDistance = new JPanel(), panelTimes = new JPanel();
		LayoutHelper lSolver = new LayoutHelper(panelSolver);
		LayoutHelper lDistance = new LayoutHelper(panelDistance);
		LayoutHelper lTimes = new LayoutHelper(panelTimes);

		try {
			initComboBox(solverBox, SBMLsimulator.getAvailableSolvers(),
					CfgKeys.SIM_ODE_SOLVER);
			initComboBox(distanceBox, SBMLsimulator.getAvailableDistances(),
					CfgKeys.SIM_DISTANCE_DEFAULT_VALUE);
		} catch (Exception exc) {
			GUITools.showErrorMessage(this, exc);
		}

		lSolver.add(new JLabel("Default ODE solver:"), new JPanel(), solverBox);
		lSolver.add(new JPanel());
		lDistance.add(new JLabel("Default distance function:"), new JPanel(),
				distanceBox);
		lDistance.add(new JPanel());

		JSpinner spinner[] = new JSpinner[keys.length];
		for (i = 0; i < spinner.length; i++) {
			spinner[i] = new JSpinner(new SpinnerNumberModel(
					((Number) properties.get(keys[i])).doubleValue(),
					spinnerMinValue, spinnerMaxValue, spinnerStepSize));
			spinner[i].setName(keys[i].toString());
			spinner[i].setToolTipText(GUITools.toHTML(String.format(
					toolTips[i], StringTools.firstLetterLowerCase(names[i]
							.substring(0, names[i].length() - 1))), 60));
			if (keys[i].equals(CfgKeys.SIM_START_TIME)) {
				spinner[i].setEnabled(false);
			}
			spinner[i].addChangeListener(this);
			if (keys[i].toString().contains("DISTANCE")) {
				addSpinner(lDistance, spinner[i], keys[i], names[i]);
			} else {
				addSpinner(lTimes, spinner[i], keys[i], names[i]);
			}
		}

		panelSolver.setBorder(BorderFactory
				.createTitledBorder(" Numerical integrator "));
		panelDistance.setBorder(BorderFactory
				.createTitledBorder(" Distance function "));
		panelTimes.setBorder(BorderFactory
				.createTitledBorder(" Integration time settings "));

		LayoutHelper lh = new LayoutHelper(this);
		lh.add(panelSolver);
		lh.add(panelDistance);
		lh.add(panelTimes);
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
	private void initComboBox(JComboBox combo, Class<?>[] classes, CfgKeys key)
			throws IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		int selectedIndex = 0;
		String name;
		for (int i = 0; i < classes.length; i++) {
			name = properties.get(key).toString();
			if (classes[i].getSimpleName().equals(
					name.substring(name.lastIndexOf('.')))) {
				selectedIndex = i;
			}
			if (key.equals(CfgKeys.SIM_ODE_SOLVER)) {
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
					CfgKeys key = CfgKeys.valueOf(combo.getName());
					if (key.equals(CfgKeys.SIM_DISTANCE_FUNCTION)) {
						properties.put(key, SBMLsimulator
								.getAvailableDistances()[combo
								.getSelectedIndex()]);
					} else if (key.equals(CfgKeys.SIM_ODE_SOLVER)) {
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
	private void addSpinner(LayoutHelper lh, JSpinner spinner, CfgKeys cfgKeys,
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
		spinnerMaxValue = ((Number) properties.get(CfgKeys.SPINNER_MAX_VALUE))
				.doubleValue();
		spinnerMinValue = ((Number) properties.get(CfgKeys.SPINNER_MIN_VALUE))
				.doubleValue();
		spinnerStepSize = ((Number) properties.get(CfgKeys.SPINNER_STEP_SIZE))
				.doubleValue();
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
					CfgKeys key = CfgKeys.valueOf(spinner.getName());
					properties.put(key, (Double) spinner.getValue());
				} catch (Throwable t) {
				}
			}
		}
		super.stateChanged(e);
	}
}
