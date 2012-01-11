/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.util.StringTools;
import org.sbml.optimization.EvA2GUIStarter;
import org.sbml.optimization.problem.EstimationOptions;
import org.sbml.optimization.problem.EstimationProblem;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.io.CSVDataImporter;
import org.sbml.simulator.io.SimulatorIOOptions;
import org.simulator.math.odes.MultiTable;

import de.zbit.AppConf;
import de.zbit.gui.ActionCommand;
import de.zbit.gui.BaseFrame;
import de.zbit.gui.GUIOptions;
import de.zbit.gui.GUITools;
import de.zbit.gui.ProgressBarSwing;
import de.zbit.io.CSVOptions;
import de.zbit.io.SBFileFilter;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.FileTools;
import de.zbit.util.ResourceManager;
import de.zbit.util.StringUtil;
import de.zbit.util.Timer;
import de.zbit.util.ValuePairUncomparable;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;
import eva2.EvAInfo;
import eva2.client.EvAClient;
import eva2.tools.BasicResourceLoader;

/**
 * Graphical user interface for {@link SBMLsimulator}.
 * 
 * @author Andreas Dr&auml;ger
 * @author Philip Stevens
 * @author Max Zwie&szlig;ele
 * @date 2010-04-15
 * @version $Rev$
 * @since 1.0
 */
