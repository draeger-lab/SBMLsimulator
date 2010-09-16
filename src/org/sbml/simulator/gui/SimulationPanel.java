/*
 *  SBMLsqueezer creates rate equations for reactions in SBML files
 *  (http://sbml.org).
 *  Copyright (C) 2009 ZBIT, University of Tübingen, Andreas Dräger
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.sbml.simulator.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.QuantityWithDefinedUnit;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.Symbol;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.Variable;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.math.Distance;
import org.sbml.simulator.math.odes.DESSolver;
import org.sbml.simulator.math.odes.MultiBlockTable;
import org.sbml.squeezer.CfgKeys;
import org.sbml.squeezer.util.HTMLFormula;

import de.zbit.gui.GUITools;
import de.zbit.gui.LayoutHelper;
import eva2.server.stat.InterfaceStatisticsListener;

/**
 * @author Andreas Dr&auml;ger
 * @since 1.4
 * @date 2010-04-06
 * 
 */
public class SimulationPanel extends JPanel implements ChangeListener,
		ItemListener, TableModelListener, InterfaceStatisticsListener {

	/**
	 * Generated serial version identifier
	 */
	private static final long serialVersionUID = -7278034514446047207L;

	/**
	 * 
	 */
	private static Properties getDefaultProperties() {
		Properties p = new Properties();
		Double maxVal = Double.valueOf(1E5);
		/*
		 * Simulation
		 */
		p.put(CfgKeys.SIM_MAX_TIME, maxVal);
		p.put(CfgKeys.SIM_START_TIME, Double.valueOf(0));
		p.put(CfgKeys.SIM_END_TIME, Double.valueOf(5));
		p.put(CfgKeys.SIM_STEP_SIZE, Double.valueOf(.01));
		p.put(CfgKeys.SIM_MAX_COMPARTMENT_SIZE, maxVal);
		p.put(CfgKeys.SIM_MAX_SPECIES_VALUE, maxVal);
		p.put(CfgKeys.SIM_MAX_PARAMETER_VALUE, maxVal);
		p.put(CfgKeys.SIM_MAX_STEPS_PER_UNIT_TIME, Integer.valueOf(500));
		p.put(CfgKeys.SIM_DISTANCE_FUNCTION, SBMLsimulator
				.getAvailableDistances()[0].getName());
		p.put(CfgKeys.SIM_ODE_SOLVER, SBMLsimulator.getAvailableSolvers()[0]
				.getName());
		p.put(CfgKeys.OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE, Double.valueOf(1d));
		p.put(CfgKeys.OPT_DEFAULT_SPECIES_INITIAL_VALUE, Double.valueOf(1d));
		p.put(CfgKeys.OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS, Double.valueOf(1d));

		/*
		 * Plot
		 */
		p.put(CfgKeys.PLOT_SHOW_GRID, Boolean.valueOf(true));
		p.put(CfgKeys.PLOT_SHOW_LEGEND, Boolean.valueOf(true));
		p.put(CfgKeys.PLOT_LOG_SCALE, Boolean.valueOf(false));
		p.put(CfgKeys.PLOT_SHOW_TOOLTIPS, Boolean.valueOf(false));

		/*
		 * CSV file parsing
		 */
		p.put(CfgKeys.CSV_FILES_OPEN_DIR, System.getProperty("user.home"));
		p.put(CfgKeys.CSV_FILES_SEPARATOR_CHAR, Character.valueOf(','));
		p.put(CfgKeys.CSV_FILES_QUOTE_CHAR, Character.valueOf('\''));
		p.put(CfgKeys.CSV_FILES_SAVE_DIR, System.getProperty("user.home"));

		/*
		 * General settings
		 */
		p.put(CfgKeys.SPINNER_STEP_SIZE, Double.valueOf(.01));
		p.put(CfgKeys.JPEG_COMPRESSION_FACTOR, Float.valueOf(.8f));
		p.put(CfgKeys.SPINNER_MAX_VALUE, Double.valueOf(1E5d));
		p.put(CfgKeys.OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE, Double.valueOf(1d));
		p.put(CfgKeys.OPT_DEFAULT_SPECIES_INITIAL_VALUE, Double.valueOf(1d));
		p.put(CfgKeys.OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS, Double.valueOf(1d));

		return p;
	}

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
	 * Table for experimental data, the legend, and the simulation data.
	 */
	private JTable expTable, legend, simTable;

	/**
	 * Switches inclusion of reactions in the plot on or off and switch to
	 * decide whether or not to draw the foot panel.
	 */
	private boolean includeReactions = true, showSettingsPanel;
	/**
	 * The maximal allowable and default values.
	 */
	private double maxCompartmentValue, maxParameterValue, maxSpeciesValue,
			defaultCompartmentValue, defaultParameterValue,
			defaultSpeciesValue;
	/**
	 * The step size for the spinner in the interactive parameter scan. and the
	 * maximal value for {@link JSpinner}s.
	 */
	private double maxSpinVal = 1E10, paramStepSize;
	/**
	 * Model to be simulated
	 */
	private Model model;
	/**
	 * Plot area
	 */
	private Plot plot;
	/**
	 * 
	 */
	private SpinnerNumberModel[] spinModSymbol;

	/**
	 * The main tabbed pane showing plot, simulatin and experimental data.
	 */
	private JTabbedPane tabbedPane;

	/**
	 * 
	 */
	private JToolBar footPanel;

	/**
	 * 
	 * @param model
	 */
	public SimulationPanel(Model model) {
		this(model, getDefaultProperties());
	}

	/**
	 * 
	 * @param model
	 * @param properties
	 */
	public SimulationPanel(Model model, Properties properties) {
		super();
		showSettingsPanel = true;
		if (SBMLsimulator.getAvailableSolvers().length == 0) {
			String msg = "Could not find any solvers for differential equation systems. A simulation is therefore not possible.";
			JOptionPane.showMessageDialog(this, GUITools.toHTML(msg),
					"No ODE solver available", JOptionPane.WARNING_MESSAGE);
		} else {
			try {
				this.model = model;
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
		tabbedPane.setEnabledAt(2, false);
		if (tabbedPane.getSelectedIndex() == 2) {
			tabbedPane.setSelectedIndex(0);
		}
	}

	/**
	 * 
	 * @return
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 */
	private SimulationPanelFoot createFootPanel()
			throws IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		footPanel = new JToolBar();
		SimulationPanelFoot foot = new SimulationPanelFoot();
		footPanel.add(foot);
		return foot;
	}

	/*
	 * (non-Javadoc)
	 * 
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
	 * 
	 * @return
	 */
	public Distance getDistance() {
		return ((SimulationPanelFoot) footPanel.getComponent(0)).getDistance();
	}

	/**
	 * 
	 * @return
	 */
	public MultiBlockTable getExperimentalData() {
		return (MultiBlockTable) expTable.getModel();
	}

	/**
	 * 
	 * @return
	 */
	public Model getModel() {
		return model;
	}

	/**
	 * 
	 * @return
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	private SimulationPanelFoot getOrCreateFootPanel()
			throws IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		if (footPanel == null) {
			return createFootPanel();
		}
		return (SimulationPanelFoot) footPanel.getComponent(0);
	}

	/**
	 * 
	 * @return
	 */
	public Plot getPlot() {
		return plot;
	}

	/**
	 * 
	 */
	public Properties getProperties() {
		Properties p = ((SimulationPanelFoot) footPanel.getComponent(0))
				.getProperties();
		/*
		 * Simulation
		 */
		p.put(CfgKeys.SIM_MAX_COMPARTMENT_SIZE, Double
				.valueOf(maxCompartmentValue));
		p.put(CfgKeys.SIM_MAX_SPECIES_VALUE, Double.valueOf(maxSpeciesValue));
		p.put(CfgKeys.SIM_MAX_PARAMETER_VALUE, Double
				.valueOf(maxParameterValue));

		/*
		 * General settings
		 */
		p.put(CfgKeys.SPINNER_STEP_SIZE, Double.valueOf(paramStepSize));
		p.put(CfgKeys.SPINNER_MAX_VALUE, Double.valueOf(maxSpinVal));

		p.put(CfgKeys.OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE, Double
				.valueOf(defaultCompartmentValue));
		p.put(CfgKeys.OPT_DEFAULT_SPECIES_INITIAL_VALUE, Double
				.valueOf(defaultSpeciesValue));
		p.put(CfgKeys.OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS, Double
				.valueOf(defaultParameterValue));

		return p;
	}

	/**
	 * 
	 * @return
	 */
	public JTable getSimulationResultsTable() {
		return simTable;
	}

	/**
	 * 
	 * @return
	 */
	public DESSolver getSolver() {
		return ((SimulationPanelFoot) footPanel.getComponent(0)).getSolver();
	}

	/***
	 * Initializes the graphics components of this panel.
	 * 
	 * @param properties
	 */
	private void init() {
		setLayout(new BorderLayout());
		try {
			if (plot == null) {
				UnitDefinition timeUnits = this.model.getTimeUnitsInstance();
				String xLab = "Time";
				if (timeUnits != null) {
					xLab += " in " + UnitDefinition.printUnits(timeUnits, true);
				}
				plot = new Plot(xLab, "Value");
				// get rid of this pop-up menu.
				MouseListener listeners[] = plot.getMouseListeners();
				for (int i = listeners.length - 1; i >= 0; i--) {
					plot.removeMouseListener(listeners[i]);
				}
			}
			SimulationPanelFoot foot = getOrCreateFootPanel();
			foot.addItemListener(this);
			if (showSettingsPanel) {
				add(footPanel, BorderLayout.SOUTH);
			}
			plot.setGridVisible(foot.getShowGrid());
			plot.setShowLegend(foot.getShowLegend());
			
			if (tabbedPane == null) {
				JPanel simPanel = new JPanel(new BorderLayout());
				simTable = new JTable();
				simPanel.add(new JScrollPane(simTable,
						JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
						JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
						BorderLayout.CENTER);

				JPanel expPanel = new JPanel(new BorderLayout());
				expTable = new JTable();
				expPanel.add(new JScrollPane(expTable,
						JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
						JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
						BorderLayout.CENTER);

				JPanel legendPanel = new JPanel(new BorderLayout());
				legend = legendTable(model);
				legendPanel.add(new JScrollPane(legend,
						JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
						JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
						BorderLayout.CENTER);

				JSplitPane topDown = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
						true, legendPanel, interactiveScanPanel(model,
								maxCompartmentValue, maxSpeciesValue,
								maxParameterValue, paramStepSize));
				topDown.setDividerLocation(topDown.getDividerLocation() + 200);
				JSplitPane leftRight = new JSplitPane(
						JSplitPane.HORIZONTAL_SPLIT, true, topDown, plot);
				leftRight
						.setDividerLocation(topDown.getDividerLocation() + 200);
				tabbedPane = new JTabbedPane();
				tabbedPane.add("Plot ", leftRight);
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
	 * 
	 * @param model
	 * @param maxCompartmentValue
	 * @param maxSpeciesValue
	 * @param maxParameterValue
	 * @param paramStepSize
	 * @return
	 */
	private JTabbedPane interactiveScanPanel(Model model,
			double maxCompartmentValue, double maxSpeciesValue,
			double maxParameterValue, double paramStepSize) {
		JTabbedPane tab = new JTabbedPane();
		JPanel parameterPanel = new JPanel();
		parameterPanel.setLayout(new BoxLayout(parameterPanel,
				BoxLayout.PAGE_AXIS));
		spinModSymbol = new SpinnerNumberModel[model.getNumCompartments()
				+ model.getNumSpecies() + model.getNumParameters()];
		boolean hasLocalParameters = false;
		for (Reaction r : model.getListOfReactions())
			if (r.isSetKineticLaw() && r.getKineticLaw().getNumParameters() > 0) {
				hasLocalParameters = true;
				JPanel panel = interactiveScanTable(r.getKineticLaw()
						.getListOfParameters(), maxParameterValue,
						paramStepSize);
				panel.setBorder(BorderFactory.createTitledBorder(String.format(
						" Reaction %s ", r.getId())));
				parameterPanel.add(panel);
			}
		tab.add("Compartments", new JScrollPane(interactiveScanTable(model
				.getListOfCompartments(), maxCompartmentValue, paramStepSize),
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		tab.setEnabledAt(0, model.getNumCompartments() > 0);
		tab.add("Species", new JScrollPane(interactiveScanTable(model
				.getListOfSpecies(), maxParameterValue, paramStepSize),
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		tab.setEnabledAt(1, model.getNumSpecies() > 0);
		tab.add("Global Parameters", new JScrollPane(interactiveScanTable(model
				.getListOfParameters(), maxSpeciesValue, paramStepSize),
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		tab.setEnabledAt(2, model.getNumParameters() > 0);
		tab.add("Local Parameters", new JScrollPane(parameterPanel,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		tab.setEnabledAt(3, hasLocalParameters);
		for (int i = tab.getTabCount() - 1; i >= 0; i--) {
			if (tab.isEnabledAt(i)) {
				tab.setSelectedIndex(i);
			}
		}
		return tab;
	}

	/**
	 * 
	 * @param maxValue
	 *            , double stepSize
	 * @param model
	 * @return
	 */
	private JPanel interactiveScanTable(
			ListOf<? extends QuantityWithDefinedUnit> list, double maxValue,
			double stepSize) {
		JPanel panel = new JPanel();
		LayoutHelper lh = new LayoutHelper(panel);
		int offset = 0;
		LinkedList<String> nans = new LinkedList<String>();
		double val = 0;
		String name = "";
		for (int i = 0; i < list.size(); i++) {
			QuantityWithDefinedUnit p = list.get(i);
			if (p instanceof Species)
				offset = model.getNumCompartments();
			if (p instanceof Parameter)
				offset = model.getNumCompartments() + model.getNumSpecies();
			val = p.getValue();
			if (Double.isNaN(p.getValue())) {
				name = p.getClass().getSimpleName().toLowerCase();
				if (p instanceof Compartment) {
					val = defaultCompartmentValue;
				} else if (p instanceof Species) {
					val = defaultSpeciesValue;
				} else if (p instanceof Parameter) {
					val = defaultParameterValue;
				}
				p.setValue(val);
				nans.add(p.getId());
				if (!(p instanceof Species) && (list.size() > 1)) {
					name += "s";
				}
			}
			maxValue = Math.max(val, maxValue);
			spinModSymbol[i + offset] = new SpinnerNumberModel(val, Math.min(
					0d, val), maxValue, stepSize);
			JSpinner spinner = new JSpinner(spinModSymbol[i + offset]);
			spinner.setName(p.getId());
			spinner.addChangeListener(this);
			lh.add(new JLabel(GUITools.toHTML(p.toString(), 40)), 0, i, 1, 1,
					0, 0);
			lh.add(spinner, 2, i, 1, 1, 0, 0);
			lh.add(new JLabel(GUITools.toHTML(p.isSetUnits() ? HTMLFormula
					.toHTML(p.getUnitsInstance()) : "")), 4, i, 1, 1, 0, 0);
		}
		lh.add(new JPanel(), 1, 0, 1, 1, 0, 0);
		lh.add(new JPanel(), 3, 0, 1, 1, 0, 0);
		if (nans.size() > 0) {
			String l = nans.toString().substring(1);
			String msg = String
					.format(
							"Undefined value%s for the %s %s ha%s been replaced by its default value %.3f.",
							nans.size() > 1 ? "s" : "", name, l.substring(0, l
									.length() - 1), nans.size() > 1 ? "ve"
									: "s", val);
			JEditorPane label = new JEditorPane("text/html", GUITools.toHTML(
					msg, 80));
			label.setEditable(false);
			Component component;
			if (nans.size() > 20) {
				component = new JScrollPane(label,
						JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
						JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				label.setPreferredSize(new Dimension(450, 450));
			} else {
				component = label;
			}
			JOptionPane.showMessageDialog(this, component,
					"Replacing undefined values",
					JOptionPane.INFORMATION_MESSAGE);
		}
		return panel;
	}

	/**
	 * 
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
	 * 
	 * @see
	 * java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof JCheckBox) {
			JCheckBox chck = (JCheckBox) e.getSource();
			if (chck.getName().equals("grid")) {
				plot.setGridVisible(chck.isSelected());
			} else if (chck.getName().equals("log")) {
				if (chck.isSelected() && !plot.checkLoggable()) {
					chck.setSelected(false);
					chck.setEnabled(false);
					String msg = "Cannot change to logarithmic scale because at least one value on the y-axis is not greater than zero.";
					JOptionPane.showMessageDialog(this, GUITools
							.toHTML(msg, 40), "Warning",
							JOptionPane.WARNING_MESSAGE);
				}
				plot.toggleLog(chck.isSelected());
			} else if (chck.getName().equals("legend")) {
				plot.setShowLegend(chck.isSelected());
			}
		}
	}

	/**
	 * 
	 * @param model
	 * @return
	 */
	private JTable legendTable(Model model) {
		JTable tab = new JTable();
		tab.setName("legend");
		tab.setModel(new LegendTableModel(model, includeReactions));
		tab.setDefaultEditor(Color.class, new ColorEditor());
		LegendTableCellRenderer renderer = new LegendTableCellRenderer();
		tab.setDefaultRenderer(Color.class, renderer);
		tab.setDefaultRenderer(NamedSBase.class, renderer);
		tab.getModel().addTableModelListener(this);
		return tab;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eva2.server.stat.InterfaceStatisticsListener#notifyGenerationPerformed
	 * (java.lang.String[], java.lang.Object[], java.lang.Double[])
	 */
	public void notifyGenerationPerformed(String[] header,
			Object[] statObjects, Double[] statDoubles) {
		// TODO Auto-generated method stub
		for (int i = 0; i < statObjects.length; i++) {
			System.out.printf("%s\t%s\n", i, statObjects[i].getClass()
					.getName());
		}
		System.out.println("notifyGenerationPerformed");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.server.stat.InterfaceStatisticsListener#notifyRunStarted(int,
	 * int, java.lang.String[], java.lang.String[])
	 */
	public void notifyRunStarted(int runNumber, int plannedMultiRuns,
			String[] header, String[] metaInfo) {
		// TODO Auto-generated method stub
		System.out.println("notifyRunStarted");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.server.stat.InterfaceStatisticsListener#notifyRunStopped(int,
	 * boolean)
	 */
	public void notifyRunStopped(int runsPerformed, boolean completedLastRun) {
		// TODO Auto-generated method stub
		System.out.println("notifyRunStopped");
	}

	/**
	 * Plots the given data set with respect to the selected columns in the
	 * legend.
	 * 
	 * @param data
	 * @param connected
	 * @param showLegend
	 */
	public void plot(MultiBlockTable data, boolean connected, boolean showLegend) {
		LegendTableModel legend = (LegendTableModel) this.legend.getModel();
		String name;
		boolean plotColumns[] = new boolean[data.getColumnCount() - 1];
		Color plotColors[] = new Color[data.getColumnCount() - 1];
		String infos[] = new String[data.getColumnCount() - 1];
		for (int i = 0; i < data.getColumnCount() - 1; i++) {
			name = data.getColumnName(i + 1);
			plotColumns[i] = legend.isSelected(name);
			plotColors[i] = legend.getColorFor(name);
			infos[i] = legend.getNameFor(name);
		}
		plot
				.plot(data, connected, showLegend,
						((SimulationPanelFoot) footPanel.getComponent(0))
								.getShowGrid(), plotColumns, plotColors, infos);
	}

	/**
	 * 
	 * @param data
	 * @throws Exception
	 */
	public void setExperimentalData(MultiBlockTable data) throws Exception {
		expTable.setModel(data);
		// deselect non available elements in the legend and select those that
		// are present in the data
		LegendTableModel tabMod = (LegendTableModel) legend.getModel();
		for (int i = 0; i < tabMod.getRowCount(); i++) {
			tabMod.setSelected(i, data.getColumn(tabMod.getId(i)) != null);
		}
		plot(data, false, ((SimulationPanelFoot) footPanel.getComponent(0))
				.getShowLegend());
		tabbedPane.setEnabledAt(2, true);
		((SimulationPanelFoot) footPanel.getComponent(0)).computeDistance(
				model, data.getBlock(0));
	}

	/**
	 * 
	 * @param includeReactions
	 */
	public void setIncludeReactions(boolean includeReactions) {
		this.includeReactions = includeReactions;
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
	public void setProperties(Properties properties)
			throws IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		maxSpinVal = ((Number) properties.get(CfgKeys.SPINNER_MAX_VALUE))
				.doubleValue();
		defaultCompartmentValue = ((Number) properties
				.get(CfgKeys.OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE))
				.doubleValue();
		defaultSpeciesValue = ((Number) properties
				.get(CfgKeys.OPT_DEFAULT_SPECIES_INITIAL_VALUE)).doubleValue();
		defaultParameterValue = ((Number) properties
				.get(CfgKeys.OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS))
				.doubleValue();
		paramStepSize = ((Number) properties.get(CfgKeys.SPINNER_STEP_SIZE))
				.doubleValue();
		double startTime = ((Number) properties.get(CfgKeys.SIM_START_TIME))
				.doubleValue();
		double endTime = ((Number) properties.get(CfgKeys.SIM_END_TIME))
				.doubleValue();
		startTime = Math.max(0, startTime);
		if (startTime > endTime) {
			swap(startTime, endTime);
		}

		maxCompartmentValue = ((Number) properties
				.get(CfgKeys.SIM_MAX_COMPARTMENT_SIZE)).doubleValue();
		maxSpeciesValue = ((Number) properties
				.get(CfgKeys.SIM_MAX_SPECIES_VALUE)).doubleValue();
		maxParameterValue = ((Number) properties
				.get(CfgKeys.SIM_MAX_PARAMETER_VALUE)).doubleValue();

		getOrCreateFootPanel().setProperties(properties);
		removeAll();
		init();
	}

	/**
	 * @param showSettingsPanel
	 *            the showSettingsPanel to set
	 */
	public void setShowSettingsPanel(boolean showSettingsPanel) {
		if (this.showSettingsPanel != showSettingsPanel) {
			this.showSettingsPanel = showSettingsPanel;
			if (!showSettingsPanel) {
				remove(footPanel);
			} else {
				add(footPanel, BorderLayout.SOUTH);
			}
			validate();
		}
	}

	/**
	 * Runs over the legend and sets all variables corresponding to the given
	 * identifiers as selected. All others will be unselected.
	 * 
	 * @param identifiers
	 *            The identifiers of the variables to be selected and to occur
	 *            in the plot.
	 */
	public void setVariables(String[] identifiers) {
		for (int i = 0; i < legend.getRowCount(); i++) {
			legend.setValueAt(Boolean.valueOf(false), i, 0);
			for (String id : identifiers) {
				if (legend.getValueAt(i, 2).toString().trim().equals(id.trim())) {
					legend.setValueAt(Boolean.valueOf(true), i, 0);
					break;
				}
			}
		}
	}

	/**
	 * Conducts the simulation.
	 * 
	 * @throws Exception
	 */
	public void simulate() throws Exception {
		SimulationPanelFoot foot = (SimulationPanelFoot) footPanel
				.getComponent(0);
		simulate(model, foot.getSimulationStartTime(), foot
				.getSimulationEndTime(), foot.getStepSize());
		tabbedPane.setEnabledAt(1, true);
	}

	/**
	 * 
	 * @param model
	 * @param t1val
	 * @param t2val
	 * @param stepSize
	 * @throws Exception
	 */
	private void simulate(Model model, double t1val, double t2val,
			double stepSize) throws Exception {
		SimulationPanelFoot foot = (SimulationPanelFoot) footPanel
				.getComponent(0);
		MultiBlockTable data = foot.solveByStepSize(model, t1val, t2val,
				stepSize);
		simTable.setModel(data);
		plot.clearAll();
		plot(data, true, foot.getShowLegend());
		if (stepSize != foot.getStepSize()) {
			foot.setStepSize(stepSize);
		}
		if (expTable.getColumnCount() > 0) {
			plot((MultiBlockTable) expTable.getModel(), false, foot
					.getShowLegend());
			foot.computeDistance(model, data.getBlock(0));
		}
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
			Variable s = model.findVariable(spin.getName());
			if (s != null && s instanceof Symbol) {
				((Symbol) s).setValue(((SpinnerNumberModel) spin.getModel())
						.getNumber().doubleValue());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seejavax.swing.event.TableModelListener#tableChanged(javax.swing.event.
	 * TableModelEvent)
	 */
	public void tableChanged(TableModelEvent e) {
		if (e.getSource() instanceof LegendTableModel) {
			if ((e.getColumn() == LegendTableModel.getBooleanColumn())
					&& (e.getType() == TableModelEvent.UPDATE)) {
				SimulationPanelFoot foot = (SimulationPanelFoot) footPanel
						.getComponent(0);
				plot.clearAll();
				if (simTable.getRowCount() > 0) {
					plot((MultiBlockTable) simTable.getModel(), true, foot
							.getShowLegend());
				}
				if (expTable.getRowCount() > 0) {
					plot((MultiBlockTable) expTable.getModel(), false, foot
							.getShowLegend());
				}
				if (foot.getShowLegend()) {
					plot.updateLegend();
				}
			}
		}
	}
}
