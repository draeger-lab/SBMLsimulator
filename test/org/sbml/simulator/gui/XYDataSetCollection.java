/*
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
package org.sbml.simulator.gui;

import java.util.LinkedList;
import java.util.List;

import org.jfree.data.xy.AbstractIntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.sbml.simulator.gui.plot.XYDatasetAdapter;
import org.simulator.math.odes.MultiTable;

/**
 * A test class for plot data collections.
 * 
 * @author Andreas Dr&auml;ger
 * @since 1.0
 */
public class XYDataSetCollection extends AbstractIntervalXYDataset {

  /**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = 1834005371697530079L;

  /**
   * 
   * @param seriesKey
   */
  public XYDataSetCollection() {
    super();
    listOfDatasets = new LinkedList<XYDataset>();
  }

  /**
   * 
   * @param tables
   */
  public XYDataSetCollection(MultiTable... tables) {
    this();
    for (MultiTable table : tables) {
      addSeries(new XYDatasetAdapter(table));
    }
  }

  /**
   * 
   */
  private List<XYDataset> listOfDatasets;


  /**
   * 
   * @param dataSet
   */
  public void addSeries(XYDataset dataSet) {
    if (dataSet == null) {
      throw new IllegalArgumentException("Null 'series' argument.");
    }
    listOfDatasets.add(dataSet);
  }

  public void removeSeries(int index) {
    listOfDatasets.remove(index);
  }

  /* (non-Javadoc)
   * @see org.jfree.data.xy.IntervalXYDataset#getEndX(int, int)
   */
  @Override
  public Number getEndX(int series, int item) {
    double maxX = Double.NEGATIVE_INFINITY;
    double curr;
    for (XYDataset dataset : listOfDatasets) {
      curr = dataset.getXValue(series, item);
      if (curr > maxX) {
        maxX = curr;
      }
    }
    return Double.valueOf(maxX);
  }

  /* (non-Javadoc)
   * @see org.jfree.data.xy.IntervalXYDataset#getEndY(int, int)
   */
  @Override
  public Number getEndY(int series, int item) {
    double maxY = Double.NEGATIVE_INFINITY;
    double curr;
    for (XYDataset dataset : listOfDatasets) {
      curr = dataset.getYValue(series, item);
      if (curr > maxY) {
        maxY = curr;
      }
    }
    return Double.valueOf(maxY);
  }

  /* (non-Javadoc)
   * @see org.jfree.data.xy.IntervalXYDataset#getStartX(int, int)
   */
  @Override
  public Number getStartX(int series, int item) {
    double minX = Double.POSITIVE_INFINITY;
    double curr;
    for (XYDataset dataset : listOfDatasets) {
      curr = dataset.getXValue(series, item);
      if (curr < minX) {
        minX = curr;
      }
    }
    return Double.valueOf(minX);
  }

  /* (non-Javadoc)
   * @see org.jfree.data.xy.IntervalXYDataset#getStartY(int, int)
   */
  @Override
  public Number getStartY(int series, int item) {
    double minY = Double.POSITIVE_INFINITY;
    double curr;
    for (XYDataset dataset : listOfDatasets) {
      curr = dataset.getYValue(series, item);
      if (curr < minY) {
        minY = curr;
      }
    }
    return Double.valueOf(minY);
  }

  /* (non-Javadoc)
   * @see org.jfree.data.xy.XYDataset#getItemCount(int)
   */
  @Override
  public int getItemCount(int series) {
    return listOfDatasets.get(0).getItemCount(series);
  }

  /* (non-Javadoc)
   * @see org.jfree.data.xy.XYDataset#getX(int, int)
   */
  @Override
  public Number getX(int series, int item) {
    return listOfDatasets.get(0).getX(series, item);
  }

  /* (non-Javadoc)
   * @see org.jfree.data.xy.XYDataset#getY(int, int)
   */
  @Override
  public Number getY(int series, int item) {
    double value = 0d;
    for (XYDataset dataset : listOfDatasets) {
      value += dataset.getYValue(series, item);
    }
    return Double.valueOf(value/listOfDatasets.size());
  }

  @Override
  public int getSeriesCount() {
    return listOfDatasets.get(0).getSeriesCount();
  }

  /* (non-Javadoc)
   * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesKey(int)
   */
  @Override
  public Comparable<?> getSeriesKey(int series) {
    return listOfDatasets.get(0).getSeriesKey(series);
  }

}
