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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;

import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Quantity;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.optimization.QuantityRange;
import org.sbml.simulator.gui.table.LegendTableCellRenderer;

import de.zbit.gui.GUITools;
import de.zbit.gui.actioncommand.ActionCommand;
import de.zbit.gui.layout.LayoutHelper;
import de.zbit.gui.table.AbstractDocumentFilterListener;
import de.zbit.gui.table.renderer.ColoredBooleanRenderer;
import de.zbit.gui.table.renderer.DecimalCellRenderer;
import de.zbit.sbml.gui.UnitDefinitionCellRenderer;
import de.zbit.util.ResourceManager;
import de.zbit.util.StringUtil;

/**
 * With this element the user can decide which model components should be
 * estimated in an optimization run.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-09-08
 * @version $Rev$
 * @since 1.0
 */
public class QuantitySelectionPanel extends JPanel implements ActionListener {
  
  /**
	 * A helpful class to filter the content of a {@link TableModel}.
	 * 
	 * @author Andreas Dr&auml;ger
	 * @version $Rev$
	 * @since 1.0
	 */
	private class DocumentFilterListener<M extends TableModel> extends AbstractDocumentFilterListener<M> {
		
		/**
		 * 
		 * @param <T>
		 * @param tf
		 * @param sorter
		 */
		public DocumentFilterListener(JTextComponent tf, TableRowSorter<M> sorter) {
			super(tf, sorter);
		}

		/* (non-Javadoc)
		 * @see de.zbit.gui.table.AbstractDocumentFilterListener#createFilter(java.lang.String)
		 */
		protected RowFilter<M, Object> createFilter(String text) throws Exception {
			return RowFilter.regexFilter(text, 1);
		}

	}
	
  /**
	 * A wrapper for {@link QuantityRange} to an {@link AbstractTableModel}.
	 * 
	 * @author Andreas Dr&auml;ger
	 * @version $Rev$
	 * @since 1.0
	 */
	private class QuantityRangeModel extends AbstractTableModel {

		/**
		 * Generated serial version identifier.
		 */
		private static final long serialVersionUID = 7445239993441176230L;

		/**
		 * Index of the last position within the overall array of
		 * {@link QuantityRange} that is stored in the surrounding class.
		 */
		private int lastPos;
		/**
		 * The list of {@link Quantity}s that are of interest here.
		 */
		private List<? extends Quantity> listOfQuantities;
		
