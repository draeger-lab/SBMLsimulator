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

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import de.zbit.gui.table.DecimalCellRenderer;

/**
 * This specialized {@link JPanel} contains a single {@link JTabbedPane}, whose
 * tabs contain a {@link JTable} on a {@link JScrollPane}. For an easy use, this
 * class implements the {@link Iterable} interface, which allows users to create
 * for-each loops that iterate over all {@link TableModel} instances
 * encapsulated in this object. Furthermore, it is possible to add or remove
 * {@link TableModel}s as well as {@link TableCellRenderer}s and
 * {@link TableModelListener}s.
 * 
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @param T the type of {@link TableModel} for all the tables on this view.
 * @since 1.0
 */
public class MultipleTableView<T extends TableModel> extends JPanel implements Iterable<T> {

	/**
	 * 
	 */
	private static final String PROPERTY_DATA_TAB = "measurements";

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = -9179484848090372062L;
	
	/**
	 * 
	 */
	private Map<Class<?>, TableCellRenderer> defaultRenderers;
	
	/**
	 * 
	 */
	private List<TableModelListener> listOfTableModelListeners;
	
	/**
	 * 
	 */
	private JTabbedPane tables;
	
	/**
	 * 
	 */
	public MultipleTableView() {
		super(new BorderLayout());
		tables = new JTabbedPane();
		listOfTableModelListeners = new LinkedList<TableModelListener>();
		defaultRenderers = new HashMap<Class<?>, TableCellRenderer>();
		defaultRenderers.put(Double.class, new DecimalCellRenderer(10, 4,
			SwingConstants.RIGHT));
		add(tables, BorderLayout.CENTER);
	}
	
	/**
	 * 
	 * @param title
	 * @param data
	 */
	public void addTable(String title, T data) {
		JTable table = new JTable(data);
		for (TableModelListener listener : listOfTableModelListeners) {
			data.addTableModelListener(listener);
		}
		for (Map.Entry<Class<?>, TableCellRenderer> entry : defaultRenderers.entrySet()) {
			table.setDefaultRenderer(entry.getKey(), entry.getValue());
		}
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		firePropertyChange(PROPERTY_DATA_TAB, null, data);
		tables.addTab(title, new JScrollPane(table));
	}
	
	/**
	 * 
	 * @param listener
	 */
	public void addTableModelListener(TableModelListener listener) {
		if (!listOfTableModelListeners.contains(listener)) {
			listOfTableModelListeners.add(listener);
			for (T tabModel : this) {
				tabModel.addTableModelListener(listener);
			}
		}
	}
	
	/**
	 * 
	 * @param index
	 * @return
	 */
	public JTable getJTable(int index) {
		if ((0 < index) || (tables.getTabCount() < index)) { 
			throw new IndexOutOfBoundsException(String.format("No tab with index %d", index)); 
		}
		return (JTable) ((JScrollPane) tables.getComponentAt(index)).getViewport().getComponent(0);
	}
	
	/**
	 * Returns the table that has been selected by the user.
	 * 
	 * @return <code>null</code> if no table is selected or this view doesn't
	 *         contain any tables, the currently selected table otherwise.
	 */
	public T getSelectedTable() {
		if (getTableCount() == 0) {
			return null;
		}
		int index = tables.getSelectedIndex();
		if (index < 0) {
			return null;
		}
		return getTable(index);
	}
	
	/**
	 * 
	 * @param index
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public T getTable(int index) {
		return (T) getJTable(index).getModel();
	}
	
	/**
	 * 
	 * @param title
	 * @return
	 */
	public T getTable(String title) {
		return getTable(getTableIndex(title));
	}
	
	/**
	 * 
	 * @return
	 */
	public int getTableCount() {
		return tables.getTabCount();
	}

	/**
	 * 
	 * @param title
	 * @return
	 */
	public int getTableIndex(String title) {
		int index = 0;
		while (index < tables.getTabCount() && !tables.getTitleAt(index).equals(title)) {
			index++;
		}
		if (index < tables.getTabCount()) {
			return index;
		}
		return -1;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<T> getTables() {
		ArrayList<T> list = new ArrayList<T>(getTableCount());
		for (T model : this) {
			list.add(model);
		}
		return list;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			
			int index = 0;
			
			/* (non-Javadoc)
			 * @see java.util.Iterator#hasNext()
			 */
			public boolean hasNext() {
				return index < getTableCount();
			}

			/* (non-Javadoc)
			 * @see java.util.Iterator#next()
			 */
			public T next() {
				return getTable(index++);
			}

			/* (non-Javadoc)
			 * @see java.util.Iterator#remove()
			 */
			public void remove() {
				removeTable(index);
			}
			
		};
	}

	/**
	 * 
	 * @param index
	 */
	public void removeTable(int index) {
		T data = getTable(index);
		tables.removeTabAt(index);
		firePropertyChange(PROPERTY_DATA_TAB, data, null);
	}
	
	/**
	 * 
	 * @param title
	 */
	public void removeTable(String title) {
		int index = getTableIndex(title);
		if (index < 0) {
			throw new IndexOutOfBoundsException(String.format("No tab with title %s", title));
		} else {
		  removeTable(index);
		}
	}

	/**
	 * 
	 * @param listener
	 */
	public void removeTableModelListener(TableModelListener listener) {
		if (listOfTableModelListeners.remove(listener)) {
			for (T tabModel : this) {
				tabModel.removeTableModelListener(listener);
			}
		}
	}

	/**
	 * 
	 * @param clazz
	 * @param renderer
	 */
	public void setDefaultRenderer(Class<?> clazz, TableCellRenderer renderer) {
		TableCellRenderer oldRenderer = defaultRenderers.put(clazz, renderer);
		if ((oldRenderer != null) && !oldRenderer.equals(renderer)) {
			// Update all tables with the new renderer for the given class.
			for (int i = 0; i < getTableCount(); i++) {
				getJTable(i).setDefaultRenderer(clazz, renderer);
			}
		}
	}
	
}
