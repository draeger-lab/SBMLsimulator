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
package org.sbml.simulator.gui;

import java.awt.Color;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.swing.table.AbstractTableModel;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.NamedSBaseWithDerivedUnit;
import org.sbml.jsbml.util.compilers.HTMLFormula;

import de.zbit.gui.ColorPalette;
import de.zbit.util.StringUtil;
import de.zbit.util.ValuePair;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-04-07
 * @version $Rev$
 * @since 1.0
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
	 * @return
	 */
	public static int getBooleanColumn() {
		return boolCol;
	}

	/**
	 * @return
	 */
	public static int getColorColumn() {
		return colorCol;
	}

	/**
	 * @return
	 */
	public static int getNamedSBaseColumn() {
		return nsbCol;
	}

	/**
	 * @return
	 */
	public static int getUnitColumn() {
		return unitCol;
	}

	/**
	 * @param index
	 * @return
	 */
	public static Color indexToColor(int index) {
		switch (index % 27) {
		case 0:
			return ColorPalette.ANTHRACITE;
		case 1:
			return ColorPalette.SECOND_292;
		case 2:
			return ColorPalette.SECOND_131;
		case 3:
			return ColorPalette.SECOND_180;
		case 4:
			return ColorPalette.SECOND_3015;
		case 5:
			return ColorPalette.SECOND_364;
		case 6:
			return ColorPalette.SECOND_557;
		case 7:
			return ColorPalette.SECOND_653;
		case 8:
			return ColorPalette.SECOND_6880;
		case 9:
			return ColorPalette.SECOND_7490;
		case 10:
			return ColorPalette.SECOND_7505;
		case 11:
			return ColorPalette.SECOND_7508;
		case 12:
			return ColorPalette.SECOND_7530;
		case 13:
			return ColorPalette.GOLD;
		case 14:
			return ColorPalette.CAMINE_RED;
		case 15:
			return ColorPalette.CAMINE_RED_50_PERCENT;
		case 16:
			return ColorPalette.GOLD_50_PERCENT;
		case 17:
			return Color.BLACK;
		case 18:
			return Color.RED;
		case 19:
			return Color.BLUE;
		case 20:
			return Color.PINK;
		case 21:
			return Color.GREEN;
		case 22:
			return Color.GRAY;
		case 23:
			return Color.MAGENTA;
		case 24:
			return Color.CYAN;
		case 25:
			return Color.ORANGE;
		case 26:
			return Color.DARK_GRAY;
		}
		return Color.BLACK;
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
	 * @param model
	 */
	public LegendTableModel(Model model) {
		this();
		setModel(model);
	}

	/**
	 * @param model
	 * @param includeReactions
	 */
	public LegendTableModel(Model model, boolean includeReactions) {
		this();
		setModel(model, includeReactions);
	}

	/**
	 * @param nsb
	 * @param rowIndex
	 */
	private void fillData(NamedSBaseWithDerivedUnit nsb, int rowIndex) {
		data[rowIndex][boolCol] = Boolean.TRUE;
		data[rowIndex][colorCol] = indexToColor(rowIndex);
		data[rowIndex][nsbCol] = nsb;

		try {
			data[rowIndex][unitCol] = StringUtil.toHTML(HTMLFormula.toHTML(nsb
					.getDerivedUnitDefinition()));

		} catch (Exception e) {
			data[rowIndex][unitCol] = "N/A";
			// TODO make exception visible for the user
		}

		id2Row.put(nsb.getId(), Integer.valueOf(rowIndex));
	}

	/**
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
	 * @param rowIndex
	 * @return
	 */
	public String getId(int rowIndex) {
		return ((NamedSBase) data[rowIndex][getNamedSBaseColumn()]).getId();
	}

	/**
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
	 * @param id
	 * @return
	 */
	public int getRowFor(String id) {
		int index = -1;
		if (lastQueried == null) {
			lastQueried = new ValuePair<String, Integer>(id,
					Integer.valueOf(index));
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

		if (columnIndex == unitCol) {
			return false;
		}

		return true;
	}

	/**
	 * @param rowIndex
	 * @return
	 */
	public boolean isSelected(int rowIndex) {
		return ((Boolean) data[rowIndex][boolCol]).booleanValue();
	}

	/**
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
	 * @param model
	 */
	public void setModel(Model model) {
		setModel(model, false);
	}

	/**
	 * @param model
	 * @param includeReactions
	 */
	public void setModel(Model model, boolean includeReactions) {
		this.includeReactions = includeReactions;
		this.model = model;
		init();
	}

	/**
	 * Changes the selection status for the {@link NamedSBase} in the given row.
	 * 
	 * @param rowIndex
	 * @param selected
	 */
	public void setSelected(int rowIndex, boolean selected) {
		setValueAt(Boolean.valueOf(selected), rowIndex, getBooleanColumn());
	}

	/**
	 * Changes the selection status for the {@link NamedSBase} with the given
	 * identifier.
	 * 
	 * @param identifier
	 * @param selected
	 */
	public void setSelected(String identifier, boolean selected) {
		setSelected(getRowFor(identifier), selected);
	}

	/**
	 * Runs over the legend and sets all variables corresponding to the given
	 * identifiers as selected. All others will be unselected.
	 * 
	 * @param identifiers
	 *            The identifiers of the variables to be selected and to occur
	 *            in the plot.
	 */
	public void setSelectedVariables(String... identifiers) {
		for (int i = 0; i < getRowCount(); i++) {
			setValueAt(Boolean.valueOf(false), i, 0);
			for (String id : identifiers) {
				if (getValueAt(i, 2).toString().trim().equals(id.trim())) {
					setValueAt(Boolean.valueOf(true), i, 0);
					break;
				}
			}
		}
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
