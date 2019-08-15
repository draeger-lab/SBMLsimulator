/*
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2016 by the University of Tuebingen, Germany.
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
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBaseWithDerivedUnit;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.simulator.gui.plot.BoxPlotDataset;
import org.sbml.simulator.gui.plot.MetaDataset;
import org.sbml.simulator.gui.plot.Plot;
import org.sbml.simulator.gui.plot.SeriesInfo;
import org.sbml.simulator.gui.plot.XYDatasetAdapter;
import org.sbml.simulator.gui.table.LegendTableModel;
import org.simulator.math.odes.MultiTable;

import de.zbit.sbml.util.HTMLtools;
import de.zbit.util.ResourceManager;
import de.zbit.util.StringUtil;

/**
 * This GUI component is a specialized split pane that contains a {@link Plot}
 * on its right-hand side and another split pane on the left-hand side
 * consisting of a legend of model components with colors, initial values, and
 * units ({@link LegendPanel}), as well as an {@link InteractiveScanPanel}. In
 * this way, this component displays all relevant information about the results
 * of a simulation to the user.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-09-20
 * @since 1.0
 */
public class SimulationVisualizationPanel extends JSplitPane implements
PreferenceChangeListener, TableModelListener {

  /**
   * For localization support.
   */
  private static final transient ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");

  /**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = 2102296020675377066L;
  /**
   *  Experimental results.
   */
  private List<MultiTable> experimentData;
  /**
   * Switches inclusion of reactions in the plot on or off.
   */
  private boolean includeReactions;
  /**
   * 
   */
  private InteractiveScanPanel interactiveScanPanel;

  /**
   * 
   */
  private LegendPanel legendPanel;

  /**
   * The step size for the spinner in the interactive parameter scan. and the
   * maximal value for {@link JSpinner}s.
   */
  private double maxSpinVal = 1E10d, paramStepSize = 0.01d;

  /**
   * The maximal allowable values.
   */
  private double minValue = -maxSpinVal, maxCompartmentValue = maxSpinVal,
      maxParameterValue = maxSpinVal, maxSpeciesValue = maxSpinVal;

  /**
   * Plot area
   */
  private Plot plot;
  /**
   * Results of a dynamic simulation.
   */
  private MultiTable simData;

  /**
   * 
   */
  public SimulationVisualizationPanel() {
    super(HORIZONTAL_SPLIT, true);
    includeReactions = true;
  }

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(SimulationVisualizationPanel.class.getName());

  /**
   * @param data
   *            the experimentData to set
   */
  public void addExperimentData(MultiTable data) {
    // deselect non available elements in the legend and select those that
    // are present in the data
    if (data != null) {
      boolean plot = false;
      if (experimentData == null) {
        experimentData = new LinkedList<MultiTable>();
        plot = true;
      }
      plot |= experimentData.add(data);
      if (plot) {
        plot();
      }
      selectLegendItems(data);
    }
  }

  private void selectLegendItems(MultiTable data) {
    LegendTableModel legend = legendPanel.getLegendTableModel();
    String id;
    int columnIndex;
    boolean selected;
    for (int i = 0; i < legend.getRowCount(); i++) {
      id = legend.getId(i);
      columnIndex = data.getColumnIndex(id);
      selected = columnIndex > -1;
      if (selected) {
        String name = legend.getNameFor(id);
        logger.fine(String.format("Set %s = %s selected = %b", id, name, selected));
        legend.setSelected(i, selected);
      }
    }
  }

  /**
   * @return the experimentData
   */
  public List<MultiTable> getExperimentData() {
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
    minValue = -maxParameterValue;
    updateUI();
  }

  /**
   * 
   */
  public void plot() {
    if ((simData != null) && (simData.getRowCount() > 0)) {
      plot(Arrays.asList(new MultiTable[] {simData}), true, true);
    }
    if ((experimentData != null) && (experimentData.size() > 0)) {
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
  private void plot(List<MultiTable> data, boolean connected, boolean clearFirst) {
    if (data.size() > 0) {
      MetaDataset d;
      if (data.size() == 1) {
        d = new XYDatasetAdapter(data.get(0));
      } else {
        d = new BoxPlotDataset(data);
      }

      String id;
      int seriesCount = d.getSeriesCount();
      Color plotColors[] = new Color[seriesCount];
      String infos[] = new String[seriesCount];
      LegendTableModel tableModel = legendPanel.getLegendTableModel();
      for (int i = 0; i < seriesCount; i++) {
        id = d.getSeriesIdentifier(i);
        if (tableModel.isSelected(id)) {
          plotColors[i] = tableModel.getColorFor(id);
          infos[i] = HTMLtools.createTooltip(tableModel.getSBase(i));
        } else {
          plotColors[i] = null;
          infos[i] = null;
        }
      }
      if (clearFirst && plot != null) {
        plot.clearAll();
      }
      plot.plot(d, connected, plotColors, infos);
    }
  }

  /* (non-Javadoc)
   * @see java.util.prefs.PreferenceChangeListener#preferenceChange(java.util.prefs.PreferenceChangeEvent)
   */
  @Override
  public void preferenceChange(PreferenceChangeEvent evt) {
    plot.preferenceChange(evt);
  }

  /**
   * 
   */
  public void removeExperimentData(int index) {
    if (experimentData != null) {
      experimentData.remove(index);
      LegendTableModel legend = legendPanel.getLegendTableModel();
      for (int i = 0; i < legend.getRowCount(); i++) {
        if (legend.isSelected(i)) {
          boolean selected = false;
          for (int j = 0; (j < experimentData.size()) && !selected; j++) {
            if (experimentData.get(j).getColumn(legend.getId(i)) != null) {
              selected = true;
            }
          }
          legend.setSelected(i, selected);
        }
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
    if (timeUnits == null) {
      timeUnits = new UnitDefinition(model.getLevel(), model.getVersion());
    }
    String title = model.isSetName() ? model.getName() : model.getId();
    plot = new Plot(title, MessageFormat.format(bundle.getString("X_AXIS_LABEL"),
      UnitDefinition.printUnits(timeUnits, true).replace('*', '\u00B7')),
      bundle.getString("Y_AXIS_LABEL"));
    plot.setBorder(BorderFactory.createLoweredBevelBorder());
    // get rid of this pop-up menu.
    // TODO: maybe we can make use of this later.
    // MouseListener listeners[] = plot.getMouseListeners();
    // for (int i = listeners.length - 1; i >= 0; i--) {
    // plot.removeMouseListener(listeners[i]);
    // }
    //
    interactiveScanPanel = new InteractiveScanPanel(model, minValue,
      maxCompartmentValue, maxSpeciesValue, maxParameterValue,
      paramStepSize);
    interactiveScanPanel.setBorder(BorderFactory.createLoweredBevelBorder());
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

  /**
   * @param button
   */
  void setPlotToLogScale(AbstractButton button) {
    if (button.isSelected() && !plot.checkLoggable()) {
      button.setSelected(false);
      button.setEnabled(false);
      JOptionPane.showMessageDialog(this, StringUtil.toHTML(
        bundle.getString("NO_LOGARITHMIC_SCALE_POSSIBLE"), 40), bundle
        .getString("WARNING"), JOptionPane.WARNING_MESSAGE);
    }
    plot.toggleLog(button.isSelected());
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
    if (this.simData != null) {
      this.simData.removeTableModelListener(this);
    }
    this.simData = simData;
    this.simData.addTableModelListener(this);
    //selectLegendItems(simData);
    plot();
  }
  
  /**
   * 
   */
  public void setLegendPanel (LegendPanel legend) {
    legendPanel = legend;  
  }

  /**
   * 
   */
  public void setPlot(Plot plot) {
    this.plot = plot;
  }

  /* (non-Javadoc)
   * @seejavax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
   */
  @Override
  public void tableChanged(TableModelEvent e) {
    if (e.getSource() instanceof LegendTableModel) {
      if (((e.getColumn() == LegendTableModel.getColumnPlot()) || (e
          .getColumn() == LegendTableModel.getColumnColor()))
          && (e.getType() == TableModelEvent.UPDATE)) {
        LegendTableModel legend = (LegendTableModel) e.getSource();
        List<SeriesInfo> items = new LinkedList<SeriesInfo>();
        String id, name, tooltip;
        Color color;
        NamedSBaseWithDerivedUnit nsb;
        SeriesInfo info;
        for (int i = e.getFirstRow(); i <= e.getLastRow(); i++) {
          boolean selected = ((Boolean) legend.getValueAt(i, LegendTableModel.getColumnPlot())).booleanValue();
          nsb = (NamedSBaseWithDerivedUnit) legend.getValueAt(i, LegendTableModel.getNamedSBaseColumn());
          id = nsb.getId();
          name = nsb.isSetName() ? nsb.getName() : null;
          tooltip = HTMLtools.createTooltip(nsb);
          color = selected ? (Color) legend.getValueAt(i, LegendTableModel.getColumnColor()) : null;
          info = new SeriesInfo(id, name, tooltip, color);
          items.add(info);
        }
        plot.setSeriesVisible(items);
      }
    } else if (e.getSource() instanceof MultiTable) {
      setSimulationData((MultiTable) e.getSource());
    }
  
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

  /**
   * 
   */
  public void print() {
    if (plot != null) {
      plot.createChartPrintJob();
    }
  }

}
