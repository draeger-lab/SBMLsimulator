/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2013 by the University of Tuebingen, Germany.
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
import org.sbml.jsbml.util.StringTools;

import de.zbit.util.objectwrapper.ValuePair;

/**
 * <p>
 * A {@link QuantityRange} gathers all necessary information about a
 * {@link Quantity} whose value is to be calibrated by an optimization procedure
 * and its allowable ranges of interest. Each {@link Quantity} may be
 * initialized within a certain interval that is a subset of its absolutely
 * allowable interval. This means, this class gives a {@link Quantity} together
 * with ranges for the optimization, where we distinguish between absolute
 * ranges and another range for the initialization before the actual
 * optimization.
 * </p>
 * <p>
 * This data structure that contains all necessary information for one
 * {@link Quantity}: a {@link Boolean} value to select or de-select it and the
 * values for the optimization. Furthermore, this data structure stores the
 * {@link Double}s for the values associated with the {@link Quantity}. In
 * addition, a pointer to the {@link Quantity} itself is also stored.
 * </p>
 * 
 * @author Andreas Dr&auml;ger
 * @author Roland Keller
 * @date 2010-09-09
 * @version $Rev$
 * @since 1.0
 */
public class QuantityRange implements Cloneable, Serializable {
	
	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 1072196369185232057L;

	/**
	 * 
	 */
	private Double minimum, maximum, initMin, initMax;
	
	/**
	 * 
	 */
	private Quantity quantity;
	
	/**
	 * 
	 */
	private Boolean selected;

	/**
	 * 
	 */
	private double initialGaussianValue;
	
	/**
	 * 
	 */
	private boolean gaussianInitialization;
	
	/**
	 * @return the gaussianInitialization
	 */
	public boolean isGaussianInitialization() {
		return gaussianInitialization;
	}

	/**
	 * 
	 */
	private double gaussianStandardDeviation;
	
	/**
	 * @return the gaussianStandardDeviation
	 */
	public double getGaussianStandardDeviation() {
		return gaussianStandardDeviation;
	}

	/**
	 * @param gaussianStandardDeviation the gaussianStandardDeviation to set
	 */
	public void setGaussianStandardDeviation(double gaussianStandardDeviation) {
		this.gaussianStandardDeviation = gaussianStandardDeviation;
		this.gaussianInitialization = true;
	}

	/**
	 * @return the initialGaussianValue
	 */
	public double getInitialGaussianValue() {
		return initialGaussianValue;
	}

	/**
	 * @param q
	 * @param check
	 * @param initMin
	 * @param initMax
	 * @param min
	 * @param max
	 */
	public QuantityRange(Quantity q, boolean selected, double initMin,
		double initMax, double min, double max) {
		quantity = q;
		this.selected = Boolean.valueOf(selected);
		this.initMin = Double.valueOf(initMin);
		this.initMax = Double.valueOf(initMax);
		this.minimum = Double.valueOf(min);
		this.maximum = Double.valueOf(max);
		this.gaussianInitialization = false;
	}
	
