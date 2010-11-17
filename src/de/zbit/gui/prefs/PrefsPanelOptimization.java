/**
 * 
 */
package de.zbit.gui.prefs;

import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.sbml.jsbml.util.StringTools;
import org.sbml.simulator.SimulatorOptions;
import org.sbml.simulator.gui.GUITools;

import de.zbit.gui.LayoutHelper;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.SBPreferences;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-14
 */
public class PrefsPanelOptimization extends PreferencesPanel {

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
     * @throws IOException
     */
    public PrefsPanelOptimization() throws IOException {
	super();
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.cfg.SettingsPanel#accepts(java.lang.Object)
     */
    @Override
    public boolean accepts(Object key) {
	return key.toString().startsWith("EST_");
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.cfg.SettingsPanel#getTitle()
     */
    @Override
    public String getTitle() {
	return "Optimization";
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.cfg.SettingsPanel#init()
     */
    @Override
    public void init() {
	JCheckBox check[] = new JCheckBox[4];
	String names[] = { "Local parameters", "Global parameters",
		"Compartments", "Species" };
	Option<?> keys[] = { SimulatorOptions.EST_ALL_LOCAL_PARAMETERS,
		SimulatorOptions.EST_ALL_GLOBAL_PARAMETERS,
		SimulatorOptions.EST_ALL_COMPARTMENTS,
		SimulatorOptions.EST_ALL_SPECIES };
	JPanel panelWhatToEstimate = new JPanel();
	LayoutHelper lh = new LayoutHelper(panelWhatToEstimate);
	int i;
	for (i = 0; i < check.length; i++) {
	    check[i] = new JCheckBox(names[i], Boolean.parseBoolean(properties
		    .get(keys[i])));
	    check[i].setName(keys[i].toString());
	    check[i].setToolTipText(StringUtil.toHTML(String.format(
		CHECKBOX_TOOLTIP, StringTools.firstLetterLowerCase(names[i])),
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
	keys = new Option<?>[] { SimulatorOptions.EST_INIT_MIN_VALUE,
		SimulatorOptions.EST_INIT_MAX_VALUE,
		SimulatorOptions.EST_MIN_VALUE, SimulatorOptions.EST_MAX_VALUE };
	int row = 0;
	for (i = 0; i < spinner.length; i++) {
	    spinner[i] = new JSpinner(new SpinnerNumberModel(Double
		    .parseDouble(properties.get(keys[i])), spinnerMinValue,
		spinnerMaxValue, spinnerStepSize));
	    spinner[i].setName(keys[i].toString());
	    spinner[i].setToolTipText(StringUtil.toHTML(String.format(
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
	    Boolean.parseBoolean(properties
		    .get(SimulatorOptions.EST_MULTI_SHOOT)),
	    SimulatorOptions.EST_MULTI_SHOOT, CHECKBOX_TOOLTIP_STRATEGY, this),
	    0, 0, 1, 1, 1, 0);
	panelIntegrationStrategy.setBorder(BorderFactory
		.createTitledBorder(" Integration strategy "));

	lh = new LayoutHelper(this);
	lh.add(panelWhatToEstimate, 0, 0, 1, 1, 1, 0);
	lh.add(panelRanges, 1, 0, 1, 1, 1, 0);
	lh.add(panelIntegrationStrategy, 0, 1, 2, 1, 1, 0);
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.prefs.PreferencesPanel#loadPreferences()
     */
    protected SBPreferences loadPreferences() throws IOException {
	return SBPreferences.getPreferencesFor(SimulatorOptions.class);
    }
}
