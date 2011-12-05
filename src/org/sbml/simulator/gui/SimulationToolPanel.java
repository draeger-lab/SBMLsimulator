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
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.math.ode.DerivativeException;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.util.StringTools;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.simulator.QualityMeasurement;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.SimulationConfiguration;
import org.sbml.simulator.SimulationManager;
import org.sbml.simulator.SimulationOptions;
import org.sbml.simulator.gui.plot.PlotOptions;
import org.sbml.simulator.math.QualityMeasure;
import org.simulator.math.odes.AbstractDESSolver;

import de.zbit.gui.GUITools;
import de.zbit.gui.LayoutHelper;
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
	 * 
	 */
	private static final Logger logger = Logger
			.getLogger(SimulationToolPanel.class.getName());

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
	 * Contains all available quality measure functions.
	 */
	private JComboBox qualityMeasureFunctions;
	
	/**
	 * Text field to display the quality of a simulation with respect to a given
	 * data set.
	 */
	private JFormattedTextField qualityMeasureField;
//	/**
//	 * Whether or not to plot in a logarithmic scale.
//	 */
//	private JCheckBox logScale;
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
	private JComboBox solverComboBox;
	
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
	 * 
	 */
	private JSpinner startTime;

	/**
	 * 
	 */
	private SimulationConfiguration simulationConfiguration;

	/**
	 * 
	 */
	private QualityMeasurement qualityMeasurement;

	/**
	 * @param worker
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	public SimulationToolPanel(SimulationManager simulationManager)
			throws IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		super(new BorderLayout());
		simulationConfiguration = simulationManager.getSimlationConfiguration();
		qualityMeasurement = simulationManager.getQualityMeasurement();
		this.addPropertyChangeListener(simulationConfiguration);
		this.addPropertyChangeListener(qualityMeasurement);

		this.setOfItemListeners = new HashSet<ItemListener>();
		spinnerStepSize = .01d;
		maxTime = 1E5;
		t1 = new SpinnerNumberModel(0d, 0d, maxTime, spinnerStepSize);
		t2 = new SpinnerNumberModel(1d, 0d, maxTime, spinnerStepSize);
		maxStepsPerUnit = 1000;
		double integrationStepSize = spinnerStepSize;
		SBPreferences prefs = SBPreferences
				.getPreferencesFor(SimulationOptions.class);
		solverComboBox = createSolversComboOrSetSelectedItem(prefs.get(
				SimulationOptions.ODE_SOLVER).toString());
		showGrid = GUITools.createJCheckBox(
				PlotOptions.SHOW_PLOT_GRID.getName(), false,
				PlotOptions.SHOW_PLOT_GRID,
				PlotOptions.SHOW_PLOT_GRID.getToolTip(), this);
		// logScale =
		// GUITools.createJCheckBox(PlotOptions.PLOT_LOG_SCALE.getName(),
		// false, PlotOptions.PLOT_LOG_SCALE, PlotOptions.PLOT_LOG_SCALE
		// .getToolTip(), this);
		showLegend = GUITools.createJCheckBox(
				PlotOptions.SHOW_PLOT_LEGEND.getName(), true,
				PlotOptions.SHOW_PLOT_LEGEND,
				PlotOptions.SHOW_PLOT_LEGEND.getToolTip(), this);
		showToolTips = GUITools.createJCheckBox(
				PlotOptions.SHOW_PLOT_TOOLTIPS.getName(), true,
				PlotOptions.SHOW_PLOT_TOOLTIPS,
				PlotOptions.SHOW_PLOT_TOOLTIPS.getToolTip(), this);

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
		settings.add("ODE Solver:", solverComboBox, 4, 2, 1, 0);
		settings.add(new JPanel());
		sPanel.setBorder(BorderFactory.createTitledBorder(" Integration "));

		JPanel dPanel = new JPanel();
		LayoutHelper dSet = new LayoutHelper(dPanel);
		Class<QualityMeasure>[] distFunctions = SBMLsimulator
				.getAvailableQualityMeasures();
		String quality[] = new String[distFunctions.length];
		for (int i = 0; i < distFunctions.length; i++) {
			QualityMeasure dist = distFunctions[i].getConstructor()
					.newInstance();
			quality[i] = dist.getName();
		}
		qualityMeasureFunctions = new JComboBox(quality);
		qualityMeasureFunctions.setName("distfun");
		qualityMeasureFunctions.addItemListener(this);
		qualityMeasureField = new JFormattedTextField();
		qualityMeasureField.setEnabled(false);
		dSet.add(qualityMeasureFunctions);
		dSet.add(new JPanel());
		dSet.add(qualityMeasureField);
		dPanel.setBorder(BorderFactory
				.createTitledBorder(" Model quality measure "));

		JPanel pPanel = new JPanel();
		LayoutHelper pSet = new LayoutHelper(pPanel);
		// JButton cameraButton = new JButton(org.sbml.simulator.gui.GUITools
		// .getIconCamera());
		// JButton saveButton = new JButton(UIManager.getIcon("ICON_SAVE_16"));
		// // cameraButton.addActionListener(EventHandler.create(
		// ActionListener.class, this, "openFileAndLogHistory"));
		// TODO
		pSet.add(showGrid, 0, 0, 1, 1, 0, 0);
//		pSet.add(new JPanel(), 1, 0, 1, 1, 0, 0);
//		pSet.add(logScale, 2, 0, 1, 1, 0, 0);
		pSet.add(showLegend, 0, 1, 1, 1, 0, 0);
		pSet.add(showToolTips, 0, 2, 1, 1, 0, 0);
//		pSet.add(new JPanel(), 3, 0, 1, 1, 0, 0);
		// pSet.add(cameraButton, 4, 0, 1, 1, 0, 0);
		// pSet.add(saveButton, 4, 1, 1, 1, 0, 0);
		pPanel.setBorder(BorderFactory.createTitledBorder(" Plot "));

		LayoutHelper aSet = new LayoutHelper(new JPanel());
		aSet.add(sPanel, 0, 0, 1, 1, 1, 0);
		aSet.add(dPanel, 1, 0, 1, 1, 0, 0);
		aSet.add(pPanel, 2, 0, 1, 1, 0, 0);

		// Main
		add(aSet.getContainer(), BorderLayout.CENTER);

		loadPreferences();
	}

	/**
	 * @param listener
	 */
	public void addItemListener(ItemListener listener) {
		setOfItemListeners.add(listener);
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
		if (solverComboBox == null) {
			solverComboBox = new JComboBox();
			name = name.substring(name.lastIndexOf('.') + 1);
			Class<AbstractDESSolver>[] solFun = SBMLsimulator
					.getAvailableSolvers();
			AbstractDESSolver solver;
			for (int i = 0; i < solFun.length; i++) {
				Class<AbstractDESSolver> c = solFun[i];
				solver = c.getConstructor().newInstance();
				solverComboBox.addItem(solver.getName());
				if (c.getName().substring(c.getName().lastIndexOf('.') + 1)
						.equals(name)) {
					solverComboBox.setSelectedIndex(i);
				}
			}
			solverComboBox.setEnabled(solverComboBox.getItemCount() > 1);
			if (solverComboBox.getSelectedIndex() != solverComboBox
					.getItemCount() - 1) {
				solver = solFun[solverComboBox.getSelectedIndex()]
						.getConstructor().newInstance();
			}
			solverComboBox.setName("solvers");
			solverComboBox.addItemListener(this);
		} else {
			Class<AbstractDESSolver>[] solFun = SBMLsimulator
					.getAvailableSolvers();

			for (int i = 0; i < solFun.length; i++) {
				Class<AbstractDESSolver> c = solFun[i];
				if (c.getName().substring(c.getName().lastIndexOf('.') + 1)
						.equals(name)) {
					solverComboBox.setSelectedIndex(i);
				}
			}
		}
		return solverComboBox;
	}

	/**
	 * Access to the currently computed distance between measurement data and
	 * simulation.
	 * 
	 * @return The distance based on the currently selected
	 *         {@link QualityMeasure} function or {@link Double.#NaN} if no
	 *         distance has been computed yet.
	 */
	public double getCurrentQuality() {
		return qualityMeasureField.getValue() == null ? Double.NaN
				: ((Number) qualityMeasureField.getValue()).doubleValue();
	}

	/**
	 * @return
	 */
	public QualityMeasure getQualityMeasure() {
		return qualityMeasurement.getDistance();
	}

