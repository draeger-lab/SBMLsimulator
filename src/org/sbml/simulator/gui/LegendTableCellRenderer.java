/**
 * 
 */
package org.sbml.simulator.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.sbml.jsbml.NamedSBase;

/**
 * @author Andreas Dr&auml;ger
 * @since 1.4
 * @date 2010-04-07
 */
public class LegendTableCellRenderer extends JLabel implements
		TableCellRenderer {

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax
	 * .swing.JTable, java.lang.Object, boolean, boolean, int, int)
	 */
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		setBackground(Color.WHITE);
		if (value instanceof Color) {
			Color newColor = (Color) value;
			setToolTipText("RGB value: " + newColor.getRed() + ", "
					+ newColor.getGreen() + ", " + newColor.getBlue());
			setBackground(newColor);
		} else if (value instanceof NamedSBase) {
			NamedSBase s = (NamedSBase) value;
			setText(s.isSetName() ? s.getName() : s.getId());
			setBackground(Color.WHITE);
		} else {
			setText(value.toString());
		}
		return this;
	}

}
