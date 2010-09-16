/**
 * 
 */
package org.sbml.simulator.gui;

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

import org.sbml.simulator.math.odes.MultiBlockTable;
import org.sbml.simulator.math.odes.MultiBlockTable.Block.Column;

import de.zbit.gui.GUITools;
import de.zbit.io.SBFileFilter;
import eva2.gui.FunctionArea;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-08
 */
public class Plot extends FunctionArea {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 176134486775218455L;

	/**
	 * 
	 */
	public Plot() {
		super();
		setAppendIndexInLegend(false);
	}

	/**
	 * @param xname
	 * @param yname
	 */
	public Plot(String xname, String yname) {
		super(xname, yname);
		setAppendIndexInLegend(false);
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
	 * @param plotColumns
	 *            An array of the same length as columns in the data object
	 *            (excluding the x-axis). Each value in this array decides
	 *            whether or not to plot the associated column.
	 * @param plotColors
	 *            Colors to be used to plot the columns. This array must also be
	 *            of the same length as all columns in the plotData.
	 * @param infos
	 *            Information to be associated with each column in the plot.
	 *            This array must have the same length as the number of columns
	 *            in the plotData field.
	 */
	public void plot(MultiBlockTable plotData, boolean connected,
			boolean showLegend, boolean showGrid, boolean[] plotColumns,
			Color[] plotColors, String[] infos) {
		int i, j, graphLabel;
		for (i = 1; i < plotData.getColumnCount(); i++) {
			Column column = plotData.getColumn(i);
			if (plotColumns[i - 1]) {
				graphLabel = connected ? i : i + plotData.getColumnCount();
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
	}

	/**
	 * 
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
		JFileChooser fc = GUITools.createJFileChooser(saveDir, false, false,
				JFileChooser.FILES_ONLY, SBFileFilter.PNG_FILE_FILTER,
				SBFileFilter.JPEG_FILE_FILTER);
		if ((fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
				&& (!fc.getSelectedFile().exists() || (GUITools
						.overwriteExistingFile(this, fc.getSelectedFile())))) {
			saveDir = fc.getSelectedFile().getParent();
			File file = fc.getSelectedFile();
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

}
