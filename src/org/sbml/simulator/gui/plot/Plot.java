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
package org.sbml.simulator.gui.plot;

import java.awt.Color;
import java.util.logging.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.util.HorizontalAlignment;
import org.jfree.chart.util.RectangleEdge;
import org.jfree.chart.util.VerticalAlignment;
import org.jfree.data.statistics.BoxAndWhiskerXYDataset;
import org.jfree.data.xy.XYDataset;

import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.SBPreferences;

/**
 * This class represents the plot panel in which the calculated data will be
 * plotted.
 * 
 * @author Andreas Dr&auml;ger
 * @author Max Zwie&szlig;ele
 * @author Philip Stevens
 * @date 2010-09-08
 * @version $Rev$
 * @since 1.0
 */
public class Plot extends ChartPanel {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 176134486775218455L;
	
	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(Plot.class.getName());
	
	/**
	 * 
	 */
	private int datasetCount;

	/**
	 * Create a new empty plot panel
	 */
	public Plot() {
		this("", "");
	}

	/**
	 * Create an new empty plot panel with xlabel xname and ylabel yname.
	 * 
	 * @param xname
	 *            the label of the x-axis
	 * @param yname
	 *            the label of the y-axis
	 */
	public Plot(String xname, String yname) {
		super(ChartFactory.createXYLineChart("", xname, yname, null, true), false,
			true, true, false, true);
		
		JFreeChart chart = getChart();
		
		XYPlot plot = chart.getXYPlot(); 
		plot.setDomainPannable(true);
		plot.setRangePannable(true);

		setMouseWheelEnabled(true);

		LegendTitle legend = chart.getLegend();
		legend.setPosition(RectangleEdge.BOTTOM);
		legend.setHorizontalAlignment(HorizontalAlignment.CENTER);
		legend.setVerticalAlignment(VerticalAlignment.CENTER);
		loadUserSettings();
	}

	/**
	 * Setup all predefined user settings.
	 */
	private void loadUserSettings() {
		// retrieve a user-defined preference
		SBPreferences prefs = SBPreferences.getPreferencesFor(PlotOptions.class);
		JFreeChart chart = getChart();
		XYPlot plot = chart.getXYPlot();
		
		plot.setBackgroundPaint(Option.parseOrCast(Color.class,
			prefs.get(PlotOptions.PLOT_BACKGROUND_COLOR)));
		plot.setDomainGridlinePaint(Option.parseOrCast(Color.class,
			prefs.get(PlotOptions.PLOT_GRID_COLOR)));
		plot.setRangeGridlinePaint(Option.parseOrCast(Color.class,
			prefs.get(PlotOptions.PLOT_GRID_COLOR)));
		
		chart.setTitle(prefs.get(PlotOptions.PLOT_TITLE));
		
		setShowLegend(prefs.getBoolean(PlotOptions.SHOW_PLOT_LEGEND));
		setGridVisible(prefs.getBoolean(PlotOptions.SHOW_PLOT_GRID));
		setDisplayToolTips(prefs.getBoolean(PlotOptions.SHOW_PLOT_TOOLTIPS));

		XYItemRenderer renderer = plot.getRenderer(1);
		if (renderer instanceof XYLineAndShapeRenderer) {
			((XYLineAndShapeRenderer) renderer).setBaseLinesVisible(false);
			((XYLineAndShapeRenderer) renderer).setBaseShapesVisible(true);
		}
	}

