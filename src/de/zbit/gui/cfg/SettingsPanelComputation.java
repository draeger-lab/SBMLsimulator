/**
 * 
 */
package de.zbit.gui.cfg;

import java.util.Properties;

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

	/* (non-Javadoc)
	 * @see de.zbit.gui.cfg.SettingsPanel#getProperties()
	 */
	@Override
	public Properties getProperties() {
		// TODO Auto-generated method stub
		return settings;
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.cfg.SettingsPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		return "Computation";
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
