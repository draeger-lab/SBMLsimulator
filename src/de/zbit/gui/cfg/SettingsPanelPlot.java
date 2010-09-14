/**
 * 
 */
package de.zbit.gui.cfg;

import java.awt.event.ItemEvent;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;

import org.sbml.squeezer.CfgKeys;

import de.zbit.gui.LayoutHelper;

/**
 * For configuration of how to save images of plotted diagrams.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-09-14
 */
public class SettingsPanelPlot extends SettingsPanel {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = -3432535613230251853L;
	/**
	 * Settings for {@link JSpinner}s to be applied here.
	 */
	private double stepSize;
	/**
	 * 
	 */
	private DirectoryChooser chooser;

	/**
	 * @param properties
	 * @param defaultProperties
	 */
	public SettingsPanelPlot(Properties properties, Properties defaultProperties) {
		super(properties, defaultProperties);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#getProperties()
	 */
	@Override
	public Properties getProperties() {
		if (chooser.checkSaveDir()) {
			settings.put(CfgKeys.PLOT_SAVE_DIR, chooser.getSaveDir());
		}
		return settings;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		return "Plot";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#init()
	 */
	@Override
	public void init() {
		chooser = new DirectoryChooser(null, settings.get(CfgKeys.PLOT_SAVE_DIR).toString());
		chooser.setBorder(BorderFactory.createTitledBorder(" Output directory for images "));

		String names[] = { "Logarithmic scale", "Show grid", "Include legend",
				"Display tooltips" };
		CfgKeys keys[] = { CfgKeys.PLOT_LOG_SCALE, CfgKeys.PLOT_SHOW_GRID,
				CfgKeys.PLOT_SHOW_LEGEND, CfgKeys.PLOT_SHOW_TOOLTIPS };
		JCheckBox check[] = new JCheckBox[names.length];
		JPanel layout = new JPanel();
		LayoutHelper lh = new LayoutHelper(layout);
		for (int i = 0; i < check.length; i++) {
			check[i] = new JCheckBox(names[i],
					((Boolean) settings.get(keys[i])).booleanValue());
			check[i].setName(keys[i].toString());
			check[i].addItemListener(this);
			lh.add(check[i]);
		}
		layout.setBorder(BorderFactory.createTitledBorder(" Layout "));

		JSpinner compression = new JSpinner(new SpinnerNumberModel(
				((Number) settings.get(CfgKeys.JPEG_COMPRESSION_FACTOR))
						.doubleValue(), 0d, 1d, stepSize));
		compression.addChangeListener(this);
		JPanel image = new JPanel();
		lh = new LayoutHelper(image);
		lh.add(new JLabel("JPEG compression factor"), 0, 0, 1, 1, 0, 0);
		lh.add(new JPanel(), 1, 0, 1, 1, 0, 0);
		lh.add(compression, 2, 0, 5, 1, 1, 0);
		image.setBorder(BorderFactory.createTitledBorder(" Image "));

		lh = new LayoutHelper(this);
		lh.add(chooser, 0, 0, 2, 1, 1, 0);
		lh.add(layout, 0, 1, 1, 1, 1, 0);
		lh.add(image, 1, 1, 1, 1, 1, 0);
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
			JCheckBox box = (JCheckBox) e.getSource();
			if (box.getName() != null) {
				settings.put(CfgKeys.valueOf(box.getName()), Boolean
						.valueOf(box.isSelected()));
			}
		}
		super.itemStateChanged(e);
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
			if (k.startsWith("PLOT_") || k.equals("JPEG_COMPRESSION_FACTOR")) {
				settings.put(key, properties.get(key));
			}
		}
		stepSize = ((Number) properties.get(CfgKeys.SPINNER_STEP_SIZE))
				.doubleValue();
		init();
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
			settings.put(CfgKeys.JPEG_COMPRESSION_FACTOR,
					(Double) ((JSpinner) e.getSource()).getValue());
		}
		super.stateChanged(e);
	}

}
