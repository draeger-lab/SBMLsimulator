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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.util.StringTools;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.SimulatorOptions;
import org.sbml.simulator.math.Distance;
import org.sbml.simulator.math.odes.AbstractDESSolver;
import org.sbml.simulator.math.odes.DESSolver;
import org.sbml.simulator.math.odes.IntegrationException;
import org.sbml.simulator.math.odes.MultiBlockTable;

import de.zbit.gui.GUITools;
import de.zbit.gui.LayoutHelper;
import de.zbit.util.StringUtil;
import de.zbit.util.ValuePair;
import de.zbit.util.prefs.SBPreferences;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-16
 * @version $Rev$
 * @since 1.0
 */
public class SimulationToolPanel extends JPanel implements ItemListener,
	ChangeListener {

    /**
     * Generated serial version identifier.
     */
    private static final long serialVersionUID = -6540887561618807199L;

    /**
     * Swaps a and b if a is greater then b.
     * 
     * @param a
     * @param b
     */
    public static void swap(double a, double b) {
	if (a > b) {
	    double swap = b;
	    b = a;
	    a = swap;
	}
    }

    /**
     * Contains all available distance functions.
     */
    private JComboBox distFun;
    /**
     * Text field to display the quality of a simulation with respect to a given
     * data set.
     */
    private JFormattedTextField distField;
    /**
     * Whether or not to plot in a logarithmic scale.
     */
    private JCheckBox logScale;
    /**
     * Maximal allowable number of integration steps per time unit
     */
    private int maxStepsPerUnit;
    /**
     * The maximal allowable simulation time
     */
    private double maxTime;
    /**
     * Decides whether or not a grid should be displayed in the plot.
     */
    private JCheckBox showGrid;
    /**
     * Decides whether or not to add a legend to the plot.
     */
    private JCheckBox showLegend;
    /**
     * Whether or not to show tool tips in plots.
     */
    private JCheckBox showToolTips;
    /**
     * The index of the class name of the solver to be used
     */
    private JComboBox solvers;
    /**
     * Simulation start time
     */
    private SpinnerNumberModel t1;
    /**
     * Simulation end time
     */
    private SpinnerNumberModel t2;
    /**
     * The spinner to change the number of integration steps.
     */
    private SpinnerNumberModel stepsModel;
    /**
	 * 
	 */
    private double spinnerStepSize;
    /**
	 * 
	 */
    private Set<ItemListener> setOfItemListeners;

    /**
     * @param enabled
     */
    public void setAllEnabled(boolean enabled) {
	GUITools.setAllEnabled(this, enabled);
	startTime.setEnabled(false);
	if (enabled && (distField.getText().length() == 0)) {
	    distField.setEditable(false);
	} else if (!enabled && (distField.getText().length() > 0)) {
	    distField.setEnabled(true);
	}
    }

    /**
	 * 
	 */
    private SimulationWorker worker;
    /**
	 * 
	 */
    private JSpinner startTime;
    /**
	 * 
	 */
    private static final String DISTANCE_FIELD_TOOL_TIP = "This field shows the %s distance between the experimental data and the simulation of the model with the current configuration, computed by the %s.";

    /**
     * @param worker
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public SimulationToolPanel(SimulationWorker worker)
	throws IllegalArgumentException, SecurityException,
	InstantiationException, IllegalAccessException,
	InvocationTargetException, NoSuchMethodException {
	super(new BorderLayout());

	this.setOfItemListeners = new HashSet<ItemListener>();
	this.worker = worker;
	spinnerStepSize = .01d;
	maxTime = 1E5;
	t1 = new SpinnerNumberModel(0d, 0d, maxTime, spinnerStepSize);
	t2 = new SpinnerNumberModel(1d, 0d, maxTime, spinnerStepSize);
	maxStepsPerUnit = 1000;
	double integrationStepSize = spinnerStepSize;
	SBPreferences prefs = SBPreferences
		.getPreferencesFor(SimulatorOptions.class);
	solvers = createSolversComboOrSetSelectedItem(prefs.get(
	    SimulatorOptions.SIM_ODE_SOLVER).toString());
	showGrid = GUITools.createJCheckBox(Plot.Command.SHOW_GRID.getText(),
	    false, Plot.Command.SHOW_GRID, Plot.Command.SHOW_GRID.getToolTip(),
	    this);
	logScale = GUITools.createJCheckBox(Plot.Command.LOG_SCALE.getText(),
	    false, Plot.Command.LOG_SCALE, Plot.Command.LOG_SCALE.getToolTip(),
	    this);
	showLegend = GUITools.createJCheckBox(Plot.Command.SHOW_LEGEND
		.getText(), true, Plot.Command.SHOW_LEGEND,
	    Plot.Command.SHOW_LEGEND.getToolTip(), this);
	showToolTips = GUITools.createJCheckBox(Plot.Command.SHOW_TOOL_TIPS
		.getText(), true, Plot.Command.SHOW_TOOL_TIPS,
	    Plot.Command.SHOW_TOOL_TIPS.getToolTip(), this);

	// Settings
	startTime = new JSpinner(t1);
	startTime.addChangeListener(this);
	startTime.setName("t1");
	startTime.setEnabled(false);
	JSpinner endTime = new JSpinner(t2);
	endTime.addChangeListener(this);
	endTime.setName("t2");
	double t1val = ((Double) t1.getValue()).doubleValue();
	double t2val = ((Double) t2.getValue()).doubleValue();
	int val = numSteps(t1val, t2val, integrationStepSize);
	int min = 1;
	int max = (int) Math.round((t2val - t1val) * maxStepsPerUnit);
	int steps = 1;
	val = Math.max(min, val);
	max = Math.max(max, val);
	stepsModel = new SpinnerNumberModel(val, min, max, steps);
	JPanel sPanel = new JPanel();
	LayoutHelper settings = new LayoutHelper(sPanel);
	settings.add("Start time:", startTime, 0, 0, 1, 0);
	settings.add(new JPanel(), 3, 0, 1, 1, 0, 0);
	settings.add("Steps:", new JSpinner(stepsModel), 4, 0, 1, 0);
	settings.add(new JPanel());
	settings.add("End time: ", endTime, 0, 2, 1, 0);
	settings.add("ODE Solver:", solvers, 4, 2, 1, 0);
	settings.add(new JPanel());
	sPanel.setBorder(BorderFactory.createTitledBorder(" Integration "));

	JPanel dPanel = new JPanel();
	LayoutHelper dSet = new LayoutHelper(dPanel);
	Class<Distance>[] distFunctions = SBMLsimulator.getAvailableDistances();
	String distances[] = new String[distFunctions.length];
	int distanceFunc = 0;
	for (int i = 0; i < distFunctions.length; i++) {
	    Distance dist = distFunctions[i].getConstructor().newInstance();
	    distances[i] = dist.getName();
	    if (i == distanceFunc) {
		worker.setDistance(dist);
	    }
	}
	distFun = new JComboBox(distances);
	distFun.setName("distfun");
	distFun.addItemListener(this);
	distFun.setSelectedItem(distanceFunc);
	distField = new JFormattedTextField();
	distField.setEnabled(false);
	dSet.add(distFun);
	dSet.add(new JPanel());
	dSet.add(distField);
	dPanel
		.setBorder(BorderFactory
			.createTitledBorder(" Distance to data "));

	JPanel pPanel = new JPanel();
	LayoutHelper pSet = new LayoutHelper(pPanel);
	pSet.add(showGrid, 0, 0, 1, 1, 0, 0);
	pSet.add(new JPanel(), 1, 0, 1, 1, 0, 0);
	pSet.add(logScale, 2, 0, 1, 1, 0, 0);
	pSet.add(showLegend, 0, 1, 1, 1, 0, 0);
	pSet.add(showToolTips, 2, 1, 1, 1, 0, 0);
	pPanel.setBorder(BorderFactory.createTitledBorder(" Plot "));

	LayoutHelper aSet = new LayoutHelper(new JPanel());
	aSet.add(sPanel, 0, 0, 1, 1, 1, 0);
	aSet.add(dPanel, 1, 0, 1, 1, 0, 0);
	aSet.add(pPanel, 2, 0, 1, 1, 0, 0);

	// Main
	add(aSet.getContainer(), BorderLayout.CENTER);
    }

    /**
     * @param listener
     */
    public void addItemListener(ItemListener listener) {
	setOfItemListeners.add(listener);
    }

    /**
     * @throws SBMLException
     * @throws IntegrationException
     * @throws ModelOverdeterminedException
     */
    public void computeDistance() throws SBMLException, IntegrationException,
	ModelOverdeterminedException {
	if (worker.isSetModel() && worker.isSetData()) {
	    setCurrentDistance(worker.computeDistance());
	    distField.setToolTipText(StringUtil.toHTML(String.format(
		DISTANCE_FIELD_TOOL_TIP, distFun.getSelectedItem(), solvers
			.getSelectedItem()), 60));
	}
    }

    /**
     * @param name
     * @return
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    private JComboBox createSolversComboOrSetSelectedItem(String name)
	throws IllegalArgumentException, SecurityException,
	InstantiationException, IllegalAccessException,
	InvocationTargetException, NoSuchMethodException {
	if (solvers == null) {
	    solvers = new JComboBox();
	    name = name.substring(name.lastIndexOf('.') + 1);
	    Class<AbstractDESSolver>[] solFun = SBMLsimulator
		    .getAvailableSolvers();
	    AbstractDESSolver solver;
	    for (int i = 0; i < solFun.length; i++) {
		Class<AbstractDESSolver> c = solFun[i];
		solver = c.getConstructor().newInstance();
		solvers.addItem(solver.getName());
		if (c.getName().substring(c.getName().lastIndexOf('.') + 1)
			.equals(name)) {
		    solvers.setSelectedIndex(i);
		    worker.setDESSolver(solver);
		}
	    }
	    solvers.setEnabled(solvers.getItemCount() > 1);
	    if (solvers.getSelectedIndex() != solvers.getItemCount() - 1) {
		solver = solFun[solvers.getSelectedIndex()].getConstructor()
			.newInstance();
	    }
	    solvers.setName("solvers");
	    solvers.addItemListener(this);
	} else {
	    Class<AbstractDESSolver>[] solFun = SBMLsimulator
		    .getAvailableSolvers();
	    AbstractDESSolver solver;
	    for (int i = 0; i < solFun.length; i++) {
		Class<AbstractDESSolver> c = solFun[i];
		solver = c.getConstructor().newInstance();
		if (c.getName().substring(c.getName().lastIndexOf('.') + 1)
			.equals(name)) {
		    solvers.setSelectedIndex(i);
		    worker.setDESSolver(solver);
		}
	    }
	}
	return solvers;
    }

    /**
     * @return
     */
    public Distance getDistance() {
	return worker.getDistance();
    }

    /**
     * @return
     */
    public JCheckBox getJCheckBoxLegend() {
	return logScale;
    }

    /**
     * @return
     */
    public double getNumIntegrationSteps() {
	return stepsModel.getNumber().doubleValue();
    }

    /**
	 * 
	 */
    public Properties getProperties() {
	Properties p = new Properties();

	/*
	 * Simulation
	 */
	p.put(SimulatorOptions.SIM_MAX_TIME, this.maxTime);
	p.put(SimulatorOptions.SIM_START_TIME, (Double) t1.getValue());
	p.put(SimulatorOptions.SIM_END_TIME, (Double) t2.getValue());
	p.put(SimulatorOptions.SIM_STEP_SIZE, Double.valueOf(worker
		.getDESSolver().getStepSize()));
	p.put(SimulatorOptions.SIM_MAX_STEPS_PER_UNIT_TIME, Integer
		.valueOf(maxStepsPerUnit));
	p.put(SimulatorOptions.SIM_DISTANCE_FUNCTION, worker.getDistance()
		.getClass().getName());
	p.put(SimulatorOptions.SIM_ODE_SOLVER, worker.getDESSolver().getClass()
		.getName());

	/*
	 * Plot
	 */
	p.put(SimulatorOptions.PLOT_SHOW_GRID, Boolean.valueOf(showGrid
		.isSelected()));
	p.put(SimulatorOptions.PLOT_LOG_SCALE, Boolean.valueOf(logScale
		.isSelected()));
	p.put(SimulatorOptions.PLOT_SHOW_LEGEND, Boolean.valueOf(showLegend
		.isSelected()));

	return p;
    }

    /**
     * @return
     */
    public boolean getShowGraphToolTips() {
	return showToolTips.isSelected();
    }

    /**
     * @return
     */
    public boolean getShowGrid() {
	return showGrid.isSelected();
    }

    /**
     * @return
     */
    public boolean getShowLegend() {
	return showLegend.isSelected();
    }

    /**
     * @return
     */
    public double getSimulationEndTime() {
	return ((Double) t2.getValue()).doubleValue();
    }

    /**
     * @return
     */
    public double getSimulationStartTime() {
	return ((Double) t1.getValue()).doubleValue();
    }

    /**
     * Start time and end time.
     * 
     * @return
     */
    public ValuePair<Double, Double> getSimulationTime() {
	return new ValuePair<Double, Double>(Double
		.valueOf(getSimulationStartTime()), Double
		.valueOf(getSimulationEndTime()));
    }

    /**
     * @return
     */
    public DESSolver getSolver() {
	return worker.getDESSolver();
    }

    /**
     * @return
     */
    public double getStepSize() {
	return (getSimulationEndTime() - getSimulationStartTime())
		/ getNumIntegrationSteps();
    }

    /*
     * (non-Javadoc)
     * @see
     * java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
     */
    public void itemStateChanged(ItemEvent e) {
	if ((e.getSource() instanceof JComboBox)
		&& (e.getStateChange() == ItemEvent.SELECTED)) {
	    JComboBox comBox = (JComboBox) e.getSource();
	    try {
		if (comBox.getName().equals("distfun")) {
		    worker
			    .setDistance(SBMLsimulator.getAvailableDistances()[comBox
				    .getSelectedIndex()].getConstructor()
				    .newInstance());
		    computeDistance();
		} else if (comBox.getName().equals("solvers")) {
		    System.out.println(comBox.getSelectedItem() + "\t"
			    + comBox.getSelectedIndex());
		    worker
			    .setDESSolver(SBMLsimulator.getAvailableSolvers()[comBox
				    .getSelectedIndex()].getConstructor()
				    .newInstance());
		    computeDistance();
		}
	    } catch (Exception exc) {
		GUITools.showErrorMessage(this, exc);
	    }
	}
	for (ItemListener l : setOfItemListeners) {
	    l.itemStateChanged(e);
	}
    }

    /**
     * @param t1
     * @param t2
     * @param stepSize
     * @return
     */
    private int numSteps(double t1, double t2, double stepSize) {
	return (int) Math.round((t2 - t1) / stepSize);
    }

    /**
     * @param data
     */
    public void setData(MultiBlockTable data) {
	worker.setData(data);
    }

    /**
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SecurityException
     * @throws IllegalArgumentException
     */
    public void loadPreferences() throws IllegalArgumentException,
	SecurityException, InstantiationException, IllegalAccessException,
	InvocationTargetException, NoSuchMethodException {

	SBPreferences prefs = SBPreferences
		.getPreferencesFor(SimulatorOptions.class);

	/*
	 * Solver and distance.
	 */
	double simEndTime = prefs.getDouble(SimulatorOptions.SIM_END_TIME);
	spinnerStepSize = prefs.getDouble(SimulatorOptions.SIM_STEP_SIZE);
	Class<Distance>[] distFun = SBMLsimulator.getAvailableDistances();

	maxTime = Math.max(prefs.getDouble(SimulatorOptions.SIM_MAX_TIME), Math
		.max(getSimulationStartTime(), simEndTime));
	t1.setMinimum(Double.valueOf(0));
	t1.setValue(Double.valueOf(getSimulationStartTime()));
	t1.setMaximum(Double.valueOf(maxTime));
	t1.setStepSize(Double.valueOf(spinnerStepSize));
	t2.setMinimum(Double.valueOf(getSimulationStartTime()));
	t2.setValue(Double.valueOf(simEndTime));
	t2.setMaximum(Double.valueOf(maxTime));
	t2.setStepSize(Double.valueOf(spinnerStepSize));
	showGrid.setSelected(prefs.getBoolean(SimulatorOptions.PLOT_SHOW_GRID));
	logScale.setSelected(prefs.getBoolean(SimulatorOptions.PLOT_LOG_SCALE));
	showLegend.setSelected(prefs
		.getBoolean(SimulatorOptions.PLOT_SHOW_LEGEND));
	showToolTips.setSelected(prefs
		.getBoolean(SimulatorOptions.PLOT_SHOW_TOOLTIPS));

	maxStepsPerUnit = prefs
		.getInt(SimulatorOptions.SIM_MAX_STEPS_PER_UNIT_TIME);

	int distanceFunc = 0;
	String name = prefs.get(SimulatorOptions.SIM_DISTANCE_FUNCTION);
	name = name.substring(name.lastIndexOf('.') + 1);
	while ((distanceFunc < distFun.length - 1)
		&& !distFun[distanceFunc].getSimpleName().equals(name)) {
	    distanceFunc++;
	}
	if (this.distFun != null) {
	    this.distFun.setSelectedIndex(distanceFunc);
	}
	solvers = createSolversComboOrSetSelectedItem(prefs
		.get(SimulatorOptions.SIM_ODE_SOLVER));

	double startTime = prefs.getDouble(SimulatorOptions.SIM_START_TIME);
	double endTime = prefs.getDouble(SimulatorOptions.SIM_END_TIME);
	startTime = Math.max(0, startTime);
	if (startTime > endTime) {
	    swap(startTime, endTime);
	}
    }

    /**
     * @param stepSize
     */
    public void setStepSize(double stepSize) {
	worker.getDESSolver().setStepSize(stepSize);
	this.stepsModel.setValue(Integer.valueOf(numSteps(((Number) t1
		.getValue()).doubleValue(), ((Number) t2.getValue())
		.doubleValue(), stepSize)));
    }

    /*
     * (non-Javadoc)
     * @see
     * 
     * javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent
     * )
     */
    public void stateChanged(ChangeEvent e) {
	if (e.getSource() instanceof JSpinner) {
	    JSpinner spin = (JSpinner) e.getSource();
	    if (spin.getName() != null && spin.getName().equals("t2")) {
		// do nothing.
	    }
	}
    }

    /**
     * Access to the currently computed distance between measurement data and
     * simulation.
     * 
     * @return The distance based on the currently selected {@link Distance}
     *         function or {@link Double.#NaN} if no distance has been computed
     *         yet.
     */
    public double getCurrentDistance() {
	return distField.getValue() == null ? Double.NaN : ((Number) distField
		.getValue()).doubleValue();
    }

    /**
     * Allows external methods do manipulate the distance field.
     * 
     * @param value
     *        The new distance value. This should be in accordance with the
     *        currently selected {@link Distance} function and also
     *        integration method. Furthermore, it should also belong to the
     *        correct {@link Model} and data set.
     */
    void setCurrentDistance(double value) {
	distField.setValue(value);
	distField.setText(StringTools.toString(value));
	// distField.setValue(Double.valueOf(value));
	if (!distField.isEnabled()) {
	    distField.setEditable(false);
	    distField.setEnabled(true);
	    distField.setAlignmentX(RIGHT_ALIGNMENT);
	}
    }

}
