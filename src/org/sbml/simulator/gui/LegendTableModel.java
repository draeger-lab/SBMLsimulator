/**
 * 
 */
package org.sbml.simulator.gui;

import java.awt.Color;
import java.util.Hashtable;
import java.util.NoSuchElementException;

import javax.swing.table.AbstractTableModel;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.util.ValuePair;

/**
 * @author Andreas Dr&auml;ger
 * @since 1.4
 * @date 2010-04-07
 * 
 */
public class LegendTableModel extends AbstractTableModel {

	/**
	 * Column indices for the content
	 */
	private static final int boolCol = 0, colorCol = 1, nsbCol = 2;

	/**
	 * 
	 * @return
	 */
	public static int getBooleanColumn() {
		return boolCol;
	}

	/**
	 * 
	 * @return
	 */
	public static int getColorColumn() {
		return colorCol;
	}

	/**
	 * 
	 * @return
	 */
	public static int getNamedSBaseColumn() {
		return nsbCol;
	}

	/**
	 * 
	 * @param index
	 * @return
	 */
	public static Color indexToColor(int index) {
		switch (index % 10) {
		case 0:
			return Color.black;
		case 1:
			return Color.red;
		case 2:
			return Color.blue;
		case 3:
			return Color.pink;
		case 4:
			return Color.green;
		case 5:
			return Color.gray;
		case 6:
			return Color.magenta;
		case 7:
			return Color.cyan;
		case 8:
			return Color.orange;
		case 9:
			return Color.darkGray;
		}
		return Color.black;
	}

	/**
	 * So save run time memorize the last queried key of the hash.
	 */
	private ValuePair<String, Integer> lastQueried;

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 7360401460080111135L;
	/**
	 * A colored button for each model component and
	 */
	private Object[][] data;

	/**
	 * A mapping between the ids in the table and the corresponding row.
	 */
	private Hashtable<String, Integer> id2Row;

	/**
	 * 
	 */
	public LegendTableModel() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 * @param model
	 */
	public LegendTableModel(Model model) {
		this(model, false);
	}

	/**
	 * 
	 * @param model
	 * @param includeReactions
	 */
	public LegendTableModel(Model model, boolean includeReactions) {
		int dim = model.getNumCompartments() + model.getNumSpecies()
				+ model.getNumParameters();
		if (includeReactions) {
			dim += model.getNumReactions();
		}
		id2Row = new Hashtable<String, Integer>();
		lastQueried = null;
		data = new Object[dim][3];
		NamedSBase sb;
		int i, j;
		for (i = 0; i < model.getNumCompartments(); i++) {
			sb = model.getCompartment(i);
			data[i][boolCol] = Boolean.TRUE;
			data[i][colorCol] = indexToColor(i);
			data[i][nsbCol] = sb;
			id2Row.put(sb.getId(), Integer.valueOf(i));
		}
		j = model.getNumCompartments();
		for (i = 0; i < model.getNumSpecies(); i++) {
			sb = model.getSpecies(i);
			data[i + j][boolCol] = Boolean.TRUE;
			data[i + j][colorCol] = indexToColor(i + j);
			data[i + j][nsbCol] = sb;
			id2Row.put(sb.getId(), Integer.valueOf(i + j));
		}
		j = model.getNumCompartments() + model.getNumSpecies();
		for (i = 0; i < model.getNumParameters(); i++) {
			sb = model.getParameter(i);
			data[i + j][boolCol] = Boolean.TRUE;
			data[i + j][colorCol] = indexToColor(i + j);
			data[i + j][nsbCol] = sb;
			id2Row.put(sb.getId(), Integer.valueOf(i + j));
		}
		if (includeReactions) {
			j = model.getNumSymbols();
			for (i = 0; i < model.getNumReactions(); i++) {
				sb = model.getReaction(i);
				data[i + j][boolCol] = Boolean.TRUE;
				data[i + j][colorCol] = indexToColor(i + j);
				data[i + j][nsbCol] = sb;
				id2Row.put(sb.getId(), Integer.valueOf(i + j));
			}
		}
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public Color getColorFor(String id) {
		int index = getRowFor(id);
		if (index >= 0) {
			return (Color) getValueAt(index, colorCol);
		}
		throw new NoSuchElementException(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
	 */
	@Override
	public Class<?> getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return data.length > 0 ? data[0].length : 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(int column) {
		if (column == boolCol)
			return "Plot";
		if (column == colorCol)
			return "Color";
		if (column == nsbCol)
			return "Symbol";
		throw new IndexOutOfBoundsException("Only " + getColumnCount()
				+ " columns, no column " + column);
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public String getNameFor(String id) {
		int index = getRowFor(id);
		if (index >= 0) {
			return getValueAt(index, nsbCol).toString();
		}
		throw new NoSuchElementException(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	public int getRowCount() {
		return data.length;
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public int getRowFor(String id) {
		int index = -1;
		if (lastQueried == null) {
			lastQueried = new ValuePair<String, Integer>(id, Integer
					.valueOf(index));
		} else if (id.equals(lastQueried.getA())) {
			return lastQueried.getB().intValue();
		}
		if (id2Row.containsKey(id)) {
			index = id2Row.get(id).intValue();
			lastQueried.setA(id);
			lastQueried.setB(Integer.valueOf(index));
		}
		return index;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == nsbCol) {
			NamedSBase nsb = (NamedSBase) data[rowIndex][columnIndex];
			return nsb.isSetName() ? nsb.getName() : nsb.getId();
		}
		return data[rowIndex][columnIndex];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
	 */
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if ((columnIndex < getColumnCount()) && (rowIndex < getRowCount())) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public boolean isSelected(String id) {
		int index = getRowFor(id);
		if (index >= 0) {
			return ((Boolean) getValueAt(index, boolCol)).booleanValue();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object,
	 * int, int)
	 */
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		data[rowIndex][columnIndex] = aValue;
		fireTableCellUpdated(rowIndex, columnIndex);
	}
}
