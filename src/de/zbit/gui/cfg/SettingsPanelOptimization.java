/**
 * 
 */
package de.zbit.gui.cfg;

import java.awt.event.ItemEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;

import org.sbml.jsbml.util.StringTools;
import org.sbml.simulator.SimulatorCfgKeys;
import org.sbml.simulator.gui.GUITools;

import de.zbit.gui.LayoutHelper;
import de.zbit.util.SBProperties;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-14
 */
public class SettingsPanelOptimization extends PreferencesPanel {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 4753517737576120353L;
	/**
	 * Tool tip text to explain the purpose of check boxes that are to select
	 * default optimization targets.
	 */
	private static final String CHECKBOX_TOOLTIP = "Check this box if you want that by default all %s are treated as the target in a model calibration.";
	/**
	 * Explains the purpose of the multiple shooting strategy check box.
	 */
	private static final String CHECKBOX_TOOLTIP_STRATEGY = "If this box is selected, the optimization will apply a multiple shooting strategy, otherwise a single-shoot strategy will be used.";
	/**
	 * Tool tip text to explain the purpose of {@link JSpinner}s where upper and
	 * lower bounds for the optimization can be selected.
	 */
	private static final String SPINNER_TOOLTIP = "Here you can select the default %s value for all optimization targets in a model calibration.";
	/**
	 * The user-defined bounds of {@link JSpinner}s
	 */
	private double spinnerMaxValue, spinnerMinValue, spinnerStepSize;

	/**
	 * @param properties
	 * @param defaultProperties
	 */
	public SettingsPanelOptimization(SBProperties properties) {
		super(properties);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.zbit.gui.cfg.SettingsPanel#initConstantFields(java.util.Properties)
	 */
	public void initConstantFields(SBProperties properties) {
		spinnerMaxValue = ((Number) properties
				.get(SimulatorCfgKeys.SPINNER_MAX_VALUE)).doubleValue();
		spinnerMinValue = ((Number) properties
				.get(SimulatorCfgKeys.SPINNER_MIN_VALUE)).doubleValue();
		spinnerStepSize = ((Number) properties
				.get(SimulatorCfgKeys.SPINNER_STEP_SIZE)).doubleValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#accepts(java.lang.Object)
	 */
	@Override
	public boolean accepts(Object key) {
		return key.toString().startsWith("EST_");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		return "Optimization";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#init()
	 */
	@Override
	public void init() {
		JCheckBox check[] = new JCheckBox[4];
		String names[] = { "Local parameters", "Global parameters",
				"Compartments", "Species" };
		String keys[] = { SimulatorCfgKeys.EST_ALL_LOCAL_PARAMETERS,
				SimulatorCfgKeys.EST_ALL_GLOBAL_PARAMETERS,
				SimulatorCfgKeys.EST_ALL_COMPARTMENTS,
				SimulatorCfgKeys.EST_ALL_SPECIES };
		JPanel panelWhatToEstimate = new JPanel();
		LayoutHelper lh = new LayoutHelper(panelWhatToEstimate);
		int i;
		for (i = 0; i < check.length; i++) {
			check[i] = new JCheckBox(names[i], ((Boolean) properties
					.get(keys[i])).booleanValue());
			check[i].setName(keys[i].toString());
			check[i].setToolTipText(GUITools.toHTML(String.format(
					CHECKBOX_TOOLTIP, StringTools
							.firstLetterLowerCase(names[i])), 60));
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
		keys = new String[] { SimulatorCfgKeys.EST_INIT_MIN_VALUE,
				SimulatorCfgKeys.EST_INIT_MAX_VALUE,
				SimulatorCfgKeys.EST_MIN_VALUE, SimulatorCfgKeys.EST_MAX_VALUE };
		int row = 0;
		for (i = 0; i < spinner.length; i++) {
			spinner[i] = new JSpinner(new SpinnerNumberModel(
					((Number) properties.get(keys[i])).doubleValue(),
					spinnerMinValue, spinnerMaxValue, spinnerStepSize));
			spinner[i].setName(keys[i].toString());
			spinner[i].setToolTipText(GUITools.toHTML(String.format(
					SPINNER_TOOLTIP, StringTools.firstLetterLowerCase(names[i]
							.substring(0, names[i].length() - 1))), 60));
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
				((Boolean) properties.get(SimulatorCfgKeys.EST_MULTI_SHOOT))
						.booleanValue(), SimulatorCfgKeys.EST_MULTI_SHOOT,
				CHECKBOX_TOOLTIP_STRATEGY, this), 0, 0, 1, 1, 1, 0);
		panelIntegrationStrategy.setBorder(BorderFactory
				.createTitledBorder(" Integration strategy "));

		lh = new LayoutHelper(this);
		lh.add(panelWhatToEstimate, 0, 0, 1, 1, 1, 0);
		lh.add(panelRanges, 1, 0, 1, 1, 1, 0);
		lh.add(panelIntegrationStrategy, 0, 1, 2, 1, 1, 0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.zbit.gui.cfg.SettingsPanel#itemStateChanged(java.awt.event.ItemEvent)
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof JCheckBox) {
			JCheckBox check = (JCheckBox) e.getSource();
			if (check.getActionCommand() != null) {
				try {
					properties.put(check.getActionCommand(), Boolean
							.valueOf(check.isSelected()));
				} catch (Throwable t) {
				}
			}
		}
		super.itemStateChanged(e);
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
