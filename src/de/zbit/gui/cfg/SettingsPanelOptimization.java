/**
 * 
 */
package de.zbit.gui.cfg;

import java.util.Properties;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-14
 */
public class SettingsPanelOptimization extends SettingsPanel {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 4753517737576120353L;

	/**
	 * @param properties
	 * @param defaultProperties
	 */
	public SettingsPanelOptimization(Properties properties,
			Properties defaultProperties) {
		super(properties, defaultProperties);
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.cfg.SettingsPanel#getProperties()
	 */
	@Override
	public Properties getProperties() {
		// TODO Auto-generated method stub
		return new Properties();
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.cfg.SettingsPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		return "Optimization";
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.cfg.SettingsPanel#init()
	 */
	@Override
	public void init() {
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.cfg.SettingsPanel#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties settings) {
		// TODO Auto-generated method stub
	}

}