	/**
	 * Plots a matrix either by displaying unconnected or connected points
	 * depending on the connected parameter.
	 * 
	 * @param plotData
	 *            The data to be plotted.
	 * @param connected
	 *            If true, all points will be connected, singular points are
	 *            plotted for false.
	 * @param showLegend
	 *            If true, a legend will be displayed in the plot.
	 * @param showGrid
	 *            If true, a grid will be added to the plot.
	 * @param plotColors
	 *            Colors to be used to plot the columns. This array must also be
	 *            of the same length as all columns in the plotData. Null values
	 *            mean that this column is not to be plotted.
	 * @param infos
	 *            Information to be associated with each column in the plot.
	 *            This array must have the same length as the number of columns
	 *            in the plotData field.
	 */
	public void plot(XYDataset dataset, boolean connected,
			boolean showLegend, boolean showGrid, Color[] plotColors,
			String[] infos) {
		
		XYPlot plot = getChart().getXYPlot();
		plot.setDataset(datasetCount, dataset);

		if (dataset instanceof BoxAndWhiskerXYDataset) {
			XYBoxAndWhiskerRenderer renderer = new XYBoxAndWhiskerRenderer();
			/*
			 * This is necessary because otherwise the boxes of all series will be
			 * drawn in an identical color. Only if the box paint is null, it will
			 * look for the series paint.
			 */
			renderer.setBoxPaint(null);
			
			for (int i = 0; i < plotColors.length; i++) {
				Color col = plotColors[i];
				boolean visible = col != null;
				renderer.setSeriesVisible(i, visible);
				renderer.setSeriesVisibleInLegend(i, visible);
				renderer.setSeriesCreateEntities(i, visible);
				renderer.setSeriesItemLabelsVisible(i, visible);
				if (visible) {
					renderer.setSeriesPaint(i, col);
					renderer.setSeriesFillPaint(i, col);
					/*
					 * Makes the "value" of the box a transparent item, i.e., invisible.
					 * This causes the average and the mean not to be visible... because
					 * both are drawn in the identical color.
					 */
					renderer.setArtifactPaint(i, new Color(0, 0, 0, 75));
				} else {
					col = new Color(255, 255, 255, 255);
					renderer.setSeriesPaint(i, col);
					renderer.setSeriesFillPaint(i, col);
					renderer.setSeriesOutlinePaint(i, col);
					renderer.setArtifactPaint(i, col);
				}
			}
			
			plot.setRenderer(datasetCount, renderer);
			
		} else {
			XYLineAndShapeRenderer linesAndShape = new XYLineAndShapeRenderer();
			linesAndShape.setBaseLinesVisible(true);
			linesAndShape.setBaseShapesVisible(false);
			for (int i = 0; i < plotColors.length; i++) {
				Color col = plotColors[i];
				boolean visible = col != null;
				linesAndShape.setSeriesLinesVisible(i, connected);
				linesAndShape.setSeriesShapesVisible(i, !connected);
				linesAndShape.setSeriesVisible(i, visible);
				linesAndShape.setSeriesVisibleInLegend(i, visible);
				linesAndShape.setSeriesPaint(i, col);
			}
			plot.setRenderer(datasetCount, linesAndShape);
		}
		
		datasetCount++;

		this.loadUserSettings();
	}

	/**
	 * plot given data into existing panel. All previous data will be cleared.
	 * 
	 * @param data
	 *            to plot
	 * @param connected
	 * @param plotColors
	 * @param infos
	 */
	public void plot(XYDataset data, boolean connected, Color[] plotColors, String[] infos) {
		// retrieve a user-defined preference
		SBPreferences prefs = SBPreferences.getPreferencesFor(PlotOptions.class);
		plot(data, connected, prefs.getBoolean(PlotOptions.SHOW_PLOT_LEGEND),
				prefs.getBoolean(PlotOptions.SHOW_PLOT_GRID), plotColors, infos);

	}

	/**
	 * Toggle, if the grid is shown in the plot panel.
	 * 
	 * @param showGrid
	 */
	public void setGridVisible(boolean showGrid) {
		// retrieve a user-defined preference
		XYPlot plot = getChart().getXYPlot();
		plot.setDomainGridlinesVisible(showGrid);
		plot.setRangeGridlinesVisible(showGrid);
	}

	/**
	 * Toggle, if the legend is shown in the plot panel.
	 * 
	 * @param showLegend
	 */
	public void setShowLegend(boolean showLegend) {
		LegendTitle legend = getChart().getLegend();
		if (legend != null) {
			legend.setVisible(showLegend);
		}
	}

	/**
	 * Clear all data, currently plotted by this panel.
	 */
	public void clearAll() {
		JFreeChart chart = getChart();
		if (chart != null) {
			for (int i = 0; i < datasetCount; i++) {
				chart.getXYPlot().setDataset(i, null);
			}
			datasetCount = 0;
		}
	}

	/**
	 * 
	 * @return false
	 */
	public boolean checkLoggable() {
		throw new UnsupportedOperationException(
				"This plot panel has no loggin feature.");
		// return false;
	}

	/**
	 * 
	 * @param selected
	 */
	public void toggleLog(boolean selected) {
		logger.severe("Log scale not implemented");
	}

}
