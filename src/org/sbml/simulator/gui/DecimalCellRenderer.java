package org.sbml.simulator.gui;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * A table renderer for decimal numbers.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-09-21
 */
public class DecimalCellRenderer extends DefaultTableCellRenderer {
	/**
	 * Generated serial version identifier
	 */
	private static final long serialVersionUID = 7169267933533860622L;

	/**
	 * alignment (LEFT, CENTER, RIGHT)
	 */
	final private int align;

	/**
	 * Switch to decide whether or not numbers should be formatted all with the
	 * identical number of fraction digits. Default is false.
	 */
	private boolean allSame;
	/**
	 * 
	 */
	private NumberFormat formatter;

	/**
	 * 
	 * @param integer
	 *            maximum integer digits
	 * @param fraction
	 *            number of fraction digits
	 * @param align
	 *            alignment (LEFT, CENTER, RIGHT)
	 */
	public DecimalCellRenderer(final int integer, final int fraction,
			final int align) {
		this(integer, fraction, align, false);
	}

	/**
	 * 
	 * @param integer
	 *            maximum integer digits
	 * @param fraction
	 *            exact number of fraction digits if exact is true
	 * @param align
	 *            alignment (LEFT, CENTER, RIGHT)
	 * @param exact
	 *            whether or not fraction should be the exact number of fraction
	 *            digits.
	 */
	public DecimalCellRenderer(int integer, int fraction, int align,
			boolean exact) {
		this.formatter = NumberFormat.getInstance();
		formatter.setMaximumIntegerDigits(integer);
		formatter.setMaximumFractionDigits(fraction);
		formatter.setMinimumFractionDigits(allSame ? fraction : 0);
		this.align = align;
	}

	/**
	 * 
	 */
	public DecimalCellRenderer() {
		this.formatter = new DecimalFormat("########0.#########E0");
		this.align = SwingConstants.RIGHT;
		this.allSame = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.swing.table.DefaultTableCellRenderer#setValue(java.lang.Object)
	 */
	@Override
	protected void setValue(final Object value) {
		if ((value != null) && (value instanceof Number)) {
			setText(formatter.format(((Number) value).doubleValue()));
		} else {
			super.setValue(value);
		}
		setHorizontalAlignment(align);
	}
}