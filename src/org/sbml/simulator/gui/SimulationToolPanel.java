/**
 * 
 */
package org.sbml.simulator.gui;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
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

import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.util.ValuePair;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.math.Distance;
import org.sbml.simulator.math.odes.AbstractDESSolver;
import org.sbml.simulator.math.odes.DESSolver;
import org.sbml.simulator.math.odes.IntegrationException;
import org.sbml.simulator.math.odes.MultiBlockTable;
import org.sbml.squeezer.CfgKeys;

import de.zbit.gui.GUITools;
import de.zbit.gui.LayoutHelper;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-16
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
	 * 
	 */
	private SimulationWorker worker;

	/**
	 * 
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
		maxStepsPerUnit = 100;
		double integrationStepSize = spinnerStepSize;
		solvers = createSolversCombo(CfgKeys.SIM_ODE_SOLVER.getProperty()
				.toString());
		showGrid = GUITools.createJCheckBox(Plot.Command.SHOW_GRID.getText(),
				false, Plot.Command.SHOW_GRID, Plot.Command.SHOW_GRID
						.getToolTip(), this);
		logScale = GUITools.createJCheckBox(Plot.Command.LOG_SCALE.getText(),
				false, Plot.Command.LOG_SCALE, Plot.Command.LOG_SCALE
						.getToolTip(), this);
		showLegend = GUITools.createJCheckBox(Plot.Command.SHOW_LEGEND
				.getText(), true, Plot.Command.SHOW_LEGEND,
				Plot.Command.SHOW_LEGEND.getToolTip(), this);
		showToolTips = GUITools.createJCheckBox(Plot.Command.SHOW_TOOL_TIPS
				.getText(), true, Plot.Command.SHOW_TOOL_TIPS,
				Plot.Command.SHOW_TOOL_TIPS.getToolTip(), this);

		// Settings
		JSpinner startTime = new JSpinner(t1);
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
		DecimalFormat format = new DecimalFormat("########0.#########E0");
		// (DecimalFormat) DecimalFormat.getInstance();
		// format.setMaximumIntegerDigits(10);
		// format.setMinimumIntegerDigits(0);
		// format.setParseIntegerOnly(false);
		// format.setMaximumFractionDigits(15);
		// format.setMinimumFractionDigits(0);
		distField = new JFormattedTextField(format);
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
	 * 
	 * @param listener
	 */
	public void addItemListener(ItemListener listener) {
		setOfItemListeners.add(listener);
	}

	/**
	 * 
	 * @throws SBMLException
	 * @throws IntegrationException
	 * @throws ModelOverdeterminedException
	 */
	public void computeDistance() throws SBMLException, IntegrationException,
			ModelOverdeterminedException {
		if (worker.isSetModel() && worker.isSetData()) {
			distField.setValue(Double.valueOf(worker.computeDistance()));
			distField.setEditable(false);
			distField.setEnabled(true);
		}
	}

	/**
	 * 
	 * @param name
	 * @return
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	private JComboBox createSolversCombo(String name)
			throws IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		JComboBox solvers = new JComboBox();
		name = name.substring(name.lastIndexOf('.') + 1);
		Class<AbstractDESSolver>[] solFun = SBMLsimulator.getAvailableSolvers();
		AbstractDESSolver solver;
		for (int i = 0; i < solFun.length; i++) {
			Class<AbstractDESSolver> c = solFun[i];
			solver = c.getConstructor().newInstance();
			solvers.addItem(solver.getName());
			if (c.getName().substring(c.getName().lastIndexOf('.') + 1).equals(
					name)) {
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
		return solvers;
	}

	/**
	 * 
	 * @return
	 */
	public Distance getDistance() {
		return worker.getDistance();
	}

	/**
	 * 
	 * @return
	 */
	public JCheckBox getJCheckBoxLegend() {
		return logScale;
	}

	/**
	 * 
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
		p.put(CfgKeys.SIM_MAX_TIME, this.maxTime);
		p.put(CfgKeys.SIM_START_TIME, (Double) t1.getValue());
		p.put(CfgKeys.SIM_END_TIME, (Double) t2.getValue());
		p.put(CfgKeys.SIM_STEP_SIZE, Double.valueOf(worker.getDESSolver()
				.getStepSize()));
		p.put(CfgKeys.SIM_MAX_STEPS_PER_UNIT_TIME, Integer
				.valueOf(maxStepsPerUnit));
		p.put(CfgKeys.SIM_DISTANCE_FUNCTION, worker.getDistance().getClass()
				.getName());
		p.put(CfgKeys.SIM_ODE_SOLVER, worker.getDESSolver().getClass()
				.getName());

		/*
		 * Plot
		 */
		p.put(CfgKeys.PLOT_SHOW_GRID, Boolean.valueOf(showGrid.isSelected()));
		p.put(CfgKeys.PLOT_LOG_SCALE, Boolean.valueOf(logScale.isSelected()));
		p.put(CfgKeys.PLOT_SHOW_LEGEND, Boolean
				.valueOf(showLegend.isSelected()));

		return p;
	}

	/**
	 * 
	 * @return
	 */
	public boolean getShowGraphToolTips() {
		return showToolTips.isSelected();
	}

	/**
	 * 
	 * @return
	 */
	public boolean getShowGrid() {
		return showGrid.isSelected();
	}

	/**
	 * 
	 * @return
	 */
	public boolean getShowLegend() {
		return showLegend.isSelected();
	}

	/**
	 * 
	 * @return
	 */
	public double getSimulationEndTime() {
		return ((Double) t2.getValue()).doubleValue();
	}

	/**
	 * 
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
	 * 
	 * @return
	 */
	public DESSolver getSolver() {
		return worker.getDESSolver();
	}

	/**
	 * 
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
					worker
							.setDistance(SBMLsimulator.getAvailableDistances()[comBox
									.getSelectedIndex()].getConstructor()
									.newInstance());
					computeDistance();
				} else if (comBox.getName().equals("solvers")) {
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
	 * 
	 * @param t1
	 * @param t2
	 * @param stepSize
	 * @return
	 */
	private int numSteps(double t1, double t2, double stepSize) {
		return (int) Math.round((t2 - t1) / stepSize);
	}

	/**
	 * 
	 * @param data
	 */
	public void setData(MultiBlockTable data) {
		worker.setData(data);
	}

	/**
	 * 
	 * @param properties
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	public void setProperties(Properties properties)
			throws IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {

		/*
		 * Solver and distance.
		 */
		double simEndTime = ((Number) properties.get(CfgKeys.SIM_END_TIME))
				.doubleValue();
		spinnerStepSize = ((Number) properties.get(CfgKeys.SIM_STEP_SIZE))
				.doubleValue();
		Class<Distance>[] distFun = SBMLsimulator.getAvailableDistances();

		maxTime = Math.max(((Number) properties.get(CfgKeys.SIM_MAX_TIME))
				.doubleValue(), Math.max(getSimulationStartTime(), simEndTime));
		t1.setMinimum(Double.valueOf(0));
		t1.setValue(Double.valueOf(getSimulationStartTime()));
		t1.setMaximum(Double.valueOf(maxTime));
		t1.setStepSize(Double.valueOf(spinnerStepSize));
		t2.setMinimum(Double.valueOf(getSimulationStartTime()));
		t2.setValue(Double.valueOf(simEndTime));
		t2.setMaximum(Double.valueOf(maxTime));
		t2.setStepSize(Double.valueOf(spinnerStepSize));
		showGrid.setSelected(((Boolean) properties.get(CfgKeys.PLOT_SHOW_GRID))
				.booleanValue());
		logScale.setSelected(((Boolean) properties.get(CfgKeys.PLOT_LOG_SCALE))
				.booleanValue());
		showLegend.setSelected(((Boolean) properties
				.get(CfgKeys.PLOT_SHOW_LEGEND)).booleanValue());
		showToolTips.setSelected(((Boolean) properties
				.get(CfgKeys.PLOT_SHOW_TOOLTIPS)).booleanValue());

		maxStepsPerUnit = ((Number) properties
				.get(CfgKeys.SIM_MAX_STEPS_PER_UNIT_TIME)).intValue();

		int distanceFunc = 0;
		String name = properties.get(CfgKeys.SIM_DISTANCE_FUNCTION).toString();
		name = name.substring(name.lastIndexOf('.') + 1);
		while (distanceFunc < distFun.length
				&& !distFun[distanceFunc].getSimpleName().equals(name)) {
			distanceFunc++;
		}
		if (this.distFun != null) {
			this.distFun.setSelectedIndex(distanceFunc);
		}
		solvers = createSolversCombo(properties.get(CfgKeys.SIM_ODE_SOLVER)
				.toString());

		double startTime = ((Number) properties.get(CfgKeys.SIM_START_TIME))
				.doubleValue();
		double endTime = ((Number) properties.get(CfgKeys.SIM_END_TIME))
				.doubleValue();
		startTime = Math.max(0, startTime);
		if (startTime > endTime) {
			swap(startTime, endTime);
		}
	}

	/**
	 * 
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
	 * 
	 * @see
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

}
