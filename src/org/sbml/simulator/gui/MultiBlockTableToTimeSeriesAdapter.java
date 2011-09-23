package org.sbml.simulator.gui;

import org.jfree.data.xy.AbstractXYDataset;
import org.sbml.simulator.math.odes.MultiBlockTable;

public class MultiBlockTableToTimeSeriesAdapter extends AbstractXYDataset {

	private MultiBlockTable table;

	public MultiBlockTableToTimeSeriesAdapter(MultiBlockTable table) {
		this.table = table;
	}

	@Override
	public int getItemCount(int series) {
		return table.getColumn(series+1).getRowCount();
	}

	@Override
	public Number getX(int series, int item) {
		return table.getTimePoint(item);
	}

	@Override
	public Number getY(int series, int item) {
		return table.getValueAt(item, series+1);
	}

	@Override
	public int getSeriesCount() {
		return table.getColumnCount()-1;
	}

	@Override
	public Comparable getSeriesKey(int series) {
		return table.getColumnName(series+1);
	}
	
}
