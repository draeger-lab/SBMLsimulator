/*
 * $Id$
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
package org.sbml.simulator.gui.plot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jfree.data.statistics.BoxAndWhiskerCalculator;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.BoxAndWhiskerXYDataset;
import org.jfree.data.xy.AbstractXYDataset;
import org.simulator.math.odes.MultiTable;

/**
 * A wrapper around a {@link MultiTable} that can be used in a plot to display
 * the data content in form of box plots.
 * 
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class BoxPlotDataset extends AbstractXYDataset implements
BoxAndWhiskerXYDataset, MetaDataset {

  /**
   * 
   * @author Andreas Dr&auml;ger
   * @version $Rev$
   * @since 1.0
   */
  private class Item {
    BoxAndWhiskerItem item;
    List<Double> values;

    /**
     * 
     */
    public Item() {
      values = new LinkedList<Double>();
      item = null;
    }

    /**
     * 
     * @param value
     * @return
     */
    public boolean add(double value) {
      if (values.add(Double.valueOf(value))) {
        item = BoxAndWhiskerCalculator.calculateBoxAndWhiskerStatistics(values);
        return true;
      }
      return false;
    }

    /**
     * 
     * @return
     */
    public BoxAndWhiskerItem getItem() {
      return item;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      if (item != null) {
        return String.format(
          "[mean=%.3f, median=%.3f, q1=%.3f, q3=%.3f]", item.getMean(),
          item.getMedian(), item.getQ1(), item.getQ3());
      }
      return values.toString();
    }

  }

  /**
   * 
   * @author Andreas Dr&auml;ger
   * @version $Rev$
   * @since 1.0
   */
  private class Series extends TreeMap<Double, Item> implements Comparable<Series> {

    /**
     * Generated serial version identifier.
     */
    private static final long serialVersionUID = -6990448446534140941L;

    /**
     * 
     */
    private String id, name;

    /**
     * 
     */
    public Series() {
      super();
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Series s) {
      return getId().compareTo(s.getId());
    }

    /**
     * 
     * @param listOfSeries
     * @param item
     * @return
     */
    public Double find(int item) {
      int i = 0;
      for (Double key : keySet()) {
        if (i == item) {
          return key;
        }
        i++;
      }
      return null;
    }

    /**
     * 
     * @param item
     * @return
     */
    public Item get(int item) {
      Double time = getTimeAt(item);
      if (time != null) {
        return get(getTimeAt(item));
      }
      return null;
    }

    /**
     * @return the id
     */
    public String getId() {
      return id;
    }

    /**
     * @return the name, or if it is not defined, the id.
     * @see #getId()
     */
    public String getName() {
      if (name == null) {
        return getId();
      }
      return name;
    }

    /**
     * 
     * @param item
     * @return
     */
    public Double getTimeAt(int item) {
      Double time = find(item);
      if (time != null) {
        return time;
      }
      return null;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
      this.id = id;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
      this.name = name;
    }

  }

  /**
   * Generated serialization identifier.
   */
  private static final long serialVersionUID = 8754435876684018316L;

  /**
   * This stores all series including their {@link Item}s in a {@link SortedMap}
   * data structure for each time point. So, the second key is the time. In this
   * way, this data structure represents a kind of a matrix with additional
   * information, but the matrix doesn't have to be a square structure, i.e., it
   * is possible to have missing data points for some series.
   */
  private List<Series> listOfSeries;

  /**
   * Mapping between the identifier of a series and the series itself.
   */
  private SortedMap<String, Series> seriesMap;

  public BoxPlotDataset() {
    super();
    listOfSeries = new ArrayList<Series>();
    seriesMap = new TreeMap<String, Series>();
  }

  /**
   * 
   * @param tables
   */
  public BoxPlotDataset(Iterable<MultiTable> tables) {
    this();
    if (tables != null) {
      for (MultiTable table : tables) {
        add(table);
      }
    }
  }

  /**
   * 
   * @param table
   */
  public void add(MultiTable table) {
    double tp[] = table.getTimePoints(), value;
    String id;
    Item item;
    Series series;
    Double time;
    for (int i = 1; i < table.getColumnCount(); i++) {
      id = table.getColumnIdentifier(i);
      if (!seriesMap.containsKey(id)) {
        series = new Series();
        seriesMap.put(id, series);
        listOfSeries.add(series);
      } else {
        series = seriesMap.get(id);
      }
      series.setId(id);
      series.setName(table.getColumnName(i));
      for (int j = 0; j < tp.length; j++) {
        value = table.getValueAt(j, i);
        if (!Double.isNaN(value)) {
          time = Double.valueOf(tp[j]);
          if (!series.containsKey(time)) {
            item = new Item();
            series.put(time, item);
          } else {
            item = series.get(time);
          }
          item.add(value);
        }
      }
    }
    Collections.sort(listOfSeries);
  }

  /* (non-Javadoc)
   * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getFaroutCoefficient()
   */
  @Override
  public double getFaroutCoefficient() {
    return 2d;
  }

  /**
   * 
   * @param series
   * @param item
   * @return
   */
  public Item getItem(int series, int item) {
    return listOfSeries.get(series).get(item);
  }

  /* (non-Javadoc)
   * @see org.jfree.data.xy.XYDataset#getItemCount(int)
   */
  @Override
  public int getItemCount(int series) {
    return listOfSeries.get(series).size();
  }

  /* (non-Javadoc)
   * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getMaxOutlier(int, int)
   */
  @Override
  public Number getMaxOutlier(int series, int item) {
    Item i = getItem(series, item);
    if ((i != null) && (i.getItem() != null)) {
      return i.getItem().getMaxOutlier();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getMaxRegularValue(int, int)
   */
  @Override
  public Number getMaxRegularValue(int series, int item) {
    Item i = getItem(series, item);
    if ((i != null) && (i.getItem() != null)) {
      return i.getItem().getMaxRegularValue();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getMeanValue(int, int)
   */
  @Override
  public Number getMeanValue(int series, int item) {
    Item i = getItem(series, item);
    if ((i != null) && (i.getItem() != null)) {
      return i.getItem().getMean();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getMedianValue(int, int)
   */
  @Override
  public Number getMedianValue(int series, int item) {
    Item i = getItem(series, item);
    if ((i != null) && (i.getItem() != null)) {
      return i.getItem().getMedian();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getMinOutlier(int, int)
   */
  @Override
  public Number getMinOutlier(int series, int item) {
    Item i = getItem(series, item);
    if ((i != null) && (i.getItem() != null)) {
      return i.getItem().getMinOutlier();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getMinRegularValue(int, int)
   */
  @Override
  public Number getMinRegularValue(int series, int item) {
    Item i = getItem(series, item);
    if ((i != null) && (i.getItem() != null)) {
      return i.getItem().getMinRegularValue();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getOutlierCoefficient()
   */
  @Override
  public double getOutlierCoefficient() {
    return 1.5d;
  }

  /* (non-Javadoc)
   * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getOutliers(int, int)
   */
  @Override
  @SuppressWarnings("rawtypes")
  public List getOutliers(int series, int item) {
    Item i = getItem(series, item);
    if ((i != null) && (i.getItem() != null)) {
      return i.getItem().getOutliers();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getQ1Value(int, int)
   */
  @Override
  public Number getQ1Value(int series, int item) {
    Item i = getItem(series, item);
    if ((i != null) && (i.getItem() != null)) {
      return i.getItem().getQ1();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getQ3Value(int, int)
   */
  @Override
  public Number getQ3Value(int series, int item) {
    Item i = getItem(series, item);
    if ((i != null) && (i.getItem() != null)) {
      return i.getItem().getQ3();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.jfree.data.general.SeriesDataset#getSeriesCount()
   */
  @Override
  public int getSeriesCount() {
    return listOfSeries.size();
  }

  /**
   * 
   */
  @Override
  public String getSeriesIdentifier(int series) {
    return listOfSeries.get(series).getId();
  }

  /* (non-Javadoc)
   * @see org.sbml.simulator.gui.plot.MetaDataset#getSeriesIndex(java.lang.String)
   */
  @Override
  public int getSeriesIndex(String identifier) {
    if (!seriesMap.containsKey(identifier)) {
      return -1;
    }
    return Collections.binarySearch(listOfSeries, seriesMap.get(identifier));
  }

  /* (non-Javadoc)
   * @see org.jfree.data.general.SeriesDataset#getSeriesKey(int)
   */
  @Override
  public String getSeriesKey(int series) {
    return listOfSeries.get(series).getName();
  }

  /* (non-Javadoc)
   * @see org.jfree.data.xy.XYDataset#getX(int, int)
   */
  @Override
  public Number getX(int series, int item) {
    return listOfSeries.get(series).getTimeAt(item);
  }

  /* (non-Javadoc)
   * @see org.jfree.data.xy.XYDataset#getY(int, int)
   */
  @Override
  public Number getY(int series, int item) {
    Item i = listOfSeries.get(series).get(item);
    if (i != null) {
      return i.getItem() != null ? i.getItem().getMean() : null;
    }
    return null;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return listOfSeries.toString();
  }

}
