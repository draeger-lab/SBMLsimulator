/*
 * $Id$ $URL:
 * https://rarepos
 * .cs.uni-tuebingen.de/svn-path/SBMLsimulator/trunk/src/org/sbml/
 * simulator/gui/SimulationPanel.java $
 * --------------------------------------------------------------------- This
 * file is part of SBMLsimulator, a Java-based simulator for models of
 * biochemical processes encoded in the modeling language SBML.
 * 
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation. A copy of the license agreement is provided in the file
 * named "LICENSE.txt" included with this software distribution and also
 * available online as <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.gui;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;

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
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.optimization.PlotOptions;
import org.sbml.optimization.problem.EstimationProblem;
import org.sbml.simulator.QualityMeasurement;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.SimulationConfiguration;
import org.sbml.simulator.SimulationManager;
import org.sbml.simulator.math.QualityMeasure;
import org.sbml.simulator.math.odes.DESSolver;
import org.sbml.simulator.math.odes.IntegrationException;
import org.sbml.simulator.math.odes.MultiBlockTable;
import org.sbml.simulator.math.odes.SimulationOptions;

import de.zbit.gui.GUITools;
import de.zbit.io.CSVOptions;
import de.zbit.io.CSVWriter;
import de.zbit.io.SBFileFilter;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.SBPreferences;
import eva2.server.go.problems.AbstractOptimizationProblem;
import eva2.server.stat.GraphSelectionEnum;
import eva2.server.stat.InterfaceStatisticsListener;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-04-06
 * @version $Rev$
 * @since 1.0
 */
