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

import static de.zbit.util.Utils.getMessage;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import javax.swing.JFileChooser;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Quantity;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.optimization.problem.EstimationOptions;
import org.sbml.optimization.problem.EstimationProblem;
import org.sbml.simulator.QualityMeasurement;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.SimulationConfiguration;
import org.sbml.simulator.SimulationManager;
import org.sbml.simulator.SimulationOptions;
import org.sbml.simulator.gui.graph.DynamicView;
import org.sbml.simulator.gui.plot.Plot;
import org.simulator.math.PearsonCorrelation;
import org.simulator.math.QualityMeasure;
import org.simulator.math.odes.DESSolver;
import org.simulator.math.odes.MultiTable;

import de.zbit.graph.io.Graph2Dwriter;
import de.zbit.gui.BaseFrameTab;
import de.zbit.gui.GUIOptions;
import de.zbit.gui.GUITools;
import de.zbit.gui.table.MultipleTableView;
import de.zbit.gui.table.renderer.DecimalCellRenderer;
import de.zbit.io.OpenedFile;
import de.zbit.io.csv.CSVOptions;
import de.zbit.io.csv.CSVWriter;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.sbml.gui.SBMLModelSplitPane;
import de.zbit.sbml.io.SBMLfileChangeListener;
import de.zbit.util.ResourceManager;
import de.zbit.util.StringUtil;
import de.zbit.util.prefs.SBPreferences;
import eva2.gui.Main;
import eva2.optimization.statistics.GraphSelectionEnum;
import eva2.optimization.statistics.InterfaceStatisticsListener;
import eva2.problems.AbstractOptimizationProblem;
import y.view.Graph2D;

/**
 * This is the main panel in the graphical user interface, in which the entire
 * content of the simulation is displayed to the user. It includes the plot
 * area ({@link Plot}), the legend overview ({@link LegendPanel}), the
 * simulation tool panel ({@link SimulationToolPanel}).
 *
 * @author Andreas Dr&auml;ger
 * @author Clemens Wrzodek
 * @date 2010-04-06
 * @since 1.0
 */
