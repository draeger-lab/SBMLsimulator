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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 * @author Andreas Dr&auml;ger
 * @author Philip Stevens
 * @author Max Zwie&szlig;ele
 * @version $Rev$
 * @since 1.0
 */
public class ColorEditor extends AbstractCellEditor implements TableCellEditor,
		ActionListener {
	/**
	 * 
	 */
	public static final String EDIT = "edit";
	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = -3645125690115981580L;
	/**
	 * 
	 */
	private JButton button;
	/**
	 * 
	 */
	private JColorChooser colorChooser;
	/**
	 * 
	 */
	private Color currentColor;
	/**
	 * 
	 */
	private JDialog dialog;
	/**
	 * 
	 */
	private LegendPanel parent;

	/**
	 * 
	 */
	public ColorEditor(LegendPanel parent) {
		button = new JButton();
		button.setActionCommand(EDIT);
		button.addActionListener(this);
		button.setBorderPainted(false);

		// Set up the dialog that the button brings up.
		colorChooser = new JColorChooser();
		dialog = JColorChooser.createDialog(button, "Pick a Color", true,
				colorChooser, this, null);

		// Set parent, for being able to send colorchanged events
		this.parent = parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (EDIT.equals(e.getActionCommand())) {
			// The user has clicked the cell, so
			// bring up the dialog.
			button.setBackground(currentColor);
			colorChooser.setColor(currentColor);
			dialog.setVisible(true);

			fireEditingStopped(); // Make the renderer reappear.

		} else { // User pressed dialog's "OK" button.
			currentColor = colorChooser.getColor();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.CellEditor#getCellEditorValue()
	 */
	public Object getCellEditorValue() {
		return currentColor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax.swing
	 * .JTable, java.lang.Object, boolean, int, int)
	 */
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		currentColor = (Color) value;
		return button;
	}
}