		/**
		 * 
		 * @param listOfQuantities
		 * @param lastPos
		 */
		public QuantityRangeModel(List<? extends Quantity> listOfQuantities, int lastPos) {
			super();
			this.lastPos = lastPos;
			this.listOfQuantities = listOfQuantities;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
		 */
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
				case 0:
					return Boolean.class;
				case 1:
					return Quantity.class;
				case 2:
				case 3:
				case 4:
				case 5:
					return Double.class;
				case 6:
					return UnitDefinition.class;
				default:
					throw new IndexOutOfBoundsException(MessageFormat.format(
						bundle.getString("COLUMN_INDEX_OUT_OF_BOUNDS"), columnIndex,
						getColumnCount()));
			}
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount() {
			return 7;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		@Override
		public String getColumnName(int columnIndex) {
			switch (columnIndex) {
				case 0:
					return bundle.getString("SELECTED");
				case 1:
					return bundle.getString("NAME");
				case 2:
					return bundle.getString("INITIAL_MINIMUM");
				case 3:
					return bundle.getString("INITIAL_MAXIMUM");
				case 4:
					return bundle.getString("ABSOLUTE_MINIMUM");
				case 5:
					return bundle.getString("ABSOLUTE_MAXIMUM");
				case 6:
					return bundle.getString("UNIT_COLUMN");
				default:
					throw new IndexOutOfBoundsException(MessageFormat.format(
						bundle.getString("COLUMN_INDEX_OUT_OF_BOUNDS"), columnIndex,
						getColumnCount()));
			}
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public int getRowCount() {
			return listOfQuantities.size();
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int rowIndex, int columnIndex) {
			if ((rowIndex < 0) || (getRowCount() <= rowIndex)) {
				throw new IndexOutOfBoundsException(
					MessageFormat.format(bundle.getString("ROW_INDEX_OUT_OF_BOUNDS"),
						rowIndex, getRowCount()));
			}
			QuantityRange qRange = quantityBlocks[lastPos - getRowCount() + rowIndex];
			switch (columnIndex) {
				case 0:
					return Boolean.valueOf(qRange.isSelected());
				case 1:
					return qRange.getQuantity();
				case 2:
					return Double.valueOf(qRange.getInitialMinimum());
				case 3:
					return Double.valueOf(qRange.getInitialMaximum());
				case 4:
					return Double.valueOf(qRange.getMinimum());
				case 5:
					return Double.valueOf(qRange.getMaximum());
				case 6:
					return qRange.getQuantity().getDerivedUnitDefinition();
				default:
					throw new IndexOutOfBoundsException(MessageFormat.format(
						bundle.getString("COLUMN_INDEX_OUT_OF_BOUNDS"), columnIndex,
						getColumnCount()));
			}
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
		 */
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if ((rowIndex < 0) || (getRowCount() <= rowIndex)) {
				throw new IndexOutOfBoundsException(
					MessageFormat.format(bundle.getString("ROW_INDEX_OUT_OF_BOUNDS"),
						rowIndex, getRowCount()));
			}
			if ((-1 < columnIndex) && (columnIndex < getColumnCount())) {
				if (columnIndex == 0) {
					return true;
				}
				if ((columnIndex == 1) || (columnIndex == 6)) {
					// We neither let the user change the name of the Quantity nor its derived unit!
					return false;
				}
				return quantityBlocks[lastPos - getRowCount() + rowIndex].isSelected();
			}
			throw new IndexOutOfBoundsException(MessageFormat.format(
				bundle.getString("COLUMN_INDEX_OUT_OF_BOUNDS"), columnIndex,
				getColumnCount()));
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
		 */
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if ((rowIndex < 0) || (getRowCount() <= rowIndex)) {
				throw new IndexOutOfBoundsException(
					MessageFormat.format(bundle.getString("ROW_INDEX_OUT_OF_BOUNDS"),
						rowIndex, getRowCount()));
			}
			QuantityRange range = quantityBlocks[lastPos - getRowCount() + rowIndex];
			double newVal;
			switch (columnIndex) {
				case 0:
					range.setSelected(((Boolean) aValue).booleanValue());
					break;
				case 1:
					// actually not supported!
					range.getQuantity().setName(aValue.toString());
					break;
				case 2:
					newVal = ((Double) aValue).doubleValue();
					if (newVal < range.getInitialMaximum()) {
						range.setInitialMinimum(newVal);
					} else {
						logger.warning(MessageFormat.format(
							bundle.getString("CANNOT_ADOPT_NEW_VALUE"),
							StringUtil.toString(newVal), StringUtil.toString(range.getInitialMaximum()), 
							bundle.getString("INITIAL_MAXIMUM"), bundle.getString("EXCEEDS")));
					}
					break;
				case 3:
					newVal = ((Double) aValue).doubleValue();
					if (range.getInitialMinimum() < newVal) {
						range.setInitialMaximum(((Double) aValue).doubleValue());
					} else {
						logger.warning(MessageFormat.format(
							bundle.getString("CANNOT_ADOPT_NEW_VALUE"),
							StringUtil.toString(newVal), StringUtil.toString(range.getInitialMinimum()), 
							bundle.getString("INITIAL_MINIMUM"), bundle.getString("FALLS_BELOW")));
					}
					break;
				case 4:
					newVal = ((Double) aValue).doubleValue();
					if (newVal < range.getMaximum()) {
						range.setMinimum(((Double) aValue).doubleValue());
					} else {
						logger.warning(MessageFormat.format(
							bundle.getString("CANNOT_ADOPT_NEW_VALUE"),
							StringUtil.toString(newVal), StringUtil.toString(range.getMaximum()), 
							bundle.getString("ABSOLUTE_MAXIMUM"), bundle.getString("EXCEEDS")));
					}
					break;
				case 5:
					newVal = ((Double) aValue).doubleValue();
					if (range.getMinimum() < newVal) {
						range.setMaximum(((Double) aValue).doubleValue());
					} else {
						logger.warning(MessageFormat.format(
							bundle.getString("CANNOT_ADOPT_NEW_VALUE"),
							StringUtil.toString(newVal), StringUtil.toString(range.getMinimum()), 
							bundle.getString("ABSOLUTE_MINIMUM"), bundle.getString("FALLS_BELOW")));
					}
					break;
				case 6:
					logger.warning(MessageFormat.format(bundle.getString("CANNOT_DERIVE_UNIT_OF"), range.getQuantity()));
					break;
				default:
					throw new IndexOutOfBoundsException(MessageFormat.format(
						bundle.getString("COLUMN_INDEX_OUT_OF_BOUNDS"), columnIndex,
						getColumnCount()));
			}
		}
		
	}
  
  /**
   * @author Andreas Dr&auml;ger
   * @date 2010-09-08
   * @version $Rev$
   * @since 1.0
   */
  public static enum SelectionCommand implements ActionCommand {
    /**
     * Action to select all elements of one group
     */
    OPTIMIZE_ALL,
    /**
     * Action to deselect all elements of one group
     */
    OPTIMIZE_NONE;

    /* (non-Javadoc)
     * @see de.zbit.gui.ActionCommand#getName()
     */
    public String getName() {
      return bundle.getString(toString());
    }

    /* (non-Javadoc)
     * @see de.zbit.gui.ActionCommand#getToolTip()
     */
    public String getToolTip() {
      return bundle.getString(toString() + "_TOOLTIP");
    }
  }
	
	/**
   * Support for localization.
   */
  private static final transient ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");
	/**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(QuantitySelectionPanel.class.getName());
	
	/**
	 * Generated serial version identifier
	 */
	private static final long serialVersionUID = -4979926949556272791L;
	
	/**
	 * The buttons that allow to (un-) select all items 
	 * representing instances of {@link Quantity}.
	 */
	private JButton[] deselectAllButtons, selectAllButtons;
	
	/**
	 * Values for {@link JSpinner}s of the initialization range
	 */
	private double initMinValue = 0d, initMaxValue = 5d;
	
	/**
	 * Values for {@link JSpinner}s for the absolute ranges
	 */
	private double minValue = 0d, maxValue = 2000d, stepSize = 0.01d;
	
	/**
	 * A pointer to the model for which this panel is being created.
	 */
	private Model model;
	
	/**
	 * This array has one range (initial min, initial max, absolute min, absolute
	 * max) for each {@link Quantity} within the given model.
	 */
	private QuantityRange quantityBlocks[];
	
	/**
	 * This pane displays all selections for each group of different
	 * {@link Quantity}s.
	 */
	private JTabbedPane tabs;
	
	/**
	 * Creates a new {@link QuantitySelectionPanel} for the given {@link Model}.
	 */
	public QuantitySelectionPanel(Model model) {
		super();
		this.model = model;
		quantityBlocks = new QuantityRange[model.getSymbolCount()
				+ model.getLocalParameterCount()];
		// One button each for every group of elements
		selectAllButtons = new JButton[4];
		deselectAllButtons = new JButton[4];
		
		tabs = new JTabbedPane();
		int curr = 0;
		tabs.addTab(bundle.getString("COMPARTMENTS"), createQuantityPanel(model.getListOfCompartments(), curr, 0));
		curr += model.getCompartmentCount();
		tabs.addTab(bundle.getString("SPECIES"), createQuantityPanel(model.getListOfSpecies(), curr, 1));
		curr += model.getSpeciesCount();
		JPanel quantityPanel = createQuantityPanel(model.getListOfParameters(), curr, 2);
		tabs.addTab(bundle.getString("GLOBAL_PARAMETERS"), quantityPanel);
		curr += model.getParameterCount();
		tabs.addTab(bundle.getString("LOCAL_PARAMETERS"), createLocalParameterTab(model));
		
		/*
		 * Enable tabs with selectable elements and disable all others.
		 */
		tabs.setEnabledAt(3, model.getLocalParameterCount() > 0);
		tabs.setToolTipTextAt(
			3,
			StringUtil.toHTMLMessageToolTip(bundle.getString("TAB_TOOL_TIP"),
			bundle.getString("LOCAL_PARAMETERS")));
		tabs.setEnabledAt(2, model.getParameterCount() > 0);
		tabs.setToolTipTextAt(
			2,
			StringUtil.toHTMLMessageToolTip(bundle.getString("TAB_TOOL_TIP"),
			bundle.getString("GLOBAL_PARAMETERS")));
		tabs.setEnabledAt(1, model.getSpeciesCount() > 0);
		tabs.setToolTipTextAt(
			1,
			StringUtil.toHTMLMessageToolTip(bundle.getString("TAB_TOOL_TIP"),
			bundle.getString("SPECIES")));
		tabs.setEnabledAt(0, model.getCompartmentCount() > 0);
		tabs.setToolTipTextAt(
			0,
			StringUtil.toHTMLMessageToolTip(bundle.getString("TAB_TOOL_TIP"),
			bundle.getString("COMPARTMENTS")));
		int i = 3;
		while ((i < tabs.getTabCount()) && (!tabs.isEnabledAt(i))) {
			i--;
		}
		tabs.setSelectedIndex(i);
		
		/*
		 * Ensure same size of all tabs.
		 */
		Component components[] = new Component[tabs.getTabCount()];
		for (i = 0; i < tabs.getTabCount(); i++) {
			components[i] = tabs.getTabComponentAt(i);
		}
		GUITools.calculateAndSetMaxWidth(components);
		
		LayoutHelper lh = new LayoutHelper(this);
		lh.add(new JLabel(StringUtil.toHTMLToolTip(bundle.getString("EXPLANATION"))), 1d, 0d);
		lh.add(tabs);
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if ((e.getActionCommand() == null) || (e.getSource() == null)
				|| !(e.getSource() instanceof JButton)) { 
			return; 
		}
		JButton button = (JButton) e.getSource();
		int i = 0, begin = 0, end = quantityBlocks.length;
		while (!GUITools.contains(tabs.getComponentAt(i), button)) {
			i++;
		}
		String title = tabs.getTitleAt(i);
		if (title.equals(bundle.getString("COMPARTMENTS"))) {
			end = model.getCompartmentCount();
		} else if (title.equals(bundle.getString("SPECIES"))) {
			begin = model.getCompartmentCount();
			end = begin + model.getSpeciesCount();
		} else if (title.equals(bundle.getString("GLOBAL_PARAMETERS"))) {
			begin = model.getCompartmentCount() + model.getSpeciesCount();
			end = begin + model.getParameterCount();
		} else {
			// local parameters
			begin = model.getSymbolCount();
		}
		boolean select;
		Dimension d = (Dimension) button.getPreferredSize().clone();
		switch (SelectionCommand.valueOf(e.getActionCommand())) {
			case OPTIMIZE_ALL:
				select = true;
				selectAllButtons[i].setEnabled(false);
				deselectAllButtons[i].setEnabled(true);
				break;
			case OPTIMIZE_NONE:
				select = false;
				selectAllButtons[i].setEnabled(true);
				deselectAllButtons[i].setEnabled(false);
				break;
			default:
				logger.warning(MessageFormat.format(bundle.getString("UNKNOWN_COMMAND"), e.getActionCommand()));
				select = false;
				break;
		}
		button.setPreferredSize(d);
		for (i = begin; i < end; i++) {
			quantityBlocks[i].setSelected(select);
		}
		tabs.getSelectedComponent().validate();
		tabs.getSelectedComponent().repaint();
	}
	
	/**
	 * Creates a panel with select and de-select buttons
	 * 
	 * @param buttonIndex
	 * @param select
	 * @return
	 */
	private Component createButtonPanel(int buttonIndex, boolean select, final JTable table) {
		JButton selectAllButton = GUITools.createButton(
			SelectionCommand.OPTIMIZE_ALL.getName(), null, this, SelectionCommand.OPTIMIZE_ALL,
			SelectionCommand.OPTIMIZE_ALL.getToolTip());
		JButton deselectAllButton = GUITools.createButton(
			SelectionCommand.OPTIMIZE_NONE.getName(), null, this, SelectionCommand.OPTIMIZE_NONE,
			SelectionCommand.OPTIMIZE_NONE.getToolTip());
		selectAllButton.setEnabled(!select);
		deselectAllButton.setEnabled(select);
		selectAllButton.setPreferredSize(deselectAllButton.getPreferredSize());
		
		selectAllButtons[buttonIndex] = selectAllButton;
		deselectAllButtons[buttonIndex] = deselectAllButton;
		
		// Filtering:
		QuantityRangeModel rangeModel = (QuantityRangeModel) table.getModel();
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));
    table.setFillsViewportHeight(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    TableRowSorter<QuantityRangeModel> sorter = new TableRowSorter<QuantityRangeModel>(rangeModel);
		table.setRowSorter(sorter);
    JTextField filterText = new JTextField(40);
    // Whenever filterText changes, invoke newFilter:
		filterText.getDocument().addDocumentListener(new DocumentFilterListener<QuantityRangeModel>(filterText, sorter));
    LayoutHelper lh = new LayoutHelper(new JPanel());
    lh.add(new JLabel(UIManager.getIcon("ICON_SEARCH_16")), filterText);
		// End Filtering
    
		JPanel panel = new JPanel();
		panel.add(selectAllButton);
		panel.add(deselectAllButton);
		lh.add(panel, 2);
		return lh.getContainer();
	}
	
