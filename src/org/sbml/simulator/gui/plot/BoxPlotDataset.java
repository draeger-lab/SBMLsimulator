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
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class BoxPlotDataset extends AbstractXYDataset implements BoxAndWhiskerXYDataset {
	
	/**
	 * Generated serialization identifier.
	 */
	private static final long serialVersionUID = 8754435876684018316L;
	
	/**
	 * The first key is the id of a data series. The second {@link SortedMap}
	 * stores data {@link Item}s for each time point. So, the second key is the
	 * time. In this way, this data structure represents a kind of a matrix with
	 * additional information, but the matrix doesn't have to be a square
	 * structure, i.e., it is possible to have missing data points for some series.
	 */
	private SortedMap<String, SortedMap<Double, Item>> data;
	
	/**
	 * 
	 * @author Andreas Dr&auml;ger
	 * @version $Rev$
	 * @since 1.0
	 */
	private class Item {
		List<Double> values;
		BoxAndWhiskerItem item;
		
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
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			if (item != null) { return String.format(
				"[mean=%.3f, median=%.3f, q1=%.3f, q3=%.3f]", item.getMean(),
				item.getMedian(), item.getQ1(), item.getQ3()); }
			return values.toString();
		}
		
		/**
		 * 
		 * @return
		 */
		public BoxAndWhiskerItem getItem() {
			return item;
		}
		
	}
	
	public BoxPlotDataset() {
		super();
		data = new TreeMap<String, SortedMap<Double,Item>>();
	}
	
	/**
	 * 
	 * @param tables
	 */
	public BoxPlotDataset(MultiTable... tables) {
		this();
		if ((tables != null) && (tables.length > 0)) {
			for (MultiTable table : tables) {
				add(table);
			}
		}
	}
	
	/**
	 * 
	 * @param tables
	 */
	public BoxPlotDataset(List<MultiTable> tables) {
		this();
		if ((tables != null) && (tables.size() > 0)) {
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
		SortedMap<Double, Item> series;
		Double time;
		for (int i = 1; i < table.getColumnCount(); i++) {
			id = table.getColumnIdentifier(i);
			if (!data.containsKey(id)) {
				series = new TreeMap<Double, Item>();
				data.put(id, series);
			} else {
				series = data.get(id);
			}
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
	}
	
	/* (non-Javadoc)
	 * @see org.jfree.data.xy.XYDataset#getItemCount(int)
	 */
	public int getItemCount(int series) {
		return data.get(getSeriesKey(series)).size();
	}
	
	/* (non-Javadoc)
	 * @see org.jfree.data.xy.XYDataset#getX(int, int)
	 */
	public Number getX(int series, int item) {
		return find(data.get(getSeriesKey(series)), item);
	}
	
	/**
	 * 
	 * @param series
	 * @param item
	 * @return
	 */
	private Double find(SortedMap<Double, Item> series, int item) {
		int i = 0;
		for (Double key : series.keySet()) {
			if (i == item) {
				return key;
			}
			i++;
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.jfree.data.xy.XYDataset#getY(int, int)
	 */
	public Number getY(int series, int item) {
		SortedMap<Double, Item> s = data.get(getSeriesKey(series));
		if (s != null) {
			Double time = find(s, item);
			if (time != null) {
				Item i = s.get(time);
				return i.getItem() != null ? i.getItem().getMean() : null;
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.jfree.data.general.SeriesDataset#getSeriesCount()
	 */
	public int getSeriesCount() {
		return data.size();
	}
	
	/* (non-Javadoc)
	 * @see org.jfree.data.general.SeriesDataset#getSeriesKey(int)
	 */
	public String getSeriesKey(int series) {
		int i = 0;
		for (String key : data.keySet()) {
			if (i == series) {
				return key;
			}
			i++;
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return data.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getFaroutCoefficient()
	 */
	public double getFaroutCoefficient() {
		return 2d;
	}
	
	/* (non-Javadoc)
	 * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getMaxOutlier(int, int)
	 */
	public Number getMaxOutlier(int series, int item) {
		Item i = getItem(series, item);
		if ((i != null) && (i.getItem() != null)) {
			return i.getItem().getMaxOutlier();
		}
		return null;
	}
	
	/**
	 * 
	 * @param series
	 * @param item
	 * @return
	 */
	public Item getItem(int series, int item) {
		String key = getSeriesKey(series);
		if (key != null) {
			SortedMap<Double, Item> s = data.get(key);
			if (s != null) {
				Double time = find(s, item);
				if (time != null) { return s.get(time); }
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getMaxRegularValue(int, int)
	 */
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
	public double getOutlierCoefficient() {
		return 1.5d;
	}
	
	/* (non-Javadoc)
	 * @see org.jfree.data.statistics.BoxAndWhiskerXYDataset#getOutliers(int, int)
	 */
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
	public Number getQ3Value(int series, int item) {
		Item i = getItem(series, item);
		if ((i != null) && (i.getItem() != null)) {
			return i.getItem().getQ3();
		}
		return null;
	}
	
}
