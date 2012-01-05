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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.Quantity;
import org.sbml.optimization.problem.EstimationProblem;
import org.sbml.simulator.QualityMeasurement;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.SimulationConfiguration;
import org.sbml.simulator.SimulationManager;
import org.sbml.simulator.SimulationOptions;
import org.sbml.simulator.gui.table.DecimalCellRenderer;
import org.sbml.simulator.math.QualityMeasure;
import org.simulator.math.odes.DESSolver;
import org.simulator.math.odes.MultiTable;

import de.zbit.gui.GUITools;
import de.zbit.io.CSVOptions;
import de.zbit.io.CSVWriter;
import de.zbit.io.SBFileFilter;
import de.zbit.sbml.gui.SBMLModelSplitPane;
import de.zbit.util.ResourceManager;
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
   * Support for localization.
   */
  private static final transient ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");
  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(SimulationPanel.class.getName());
  
  /**
   * Table for the simulation data.
   */
  private JTable simTable;
  
  /**
   * Multiple tables
   */
  private MultipleTableView<MultiTable> dataTableView;
  
  /**
   * 
   */
  private JToolBar simulationToolPanel;
  
  /**
	 * 
	 */
  private List<PropertyChangeListener> listeners;
  
  /**
   * Array of identifiers of those {@link Quantity}s that are the target of a
   * value optimization.
   */
  private String[] selectedQuantityIds;
  
  /**
   * Switch to decide whether or not to draw the foot panel.
   */
  private boolean showSimulationToolPanel;
  
  /**
   * Indices to more efficiently memorize the location of interesting elements
   * in the call-back function.
   */
  private int simulationDataIndex, solutionIndex, runBestIndex;
  
  /**
   * The simulation manager for the current simulation.
   */
  private SimulationManager simulationManager;
  
  /**
	 * @return the simulationManager
	 */
	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	/**
   * The main tabbed pane showing plot, simulation and experimental data.
   */
  private JTabbedPane tabbedPane;
  
  /**
	 * 
	 */
  private SimulationVisualizationPanel visualizationPanel;
  
  /**
   * @param model
   */
  public SimulationPanel(Model model) {
    super();
    showSimulationToolPanel = true;
    this.listeners = new ArrayList<PropertyChangeListener>();
    if (SBMLsimulator.getAvailableSolvers().length == 0) {
      JOptionPane.showMessageDialog(this, StringUtil.toHTML(
        bundle.getString("NO_ODE_SOLVERS_AVAILABLE_MESSAGE")),
        bundle.getString("NO_ODE_SOLVERS_AVAILABLE"), JOptionPane.WARNING_MESSAGE);
    } else {
      try {
        if (model == null) {
          throw new NullPointerException(bundle.getString("NULL_MODEL"));
        }
        SBPreferences prefs = SBPreferences.getPreferencesFor(SimulationOptions.class);
        String clazz = prefs.get(SimulationOptions.ODE_SOLVER);
        DESSolver solver = (DESSolver) Class.forName(clazz.substring(clazz.indexOf(' ') + 1)).newInstance();
        double timeStart = prefs.getDouble(SimulationOptions.SIM_START_TIME);
        double timeEnd = prefs.getDouble(SimulationOptions.SIM_END_TIME);
        double stepSize = prefs.getDouble(SimulationOptions.SIM_STEP_SIZE);
        boolean includeReactions = true;
        clazz = prefs.get(SimulationOptions.QUALITY_MEASURE);
        QualityMeasure quality = (QualityMeasure) Class.forName(clazz.substring(clazz.indexOf(' ') + 1)).newInstance();
        QualityMeasurement measurement = new QualityMeasurement(quality);
        simulationManager = new SimulationManager(measurement,
          new SimulationConfiguration(model, solver, timeStart, timeEnd, stepSize, includeReactions));
        simulationManager.addPropertyChangeListener(this);
        this.addPropertyChangedListener(measurement);
        visualizationPanel = new SimulationVisualizationPanel();
        init();
      } catch (Throwable exc) {
        GUITools.showErrorMessage(this, exc);
      }
    }
  }
  
  /* (non-Javadoc)
   * @see org.sbml.simulator.math.odes.DESSolver#addPropertyChangedListener(java.beans.PropertyChangeListener)
   */
  public void addPropertyChangedListener(PropertyChangeListener listener) {
    if (!listeners.contains(listener)) {
      this.listeners.add(listener);
    }
  }
  
  /**
	 * 
	 */
  public void closeAllExperimentalData() {
    for (int i = dataTableView.getTableCount() - 1; i >= 0; i--) {
    	closeExpermentalData(i);
    }
  }
  
  /**
   * 
   * @param index
   */
  public void closeExpermentalData(int index) {
  	dataTableView.removeTable(index);
    if (tabbedPane.getSelectedIndex() == 2) {
      tabbedPane.setSelectedIndex(0);
    }
		if (dataTableView.getTableCount() == 0) {
			tabbedPane.setEnabledAt(2, false);
			visualizationPanel.unsetExperimentData();
		}
  }
  
  /**
   * @return
   */
  private SimulationToolPanel createFootPanel() {
    simulationToolPanel = new JToolBar(bundle.getString("INTEGRATION_TOOLBOX"));
    SimulationToolPanel foot = new SimulationToolPanel(simulationManager);
    simulationToolPanel.add(foot);
    return foot;
  }
  
  /* (non-Javadoc)
   * @see eva2.server.stat.InterfaceStatisticsListener#finalMultiRunResults(java.lang.String[], java.util.List)
   */
  public void finalMultiRunResults(String[] header,
    List<Object[]> multiRunFinalObjectData) {
    // TODO Auto-generated method stub
    logger.fine("finalMultiRunResults");
  }
  
  /* (non-Javadoc)
   * @see org.sbml.simulator.math.odes.DESSolver#firePropertyChanged(double, double)
   */
  public void firePropertyChanged(PropertyChangeEvent evt) {
    if (!this.listeners.isEmpty()) {
      for (PropertyChangeListener listener : this.listeners) {
        listener.propertyChange(evt);
      }
    }
  }
  
  /**
   * @return
   */
  public QualityMeasure getDistance() {
    return ((SimulationToolPanel) simulationToolPanel.getComponent(0))
        .getQualityMeasure();
  }
  
  /**
   * @return
   */
  public MultiTable getExperimentalData(int index) {
    return dataTableView.getTable(index);
  }
  
  /**
   * 
   * @return
   */
  public List<MultiTable> getExperimentalData() {
  	return dataTableView.getTables();
  }
  
  /**
   * @return
   */
  public Model getModel() {
    return this.simulationManager.getSimlationConfiguration().getModel();
  }
  
  /**
   * @return
   */
  public MultiTable getSimulationResultsTable() {
    return (MultiTable) simTable.getModel();
  }
  
  /**
   * @return
   */
  public SimulationToolPanel getSimulationToolPanel() {
    if (simulationToolPanel == null) {
      return createFootPanel();
    }
    return (SimulationToolPanel) simulationToolPanel.getComponent(0);
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
      visualizationPanel.setModel(simulationManager.getSimlationConfiguration().getModel());
      SimulationToolPanel foot = getSimulationToolPanel();
      foot.addItemListener(visualizationPanel);
      if (showSimulationToolPanel) {
        add(simulationToolPanel, BorderLayout.SOUTH);
      }
      visualizationPanel.getPlot().setGridVisible(foot.getShowGrid());
      visualizationPanel.getPlot().setShowLegend(foot.getShowLegend());
      visualizationPanel.getPlot().setShowGraphToolTips(
        foot.getShowGraphToolTips());
//      visualizationPanel.setPlotToLogScale(foot.getJCheckBoxLegend());
      
      if (tabbedPane == null) {
        JPanel simPanel = new JPanel(new BorderLayout());
        simTable = new JTable();
        simTable.setDefaultRenderer(Double.class, new DecimalCellRenderer(10, 4, SwingConstants.RIGHT));
        simPanel.add(new JScrollPane(simTable), BorderLayout.CENTER);
        simTable.getModel().addTableModelListener(visualizationPanel);
        
        dataTableView = new MultipleTableView<MultiTable>();
        dataTableView.addTableModelListener(visualizationPanel);
        
        tabbedPane = new JTabbedPane();
        tabbedPane.add(bundle.getString("TAB_SIMULATION"), visualizationPanel);
        tabbedPane.add(bundle.getString("TAB_IN_SILICO_DATA"), simPanel);
        tabbedPane.add(bundle.getString("TAB_EXPERIMENTAL_DATA"), dataTableView);
				tabbedPane.add(bundle.getString("TAB_MODEL_VIEW"),
					new SBMLModelSplitPane(simulationManager.getSimlationConfiguration()
							.getModel().getSBMLDocument(), true));
        tabbedPane.setEnabledAt(0, true);
        tabbedPane.setEnabledAt(1, false);
        tabbedPane.setEnabledAt(2, false);
        tabbedPane.setEnabledAt(3, true);
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
    return (dataTableView != null) && (dataTableView.getTableCount() > 0);
  }
  
  /**
   * @return the showSettingsPanel
   */
  public boolean isShowSettingsPanel() {
    return showSimulationToolPanel;
  }
  
  /**
   * 
   */
  public void loadPreferences() {
    if (visualizationPanel != null) {
      visualizationPanel.loadPreferences();
    }
    removeAll();
    this.simulationToolPanel = null;
    init();
    validate();
  }
  
  /* (non-Javadoc)
   * @see eva2.server.stat.InterfaceStatisticsListener#notifyGenerationPerformed(java.lang.String[], java.lang.Object[], java.lang.Double[])
   */
  public void notifyGenerationPerformed(String[] header, Object[] statObjects,
    Double[] statDoubles) {
    SimulationToolPanel tools = (SimulationToolPanel) simulationToolPanel.getComponent(0);
    double currentDistance = tools.getCurrentQuality();
    if (Double.isNaN(currentDistance)
        || (currentDistance > statDoubles[runBestIndex].doubleValue())) {
      setSimulationData((MultiTable) statObjects[simulationDataIndex]);
			firePropertyChange("quality", getSimulationToolPanel().getCurrentQuality(),
				statDoubles[runBestIndex].doubleValue());
      String[] solutionString = statObjects[solutionIndex].toString().replace("{","").replace("}","").split(", ");
      for (int i = 0; i < selectedQuantityIds.length; i++) {
        visualizationPanel.updateQuantity(selectedQuantityIds[i], Double.parseDouble(solutionString[i].replace(',', '.')));
      }
    }
  }
  
  /* (non-Javadoc)
   * @see eva2.server.stat.InterfaceStatisticsListener#notifyMultiRunFinished(java.lang.String[], java.util.List)
   */
  public boolean notifyMultiRunFinished(String[] header,
    List<Object[]> multiRunFinalObjectData) {
    for(Object[] obj: multiRunFinalObjectData) {
      String[] solutionString = obj[solutionIndex].toString().replace("{","").replace("}","").split(", ");
      logger.fine("Fitness: " + ((Double) obj[1]));
      for (int i = 0; i < selectedQuantityIds.length; i++) {
        double currentQuantity=Double.parseDouble(solutionString[i].replace(',', '.'));
        visualizationPanel.updateQuantity(selectedQuantityIds[i], currentQuantity);
        System.out.println(selectedQuantityIds[i] + ": " +currentQuantity);
      
      }
    }
    return true;
  }
  
  /**
   * @param selectedQuantityIds
   */
  public void notifyQuantitiesSelected(String[] selectedQuantityIds) {
    this.selectedQuantityIds = selectedQuantityIds;
  }
  
  /* (non-Javadoc)
   * @see eva2.server.stat.InterfaceStatisticsListener#notifyRunStarted(int, int, java.lang.String[], java.lang.String[])
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
  
  /* (non-Javadoc)
   * @see eva2.server.stat.InterfaceStatisticsListener#notifyRunStopped(int, boolean)
   */
  public void notifyRunStopped(int runsPerformed, boolean completedLastRun) {
    logger.fine("notifyRunStopped");
  }
  
  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {
    if ("progress".equals(evt.getPropertyName())) {
      this.firePropertyChanged(evt);
    } else if ("done".equals(evt.getPropertyName())) {
      MultiTable data = (MultiTable) evt.getNewValue();
      if (data != null) {
        setSimulationData(data);
      }
      try {
				if (simulationManager.getQualityMeasurement().getMeasurements().size() > 0) {
					firePropertyChange("quality", getSimulationToolPanel()
							.getCurrentQuality(), simulationManager.getMeanDistanceValue());
				}
      } catch (Exception e) {
      }
      firePropertyChanged(evt);
    }
  }
  
  /**
   * 
   */
  public void refreshStepSize() {
    getSolver().setStepSize(simulationManager.getSimlationConfiguration().getStepSize());
  }
  
  /* (non-Javadoc)
   * @see org.sbml.simulator.math.odes.DESSolver#removePropertyChangedListener(java.beans.PropertyChangeListener)
   */
  public void removePropertyChangedListener(PropertyChangeListener listener) {
    if (listeners.contains(listener))
      this.listeners.remove(listener);
  }
  
  /**
   * 
   */
  public void saveSimulationResults() {
    try {
      MultiTable simTabModel = getSimulationResultsTable();
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
        JOptionPane.showMessageDialog(this,
					StringUtil.toHTML(bundle.getString("NO_SIMULATION_PERFORMED"), 40));
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
    ((SimulationToolPanel) simulationToolPanel.getComponent(0)).setAllEnabled(enabled);
  }
  
  /**
   * @param data
   * @throws Exception
   */
  public void addExperimentalData(String title, MultiTable data) throws Exception {
    tabbedPane.setEnabledAt(2, true);
    this.firePropertyChange("measurements", null, data);
    //TODO preliminary version: property does not change for quality measurement with the call firePropertyChange()
    simulationManager.getQualityMeasurement().propertyChange(new PropertyChangeEvent(this, "measurements", null, data));
    visualizationPanel.setExperimentData(data);
  }
  
  /**
   * @param ids
   */
  public void setSelectedQuantities(String... ids) {
    visualizationPanel.setSelectedQuantities(ids);
  }
  
  /**
   * @param visible
   *        the showSettingsPanel to set
   */
  public void setShowSimulationToolPanel(boolean visible) {
    if (this.showSimulationToolPanel != visible) {
      this.showSimulationToolPanel = visible;
      if (!visible) {
        remove(simulationToolPanel);
      } else {
        add(simulationToolPanel, BorderLayout.SOUTH);
      }
      simulationToolPanel.setVisible(visible);
      validate();
    }
  }
  
  /**
   * @param data
   */
  private void setSimulationData(MultiTable data) {
    data.addTableModelListener(visualizationPanel);
    visualizationPanel.setSimulationData(data);
    simTable.setModel(data);
    simTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    TableColumnModel tabColMod = simTable.getColumnModel();
    TableColumn col;
    for (int i = 0; i < simTable.getColumnCount(); i++) {
      col = tabColMod.getColumn(i);
      col.setPreferredWidth(100);
      col.setMinWidth(100);
    }
    tabbedPane.setEnabledAt(1, true);
  }

  /**
   * Conducts the simulation.
   * 
   * @throws Exception
   */
  public void simulate() throws Exception {
    simulationManager.simulate();
  }

  /**
   * 
   * @return
   */
	public int getExperimentalDataCount() {
		return dataTableView.getTableCount();
	}
  
}
