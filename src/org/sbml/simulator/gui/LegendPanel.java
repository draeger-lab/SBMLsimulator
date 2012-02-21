/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2012 by the University of Tuebingen, Germany.
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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBaseWithDerivedUnit;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.simulator.gui.table.LegendTableCellRenderer;
import org.sbml.simulator.gui.table.LegendTableModel;

import de.zbit.gui.ActionCommand;
import de.zbit.gui.GUITools;
import de.zbit.gui.JDropDownButton;
import de.zbit.gui.table.ColorEditor;
import de.zbit.gui.table.ColoredBooleanRenderer;
import de.zbit.gui.table.JTableTools;
import de.zbit.sbml.gui.UnitDefinitionCellRenderer;
import de.zbit.util.ResourceManager;

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
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(LegendPanel.class.getName());

	/**
	 * 
	 * @author Andreas Dr&auml;ger
	 * @version $Rev$
	 * @since 1.0
	 */
	public static enum COMPONENT implements ActionCommand {
	  /**
	   * Selects all available components for the plot.
	   */
	  ALL,
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
    FLUXES,
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
	 * Localization support.
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
	private JDropDownButton selectAll, selectNone;
	
	/**
	 * 
	 */
	private List<TableModelListener> listOfTableModelListeners;
		
	/**
	 * 
	 * @param model
	 */
	public LegendPanel(Model model, boolean includeReactions) {
	  super(new BorderLayout());
    listOfTableModelListeners = new LinkedList<TableModelListener>();
    final JTable table = createLegendTable(model, includeReactions);
		add(new JScrollPane(table), BorderLayout.CENTER);
    JPopupMenu menuSelect = new JPopupMenu(), menuDeselect = new JPopupMenu();
    for (COMPONENT item : COMPONENT.values()) {
      menuSelect.add(GUITools.createJMenuItem(this, item));
      menuDeselect.add(GUITools.createJMenuItem(this, item));
    }
    menuSelect.setName("SELECT_ALL");
    menuDeselect.setName("SELECT_NONE");
    selectAll = GUITools.createJDropDownButton(menuSelect.getName(), bundle, menuSelect);
    selectNone = GUITools.createJDropDownButton(menuDeselect.getName(), bundle, menuDeselect);
    selectAll.setPreferredSize(GUITools.getMaxPreferredSize(selectAll, selectNone));
    selectNone.setPreferredSize(selectAll.getPreferredSize());    
    checkSelected();
    
    JButton searchButton = new JButton(UIManager.getIcon("ICON_SEARCH_16"));
    searchButton.addActionListener(new ActionListener() {
			
    	/* (non-Javadoc)
    	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    	 */
			public void actionPerformed(ActionEvent e) {
				KeyEvent F3 = new KeyEvent(table, 0, 0, 0, 114, (char) 114);
				for (KeyListener l : table.getKeyListeners()) {
					l.keyPressed(F3);
				}
			}
		});
    
    JPanel foot = new JPanel();
    foot.add(selectAll);
    foot.add(selectNone);
    foot.add(searchButton); 
    add(foot, BorderLayout.SOUTH);
  }

  /* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
    if ((e.getActionCommand() != null) && (e.getSource() != null)) {
		  try {
        boolean selected = ((Component) e.getSource()).getParent().getName()
            .equals(selectAll.getName());
				COMPONENT component = COMPONENT.valueOf(e.getActionCommand());
				switch (component) {
          case ALL:
            legend.setSelected(selected);
            break;
          case COMPARTMENTS:
            legend.setSelected(Compartment.class, selected);
            break;
          case PARAMETERS:
            legend.setSelected(Parameter.class, selected);
            break;
          case FLUXES:
            legend.setSelected(Reaction.class, selected);
            break;
          case SPECIES:
            legend.setSelected(Species.class, selected);
            break;
          default:
            break;
        }
				checkSelected();
			} catch (Throwable exc) {
			  logger.fine(exc.getLocalizedMessage());
			}
		}
	}

	/**
	 * 
	 * @param listener
	 * @return
	 */
	public boolean addTableModelListener(TableModelListener listener) {
		if (!listOfTableModelListeners.contains(listener)) { 
			return listOfTableModelListeners.add(listener); 
		}
		return false;
	}

	/**
	 * 
	 */
  private void checkSelected() {
    int selected = legend.getSelectedCount();
    selectAll.setEnabled(selected < legend.getRowCount());
    selectNone.setEnabled(!selectAll.isEnabled() || (selected > 0));
  }

	/**
	 * 
	 * @param model
	 * @return
	 */
	private JTable createLegendTable(Model model, boolean includeReactions) {
		this.legend = new LegendTableModel(model, includeReactions, this);
		JTable tab = new JTable();
		tab.setName("legend");
		tab.setModel(legend);
		tab.setDefaultEditor(Color.class, new ColorEditor());
		tab.setDefaultRenderer(Color.class, new LegendTableCellRenderer());
		tab.setDefaultRenderer(NamedSBaseWithDerivedUnit.class, new LegendTableCellRenderer());
		tab.setDefaultRenderer(UnitDefinition.class, new UnitDefinitionCellRenderer());
		tab.setDefaultRenderer(Boolean.class, new ColoredBooleanRenderer());
		tab.getModel().addTableModelListener(this);
		JTableTools.setQuickSearch(tab);
    //		tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    TableColumnModel colModel = tab.getColumnModel();
    int index[] = new int[] { LegendTableModel.getColumnPlot(),
        LegendTableModel.getColumnColor(), LegendTableModel.getColumnUnit() };
    int width[] = new int[] { 80, 50, 100 };
    for (int i = 0; i < index.length; i++) {
      colModel.getColumn(index[i]).setPreferredWidth(width[i]);
    }
		return tab;
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
	 * @param listener
	 * @return
	 */
	public boolean removeTableModelListener(TableModelListener listener) {
		return listOfTableModelListeners.remove(listener);
	}

	/* (non-Javadoc)
	 * @seejavax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
	 */
	public void tableChanged(TableModelEvent e) {
    if (e.getType() == TableModelEvent.UPDATE) {
      if (e.getColumn() == LegendTableModel.getColumnPlot()) {
        checkSelected();
      } else if (e.getColumn() == LegendTableModel.getColumnUnit()) {
        validate();
      }
    }
		for (TableModelListener tml : listOfTableModelListeners) {
			tml.tableChanged(e);
		}
	}
}
