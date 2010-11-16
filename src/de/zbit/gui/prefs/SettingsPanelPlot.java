/**
 * 
 */
package de.zbit.gui.prefs;

import java.awt.event.ItemEvent;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;

import org.sbml.simulator.SimulatorOptions;

import de.zbit.gui.LayoutHelper;
import de.zbit.gui.cfg.DirectoryChooser;
import de.zbit.util.SBProperties;

/**
 * For configuration of how to save images of plotted diagrams.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-09-14
 */
public class SettingsPanelPlot extends PreferencesPanel {

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
	public SettingsPanelPlot(SBProperties properties) {
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
		return k.startsWith("PLOT_") || k.equals("JPEG_COMPRESSION_FACTOR");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#getProperties()
	 */
	@Override
	public SBProperties getProperties() {
		if (chooser.checkSaveDir()) {
			properties
					.put(SimulatorOptions.PLOT_SAVE_DIR, chooser.getSaveDir());
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
		return "Plot";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.zbit.gui.cfg.SettingsPanel#init()
	 */
	@Override
	public void init() {
		chooser = new DirectoryChooser(null, properties.get(
				SimulatorOptions.PLOT_SAVE_DIR).toString());
		chooser.setBorder(BorderFactory
				.createTitledBorder(" Output directory for images "));

		String names[] = { "Logarithmic scale", "Show grid", "Include legend",
				"Display tooltips" };
		String keys[] = { SimulatorOptions.PLOT_LOG_SCALE,
				SimulatorOptions.PLOT_SHOW_GRID,
				SimulatorOptions.PLOT_SHOW_LEGEND,
				SimulatorOptions.PLOT_SHOW_TOOLTIPS };
		JCheckBox check[] = new JCheckBox[names.length];
		JPanel layout = new JPanel();
		LayoutHelper lh = new LayoutHelper(layout);
		for (int i = 0; i < check.length; i++) {
			check[i] = new JCheckBox(names[i], ((Boolean) properties
					.get(keys[i])).booleanValue());
			check[i].setName(keys[i].toString());
			check[i].addItemListener(this);
			lh.add(check[i]);
		}
		layout.setBorder(BorderFactory.createTitledBorder(" Layout "));

		JSpinner compression = new JSpinner(new SpinnerNumberModel(
				((Number) properties
						.get(SimulatorOptions.JPEG_COMPRESSION_FACTOR))
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
	 * de.zbit.gui.cfg.SettingsPanel#initConstantFields(java.util.Properties)
	 */
	@Override
	public void initConstantFields(Properties properties) {
		stepSize = ((Number) properties.get(SimulatorCfgKeys.SimulatorOptions))
				.doubleValue();
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
			if (box.getActionCommand() != null) {
				try {
					properties.put(box.getActionCommand(), Boolean.valueOf(box
							.isSelected()));
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
			properties.put(SimulatorOptions.JPEG_COMPRESSION_FACTOR,
					(Double) ((JSpinner) e.getSource()).getValue());
		}
		super.stateChanged(e);
	}

}
