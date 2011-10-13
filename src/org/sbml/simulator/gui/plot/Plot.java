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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.VerticalAlignment;
import org.sbml.simulator.math.odes.MultiBlockTable;

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
	// /**
	// * The x/y label of the plot's axes
	// */
	// private String xlabel,ylabel;

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
		super(ChartFactory.createXYLineChart("", xname, yname,
				new DefaultXYDataset(), PlotOrientation.VERTICAL, true, false,
				false), false, true, true, false, true);

		this.getChart().getXYPlot().setDomainPannable(true);
		this.getChart().getXYPlot().setRangePannable(true);

		this.setMouseWheelEnabled(true);

		this.getChart().getLegend().setPosition(RectangleEdge.BOTTOM);
		this.getChart().getLegend()
				.setHorizontalAlignment(HorizontalAlignment.CENTER);
		this.getChart().getLegend()
				.setVerticalAlignment(VerticalAlignment.CENTER);

		loadUserSettings();
	}

	/**
	 * Setup all predefined user settings.
	 */
	private void loadUserSettings() {
		// retrieve a user-defined preference
		SBPreferences prefs = SBPreferences
				.getPreferencesFor(PlotOptions.class);

		this.getChart()
				.getXYPlot()
				.setBackgroundPaint(
						Option.parseOrCast(Color.class,
								prefs.get(PlotOptions.PLOT_BACKGROUND_COLOR)));
		this.getChart()
				.getXYPlot()
				.setDomainGridlinePaint(
						Option.parseOrCast(Color.class,
								prefs.get(PlotOptions.PLOT_GRID_COLOR)));
		this.getChart()
				.getXYPlot()
				.setRangeGridlinePaint(
						Option.parseOrCast(Color.class,
								prefs.get(PlotOptions.PLOT_GRID_COLOR)));

		this.getChart().setTitle(prefs.get(PlotOptions.PLOT_TITLE));
		
		this.setShowLegend(prefs.getBoolean(PlotOptions.SHOW_PLOT_LEGEND));
		this.setGridVisible(prefs.getBoolean(PlotOptions.SHOW_PLOT_GRID));
		this.setShowGraphToolTips(prefs.getBoolean(PlotOptions.SHOW_PLOT_TOOLTIPS));
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
	public void plot(MultiBlockTable plotData, boolean connected,
			boolean showLegend, boolean showGrid, Color[] plotColors,
			String[] infos) {
		this.getChart().getXYPlot()
				.setDataset(new MultiBlockTableToTimeSeriesAdapter(plotData));

		XYItemRenderer renderer = this.getChart().getXYPlot().getRenderer();

		for (int i = 0; i < plotColors.length; i++) {
			Color col = plotColors[i];
			boolean visible = col != null;
			renderer.setSeriesVisible(i, visible);
			renderer.setSeriesVisibleInLegend(i, visible);
			renderer.setSeriesPaint(i, col);
		}

		this.loadUserSettings();
	}

	// /**
	// * @param saveDir
	// * @param compression
	// * @return The path to the directory where the user wants to save the plot
	// * image. If no image is saved, i.e., the user canceled this action,
	// * the same directory will be returned that has been given as an
	// * argument.
	// * @throws AWTException
	// * @throws IOException
	// */
	// public String savePlotImage(String saveDir, float compression)
	// throws AWTException, IOException {
	//
	// try {
	// File file = GUITools.saveFileDialog(this, saveDir, false, false,
	// JFileChooser.FILES_ONLY,
	// SBFileFilter.createPNGFileFilter(),
	// SBFileFilter.createJPEGFileFilter());
	// ChartUtilities.saveChartAsJPEG(file, compression, this.getChart(),
	// WIDTH, HEIGHT, getChartRenderingInfo());
	// System.out.println(saveDir);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// // Rectangle area = getBounds();
	// // area.setLocation(getLocationOnScreen());
	// // BufferedImage bufferedImage = (new
	// // Robot()).createScreenCapture(area);
	// // File file = GUITools.saveFileDialog(this, saveDir, false, false,
	// // JFileChooser.FILES_ONLY, SBFileFilter.createPNGFileFilter(),
	// // SBFileFilter.createJPEGFileFilter());
	// // if (file != null) {
	// // saveDir = file.getParent();
	// // if (SBFileFilter.isPNGFile(file)) {
	// // ImageIO.write(bufferedImage, "png", file);
	// // } else if (SBFileFilter.isJPEGFile(file)) {
	// // FileImageOutputStream out = new FileImageOutputStream(file);
	// // ImageWriter encoder = (ImageWriter) ImageIO
	// // .getImageWritersByFormatName("JPEG").next();
	// // JPEGImageWriteParam param = new JPEGImageWriteParam(null);
	// // param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	// // param.setCompressionQuality(compression);
	// // encoder.setOutput(out);
	// // encoder.write((IIOMetadata) null, new IIOImage(bufferedImage,
	// // null, null), param);
	// // out.close();
	// // }
	// // }
	//
	// return saveDir;
	// }

	/**
	 * plot given data into existing panel. All previous data will be cleared.
	 * 
	 * @param data
	 *            to plot
	 * @param connected
	 * @param plotColors
	 * @param infos
	 */
	public void plot(MultiBlockTable data, boolean connected,
			Color[] plotColors, String[] infos) {
		// retrieve a user-defined preference
				SBPreferences prefs = SBPreferences
						.getPreferencesFor(PlotOptions.class);
		plot(data, connected, prefs.getBoolean(PlotOptions.SHOW_PLOT_LEGEND), prefs.getBoolean(PlotOptions.SHOW_PLOT_GRID), plotColors, infos);
		
	}
	/**
	 * Toggle, if the grid is shown in the plot panel.
	 * 
	 * @param showGrid
	 */
	public void setGridVisible(boolean showGrid) {
		// retrieve a user-defined preference
		this.getChart().getXYPlot().setDomainGridlinesVisible(showGrid);
		this.getChart().getXYPlot().setRangeGridlinesVisible(showGrid);
	}

	/**
	 * Toggle, if the legend is shown in the plot panel.
	 * 
	 * @param showLegend
	 */
	public void setShowLegend(boolean showLegend) {
		// retrieve a user-defined preference
//		SBPreferences prefs = SBPreferences
//				.getPreferencesFor(PlotOptions.class);
//		prefs.put(PlotOptions.SHOW_PLOT_LEGEND, showLegend);
		if (this.getChart().getLegend() != null)
			this.getChart().getLegend().setVisible(showLegend);
	}

	/**
	 * Toggle, if tooltips are shown in the graph.
	 * 
	 * @param showGraphToolTips
	 */
	public void setShowGraphToolTips(boolean showGraphToolTips) {
		// retrieve a user-defined preference
//		SBPreferences prefs = SBPreferences
//				.getPreferencesFor(PlotOptions.class);
//		prefs.put(PlotOptions.SHOW_PLOT_TOOLTIPS, showGraphToolTips);
		this.setDisplayToolTips(showGraphToolTips);
	}

	/**
	 * Clear all data, currently plotted by this panel.
	 */
	public void clearAll() {
		if (this.getChart() != null)
			this.getChart().getXYPlot().setDataset(new DefaultXYDataset());
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

	}

}
