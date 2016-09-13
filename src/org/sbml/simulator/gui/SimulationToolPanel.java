/*
 * $Id$
 * $URL$
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

import static de.zbit.util.Utils.getMessage;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.sbml.optimization.problem.EstimationOptions;
import org.sbml.simulator.QualityMeasurement;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.SimulationConfiguration;
import org.sbml.simulator.SimulationManager;
import org.sbml.simulator.SimulationOptions;
import org.sbml.simulator.gui.plot.PlotOptions;
import org.simulator.math.QualityMeasure;
import org.simulator.math.odes.AbstractDESSolver;

import de.zbit.gui.GUITools;
import de.zbit.gui.actioncommand.ActionCommandRenderer;
import de.zbit.gui.layout.LayoutHelper;
import de.zbit.util.ResourceManager;
import de.zbit.util.StringUtil;
import de.zbit.util.objectwrapper.ValuePair;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.SBPreferences;

/**
 * A simple tool panel that contains a minimum of options that help users to
 * influence the way how a simulation is performed.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-09-16
 * @version $Rev$
 * @since 1.0
 */
public class SimulationToolPanel extends JPanel implements ItemListener,
ChangeListener, PreferenceChangeListener, PropertyChangeListener {

  /**
   * For a better localization.
   */
  private static final transient ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(SimulationToolPanel.class.getName());

  /**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = -6540887561618807199L;

  /**
   * 
   */
  private List<PreferenceChangeListener> listOfPreferenceChangeListeners;

  /**
   * Text field to display the quality of a simulation with respect to a given
   * data set.
   */
  private JFormattedTextField qualityMeasureField;

  /**
   * Contains all available quality measure functions and ODE solvers.
   */
  private JComboBox qualityMeasureFunctions, solverComboBox;

  /**
   * Decides whether or not a grid, a legend, or tool tips should be displayed
   * in the plot.
   */
  private JCheckBox showGrid, showLegend, showToolTips;

  /**
   * 
   */
  private SimulationManager simulationManager;

  /**
   * 
   */
  private JSpinner startTime;

  /**
   * Simulation start and end time as well as the number of integration steps
   */
  private SpinnerNumberModel t1, t2, stepsModel;

  /**
   * @param worker
   */
  public SimulationToolPanel(SimulationManager simulationManager) {
    super(new BorderLayout());
    // Set basic properties:
    listOfPreferenceChangeListeners = new LinkedList<PreferenceChangeListener>();
    this.simulationManager = simulationManager;

    // Create content:
    LayoutHelper aSet = new LayoutHelper(new JPanel());
    aSet.add(createIntegrationPanel(), 0, 0, 1, 1, .33d, 0d);
    aSet.add(createQualityPanel(), 1, 0, 1, 1, .33d, 0d);
    aSet.add(createPlotPanel(), 2, 0, 1, 1, .33d, 0d);
    add(aSet.getContainer(), BorderLayout.CENTER);

    // Initialize and add listeners:
    this.simulationManager.addPropertyChangeListener(this);
    SimulationConfiguration simulationConfiguration = simulationManager.getSimulationConfiguration();
    QualityMeasurement qualityMeasurement = simulationManager.getQualityMeasurement();
    addPreferenceChangeListener(simulationConfiguration);
    addPropertyChangeListener(simulationConfiguration);
    addPropertyChangeListener(qualityMeasurement);
  }

  /**
   * 
   * @param listener
   * @return
   */
  public boolean addPreferenceChangeListener(PreferenceChangeListener listener) {
    return listOfPreferenceChangeListeners.add(listener);
  }

  /**
   * 
   * @return
   */
  private Component createIntegrationPanel() {
    SimulationConfiguration simulationConfiguration = simulationManager.getSimulationConfiguration();
    double maxTime = 1E5d, spinnerStepSize = .01d;
    double t1val = simulationConfiguration.getStart();
    double t2val = simulationConfiguration.getEnd();

    t1 = new SpinnerNumberModel(t1val, 0d, Math.max(t1val, maxTime), spinnerStepSize);
    t2 = new SpinnerNumberModel(t2val, 0d, Math.max(t2val, maxTime), spinnerStepSize);
    startTime = GUITools.createJSpinner(t1,
      SimulationOptions.SIM_START_TIME.toString(),
      SimulationOptions.SIM_START_TIME.getDescription(), false, this);
    JSpinner endTime = GUITools.createJSpinner(t2,
      SimulationOptions.SIM_END_TIME.toString(),
      SimulationOptions.SIM_END_TIME.getDescription(), this);

    int maxStepsPerUnit = 1000;
    int val = numSteps(t1val, t2val, spinnerStepSize);
    int min = 1, max = (int) Math.round((t2val - t1val) * maxStepsPerUnit);
    int steps = 1;

    val = Math.max(min, val);
    max = Math.max(max, val);
    stepsModel = new SpinnerNumberModel(val, min, max, steps);
    JSpinner stepsSpin = GUITools.createJSpinner(stepsModel, "stepCount",
      bundle.getString("STEP_COUNT_TOOLTIP"), this);
    JPanel sPanel = new JPanel();
    LayoutHelper settings = new LayoutHelper(sPanel);

    settings.add(SimulationOptions.SIM_START_TIME.getDisplayName(), startTime, 0, 0, 1, 0);
    settings.add(new JPanel(), 3, 0, 1, 1, 0, 0);
    settings.add(bundle.getString("STEP_COUNT"), stepsSpin, 4, 0, 1, 0);
    settings.add(new JPanel());
    settings.add(SimulationOptions.SIM_END_TIME.getDisplayName(), endTime, 0, 2, 1, 0);
    settings.add(SimulationOptions.ODE_SOLVER.getDisplayName(),
      createSolversCombobox(simulationConfiguration.getSolver().getClass()
        .getName()), 4, 2, 1, 0);
    settings.add(new JPanel());
    sPanel.setBorder(BorderFactory.createTitledBorder(
      ' ' + SimulationOptions.SIMULATION_CONFIGURATION.getName() + ' '));
    return sPanel;
  }

  /**
   * 
   * @return
   */
  private Component createPlotPanel() {
    SBPreferences prefs = SBPreferences.getPreferencesFor(PlotOptions.class);
    JPanel pPanel = new JPanel();
    LayoutHelper pSet = new LayoutHelper(pPanel);
    showGrid = GUITools.createJCheckBox(PlotOptions.SHOW_PLOT_GRID, prefs, this);
    showLegend = GUITools.createJCheckBox(PlotOptions.SHOW_PLOT_LEGEND, prefs, this);
    showToolTips = GUITools.createJCheckBox(PlotOptions.SHOW_PLOT_TOOLTIPS, prefs, this);
    pSet.add(showGrid, 0, 0, 1, 1, 0d, 0d);
    pSet.add(showLegend, 0, 1, 1, 1, 0d, 0d);
    pSet.add(showToolTips, 0, 2, 1, 1, 0d, 0d);
    pPanel.setBorder(BorderFactory.createTitledBorder(' ' + bundle.getString("PLOT_SETTINGS") + ' '));

    return pPanel;
  }

  /**
   * 
   * @return
   */
  private Component createQualityPanel() {
    JPanel dPanel = new JPanel();
    LayoutHelper dSet = new LayoutHelper(dPanel);
    Class<QualityMeasure>[] distFunctions = SBMLsimulator
        .getAvailableQualityMeasures();
    QualityMeasurement qualityMeasurement = simulationManager.getQualityMeasurement();
    qualityMeasureFunctions = GUITools.createJComboBox(
      distFunctions,
      new ActionCommandRenderer(),
      distFunctions.length > 1,
      EstimationOptions.QUALITY_MEASURE.toString(),
      EstimationOptions.QUALITY_MEASURE.getDescription(),
      searchSelectedItem(distFunctions, qualityMeasurement.getDistance()
        .getClass().getName()), this);
    qualityMeasureField = new JFormattedTextField();
    qualityMeasureField.setEnabled(false);
    dSet.add(qualityMeasureFunctions);
    dSet.add(new JPanel());
    dSet.add(qualityMeasureField);
    dPanel.setBorder(BorderFactory.createTitledBorder(
      ' ' + EstimationOptions.QUALITY_MEASURES.getName() + ' '));
    return dPanel;
  }

  /**
   * @param name
   * @return
   */
  private JComboBox createSolversCombobox(String name) {
    Class<AbstractDESSolver> solFun[] = SBMLsimulator.getAvailableSolvers();
    if (solverComboBox == null) {
      solverComboBox = GUITools.createJComboBox(solFun,
        new ActionCommandRenderer(), solFun.length > 1,
        SimulationOptions.ODE_SOLVER.toString(),
        SimulationOptions.ODE_SOLVER.getDescription(),
        searchSelectedItem(solFun, name), this);
    }
    solverComboBox.addItemListener(this);
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
  public double getNumIntegrationSteps() {
    return stepsModel.getNumber().doubleValue();
  }

  /**
   * @return
   */
  public QualityMeasure getQualityMeasure() {
    return simulationManager.getQualityMeasurement().getDistance();
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

  /* (non-Javadoc)
   * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
   */
  @Override
  public void itemStateChanged(ItemEvent e) {
    logger.fine(e.getItem().toString() + ' ' + e.getSource());
    if ((e.getSource() instanceof JComboBox)
        && (e.getStateChange() == ItemEvent.SELECTED)) {
      JComboBox comBox = (JComboBox) e.getSource();
      try {
        logger.fine(e.getItem().toString() + "\t" + comBox.getName());
        if (comBox.getName().equals(
          EstimationOptions.QUALITY_MEASURE.toString())) {
          QualityMeasure dist = SBMLsimulator.getAvailableQualityMeasures()[comBox
                                                                            .getSelectedIndex()].getConstructor().newInstance();
          firePropertyChange(EstimationOptions.QUALITY_MEASURE.toString(),
            simulationManager.getQualityMeasurement().getDistance(), dist);

        } else if (comBox.getName().equals(
          SimulationOptions.ODE_SOLVER.toString())) {
          firePropertyChange(SimulationOptions.ODE_SOLVER.toString(),
            simulationManager.getSimulationConfiguration().getSolver(),
            SBMLsimulator.getAvailableSolvers()[comBox.getSelectedIndex()]
                .getConstructor().newInstance());
        }
      } catch (Exception exc) {
        GUITools.showErrorMessage(this, exc);
      }
    } else if (e.getSource() instanceof JCheckBox) {
      JCheckBox box = (JCheckBox) e.getSource();
      if (box.getName() != null) {
        String name = box.getName();
        if (KeyProvider.Tools.providesOption(PlotOptions.class, name)) {
          SBPreferences prefs = SBPreferences.getPreferencesFor(PlotOptions.class);
          logger.fine(name + "=" + box.isSelected());
          prefs.addAllPreferenceChangeListeners(listOfPreferenceChangeListeners);
          prefs.put(name, box.isSelected());
          try {
            prefs.flush();
          } catch (BackingStoreException exc) {
            logger.fine(getMessage(exc));
          }
        }
      }
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

  /* (non-Javadoc)
   * @see java.util.prefs.PreferenceChangeListener#preferenceChange(java.util.prefs.PreferenceChangeEvent)
   */
  @Override
  public void preferenceChange(PreferenceChangeEvent evt) {
    String key = evt.getKey();

    /*
     * Simulation options
     */

    if (key.equals(SimulationOptions.SIM_START_TIME.toString())) {
      t1.setValue(Double.valueOf(evt.getNewValue()));
    } else if (key.equals(SimulationOptions.SIM_END_TIME.toString())) {
      t2.setValue(Double.valueOf(evt.getNewValue()));
    } else if (key.equals(SimulationOptions.SIM_STEP_SIZE.toString())) {
      setStepSize(Double.parseDouble(evt.getNewValue()));
    } else if (key.equals(SimulationOptions.ODE_SOLVER.toString())) {
      solverComboBox.setSelectedIndex(searchSelectedItem(
        SBMLsimulator.getAvailableSolvers(), evt.getNewValue()));
    } else if (key.equals(EstimationOptions.QUALITY_MEASURE.toString())) {
      qualityMeasureFunctions.setSelectedIndex(searchSelectedItem(
        SBMLsimulator.getAvailableQualityMeasures(), evt.getNewValue()));

      /*
       * Plot options
       */

    } else if (key.equals(PlotOptions.SHOW_PLOT_GRID.toString())) {
      showGrid.setSelected(Boolean.parseBoolean(evt.getNewValue()));
    } else if (key.equals(PlotOptions.SHOW_PLOT_LEGEND.toString())) {
      showLegend.setSelected(Boolean.parseBoolean(evt.getNewValue()));
    } else if (key.equals(PlotOptions.SHOW_PLOT_TOOLTIPS.toString())) {
      showToolTips.setSelected(Boolean.parseBoolean(evt.getNewValue()));

      /*
       * Forward all other options to registered preference listeners.
       */

    } else {
      logger.fine(key + "=" + evt.getNewValue());
      for (PreferenceChangeListener listener : listOfPreferenceChangeListeners) {
        listener.preferenceChange(evt);
      }
    }

  }

  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent pEvt) {
    logger.fine(pEvt.toString());
    if (pEvt.getPropertyName().equals("done")
        && (simulationManager.getDistanceValues() != null)
        && (simulationManager.getDistanceValues().length > 0)
        && !((pEvt.getNewValue() == null) && (pEvt.getOldValue() == null))
        && ((((pEvt.getNewValue() == null) && (pEvt.getOldValue() != null)) || ((pEvt
            .getNewValue() != null) && (pEvt.getOldValue() == null))) || (!pEvt
                .getNewValue().equals(pEvt.getOldValue())))) {
      qualityMeasureField.setValue(simulationManager.getMeanDistanceValue());
      qualityMeasureField.setText(StringUtil.toString(
        simulationManager.getMeanDistanceValue()));
      if (!qualityMeasureField.isEnabled()) {
        qualityMeasureField.setEditable(false);
        qualityMeasureField.setEnabled(true);
        qualityMeasureField.setAlignmentX(RIGHT_ALIGNMENT);
      }
    } else if(pEvt.getPropertyName().equals("quality")) {
      double newValue = (Double)pEvt.getNewValue();
      qualityMeasureField.setValue(newValue);
      qualityMeasureField.setText(StringUtil.toString(
        newValue));
      if (!qualityMeasureField.isEnabled()) {
        qualityMeasureField.setEditable(false);
        qualityMeasureField.setEnabled(true);
        qualityMeasureField.setAlignmentX(RIGHT_ALIGNMENT);
      }
    }
  }

  /**
   * 
   * @param items
   * @param name
   * @return
   */
  private <T> int searchSelectedItem(Class<T> items[], String name) {
    if (name.contains(" ")) {
      name = name.substring(name.indexOf(' ') + 1);
    }
    for (int i = 0; i < items.length; i++) {
      Class<T> c = items[i];
      if (c.getName().equals(name) || c.getSimpleName().equals(name)) {
        return i;
      }
    }
    return -1;
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
   * @param stepSize
   */
  public void setStepSize(double stepSize) {
    stepsModel.setValue(Integer.valueOf(numSteps(
      ((Number) t1.getValue()).doubleValue(),
      ((Number) t2.getValue()).doubleValue(), stepSize)));
    this.firePropertyChange(SimulationOptions.SIM_STEP_SIZE.toString(),
      simulationManager.getSimulationConfiguration().getStepSize(), stepSize);
  }

  /* (non-Javadoc)
   * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
   */
  @Override
  public void stateChanged(ChangeEvent e) {
    if (e.getSource() instanceof JSpinner) {
      String name = ((JSpinner) e.getSource()).getName();
      logger.fine(e.toString());
      if (name != null) {
        boolean diffStepSize = false;
        SimulationConfiguration simulationConfiguration = simulationManager.getSimulationConfiguration();
        if (name.equals(SimulationOptions.SIM_START_TIME.toString())) {
          diffStepSize = true;
          firePropertyChange(name, simulationConfiguration.getStart(),
            getSimulationStartTime());
        } else if (name.equals(SimulationOptions.SIM_END_TIME.toString())) {
          diffStepSize = true;
          firePropertyChange(name, simulationConfiguration.getEnd(),
            getSimulationEndTime());
        }
        if (diffStepSize || name.equals("stepCount")) {
          firePropertyChange(SimulationOptions.SIM_STEP_SIZE.toString(),
            simulationConfiguration.getStepSize(), getStepSize());
        }
      }
    }
  }

}
