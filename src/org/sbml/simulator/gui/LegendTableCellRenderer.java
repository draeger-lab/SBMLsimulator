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
import java.util.ResourceBundle;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import de.zbit.util.ResourceManager;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-04-07
 * @version $Rev$
 * @since 1.0
 */
public class LegendTableCellRenderer extends JLabel implements
		TableCellRenderer {

  /**
   * The resource bundle to be used to display texts to a user.
   */
  private static final transient ResourceBundle bundle = ResourceManager
      .getBundle("org.sbml.simulator.locales.Simulator");
  
	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = -2028676603661740472L;
	
	/**
	 * 
	 */
	public LegendTableCellRenderer() {
		super();
		setOpaque(true);
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
	 */
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		setBackground(Color.WHITE);
		if (value instanceof Color) {
			Color newColor = (Color) value;
      setToolTipText(String.format(bundle.getString("RGB_TOOLTIP"), newColor
          .getRed(), newColor.getGreen(), newColor.getBlue()));
			setBackground(newColor);
			setForeground(newColor);
		}
		
		return this;
	}

}
