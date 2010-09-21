package org.sbml.simulator.gui;

import java.text.NumberFormat;

import javax.swing.table.DefaultTableCellRenderer;

/**
 * A table renderer for numbers.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-09-21
 */
public class FractionCellRenderer extends DefaultTableCellRenderer {
	/**
	 * Generated serial version identifier
	 */
	private static final long serialVersionUID = 7169267933533860622L;

	/**
	 * maximum integer digits
	 */
	final private int integer;

	/**
	 * exact number of fraction digits
	 */
	final private int fraction;

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
	final private NumberFormat formatter = NumberFormat.getInstance();
	/**
	 * 
	 * @param integer
	 *            maximum integer digits
	 * @param fraction
	 *            number of fraction digits
	 * @param align
	 *            alignment (LEFT, CENTER, RIGHT)
	 */
	public FractionCellRenderer(final int integer, final int fraction,
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
	public FractionCellRenderer(int integer, int fraction, int align,
			boolean exact) {
		this.integer = integer;
		this.fraction = fraction;
		this.align = align;
		this.allSame = exact;
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
			formatter.setMaximumIntegerDigits(integer);
			formatter.setMaximumFractionDigits(fraction);
			formatter.setMinimumFractionDigits(allSame ? fraction : 0);
			setText(formatter.format(((Number) value).doubleValue()));
		} else {
			super.setValue(value);
		}
		setHorizontalAlignment(align);
	}
}
