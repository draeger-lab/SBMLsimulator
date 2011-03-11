/*
 * $Id$
 * $URL:
 * svn://rarepos/SBMLsimulator/trunk/src/org/sbml/simulator/gui/SimulatorUI.java
 * $
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.prefs.BackingStoreException;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.sbml.jsbml.xml.stax.SBMLWriter;
import org.sbml.optimization.EvA2GUIStarter;
import org.sbml.optimization.problem.EstimationProblem;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.SimulatorOptions;
import org.sbml.simulator.math.odes.MultiBlockTable;
import org.sbml.simulator.resources.Resource;

import de.zbit.gui.ActionCommand;
import de.zbit.gui.BaseFrame;
import de.zbit.gui.GUIOptions;
import de.zbit.gui.prefs.FileHistory;
import de.zbit.io.SBFileFilter;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.SBPreferences;
import eva2.client.EvAClient;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-04-15
 * @version $Rev$
 * @since 1.0
 */
public class SimulatorUI extends BaseFrame implements ActionListener,
	ItemListener, WindowListener {

    /**
     * Commands that can be understood by this dialog.
     * 
     * @author Andreas Dr&auml;ger
     */
    public static enum Command implements ActionCommand {
	/**
	 * Starts the optimization of the model with respect to given data.
	 */
	OPTIMIZATION,
	/**
	 * Save the plot as an image.
	 */
	SAVE_PLOT_IMAGE,
	/**
	 * Save the results of the simulation to a CSV file.
	 */
	SAVE_SIMULATION,
	/**
	 * Save the optimized model in an SBML file
	 */
	SAVE_MODEL,
	/**
	 * Start a new simulation with the current settings.
	 */
	SIMULATION_START,
	/**
	 * Whether or not to display a tool bar below the plot.
	 */
	SHOW_OPTIONS;

	/*
	 * (non-Javadoc)
	 * @see de.zbit.gui.ActionCommand#getName()
	 */
	public String getName() {
	    switch (this) {
	    case OPTIMIZATION:
		return "Optimization";
	    case SAVE_MODEL:
		return "Save model";
	    case SAVE_PLOT_IMAGE:
		return "Save plot image";
	    case SAVE_SIMULATION:
		return "Save simulation results";
	    case SHOW_OPTIONS:
		return "Show options";
	    case SIMULATION_START:
		return "Start simulation";
	    default:
		return StringUtil.firstLetterUpperCase(toString().replace('_',
		    ' '));
	    }
	}

	/*
	 * (non-Javadoc)
	 * @see de.zbit.gui.ActionCommand#getToolTip()
	 */
	public String getToolTip() {
	    switch (this) {
	    case OPTIMIZATION:
		return "Launches the optimization of the quantities in the current model.";
//		"Starts the optimization of the model with respect to given experimental data."
	    case SAVE_MODEL:
		return "Saves the model including estimated values.";
	    case SAVE_PLOT_IMAGE:
		return "Saves the plot as an image file.";
//		"Save plot in an image."
	    case SAVE_SIMULATION:
		return "Saves the simulation results in a comma separated text file.";
//		"Save simulation results to file."
	    case SHOW_OPTIONS:
		return "Decide whether or not to display the options.";
	    case SIMULATION_START:
		return "Starts the dynamic simulation.";
//		"Perform a simulation run with the current settings."
	    default:
		return StringUtil.firstLetterUpperCase(toString().replace('_',
		    ' '));
	    }
	}
    }

    /**
     * Generated serial version identifier
     */
    private static final long serialVersionUID = -5289766427756813972L;

    /**
     * GUI element that lets the user run the simulation.
     */
    private SimulationPanel simPanel;

    /**
     * 
     */
    public SimulatorUI() {
	super();
	loadPreferences();
	// getContentPane().add(new StatusBar(), BorderLayout.SOUTH);
	setOptimalSize();
    }

    /**
     * @param owner
     * @param model
     */
    public SimulatorUI(Model model) {
	this(model, new SimulationPanel(model));
    }

    /**
     * @param owner
     * @param model
     * @param simulationPanel
     */
    public SimulatorUI(Model model, SimulationPanel simulationPanel) {
	this();
	simPanel = simulationPanel;
	getContentPane().add(simPanel, BorderLayout.CENTER);
	setOptimalSize();
	setLocationRelativeTo(null);
    }

    /*
     * (non-Javadoc)
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
	if (e.getActionCommand() == null) {
	    return;
	}
	switch (Command.valueOf(e.getActionCommand())) {
	case SIMULATION_START:
	    simulate();
	    break;
	case SAVE_PLOT_IMAGE:
	    simPanel.savePlotImage();
	    break;
	case SAVE_SIMULATION:
	    simPanel.saveSimulationResults();
	    break;
	case SAVE_MODEL:
	    saveModel(simPanel.getModel());
	    break;
	case OPTIMIZATION:
	    new Thread(new Runnable() {
		public void run() {
		    optimize();
		}
	    }).start();
	    break;
	case SHOW_OPTIONS:
	    // treated in {@link #itemStateChanged(ItemEvent)
	    break;
	default:
	    JOptionPane.showMessageDialog(this, "Invalid option "
		    + e.getActionCommand(), "Warning",
		JOptionPane.WARNING_MESSAGE);
	    break;
	}
    }

    
    
    /* (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#preferences()
     */
    @Override
    public boolean preferences() {
	boolean status = super.preferences();
	if (status && (simPanel != null)) {
	    try {
		simPanel.loadPreferences();
	    } catch (Exception exc) {
		GUITools.showErrorMessage(this, exc);
	    }
	}
	return status;
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
		SBMLWriter writer = new SBMLWriter();
		writer.write(model.getSBMLDocument(), f, SBMLsimulator.class
			.getSimpleName(), SBMLsimulator.getVersionNumber());
		// TODO Just for debugging:
		writer.write(model.getSBMLDocument(), System.out);
		prefs.put(GUIOptions.SAVE_DIR, f.getParent());
	    } catch (Exception exc) {
		exc.printStackTrace();
		GUITools.showErrorMessage(this, exc);
	    }
	}
    }
    
    

    /* (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#additionalFileMenuItems()
     */
    @Override
    protected JMenuItem[] additionalFileMenuItems() {
	JMenuItem saveModel = GUITools.createJMenuItem(this,
	    Command.SAVE_MODEL, GUITools.getIconSave(), KeyStroke.getKeyStroke(
		'S', InputEvent.CTRL_DOWN_MASK), 'S', false);
	JMenuItem saveSimItem = GUITools.createJMenuItem(this,
	    Command.SAVE_SIMULATION, GUITools.getIconSave(), KeyStroke
		    .getKeyStroke('E', InputEvent.CTRL_DOWN_MASK));
	JMenuItem savePlotItem = GUITools.createJMenuItem(this,
	    Command.SAVE_PLOT_IMAGE, GUITools.getIconCamera(), KeyStroke
		    .getKeyStroke('S', KeyEvent.ALT_DOWN_MASK));	
	return new JMenuItem[] {saveModel, saveSimItem, savePlotItem};
    }
    
    

    /* (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#additionalEditMenuItems()
     */
    @Override
    protected JMenuItem[] additionalEditMenuItems() {
	JMenuItem simulation = GUITools.createJMenuItem(this,
	    Command.SIMULATION_START, GUITools.getIconGear(), 'S');
	JMenuItem optimization = GUITools.createJMenuItem(this,
	    Command.OPTIMIZATION, GUITools.getIconEvA2(), 'O');
	optimization.setEnabled(false);
	JCheckBoxMenuItem item = new JCheckBoxMenuItem("Show options",
	    simPanel != null ? simPanel.isShowSettingsPanel() : true);
	if (simPanel == null) {
	    item.setEnabled(false);
	}
	item.setActionCommand(Command.SHOW_OPTIONS.toString());
	item.addItemListener(this);
	return new JMenuItem[] {simulation, optimization, item};
    }

    /*
     * (non-Javadoc)
     * @see
     * java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
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
	JFileChooser chooser = GUITools.createJFileChooser(prefs.get(
	    GUIOptions.OPEN_DIR).toString(), false, false,
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
		MultiBlockTable data = importer.convert(model, file
			.getAbsolutePath());
		if (data != null) {
		    simPanel.setExperimentalData(data);
		    GUITools.setEnabled(false, getJMenuBar(), toolBar,
			BaseAction.FILE_OPEN);
		    // Optimization should not be available if there is nothing
		    // to
		    // optimize.
		    GUITools.setEnabled(model.getNumQuantities()
			    - model.getNumSpeciesReferences() > 0,
			getJMenuBar(), toolBar, Command.OPTIMIZATION);
		}
		SBPreferences prefs = SBPreferences
			.getPreferencesFor(GUIOptions.class);
		prefs.put(GUIOptions.OPEN_DIR, file.getParent());
	    } catch (Exception exc) {
		exc.printStackTrace();
		GUITools.showErrorMessage(this, exc);
	    }
	}
    }

    /**
     * @param path
     */
    public void openExperimentalData(String path) {
	try {
	    openExperimentalData(new File(path));
	} catch (Exception exc) {
	    exc.printStackTrace();
	    GUITools.showErrorMessage(this, exc);
	}
    }

    /**
     * 
     */
    public File[] openModel() {
	SBPreferences prefs = SBPreferences.getPreferencesFor(GUIOptions.class);
	File files[] = GUITools.openFileDialog(this, prefs.get(
	    GUIOptions.OPEN_DIR).toString(), false, false,
	    JFileChooser.FILES_ONLY, SBFileFilter.createSBMLFileFilter());
	if ((files != null) && (files.length == 1)) {
	    return openModel(files[0]);
	}
	return null;
    }

    /**
     * @param file
     */
    public File[] openModel(File file) {
	if (file != null) {
	    try {
		SBMLReader reader = new SBMLReader();
		SBMLDocument doc = reader.readSBML(file);
		if ((doc != null) && (doc.isSetModel())) {
		    SBPreferences prefs = SBPreferences
			    .getPreferencesFor(GUIOptions.class);
		    prefs.put(GUIOptions.OPEN_DIR, file.getParent());
		    simPanel = new SimulationPanel(doc.getModel());
		    if (GUITools.contains(getContentPane(), simPanel)) {
			getContentPane().remove(simPanel);
		    }
		    getContentPane().add(simPanel, BorderLayout.CENTER);
		    setOptimalSize();
		    setTitle(getApplicationName() + " - " + file.getName());
		    GUITools.setEnabled(true, getJMenuBar(), toolBar,
			Command.SAVE_PLOT_IMAGE, Command.SAVE_MODEL,
			Command.SIMULATION_START, Command.SHOW_OPTIONS);
		    return new File[] {file};
		}
		JOptionPane.showMessageDialog(this, StringUtil.toHTML(
		    "Could not open model " + file.getAbsolutePath(), 60));
	    } catch (Exception exc) {
		exc.printStackTrace();
		GUITools.showErrorMessage(this, exc);
	    }
	}
	return null;
    }

    /* (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#getFileHistoryKeyProvider()
     */
    @Override
    public Class<? extends FileHistory> getFileHistoryKeyProvider() {
	return ModelHistory.class;
    }

    /**
     * Resizes this {@link JFrame} to an appropriate size.
     */
    private void setOptimalSize() {
	pack();
	setMinimumSize(new Dimension(640, 480));
	int maxSize = 700;
	if (getWidth() > 1.5d * maxSize) {
	    this.setSize((int) Math.round(1.5d * maxSize), getHeight());
	}
	if (getHeight() > maxSize) {
	    this.setSize(getWidth(), maxSize);
	}
    }

    /**
     * 
     */
    public void loadPreferences() {
	if (simPanel != null) {
	    try {
		simPanel.loadPreferences();
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
     * 
     */
    public void simulate() {
	try {
	    GUITools.setEnabled(false, getJMenuBar(), toolBar,
		Command.SIMULATION_START);
	    simPanel.simulate();
	    GUITools.setEnabled(true, getJMenuBar(), toolBar,
		Command.SAVE_SIMULATION, Command.SAVE_PLOT_IMAGE,
		Command.SIMULATION_START);
	} catch (Exception exc) {
	    exc.printStackTrace();
	    GUITools.showErrorMessage(this, exc);
	}
    }

    /**
     * 
     */
    public void optimize() {
	Model model = simPanel.getModel().getSBMLDocument().clone().getModel();
	QuantitySelectionPanel panel = new QuantitySelectionPanel(model);
	if (JOptionPane.showConfirmDialog(this, panel,
	    "Select quantities for optimization", JOptionPane.OK_CANCEL_OPTION,
	    JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
	    simPanel.setAllEnabled(false);
	    try {
		simPanel.notifyQuantitiesSelected(panel
			.getSelectedQuantityIds());
		GUITools.setEnabled(false, getJMenuBar(), toolBar,
		    Command.SIMULATION_START, BaseAction.FILE_CLOSE,
		    Command.OPTIMIZATION, BaseAction.EDIT_PREFERENCES);
		SBPreferences prefs = SBPreferences
			.getPreferencesFor(SimulatorOptions.class);
		EvA2GUIStarter.init(new EstimationProblem(simPanel.getSolver(),
		    simPanel.getDistance(), model, simPanel
			    .getExperimentalData(), prefs
			    .getBoolean(SimulatorOptions.EST_MULTI_SHOOT),
		    panel.getSelectedQuantityRanges()), this, simPanel, this);
	    } catch (Throwable exc) {
		exc.printStackTrace();
		GUITools.showErrorMessage(this, exc);
		simPanel.setAllEnabled(true);
	    }
	}
    }

    /*
     * (non-Javadoc)
     * @see
     * java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
     */
    public void windowActivated(WindowEvent e) {
    }

    /*
     * (non-Javadoc)
     * @see
     * java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
     */
    public void windowClosed(WindowEvent e) {
    }

    /*
     * (non-Javadoc)
     * @see
     * java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
     */
    public void windowClosing(WindowEvent e) {
	if (e.getSource() instanceof JFrame) {
	    JFrame frame = (JFrame) e.getSource();
	    if ((frame.getName() != null)
		    && (frame.getName().equals(EvAClient.class.getSimpleName()))) {
		simPanel.setAllEnabled(true);
		GUITools.setEnabled(true, getJMenuBar(), toolBar,
		    Command.SAVE_MODEL, Command.SIMULATION_START,
		    BaseAction.FILE_CLOSE, Command.OPTIMIZATION,
		    BaseAction.EDIT_PREFERENCES);
	    }
	}
    }

    /*
     * (non-Javadoc)
     * @see
     * 
     * java.awt.event.WindowListener#windowDeactivated(java.awt.event.WindowEvent
     * )
     */
    public void windowDeactivated(WindowEvent e) {
    }

    /*
     * (non-Javadoc)
     * @see
     * 
     * java.awt.event.WindowListener#windowDeiconified(java.awt.event.WindowEvent
     * )
     */
    public void windowDeiconified(WindowEvent e) {
    }

    /*
     * (non-Javadoc)
     * @see
     * java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
     */
    public void windowIconified(WindowEvent e) {
    }

    /*
     * (non-Javadoc)
     * @see
     * java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
     */
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public boolean closeFile() {
	if (simPanel != null) {
	    if (simPanel.isSetExperimentalData()) {
		simPanel.closeExperimentalData();
		GUITools.setEnabled(false, getJMenuBar(), toolBar,
		    Command.OPTIMIZATION, Command.SAVE_PLOT_IMAGE,
		    Command.SAVE_SIMULATION, Command.SIMULATION_START);
	    } else {
		getContentPane().remove(simPanel);
		simPanel = null;
		GUITools.setEnabled(false, getJMenuBar(), toolBar,
		    BaseAction.FILE_CLOSE, Command.SAVE_PLOT_IMAGE,
		    Command.SIMULATION_START);
		setOptimalSize();
		setTitle(getApplicationName());
	    }
	    GUITools.setEnabled(true, getJMenuBar(), toolBar, BaseAction.FILE_OPEN);
	}
	if (simPanel == null) {
	    GUITools.setEnabled(false, getJMenuBar(), toolBar,
		Command.SHOW_OPTIONS, Command.SAVE_MODEL);
	}
	return false;
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#createJToolBar()
     */
    @Override
    protected JToolBar createJToolBar() {
	return createDefaultToolBar();
    }

    @Override
    protected Component createMainComponent() {
	// TODO Auto-generated method stub
	return null;
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#exit()
     */
    @Override
    public void exit() {
	try {
	    SBPreferences prefs = SBPreferences
		    .getPreferencesFor(SimulatorOptions.class);
	    if (simPanel != null) {
		prefs.putAll(simPanel.getProperties());
	    }
	    prefs.flush();
	} catch (BackingStoreException exc) {
	    GUITools.showErrorMessage(this, exc);
	}
	System.exit(0);
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#getApplicationName()
     */
    @Override
    public String getApplicationName() {
	return SBMLsimulator.class.getSimpleName();
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#getCommandLineOptions()
     */
    @Override
    public Class<? extends KeyProvider>[] getCommandLineOptions() {
	return SBMLsimulator.getCommandLineOptions();
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#getDottedVersionNumber()
     */
    @Override
    public String getDottedVersionNumber() {
	return SBMLsimulator.getVersionNumber();
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#getMaximalFileHistorySize()
     */
    @Override
    public short getMaximalFileHistorySize() {
	return 10;
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#getURLAboutMessage()
     */
    @Override
    public URL getURLAboutMessage() {
	return Resource.class.getResource("html/about.html");
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#getURLLicense()
     */
    @Override
    public URL getURLLicense() {
	return Resource.class.getResource("html/License.html");
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#getURLOnlineHelp()
     */
    @Override
    public URL getURLOnlineHelp() {
	return Resource.class.getResource("html/online-help.html");
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#getURLOnlineUpdate()
     */
    @Override
    public URL getURLOnlineUpdate() {
	try {
	    return SBMLsimulator.getURLOnlineUpdate();
	} catch (MalformedURLException exc) {
	    return null;
	}
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.BaseFrame#openFile(java.io.File[])
     */
    @Override
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

    @Override
    public void saveFile() {
	if (simPanel != null) {
	    // TODO Auto-generated method stub
	}
    }
}