	/**
	 * 
	 * @param q
	 * @param selected
	 * @param initMin
	 * @param initMax
	 * @param min
	 * @param max
	 * @param initialGaussianValue
	 * @param gaussianStandardDeviation
	 */
	public QuantityRange(Quantity q, boolean selected, double initMin,
		double initMax, double min, double max, double initialGaussianValue, double gaussianStandardDeviation) {
		this(q, selected, initMin, initMax, min, max);
		this.initialGaussianValue = initialGaussianValue;
		this.gaussianStandardDeviation = gaussianStandardDeviation;
		this.gaussianInitialization = true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected QuantityRange clone() {
		if(this.gaussianInitialization) {
			return new QuantityRange((Quantity) quantity.clone(),
			selected.booleanValue(), initMin.doubleValue(), initMax.doubleValue(),
			minimum.doubleValue(), maximum.doubleValue(), initialGaussianValue, gaussianStandardDeviation);
		}
		else {
			return new QuantityRange((Quantity) quantity.clone(),
				selected.booleanValue(), initMin.doubleValue(), initMax.doubleValue(),
				minimum.doubleValue(), maximum.doubleValue());
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// Check if the given object is a pointer to precisely the same object:
		if (super.equals(obj)) {
			return true;
		}
		// Check if the given object is of identical class and not null: 
		if ((obj == null) || (!getClass().equals(obj.getClass()))) {
			return false;
		}
		// Check all child nodes recursively:
		if (obj instanceof  QuantityRange) {
			QuantityRange range = (QuantityRange) obj;
			boolean equal = range.getQuantity().equals(getQuantity());
			if (equal) {
				equal &= range.getInitialMinimum() == getInitialMinimum();
				equal &= range.getInitialMaximum() == getInitialMaximum();
				equal &= range.getMinimum() == getMinimum();
				equal &= range.getMaximum() == getMaximum();
			}
			return equal;
		}
		return false;
	}
	
	/**
	 * Delivers the maximal allowable value that can be assigned to the
	 * {@link Quantity} belonging to this {@link Object} during the
	 * initialization of an optimization procedure.
	 * 
	 * @return The maximal initialization value.
	 */
	public double getInitialMaximum() {
		return initMax != null ? initMax.doubleValue() : Double.NaN;
	}
	
	/**
	 * Gives the minimal value that can be used to initialize the
	 * {@link Quantity} belonging to this {@link Object}.
	 * 
	 * @return The lowest possible initialization value for the corresponding
	 *         {@link Quantity}.
	 */
	public double getInitialMinimum() {
		return initMin != null ? initMin.doubleValue() : Double.NaN;
	}
	
	/**
	 * This method directly delivers the range for the initialization with lower
	 * and upper bound.
	 * 
	 * @return A pair of tow {@link Double} values: the lower initialization
	 *         bound {@link #getInitialMinimum()} and the upper initialization
	 *         bound {@link #getInitialMaximum()}.
	 */
	public ValuePair<Double, Double> getInitialRange() {
		return new ValuePair<Double, Double>(Double.valueOf(getInitialMinimum()),
			Double.valueOf(getInitialMaximum()));
	}
	
	/**
	 * This gives the absolute maximal value that can be assigned to the
	 * {@link Quantity} belonging to this {@link Object} during a model
	 * calibration.
	 * 
	 * @return The highest allowable value for the corresponding
	 *         {@link Quantity}.
	 */
	public double getMaximum() {
		return maximum != null ? maximum.doubleValue() : Double.NaN;
	}
	
	/**
	 * The value returned by this method gives the absolute minimum value that
	 * can be assigned to the corresponding {@link Quantity} during an
	 * optimization.
	 * 
	 * @return The lowest allowable value for this quantity.
	 */
	public double getMinimum() {
		return minimum != null ? minimum.doubleValue() : Double.NaN;
	}
	
	/**
	 * This gives the {@link Quantity} whose value is to be calibrated.
	 * 
	 * @return The {@link Quantity} of interest.
	 */
	public Quantity getQuantity() {
		return quantity;
	}
	
	/**
	 * With this method the allowable range of minimal ({@link #getMinimum())
	 * and maximal ({@link #getMaximum()}) value can be easily accessed.
	 * 
	 * @return The possible interval for the corresponding {@link Quantity}.
	 */
	public ValuePair<Double, Double> getRange() {
		return new ValuePair<Double, Double>(Double.valueOf(getMinimum()),
			Double.valueOf(getMaximum()));
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 769;
		int hashCode = getClass().getName().hashCode();
		hashCode += prime * selected.hashCode();
		hashCode += prime * quantity.hashCode();
		hashCode += prime * initMin.hashCode();
		hashCode += prime * initMax.hashCode();
		hashCode += prime * minimum.hashCode();
		hashCode += prime * maximum.hashCode();
		return hashCode;
	}

	/**
	 * @return
	 */
	public boolean isSelected() {
		return selected != null ? selected.booleanValue() : false;
	}

	/* (non-Javadoc)
	 * @see org.sbml.optimization.QuantityRange#setInitialMaximum(double)
	 */
	public void setInitialMaximum(double initMax) {
		this.initMax = Double.valueOf(initMax); 
	}

	/* (non-Javadoc)
	 * @see org.sbml.optimization.QuantityRange#setInitialMinimum(double)
	 */
	public void setInitialMinimum(double initMin) {
		this.initMin = Double.valueOf(initMin);
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.optimization.QuantityRange#setMaximum(double)
	 */
	public void setMaximum(double max) {
		this.maximum = Double.valueOf(max);
	}

	/* (non-Javadoc)
	 * @see org.sbml.optimization.QuantityRange#setMinimum(double)
	 */
	public void setMinimum(double min) {
		this.minimum = Double.valueOf(min);
	}

	/**
	 * @param select
	 */
	public void setSelected(boolean select) {
		selected = Boolean.valueOf(select);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringTools.concat(Character.valueOf('['), quantity, ", ",
			Boolean.valueOf(isSelected()), ": initRange(", getInitialMinimum(),
			", ", getInitialMaximum(), "), absolutRange(", getMinimum(), ", ",
			getMaximum(), ")]").toString();
	}

	/**
	 * 
	 * @param initialGaussianValue
	 */
	public void setInitialGaussianValue(double initialGaussianValue) {
		this.initialGaussianValue = initialGaussianValue;
		this.gaussianInitialization = true;
	}

}
