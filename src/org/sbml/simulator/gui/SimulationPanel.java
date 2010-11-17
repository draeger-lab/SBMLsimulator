/*
 * SBMLsqueezer creates rate equations for reactions in SBML files
 * (http://sbml.org).
 * Copyright (C) 2009 ZBIT, University of Tübingen, Andreas Dräger
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.sbml.simulator.gui;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.table.TableModel;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.Quantity;
import org.sbml.optimization.problem.EstimationProblem;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.SimulatorOptions;
import org.sbml.simulator.math.Distance;
import org.sbml.simulator.math.odes.DESSolver;
import org.sbml.simulator.math.odes.MultiBlockTable;

import de.zbit.gui.GUITools;
import de.zbit.io.CSVOptions;
import de.zbit.io.CSVWriter;
import de.zbit.io.SBFileFilter;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;
import eva2.server.go.problems.AbstractOptimizationProblem;
import eva2.server.stat.GraphSelectionEnum;
import eva2.server.stat.InterfaceStatisticsListener;

/**
 * @author Andreas Dr&auml;ger
 * @since 1.4
 * @date 2010-04-06
 */
public class SimulationPanel extends JPanel implements
	InterfaceStatisticsListener {

    /**
     * Generated serial version identifier
     */
    private static final long serialVersionUID = -7278034514446047207L;

    /**
	 * 
	 */
    private static SBProperties getDefaultProperties() {
	SBProperties p = new SBProperties();
	Double maxVal = Double.valueOf(1E5);
	/*
	 * Simulation
	 */
	p.put(SimulatorOptions.SIM_MAX_TIME, maxVal);
	p.put(SimulatorOptions.SIM_START_TIME, Double.valueOf(0));
	p.put(SimulatorOptions.SIM_END_TIME, Double.valueOf(5));
	p.put(SimulatorOptions.SIM_STEP_SIZE, Double.valueOf(.01));
	p.put(SimulatorOptions.SIM_MAX_COMPARTMENT_SIZE, maxVal);
	p.put(SimulatorOptions.SIM_MAX_SPECIES_VALUE, maxVal);
	p.put(SimulatorOptions.SIM_MAX_PARAMETER_VALUE, maxVal);
	p.put(SimulatorOptions.SIM_MAX_STEPS_PER_UNIT_TIME, Integer
		.valueOf(500));
	p.put(SimulatorOptions.SIM_DISTANCE_FUNCTION, SBMLsimulator
		.getAvailableDistances()[0].getName());
	p.put(SimulatorOptions.SIM_ODE_SOLVER, SBMLsimulator
		.getAvailableSolvers()[0].getName());
	p.put(SimulatorOptions.OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE, Double
		.valueOf(1d));
	p.put(SimulatorOptions.OPT_DEFAULT_SPECIES_INITIAL_VALUE, Double
		.valueOf(1d));
	p.put(SimulatorOptions.OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS, Double
		.valueOf(1d));

	/*
	 * Plot
	 */
	p.put(SimulatorOptions.PLOT_SHOW_GRID, Boolean.valueOf(true));
	p.put(SimulatorOptions.PLOT_SHOW_LEGEND, Boolean.valueOf(true));
	p.put(SimulatorOptions.PLOT_LOG_SCALE, Boolean.valueOf(false));
	p.put(SimulatorOptions.PLOT_SHOW_TOOLTIPS, Boolean.valueOf(false));

	/*
	 * CSV file parsing
	 */
	p.put(CSVOptions.CSV_FILES_OPEN_DIR, System.getProperty("user.home"));
	p.put(CSVOptions.CSV_FILES_SEPARATOR_CHAR, Character.valueOf(','));
	p.put(CSVOptions.CSV_FILES_QUOTE_CHAR, Character.valueOf('\''));
	p.put(CSVOptions.CSV_FILES_SAVE_DIR, System.getProperty("user.home"));

	/*
	 * General settings
	 */
	p.put(GUIOptions.SPINNER_STEP_SIZE, Double.valueOf(.01d));
	p.put(SimulatorOptions.JPEG_COMPRESSION_FACTOR, Float.valueOf(.8f));
	p.put(GUIOptions.SPINNER_MAX_VALUE, Double.valueOf(1E5d));
	p.put(SimulatorOptions.OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE, Double
		.valueOf(1d));
	p.put(SimulatorOptions.OPT_DEFAULT_SPECIES_INITIAL_VALUE, Double
		.valueOf(1d));
	p.put(SimulatorOptions.OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS, Double
		.valueOf(1d));

	return p;
    }

    /**
     * Table for experimental data, the legend, and the simulation data.
     */
    private JTable expTable, simTable;
    /**
	 * 
	 */
    private JToolBar footPanel;
    /**
     * Switch to decide whether or not to draw the foot panel.
     */
    private boolean showSettingsPanel;
    /**
     * The main tabbed pane showing plot, simulation and experimental data.
     */
    private JTabbedPane tabbedPane;
    /**
	 * 
	 */
    private SimulationWorker worker;
    /**
	 * 
	 */
    private SimulationVisualizationPanel visualizationPanel;
    /**
     * Indices to more efficiently memorize the location of interesting elements
     * in the call-back function.
     */
    private int simulationDataIndex, solutionIndex, runBestIndex;
    /**
     * Array of identifiers of those {@link Quantity}s that are the target of a
     * value optimization.
     */
    private String[] selectedQuantityIds;

    /**
     * @param model
     */
    public SimulationPanel(Model model) {
	this(model, getDefaultProperties());
    }

    /**
     * @param model
     * @param properties
     */
    public SimulationPanel(Model model, SBProperties properties) {
	super();
	showSettingsPanel = true;
	if (SBMLsimulator.getAvailableSolvers().length == 0) {
	    String msg = "Could not find any solvers for differential equation systems. A simulation is therefore not possible.";
	    JOptionPane.showMessageDialog(this, StringUtil.toHTML(msg),
		"No ODE solver available", JOptionPane.WARNING_MESSAGE);
	} else {
	    try {
		worker = new SimulationWorker(model);
		visualizationPanel = new SimulationVisualizationPanel();
		setProperties(properties);
	    } catch (Exception exc) {
		GUITools.showErrorMessage(this, exc);
	    }
	}
    }

    /**
	 * 
	 */
    public void closeExperimentalData() {
	expTable = new JTable();
	expTable.setDefaultRenderer(Double.class, new DecimalCellRenderer(10,
	    4, SwingConstants.RIGHT));
	tabbedPane.setEnabledAt(2, false);
	if (tabbedPane.getSelectedIndex() == 2) {
	    tabbedPane.setSelectedIndex(0);
	}
	visualizationPanel.unsetExperimentData();
    }

    /**
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SecurityException
     * @throws IllegalArgumentException
     */
    private SimulationToolPanel createFootPanel()
	throws IllegalArgumentException, SecurityException,
	InstantiationException, IllegalAccessException,
	InvocationTargetException, NoSuchMethodException {
	footPanel = new JToolBar("Integration toolbox");
	SimulationToolPanel foot = new SimulationToolPanel(worker);
	footPanel.add(foot);
	return foot;
    }

    /*
     * (non-Javadoc)
     * @see
     * eva2.server.stat.InterfaceStatisticsListener#finalMultiRunResults(java
     * .lang.String[], java.util.List)
     */
    public void finalMultiRunResults(String[] header,
	List<Object[]> multiRunFinalObjectData) {
	// TODO Auto-generated method stub
	System.out.println("finalMultiRunResults");
    }

    /**
     * @return
     */
    public Distance getDistance() {
	return ((SimulationToolPanel) footPanel.getComponent(0)).getDistance();
    }

    /**
     * @return
     */
    public MultiBlockTable getExperimentalData() {
	return (MultiBlockTable) expTable.getModel();
    }

    /**
     * @return
     */
    public Model getModel() {
	return worker.getModel();
    }

    /**
     * @return
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    private SimulationToolPanel getOrCreateFootPanel()
	throws IllegalArgumentException, SecurityException,
	InstantiationException, IllegalAccessException,
	InvocationTargetException, NoSuchMethodException {
	if (footPanel == null) {
	    return createFootPanel();
	}
	return (SimulationToolPanel) footPanel.getComponent(0);
    }

    /**
	 * 
	 */
    public Properties getProperties() {
	Properties p = ((SimulationToolPanel) footPanel.getComponent(0))
		.getProperties();
	p.putAll(visualizationPanel.getProperties());
	return p;
    }

    /**
     * @return
     */
    public MultiBlockTable getSimulationResultsTable() {
	return (MultiBlockTable) simTable.getModel();
    }

    /**
     * @return
     */
    public DESSolver getSolver() {
	return ((SimulationToolPanel) footPanel.getComponent(0)).getSolver();
    }

    /***
     * Initializes the graphics components of this panel.
     * 
     * @param properties
     */
    private void init() {
	setLayout(new BorderLayout());
	try {
	    if (visualizationPanel == null) {
		visualizationPanel = new SimulationVisualizationPanel();
	    }
	    visualizationPanel.setModel(worker.getModel());
	    SimulationToolPanel foot = getOrCreateFootPanel();
	    foot.addItemListener(visualizationPanel);
	    if (showSettingsPanel) {
		add(footPanel, BorderLayout.SOUTH);
	    }
	    visualizationPanel.getPlot().setGridVisible(foot.getShowGrid());
	    visualizationPanel.getPlot().setShowLegend(foot.getShowLegend());
	    visualizationPanel.getPlot().setShowGraphToolTips(
		foot.getShowGraphToolTips());
	    visualizationPanel.setPlotToLogScale(foot.getJCheckBoxLegend());

	    if (tabbedPane == null) {
		JPanel simPanel = new JPanel(new BorderLayout());
		simTable = new JTable();
		simTable.setDefaultRenderer(Double.class,
		    new DecimalCellRenderer(10, 4, SwingConstants.RIGHT));
		simPanel.add(new JScrollPane(simTable,
		    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
		    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
		    BorderLayout.CENTER);
		simTable.getModel().addTableModelListener(visualizationPanel);

		JPanel expPanel = new JPanel(new BorderLayout());
		expTable = new JTable();
		expTable.setDefaultRenderer(Double.class,
		    new DecimalCellRenderer(10, 4, SwingConstants.RIGHT));
		expPanel.add(new JScrollPane(expTable,
		    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
		    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
		    BorderLayout.CENTER);
		expTable.getModel().addTableModelListener(visualizationPanel);

		tabbedPane = new JTabbedPane();
		tabbedPane.add("Plot ", visualizationPanel);
		tabbedPane.add("Simulated data", simPanel);
		tabbedPane.add("Experimental data", expPanel);
		tabbedPane.setEnabledAt(0, true);
		tabbedPane.setEnabledAt(1, false);
		tabbedPane.setEnabledAt(2, false);
	    }
	    add(tabbedPane, BorderLayout.CENTER);
	} catch (Exception exc) {
	    GUITools.showErrorMessage(this, exc);
	}
    }

    /**
     * @return
     */
    public boolean isSetExperimentalData() {
	return expTable.getRowCount() > 0;
    }

    /**
     * @return the showSettingsPanel
     */
    public boolean isShowSettingsPanel() {
	return showSettingsPanel;
    }

    /*
     * (non-Javadoc)
     * @see
     * eva2.server.stat.InterfaceStatisticsListener#notifyGenerationPerformed
     * (java.lang.String[], java.lang.Object[], java.lang.Double[])
     */
    public void notifyGenerationPerformed(String[] header,
	Object[] statObjects, Double[] statDoubles) {
	SimulationToolPanel tools = (SimulationToolPanel) footPanel
		.getComponent(0);
	double currentDistance = tools.getCurrentDistance();
	if (Double.isNaN(currentDistance)
		|| (currentDistance > statDoubles[runBestIndex].doubleValue())) {
	    setSimulationData((MultiBlockTable) statObjects[simulationDataIndex]);
	    tools.setCurrentDistance(statDoubles[runBestIndex].doubleValue());
	    double solution[] = (double[]) statObjects[solutionIndex];
	    for (int i = 0; i < selectedQuantityIds.length; i++) {
		visualizationPanel.updateQuantity(selectedQuantityIds[i],
		    solution[i]);
	    }
	}
    }

    /**
     * @param selectedQuantityIds
     */
    public void notifyQuantitiesSelected(String[] selectedQuantityIds) {
	this.selectedQuantityIds = selectedQuantityIds;
    }

    /*
     * (non-Javadoc)
     * @see eva2.server.stat.InterfaceStatisticsListener#notifyRunStarted(int,
     * int, java.lang.String[], java.lang.String[])
     */
    public void notifyRunStarted(int runNumber, int plannedMultiRuns,
	String[] header, String[] metaInfo) {
	// Determine indices
	int i, allFound = 0;
	for (i = 0; (i < header.length) && (allFound < 3); i++) {
	    if (header[i].equals(EstimationProblem.SIMULATION_DATA)) {
		simulationDataIndex = i;
		allFound++;
	    } else if (header[i]
		    .equals(AbstractOptimizationProblem.STAT_SOLUTION_HEADER)) {
		solutionIndex = i;
		allFound++;
	    } else if (header[i].equals(GraphSelectionEnum.runBest.toString())) {
		runBestIndex = i;
		allFound++;
	    }
	}
    }

    /*
     * (non-Javadoc)
     * @see eva2.server.stat.InterfaceStatisticsListener#notifyRunStopped(int,
     * boolean)
     */
    public void notifyRunStopped(int runsPerformed, boolean completedLastRun) {
	// System.out.println("notifyRunStopped");
    }

    /**
	 * 
	 */
    public void savePlotImage() {
	try {
	    SBPreferences prefs = SBPreferences
		    .getPreferencesFor(SimulatorOptions.class);
	    String saveDir = prefs.get(SimulatorOptions.PLOT_SAVE_DIR)
		    .toString();
	    float compression = prefs
		    .getFloat(SimulatorOptions.JPEG_COMPRESSION_FACTOR);
	    prefs.put(SimulatorOptions.PLOT_SAVE_DIR, visualizationPanel
		    .getPlot().savePlotImage(saveDir,
			Float.valueOf(compression)));
	} catch (Exception exc) {
	    GUITools.showErrorMessage(this, exc);
	}
    }

    /**
	 * 
	 */
    public void saveSimulationResults() {
	try {
	    TableModel simTabModel = getSimulationResultsTable();
	    SBPreferences prefs = SBPreferences
		    .getPreferencesFor(CSVOptions.class);
	    if (simTabModel.getRowCount() > 0) {
		File out = GUITools.saveFileDialog(this, prefs.get(
		    CSVOptions.CSV_FILES_SAVE_DIR).toString(), false, false,
		    JFileChooser.FILES_ONLY, SBFileFilter.CSV_FILE_FILTER);
		if (out != null) {
		    (new CSVWriter()).write(simTabModel, prefs.get(
			CSVOptions.CSV_FILES_SEPARATOR_CHAR).toString().charAt(
			0), out);
		    prefs.put(CSVOptions.CSV_FILES_SAVE_DIR, out.getParent());
		}
	    } else {
		String msg = "No simulation has been performed yet. Please run the simulation first.";
		JOptionPane.showMessageDialog(this, StringUtil.toHTML(msg, 40));
	    }
	} catch (IOException exc) {
	    GUITools.showErrorMessage(this, exc);
	}
    }

    /**
     * @param enabled
     */
    public void setAllEnabled(boolean enabled) {
	this.visualizationPanel.setInteractiveScanEnabled(enabled);
	((SimulationToolPanel) footPanel.getComponent(0))
		.setAllEnabled(enabled);
    }

    /**
     * @param data
     * @throws Exception
     */
    public void setExperimentalData(MultiBlockTable data) throws Exception {
	expTable.setModel(data);
	tabbedPane.setEnabledAt(2, true);
	worker.setData(data);
	SimulationToolPanel tools = getOrCreateFootPanel();
	visualizationPanel.setExperimentData(data);
	tools.computeDistance();
    }

    /**
     * Assign properties from the settings
     * 
     * @param properties
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SecurityException
     * @throws IllegalArgumentException
     */
    public void setProperties(SBProperties properties)
	throws IllegalArgumentException, SecurityException,
	InstantiationException, IllegalAccessException,
	InvocationTargetException, NoSuchMethodException {

	if (visualizationPanel != null) {
	    visualizationPanel.setProperties(properties);
	}

	getOrCreateFootPanel().setProperties(properties);
	removeAll();
	init();
    }

    /**
     * @param ids
     */
    public void setSelectedQuantities(String... ids) {
	visualizationPanel.setSelectedQuantities(ids);
    }

    /**
     * @param showSettingsPanel
     *        the showSettingsPanel to set
     */
    public void setShowSettingsPanel(boolean showSettingsPanel) {
	if (this.showSettingsPanel != showSettingsPanel) {
	    this.showSettingsPanel = showSettingsPanel;
	    if (!showSettingsPanel) {
		remove(footPanel);
	    } else {
		add(footPanel, BorderLayout.SOUTH);
	    }
	    footPanel.setVisible(showSettingsPanel);
	    validate();
	}
    }

    /**
     * @param data
     */
    private void setSimulationData(MultiBlockTable data) {
	data.addTableModelListener(visualizationPanel);
	visualizationPanel.setSimulationData(data);
	simTable.setModel(data);
	tabbedPane.setEnabledAt(1, true);
    }

    /**
     * Conducts the simulation.
     * 
     * @throws Exception
     */
    public void simulate() throws Exception {
	SimulationToolPanel foot = getOrCreateFootPanel();
	simulate(worker.getModel(), foot.getSimulationStartTime(), foot
		.getSimulationEndTime(), foot.getStepSize());
	foot.computeDistance();
    }

    /**
     * @param model
     * @param t1val
     * @param t2val
     * @param stepSize
     * @throws Exception
     */
    private void simulate(Model model, double t1val, double t2val,
	double stepSize) throws Exception {
	SimulationToolPanel foot = (SimulationToolPanel) footPanel
		.getComponent(0);
	MultiBlockTable data = SimulationWorker.solveByStepSize(foot
		.getSolver(), model, t1val, t2val, stepSize, visualizationPanel
		.getIncludeReactions(), this);
	setSimulationData(data);
	if (stepSize != foot.getStepSize()) {
	    foot.setStepSize(stepSize);
	}
    }
}
