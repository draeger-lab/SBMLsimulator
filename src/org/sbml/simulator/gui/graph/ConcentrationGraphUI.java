package org.sbml.simulator.gui.graph;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.sbml.simulator.gui.LegendPanel;
import org.sbml.simulator.gui.SimulatorUI;
import org.sbml.simulator.gui.plot.BoxPlotDataset;
import org.sbml.simulator.gui.plot.MetaDataset;
import org.sbml.simulator.gui.plot.Plot;
import org.sbml.simulator.gui.plot.XYDatasetAdapter;
import org.sbml.simulator.gui.table.LegendTableModel;
import org.simulator.math.odes.MultiTable;

import de.zbit.sbml.util.HTMLtools;
import de.zbit.util.ResourceManager;

/**
 * This class opens a small window where all the concentrations of all species are shown. A red line is
 * passing the timepoints to give a better overvier over the different concentrations at a time.
 * It's opened by clicking an imagebutton in the graph view.
 * 
 * @author Lea Buchweitz
 */

public class ConcentrationGraphUI extends JFrame {

  private List<MultiTable> graphData;

  private Plot concentrationGraph;

  private JPanel graphPanel = new JPanel();

  private double timepoint = 0;

  private XYPlot plot;

  /**
   * Localization support.
   */
  private static final transient ResourceBundle bundle = ResourceManager
      .getBundle("org.sbml.simulator.gui.graph.DynamicGraph");


  public ConcentrationGraphUI(MultiTable data, LegendPanel legend, double[] minMax, double maxTime) {
    super();

    setResizable(true);
    setSize(400,400);
    setLocation(5,10);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    setTitle(bundle.getString("CONCENTRATION_GRAPH_TITLE"));
    setIconImage(new ImageIcon(SimulatorUI.class.getResource("img/SBMLsimulator_32.png")).getImage());

    setVisible(true);

    concentrationGraph = new Plot("", bundle.getString("X_AXIS"), bundle.getString("Y_AXIS"));

    if ((data != null) && (data.getRowCount() > 0)) {
      graphData = Arrays.asList(new MultiTable[] {data});
    }

    if (graphData.size() > 0) {

      MetaDataset d;
      if (graphData.size() == 1) {
        d = new XYDatasetAdapter(graphData.get(0));
      } else {
        d = new BoxPlotDataset(graphData);
      }
      String id;
      int seriesCount = d.getSeriesCount();
      Color plotColors[] = new Color[seriesCount];
      String infos[] = new String[seriesCount];
      LegendTableModel tableModel = legend.getLegendTableModel();
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
      concentrationGraph.plot(d, true, plotColors, infos);
      concentrationGraph.setGridVisible(true);
      concentrationGraph.setLegendVisible(false);
    }	

    graphPanel.add(concentrationGraph);

    plot = concentrationGraph.getChart().getXYPlot();
    ValueMarker marker = new ValueMarker(0);
    marker.setPaint(Color.red);
    plot.addDomainMarker(marker);

    NumberAxis yAxis = new LogarithmicAxis(bundle.getString("Y_AXIS"));
    yAxis.setLowerBound(minMax[0]); 
    yAxis.setUpperBound(minMax[1]+40); 
    NumberAxis xAxis = new NumberAxis(bundle.getString("X_AXIS"));
    xAxis.setLowerBound(1E-1);
    xAxis.setUpperBound(maxTime);
    plot.setRangeAxis(yAxis);
    plot.setDomainAxis(xAxis);

    add(graphPanel, BorderLayout.WEST);
    this.pack();

    setFocusableWindowState(false);
    setAlwaysOnTop(true);
  }

  public void setTimepoint(double timepoint) {
    plot = concentrationGraph.getChart().getXYPlot();
    plot.clearDomainMarkers();
    ValueMarker marker = new ValueMarker(timepoint);
    marker.setPaint(Color.red);
    marker.setStroke(new BasicStroke ( 1.5f ));
    plot.addDomainMarker(marker);
  }

}
