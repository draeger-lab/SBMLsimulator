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
import org.sbml.simulator.math.odes.MultiBlockTable;

/**
 * 
 * @author Max Zwie&szlig;ele
 * @author Philip Stevens
 * @version $Rev$
 * @since 1.0
 */
public class MultiBlockTableToTimeSeriesAdapter extends AbstractXYDataset {

	/**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = -3091202204421001475L;
  /**
   * 
   */
  private MultiBlockTable table;

	public MultiBlockTableToTimeSeriesAdapter(MultiBlockTable table) {
		this.table = table;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jfree.data.xy.XYDataset#getItemCount(int)
	 */
	public int getItemCount(int series) {
		return table.getColumn(series+1).getRowCount();
	}

	/*
	 * (non-Javadoc)
	 * @see org.jfree.data.xy.XYDataset#getX(int, int)
	 */
	public Number getX(int series, int item) {
		return table.getTimePoint(item);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jfree.data.xy.XYDataset#getY(int, int)
	 */
	public Number getY(int series, int item) {
		return table.getValueAt(item, series+1);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesCount()
	 */
	public int getSeriesCount() {
		return table.getColumnCount()-1;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesKey(int)
	 */
	public Comparable<String> getSeriesKey(int series) {
		return table.getColumnName(series+1);
	}
	
}