	/**
	 * @param model
	 * @return
	 */
	private Container createLocalParameterTab(Model model) {
		List<LocalParameter> listOfAllLocalParameters = new LinkedList<LocalParameter>();
		for (Reaction r : model.getListOfReactions()) {
			if (r.isSetKineticLaw() && (r.getKineticLaw().getLocalParameterCount() > 0)) {
				listOfAllLocalParameters.addAll(r.getKineticLaw().getListOfLocalParameters());
			}
		}
		return createQuantityPanel(listOfAllLocalParameters, model.getSymbolCount(), 3);
	}
	
	/**
	 * @param listOfQuantities
	 * @param curr
	 * @param tabIndex
	 * @return
	 */
	private JPanel createQuantityPanel(
		List<? extends Quantity> listOfQuantities, int curr, int tabIndex) {
		boolean isLocalParameter = false;
		boolean select = true;
		for (Quantity q : listOfQuantities) {
			isLocalParameter |= q instanceof LocalParameter;
			select = isLocalParameter || (q instanceof Parameter);
			quantityBlocks[curr++] = new QuantityRange(q, select, initMinValue, initMaxValue,
				minValue, maxValue);
		}
		QuantityRangeModel rangeModel = new QuantityRangeModel(listOfQuantities, curr);
		JTable table = new JTable(rangeModel) {
			
			/**
			 * Generated serial version identifier.
			 */
			private static final long serialVersionUID = -2185541045003706883L;

			/* (non-Javadoc)
			 * @see javax.swing.JTable#getToolTipText(java.awt.event.MouseEvent)
			 */
			@Override
			public String getToolTipText(MouseEvent e) {
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);
        int realColumnIndex = convertColumnIndexToModel(colIndex);
        if (rowIndex < 0) {
        	return null;
        }
        Quantity q = (Quantity) getValueAt(rowIndex, 1);
        String name = q.toString();
        String className = q.getElementName();
        switch (realColumnIndex) {
					case 0:
						return StringUtil.toHTMLMessageToolTip(
		        	bundle.getString("CHECK_BOX_TOOLTIP"), className, name);
					case 1:
						return super.getToolTipText(e); 
					case 2:
						return StringUtil.toHTMLMessageToolTip(
		        	bundle.getString("INIT_MIN_MAX_SPINNER_TOOL_TIP"),
		        	bundle.getString("MINIMUM"), className, name);
					case 3:
						return StringUtil.toHTMLMessageToolTip(
		        	bundle.getString("INIT_MIN_MAX_SPINNER_TOOL_TIP"),
		        	bundle.getString("MAXIMUM"), className, name);
					case 4:
						return StringUtil.toHTMLMessageToolTip(
		        	bundle.getString("MIN_MAX_SPINNER_TOOL_TIP"),
		        	bundle.getString("MINIMUM"), className, name);
					case 5:
						return StringUtil.toHTMLMessageToolTip(
		        	bundle.getString("MIN_MAX_SPINNER_TOOL_TIP"),
		        	bundle.getString("MAXIMUM"), className, name);
					default:
						return null;
				}
    }};
		table.setDefaultRenderer(Boolean.class, new ColoredBooleanRenderer());
		table.setDefaultRenderer(Quantity.class, new LegendTableCellRenderer());
		table.setDefaultRenderer(Double.class, new DecimalCellRenderer());
		table.setDefaultRenderer(UnitDefinition.class, new UnitDefinitionCellRenderer());
		//  table.setDefaultEditor(Double.class, new DefaultCellEditor(new JSpinner(new SpinnerNumberModel())));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    
		LayoutHelper lh = new LayoutHelper(new JPanel());
		lh.add(new JScrollPane(table));
		
