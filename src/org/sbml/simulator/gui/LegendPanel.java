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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.sbml.jsbml.Model;

import de.zbit.gui.ActionCommand;
import de.zbit.gui.GUITools;
import de.zbit.gui.JDropDownButton;
import de.zbit.util.ResourceManager;
import de.zbit.util.StringUtil;

/**
 * Container for a {@link LegendTableModel} in a {@link JTable} and two
 * {@link JButton} instances to select or deselect all elements.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-09-17
 * @version $Rev$
 * @since 1.0
 */
public class LegendPanel extends JPanel implements TableModelListener,
		ActionListener {

	/**
	 * 
	 * @author Andreas Dr&auml;ger
	 * @version $Rev$
	 * @since 1.0
	 */
	public static enum SelectAll implements ActionCommand {
	  /**
	   * Selects all compartments for the plot.
	   */
    COMPARTMENTS, 
    /**
     * With this option you can plot the changes of all global parameters in the model.
     */
    PARAMETERS,
    /**
     * Select this option to plot the evolution of all flux values in the model.
     */
    REACTIONS,
    /**
     * If this option is selected, all species of the model are plotted.
     */
    SPECIES;

    /* (non-Javadoc)
     * @see de.zbit.gui.ActionCommand#getName()
     */
    public String getName() {
      return bundle.getString(this.toString());
    }

    /* (non-Javadoc)
     * @see de.zbit.gui.ActionCommand#getToolTip()
     */
    public String getToolTip() {
      return bundle.getString(this.toString() + "_TOOLTIP");
    }

	}
	
	/**
	 * 
	 */
  private static final transient ResourceBundle bundle = ResourceManager
      .getBundle("org.sbml.simulator.locales.Simulator");
	
	/**
	 * Generated serial version identifier
	 */
	private static final long serialVersionUID = 4018387447860613404L;
	/**
	 * 
	 */
	private LegendTableModel legend;
	/**
	 * 
	 */
	private JDropDownButton selectAll;
	/**
	 * 
	 */
	private int selectedCount;
	/**
	 * 
	 */
	private JButton selectNone;

	/**
	 * 
	 */
	private Set<TableModelListener> setOfTableModelListeners;
	
	/**
	 * 
	 * @param model
	 */
	public LegendPanel(Model model, boolean includeReactions) {
		super(new BorderLayout());
		setOfTableModelListeners = new HashSet<TableModelListener>();
		JTable legendTable = createLegendTable(model, includeReactions);
		add(new JScrollPane(legendTable,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
				BorderLayout.CENTER);
		SelectionCommand command = SelectionCommand.ALL;
		
		JPopupMenu menu = new JPopupMenu();
    for (SelectAll item : SelectAll.values()) {
      menu.add(GUITools.createJMenuItem(this, item));
    }
		selectAll = new JDropDownButton(command.getText(), menu);
		String tooltip = command.getToolTip();
    if ((tooltip != null) && (tooltip.length() > 0)) {
      selectAll.setToolTipText(StringUtil.toHTML(tooltip,
        GUITools.TOOLTIP_LINE_LENGTH));
    }
    
		command = SelectionCommand.NONE;
		selectNone = GUITools.createButton(command.getText(), null, this,
				command, command.getToolTip());
		selectAll.setPreferredSize(selectNone.getPreferredSize());
		checkSelected();
		JPanel foot = new JPanel();
		foot.add(selectAll);
		foot.add(selectNone);
		add(foot, BorderLayout.SOUTH);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() != null) {
			try {
				boolean selected = false;
				if (SelectionCommand.valueOf(e.getActionCommand()).equals(
						SelectionCommand.ALL)) {
					selected = true;
				}
				for (int i = 0; i < legend.getRowCount(); i++) {
					legend.setSelected(i, selected);
				}
				selectAll.setEnabled(!selected);
				selectNone.setEnabled(selected);
				selectedCount = selected ? legend.getRowCount() : 0;
			} catch (Throwable t) {
			}
		}
	}

	/**
	 * 
	 * @param listener
	 * @return
	 */
	public boolean addTableModelListener(TableModelListener listener) {
		return setOfTableModelListeners.add(listener);
	}

	/**
	 * 
	 */
	private void checkSelected() {
		selectedCount = 0;
		for (int i = 0; i < legend.getRowCount(); i++) {
			if (legend.isSelected(i)) {
				selectedCount++;
			}
		}
		selectAll.setEnabled(selectedCount < legend.getRowCount());
		selectNone.setEnabled(!selectAll.isEnabled());
	}

	/**
	 * 
	 * @param model
	 * @return
	 */
	private JTable createLegendTable(Model model, boolean includeReactions) {
		this.legend = new LegendTableModel(model, includeReactions);
		JTable tab = new JTable();
		tab.setName("legend");
		tab.setModel(legend);

		tab.setDefaultEditor(Color.class, new ColorEditor(this));
		LegendTableCellRenderer renderer = new LegendTableCellRenderer();
		tab.setDefaultRenderer(Color.class, renderer);
		tab.getModel().addTableModelListener(this);

		return tab;
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public Color getColorFor(String id) {
		return legend.getColorFor(id);
	}

	/**
	 * 
	 * @return
	 */
	public LegendTableModel getLegendTableModel() {
		return legend;
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public String getNameFor(String id) {
		return legend.getNameFor(id);
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public boolean isSelected(String id) {
		return legend.isSelected(id);
	}

	/**
	 * 
	 * @param listener
	 * @return
	 */
	public boolean removeTableModelListener(TableModelListener listener) {
		return setOfTableModelListeners.remove(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seejavax.swing.event.TableModelListener#tableChanged(javax.swing.event.
	 * TableModelEvent)
	 */
	public void tableChanged(TableModelEvent e) {
		if ((e.getType() == TableModelEvent.UPDATE)
				&& (e.getColumn() == LegendTableModel.getBooleanColumn())) {
			if (legend.isSelected(e.getFirstRow())) {
				if (selectedCount < legend.getRowCount()) {
					selectedCount++;
				}
			} else if (selectedCount > 0) {
				selectedCount--;
			}
			selectNone.setEnabled(selectedCount > 0);
			selectAll.setEnabled(selectedCount < legend.getRowCount());
		}
		for (TableModelListener tml : setOfTableModelListeners) {
			tml.tableChanged(e);
		}
	}
}