public class SimulatorUI extends BaseFrame implements CSVOptions, ItemListener,
		PropertyChangeListener {
  
	/**
	 * Commands that can be understood by this dialog.
	 * 
	 * @author Andreas Dr&auml;ger
	 */
	public static enum Command implements ActionCommand {
		/**
		 * Open experimental data.
		 */
		OPEN_DATA,
		/**
		 * Starts the optimization of the model with respect to given data.
		 */
		OPTIMIZATION,
		/**
		 * Whether or not to display a tool bar below the plot.
		 */
		SHOW_OPTIONS,
		/**
		 * Start a new simulation with the current settings.
		 */
		SIMULATION_START;

		/* (non-Javadoc)
		 * @see de.zbit.gui.ActionCommand#getName()
		 */
		public String getName() {
			String elem = toString();
			if (bundle.containsKey(elem)) {
				return bundle.getString(elem);
			}
			return StringUtil.firstLetterUpperCase(elem.replace('_', ' '));
		}

		/* (non-Javadoc)
		 * @see de.zbit.gui.ActionCommand#getToolTip()
		 */
		public String getToolTip() {
			String elem = toString() + "_TOOLTIP";
			if (bundle.containsKey(elem)) {
				return bundle.getString(elem);
			}
			return StringUtil
					.firstLetterUpperCase(toString().replace('_', ' '));
		}
	}

	/**
   * The resource bundle to be used to display texts to a user.
   */
  private static final transient ResourceBundle bundle = ResourceManager
      .getBundle("org.sbml.simulator.locales.Simulator");

	/**
   * A {@link Logger} for this class.
   */
	private static final Logger logger = Logger.getLogger(SimulatorUI.class.getName());

	/**
	 * Generated serial version identifier
	 */
	private static final long serialVersionUID = -5289766427756813972L;

	static {
		BasicResourceLoader rl = BasicResourceLoader.instance();
		byte[] bytes = rl.getBytesFromResourceLocation(EvAInfo.iconLocation, true);
		UIManager.put("ICON_EVA2", new ImageIcon(Toolkit.getDefaultToolkit().createImage(bytes)));
	}

	/**
	 * GUI element that lets the user run the simulation.
	 */
	private SimulationPanel simPanel;

	/**
	 * 
	 */
	public SimulatorUI() {
		this((AppConf) null);
	}
	
	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#getAlternativeBaseActionNames()
	 */
	@Override
	protected Map<BaseAction, String> getAlternativeBaseActionNames() {
		Map<BaseAction, String> nameMap = new HashMap<BaseFrame.BaseAction, String>();
		nameMap.put(BaseAction.FILE_OPEN, bundle.getString(BaseAction.FILE_OPEN.toString()));
		nameMap.put(BaseAction.FILE_CLOSE, bundle.getString(BaseAction.FILE_CLOSE.toString()));
		return nameMap;
	}

	/**
	 * 
	 * @param appConf
	 */
	public SimulatorUI(AppConf appConf) {
		super(appConf);
		GUITools.setEnabled(false, getJMenuBar(), toolBar, Command.SIMULATION_START);
		SBProperties props = appConf.getCmdArgs();
		if (props.containsKey(SimulatorIOOptions.SBML_INPUT_FILE)) {
			open(new File(props.get(SimulatorIOOptions.SBML_INPUT_FILE).toString()));
			if (props.containsKey(SimulatorIOOptions.TIME_SERIES_FILE)) {
				open(new File(props.get(SimulatorIOOptions.TIME_SERIES_FILE).toString()));
			}
		}
	}
	
	

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#createFileMenu(boolean)
	 */
	@Override
	protected JMenu createFileMenu(boolean loadDefaultFileMenuEntries) {
		JMenu fileMenu = super.createFileMenu(loadDefaultFileMenuEntries);
		JMenuItem openFile = (JMenuItem) GUITools.find(fileMenu, BaseAction.FILE_OPEN);
		JMenuItem openData = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this,
				"openFileAndLogHistory"), Command.OPEN_DATA);
		openData.setEnabled(false);
		fileMenu.remove(openFile);
		JMenu openMenu = GUITools.createJMenu(bundle.getString("OPEN"), bundle.getString("OPEN_TOOLTIP"), openFile, openData);
		openMenu.addActionListener(EventHandler.create(ActionListener.class, this, "openFileAndLogHistory"));
		openMenu.setIcon(openFile.getIcon());
		openMenu.setMnemonic(openFile.getMnemonic());
		openFile.setIcon(null);
		fileMenu.add(openMenu, 0);
		return fileMenu;
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#additionalEditMenuItems()
	 */
	@Override
	protected JMenuItem[] additionalEditMenuItems() {
		// new ImageIcon(SimulatorUI.class.getResource("img/CAMERA_16.png"))
		UIManager.put("PLAY_16", new ImageIcon(SimulatorUI.class.getResource("img/PLAY_16.png")));
		JMenuItem simulation = GUITools.createJMenuItem(
				EventHandler.create(ActionListener.class, this, "simulate"),
				Command.SIMULATION_START, UIManager.getIcon("PLAY_16"), 'S');
		JMenuItem optimization = GUITools.createJMenuItem(
				EventHandler.create(ActionListener.class, this, "optimize"),
				Command.OPTIMIZATION, UIManager.getIcon("ICON_EVA2"), 'O');
		optimization.setEnabled(false);
		JCheckBoxMenuItem item = GUITools.createJCheckBoxMenuItem(
			Command.SHOW_OPTIONS, simPanel != null ? simPanel.isShowSettingsPanel()
					: true, simPanel != null, this);
		return new JMenuItem[] { simulation, optimization, item };
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#closeFile()
	 */
	public boolean closeFile() {
		return closeFile(true);
	}

	/**
	 * 
	 * @param onlyClosing whether or not this method is called with the purpose to open another model right afterwards.
	 * 
	 * @return
	 */
	private boolean closeFile(boolean onlyClosing) {
		if (simPanel != null) {
			String message = bundle.getString("CLOSE_MODEL_WITHOUT_SAVING");
			if (!onlyClosing) {
				message = bundle.getString("CLOSE_MODEL_BEFORE_OPENING_NEXT") + ' ' + message;
			}
			if (GUITools.showQuestionMessage(this, message,
				bundle.getString("CLOSING_MODEL"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				listOfPrefChangeListeners.remove(simPanel.getSimulationToolPanel());
				if (GUITools.contains(getContentPane(), simPanel)) {
					getContentPane().remove(simPanel);
				}
				simPanel = null;
				setTitle(getApplicationName());
				if (onlyClosing) {
					GUITools.swapAccelerator(getJMenuBar(), BaseAction.FILE_OPEN,
						Command.OPEN_DATA);
				}
				GUITools.setEnabled(false, getJMenuBar(), toolBar,
					BaseAction.FILE_CLOSE, BaseAction.FILE_SAVE_AS,
					Command.SIMULATION_START, Command.SHOW_OPTIONS, Command.OPEN_DATA,
					Command.OPTIMIZATION);
				GUITools.setEnabled(true, getJMenuBar(), toolBar, BaseAction.FILE_OPEN);
				repaint();
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#createJToolBar()
	 */
	protected JToolBar createJToolBar() {
		return createDefaultToolBar();
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#createMainComponent()
	 */
	protected Component createMainComponent() {
		this.simPanel = null;
		return simPanel;
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#getURLAboutMessage()
	 */
	public URL getURLAboutMessage() {
		return getClass().getResource("html/about.html");
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#getURLLicense()
	 */
	public URL getURLLicense() {
		return getClass().getResource("html/License.html");
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#getURLOnlineHelp()
	 */
	public URL getURLOnlineHelp() {
		return getClass().getResource("html/online-help.html");
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof JCheckBoxMenuItem) {
			JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
			if ((item.getActionCommand() != null)
					&& (item.getActionCommand().equals(Command.SHOW_OPTIONS
							.toString()))) {
				simPanel.setShowSimulationToolPanel(item.isSelected());
			}
		}
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#openFile(java.io.File[])
	 */
	protected File[] openFile(File... files) {
		File[] modelFiles = null;
		File[] dataFiles = null;
		
		/*
		 * Investigate the given files or let the user open a model and some data:
		 */
		if ((files != null) && (files.length > 0)) {
			// Files are given to this method
			ValuePairUncomparable<List<File>, List<File>> vp = SBFileFilter.createSBMLFileFilter().separate(files);
			modelFiles = vp.getA().toArray(new File[0]);
			if (vp.getB().size() > 0) {
				vp = SBFileFilter.createCSVFileFilter().separate(vp.getB().toArray(new File[0]));
				if (vp.getA().size() > 0) {
					dataFiles = vp.getA().toArray(new File[0]);
				}
				if (vp.getB().size() > 0) {
					GUITools.showListMessage(this,
						bundle.getString("COULD_NOT_OPEN_FILES"),
						bundle.getString("UNSUPPORTED_FILE_TYPES"), vp.getB());
				}
			}
		} else {
			// TODO: also allow ZIP or other archives...
			// No files are given, we have to let the user open a file.
			SBPreferences prefs = SBPreferences.getPreferencesFor(getClass());
			if (simPanel == null) { 
				// Open a model
				modelFiles = GUITools.openFileDialog(this,
					prefs.get(GUIOptions.OPEN_DIR).toString(), false, false,
					JFileChooser.FILES_ONLY, SBFileFilter.createSBMLFileFilterList());
			} else { 
				// Open (experimental) data files
				dataFiles = GUITools.openFileDialog(this,
					prefs.get(GUIOptions.OPEN_DIR), true, false, JFileChooser.FILES_ONLY,
					SBFileFilter.createCSVFileFilter());
			}
		}
		
		/*
		 * Update the graphical user interface given some files (model and maybe data).
		 */
		
		// First the model(s):
		if ((modelFiles != null) && (modelFiles.length > 0)) {
			try {
				// TODO: Do this in a different thread with some progress monitor for the user.
				Timer timer = new Timer();
				SBMLDocument doc = SBMLReader.read(modelFiles[0]);
				logger.info(String.format(bundle.getString("READING_TIME"),
					StringTools.toString(timer.getAndReset(false))));
				if ((doc != null) && (doc.isSetModel())) {
					
					if ((simPanel != null) && !closeFile(false)) {
						return null;
					}
					
					Model model = doc.getModel();
					simPanel = new SimulationPanel(model);
					getContentPane().add(simPanel, BorderLayout.CENTER);
					addPreferenceChangeListener(simPanel.getSimulationToolPanel());
					
					GUITools.swapAccelerator(getJMenuBar(), BaseAction.FILE_OPEN ,Command.OPEN_DATA);
					GUITools.setEnabled(false, getJMenuBar(), BaseAction.FILE_OPEN);
					GUITools.setEnabled(true, getJMenuBar(), toolBar,
						BaseAction.FILE_SAVE_AS, Command.SIMULATION_START,
						Command.SHOW_OPTIONS, Command.OPEN_DATA);
					setTitle(String.format("%s - %s", getApplicationName(),
						model.isSetName() ? model.getName() : modelFiles[0].getName()));
					
				} else {
					JOptionPane.showMessageDialog(this, StringUtil.toHTML(
						String.format(bundle.getString("COULD_NOT_OPEN_MODEL"),
							modelFiles[0].getAbsolutePath()), GUITools.TOOLTIP_LINE_LENGTH));
				}
			} catch (Exception exc) {
				GUITools.showErrorMessage(this, exc);
			}
			if (modelFiles.length > 1) {
				GUITools.showListMessage(this, bundle
						.getString("CAN_ONLY_OPEN_ONE_MODEL_AT_A_TIME"), bundle
						.getString("TOO_MANY_MODEL_FILES"), Arrays.asList(modelFiles)
						.subList(1, modelFiles.length - 1));
			}
		}
		
		// Second: the data
		if ((dataFiles != null) && (dataFiles.length > 0)) {
			if (simPanel != null) {
				Model model = simPanel.getModel();
				CSVDataImporter importer = new CSVDataImporter();
				File openDir = null;
				for (File dataFile : dataFiles) {
					try {
						MultiTable data = importer.convert(model,
							dataFile.getAbsolutePath(), this);
						if (data != null) {
							simPanel.addExperimentalData(
								FileTools.trimExtension(dataFile.getName()), data);
							if (openDir == null) {
								openDir = dataFile.getParentFile();
							}
						}
					} catch (Exception exc) {
						GUITools.showErrorMessage(this, exc);
					}
				}
				if (simPanel.getExperimentalDataCount() > 0) {
					// Optimization should not be available if there is nothing to optimize.
					GUITools.setEnabled(
						model.getNumQuantities() - model.getNumSpeciesReferences() > 0,
						getJMenuBar(), toolBar, Command.OPTIMIZATION);
				}
				if (openDir != null) {
					SBPreferences prefs = SBPreferences.getPreferencesFor(getClass());
					prefs.put(CSVOptions.CSV_FILES_OPEN_DIR, openDir);
				}
			} else {
				// reject data files
				JOptionPane.showMessageDialog(this,
					bundle.getString("CANNOT_OPEN_DATA_WIHTOUT_MODEL"),
					bundle.getString("UNABLE_TO_OPEN_DATA"), JOptionPane.WARNING_MESSAGE);
			}
		}

		// setStatusBarToMemoryUsage();
		validate();
		
		return modelFiles;
	}

	/**
	 * @param file
	 */
	public File[] open(File... files) {
    return openFileAndLogHistory(files);
	}

	/**
	 * 
	 */
	public void optimize() {
		final Model model = simPanel.getModel().getSBMLDocument().clone().getModel();
		final QuantitySelectionPanel panel = new QuantitySelectionPanel(model);
		if (JOptionPane.showConfirmDialog(this, panel,
			bundle.getString("SELECT_QUANTITIES_FOR_OPTIMIZATION"), JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
			simPanel.setAllEnabled(false);
			try {
				simPanel.notifyQuantitiesSelected(panel.getSelectedQuantityIds());
				GUITools.setEnabled(false, getJMenuBar(), toolBar,
					Command.SIMULATION_START, BaseAction.FILE_CLOSE,
					Command.OPTIMIZATION, BaseAction.EDIT_PREFERENCES);
				// windowClosing
				final WindowListener wl = EventHandler.create(WindowListener.class,
					this, "windowClosing");
				final SimulatorUI ui = this;
				new Thread(new Runnable() {
					/* (non-Javadoc)
					 * @see java.lang.Runnable#run()
					 */
					public void run() {
						// TODO: implement org.sbml.optimization.OptimizationWorker to do this job
						try {
							SBPreferences prefs = SBPreferences
									.getPreferencesFor(EstimationOptions.class);
							simPanel.refreshStepSize();
							EvA2GUIStarter.init(
								new EstimationProblem(simPanel.getSolver(), simPanel
										.getDistance(), model, simPanel.getExperimentalData(),
									prefs.getBoolean(EstimationOptions.EST_MULTI_SHOOT), panel
											.getSelectedQuantityRanges()), ui, simPanel,
								wl);
							simPanel.setAllEnabled(true);
						} catch (Throwable exc) {
							GUITools.showErrorMessage(ui, exc);
							simPanel.setAllEnabled(true);
						}
					}
				}).start();
			} catch (Throwable exc) {
				GUITools.showErrorMessage(this, exc);
				simPanel.setAllEnabled(true);
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equalsIgnoreCase("progress")) {
			AbstractProgressBar memoryBar = this.statusBar.showProgress();

			// TODO: find a better place for this
			((ProgressBarSwing) memoryBar).getProgressBar().setStringPainted(
					true);

			int process = (int) Math.round(((Number) evt.getNewValue())
					.doubleValue());
			memoryBar.percentageChanged(process, -1, bundle.getString("COMPUTED"));
		} else if (evt.getPropertyName().equalsIgnoreCase("done")) {
			statusBar.reset();
			GUITools.setEnabled(true, getJMenuBar(), getJToolBar(),
					BaseAction.FILE_SAVE_AS, Command.SIMULATION_START);
		}
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#saveFile()
	 */
	public File saveFile() {
		if (simPanel != null) { 
			return simPanel.saveToFile(); 
		}
		GUITools.showMessage(bundle.getString("NOTHING_TO_SAVE"),
			bundle.getString("INFORMATION"));
		return null;
	}

	/**
	 * @param ids
	 */
	public void setSelectedQuantities(String... ids) {
		simPanel.setSelectedQuantities(ids);
	}

	/**
	 * 
	 */
	public void simulate() {
    try {
      logger.info(bundle.getString("LAUNCHING_SIMULATION"));
      GUITools.setEnabled(false, getJMenuBar(), getJToolBar(), Command.SIMULATION_START);
      simPanel.addPropertyChangedListener(this);
      simPanel.simulate();
    } catch (Exception exc) {
      GUITools.showErrorMessage(this, exc);
    }
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
	 */
	public void windowClosing(WindowEvent e) {
		if (e.getSource() instanceof JFrame) {
			JFrame frame = (JFrame) e.getSource();
			if ((frame.getName() != null)
					&& (frame.getName().equals(EvAClient.class.getSimpleName()))) {
				simPanel.setAllEnabled(true);
				GUITools.setEnabled(true, getJMenuBar(), toolBar,
						BaseAction.FILE_SAVE_AS, Command.SIMULATION_START, BaseAction.FILE_CLOSE,
						Command.OPTIMIZATION, BaseAction.EDIT_PREFERENCES);
			}
		}
		logger.finer(bundle.getString("RECEIVED_WINDOW_EVENT"));
	}
	
}