public class SimulationPanel extends JPanel implements
    InterfaceStatisticsListener, PropertyChangeListener {
  
  /**
   * Generated serial version identifier
   */
  private static final long serialVersionUID = -7278034514446047207L;
  
  /**
   * Table for experimental data, the legend, and the simulation data.
   */
  private JTable expTable, simTable;
  /**
     * 
     */
  private JToolBar footPanel;
  /**
   * Array of identifiers of those {@link Quantity}s that are the target of a
   * value optimization.
   */
  private String[] selectedQuantityIds;
  /**
   * Switch to decide whether or not to draw the foot panel.
   */
  private boolean showSettingsPanel;
  /**
   * Indices to more efficiently memorize the location of interesting elements
   * in the call-back function.
   */
  private int simulationDataIndex, solutionIndex, runBestIndex;
  /**
   * The main tabbed pane showing plot, simulation and experimental data.
   */
  private JTabbedPane tabbedPane;
  
  /**
	 * 
	 */
  private SimulationVisualizationPanel visualizationPanel;
  
  /**
	 * 
	 */
  private ArrayList<PropertyChangeListener> listeners;
  
  /**
	 * The simulation manager for the current simulation.
	 */
  private SimulationManager simulationManager;
  
  /**
   * @param model
   */
  public SimulationPanel(Model model) {
    super();
    showSettingsPanel = true;
    this.listeners = new ArrayList<PropertyChangeListener>();
    if (SBMLsimulator.getAvailableSolvers().length == 0) {
      JOptionPane
          .showMessageDialog(
            this,
            StringUtil
                .toHTML("Could not find any solvers for differential equation systems. A simulation is therefore not possible."),
            "No ODE solver available", JOptionPane.WARNING_MESSAGE);
    } else {
      try {
        if (model == null) {
          throw new NullPointerException("Model is null.");
        }        
        simulationManager = new SimulationManager(new QualityMeasurement(),
          new SimulationConfiguration(model));
        simulationManager.addPropertyChangeListener(this);
        visualizationPanel = new SimulationVisualizationPanel();
        loadPreferences();
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
    expTable.setDefaultRenderer(Double.class, new DecimalCellRenderer(10, 4,
      SwingConstants.RIGHT));
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
    throws IllegalArgumentException, SecurityException, InstantiationException,
    IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    footPanel = new JToolBar("Integration toolbox");
    SimulationToolPanel foot = new SimulationToolPanel(simulationManager);
    footPanel.add(foot);
    return foot;
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see eva2.server.stat.InterfaceStatisticsListener#finalMultiRunResults(java
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
  public QualityMeasure getDistance() {
    return ((SimulationToolPanel) footPanel.getComponent(0))
        .getQualityMeasure();
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
    return this.simulationManager.getSimlationConfiguration().getModel();
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
    throws IllegalArgumentException, SecurityException, InstantiationException,
    IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    if (footPanel == null) {
      return createFootPanel();
    }
    return (SimulationToolPanel) footPanel.getComponent(0);
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
    return simulationManager.getSimlationConfiguration().getSolver();
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
      visualizationPanel.setModel(this.simulationManager
          .getSimlationConfiguration().getModel());
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
        simTable.setDefaultRenderer(Double.class, new DecimalCellRenderer(10,
          4, SwingConstants.RIGHT));
        simPanel.add(new JScrollPane(simTable,
          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        simTable.getModel().addTableModelListener(visualizationPanel);
        
        JPanel expPanel = new JPanel(new BorderLayout());
        expTable = new JTable();
        expTable.setDefaultRenderer(Double.class, new DecimalCellRenderer(10,
          4, SwingConstants.RIGHT));
        expPanel.add(new JScrollPane(expTable,
          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
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
  
  /**
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws SecurityException
   * @throws IllegalArgumentException
   * @throws ModelOverdeterminedException
   * @throws IntegrationException
   * @throws SBMLException
   */
  public void loadPreferences() throws IllegalArgumentException,
    SecurityException, InstantiationException, IllegalAccessException,
    InvocationTargetException, NoSuchMethodException, SBMLException,
    IntegrationException, ModelOverdeterminedException {
    
    if (visualizationPanel != null) {
      visualizationPanel.loadPreferences();
    }
    SimulationToolPanel foot = getOrCreateFootPanel();
    foot.loadPreferences();
    removeAll();
    init();
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see eva2.server.stat.InterfaceStatisticsListener#notifyGenerationPerformed
   * (java.lang.String[], java.lang.Object[], java.lang.Double[])
   */
  public void notifyGenerationPerformed(String[] header, Object[] statObjects,
    Double[] statDoubles) {
    SimulationToolPanel tools = (SimulationToolPanel) footPanel.getComponent(0);
    double currentDistance = tools.getCurrentQuality();
    if (Double.isNaN(currentDistance)
        || (currentDistance > statDoubles[runBestIndex].doubleValue())) {
      setSimulationData((MultiBlockTable) statObjects[simulationDataIndex]);
      tools.setCurrentQualityMeasure(statDoubles[runBestIndex].doubleValue());
      double solution[] = (double[]) statObjects[solutionIndex];
      for (int i = 0; i < selectedQuantityIds.length; i++) {
        visualizationPanel.updateQuantity(selectedQuantityIds[i], solution[i]);
      }
    }
  }
  

  /*
   * (non-Javadoc)
   * @see eva2.server.stat.InterfaceStatisticsListener#notifyMultiRunFinished(java.lang.String[], java.util.List)
   */
  public boolean notifyMultiRunFinished(String[] header,
    List<Object[]> multiRunFinalObjectData) {
    // TODO Auto-generated method stub
    return false;
  }
  
  /**
   * @param selectedQuantityIds
   */
  public void notifyQuantitiesSelected(String[] selectedQuantityIds) {
    this.selectedQuantityIds = selectedQuantityIds;
  }
  
  /*
   * (non-Javadoc)
   * 
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
   * 
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
      SBPreferences prefs = SBPreferences.getPreferencesFor(PlotOptions.class);
      String saveDir = prefs.get(PlotOptions.PLOT_SAVE_DIR).toString();
      float compression = prefs.getFloat(PlotOptions.JPEG_COMPRESSION_FACTOR);
      prefs.put(PlotOptions.PLOT_SAVE_DIR, visualizationPanel.getPlot()
          .savePlotImage(saveDir, Float.valueOf(compression)));
    } catch (Exception exc) {
      GUITools.showErrorMessage(this, exc);
    }
  }
  
  /**
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws SecurityException
   * @throws IllegalArgumentException
   * @throws BackingStoreException
   */
  public void savePreferences() throws IllegalArgumentException,
    SecurityException, InstantiationException, IllegalAccessException,
    InvocationTargetException, NoSuchMethodException, BackingStoreException {
    SimulationToolPanel foot = getOrCreateFootPanel();
    SBPreferences prefs = SBPreferences
        .getPreferencesFor(SimulationOptions.class);
    prefs.put(SimulationOptions.SIM_START_TIME, foot.getSimulationStartTime());
    prefs.put(SimulationOptions.SIM_END_TIME, foot.getSimulationEndTime());
    prefs.flush();
  }
  
  /**
   * 
   */
  public void saveSimulationResults() {
    try {
      TableModel simTabModel = getSimulationResultsTable();
      SBPreferences prefs = SBPreferences.getPreferencesFor(CSVOptions.class);
      if (simTabModel.getRowCount() > 0) {
        File out = GUITools.saveFileDialog(this, prefs.get(
          CSVOptions.CSV_FILES_SAVE_DIR).toString(), false, false,
          JFileChooser.FILES_ONLY, SBFileFilter.createCSVFileFilter());
        if (out != null) {
          (new CSVWriter()).write(simTabModel, prefs.get(
            CSVOptions.CSV_FILES_SEPARATOR_CHAR).toString().charAt(0), out);
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
    ((SimulationToolPanel) footPanel.getComponent(0)).setAllEnabled(enabled);
  }
  
  /**
   * @param data
   * @throws Exception
   */
  public void setExperimentalData(MultiBlockTable data) throws Exception {
    expTable.setModel(data);
    tabbedPane.setEnabledAt(2, true);
    this.firePropertyChange("measurements", null, data);
    visualizationPanel.setExperimentData(data);
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
    SimulationToolPanel foot = (SimulationToolPanel) footPanel.getComponent(0);
    foot.processSimulationValues();
    simulationManager.simulate();
    
    if (simulationManager.getSimlationConfiguration().getStepSize() != foot
        .getStepSize()) {
      foot.setStepSize(simulationManager.getSimlationConfiguration()
          .getStepSize());
    }
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent
   * )
   */
  public void propertyChange(PropertyChangeEvent evt) {
    if ("progress" == evt.getPropertyName()) {
      this.firePropertyChanged(evt);
    }
    if ("done" == evt.getPropertyName()) {
      MultiBlockTable data = (MultiBlockTable) evt.getNewValue();
      if (data != null) {
        setSimulationData(data);
      }
      firePropertyChanged(evt);
    }
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.sbml.simulator.math.odes.DESSolver#addPropertyChangedListener(java
   * .beans.PropertyChangeListener)
   */
  public void addPropertyChangedListener(PropertyChangeListener listener) {
    if (!listeners.contains(listener))
      this.listeners.add(listener);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.sbml.simulator.math.odes.DESSolver#removePropertyChangedListener(
   * java.beans.PropertyChangeListener)
   */
  public void removePropertyChangedListener(PropertyChangeListener listener) {
    if (listeners.contains(listener))
      this.listeners.remove(listener);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.sbml.simulator.math.odes.DESSolver#firePropertyChanged(double,
   * double)
   */
  public void firePropertyChanged(PropertyChangeEvent evt) {
    if (!this.listeners.isEmpty()) {
      for (PropertyChangeListener listener : this.listeners) {
        listener.propertyChange(evt);
      }
    }
  }
  
}