		// finish container
		int numElements = listOfQuantities.size();
		int height = 10 + 25 * numElements;
		Container container = lh.getContainer();
		container.setPreferredSize(new Dimension(550, (int) Math.min(250, height)));
		lh = new LayoutHelper(new JPanel());
		lh.add(container);
		lh.add(new JSeparator(), 0, lh.getRow() + 1, 5, 1, 1d, 0d);
		lh.add(createButtonPanel(tabIndex, select, table), 0, lh.getRow() + 2, 5, 1, 0d, 0d);
		
		return (JPanel) lh.getContainer();
	}
	
	/**
	 * @return the initMaxValue
	 */
	public double getInitMaxValue() {
		return initMaxValue;
	}
	
	/**
	 * @return the initMinValue
	 */
	public double getInitMinValue() {
		return initMinValue;
	}
	
	/**
	 * @return the maxValue
	 */
	public double getMaxValue() {
		return maxValue;
	}
	
	/**
	 * @return the minValue
	 */
	public double getMinValue() {
		return minValue;
	}
	
	/**
	 * @return
	 */
	public Quantity[] getSelectedQuantities() {
		LinkedList<Quantity> quantityList = new LinkedList<Quantity>();
		for (int i = 0; i < quantityBlocks.length; i++) {
			if (quantityBlocks[i].isSelected()) {
				quantityList.add(quantityBlocks[i].getQuantity());
			}
		}
		return quantityList.toArray(new Quantity[0]);
	}
	
