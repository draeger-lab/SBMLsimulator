/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2016 by the University of Tuebingen, Germany.
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

import java.text.MessageFormat;
import java.util.ResourceBundle;

import org.jfree.data.xy.XYDataset;

import de.zbit.util.ResourceManager;

/**
 * This class is responsible for the display of tooltips inside of a box plot.
 * The tooltips inform about mean, median, min, max, as well as the two
 * quartils Q1 and Q3.
 * 
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class BoxAndWhiskerXYToolTipGenerator extends
org.jfree.chart.labels.BoxAndWhiskerXYToolTipGenerator {

  /**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = -8559133032127605483L;

  /**
   * 
   */
  private String formatString;

  /**
   * 
   */
  public BoxAndWhiskerXYToolTipGenerator() {
    super();
    ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");
    formatString = String.format(
      "{0}: {1} %s: {2} %s: {3} %s: {4} %s: {5} %s: {6} %s: {7}",
      bundle.getString("MEAN"), bundle.getString("MEDIAN"),
      bundle.getString("MIN"), bundle.getString("MAX"), bundle.getString("Q1"),
      bundle.getString("Q3"));
  }

  /* (non-Javadoc)
   * @see org.jfree.chart.labels.AbstractXYItemLabelGenerator#generateLabelString(org.jfree.data.xy.XYDataset, int, int)
   */
  @Override
  public String generateLabelString(XYDataset dataset, int series, int item) {
    return MessageFormat.format(formatString, createItemArray(dataset, series, item));
  }

}
