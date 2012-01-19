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

import org.jfree.data.xy.AbstractXYDataset;
import org.simulator.math.odes.MultiTable;

/**
 * A wrapper for a {@link MultiTable} to be plotted easily.
 * 
 * @author Max Zwie&szlig;ele
 * @author Philip Stevens
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class XYDatasetAdapter extends AbstractXYDataset implements MetaDataset {

	/**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = -3091202204421001475L;
  /**
   * 
   */
  private MultiTable table;

  /**
   * 
   * @param table
   */
	public XYDatasetAdapter(MultiTable table) {
		super();
		this.table = table;
	}

	/* (non-Javadoc)
	 * @see org.jfree.data.general.AbstractDataset#clone()
	 */
	@Override
	public XYDatasetAdapter clone() throws CloneNotSupportedException {
		return new XYDatasetAdapter(table);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// Check if the given object is a pointer to precisely the same object:
		if (super.equals(obj)) {
			return true;
		}
		// Check if the given object is of identical class and not null: 
		if ((obj == null) || (!getClass().equals(obj.getClass()))) {
			return false;
		}
		// Check all child nodes recursively:
		if (obj instanceof XYDatasetAdapter) {
			XYDatasetAdapter data = (XYDatasetAdapter) obj;
			return data.table.equals(table);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.jfree.data.xy.XYDataset#getItemCount(int)
	 */
	public int getItemCount(int series) {
		return table.getColumn(series + 1).getRowCount();
	}

	/* (non-Javadoc)
	 * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesCount()
	 */
	public int getSeriesCount() {
		return table.getColumnCount() - 1;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.gui.plot.MetaDataset#getId(int)
	 */
	public String getSeriesIdentifier(int series) {
		return table.getColumnIdentifier(series + 1);
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.gui.plot.MetaDataset#getSeriesIndex(java.lang.String)
	 */
	public int getSeriesIndex(String identifier) {
		return table.getColumnIndex(identifier) - 1;
	}

	/* (non-Javadoc)
	 * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesKey(int)
	 */
	public Comparable<String> getSeriesKey(int series) {
		return table.getColumnName(series + 1);
	}

	/* (non-Javadoc)
	 * @see org.jfree.data.xy.XYDataset#getX(int, int)
	 */
	public Number getX(int series, int item) {
		return table.getTimePoint(item);
	}

	/* (non-Javadoc)
	 * @see org.jfree.data.xy.XYDataset#getY(int, int)
	 */
	public Number getY(int series, int item) {
		return table.getValueAt(item, series + 1);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 769;
		int hashCode = getClass().getName().hashCode();
		hashCode += prime * table.hashCode();
		return hashCode;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return table.toString();
	}

}
