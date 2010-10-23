/**
 * 
 */
package de.zbit.gui.cfg;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.sbml.simulator.SimulatorCfgKeys;

import de.zbit.gui.LayoutHelper;
import de.zbit.util.SBProperties;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-14
 */
public class SettingsPanelParsing extends SettingsPanel {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 4424293629377476108L;
	/**
	 * 
	 */
	private DirectoryChooser chooser;
	/**
	 * 
	 */
	private JTextField tfQuoteChar, tfSeparatorChar;

	/**
	 * @param properties
	 * @param defaultProperties
	 */
	public SettingsPanelParsing(SBProperties properties) {
		super(properties);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#accepts(java.lang.Object)
	 */
	@Override
	public boolean accepts(Object key) {
		return key.toString().startsWith("CSV_");
	}

	/**
	 * 
	 * @param tf
	 * @return
	 */
	private boolean check(JTextField tf) {
		String text = tf.getText();
		if ((text.length() == 1)
				|| ((text.length() == 2) && (text.charAt(0) == '\\'))) {
			return true;
		}
		JOptionPane.showMessageDialog(this, String.format(
				"Invalid character %s.", text), "Warning",
				JOptionPane.WARNING_MESSAGE);
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#getProperties()
	 */
	@Override
	public SBProperties getProperties() {
		if (chooser.checkOpenDir()) {
			properties.put(SimulatorCfgKeys.CSV_FILES_OPEN_DIR, chooser
					.getOpenDir());
		}
		if (chooser.checkSaveDir()) {
			properties.put(SimulatorCfgKeys.CSV_FILES_SAVE_DIR, chooser
					.getSaveDir());
		}
		if (check(tfQuoteChar)) {
			properties.put(SimulatorCfgKeys.CSV_FILES_QUOTE_CHAR, tfQuoteChar
					.getText());
		}
		if (check(tfSeparatorChar)) {
			properties.put(SimulatorCfgKeys.CSV_FILES_QUOTE_CHAR, tfQuoteChar
					.getText());
		}
		return properties;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		return "Parsing and writing";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#init()
	 */
	@Override
	public void init() {
		chooser = new DirectoryChooser(properties.get(
				SimulatorCfgKeys.CSV_FILES_OPEN_DIR).toString(), properties
				.get(SimulatorCfgKeys.CSV_FILES_SAVE_DIR).toString());
		chooser.setBorder(BorderFactory
				.createTitledBorder(" Default directories for CSV files "));

		tfQuoteChar = new JTextField(properties.get(
				SimulatorCfgKeys.CSV_FILES_QUOTE_CHAR).toString());
		tfQuoteChar.addKeyListener(this);
		tfSeparatorChar = new JTextField(properties.get(
				SimulatorCfgKeys.CSV_FILES_SEPARATOR_CHAR).toString());
		tfSeparatorChar.addKeyListener(this);
		JPanel panel = new JPanel();
		LayoutHelper lh = new LayoutHelper(panel);
		lh.add(new JLabel("Element separator:"), 0, 0, 1, 1, 0, 0);
		lh.add(new JPanel(), 1, 0, 1, 1, 0, 0);
		lh.add(tfSeparatorChar, 2, 0, 1, 1, 1, 0);
		lh.add(new JPanel(), 0, 1, 3, 1, 1, 0);
		lh.add(new JLabel("Comment symbol:"), 0, 2, 1, 1, 0, 0);
		lh.add(tfQuoteChar, 2, 2, 1, 1, 1, 0);
		panel.setBorder(BorderFactory
				.createTitledBorder(" Separator and comment character "));

		lh = new LayoutHelper(this);
		lh.add(chooser, 0, 0, 1, 1, 1, 0);
		lh.add(panel, 0, 1, 1, 1, 1, 0);
	}
}
