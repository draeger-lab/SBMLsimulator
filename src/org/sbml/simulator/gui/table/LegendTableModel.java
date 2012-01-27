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
package org.sbml.simulator.gui.table;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.NamedSBaseWithDerivedUnit;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;

import de.zbit.gui.ColorPalette;
import de.zbit.sbml.gui.UnitDerivationWorker;
import de.zbit.util.ResourceManager;
import de.zbit.util.ValuePair;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-04-07
 * @version $Rev$
 * @since 1.0
 */
public class LegendTableModel extends AbstractTableModel implements PropertyChangeListener {
	
	/**
	 * Column indices for the content
	 */
	private static final int boolCol = 0, colorCol = 1, nsbCol = 2,
			unitCol = 3;

	/**
	 * 
	 */
	private static final transient ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");

	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(LegendTableModel.class.getName());
	
  /**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 7360401460080111135L;

	/**
	 * @return
	 */
	public static int getColumnColor() {
		return colorCol;
	}

	/**
	 * @return
	 */
	public static int getColumnPlot() {
		return boolCol;
	}

	/**
	 * @return
	 */
	public static int getColumnUnit() {
		return unitCol;
	}

	/**
	 * @return
	 */
	public static int getNamedSBaseColumn() {
		return nsbCol;
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
   * The number of selected items in the table.
   */
  private int selectedCount;

	/**
	 * 
	 */
	public LegendTableModel() {
	  super();
	  selectedCount = 0;
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
	public LegendTableModel(Model model, boolean includeReactions, TableModelListener listener) {
		this();
		addTableModelListener(listener);
		setModel(model, includeReactions);
	}

	/**
	 * @param nsb
	 * @param rowIndex
	 */
  private void fillData(NamedSBaseWithDerivedUnit nsb, int rowIndex,
    boolean boolcol) {
    data[rowIndex][boolCol] = Boolean.valueOf(boolcol);
    if (boolcol) {
      selectedCount++;
    }
    data[rowIndex][colorCol] = ColorPalette.indexToColor(rowIndex);
    data[rowIndex][nsbCol] = nsb;
    id2Row.put(nsb.getId(), Integer.valueOf(rowIndex));
  }

	/**
   * Determines the currently specified {@link Color} for the {@link NamedSBase}
   * with the given <code>id</code>.
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

	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
	 */
	@Override
	public Class<?> getColumnClass(int c) {
		if (c == nsbCol) {
			return NamedSBaseWithDerivedUnit.class;
		}
		return getValueAt(0, c).getClass();
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return data.length > 0 ? data[0].length : 0;
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(int column) {
		switch (column) {
		case boolCol:
			return bundle.getString("PLOT_COLUMN");
		case colorCol:
			return bundle.getString("COLOR_COLUMN");
		case nsbCol:
			return bundle.getString("COMPONENT_COLUMN");
		case unitCol:
			return bundle.getString("UNIT_COLUMN");
		default:
			break;
		}
    throw new IndexOutOfBoundsException(String.format(
      bundle.getString("UNKOWN_CLUMN_EXC"), getColumnCount(), column));
	}

	/**
	 * @param rowIndex
	 * @return
	 */
	public String getId(int rowIndex) {
		return ((NamedSBase) data[rowIndex][getNamedSBaseColumn()]).getId();
	}

	/**
	 * Determines the name if there is any for the {@link NamedSBase} belonging to
   * the given identifier.
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

	/* (non-Javadoc)
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
		Integer index = Integer.valueOf(-1);
		if (lastQueried == null) {
			lastQueried = new ValuePair<String, Integer>(id, index);
		} else if (id.equals(lastQueried.getA())) {
			return lastQueried.getB().intValue();
		}
		if (id2Row.containsKey(id)) {
			index = id2Row.get(id);
			lastQueried.setA(id);
			lastQueried.setB(index);
		}
		return index.intValue();
	}

	/**
	 * 
	 * @param rowIndex
	 * @return
	 */
	public NamedSBaseWithDerivedUnit getSBase(int rowIndex) {
		return (NamedSBaseWithDerivedUnit) getValueAt(rowIndex, getNamedSBaseColumn());
	}
	
	/**
   * @return the selectedCount the number of selected components in the model.
   */
  public int getSelectedCount() {
    return selectedCount;
  }

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	public Object getValueAt(int rowIndex, int columnIndex) {
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
		for (i=0; i<data.length; i++) {
			data[i][unitCol] = "";
		}
		
		for (i = 0; i < model.getNumCompartments(); i++) {
      fillData(model.getCompartment(i), i, false);
    }
    j = model.getNumCompartments();
    for (i = 0; i < model.getNumSpecies(); i++) {
      fillData(model.getSpecies(i), i + j, false); // true
    }
    j = model.getNumCompartments() + model.getNumSpecies();
    for (i = 0; i < model.getNumParameters(); i++) {
      fillData(model.getParameter(i), i + j, false);
    }
    if (includeReactions) {
      j = model.getNumSymbols();
      for (i = 0; i < model.getNumReactions(); i++) {
        fillData(model.getReaction(i), i + j, false);
      }
    }
    updateOrDeriveUnits();
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
	 */
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return (columnIndex == colorCol) || (columnIndex == boolCol);
	}

	/**
	 * @param rowIndex
	 * @return
	 */
	public boolean isSelected(int rowIndex) {
		return ((Boolean) data[rowIndex][boolCol]).booleanValue();
	}
	
	/**
	 * Returns <code>true</code> if the {@link NamedSBase} belonging to the given
   * <code>id</code> is selected for being shown in the plot.
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

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("done") && (evt.getOldValue() == null)) {
			String defs[] = (String[]) evt.getNewValue();
			for (int rowIndex = 0; rowIndex < defs.length; rowIndex++) {
				data[rowIndex][unitCol] = defs[rowIndex];
			}
			for (TableModelListener l : getTableModelListeners()) {
				l.tableChanged(new TableModelEvent(this, 0, getRowCount() - 1,
					getColumnUnit(), TableModelEvent.UPDATE));
			}
		}
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
	 * 
	 * @param selected
	 */
	public void setSelected(boolean selected) {
		for (int i = 0; i < getRowCount(); i++) {
	    data[i][getColumnPlot()] = Boolean.valueOf(selected);
	  }
	  TableModelEvent evt = new TableModelEvent(this, 0, getRowCount() - 1, getColumnPlot(), TableModelEvent.UPDATE);
		selectedCount = selected ? getRowCount() : 0;
		fireTableChanged(evt);
	}
	
	/**
	 * 
	 * @param clazz
	 * @param selected
	 */
	public void setSelected(Class<? extends NamedSBase> clazz, boolean selected) {
	  int start = 0, end = 0;
	  if (clazz.isAssignableFrom(Reaction.class)) {
	    start = model.getNumSymbols();
	    end = getRowCount();
	  } else {
	    if (clazz.isAssignableFrom(Compartment.class)) {
	      end = model.getNumCompartments();
	    } else if (clazz.isAssignableFrom(Parameter.class)) {
	      start = model.getNumCompartments() + model.getNumSpecies();
	      end = model.getNumSymbols();
	    } else if (clazz.isAssignableFrom(Species.class)) {
	      start = model.getNumCompartments();
	      end = start + model.getNumSpecies();
	    }
	  }
	  for (int i = start; i < end; i++) {
	    setSelected(i, selected);
	  }
	}

	/**
	 * Changes the selection status for the {@link NamedSBase} in the given row.
	 * 
	 * @param rowIndex
	 * @param selected
	 */
	public void setSelected(int rowIndex, boolean selected) {
		setValueAt(Boolean.valueOf(selected), rowIndex, getColumnPlot());
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

	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
	 */
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		Object oldValue = getValueAt(rowIndex, columnIndex);
    if (columnIndex == getColumnPlot()) {
      boolean plot = ((Boolean) oldValue).booleanValue();
      boolean plotNew = ((Boolean) aValue).booleanValue();
      if (plot && !plotNew && (selectedCount > 0)) {
        selectedCount--;
      } else if (!plot && plotNew && (selectedCount < getRowCount())) {
        selectedCount++;
      }
    }
		if (!oldValue.equals(aValue)) {
			data[rowIndex][columnIndex] = aValue;
			fireTableCellUpdated(rowIndex, columnIndex);
		}
	}

	/**
	 * 
	 */
	public void updateOrDeriveUnits() {
		UnitDerivationWorker worker = new UnitDerivationWorker(model);
		worker.addPropertyChangeListener(this);
		worker.execute();
	}

}
