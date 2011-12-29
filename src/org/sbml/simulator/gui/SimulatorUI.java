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
import java.awt.Dimension;
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
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.util.StringTools;
import org.sbml.optimization.EvA2GUIStarter;
import org.sbml.optimization.problem.EstimationOptions;
import org.sbml.optimization.problem.EstimationProblem;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.tolatex.LaTeXOptions;
import org.simulator.math.odes.MultiTable;

import de.zbit.AppConf;
import de.zbit.gui.ActionCommand;
import de.zbit.gui.BaseFrame;
import de.zbit.gui.GUIOptions;
import de.zbit.gui.GUITools;
import de.zbit.gui.JOptionPane2;
import de.zbit.gui.ProgressBarSwing;
import de.zbit.io.SBFileFilter;
import de.zbit.sbml.gui.SBMLModelSplitPane;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.ResourceManager;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.SBPreferences;
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
public class SimulatorUI extends BaseFrame implements ItemListener,
    PropertyChangeListener {
  
	/**
	 * Commands that can be understood by this dialog.
	 * 
	 * @author Andreas Dr&auml;ger
	 */
	public static enum Command implements ActionCommand {
		/**
		 * Option to change the content of the model
		 */
		EDIT_MODEL,
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
	private static final Logger logger = Logger.getLogger(SimulatorUI.class
			.getName());

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
	 * 
	 */
	private Model model;

	/**
	 * 
	 */
	private QuantitySelectionPanel panel;

	/**
	 * GUI element that lets the user run the simulation.
	 */
	private SimulationPanel simPanel;
	
	/**
	 * 
	 */
	private long simulationTime;

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
		GUITools.setEnabled(false, getJMenuBar(), toolBar, Command.EDIT_MODEL,
				Command.SIMULATION_START);
		setStatusBarToMemoryUsage();
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#additionalEditMenuItems()
	 */
	@Override
	protected JMenuItem[] additionalEditMenuItems() {
		// new ImageIcon(SimulatorUI.class.getResource("img/CAMERA_16.png"))
		UIManager.put("PLAY_16", new ImageIcon(SimulatorUI.class.getResource("img/PLAY_16.png")));
		JMenuItem editModel = GUITools.createJMenuItem(
				EventHandler.create(ActionListener.class, this, "editModel"),
				Command.EDIT_MODEL, UIManager.getIcon("ICON_PENCIL_16"), 'M');
		JMenuItem simulation = GUITools.createJMenuItem(
				EventHandler.create(ActionListener.class, this, "simulate"),
				Command.SIMULATION_START, UIManager.getIcon("PLAY_16"), 'S');
		JMenuItem optimization = GUITools.createJMenuItem(
				EventHandler.create(ActionListener.class, this, "optimize"),
				Command.OPTIMIZATION, UIManager.getIcon("ICON_EVA2"), 'O');
		optimization.setEnabled(false);
		JCheckBoxMenuItem item = new JCheckBoxMenuItem("Show options",
				simPanel != null ? simPanel.isShowSettingsPanel() : true);
		if (simPanel == null) {
			item.setEnabled(false);
		}
		item.setActionCommand(Command.SHOW_OPTIONS.toString());
		item.addItemListener(this);
		return new JMenuItem[] { editModel, simulation, optimization, item };
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#closeFile()
	 */
	public boolean closeFile() {
	  boolean retVal = false;
		if (simPanel != null) {
			if (simPanel.isSetExperimentalData()) {
				simPanel.closeExperimentalData();
				GUITools.setEnabled(false, getJMenuBar(), toolBar,
						Command.OPTIMIZATION, BaseAction.FILE_SAVE_AS);
			} else {
				getContentPane().remove(simPanel);
				simPanel = null;
				GUITools.setEnabled(false, getJMenuBar(), toolBar,
						BaseAction.FILE_CLOSE, BaseAction.FILE_SAVE_AS,
						Command.EDIT_MODEL, Command.SIMULATION_START);
				setTitle(getApplicationName());
			}
			GUITools.setEnabled(true, getJMenuBar(), toolBar,
					BaseAction.FILE_OPEN);
			retVal = true;
		}
		if (simPanel == null) {
			GUITools.setEnabled(false, getJMenuBar(), toolBar,
					Command.SHOW_OPTIONS, BaseAction.FILE_SAVE_AS);
			retVal = true;
		}
		if (retVal) {
		  repaint();
		}
		return retVal;
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
		return null;
	}

	/**
	 * 
	 */
	public void editModel() {
		logger.info("Starting model editor");
		try {
			// Cloning is necessary, because the model might be changed.
			// If not, we want to stick with the previous version of the
			// model.
			SBMLDocument doc = simPanel.getModel().getSBMLDocument().clone();
			SBMLModelSplitPane split = new SBMLModelSplitPane(doc,
					SBPreferences.getPreferencesFor(LaTeXOptions.class)
							.getBoolean(LaTeXOptions.PRINT_NAMES_IF_AVAILABLE));
			split.setPreferredSize(new Dimension(640, 480));
			if (JOptionPane2.showOptionDialog(this, split, "Model Editor",
					JOptionPane2.OK_CANCEL_OPTION,
					JOptionPane2.INFORMATION_MESSAGE, null, null, null, true) == JOptionPane2.OK_OPTION) {
		    // TODO: remove older Pref Listeners!
				simPanel = new SimulationPanel(doc.getModel());
				addPreferenceChangeListener(simPanel.getSimulationToolPanel());
				validate();
			}
		} catch (Throwable exc) {
			GUITools.showErrorMessage(this, exc);
		}
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
				simPanel.setShowSettingsPanel(item.isSelected());
			}
		}
	}

	/**
	 * 
	 */
	public void openExperimentalData() {
		SBPreferences prefs = SBPreferences.getPreferencesFor(GUIOptions.class);
		JFileChooser chooser = GUITools.createJFileChooser(
				prefs.get(GUIOptions.OPEN_DIR).toString(), false, false,
				JFileChooser.FILES_ONLY, SBFileFilter.createCSVFileFilter());
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			openExperimentalData(chooser.getSelectedFile());
		}
	}

	/**
	 * @param file
	 */
	public void openExperimentalData(File file) {
		CSVDataImporter importer = new CSVDataImporter();
		if (simPanel != null) {
			Model model = simPanel.getModel();
			try {
        MultiTable data = importer.convert(model, file.getAbsolutePath(), this);
				if (data != null) {
					simPanel.setExperimentalData(data);
					GUITools.setEnabled(false, getJMenuBar(), toolBar,
							BaseAction.FILE_OPEN);
					// Optimization should not be available if there is nothing
					// to
					// optimize.
					GUITools.setEnabled(
							model.getNumQuantities()
									- model.getNumSpeciesReferences() > 0,
							getJMenuBar(), toolBar, Command.OPTIMIZATION);
				}
				SBPreferences prefs = SBPreferences
						.getPreferencesFor(GUIOptions.class);
				prefs.put(GUIOptions.OPEN_DIR, file.getParent());
			} catch (Exception exc) {
				GUITools.showErrorMessage(this, exc);
			} finally {
				// setStatusBarToMemoryUsage();
			}
		}
	}

	/**
	 * @param path
	 */
	public void openExperimentalData(String path) {
		openExperimentalData(new File(path));
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#openFile(java.io.File[])
	 */
	protected File[] openFile(File... files) {
		if (simPanel == null) {
			if ((files != null) && (files.length > 0)) {
				return openModel(files[0]);
			} else {
				return openModel();
			}
		} else {
			openExperimentalData();
		}
		return null;
	}

	/**
	 * 
	 * @return
	 */
	public File[] openModel() {
		SBPreferences prefs = SBPreferences.getPreferencesFor(GUIOptions.class);
		File files[] = GUITools.openFileDialog(this,
				prefs.get(GUIOptions.OPEN_DIR).toString(), false, false,
				JFileChooser.FILES_ONLY,
				SBFileFilter.createSBMLFileFilterList());
		if ((files != null) && (files.length == 1)) {
			return openModel(files[0]);
		}
		return null;
	}

	/**
	 * @param file
	 */
	public File[] openModel(File file) {
    try {
      SBMLDocument doc = SBMLReader.read(file);
      if ((doc != null) && (doc.isSetModel())) {
        SBPreferences prefs = SBPreferences.getPreferencesFor(GUIOptions.class);
        prefs.put(GUIOptions.OPEN_DIR, file.getParent());
        setTitle(getApplicationName() + " - " + file.getName());
        openModel(doc.getModel());
        return new File[] { file };
      }
      JOptionPane.showMessageDialog(this, StringUtil.toHTML(
        "Could not open model " + file.getAbsolutePath(),
        GUITools.TOOLTIP_LINE_LENGTH));
    } catch (Exception exc) {
      GUITools.showErrorMessage(this, exc);
    } finally {
      // setStatusBarToMemoryUsage();
    }
		return null;
	}

	/**
	 * @param model
	 */
	public void openModel(Model model) {
    if (model.isSetName() || model.isSetId()) {
      setTitle(getApplicationName() + " - "
          + (model.isSetName() ? model.getName() : model.getId()));
    }
    // TODO: remove older Pref Listeners!
		simPanel = new SimulationPanel(model);
		if (GUITools.contains(getContentPane(), simPanel)) {
			getContentPane().remove(simPanel);
		}
		getContentPane().add(simPanel, BorderLayout.CENTER);
		validate();
		GUITools.setEnabled(true, getJMenuBar(), toolBar,
			BaseAction.FILE_SAVE_AS, Command.EDIT_MODEL,
			Command.SIMULATION_START, Command.SHOW_OPTIONS);
		addPreferenceChangeListener(simPanel.getSimulationToolPanel());
	}

	/**
	 * 
	 */
	public void optimize() {
		model = simPanel.getModel().getSBMLDocument().clone().getModel();
		panel = new QuantitySelectionPanel(model);
		if (JOptionPane.showConfirmDialog(this, panel,
			"Select quantities for optimization", JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
			simPanel.setAllEnabled(false);
			try {
				simPanel.savePreferences();
				simPanel.notifyQuantitiesSelected(panel.getSelectedQuantityIds());
				GUITools.setEnabled(false, getJMenuBar(), toolBar, Command.EDIT_MODEL,
					Command.SIMULATION_START, BaseAction.FILE_CLOSE,
					Command.OPTIMIZATION, BaseAction.EDIT_PREFERENCES);
				// windowClosing
				final WindowListener wl = EventHandler.create(WindowListener.class,
					this, "windowClosing", "");
				final SimulatorUI ui = this;
				new Thread(new Runnable() {
					/*
					 * (non-Javadoc)
					 * @see java.lang.Runnable#run()
					 */
					public void run() {
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

			// TODO: Bessere stelle finden
			((ProgressBarSwing) memoryBar).getProgressBar().setStringPainted(
					true);

			int process = (int) Math.round(((Number) evt.getNewValue())
					.doubleValue());
			memoryBar.percentageChanged(process, -1, "computed");
			// logger.log(Level.INFO, String.format("Simulating... %1$3s%%",
			// process));
		} else if (evt.getPropertyName().equalsIgnoreCase("done")) {
			this.simulationTime = System.currentTimeMillis()
					- this.simulationTime;
			logger.info(String.format("Simulation time: %s s",
				StringTools.toString(simulationTime * 1E-3d)));
			this.statusBar.reset();
			// setStatusBarToMemoryUsage();
			GUITools.setEnabled(true, getJMenuBar(), getJToolBar(),
					BaseAction.FILE_SAVE_AS, Command.EDIT_MODEL,
					Command.SIMULATION_START);
		}
	}

	/* (non-Javadoc)
	 * @see de.zbit.gui.BaseFrame#saveFile()
	 */
	public void saveFile() {
		if (simPanel != null) {
			// TODO Auto-generated method stub
			// simPanel.savePlotImage();
			simPanel.saveSimulationResults();
			saveModel(simPanel.getModel());
		}
	}

	/**
	 * @param model
	 */
	public void saveModel(Model model) {
		SBPreferences prefs = SBPreferences.getPreferencesFor(GUIOptions.class);
		File f = GUITools.saveFileDialog(this, prefs.get(GUIOptions.SAVE_DIR)
				.toString(), false, false, JFileChooser.FILES_ONLY,
				SBFileFilter.createSBMLFileFilter());
		if (f != null) {
			try {
			  SBMLWriter.write(model.getSBMLDocument(), f,
						SBMLsimulator.class.getSimpleName(),
						getDottedVersionNumber());
				prefs.put(GUIOptions.SAVE_DIR, f.getParent());
			} catch (Exception exc) {
				GUITools.showErrorMessage(this, exc);
			}
		}
	}

	/**
	 * @param ids
	 */
	public void setSelectedQuantities(String... ids) {
		simPanel.setSelectedQuantities(ids);
	}

	/**
	 * add progressbar to the statusbar representing the current memory usage
	 * method should be called after all import operations
	 */
	private void setStatusBarToMemoryUsage() {
		// AbstractProgressBar memoryBar = this.statusBar.showProgress();
		int use = (int) ((1f - ((float) Runtime.getRuntime().freeMemory() / (float) Runtime
				.getRuntime().totalMemory())) * 100);
		// statusBar.log.info(String.format("Memory usage: %1$3s%%", use));
		// memoryBar.percentageChanged(use, -1, "of memory used");

		// Create a smaller panel for the statusBar
		Dimension panelSize = new Dimension(100, 15);
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setPreferredSize(panelSize);
		JProgressBar progress = new JProgressBar(0, 100);
		progress.setValue(use);
		progress.setPreferredSize(new Dimension(panelSize.width,
				panelSize.height));
		panel.add(progress);

		getStatusBar().add(panel, BorderLayout.EAST);
	}

	/**
	 * 
	 */
	public void simulate() {
    try {
      logger.info("Starting simulation");
      GUITools.setEnabled(false, getJMenuBar(), getJToolBar(),
        Command.EDIT_MODEL, Command.SIMULATION_START);
      simPanel.addPropertyChangedListener(this);
      simulationTime = System.currentTimeMillis();
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
						BaseAction.FILE_SAVE_AS, Command.EDIT_MODEL,
						Command.SIMULATION_START, BaseAction.FILE_CLOSE,
						Command.OPTIMIZATION, BaseAction.EDIT_PREFERENCES);
			}
		}
		logger.finer("Clicked OK/Cancel");
	}
	
}
