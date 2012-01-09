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

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.sbml.simulator.gui.plot.BoxPlotDataset;
import org.sbml.simulator.gui.plot.Plot;
import org.sbml.simulator.gui.plot.XYDatasetAdapter;
import org.sbml.simulator.io.CSVDataImporter;
import org.simulator.math.odes.MultiTable;

import de.zbit.gui.ColorPalette;


/**
 * A test function for plotting.
 * 
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class PlotTest {
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		CSVDataImporter importer = new CSVDataImporter();
		MultiTable tables[] = new MultiTable[args.length];
		for (int i = 0; i < args.length; i++) {
			tables[i] = importer.convert(args[i]);
		}
		new PlotTest(tables);
	}

	/**
	 * 
	 * @param tables
	 */
	public PlotTest(MultiTable... tables) {
		Plot plot = new Plot();
//		XYDataSetCollection collection = new XYDataSetCollection();
//		for (MultiTable table : tables) {
//			collection.addSeries(new XYDatasetAdapter(table));
//		}
		
		BoxPlotDataset dataset = new BoxPlotDataset(tables);
		
//		plot.plot(collection, true, ColorPalette.palette(tables[0].getColumnCount() - 1), null);
		plot.plot(dataset, false, ColorPalette.palette(tables[0].getColumnCount() - 1), null);
		show(plot);
		
//		show(new ChartPanel(ChartFactory.createBoxAndWhiskerChart("Test", "time", "value", dataset, true)));
	}

	/**
	 * Shows the plot in a dialog window.
	 * 
	 * @param plot
	 */
	private void show(ChartPanel plot) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(plot, BorderLayout.CENTER);
		panel.setBorder(BorderFactory.createLoweredBevelBorder());
		JOptionPane.showMessageDialog(null, panel);
	}
	
}
