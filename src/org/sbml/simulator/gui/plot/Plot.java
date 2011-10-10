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

import java.awt.AWTException;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Formatter;

import javax.swing.JFileChooser;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.editor.ChartEditor;
import org.jfree.chart.editor.ChartEditorFactory;
import org.jfree.chart.editor.ChartEditorManager;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.VerticalAlignment;
import org.sbml.jsbml.util.StringTools;
import org.sbml.simulator.gui.GUITools;
import org.sbml.simulator.math.odes.MultiBlockTable;

import de.zbit.io.SBFileFilter;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.SBPreferences;

/**
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
	private String xlabel;
	private String ylabel;
	private boolean legend, grid;

	/**
     * 
     */
	public Plot() {
		this("", "");
	}

	/**
	 * @param xname
	 * @param yname
	 */
	public Plot(String xname, String yname) {
		super(ChartFactory.createXYLineChart("", xname, yname,
				new DefaultXYDataset(), PlotOrientation.VERTICAL, true, false,
				false), false, true, true, false, true);

		this.getChart().getXYPlot().setDomainPannable(true);
		this.getChart().getXYPlot().setRangePannable(true);

		this.xlabel = xname;
		this.ylabel = yname;
		this.setMouseWheelEnabled(true);
		
		this.getChart().getLegend().setPosition(RectangleEdge.BOTTOM);
		this.getChart().getLegend()
				.setHorizontalAlignment(HorizontalAlignment.CENTER);
		this.getChart().getLegend()
				.setVerticalAlignment(VerticalAlignment.CENTER);

		loadUserSettings();
	}

	private void loadUserSettings() {
		// retrieve a user-defined preference
		SBPreferences prefs = SBPreferences
				.getPreferencesFor(PlotOptions.class);

		this.getChart().getXYPlot().setBackgroundPaint(Option.parseOrCast(Color.class, prefs.get(PlotOptions.PLOT_BACKGROUND)));
		
		this.getChart().getXYPlot().setDomainGridlinePaint(Option.parseOrCast(Color.class, prefs.get(PlotOptions.PLOT_GRID_COLOR)));
		this.getChart().getXYPlot().setRangeGridlinePaint(Option.parseOrCast(Color.class, prefs.get(PlotOptions.PLOT_GRID_COLOR)));
		
		this.setGridVisible(grid);
		this.setShowLegend(legend);
		
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

		this.grid = showGrid;
		this.legend = showLegend;
		
		this.loadUserSettings();	
	}

	/**
	 * @param saveDir
	 * @param compression
	 * @return The path to the directory where the user wants to save the plot
	 *         image. If no image is saved, i.e., the user canceled this action,
	 *         the same directory will be returned that has been given as an
	 *         argument.
	 * @throws AWTException
	 * @throws IOException
	 */
	public String savePlotImage(String saveDir, float compression)
			throws AWTException, IOException {

		try {
			File file = GUITools.saveFileDialog(this, saveDir, false, false,
					JFileChooser.FILES_ONLY,
					SBFileFilter.createPNGFileFilter(),
					SBFileFilter.createJPEGFileFilter());
			ChartUtilities.saveChartAsJPEG(file, compression, this.getChart(),
					WIDTH, HEIGHT, getChartRenderingInfo());
			System.out.println(saveDir);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Rectangle area = getBounds();
		// area.setLocation(getLocationOnScreen());
		// BufferedImage bufferedImage = (new
		// Robot()).createScreenCapture(area);
		// File file = GUITools.saveFileDialog(this, saveDir, false, false,
		// JFileChooser.FILES_ONLY, SBFileFilter.createPNGFileFilter(),
		// SBFileFilter.createJPEGFileFilter());
		// if (file != null) {
		// saveDir = file.getParent();
		// if (SBFileFilter.isPNGFile(file)) {
		// ImageIO.write(bufferedImage, "png", file);
		// } else if (SBFileFilter.isJPEGFile(file)) {
		// FileImageOutputStream out = new FileImageOutputStream(file);
		// ImageWriter encoder = (ImageWriter) ImageIO
		// .getImageWritersByFormatName("JPEG").next();
		// JPEGImageWriteParam param = new JPEGImageWriteParam(null);
		// param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		// param.setCompressionQuality(compression);
		// encoder.setOutput(out);
		// encoder.write((IIOMetadata) null, new IIOImage(bufferedImage,
		// null, null), param);
		// out.close();
		// }
		// }

		return saveDir;
	}

	/**
	 * @param data
	 * @param connected
	 * @param plotColors
	 * @param infos
	 */
	public void plot(MultiBlockTable data, boolean connected,
			Color[] plotColors, String[] infos) {
		plot(data, connected, legend, grid, plotColors, infos);
	}

	public void setGridVisible(boolean showGrid) {
		this.grid = showGrid;
		this.getChart().getXYPlot().setDomainGridlinesVisible(showGrid);
		this.getChart().getXYPlot().setRangeGridlinesVisible(showGrid);
	}

	public void setShowLegend(boolean showLegend) {
		this.legend = showLegend;
		if (this.getChart().getLegend() != null)
			this.getChart().getLegend().setVisible(showLegend);
	}

	public void setShowGraphToolTips(boolean showGraphToolTips) {
		this.setDisplayToolTips(showGraphToolTips);
	}

	public void clearAll() {
		if (this.getChart() != null)
			this.getChart().getXYPlot().setDataset(new DefaultXYDataset());
	}

	public boolean checkLoggable() {
		return false;
	}

	public void toggleLog(boolean selected) {

	}

}
