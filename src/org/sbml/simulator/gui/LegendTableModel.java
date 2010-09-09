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
import org.sbml.jsbml.NamedSBaseWithDerivedUnit;
import org.sbml.jsbml.util.ValuePair;
import org.sbml.squeezer.util.HTMLFormula;

import de.zbit.gui.GUITools;

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
	private static final int boolCol = 0, colorCol = 1, nsbCol = 2,
			unitCol = 3;

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 7360401460080111135L;

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
	 * @return
	 */
	public static int getUnitColumn() {
		return unitCol;
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
	 * A colored button for each model component and
	 */
	private Object[][] data;
	/**
	 * A mapping between the ids in the table and the corresponding row.
	 */
	private Hashtable<String, Integer> id2Row;

	/**
	 * Switch of whether or not to include reactions in the legend.
	 */
	private boolean includeReactions;

	/**
	 * So save run time memorize the last queried key of the hash.
	 */
	private ValuePair<String, Integer> lastQueried;

	/**
	 * Pointer to the model
	 */
	private Model model;

	/**
	 * 
	 */
	public LegendTableModel() {
	}

	/**
	 * 
	 * @param model
	 */
	public LegendTableModel(Model model) {
		this();
		setModel(model);
	}

	/**
	 * 
	 * @param model
	 * @param includeReactions
	 */
	public LegendTableModel(Model model, boolean includeReactions) {
		this();
		setModel(model, includeReactions);
	}

	/**
	 * 
	 * @param nsb
	 * @param rowIndex
	 */
	private void fillData(NamedSBaseWithDerivedUnit nsb, int rowIndex) {
		data[rowIndex][boolCol] = Boolean.TRUE;
		data[rowIndex][colorCol] = indexToColor(rowIndex);
		data[rowIndex][nsbCol] = nsb;
		data[rowIndex][unitCol] = GUITools.toHTML(HTMLFormula.toHTML(nsb
				.getDerivedUnitDefinition()));
		id2Row.put(nsb.getId(), Integer.valueOf(rowIndex));
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
		switch (column) {
		case boolCol:
			return "Plot";
		case colorCol:
			return "Color";
		case nsbCol:
			return "Component";
		case unitCol:
			return "Unit";
		default:
			break;
		}
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

	/**
	 * 
	 */
	private void init() {
		int dim = model.getNumCompartments() + model.getNumSpecies()
				+ model.getNumParameters();
		if (includeReactions) {
			dim += model.getNumReactions();
		}
		id2Row = new Hashtable<String, Integer>();
		lastQueried = null;
		data = new Object[dim][4];
		int i, j;
		for (i = 0; i < model.getNumCompartments(); i++) {
			fillData(model.getCompartment(i), i);
		}
		j = model.getNumCompartments();
		for (i = 0; i < model.getNumSpecies(); i++) {
			fillData(model.getSpecies(i), i + j);
		}
		j = model.getNumCompartments() + model.getNumSpecies();
		for (i = 0; i < model.getNumParameters(); i++) {
			fillData(model.getParameter(i), i + j);
		}
		if (includeReactions) {
			j = model.getNumSymbols();
			for (i = 0; i < model.getNumReactions(); i++) {
				fillData(model.getReaction(i), i + j);
			}
		}
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

	/**
	 * 
	 * @param model
	 */
	public void setModel(Model model) {
		setModel(model, false);
	}

	/**
	 * 
	 * @param model
	 * @param includeReactions
	 */
	public void setModel(Model model, boolean includeReactions) {
		this.includeReactions = includeReactions;
		this.model = model;
		init();
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
