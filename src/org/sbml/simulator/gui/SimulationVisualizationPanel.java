/**
 * 
 */
package org.sbml.simulator.gui;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.util.Properties;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.simulator.math.odes.MultiBlockTable;
import org.sbml.squeezer.CfgKeys;

import de.zbit.gui.GUITools;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-20
 */
public class SimulationVisualizationPanel extends JSplitPane implements
		ItemListener, TableModelListener {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 2102296020675377066L;

	/**
	 * Switches inclusion of reactions in the plot on or off.
	 */
	private boolean includeReactions;
	/**
	 * The maximal allowable values.
	 */
	private double maxCompartmentValue, maxParameterValue, maxSpeciesValue;
	/**
	 * The step size for the spinner in the interactive parameter scan. and the
	 * maximal value for {@link JSpinner}s.
	 */
	private double maxSpinVal = 1E10, paramStepSize;
	/**
	 * Plot area
	 */
	private Plot plot;
	/**
	 * 
	 */
	private InteractiveScanPanel interactiveScanPanel;
	/**
	 * 
	 */
	private LegendPanel legendPanel;
	/**
	 * Results of simulation and experiments.
	 */
	private MultiBlockTable simData, experimentData;

	/**
	 * 
	 * @return
	 */
	public Plot getPlot() {
		return plot;
	}

	/**
	 * 
	 * @return
	 */
	public Properties getProperties() {
		Properties p = new Properties();

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
		p.putAll(interactiveScanPanel.getProperties());

		return p;
	}

	/**
	 * 
	 * @param model
	 */
	public SimulationVisualizationPanel(Model model) {
		super(HORIZONTAL_SPLIT, true);

		includeReactions = true;

		UnitDefinition timeUnits = model.getTimeUnitsInstance();
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

		interactiveScanPanel = new InteractiveScanPanel(model,
				maxCompartmentValue, maxSpeciesValue, maxParameterValue,
				paramStepSize);
		interactiveScanPanel
				.setBorder(BorderFactory.createLoweredBevelBorder());
		legendPanel = new LegendPanel(model, includeReactions);
		legendPanel.addTableModelListener(this);
		legendPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		JSplitPane topDown = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
				legendPanel, interactiveScanPanel);
		topDown.setDividerLocation(topDown.getDividerLocation() + 200);
		setLeftComponent(topDown);
		setRightComponent(plot);
		setDividerLocation(topDown.getDividerLocation() + 200);
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
				plot();
			}
		}
	}

	/**
	 * 
	 */
	public void plot() {
		if ((simData != null) && (simData.getRowCount() > 0)) {
			plot(simData, true, true);
		}
		if ((experimentData != null) && (experimentData.getRowCount() > 0)) {
			plot(experimentData, false, simData == null);
		}
	}

	/**
	 * 
	 */
	public void unsetSimulationData() {
		setSimulationData(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof AbstractButton) {
			AbstractButton button = (AbstractButton) e.getSource();
			if (button.getActionCommand() != null) {
				Plot.Command com = Plot.Command.valueOf(button
						.getActionCommand());
				switch (com) {
				case SHOW_GRID:
					plot.setGridVisible(button.isSelected());
					break;
				case LOG_SCALE:
					setPlotToLogScale(button);
					break;
				case SHOW_LEGEND:
					plot.setShowLegend(button.isSelected());
					break;
				case SHOW_TOOL_TIPS:
					plot.setShowGraphToolTips(button.isSelected());
					break;
				default:
					break;
				}
			}
		}
	}

	/**
	 * Plots the given data set with respect to the selected columns in the
	 * legend.
	 * 
	 * @param data
	 * @param connected
	 * @param clearFirst
	 *            if true, everything already plotted will be removed before
	 *            plotting.
	 */
	private void plot(MultiBlockTable data, boolean connected,
			boolean clearFirst) {
		String name;
		Color plotColors[] = new Color[data.getColumnCount() - 1];
		String infos[] = new String[data.getColumnCount() - 1];
		for (int i = 0; i < data.getColumnCount() - 1; i++) {
			name = data.getColumnName(i + 1);
			if (legendPanel.isSelected(name)) {
				plotColors[i] = legendPanel.getColorFor(name);
				infos[i] = legendPanel.getNameFor(name);
			}
		}
		if (clearFirst) {
			plot.clearAll();
		}
		plot.plot(data, connected, plotColors, infos);
	}

	/**
	 * 
	 * @param includeReactions
	 */
	public void setIncludeReactions(boolean includeReactions) {
		this.includeReactions = includeReactions;
	}

	/**
	 * 
	 * @param button
	 */
	void setPlotToLogScale(AbstractButton button) {
		if (button.isSelected() && !plot.checkLoggable()) {
			button.setSelected(false);
			button.setEnabled(false);
			String msg = "Cannot change to logarithmic scale because at least one value on the y-axis is not greater than zero.";
			JOptionPane.showMessageDialog(this, GUITools.toHTML(msg, 40),
					"Warning", JOptionPane.WARNING_MESSAGE);
		}
		plot.toggleLog(button.isSelected());
	}

	/**
	 * 
	 * @param data
	 *            the experimentData to set
	 */
	public void setExperimentData(MultiBlockTable data) {
		// deselect non available elements in the legend and select those that
		// are present in the data
		this.experimentData = data;
		if (experimentData != null) {
			LegendTableModel legend = legendPanel.getLegendTableModel();
			for (int i = 0; i < legend.getRowCount(); i++) {
				legend.setSelected(i,
						experimentData.getColumn(legend.getId(i)) != null);
			}
		}
	}

	/**
	 * 
	 * @param properties
	 */
	public void setProperties(Properties properties) {
		if (interactiveScanPanel != null) {
			interactiveScanPanel.setProperties(properties);
		}

		maxSpinVal = ((Number) properties.get(CfgKeys.SPINNER_MAX_VALUE))
				.doubleValue();
		paramStepSize = ((Number) properties.get(CfgKeys.SPINNER_STEP_SIZE))
				.doubleValue();

		maxCompartmentValue = ((Number) properties
				.get(CfgKeys.SIM_MAX_COMPARTMENT_SIZE)).doubleValue();
		maxSpeciesValue = ((Number) properties
				.get(CfgKeys.SIM_MAX_SPECIES_VALUE)).doubleValue();
		maxParameterValue = ((Number) properties
				.get(CfgKeys.SIM_MAX_PARAMETER_VALUE)).doubleValue();
	}

	/**
	 * Runs over the legend and sets all variables corresponding to the given
	 * identifiers as selected. All others will be unselected.
	 * 
	 * @param identifiers
	 *            The identifiers of the variables to be selected and to occur
	 *            in the plot.
	 */
	public void setSelectedVariables(String... identifiers) {
		legendPanel.getLegendTableModel().setSelectedVariables(identifiers);
	}

	/**
	 * 
	 * @return
	 */
	public boolean getIncludeReactions() {
		return includeReactions;
	}

	/**
	 * @return the experimentData
	 */
	public MultiBlockTable getExperimentData() {
		return experimentData;
	}

	/**
	 * @param simData
	 *            the simData to set
	 */
	public void setSimulationData(MultiBlockTable simData) {
		this.simData = simData;
		plot();
	}

	public void unsetExperimentData() {
		setExperimentData(null);
	}

	/**
	 * @return the simData
	 */
	public MultiBlockTable getSimulationData() {
		return simData;
	}
}
