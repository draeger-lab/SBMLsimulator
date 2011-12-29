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

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.simulator.gui.plot.Plot;
import org.sbml.simulator.gui.plot.PlotOptions;
import org.sbml.simulator.gui.table.LegendTableModel;
import org.simulator.math.odes.MultiTable;

import de.zbit.util.StringUtil;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-20
 * @version $Rev$
 * @since 1.0
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
	 * The step size for the spinner in the interactive parameter scan. and the
	 * maximal value for {@link JSpinner}s.
	 */
	private double maxSpinVal = 1E10, paramStepSize = 0.01d;
	/**
	 * The maximal allowable values.
	 */
	private double maxCompartmentValue = maxSpinVal,
			maxParameterValue = maxSpinVal, maxSpeciesValue = maxSpinVal;
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
	private MultiTable simData, experimentData;

	/**
	 * 
	 */
	public SimulationVisualizationPanel() {
		super(HORIZONTAL_SPLIT, true);
		includeReactions = true;
	}

	/**
	 * @return the experimentData
	 */
	public MultiTable getExperimentData() {
		return experimentData;
	}

	/**
	 * @return
	 */
	public boolean getIncludeReactions() {
		return includeReactions;
	}

	/**
	 * @return
	 */
	public Plot getPlot() {
		return plot;
	}

	/**
	 * @return
	 */
	public Properties getProperties() {
		Properties p = new Properties();
		/*
		 * General settings
		 */
		p.putAll(interactiveScanPanel.getProperties());

		return p;
	}

	/**
	 * @return the simData
	 */
	public MultiTable getSimulationData() {
		return simData;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof AbstractButton) {
			AbstractButton button = (AbstractButton) e.getSource();
			if (button.getActionCommand() != null) {
				String com = button.getActionCommand();
				if (com.equals(PlotOptions.SHOW_PLOT_GRID.getOptionName())) {
					plot.setGridVisible(button.isSelected());
					// } else if
					// (com.equals(PlotOptions.PLOT_LOG_SCALE.getOptionName()))
					// {
					// setPlotToLogScale(button);
				} else if (com.equals(PlotOptions.SHOW_PLOT_LEGEND
						.getOptionName())) {
					plot.setShowLegend(button.isSelected());
				} else if (com.equals(PlotOptions.SHOW_PLOT_TOOLTIPS
						.getOptionName())) {
					plot.setShowGraphToolTips(button.isSelected());
				}
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
	 * Plots the given data set with respect to the selected columns in the
	 * legend.
	 * 
	 * @param data
	 * @param connected
	 * @param clearFirst
	 *            if true, everything already plotted will be removed before
	 *            plotting.
	 */
	private void plot(MultiTable data, boolean connected,
			boolean clearFirst) {
		String name;
		Color plotColors[] = new Color[data.getColumnCount() - 1];
		String infos[] = new String[data.getColumnCount() - 1];
		for (int i = 0; i < data.getColumnCount() - 1; i++) {
			name = data.getColumnName(i + 1);
			LegendTableModel tableModel = legendPanel.getLegendTableModel();
			if (tableModel.isSelected(name)) {
				plotColors[i] = tableModel.getColorFor(name);
				infos[i] = tableModel.getNameFor(name);
			}
		}
		if (clearFirst) {
			plot.clearAll();
		}
		plot.plot(data, connected, plotColors, infos);
	}

	/**
	 * @param data
	 *            the experimentData to set
	 */
	public void setExperimentData(MultiTable data) {
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
	 * @param includeReactions
	 */
	public void setIncludeReactions(boolean includeReactions) {
		this.includeReactions = includeReactions;
	}

	/**
	 * @param enabled
	 */
	public void setInteractiveScanEnabled(boolean enabled) {
		interactiveScanPanel.setAllEnabled(enabled);
	}

	/**
	 * @param model
	 */
	public void setModel(Model model) {
		if (leftComponent != null) {
			remove(leftComponent);
		}
		if (rightComponent != null) {
			remove(rightComponent);
		}
		UnitDefinition timeUnits = model.getTimeUnitsInstance();
		String xLab = "Time";
		if (timeUnits != null) {
			xLab += " in " + UnitDefinition.printUnits(timeUnits, true);
		}
		plot = new Plot(xLab, "Value");
		// get rid of this pop-up menu.
		// TODO: maybe we can make use of this later.
		// MouseListener listeners[] = plot.getMouseListeners();
		// for (int i = listeners.length - 1; i >= 0; i--) {
		// plot.removeMouseListener(listeners[i]);
		// }
		//
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
    SwingUtilities.invokeLater(new Runnable() {
      /* (non-Javadoc)
       * @see java.lang.Runnable#run()
       */
      public void run() {
        legendPanel.updateOrDeriveUnits(); 
      }
    });
	}

	/**
	 * @param button
	 */
	void setPlotToLogScale(AbstractButton button) {
		if (button.isSelected() && !plot.checkLoggable()) {
			button.setSelected(false);
			button.setEnabled(false);
			String msg = "Cannot change to logarithmic scale because at least one value on the y-axis is not greater than zero.";
			JOptionPane.showMessageDialog(this, StringUtil.toHTML(msg, 40),
					"Warning", JOptionPane.WARNING_MESSAGE);
		}
		plot.toggleLog(button.isSelected());
	}

	/**
	 * @param properties
	 */
	public void loadPreferences() {
		if (interactiveScanPanel != null) {
			interactiveScanPanel.loadPreferences();
		}
		maxSpinVal = 2000;
		paramStepSize = .1d;
		maxCompartmentValue = maxSpeciesValue =  maxParameterValue = 1E8;
		updateUI();
	}

	/**
	 * Runs over the legend and sets all variables corresponding to the given
	 * identifiers as selected. All others will be unselected.
	 * 
	 * @param identifiers
	 *            The identifiers of the variables to be selected and to occur
	 *            in the plot.
	 */
	public void setSelectedQuantities(String... identifiers) {
		legendPanel.getLegendTableModel().setSelectedVariables(identifiers);
	}

	/**
	 * @param simData
	 *            the simData to set
	 */
	public void setSimulationData(MultiTable simData) {
		this.simData = simData;
		this.simData.addTableModelListener(this);
		plot();
	}

	/* (non-Javadoc)
	 * @seejavax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
	 */
	public void tableChanged(TableModelEvent e) {
		if (e.getSource() instanceof LegendTableModel) {
			if ((e.getColumn() == LegendTableModel.getColumnPlot() || e
					.getColumn() == LegendTableModel.getColumnColor())
					&& (e.getType() == TableModelEvent.UPDATE)) {
				plot();
			}
		} else if (e.getSource() instanceof MultiTable) {
			setSimulationData((MultiTable) e.getSource());
		}
	}

	/**
	 * 
	 */
	public void unsetExperimentData() {
		setExperimentData(null);
	}

	/**
	 * 
	 */
	public void unsetSimulationData() {
		setSimulationData(null);
	}

	/**
	 * @param id
	 * @param value
	 */
	public void updateQuantity(String id, double value) {
		interactiveScanPanel.updateQuantity(id, value);
	}

}
