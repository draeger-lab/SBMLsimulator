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
package org.sbml.optimization;

import java.io.Serializable;

import org.sbml.jsbml.Quantity;

import de.zbit.util.ValuePair;

/**
 * A {@link QuantityRange} gathers all necessary information about a
 * {@link Quantity} whose value is to be calibrated by an optimization procedure
 * and its allowable ranges of interest. Each {@link Quantity} may be
 * initialized within a certain interval that is a subset of its absolutely
 * allowable interval. This means, this class gives a {@link Quantity} together
 * with ranges for the optimization, where we distinguish between absolute
 * ranges and another range for the initialization before the actual
 * optimization.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-09-09
 * @version $Rev$
 * @since 1.0
 */
public interface QuantityRange extends Serializable {

	/**
	 * Delivers the maximal allowable value that can be assigned to the
	 * {@link Quantity} belonging to this {@link Object} during the
	 * initialization of an optimization procedure.
	 * 
	 * @return The maximal initialization value.
	 */
	public double getInitialMaximum();

	/**
	 * Gives the minimal value that can be used to initialize the
	 * {@link Quantity} belonging to this {@link Object}.
	 * 
	 * @return The lowest possible initialization value for the corresponding
	 *         {@link Quantity}.
	 */
	public double getInitialMinimum();

	/**
	 * This method directly delivers the range for the initialization with lower
	 * and upper bound.
	 * 
	 * @return A pair of tow {@link Double} values: the lower initialization
	 *         bound {@link #getInitialMinimum()} and the upper initialization
	 *         bound {@link #getInitialMaximum()}.
	 */
	public ValuePair<Double, Double> getInitialRange();

	/**
	 * This gives the absolute maximal value that can be assigned to the
	 * {@link Quantity} belonging to this {@link Object} during a model
	 * calibration.
	 * 
	 * @return The highest allowable value for the corresponding
	 *         {@link Quantity}.
	 */
	public double getMaximum();

	/**
	 * The value returned by this method gives the absolute minimum value that
	 * can be assigned to the corresponding {@link Quantity} during an
	 * optimization.
	 * 
	 * @return The lowest allowable value for this quantity.
	 */
	public double getMinimum();

	/**
	 * This gives the {@link Quantity} whose value is to be calibrated.
	 * 
	 * @return The {@link Quantity} of interest.
	 */
	public Quantity getQuantity();

	/**
	 * With this method the allowable range of minimal ({@link #getMinimum())
	 * and maximal ({@link #getMaximum()}) value can be easily accessed.
	 * 
	 * @return The possible interval for the corresponding {@link Quantity}.
	 */
	public ValuePair<Double, Double> getRange();

	/**
	 * Gives a {@link String} representation of this {@link Object} that
	 * represents all of its values.
	 * 
	 * @return A {@link String} containing the properties of this {@link Object}
	 */
	public String toString();

}
