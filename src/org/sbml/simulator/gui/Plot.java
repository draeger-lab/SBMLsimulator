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

import java.awt.AWTException;
import java.awt.Color;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.sbml.simulator.math.odes.MultiBlockTable;

import de.zbit.io.SBFileFilter;

/**
 * @author Andreas Dr&auml;ger
 * @author Max Zwie§ele
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
				new DefaultXYDataset(), PlotOrientation.VERTICAL, false, false,
				false));
		this.xlabel = xname;
		this.ylabel = yname;
		this.setMouseWheelEnabled(true);
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

		JFreeChart chart = ChartFactory.createXYLineChart("", xlabel, ylabel,
				new MultiBlockTableToTimeSeriesAdapter(plotData),
				PlotOrientation.VERTICAL, true, true, false);
				
		XYPlot plot = chart.getXYPlot();
		XYItemRenderer renderer = plot.getRenderer();

		for (int i = 0; i < plotColors.length; i++) {
			renderer.setSeriesPaint(i, plotColors[i]);
		}
			
		ValueAxis yAxis = chart.getXYPlot().getRangeAxis();
		ValueAxis xAxis = chart.getXYPlot().getDomainAxis();
		
		yAxis.setLowerBound(0);
		
		this.setChart(chart);
		this.setGridVisible(showGrid);
		this.setShowLegend(showLegend);

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
		if(this.getChart().getLegend()!=null)
			this.getChart().getLegend().setVisible(showLegend);
	}

	public void setShowGraphToolTips(boolean showGraphToolTips) {
		this.setDisplayToolTips(showGraphToolTips);
	}

	public void clearAll() {
		JFreeChart chart = ChartFactory.createXYLineChart("", xlabel, ylabel,
				new DefaultXYDataset(), PlotOrientation.VERTICAL, false, false,
				false);
		this.setChart(chart);
	}

	public boolean checkLoggable() {
		return false;
	}

	public void toggleLog(boolean selected) {

	}

}
