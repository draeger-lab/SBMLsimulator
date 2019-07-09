/*
 * $Id:  VODPanel.java 16:43:13 Meike Aichele$
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
package org.sbml.simulator.fba.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.category.SlidingCategoryDataset;
import org.sbml.jsbml.SBMLDocument;


/**
 * This class represents a panel for the Visualization Of Datasets (VOD)
 * of the computed datasets of fba.
 *
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class VODPanel extends JPanel implements ActionListener{

  /**
   * Contains the computed values for the concentrations
   */
  private double[] concentrations;

  /**
   * Contains the computed values for the fluxes
   */
  private double[] fluxes;

  /**
   * Contains the plot
   */
  private JPanel plot;

  private SBMLDocument modDoc;


  /**
   * default serial version
   */
  private static final long serialVersionUID = 1L;


  /**
   * Default constructor
   */
  public VODPanel() {
    plot = new JPanel(new BorderLayout());
  }

  /*
   * (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO: make button pressed answer

  }

  /**
   * Sets the conditions of the plot (name, axes, etc.)
   * @param dataset
   * @return
   */
  private static JFreeChart createChart(SlidingCategoryDataset dataset) {
    JFreeChart jfreechart = ChartFactory.createBarChart("Steady-State Fluxes", "Reaction", "Value", dataset, false);
    CategoryPlot cplot = (CategoryPlot) jfreechart.getPlot();

    CategoryAxis domainAxis = cplot.getDomainAxis();
    domainAxis.setMaximumCategoryLabelWidthRatio(0.8f);
    domainAxis.setLowerMargin(0.02);
    domainAxis.setUpperMargin(0.02);

    // disable bar outlines...
    BarRenderer renderer = (BarRenderer) cplot.getRenderer();
    renderer.setDrawBarOutline(false);

    // set up gradient paints for series...
    GradientPaint gp0 = new GradientPaint(0.0f, 0.0f, Color.blue,
      0.0f, 0.0f, new Color(0, 0, 64));
    renderer.setSeriesPaint(0, gp0);

    return jfreechart;
  }

  /**
   * @return the concentrations
   */
  public double[] getConcentrations() {
    return concentrations;
  }

  /**
   * @return the fluxes
   */
  public double[] getFluxes() {
    return fluxes;
  }

  /**
   * Sets the bar-plot
   */
  public void init() {
    DefaultCategoryDataset underlying = new DefaultCategoryDataset();
    for (int i = 0; i < fluxes.length; i++) {
      underlying.setValue(fluxes[i], "", modDoc.getModel().getReaction(i).getId());
    }
    SlidingCategoryDataset scd = new SlidingCategoryDataset(underlying, 0, fluxes.length);

    JFreeChart jfreechart = createChart(scd);
    org.jfree.chart.ChartPanel chartpanel = new org.jfree.chart.ChartPanel(jfreechart);

    plot.add(chartpanel, BorderLayout.CENTER);
    this.add(plot, BorderLayout.CENTER);
    //		JPanel changePlotPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    //		JButton switchButton = new JButton("Change to dynamic View");
    //		switchButton.addActionListener(this);
    //		changePlotPanel.add(switchButton, BorderLayout.EAST);
    //
    //		add(changePlotPanel, BorderLayout.SOUTH);
    setVisible(true);
  }

  /**
   * @param concentrations the concentrations to set
   */
  public void setConcentrations(double[] concentrations) {
    this.concentrations = concentrations;
  }

  /**
   * @param fluxes the fluxes to set
   */
  public void setFluxes(double[] fluxes) {
    this.fluxes = fluxes;
  }

  public void setDocument(SBMLDocument expandedDocument) {
    modDoc = expandedDocument;
  }


}