public class SimulationPanel extends JPanel implements
BaseFrameTab, InterfaceStatisticsListener, PropertyChangeListener, PreferenceChangeListener {

  /**
   * Support for localization.
   */
  private static final transient ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(SimulationPanel.class.getName());
  /**
   * Generated serial version identifier
   */
  private static final long serialVersionUID = -7278034514446047207L;

  /**
   *
   */
  private static final int TAB_FBA_INDEX = 5;

  /**
   *
   */
  private static final int TAB_EXPERIMENT_INDEX = 2;

  /**
   *
   */
  private static final int TAB_IN_SILICO_DATA_INDEX = 1;

  /**
   *
   */
  private static final int TAB_MODEL_VIEW_INDEX = 3;

  /**
   *
   */
  private static final int TAB_GRAPH_VIEW_INDEX = 4;

  /**
   *
   */
  private static final int TAB_SIMULATION_INDEX = 0;

  /**
   * Multiple tables
   */
  private MultipleTableView<MultiTable> dataTableView;

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
   * Table for the simulation data.
   */
  private JTable simTable;

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
   *
   */
  private JToolBar simulationToolPanel;

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
  private DynamicView dynamicGraphView;

  private Main evaClient;

  /**
   * @param model
   */
  public SimulationPanel(Model model) {
    super();
    showSimulationToolPanel = true;
    listeners = new ArrayList<PropertyChangeListener>();
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
        double absTol = prefs.getDouble(SimulationOptions.ABS_TOL);
        double relTol = prefs.getDouble(SimulationOptions.REL_TOL);
        prefs = SBPreferences.getPreferencesFor(EstimationOptions.class);
        clazz = prefs.get(EstimationOptions.QUALITY_MEASURE);
        QualityMeasure quality;
        quality = (QualityMeasure) Class.forName(clazz.substring(clazz.indexOf(' ') + 1)).newInstance();
        QualityMeasurement measurement = new QualityMeasurement(quality);
        simulationManager = new SimulationManager(measurement,
          new SimulationConfiguration(model, solver, timeStart, timeEnd, stepSize, includeReactions, absTol, relTol));
        simulationManager.addPropertyChangeListener(this);
        addPropertyChangedListener(measurement);
        visualizationPanel = new SimulationVisualizationPanel();
        init();
      } catch (Throwable exc) {
        GUITools.showErrorMessage(this, exc);
      }
    }
  }

  /**
   *
   * @param title
   * @param data
   */
  public void addExperimentalData(String title, MultiTable data) {
    dataTableView.addTable(title, data);
    tabbedPane.setEnabledAt(TAB_EXPERIMENT_INDEX, true);
    // TODO: Don't fire property change event twice!
    this.firePropertyChange("measurements", null, data);
    //TODO preliminary version: property does not change for quality measurement with the call firePropertyChange()
    simulationManager.getQualityMeasurement().propertyChange(new PropertyChangeEvent(this, "measurements", null, data));
    visualizationPanel.addExperimentData(data);
  }

  /* (non-Javadoc)
   * @see org.sbml.simulator.math.odes.DESSolver#addPropertyChangedListener(java.beans.PropertyChangeListener)
   */
  public void addPropertyChangedListener(PropertyChangeListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
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
    visualizationPanel.removeExperimentData(index);
    if (tabbedPane.getSelectedIndex() == TAB_EXPERIMENT_INDEX) {
      tabbedPane.setSelectedIndex(TAB_SIMULATION_INDEX);
    }
    if (dataTableView.getTableCount() == TAB_SIMULATION_INDEX) {
      tabbedPane.setEnabledAt(TAB_EXPERIMENT_INDEX, false);
    }
  }

  /* (non-Javadoc)
   * @see eva2.server.stat.InterfaceStatisticsListener#finalMultiRunResults(java.lang.String[], java.util.List)
   */
  @Override
  public void finalMultiRunResults(String[] header,
    List<Object[]> multiRunFinalObjectData) {
    // TODO Auto-generated method stub
    logger.fine("finalMultiRunResults");
  }

  /* (non-Javadoc)
   * @see org.sbml.simulator.math.odes.DESSolver#firePropertyChanged(double, double)
   */
  public void firePropertyChanged(PropertyChangeEvent evt) {
    for (PropertyChangeListener listener : listeners) {
      listener.propertyChange(evt);
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
   *
   * @return
   */
  public List<MultiTable> getExperimentalData() {
    return dataTableView.getTables();
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
  public int getExperimentalDataCount() {
    return dataTableView.getTableCount();
  }

  /**
   * @return
   */
  public SimulationVisualizationPanel getVisualizationPanel() {
    return visualizationPanel;
  }

  /**
   * @return
   */
  public Model getModel() {
    return simulationManager.getSimulationConfiguration().getModel();
  }

  /**
   * @return the simulationManager
   */
  public SimulationManager getSimulationManager() {
    return simulationManager;
  }

  /**
   * @return
   */
  public MultiTable getSimulationResultsTable() {
    TableModel model = simTable.getModel();
    if (model instanceof MultiTable) {
      return (MultiTable) model;
    }
    return null;
  }

  /**
   * @return
   */
  public SimulationToolPanel getSimulationToolPanel() {
    if (simulationToolPanel == null) {
      simulationToolPanel = new JToolBar(bundle.getString("INTEGRATION_TOOLBOX"));
      SimulationToolPanel foot = new SimulationToolPanel(simulationManager);
      simulationToolPanel.add(foot);
      this.addPropertyChangeListener(foot);
      foot.addPreferenceChangeListener(visualizationPanel);
    }
    return (SimulationToolPanel) simulationToolPanel.getComponent(0);
  }

  /**
   * @return
   */
  public DESSolver getSolver() {
    return simulationManager.getSimulationConfiguration().getSolver();
  }

  /***
   * Initializes the graphics components of this panel.
   */
  private void init() {
    setLayout(new BorderLayout());
    try {
      if (visualizationPanel == null) {
        visualizationPanel = new SimulationVisualizationPanel();
      }
      visualizationPanel.setModel(simulationManager.getSimulationConfiguration().getModel());
      SimulationToolPanel foot = getSimulationToolPanel();
      if (showSimulationToolPanel) {
        add(simulationToolPanel, BorderLayout.SOUTH);
      }
      Plot plot = visualizationPanel.getPlot();
      plot.setGridVisible(foot.getShowGrid());
      plot.setLegendVisible(foot.getShowLegend());
      plot.setDisplayToolTips(foot.getShowGraphToolTips());
      //      plot.setPlotToLogScale(foot.getJCheckBoxLegend());

      if (tabbedPane == null) {
        JPanel simPanel = new JPanel(new BorderLayout());
        simTable = new JTable();
        simTable.setDefaultRenderer(Double.class, new DecimalCellRenderer(10, 4, SwingConstants.RIGHT));
        simPanel.add(new JScrollPane(simTable), BorderLayout.CENTER);
        simTable.getModel().addTableModelListener(visualizationPanel);

        dataTableView = new MultipleTableView<MultiTable>();
        dataTableView.addTableModelListener(visualizationPanel);

        dynamicGraphView = new DynamicView(getModel().getSBMLDocument());
        simulationManager.addPropertyChangeListener(dynamicGraphView); //get simulation data when finished
        foot.addPreferenceChangeListener(dynamicGraphView.getDynamicController()); //get changed options
        this.addPropertyChangeListener(dynamicGraphView); //get experimental data
        dynamicGraphView.addPropertyChangeListener(this); //use of progressBar
        //        TranslatorSBMLgraphPanel graphView = new TranslatorSBMLgraphPanel(getModel().getSBMLDocument(), false);

        OpenedFile<SBMLDocument> openFile = new OpenedFile<SBMLDocument>(simulationManager.getSimulationConfiguration().getModel().getSBMLDocument());
        openFile.getDocument().addTreeNodeChangeListener(new SBMLfileChangeListener(openFile));

        tabbedPane = new JTabbedPane();
        tabbedPane.add(bundle.getString("TAB_SIMULATION"), visualizationPanel);
        tabbedPane.add(bundle.getString("TAB_IN_SILICO_DATA"), simPanel);
        tabbedPane.add(bundle.getString("TAB_EXPERIMENTAL_DATA"), dataTableView);
        tabbedPane.add(bundle.getString("TAB_MODEL_VIEW"), new SBMLModelSplitPane(openFile, true));
        tabbedPane.add(bundle.getString("TAB_GRAPH_VIEW"), dynamicGraphView);
        tabbedPane.setEnabledAt(TAB_SIMULATION_INDEX, true);
        tabbedPane.setEnabledAt(TAB_IN_SILICO_DATA_INDEX, false);
        tabbedPane.setEnabledAt(TAB_EXPERIMENT_INDEX, false);
        tabbedPane.setEnabledAt(TAB_MODEL_VIEW_INDEX, true);
        tabbedPane.setEnabledAt(TAB_GRAPH_VIEW_INDEX, true);
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

  /* (non-Javadoc)
   * @see eva2.server.stat.InterfaceStatisticsListener#notifyGenerationPerformed(java.lang.String[], java.lang.Object[], java.lang.Double[])
   */
  @Override
  public void notifyGenerationPerformed(String[] header, Object[] statObjects, Double[] statDoubles) {
    if ((simulationDataIndex == 0) && (runBestIndex == 0)) {
      notifyRunStarted(0, 1, header, null);
    }
    else {
      getSimulationManager().getEstimationProblem().calculateStatisticsForGeneration();

      setSimulationData(getSimulationManager().getEstimationProblem().getCurrentSimulationData());

      double newValue = statDoubles[runBestIndex].doubleValue();
      if (getSimulationToolPanel().getQualityMeasure() instanceof PearsonCorrelation) {
        newValue = Math.abs(newValue);
      }
      firePropertyChange("quality", getSimulationToolPanel().getCurrentQuality(),
        newValue);
      double[] quantities = getSimulationManager().getEstimationProblem().getBestSolutionFound();
      if ((quantities != null) && (quantities.length == selectedQuantityIds.length)) {
        for (int i = 0; i < selectedQuantityIds.length; i++) {
          visualizationPanel.updateQuantity(selectedQuantityIds[i],
            quantities[i]);
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see eva2.server.stat.InterfaceStatisticsListener#notifyMultiRunFinished(java.lang.String[], java.util.List)
   */
  @Override
  public boolean notifyMultiRunFinished(String[] header, List<Object[]> multiRunFinalObjectData) {
    int i;
    for (Object[] obj: multiRunFinalObjectData) {
      String tmp = obj[solutionIndex].toString();
      StringBuilder sb = new StringBuilder();
      char curr;
      for (i = 0; i < tmp.length(); i++) {
        curr = tmp.charAt(i);
        if ((curr != '{') && (curr != '}')) {
          sb.append(curr);
          if ((curr == ',') && (tmp.charAt(i + 1) == '-')) {
            sb.append(' ');
          }
        }
      }
      //String[] solutionString = sb.toString().split(", ");
      logger.info("Fitness: " + (obj[1]));
      double[] quantities = getSimulationManager().getEstimationProblem().getBestSolutionFound();
      for (i = 0; i < selectedQuantityIds.length; i++) {
        visualizationPanel.updateQuantity(selectedQuantityIds[i], quantities[i]);
        logger.info(selectedQuantityIds[i] + ": " + quantities[i]);
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
  @Override
  public void notifyRunStarted(int runNumber, int plannedMultiRuns,
    String[] header, String[] metaInfo) {
    getSimulationManager().getEstimationProblem().setOptimizer(evaClient.getOptimizationParameters().getOptimizer());


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
  @Override
  public void notifyRunStopped(int runsPerformed, boolean completedLastRun) {
    logger.fine("notifyRunStopped");
  }

  /* (non-Javadoc)
   * @see java.util.prefs.PreferenceChangeListener#preferenceChange(java.util.prefs.PreferenceChangeEvent)
   */
  @Override
  public void preferenceChange(PreferenceChangeEvent evt) {
    getSimulationToolPanel().preferenceChange(evt);
  }

  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    logger.fine(evt.getPropertyName());
    if (evt.getPropertyName().equals("progress")) {
      firePropertyChanged(evt);
    } else if (evt.getPropertyName().equals("done")) {
      MultiTable data = (MultiTable) evt.getNewValue();
      if (data != null) {
        setSimulationData(data);
        try {
          if (simulationManager.getQualityMeasurement().getMeasurements().size() > 0) {
            firePropertyChange("quality", getSimulationToolPanel()
              .getCurrentQuality(), simulationManager.getMeanDistanceValue());
          }
        } catch (Exception exc) {
          logger.warning(getMessage(exc));
        }
      }
      firePropertyChanged(evt);
    }
  }

  /**
   *
   */
  public void refreshStepSize() {
    getSolver().setStepSize(simulationManager.getSimulationConfiguration().getStepSize());
  }

  /* (non-Javadoc)
   * @see org.sbml.simulator.math.odes.DESSolver#removePropertyChangedListener(java.beans.PropertyChangeListener)
   */
  public void removePropertyChangedListener(PropertyChangeListener listener) {
    if (listeners.contains(listener)) {
      listeners.remove(listener);
    }
  }

  /**
   *
   * @param saveDir
   * @return
   */
  private File saveModel(String saveDir) {
    File f = GUITools.saveFileDialog(this, saveDir, false, false,
      JFileChooser.FILES_ONLY, SBFileFilter.createSBMLFileFilter());
    if (f != null) {
      try {
        SBMLWriter.write(getModel().getSBMLDocument(), f,
          System.getProperty("app.name"),
          System.getProperty("app.version"));
        return f;
      } catch (Exception exc) {
        GUITools.showErrorMessage(this, exc);
      }
    }
    return null;
  }

  /**
   *
   * @param saveDir
   * @return
   */
  private File savePlotImage(String saveDir) {
    try {
      Plot plot = visualizationPanel.getPlot();
      JFreeChart chart = plot.getChart();
      SBFileFilter pngFileFilter = SBFileFilter.createPNGFileFilter();
      SBFileFilter jpegFileFilter = SBFileFilter.createJPEGFileFilter();
      File file = GUITools.saveFileDialog(this, saveDir, false, false, true,
        JFileChooser.FILES_ONLY, pngFileFilter, jpegFileFilter);
      if (file != null) {
        if (jpegFileFilter.accept(file)) {
          ChartUtilities.saveChartAsJPEG(file, chart, plot.getWidth(),
            plot.getHeight());
        } else { //if (pngFileFilter.accept(file)) {
          ChartUtilities.saveChartAsPNG(file, chart, plot.getWidth(),
            plot.getHeight());
        }
      }
    } catch (IOException exc) {
      GUITools.showErrorMessage(this, exc);
    }
    return null;
  }

  /**
   *
   */
  private File saveTable(MultiTable tableModel, String noDataAvailableMessage) {
    try {
      SBPreferences prefs = SBPreferences.getPreferencesFor(SimulatorUI.class);
      if (tableModel.getRowCount() > 0) {
        File out = GUITools.saveFileDialog(this, prefs.get(
          CSVOptions.CSV_FILES_SAVE_DIR).toString(), false, false,
          JFileChooser.FILES_ONLY, SBFileFilter.createCSVFileFilter());
        if (out != null) {
          (new CSVWriter()).write(tableModel, prefs.get(
            CSVOptions.CSV_FILES_SEPARATOR_CHAR).toString().charAt(0), out);
          prefs.put(CSVOptions.CSV_FILES_SAVE_DIR, out.getParent());
          try {
            prefs.flush();
          } catch (BackingStoreException exc) {
            // Ignore this exception because the user doesn't have a chance to do anything here.
            logger.fine(getMessage(exc));
          }
        }
      } else {
        JOptionPane.showMessageDialog(this,
          StringUtil.toHTML(noDataAvailableMessage, 40));
      }
    } catch (IOException exc) {
      GUITools.showErrorMessage(this, exc);
    }
    // This is on purpose! Otherwise, the CSV save directory would be used for other files also!
    return null;
  }

  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrameTab#saveToFile()
   */
  @Override
  public File saveToFile() {
    SBPreferences prefs = SBPreferences.getPreferencesFor(SimulatorUI.class);
    String saveDir = prefs.get(GUIOptions.SAVE_DIR);
    switch (tabbedPane.getSelectedIndex()) {
    case TAB_SIMULATION_INDEX:
      return savePlotImage(saveDir);
    case TAB_MODEL_VIEW_INDEX:
      return saveModel(saveDir);
    case TAB_EXPERIMENT_INDEX:
      return saveTable(dataTableView.getSelectedTable(), bundle.getString("NO_EXPERIMENTAL_DATA_LOADED"));
    case TAB_IN_SILICO_DATA_INDEX:
      return saveTable(getSimulationResultsTable(), bundle.getString("NO_SIMULATION_PERFORMED"));
    case TAB_GRAPH_VIEW_INDEX:
      return saveGraph(saveDir);
    default:
      return null;
    }
  }

  /**
   * @param saveDir
   * @return
   */
  private File saveGraph(String saveDir) {
    File f = GUITools.saveFileDialog(this, saveDir, false, false,
      JFileChooser.FILES_ONLY, SBFileFilter.createJPEGFileFilter());
    if (f != null) {
      try {
        Graph2D graph = dynamicGraphView.getGraph().getGraph2D();
        new Graph2Dwriter(Graph2Dwriter.WriteableFileExtensions.jpeg).writeToFile(graph,f.toString());
        return f;
      } catch (Exception exc) {
        GUITools.showErrorMessage(this, exc);
      }
    }
    return null;
  }

  /**
   * @param enabled
   */
  public void setAllEnabled(boolean enabled) {
    visualizationPanel.setInteractiveScanEnabled(enabled);
    ((SimulationToolPanel) simulationToolPanel.getComponent(0)).setAllEnabled(enabled);
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
    if (showSimulationToolPanel != visible) {
      showSimulationToolPanel = visible;
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
    data.setTimeName(bundle.getString("TIME"));
    visualizationPanel.setSimulationData(data);
    simTable.setModel(data);
    simTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    TableColumnModel tabColMod = simTable.getColumnModel();
    TableColumn col;
    for (int i = 0; i < simTable.getColumnCount(); i++) {
      col = tabColMod.getColumn(i);
      col.setPreferredWidth(140);
      col.setMinWidth(140);
    }
    tabbedPane.setEnabledAt(TAB_IN_SILICO_DATA_INDEX, true);
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
   * Interrupts a running simulation.
   */
  public boolean stopSimulation() {
    if (simulationManager != null) {
      return simulationManager.stopSimulation();
    }
    return false;
  }

  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrameTab#updateButtons(javax.swing.JMenuBar, javax.swing.JToolBar)
   */
  @Override
  public void updateButtons(JMenuBar menuBar, JToolBar... toolbar) {
    // TODO Auto-generated method stub
  }

  /**
   * Opens a dialog to print the current plot.
   */
  public void print() {
    if (visualizationPanel != null) {
      visualizationPanel.print();
    }
  }

  /**
   * @return the tabbedPane
   */
  public JTabbedPane getTabbedPane() {
    return tabbedPane;
  }

  /**
   * @param tabbedPane the tabbedPane to set
   */
  public void setTabbedPane(JTabbedPane tabbedPane) {
    this.tabbedPane = tabbedPane;
  }

  /**
   * @return the dynamicGraphView
   */
  public DynamicView getDynamicGraphView() {
    return dynamicGraphView;
  }

  /**
   * @param evaClient
   */
  public void setClient(Main evaClient) {
    this.evaClient = evaClient;
  }

}
