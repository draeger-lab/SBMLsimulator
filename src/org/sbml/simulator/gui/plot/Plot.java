/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2014 by the University of Tuebingen, Germany.
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
import java.awt.Paint;
import java.awt.Shape;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
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
public class Plot extends ChartPanel implements PreferenceChangeListener {

	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(Plot.class.getName());
	
	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 176134486775218455L;

	private Map<String, LegendItem> legendItems;

	/**
	 * Create a new empty plot panel
	 */
	public Plot() {
		this("", "", "");
	}

	/**
	 * Create an new empty plot panel with xlabel xname and ylabel yname.
	 * 
	 * @param title
	 *        the title of the plot.
	 * @param xname
	 *        the label of the x-axis
	 * @param yname
	 *        the label of the y-axis
	 */
	public Plot(String title, String xname, String yname) {
		super(ChartFactory.createXYLineChart(title, xname, yname, null, true), false,
			true, true, false, true);
		this.listOfDataSetProps = new LinkedList<Boolean>();
		
		JFreeChart chart = getChart();
		
		XYPlot plot = chart.getXYPlot(); 
		plot.setDomainPannable(true);
		plot.setRangePannable(true);

		setMouseWheelEnabled(true);

		LegendTitle legend = chart.getLegend();
		legend.setPosition(RectangleEdge.BOTTOM);
		legend.setHorizontalAlignment(HorizontalAlignment.CENTER);
		legend.setVerticalAlignment(VerticalAlignment.CENTER);
		legendItems = new TreeMap<String, LegendItem>();
		loadUserSettings();
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
	 * Clear all data, currently plotted by this panel.
	 */
	public void clearAll() {
		JFreeChart chart = getChart();
		if (chart != null) {
			for (int i = 0; i < listOfDataSetProps.size(); i++) {
				chart.getXYPlot().setDataset(i, null);
			}
			legendItems.clear();
			listOfDataSetProps.clear();
		}
	}

	/**
	 * 
	 * @param series
	 * @param renderer
	 * @param id
	 * @param label
	 * @param infos
	 * @param col
	 * @param connected
	 * @param visible
	 */
	private void createItemLabel(int series, AbstractRenderer renderer,
		String id, String label, String infos, Paint col, boolean connected,
		boolean visible) {
		if (visible) {
			LegendItem legendItem = legendItems.containsKey(id) ? legendItems.get(id) : new LegendItem(label, col);
			if (infos != null) {
				legendItem.setToolTipText(infos);
			}
			legendItem.setSeriesIndex(series);
			Shape shape = null;
			if (connected) { //  || (datasetCount > 0)
				shape = renderer.lookupLegendShape(series);
				if (shape != null) {
					legendItem.setLine(shape);
				}
				legendItem.setLinePaint(col);
				legendItem.setLineVisible(true);
			}
			if (!connected) { //  || (datasetCount > 0)
				shape = renderer.lookupSeriesShape(series);
				if (shape != null) {
					legendItem.setShape(shape);
				}
				legendItem.setShapeVisible(true);
				legendItem.setOutlinePaint(Color.BLACK); 
			}
			legendItems.put(id, legendItem);
		}
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
		
		setLegendVisible(prefs.getBoolean(PlotOptions.SHOW_PLOT_LEGEND));
		setGridVisible(prefs.getBoolean(PlotOptions.SHOW_PLOT_GRID));
		setDisplayToolTips(prefs.getBoolean(PlotOptions.SHOW_PLOT_TOOLTIPS));

		XYItemRenderer renderer = plot.getRenderer(1);
		if (renderer instanceof XYLineAndShapeRenderer) {
			((XYLineAndShapeRenderer) renderer).setBaseLinesVisible(false);
			((XYLineAndShapeRenderer) renderer).setBaseShapesVisible(true);
		}
	}

	private LinkedList<Boolean> listOfDataSetProps;
	
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
		int datasetCount = listOfDataSetProps.size();
		listOfDataSetProps.add(Boolean.valueOf(connected));
		XYPlot plot = getChart().getXYPlot();
		plot.setDataset(datasetCount, dataset);

		AbstractRenderer renderer;
		
		if (dataset instanceof BoxAndWhiskerXYDataset) {
			renderer = new XYBoxAndWhiskerRenderer();
			/*
			 * This is necessary because otherwise the boxes of all series will be
			 * drawn in an identical color. Only if the box paint is null, it will
			 * look for the series paint.
			 */
			((XYBoxAndWhiskerRenderer) renderer).setBoxPaint(null);
		} else {
			renderer = new XYLineAndShapeRenderer();
			((XYLineAndShapeRenderer) renderer).setBaseLinesVisible(true);
			((XYLineAndShapeRenderer) renderer).setBaseShapesVisible(false);
		}
		
		plot.setRenderer(datasetCount, (XYItemRenderer) renderer);
		
		for (int i = 0; i < plotColors.length; i++) {
			Color col = plotColors[i];
			boolean visible = col != null;
			setSeriesVisible(datasetCount, renderer, i, infos[i], col);
			if (!(dataset instanceof MetaDataset)) {
				createItemLabel(i, renderer,
					((MetaDataset) dataset).getSeriesIdentifier(i),
					dataset.getSeriesKey(i).toString(), infos[i], col, connected, visible);
			}
		}
		setLegendVisible(showLegend);
		setGridVisible(showGrid);
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

	/* (non-Javadoc)
	 * @see java.util.prefs.PreferenceChangeListener#preferenceChange(java.util.prefs.PreferenceChangeEvent)
	 */
	public void preferenceChange(PreferenceChangeEvent evt) {
		String key = evt.getKey();
		logger.fine(key + " = " + evt.getNewValue());
		if (PlotOptions.PLOT_GRID_COLOR.equals(key)) {
			getChart().getXYPlot().setRangeGridlinePaint(Option.parseOrCast(Color.class, evt.getNewValue()));
		} else if (PlotOptions.PLOT_BACKGROUND_COLOR.equals(key)) {
			getChart().getXYPlot().setBackgroundPaint(Option.parseOrCast(Color.class, evt.getNewValue()));
		} else if (PlotOptions.SHOW_PLOT_GRID.equals(key)) {
			setGridVisible(Boolean.parseBoolean(evt.getNewValue()));
		} else if (PlotOptions.SHOW_PLOT_LEGEND.equals(key)) {
			setLegendVisible(Boolean.parseBoolean(evt.getNewValue()));
		} else if (PlotOptions.SHOW_PLOT_TOOLTIPS.equals(key)) {
			setDisplayToolTips(Boolean.parseBoolean(evt.getNewValue()));
		}
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
	public void setLegendVisible(boolean showLegend) {
		LegendTitle legend = getChart().getLegend();
		if (legend != null) {
			if (showLegend) {
				LegendItemCollection legendItemCollection = new LegendItemCollection();
				LegendItem item;
				for (Map.Entry<String, LegendItem> entry : legendItems.entrySet()) {
					item = entry.getValue();
					legendItemCollection.add(item);
				}
				getChart().getXYPlot().setFixedLegendItems(legendItemCollection);
			}
			legend.setVisible(showLegend);
		}
	}

	/**
	 * 
	 * @param items
	 */
	public void setSeriesVisible(List<SeriesInfo> items) {
		int index;
		Paint paint;
		XYPlot plot = getChart().getXYPlot();
		String id, tooltip;
		MetaDataset data;
		SeriesInfo item;
		AbstractRenderer renderer;
		for (int i = 0; i < plot.getDatasetCount(); i++) {
			// go through all datasets
			data = (MetaDataset) plot.getDataset(i);
			renderer = (AbstractRenderer) plot.getRenderer(i);
			for (int j = 0; (j < items.size()) && (data != null); j++) {
				// look at each item whose state might have changed
				item = items.get(j);
				id = item.getId();
				tooltip = item.getTooltip();
				paint = item.getPaint();
				index = data.getSeriesIndex(id);
				setSeriesVisible(i, renderer, index, tooltip, paint);
			}
		}
		// update the legend
		setLegendVisible(getChart().getLegend().isVisible());
	}

	/**
	 * 
	 * @param dataSetIndex
	 * @param renderer
	 * @param index
	 * @param tooltip
	 * @param paint
	 */
	private void setSeriesVisible(int dataSetIndex, AbstractRenderer renderer,
		int index, String tooltip, Paint paint) {
		if (index > -1) {
			Boolean visible = Boolean.valueOf(paint != null);
			XYPlot plot = getChart().getXYPlot();
			renderer.setSeriesVisible(index, visible);
			renderer.setSeriesVisibleInLegend(index, visible);
			renderer.setSeriesItemLabelsVisible(index, visible);
			renderer.setSeriesCreateEntities(index, visible);
			XYDataset data = plot.getDataset(dataSetIndex);
			String label = data.getSeriesKey(index).toString();
			String id = null;
			if (data instanceof MetaDataset) {
				id = ((MetaDataset) data).getSeriesIdentifier(index);
			}
			if (visible.booleanValue()) {
				Boolean v = listOfDataSetProps.get(dataSetIndex);
				boolean connected = v.booleanValue();
				boolean linesVisible = v.booleanValue();
				boolean shapesVisible = !v.booleanValue();
				if (renderer instanceof XYBoxAndWhiskerRenderer) {
					/*
					 * Makes the "value" of the box a transparent item, i.e., invisible.
					 * This causes the average and the mean not to be visible... because
					 * both are drawn in the identical color.
					 */
					((XYBoxAndWhiskerRenderer) renderer).setArtifactPaint(index, Color.BLACK);
					((XYBoxAndWhiskerRenderer) renderer).setMeanPaint(index, new Color(0, 0, 0, 75));
				} else if (renderer instanceof XYLineAndShapeRenderer) {
					((XYLineAndShapeRenderer) renderer).setSeriesLinesVisible(index, linesVisible);
					((XYLineAndShapeRenderer) renderer).setSeriesShapesVisible(index, shapesVisible);
				}
				renderer.setSeriesPaint(index, paint);
				renderer.setSeriesFillPaint(index, paint);
				if ((id != null) && !legendItems.containsKey(id)) {
					createItemLabel(index, renderer, id, label, tooltip, paint, connected, visible);
				}
			} else if ((id != null) && legendItems.containsKey(id)) {
				legendItems.remove(id);
			}
		}
	}

	/**
	 * 
	 * @param selected
	 */
	public void toggleLog(boolean selected) {
		logger.severe("Log scale not implemented");
	}

}
