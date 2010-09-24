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
	 * @author Andreas Dr&auml;ger
	 * @date 2010-09-20
	 */
	public enum Command {
		/**
		 * 
		 */
		LOG_SCALE,
		/**
		 * 
		 */
		SHOW_GRID,
		/**
		 * 
		 */
		SHOW_LEGEND,
		/**
		 * 
		 */
		SHOW_TOOL_TIPS;

		/**
		 * Returns a human-readable text for each {@link Command}.
		 * 
		 * @return
		 */
		public String getText() {
			switch (this) {
			case LOG_SCALE:
				return "Log scale";
			case SHOW_GRID:
				return "Grid";
			case SHOW_LEGEND:
				return "Legend";
			case SHOW_TOOL_TIPS:
				return "Tool tips";
			default:
				return null;
			}
		}

		/**
		 * Returns tool tip information for each command.
		 * 
		 * @return
		 */
		public String getToolTip() {
			switch (this) {
			case LOG_SCALE:
				return "Select this checkbox if the y-axis should be drawn in a logarithmic scale. This is, however, only possible if all values are greater than zero.";
			case SHOW_GRID:
				return "Decide whether or not to draw a grid in the plot area.";
			case SHOW_LEGEND:
				return "Add or remove a legend in the plot.";
			case SHOW_TOOL_TIPS:
				return "Let the plot display tool tips for each curve.";
			default:
				return null;
			}
		}
	}

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
		File file = GUITools.saveFileDialog(this, saveDir, false, false,
				JFileChooser.FILES_ONLY, SBFileFilter.PNG_FILE_FILTER,
				SBFileFilter.JPEG_FILE_FILTER);
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
	 * 
	 * @param data
	 * @param connected
	 * @param plotColors
	 * @param infos
	 */
	public void plot(MultiBlockTable data, boolean connected,
			Color[] plotColors, String[] infos) {
		plot(data, connected, isShowLegend(), isShowGrid(), plotColors, infos);
	}

}
