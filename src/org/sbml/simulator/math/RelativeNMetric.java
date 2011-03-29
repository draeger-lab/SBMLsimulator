/*
 * $Id:  RelativeNMetric.java 17:06:20 keller$
 * $URL: RelativeNMetric.java $
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

package org.sbml.simulator.math;

import java.util.LinkedList;
import java.util.List;


/**
 * @author Roland Keller
 * @version $Rev$
 * @since
 */
public class RelativeNMetric extends QualityMeasure {
	
	protected NMetric metric;
	/**
	 * 
	 */
	private static final long serialVersionUID = 5066304615795368201L;

	public RelativeNMetric() {
		super();
		metric=new NMetric();
	}
	
	public RelativeNMetric(double root) {
		super(Double.NaN);
		metric=new NMetric(root);
	}

	public RelativeNMetric(NMetric metric) {
		super(Double.NaN);
		this.metric=metric;
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#getName()
	 */
	@Override
	public String getName() {
		return "Relative " + metric.getName();
	}


	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#distance(java.lang.Iterable, java.lang.Iterable, double)
	 */
	@Override
	public double distance(Iterable<? extends Number> x,
			Iterable<? extends Number> expected, double defaultValue) {
		double numerator=metric.distance(x, expected, defaultValue);
		List<Double> nullVector = new LinkedList<Double>();
		for(@SuppressWarnings("unused") Number n:expected) {
			nullVector.add(0d);
		}
		double denominator=metric.distance(expected,nullVector,defaultValue);
		if(denominator != 0) {
			return numerator/denominator;
		}
		else {
			return numerator;
		}
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#toString()
	 */
	@Override
	public String toString() {
		return getName() + " distance";
	}

	/**
	 * @param double1
	 */
	public void setRoot(double root) {
		metric.setRoot(root);
	}

}
