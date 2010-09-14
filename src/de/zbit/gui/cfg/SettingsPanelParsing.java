/**
 * 
 */
package de.zbit.gui.cfg;

import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.sbml.squeezer.CfgKeys;

import de.zbit.gui.LayoutHelper;

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
	public SettingsPanelParsing(Properties properties,
			Properties defaultProperties) {
		super(properties, defaultProperties);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#getProperties()
	 */
	@Override
	public Properties getProperties() {
		if (chooser.checkOpenDir()) {
			settings.put(CfgKeys.CSV_FILES_OPEN_DIR, chooser.getOpenDir());
		}
		if (chooser.checkSaveDir()) {
			settings.put(CfgKeys.CSV_FILES_SAVE_DIR, chooser.getSaveDir());
		}
		if (check(tfQuoteChar)) {
			settings.put(CfgKeys.CSV_FILES_QUOTE_CHAR, tfQuoteChar.getText());
		}
		if (check(tfSeparatorChar)) {
			settings.put(CfgKeys.CSV_FILES_QUOTE_CHAR, tfQuoteChar.getText());
		}
		return settings;
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
		chooser = new DirectoryChooser(settings.get(CfgKeys.CSV_FILES_OPEN_DIR)
				.toString(), settings.get(CfgKeys.CSV_FILES_SAVE_DIR)
				.toString());
		chooser.setBorder(BorderFactory
				.createTitledBorder(" Default directories for CSV files "));

		tfQuoteChar = new JTextField(settings.get(CfgKeys.CSV_FILES_QUOTE_CHAR)
				.toString());
		tfQuoteChar.addKeyListener(this);
		tfSeparatorChar = new JTextField(settings.get(
				CfgKeys.CSV_FILES_SEPARATOR_CHAR).toString());
		tfSeparatorChar.addKeyListener(this);
		JPanel panel = new JPanel();
		LayoutHelper lh = new LayoutHelper(panel);
		lh.add(new JLabel("Element separator:"), tfSeparatorChar);
		lh.add(new JLabel("Comment symbol:"), tfQuoteChar);
		panel.setBorder(BorderFactory
				.createTitledBorder(" Separator and comment character "));

		lh = new LayoutHelper(this);
		lh.add(chooser);
		lh.add(panel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties properties) {
		this.settings = new Properties();
		String k;
		for (Object key : properties.keySet()) {
			k = key.toString();
			if (k.startsWith("CSV_")) {
				settings.put(key, properties.get(key));
			}
		}
		init();
	}

}