	/**
	 * @return
	 */
	public String[] getSelectedQuantityIds() {
		LinkedList<String> quantityList = new LinkedList<String>();
		for (int i = 0; i < quantityBlocks.length; i++) {
			if (quantityBlocks[i].isSelected()) {
				quantityList.add(quantityBlocks[i].getQuantity().getId());
			}
		}
		return quantityList.toArray(new String[0]);
	}
	
	/**
	 * @return
	 */
	public QuantityRange[] getSelectedQuantityRanges() {
		LinkedList<QuantityRange> quantityList = new LinkedList<QuantityRange>();
		for (int i = 0; i < quantityBlocks.length; i++) {
			if (quantityBlocks[i].isSelected()) {
				quantityList.add(quantityBlocks[i]);
			}
		}
		return quantityList.toArray(new QuantityRange[0]);
	}
	
	/**
	 * @return the stepSize
	 */
	public double getStepSize() {
		return stepSize;
	}
	
	/**
	 * @param initMaxValue
	 *        the initMaxValue to set
	 */
	public void setInitMaxValue(double initMaxValue) {
		this.initMaxValue = initMaxValue;
	}
	
	/**
	 * @param initMinValue
	 *        the initMinValue to set
	 */
	public void setInitMinValue(double initMinValue) {
		this.initMinValue = initMinValue;
	}
	
	/**
	 * @param maxValue
	 *        the maxValue to set
	 */
	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}
	
	/**
	 * @param minValue
	 *        the minValue to set
	 */
	public void setMinValue(double minValue) {
		this.minValue = minValue;
	}
	
	/**
	 * @param stepSize
	 *        the stepSize to set
	 */
	public void setStepSize(double stepSize) {
		this.stepSize = stepSize;
	}

}
