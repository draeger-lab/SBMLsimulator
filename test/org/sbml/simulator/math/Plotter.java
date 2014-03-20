/*
 * $Id:  Plotter.java 14:26:05 keller$
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
package org.sbml.simulator.math;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.simulator.math.odes.MultiTable;
import org.simulator.math.odes.MultiTable.Block.Column;

import de.zbit.gui.GUITools;
import eva2.gui.Plot;

/**
A runnable that generates a plot for a given simulation result and
 * experimental data that stores the plot in a file.
 * 
 * @author Andreas Dr&auml;ger
 * @author Roland Keller
 * @version $Rev$
 * @since 1.0
 */
public class Plotter implements Runnable {

  /**
   * 
   */
  private MultiTable solution, data;
  /**
   * 
   */
  private String outFileName;


  public Plotter(MultiTable solution, MultiTable data, String outFileName) {
    this.solution = solution;
    this.data = data;
    this.outFileName = outFileName;
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    Plot plot = new Plot("Simulation", "time", "value");
    int i, j;
    Column col;

    String identifiers[] = data.getBlock(0).getIdentifiers();


    for (i = 0; i < identifiers.length; i++) {
      col = solution.getColumn(identifiers[i]);
      for (j = 0; j < data.getRowCount(); j++) {
        plot.setConnectedPoint(solution.getTimePoint(j),
          col.getValue(j), i);
        plot.setUnconnectedPoint(solution.getTimePoint(j),
          data.getValueAt(j, i+1), 90 + i);


      }

    }

    /*
		for (i = 0; i < solution.getRowCount(); i++) {
			for (j = 0; j < solution.getColumnCount(); j++) {
				plot.setConnectedPoint(solution.getTimePoint(i),
						solution.getValueAt(i, j), j);
			}
		}
		for (i = 0; i < data.getRowCount(); i++) {
			for (j = 0; j < data.getColumnCount(); j++) {
				plot.setUnconnectedPoint(solution.getTimePoint(i),
						data.getValueAt(i, j), 90 + j);
			}
		}
     */


    // save graph as jpg
    BufferedImage img = new BufferedImage(plot
      .getFunctionArea().getWidth(), plot
      .getFunctionArea().getHeight(),
      BufferedImage.TYPE_INT_RGB);
    plot.getFunctionArea().paint(img.createGraphics());
    try {
      ImageIO.write(img, "jpg", new File(outFileName));
    } catch (IOException exc) {
      GUITools.showErrorMessage(null, exc);
    }
    //		try {
    //			wait();
    //		} catch (InterruptedException exc) {
    //			GUITools.showErrorMessage(null, exc);
    //		}
    //plot.dispose();
  }

}