//	/**
//	 * @return
//	 */
//	public JCheckBox getJCheckBoxLegend() {
//		return logScale;
//	}

	/**
	 * @return
	 */
	public double getNumIntegrationSteps() {
		return stepsModel.getNumber().doubleValue();
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
		return new ValuePair<Double, Double>(
				Double.valueOf(getSimulationStartTime()),
				Double.valueOf(getSimulationEndTime()));
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
	 * 
	 * @see
	 * java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		if ((e.getSource() instanceof JComboBox)
				&& (e.getStateChange() == ItemEvent.SELECTED)) {
			JComboBox comBox = (JComboBox) e.getSource();
			try {

				if (comBox.getName().equals("distfun")) {
					QualityMeasure dist = SBMLsimulator
							.getAvailableQualityMeasures()[comBox
							.getSelectedIndex()].getConstructor().newInstance();
					firePropertyChange("distance",
							qualityMeasurement.getDistance(), dist);

				} else if (comBox.getName().equals("solvers")) {
					setSolver(comBox);
					AbstractDESSolver solver = SBMLsimulator
							.getAvailableSolvers()[comBox.getSelectedIndex()]
							.getConstructor().newInstance();
					firePropertyChange("solver",
							simulationConfiguration.getSolver(), solver);
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

		SBPreferences prefsSimulation = SBPreferences
				.getPreferencesFor(SimulationOptions.class);
		SBPreferences prefsPlot = SBPreferences
				.getPreferencesFor(PlotOptions.class);
		/*
		 * Solver and distance.
		 */
		double simEndTime = prefsSimulation
				.getDouble(SimulationOptions.SIM_END_TIME);
		spinnerStepSize = prefsSimulation
				.getDouble(SimulationOptions.SIM_STEP_SIZE);
		Class<QualityMeasure>[] distFun = SBMLsimulator
				.getAvailableQualityMeasures();

		firePropertyChange("stepSize", null, spinnerStepSize);

		maxTime = Math.max(1E8, Math.max(getSimulationStartTime(), simEndTime));
		t1.setMinimum(Double.valueOf(0));
		t1.setValue(Double.valueOf(getSimulationStartTime()));
		t1.setMaximum(Double.valueOf(maxTime));
		t1.setStepSize(Double.valueOf(spinnerStepSize));
		t2.setMinimum(Double.valueOf(getSimulationStartTime()));
		t2.setValue(Double.valueOf(simEndTime));
		t2.setMaximum(Double.valueOf(maxTime));
		t2.setStepSize(Double.valueOf(spinnerStepSize));
		showGrid.setSelected(prefsPlot.getBoolean(PlotOptions.SHOW_PLOT_GRID));
		// logScale.setSelected(prefsPlot.getBoolean(PlotOptions.PLOT_LOG_SCALE));
		showLegend.setSelected(prefsPlot
				.getBoolean(PlotOptions.SHOW_PLOT_LEGEND));
		showToolTips.setSelected(prefsPlot
				.getBoolean(PlotOptions.SHOW_PLOT_TOOLTIPS));

		maxStepsPerUnit = (int) 1E8d;

		int qualityFunc = 0;
		String name = prefsSimulation
				.get(SimulationOptions.QUALITY_MEASURE);
		name = name.substring(name.lastIndexOf('.') + 1);
		while ((qualityFunc < distFun.length - 1)
				&& !distFun[qualityFunc].getSimpleName().equals(name)) {
			qualityFunc++;
		}
		if (this.qualityMeasureFunctions != null) {
			qualityMeasureFunctions.setSelectedIndex(qualityFunc);
		}
		firePropertyChange("distance", null,
				SBMLsimulator.getAvailableQualityMeasures()[qualityFunc]
						.getConstructor().newInstance());

		solverComboBox = createSolversComboOrSetSelectedItem(prefsSimulation
				.get(SimulationOptions.ODE_SOLVER));
		firePropertyChange("solver", null,
				SBMLsimulator.getAvailableSolvers()[solverComboBox
						.getSelectedIndex()].getConstructor().newInstance());

		double startTime = prefsSimulation
				.getDouble(SimulationOptions.SIM_START_TIME);
		double endTime = prefsSimulation
				.getDouble(SimulationOptions.SIM_END_TIME);
		startTime = Math.max(0, startTime);
		if (startTime > endTime) {
			swap(startTime, endTime);
		}
		firePropertyChange("start", null, startTime);
		firePropertyChange("end", null, endTime);
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
	 * @param enabled
	 */
	public void setAllEnabled(boolean enabled) {
		GUITools.setAllEnabled(this, enabled);
		startTime.setEnabled(false);
		if (enabled && (qualityMeasureField.getText().length() == 0)) {
			qualityMeasureField.setEditable(false);
		} else if (!enabled && (qualityMeasureField.getText().length() > 0)) {
			qualityMeasureField.setEnabled(true);
		}
	}

	/**
	 * Allows external methods do manipulate the distance field.
	 * 
	 * @param value
	 *            The new distance value. This should be in accordance with the
	 *            currently selected {@link QualityMeasure} function and also
	 *            integration method. Furthermore, it should also belong to the
	 *            correct {@link Model} and data set.
	 */
	void setCurrentQualityMeasure(double value) {
		qualityMeasureField.setValue(value);
		qualityMeasureField.setText(StringTools.toString(value));
		// distField.setValue(Double.valueOf(value));
		if (!qualityMeasureField.isEnabled()) {
			qualityMeasureField.setEditable(false);
			qualityMeasureField.setEnabled(true);
			qualityMeasureField.setAlignmentX(RIGHT_ALIGNMENT);
		}
	}

	/**
	 * @throws ModelOverdeterminedException
	 * @throws IntegrationException
	 * @throws SBMLException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 */
	public void setSolver() throws IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, SBMLException,
			DerivativeException, ModelOverdeterminedException {
		setSolver(solverComboBox);
	}

	/**
	 * 
	 * @param comBox
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SBMLException
	 * @throws IntegrationException
	 * @throws ModelOverdeterminedException
	 */
	private void setSolver(JComboBox comBox) throws IllegalArgumentException,
			SecurityException, InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, SBMLException,
			DerivativeException, ModelOverdeterminedException {
		logger.fine(comBox.getSelectedItem() + "\t" + comBox.getSelectedIndex());
		this.firePropertyChange("solver", simulationConfiguration.getSolver(),
				SBMLsimulator.getAvailableSolvers()[comBox.getSelectedIndex()]
						.getConstructor().newInstance());
	}

	/**
	 * @param stepSize
	 */
	public void setStepSize(double stepSize) {
		this.firePropertyChange("stepSize",
				simulationConfiguration.getStepSize(), stepSize);
		this.stepsModel.setValue(Integer.valueOf(numSteps(
				((Number) t1.getValue()).doubleValue(),
				((Number) t2.getValue()).doubleValue(), stepSize)));
	}

	/*
	 * (non-Javadoc)
	 * 
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
	 * Broadcasts the current values of start, end and step size of a simulation
	 * e.g. in order to submit them to the simulation configuration. This method
	 * should be called before starting a simulation so the values will be
	 * submitted only once at the correct time.
	 */
	public void processSimulationValues() {
		firePropertyChange("start", simulationConfiguration.getStart(),
				getSimulationStartTime());
		firePropertyChange("end", simulationConfiguration.getEnd(),
				getSimulationEndTime());
		firePropertyChange("stepSize", simulationConfiguration.getStepSize(),
				getStepSize());
	}

}
