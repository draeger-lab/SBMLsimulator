/*
 * $Id: Plot.java 151 2011-03-31 08:39:33Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/SBMLsimulator/trunk/src/org/sbml/simulator/gui/Plot.java $
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
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JFileChooser;

import org.simulator.math.odes.MultiTable;
import org.simulator.math.odes.MultiTable.Block.Column;

import de.zbit.gui.GUITools;
import de.zbit.io.SBFileFilter;
import eva2.gui.FunctionArea;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-08
 * @version $Rev: 151 $
 * @since 1.0
 */
public class Plot_EVA2 extends FunctionArea {

    /**
     * Generated serial version identifier.
     */
    private static final long serialVersionUID = 176134486775218455L;

    /**
     * 
     */
    public Plot_EVA2() {
	super();
	setAppendIndexInLegend(false);
    }

    /**
     * @param xname
     * @param yname
     */
    public Plot_EVA2(String xname, String yname) {
	super(xname, yname);
	setAppendIndexInLegend(false);
    }

    /**
     * Plots a matrix either by displaying unconnected or connected points
     * depending on the connected parameter.
     * 
     * @param plotData
     *        The data to be plotted.
     * @param connected
     *        If true, all points will be connected, singular points are
     *        plotted for false.
     * @param showLegend
     *        If true, a legend will be displayed in the plot.
     * @param showGrid
     *        If true, a grid will be added to the plot.
     * @param plotColors
     *        Colors to be used to plot the columns. This array must also be
     *        of the same length as all columns in the plotData. Null values
     *        mean that this column is not to be plotted.
     * @param infos
     *        Information to be associated with each column in the plot.
     *        This array must have the same length as the number of columns
     *        in the plotData field.
     */
    public void plot(MultiTable plotData, boolean connected,
	boolean showLegend, boolean showGrid, Color[] plotColors, String[] infos) {
	int i, j, graphLabel;
	for (i = 1; i < plotData.getColumnCount(); i++) {
	    Column column = plotData.getColumn(i);
	    if (plotColors[i - 1] != null) {
		graphLabel = connected ? i : Integer.MAX_VALUE
			- plotData.getColumnCount() + i;
		// plot.clearGraph(graphLabel);
		setGraphColor(graphLabel, plotColors[i - 1]);
		setInfoString(graphLabel, infos[i - 1], 1);
		for (j = 0; j < column.getRowCount(); j++) {
		    if (connected) {
			setConnectedPoint(plotData.getTimePoint(j), column
				.getValue(j), graphLabel);
		    } else {
			setUnconnectedPoint(plotData.getTimePoint(j), column
				.getValue(j), graphLabel);
		    }
		}
	    }
	}
	setGridVisible(showGrid);
	setShowLegend(showLegend);
	if (showLegend) {
	    updateLegend();
	}
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
	Rectangle area = getBounds();
	area.setLocation(getLocationOnScreen());
	BufferedImage bufferedImage = (new Robot()).createScreenCapture(area);
	File file = GUITools.saveFileDialog(this, saveDir, false, false,
	    JFileChooser.FILES_ONLY, SBFileFilter.createPNGFileFilter(),
	    SBFileFilter.createJPEGFileFilter());
	if (file != null) {
	    saveDir = file.getParent();
	    if (SBFileFilter.isPNGFile(file)) {
		ImageIO.write(bufferedImage, "png", file);
	    } else if (SBFileFilter.isJPEGFile(file)) {
		FileImageOutputStream out = new FileImageOutputStream(file);
		ImageWriter encoder = (ImageWriter) ImageIO
			.getImageWritersByFormatName("JPEG").next();
		JPEGImageWriteParam param = new JPEGImageWriteParam(null);
		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		param.setCompressionQuality(compression);
		encoder.setOutput(out);
		encoder.write((IIOMetadata) null, new IIOImage(bufferedImage,
		    null, null), param);
		out.close();
	    }
	}
	return saveDir;
    }

    /**
     * @param data
     * @param connected
     * @param plotColors
     * @param infos
     */
    public void plot(MultiTable data, boolean connected,
	Color[] plotColors, String[] infos) {
	plot(data, connected, isShowLegend(), isShowGrid(), plotColors, infos);
    }

}
