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

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Properties;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
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
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.NumberFormatter;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.QuantityWithDefinedUnit;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.Symbol;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.Variable;
import org.sbml.jsbml.util.ValuePair;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.math.Distance;
import org.sbml.simulator.math.SBMLinterpreter;
import org.sbml.simulator.math.odes.AbstractDESSolver;
import org.sbml.simulator.math.odes.MultiBlockTable;
import org.sbml.simulator.math.odes.MultiBlockTable.Block.Column;
import org.sbml.simulator.resources.Resource;
import org.sbml.squeezer.CfgKeys;
import org.sbml.squeezer.gui.SettingsDialog;
import org.sbml.squeezer.io.SBFileFilter;
import org.sbml.squeezer.util.HTMLFormula;

import de.zbit.gui.GUITools;
import de.zbit.gui.LayoutHelper;
import de.zbit.io.CSVWriter;
import eva2.gui.FunctionArea;

/**
 * @author Andreas Dr&auml;ger
 * @since 1.4
 * 
 */
class ColorEditor extends AbstractCellEditor implements TableCellEditor,
		ActionListener {
	/**
	 * 
	 */
	public static final String EDIT = "edit";
	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = -3645125690115981580L;
	/**
	 * 
	 */
	private JButton button;
	/**
	 * 
	 */
	private JColorChooser colorChooser;
	/**
	 * 
	 */
	private Color currentColor;
	/**
	 * 
	 */
	private JDialog dialog;

	/**
	 * 
	 */
	public ColorEditor() {
		button = new JButton();
		button.setActionCommand(EDIT);
		button.addActionListener(this);
		button.setBorderPainted(false);

		// Set up the dialog that the button brings up.
		colorChooser = new JColorChooser();
		dialog = JColorChooser.createDialog(button, "Pick a Color", true,
				colorChooser, this, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (EDIT.equals(e.getActionCommand())) {
			// The user has clicked the cell, so
			// bring up the dialog.
			button.setBackground(currentColor);
			colorChooser.setColor(currentColor);
			dialog.setVisible(true);

			fireEditingStopped(); // Make the renderer reappear.

		} else { // User pressed dialog's "OK" button.
			currentColor = colorChooser.getColor();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.CellEditor#getCellEditorValue()
	 */
	public Object getCellEditorValue() {
		return currentColor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax.swing
	 * .JTable, java.lang.Object, boolean, int, int)
	 */
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		currentColor = (Color) value;
		return button;
	}
}

/**
 * A table model that allows easy manipulation of the underlying data.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-04-08
 * @since 1.4
 */
class MyDefaultTableModel extends DefaultTableModel {

	/**
	 * Generated serial version identifier
	 */
	private static final long serialVersionUID = 6339470859385085061L;
	/**
	 * Field to indicate columns that are editable.
	 */
	private boolean[] colEditable;

	/**
	 * 
	 * @param data
	 * @param columnNames
	 */
	public MyDefaultTableModel(Object[][] data, String[] columnNames) {
		super(data, columnNames);
		colEditable = new boolean[columnNames.length];
		Arrays.fill(colEditable, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
	 */
	@Override
	public Class<?> getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.DefaultTableModel#isCellEditable(int, int)
	 */
	@Override
	public boolean isCellEditable(int row, int column) {
		return colEditable[column];
	}

	/**
	 * Decide whether or not the column at the given index should be editable.
	 * 
	 * @param column
	 * @param editable
	 */
	public void setColumnEditable(int column, boolean editable) {
		colEditable[column] = editable;
	}
}

/**
 * @author Andreas Dr&auml;ger
 * @since 1.4
 * @date 2010-04-06
 * 
 */
public class SimulationPanel extends JPanel implements ActionListener,
		ChangeListener, ItemListener, TableModelListener {

	/**
	 * Commands that can be understood by this dialog.
	 * 
	 * @author Andreas Dr&auml;ger
	 * 
	 */
	public static enum Command {
		/**
		 * Open file with experimental data.
		 */
		OPEN_DATA,
		/**
		 * Save the plot as an image.
		 */
		SAVE_PLOT_IMAGE,
		/**
		 * Save the results of the simulation to a CSV file.
		 */
		SAVE_SIMULATION,
		/**
		 * Adjust user's preferences
		 */
		SETTINGS,
		/**
		 * Start a new simulation with the current settings.
		 */
		SIMULATION_START
	}

	/**
	 * Generated serial version identifier
	 */
	private static final long serialVersionUID = -7278034514446047207L;

	/**
	 * 
	 * @param index
	 * @return
	 */
	public static Color indexToColor(int index) {
		switch (index % 10) {
		case 0:
			return Color.black;
		case 1:
			return Color.red;
		case 2:
			return Color.blue;
		case 3:
			return Color.pink;
		case 4:
			return Color.green;
		case 5:
			return Color.gray;
		case 6:
			return Color.magenta;
		case 7:
			return Color.cyan;
		case 8:
			return Color.orange;
		case 9:
			return Color.darkGray;
		}
		return Color.black;
	}

	/**
	 * Compression factor for JPEG output
	 */
	private float compression;

	/**
	 * Table for experimental data.
	 */
	private JTable expTable;

	/**
	 * Table that contains the legend of this plot.
	 */
	private JTable legend;
	/**
	 * Whether or not to plot in a logarithmic scale.
	 */
	private JCheckBox logScale;

	/**
	 * 
	 */
	private double maxCompartmentValue;

	/**
	 * The maximal allowable parameter value.
	 */
	private double maxParameterValue;

	/**
	 * 
	 */
	private double maxSpeciesValue;

	/**
	 * 
	 */
	private double maxSpinVal = 1E10;

	/**
	 * Maximal allowable number of integration steps per time unit
	 */
	private int maxStepsPerUnit;

	/**
	 * The maximal allowable simulation time
	 */
	private double maxTime;

	/**
	 * Model to be simulated
	 */
	private Model model;

	/**
	 * Standard directory to open data files.
	 */
	private String openDir;

	/**
	 * The step size for the spinner in the interactive parameter scan.
	 */
	private double paramStepSize;

	/**
	 * Plot area
	 */
	private FunctionArea plot;

	/**
	 * This is the quote character in CSV files
	 */
	private char quoteChar;

	/**
	 * 
	 */
	private String saveDir;

	/**
	 * This is the separator char in CSV files
	 */
	private char separatorChar;

	/**
	 * Decides whether or not a grid should be displayed in the plot.
	 */
	private JCheckBox showGrid;

	/**
	 * Decides whether or not to add a legend to the plot.
	 */
	private JCheckBox showLegend;

	/**
	 * Table for the simulation data.
	 */
	private JTable simTable;

	/**
	 * The integrator for the simulation
	 */
	private AbstractDESSolver solver;

	/**
	 * The index of the class name of the solver to be used
	 */
	private JComboBox solvers;
	/**
	 * 
	 */
	private SpinnerNumberModel[] spinModSymbol;
	/**
	 * The spinner to change the number of integration steps.
	 */
	private SpinnerNumberModel stepsModel;

	/**
	 * Simulation start time
	 */
	private SpinnerNumberModel t1;

	/**
	 * Simulation end time
	 */
	private SpinnerNumberModel t2;

	/**
	 * Default value to be used for simulation
	 */
	private double defaultCompartmentValue;
	/**
	 * Default value to be used for simulation
	 */
	private double defaultSpeciesValue;
	/**
	 * Default value to be used for simulation
	 */
	private double defaultParameterValue;
	/**
	 * The main tabbed pane showing plot, simulatin and experimental data.
	 */
	private JTabbedPane tabbedPane;
	/**
	 * The toolbar of this element.
	 */
	private JToolBar toolbar;
	/**
	 * Text field to display the quality of a simulation with respect to a given
	 * data set.
	 */
	private JFormattedTextField distField;
	/**
	 * The currently used distance function.
	 */
	private Distance distance;
	/**
	 * Contains all available distance functions.
	 */
	private JComboBox distFun;

	/**
	 * Necessary to remember the originally set distance function.
	 */
	private int distanceFunc;

	/**
	 * Switches inclusion of reactions in the plot on or off.
	 */
	private boolean includeReactions = true;

	/**
	 * 
	 * @param model
	 */
	public SimulationPanel(Model model) {
		super();
		if (SBMLsimulator.getAvailableSolvers().length == 0) {
			String msg = "Could not find any solvers for differential equation systems. A simulation is therefore not possible.";
			JOptionPane.showMessageDialog(this, GUITools.toHTML(msg),
					"No ODE solver available", JOptionPane.WARNING_MESSAGE);
		} else
			try {
				this.model = model;
				setProperties(getDefaultProperties());
			} catch (Exception exc) {
				GUITools.showErrorMessage(this, exc);
			}
	}

	/**
	 * 
	 * @param model
	 * @param settings
	 */
	public SimulationPanel(Model model, Properties settings) {
		this(model);
		try {
			setProperties((Properties) settings.clone());
		} catch (Exception exc) {
			GUITools.showErrorMessage(this, exc);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		switch (Command.valueOf(e.getActionCommand())) {
		case SIMULATION_START:
			try {
				simulate();
			} catch (Exception exc) {
				exc.printStackTrace();
				GUITools.showErrorMessage(this, exc);
			}
			break;
		case OPEN_DATA:
			try {
				openExperimentalData();
			} catch (SBMLException exc) {
				exc.printStackTrace();
				GUITools.showErrorMessage(this, exc);
			}
			break;
		case SAVE_PLOT_IMAGE:
			try {
				savePlotImage();
			} catch (Exception exc) {
				GUITools.showErrorMessage(this, exc);
			}
			break;
		case SAVE_SIMULATION:
			try {
				saveSimulationResults();
			} catch (IOException exc) {
				GUITools.showErrorMessage(this, exc);
			}
			break;
		case SETTINGS:
			adjustPreferences();
			break;
		default:
			JOptionPane.showMessageDialog(this, "Invalid option "
					+ e.getActionCommand(), "Warning",
					JOptionPane.WARNING_MESSAGE);
			break;
		}
	}

	/**
	 * 
	 */
	private void adjustPreferences() {
		try {
			SettingsPanelSimulation ps = new SettingsPanelSimulation();
			ps.setProperties(getProperties());
			SettingsDialog dialog = new SettingsDialog("Simulatin Preferences");
			Properties p = new Properties();
			if (dialog.showSettingsDialog(getProperties(), ps) == SettingsDialog.APPROVE_OPTION) {
				for (Object key : dialog.getSettings().keySet())
					p.put(key, dialog.getSettings().get(key));
				setProperties(p);
			}
		} catch (Exception exc) {
			GUITools.showErrorMessage(this, exc);
		}
	}

	/**
	 * 
	 * @param model
	 * @param stepSize
	 * @return
	 * @throws Exception
	 */
	private double computeDistance(Model model, double stepSize)
			throws Exception {
		MultiBlockTable simData = solveAtTimePoints(model,
				((MultiBlockTable) expTable.getModel()).getTimePoints(),
				stepSize);
		MultiBlockTable expData = (MultiBlockTable) expTable.getModel();
		return distance.distance(simData.getBlock(0), expData.getBlock(0));
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
	private Component createFootPanel() throws IllegalArgumentException,
			SecurityException, InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {

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
		int val = (int) Math.round((t2val - t1val) / solver.getStepSize());
		int min = 1;
		int max = (int) Math.round((t2val - t1val) * maxStepsPerUnit);
		int steps = 1;
		val = Math.max(min, val);
		max = Math.max(max, val);
		stepsModel = new SpinnerNumberModel(val, min, max, steps);
		JPanel sPanel = new JPanel();
		LayoutHelper settings = new LayoutHelper(sPanel);
		settings.add("Start time: ", startTime, new JLabel("Steps: "),
				new JSpinner(stepsModel));
		settings.add(new JPanel());
		settings
				.add("End time: ", endTime, new JLabel("ODE Solver: "), solvers);
		settings.add(new JPanel());
		sPanel.setBorder(BorderFactory.createTitledBorder(" Integration "));

		JPanel dPanel = new JPanel();
		LayoutHelper dSet = new LayoutHelper(dPanel);
		Class<Distance>[] distFunctions = SBMLsimulator.getAvailableDistances();
		String distances[] = new String[distFunctions.length];
		for (int i = 0; i < distFunctions.length; i++) {
			Distance dist = distFunctions[i].getConstructor().newInstance();
			distances[i] = dist.getName();
			if (i == distanceFunc) {
				distance = dist;
			}
		}
		distFun = new JComboBox(distances);
		distFun.setName("distfun");
		distFun.addItemListener(this);
		distFun.setSelectedItem(distanceFunc);
		distField = new JFormattedTextField(new NumberFormatter());
		distField.setEnabled(false);
		dSet.add(distFun);
		dSet.add(new JPanel());
		dSet.add(distField);
		dPanel
				.setBorder(BorderFactory
						.createTitledBorder(" Distance to data "));

		JPanel pPanel = new JPanel();
		LayoutHelper pSet = new LayoutHelper(pPanel);
		pSet.add(showGrid);
		pSet.add(logScale);
		pSet.add(showLegend);
		pPanel.setBorder(BorderFactory.createTitledBorder(" Plot "));

		LayoutHelper aSet = new LayoutHelper(new JPanel());
		aSet.add(sPanel, dPanel, pPanel);

		// Actions
		JButton start = GUITools.createButton("Run", new ImageIcon(
				Resource.class.getResource("img/gear_16.png")), this,
				Command.SIMULATION_START,
				"Perform a simulation run with the current settings.");

		JPanel aPanel = new JPanel();
		aPanel.add(start);

		// Main
		JPanel mPanel = new JPanel(new BorderLayout());
		mPanel.add(aSet.getContainer(), BorderLayout.CENTER);
		mPanel.add(aPanel, BorderLayout.SOUTH);
		return mPanel;
	}

	/**
	 * 
	 * @return
	 */
	private JToolBar createToolBar() {
		JToolBar toolbar = new JToolBar("Tools");
		ImageIcon icon = new ImageIcon(Resource.class
				.getResource("img/folder_16.png"));
		if (icon != null)
			toolbar.add(GUITools.createButton(icon, this, Command.OPEN_DATA,
					"Load  experimental data from file."));
		icon = new ImageIcon(Resource.class.getResource("img/save_16.png"));
		if (icon != null)
			toolbar.add(GUITools
					.createButton(icon, this, Command.SAVE_SIMULATION,
							"Save simulation results to file."));
		icon = new ImageIcon(Resource.class.getResource("img/camera_16.png"));
		if (icon != null) {
			toolbar.add(GUITools.createButton(icon, this,
					Command.SAVE_PLOT_IMAGE, "Save plot in an image."));
		}
		icon = new ImageIcon(Resource.class.getResource("img/settings_16.png"));
		if (icon != null)
			toolbar.add(GUITools.createButton(icon, this, Command.SETTINGS,
					"Adjust your preferences"));
		GUITools.setEnabled(false, toolbar, Command.SAVE_PLOT_IMAGE,
				Command.SAVE_SIMULATION);
		return toolbar;
	}

	/**
	 * 
	 */
	private Properties getDefaultProperties() {
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
		p.put(CfgKeys.SIM_STEP_SIZE, Double.valueOf(solver.getStepSize()));
		p.put(CfgKeys.SIM_MAX_COMPARTMENT_SIZE, Double
				.valueOf(maxCompartmentValue));
		p.put(CfgKeys.SIM_MAX_SPECIES_VALUE, Double.valueOf(maxSpeciesValue));
		p.put(CfgKeys.SIM_MAX_PARAMETER_VALUE, Double
				.valueOf(maxParameterValue));
		p.put(CfgKeys.SIM_MAX_STEPS_PER_UNIT_TIME, Integer
				.valueOf(maxStepsPerUnit));
		p.put(CfgKeys.SIM_DISTANCE_FUNCTION, distance.getClass().getName());
		p.put(CfgKeys.SIM_ODE_SOLVER,
				SBMLsimulator.getAvailableSolvers()[solvers.getSelectedIndex()]
						.getName());

		/*
		 * Plot
		 */
		p.put(CfgKeys.PLOT_SHOW_GRID, Boolean.valueOf(showGrid.isSelected()));
		p.put(CfgKeys.PLOT_SHOW_LEGEND, Boolean
				.valueOf(showLegend.isSelected()));
		p.put(CfgKeys.PLOT_LOG_SCALE, Boolean.valueOf(logScale.isSelected()));

		/*
		 * CSV file parsing
		 */
		p.put(CfgKeys.CSV_FILES_OPEN_DIR, openDir);
		p.put(CfgKeys.CSV_FILES_SAVE_DIR, saveDir);
		p.put(CfgKeys.CSV_FILES_SEPARATOR_CHAR, Character
				.valueOf(separatorChar));
		p.put(CfgKeys.CSV_FILES_QUOTE_CHAR, Character.valueOf(quoteChar));

		/*
		 * General settings
		 */
		p.put(CfgKeys.SPINNER_STEP_SIZE, Double.valueOf(paramStepSize));
		p.put(CfgKeys.JPEG_COMPRESSION_FACTOR, Float.valueOf(compression));
		p.put(CfgKeys.SPINNER_MAX_VALUE, Double.valueOf(maxSpinVal));

		p.put(CfgKeys.OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE, Double
				.valueOf(defaultCompartmentValue));
		p.put(CfgKeys.OPT_DEFAULT_SPECIES_INITIAL_VALUE, Double
				.valueOf(defaultSpeciesValue));
		p.put(CfgKeys.OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS, Double
				.valueOf(defaultParameterValue));

		return p;
	}

	/***
	 * Initializes the graphics components of this panel.
	 * 
	 * @param settings
	 */
	private void init() {
		try {
			UnitDefinition timeUnits = this.model.getTimeUnitsInstance();
			String xLab = "Time";
			if (timeUnits != null)
				xLab += " in " + UnitDefinition.printUnits(timeUnits, true);
			plot = new FunctionArea(xLab, "Value");
			plot.setGridVisible(showGrid.isSelected());
			plot.setShowLegend(showLegend.isSelected());
			// get rid of this pop-up menu.
			MouseListener listeners[] = plot.getMouseListeners();
			for (int i = listeners.length - 1; i >= 0; i--) {
				plot.removeMouseListener(listeners[i]);
			}

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
			JSplitPane leftRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					true, topDown, plot);
			leftRight.setDividerLocation(topDown.getDividerLocation() + 200);
			tabbedPane = new JTabbedPane();
			tabbedPane.add("Plot ", leftRight);
			tabbedPane.add("Simulated data", simPanel);
			tabbedPane.add("Experimental data", expPanel);
			tabbedPane.setEnabledAt(0, true);
			tabbedPane.setEnabledAt(1, false);
			tabbedPane.setEnabledAt(2, false);

			setLayout(new BorderLayout());
			toolbar = createToolBar();
			add(toolbar, BorderLayout.NORTH);
			add(tabbedPane, BorderLayout.CENTER);
			add(createFootPanel(), BorderLayout.SOUTH);
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
			JOptionPane.showMessageDialog(this, new JLabel(GUITools.toHTML(msg,
					60)), "Replacing undefined values",
					JOptionPane.INFORMATION_MESSAGE);
		}
		return panel;
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
		} else if (e.getSource() instanceof JComboBox) {
			JComboBox comBox = (JComboBox) e.getSource();
			if (comBox.getName().equals("distfun")) {
				try {
					distanceFunc = comBox.getSelectedIndex();
					distance = SBMLsimulator.getAvailableDistances()[distanceFunc]
							.getConstructor().newInstance();
					if (expTable.getRowCount() > 0) {
						distField.setText(Double.toString(computeDistance(
								model, solver.getStepSize())));
					}
					distField.setEditable(false);
					distField.setEnabled(true);
				} catch (Exception exc) {
					GUITools.showErrorMessage(this, exc);
				}
			} else if (comBox.getName().equals("solvers")) {
				Class<AbstractDESSolver>[] solFun = SBMLsimulator
						.getAvailableSolvers();
				for (int i = 0; i < solFun.length; i++) {
					try {
						Class<AbstractDESSolver> c = solFun[i];
						solver = c.getConstructor().newInstance();
						if (solver.getName().equals(
								solvers.getSelectedItem().toString())) {
							break;
						}
					} catch (Exception exc) {
						GUITools.showErrorMessage(this, exc);
					}
				}
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
		tab.setModel(new TableModelLedgend(model, includeReactions));
		tab.setDefaultEditor(Color.class, new ColorEditor());
		tab.setDefaultRenderer(Color.class, new TableCellRendererObjects());
		tab.setDefaultRenderer(Symbol.class, new TableCellRendererObjects());
		tab.getModel().addTableModelListener(this);
		return tab;
	}

	/**
	 * @throws SBMLException
	 * 
	 */
	private void openExperimentalData() throws SBMLException {
		JFileChooser chooser = GUITools.createJFileChooser(openDir, false,
				false, JFileChooser.FILES_ONLY, SBFileFilter.CSV_FILE_FILTER);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				openExperimentalData(chooser.getSelectedFile());
			} catch (Exception exc) {
				exc.printStackTrace();
				GUITools.showErrorMessage(this, exc);
			}
		}
	}

	/**
	 * 
	 * @param file
	 * @throws Exception
	 */
	public void openExperimentalData(File file) throws Exception {
		openDir = file.getParent();
		CSVDataImporter importer = new CSVDataImporter();
		MultiBlockTable data = importer.convert(model, file.getAbsolutePath());
		expTable.setModel(data);
		plot(data, false, showLegend.isSelected());
		tabbedPane.setEnabledAt(2, true);
		GUITools.setEnabled(false, toolbar, Command.OPEN_DATA);
		distField.setText(Double.toString(computeDistance(model, solver
				.getStepSize())));
		distField.setEditable(false);
		distField.setEnabled(true);
	}

	/**
	 * 
	 * @param path
	 * @throws Exception
	 */
	public void openExperimentalData(String path) throws Exception {
		openExperimentalData(new File(path));
	}

	/**
	 * Plots a matrix either by displaying unconnected or connected points
	 * depending on the connected parameter.
	 * 
	 * @param solution
	 *            The solution of a simulation, where the first column is
	 *            assumed to contain the simulation time.
	 * @param connected
	 *            If true, all points will be connected, singular points are
	 *            plotted for false.
	 */
	private void plot(MultiBlockTable solution, boolean connected,
			boolean showLegend) {
		TableModelLedgend tabMod = (TableModelLedgend) legend.getModel();
		int i, j, graphLabel;
		for (i = 1; i < solution.getColumnCount(); i++) {
			Column column = solution.getColumn(i);
			if (tabMod.isSelected(column.getId())) {
				graphLabel = connected ? i : i + solution.getColumnCount();
				// plot.clearGraph(graphLabel);
				plot.setGraphColor(graphLabel, tabMod.getColorFor(column
						.getId()));
				plot.setInfoString(graphLabel, tabMod
						.getNameFor(column.getId()), 1);
				for (j = 0; j < column.getRowCount(); j++) {
					if (connected) {
						plot.setConnectedPoint(solution.getTimePoint(j), column
								.getValue(j), graphLabel);
					} else {
						plot.setUnconnectedPoint(solution.getTimePoint(j),
								column.getValue(j), graphLabel);
					}
				}
			}
		}
		plot.setGridVisible(showGrid.isSelected());
		plot.setShowLegend(showLegend);
	}

	/**
	 * @throws AWTException
	 * @throws IOException
	 * 
	 */
	private void savePlotImage() throws AWTException, IOException {
		Rectangle area = plot.getBounds();
		area.setLocation(plot.getLocationOnScreen());
		BufferedImage bufferedImage = (new Robot()).createScreenCapture(area);
		JFileChooser fc = GUITools.createJFileChooser(saveDir, false, false,
				JFileChooser.FILES_ONLY, SBFileFilter.PNG_FILE_FILTER,
				SBFileFilter.JPEG_FILE_FILTER);
		if (fc.showSaveDialog(plot) == JFileChooser.APPROVE_OPTION
				&& !fc.getSelectedFile().exists()
				|| (GUITools.overwriteExistingFile(this, fc.getSelectedFile()))) {
			this.saveDir = fc.getSelectedFile().getParent();
			File file = fc.getSelectedFile();
			if (SBFileFilter.isPNGFile(file))
				ImageIO.write(bufferedImage, "png", file);
			else if (SBFileFilter.isJPEGFile(file)) {
				FileImageOutputStream out = new FileImageOutputStream(file);
				ImageWriter encoder = (ImageWriter) ImageIO
						.getImageWritersByFormatName("JPEG").next();
				JPEGImageWriteParam param = new JPEGImageWriteParam(null);
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(compression);
				encoder.setOutput(out);
				encoder.write((IIOMetadata) null, new IIOImage(bufferedImage,
						null, null), param);
				out.close();
			}
		}
	}

	/**
	 * @throws IOException
	 * 
	 */
	private void saveSimulationResults() throws IOException {
		if (simTable.getRowCount() > 0) {
			JFileChooser fc = GUITools.createJFileChooser(saveDir, false,
					false, JFileChooser.FILES_ONLY,
					SBFileFilter.CSV_FILE_FILTER);
			if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				File out = fc.getSelectedFile();
				this.saveDir = out.getParent();
				if (!out.exists() || GUITools.overwriteExistingFile(this, out)) {
					CSVWriter writer = new CSVWriter();
					writer.write(simTable.getModel(), separatorChar, out);
				}
			}
		} else {
			String msg = "No simulation has been performed yet. Please run the simulation first.";
			JOptionPane.showMessageDialog(this, GUITools.toHTML(msg, 40));
		}
	}

	/**
	 * Assign properties from the settings
	 * 
	 * @param settings
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 */
	public void setProperties(Properties settings)
			throws IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		maxSpinVal = ((Number) settings.get(CfgKeys.SPINNER_MAX_VALUE))
				.doubleValue();
		defaultCompartmentValue = ((Number) settings
				.get(CfgKeys.OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE))
				.doubleValue();
		defaultSpeciesValue = ((Number) settings
				.get(CfgKeys.OPT_DEFAULT_SPECIES_INITIAL_VALUE)).doubleValue();
		defaultParameterValue = ((Number) settings
				.get(CfgKeys.OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS))
				.doubleValue();
		paramStepSize = ((Number) settings.get(CfgKeys.SPINNER_STEP_SIZE))
				.doubleValue();
		double startTime = ((Number) settings.get(CfgKeys.SIM_START_TIME))
				.doubleValue();
		double endTime = ((Number) settings.get(CfgKeys.SIM_END_TIME))
				.doubleValue();
		startTime = Math.max(0, startTime);
		if (startTime > endTime) {
			swap(startTime, endTime);
		}
		double stepSize = ((Number) settings.get(CfgKeys.SIM_STEP_SIZE))
				.doubleValue();
		maxTime = Math.max(((Number) settings.get(CfgKeys.SIM_MAX_TIME))
				.doubleValue(), Math.max(startTime, endTime));
		t1 = new SpinnerNumberModel(startTime, 0, maxTime, stepSize);
		t2 = new SpinnerNumberModel(endTime, Math.min(((Number) t1.getValue())
				.doubleValue(), endTime), maxTime, stepSize);
		showGrid = GUITools.createJCheckBox("Grid", ((Boolean) settings
				.get(CfgKeys.PLOT_SHOW_GRID)).booleanValue(), "grid", this,
				"Decide whether or not to draw a grid in the plot area.");
		String toolTip = "Select this checkbox if the y-axis should be drawn in a logarithmic scale. This is, however, only possible if all values are greater than zero.";
		logScale = GUITools.createJCheckBox("Log", ((Boolean) settings
				.get(CfgKeys.PLOT_LOG_SCALE)).booleanValue(), "log", this,
				toolTip);
		showLegend = GUITools.createJCheckBox("Legend", ((Boolean) settings
				.get(CfgKeys.PLOT_SHOW_LEGEND)).booleanValue(), "legend", this,
				"Add or remove a legend in the plot.");
		maxStepsPerUnit = ((Integer) settings
				.get(CfgKeys.SIM_MAX_STEPS_PER_UNIT_TIME)).intValue();
		openDir = settings.get(CfgKeys.CSV_FILES_OPEN_DIR).toString();
		separatorChar = ((Character) settings
				.get(CfgKeys.CSV_FILES_SEPARATOR_CHAR)).charValue();
		quoteChar = ((Character) settings.get(CfgKeys.CSV_FILES_QUOTE_CHAR))
				.charValue();
		maxCompartmentValue = ((Number) settings
				.get(CfgKeys.SIM_MAX_COMPARTMENT_SIZE)).doubleValue();
		maxSpeciesValue = ((Number) settings.get(CfgKeys.SIM_MAX_SPECIES_VALUE))
				.doubleValue();
		maxParameterValue = ((Number) settings
				.get(CfgKeys.SIM_MAX_PARAMETER_VALUE)).doubleValue();
		saveDir = settings.get(CfgKeys.CSV_FILES_SAVE_DIR).toString();
		compression = Float.parseFloat(settings.get(
				CfgKeys.JPEG_COMPRESSION_FACTOR).toString());
		/*
		 * Solver and distance.
		 */
		Class<Distance>[] distFun = SBMLsimulator.getAvailableDistances();
		Class<AbstractDESSolver>[] solFun = SBMLsimulator.getAvailableSolvers();
		distanceFunc = 0;
		while (distanceFunc < distFun.length
				&& !distFun[distanceFunc].getName().equals(
						settings.get(CfgKeys.SIM_DISTANCE_FUNCTION))) {
			distanceFunc++;
		}
		if (this.distFun != null) {
			this.distFun.setSelectedIndex(distanceFunc);
		}
		solvers = new JComboBox();
		for (int i = 0; i < solFun.length; i++) {
			Class<AbstractDESSolver> c = solFun[i];
			solver = c.getConstructor().newInstance();
			solvers.addItem(solver.getName());
			if (c.getName().equals(settings.get(CfgKeys.SIM_ODE_SOLVER))) {
				solvers.setSelectedIndex(i);
			}
		}
		solvers.setEnabled(solvers.getItemCount() > 1);
		if (solvers.getSelectedIndex() != solvers.getItemCount() - 1) {
			solver = solFun[solvers.getSelectedIndex()].getConstructor()
					.newInstance();
		}
		solvers.setName("solvers");
		solvers.addItemListener(this);
		removeAll();
		init();
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
		double t1val = ((Double) t1.getValue()).doubleValue();
		double t2val = ((Double) t2.getValue()).doubleValue();
		double stepSize = (t2val - t1val)
				/ stepsModel.getNumber().doubleValue();
		simulate(model, t1val, t2val, stepSize);
		tabbedPane.setEnabledAt(1, true);
		GUITools.setEnabled(true, toolbar, Command.SAVE_SIMULATION,
				Command.SAVE_PLOT_IMAGE);
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
		MultiBlockTable data = solveByStepSize(model, t1val, t2val, stepSize);
		simTable.setModel(data);
		plot.clearAll();
		plot(data, true, showLegend.isSelected());
		if (expTable.getColumnCount() > 0) {
			plot((MultiBlockTable) expTable.getModel(), false, showLegend
					.isSelected());
			distField
					.setText(Double.toString(computeDistance(model, stepSize)));
			distField.setEditable(false);
			distField.setEnabled(true);
		}
	}

	/**
	 * 
	 * @param model
	 * @param timePoints
	 * @param stepSize
	 * @return
	 * @throws Exception
	 */
	private MultiBlockTable solveAtTimePoints(Model model, double times[],
			double stepSize) throws Exception {
		SBMLinterpreter interpreter = new SBMLinterpreter(model);
		solver.setStepSize(stepSize);
		solver.setIncludeIntermediates(false);
		MultiBlockTable solution = solver.solve(interpreter, interpreter
				.getInitialValues(), times);
		if (solver.isUnstable()) {
			JOptionPane.showMessageDialog(this, "Unstable!",
					"Simulation not possible", JOptionPane.WARNING_MESSAGE);
		}
		return solution;
	}

	/**
	 * 
	 * @param model
	 * @param t1
	 *            Time begin
	 * @param t2
	 *            Time end
	 * @param stepSize
	 * @return
	 * @throws Exception
	 */
	private MultiBlockTable solveByStepSize(Model model, double t1, double t2,
			double stepSize) throws Exception {
		SBMLinterpreter interpreter = new SBMLinterpreter(model);
		solver.setStepSize(stepSize);
		solver.setIncludeIntermediates(includeReactions);
		MultiBlockTable solution = solver.solve(interpreter, interpreter
				.getInitialValues(), t1, t2);
		if (solver.isUnstable()) {
			JOptionPane.showMessageDialog(this, "Unstable!",
					"Simulation not possible", JOptionPane.WARNING_MESSAGE);
		}
		return solution;
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
			} else {
				Variable s = model.findVariable(spin.getName());
				if (s != null && s instanceof Symbol) {
					((Symbol) s)
							.setValue(((SpinnerNumberModel) spin.getModel())
									.getNumber().doubleValue());
				}
			}
		}
	}

	/**
	 * Swaps a and b if a is greater then b.
	 * 
	 * @param a
	 * @param b
	 */
	private void swap(double a, double b) {
		if (a > b) {
			double swap = b;
			b = a;
			a = swap;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seejavax.swing.event.TableModelListener#tableChanged(javax.swing.event.
	 * TableModelEvent)
	 */
	public void tableChanged(TableModelEvent e) {
		if (e.getSource() instanceof TableModelLedgend) {
			if ((e.getColumn() == TableModelLedgend.getBooleanColumn())
					&& (e.getType() == TableModelEvent.UPDATE)) {
				plot.clearAll();
				if (simTable.getRowCount() > 0) {
					plot((MultiBlockTable) simTable.getModel(), true,
							showLegend.isSelected());
				}
				if (expTable.getRowCount() > 0) {
					plot((MultiBlockTable) expTable.getModel(), false,
							showLegend.isSelected());
				}
				if (showLegend.isSelected()) {
					plot.updateLegend();
				}
			}
		}
	}
}

/**
 * 
 * @author Andreas Dr&auml;ger
 * @since 1.4
 * @date 2010-04-07
 */
class TableCellRendererObjects extends JLabel implements TableCellRenderer {

	/**
	 * Generated serial version identifier
	 */
	private static final long serialVersionUID = 5233542392522297524L;

	/**
	 * 
	 */
	public TableCellRendererObjects() {
		super();
		setOpaque(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax
	 * .swing.JTable, java.lang.Object, boolean, boolean, int, int)
	 */
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		setBackground(Color.WHITE);
		if (value instanceof Color) {
			Color newColor = (Color) value;
			setToolTipText("RGB value: " + newColor.getRed() + ", "
					+ newColor.getGreen() + ", " + newColor.getBlue());
			setBackground(newColor);
		} else if (value instanceof Symbol) {
			Symbol s = (Symbol) value;
			setText(s.isSetName() ? s.getName() : s.getId());
			setBackground(Color.WHITE);
		} else
			setText(value.toString());
		return this;
	}

}

/**
 * 
 * @author Andreas Dr&auml;ger
 * @since 1.4
 * @date 2010-04-07
 * 
 */
class TableModelLedgend extends AbstractTableModel {

	/**
	 * Column indices for the content
	 */
	private static final int boolCol = 0, colorCol = 1, nsbCol = 2;

	/**
	 * 
	 * @return
	 */
	public static int getBooleanColumn() {
		return boolCol;
	}

	/**
	 * 
	 * @return
	 */
	public static int getColorColumn() {
		return colorCol;
	}

	/**
	 * 
	 * @return
	 */
	public static int getNamedSBaseColumn() {
		return nsbCol;
	}

	/**
	 * So save run time memorize the last queried key of the hash.
	 */
	private ValuePair<String, Integer> lastQueried;

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 7360401460080111135L;
	/**
	 * A colored button for each model component and
	 */
	private Object[][] data;

	/**
	 * A mapping between the ids in the table and the corresponding row.
	 */
	private Hashtable<String, Integer> id2Row;

	/**
	 * 
	 * @param model
	 */
	public TableModelLedgend(Model model) {
		this(model, false);
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public String getNameFor(String id) {
		int index = getRowFor(id);
		if (index >= 0) {
			return getValueAt(index, nsbCol).toString();
		}
		throw new NoSuchElementException(id);
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public Color getColorFor(String id) {
		int index = getRowFor(id);
		if (index >= 0) {
			return (Color) getValueAt(index, colorCol);
		}
		throw new NoSuchElementException(id);
	}

	/**
	 * 
	 * @param model
	 * @param includeReactions
	 */
	public TableModelLedgend(Model model, boolean includeReactions) {
		int dim = model.getNumCompartments() + model.getNumSpecies()
				+ model.getNumParameters();
		if (includeReactions) {
			dim += model.getNumReactions();
		}
		id2Row = new Hashtable<String, Integer>();
		lastQueried = null;
		data = new Object[dim][3];
		NamedSBase sb;
		int i, j;
		for (i = 0; i < model.getNumCompartments(); i++) {
			sb = model.getCompartment(i);
			data[i][boolCol] = Boolean.TRUE;
			data[i][colorCol] = SimulationPanel.indexToColor(i);
			data[i][nsbCol] = sb;
			id2Row.put(sb.getId(), Integer.valueOf(i));
		}
		j = model.getNumCompartments();
		for (i = 0; i < model.getNumSpecies(); i++) {
			sb = model.getSpecies(i);
			data[i + j][boolCol] = Boolean.TRUE;
			data[i + j][colorCol] = SimulationPanel.indexToColor(i + j);
			data[i + j][nsbCol] = sb;
			id2Row.put(sb.getId(), Integer.valueOf(i + j));
		}
		j = model.getNumCompartments() + model.getNumSpecies();
		for (i = 0; i < model.getNumParameters(); i++) {
			sb = model.getParameter(i);
			data[i + j][boolCol] = Boolean.TRUE;
			data[i + j][colorCol] = SimulationPanel.indexToColor(i + j);
			data[i + j][nsbCol] = sb;
			id2Row.put(sb.getId(), Integer.valueOf(i + j));
		}
		if (includeReactions) {
			j = model.getNumSymbols();
			for (i = 0; i < model.getNumReactions(); i++) {
				sb = model.getReaction(i);
				data[i + j][boolCol] = Boolean.TRUE;
				data[i + j][colorCol] = SimulationPanel.indexToColor(i + j);
				data[i + j][nsbCol] = sb;
				id2Row.put(sb.getId(), Integer.valueOf(i + j));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
	 */
	@Override
	public Class<?> getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return data.length > 0 ? data[0].length : 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(int column) {
		if (column == boolCol)
			return "Plot";
		if (column == colorCol)
			return "Color";
		if (column == nsbCol)
			return "Symbol";
		throw new IndexOutOfBoundsException("Only " + getColumnCount()
				+ " columns, no column " + column);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	public int getRowCount() {
		return data.length;
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public int getRowFor(String id) {
		int index = -1;
		if (lastQueried == null) {
			lastQueried = new ValuePair<String, Integer>(id, Integer
					.valueOf(index));
		} else if (id.equals(lastQueried.getA())) {
			return lastQueried.getB().intValue();
		}
		if (id2Row.containsKey(id)) {
			index = id2Row.get(id).intValue();
			lastQueried.setA(id);
			lastQueried.setB(Integer.valueOf(index));
		}
		return index;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == nsbCol) {
			NamedSBase nsb = (NamedSBase) data[rowIndex][columnIndex];
			return nsb.isSetName() ? nsb.getName() : nsb.getId();
		}
		return data[rowIndex][columnIndex];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
	 */
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if ((columnIndex < getColumnCount()) && (rowIndex < getRowCount())) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public boolean isSelected(String id) {
		int index = getRowFor(id);
		if (index >= 0) {
			return ((Boolean) getValueAt(index, boolCol)).booleanValue();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object,
	 * int, int)
	 */
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		data[rowIndex][columnIndex] = aValue;
		fireTableCellUpdated(rowIndex, columnIndex);
	}
}
