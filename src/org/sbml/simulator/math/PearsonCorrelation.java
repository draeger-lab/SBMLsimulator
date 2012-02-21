/*
 * $Id:  Pearson.java 09:55:58 keller$
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
package org.sbml.simulator.math;

import java.util.Iterator;

import de.zbit.util.ResourceManager;


/**
 * @author Roland Keller
 * @version $Rev$
 * @since 1.0
 */
public class PearsonCorrelation extends QualityMeasure {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = -493779339080103217L;

	
	/**
	 * Default constructor. This sets the standard value for the parameter as
	 * given by the getStandardParameter() method. The default value is set to
	 * NaN.
	 */
	public PearsonCorrelation() {
		super();
		meanFunction=new ArithmeticMean();
	}

	/**
	 * Constructor, which allows setting the parameter value for default value.
	 * 
	 * @param defaultValue
	 */
	public PearsonCorrelation(double defaultValue) {
		super(defaultValue);
		meanFunction=new ArithmeticMean();
	}
	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#distance(java.lang.Iterable, java.lang.Iterable, double)
	 */
	public double distance(Iterable<? extends Number> x,
			Iterable<? extends Number> y, double defaultValue) {
		Iterator<? extends Number> yIterator = y.iterator();
		
		MeanFunction meanF = new ArithmeticMean();
		double meanX = meanF.computeMean(x);
		double meanY = meanF.computeMean(y);
		
		double sumNumerator = 0d;
		double sumXSquared = 0d;
		double sumYSquared = 0d;
		
		for (Number number : x) {
			if (!yIterator.hasNext()) {
				break;
			}
			double x_i = number.doubleValue();
			double y_i = yIterator.next().doubleValue();
			sumNumerator+= (x_i-meanX)*(y_i-meanY);
			sumXSquared+= (x_i-meanX)*(x_i-meanX);
			sumYSquared+= (y_i-meanY)*(y_i-meanY);
		}
		
		double denominator=Math.sqrt(sumXSquared*sumYSquared);
		if(denominator!=0) {
			return sumNumerator/denominator;
		}
		else {
			return defaultValue;
		}
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#getName()
	 */
	public String getName() {
		return ResourceManager.getBundle("org.sbml.simulator.locales.Simulator").getString(getClass().getSimpleName());
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#toString()
	 */
	public String toString() {
		return getName();
	}

}
