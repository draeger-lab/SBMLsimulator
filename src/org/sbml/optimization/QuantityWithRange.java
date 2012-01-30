/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models of biochemical processes encoded in the modeling language SBML.
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

import org.sbml.jsbml.Quantity;

import de.zbit.util.ValuePair;

/**
 * @author Roland Keller
 * @version $Rev$
 */
public class QuantityWithRange implements QuantityRange {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	private double min;
	
	/**
	 * 
	 */
	private double max;
	
	/**
	 * 
	 */
	private double initMin;
	
	/**
	 * 
	 */
	private double initMax;

	/**
	 * 
	 */
	private Quantity quantity;
	
	
	/**
	 * 
	 * @param quantity
	 * @param initMin
	 * @param initMax
	 * @param min
	 * @param max
	 */
	public QuantityWithRange(Quantity quantity, double initMin, double initMax,
		double min, double max) {
		this.quantity = quantity;
		this.min = min;
		this.max = max;
		this.initMin = initMin;
		this.initMax = initMax;
	}

	/* (non-Javadoc)
	 * @see org.sbml.optimization.QuantityRange#getInitialMaximum()
	 */
	public double getInitialMaximum() {
		return initMax;
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.optimization.QuantityRange#getInitialMinimum()
	 */
	public double getInitialMinimum() {
		return initMin;
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.optimization.QuantityRange#getInitialRange()
	 */
	public ValuePair<Double, Double> getInitialRange() {
		return new ValuePair<Double, Double>(Double.valueOf(getInitialMinimum()),
				Double.valueOf(getInitialMaximum()));
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.optimization.QuantityRange#getMaximum()
	 */
	public double getMaximum() {
		return max;
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.optimization.QuantityRange#getMinimum()
	 */
	public double getMinimum() {
		return min;
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.optimization.QuantityRange#getQuantity()
	 */
	public Quantity getQuantity() {
		return quantity;
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.optimization.QuantityRange#getRange()
	 */
	public ValuePair<Double, Double> getRange() {
		return new ValuePair<Double, Double>(Double.valueOf(getMinimum()),
				Double.valueOf(getMaximum()));
	}
	
}
