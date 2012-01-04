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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * A table renderer for decimal numbers.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-09-21
 * @version $Rev$
 * @since 1.0
 */
public class DecimalCellRenderer extends DefaultTableCellRenderer {
	
	/**
	 * Generated serial version identifier
	 */
	private static final long serialVersionUID = 7169267933533860622L;
	
	/**
	 * 
	 */
	public static final String REAL_FORMAT = "########.###############";
	/**
	 * 
	 */
	public static final String SCIENTIFIC_FORMAT = "###0.######E0";
	
  /**
	 * 
	 */
	public static final String DECIMAL_FORMAT = "###0.######";
	
	/**
	 * alignment (LEFT, CENTER, RIGHT)
	 */
	private int align;
	
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
	 * @param integer
	 *        maximum integer digits
	 * @param fraction
	 *        number of fraction digits
	 * @param align
	 *        alignment (LEFT, CENTER, RIGHT)
	 */
	public DecimalCellRenderer(final int integer, final int fraction,
		final int align) {
		this(integer, fraction, align, false);
	}
	
	/**
	 * @param integer
	 *        maximum integer digits
	 * @param fraction
	 *        exact number of fraction digits if exact is true
	 * @param align
	 *        alignment (LEFT, CENTER, RIGHT)
	 * @param exact
	 *        whether or not fraction should be the exact number of fraction
	 *        digits.
	 */
	public DecimalCellRenderer(int integer, int fraction, int align, boolean exact) {
		this();
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
		this.formatter = new DecimalFormat(DECIMAL_FORMAT);
		this.align = SwingConstants.RIGHT;
		this.allSame = false;
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.DefaultTableCellRenderer#setValue(java.lang.Object)
	 */
	@Override
	protected void setValue(final Object value) {
		if ((value != null) && (value instanceof Number)) {
			double v = ((Number) value).doubleValue();
			if (Double.isNaN(v)) {
				setText("NaN");
			} else if (Double.isInfinite(v)) {
				setText((v < 0) ? "-\u221E" : "\u221E");
			} else if (((int) v) - v == 0) {
				setText(String.format("%d", Integer.valueOf((int) v)));
			} else {
				Locale locale = Locale.getDefault();
				DecimalFormat df;
				if ((Math.abs(v) < 1E-5f) || (1E5f < Math.abs(v))) {
					df = new DecimalFormat(SCIENTIFIC_FORMAT, new DecimalFormatSymbols(locale));
				} else {
					df = new DecimalFormat(DECIMAL_FORMAT, new DecimalFormatSymbols(locale));
				}
				df.setMaximumIntegerDigits(formatter.getMaximumIntegerDigits());
				df.setMaximumFractionDigits(formatter.getMaximumFractionDigits());
				df.setMinimumFractionDigits(formatter.getMinimumFractionDigits());
				setText(df.format(value));
			}
		} else {
			super.setValue(value);
		}
		setHorizontalAlignment(align);
	}

}
