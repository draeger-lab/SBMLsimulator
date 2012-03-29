/*
 * $Id:  RelativeNMetric.java 17:06:20 keller$
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

import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import de.zbit.util.ResourceManager;


/**
 * @author Roland Keller
 * @version $Rev$
 * @since 1.0
 */
public class Relative_N_Metric extends QualityMeasure {
	
	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 5066304615795368201L;
	
	/**
	 * 
	 */
	private static final ResourceBundle bundle = ResourceManager.getBundle("org.sbml.simulator.locales.Simulator");
	
	/**
	 * Is default value NaN? (for faster computation)
	 */
	private boolean defaultNaN;
	
	/**
	 * 
	 */
	protected N_Metric metric;

	/**
	 * 
	 */
	public Relative_N_Metric() {
		super(Double.NaN);
		metric = new N_Metric();
		defaultNaN = true;
	}
	
	/**
	 * 
	 * @param root
	 */
	public Relative_N_Metric(double root) {
		super(Double.NaN);
		metric = new N_Metric(root);
		defaultNaN = true;
	}

	/**
	 * 
	 * @param metric
	 */
	public Relative_N_Metric(N_Metric metric) {
		super(Double.NaN);
		this.metric = metric;
		if(Double.isNaN(metric.getDefaultValue())) {
			defaultNaN = true;
		}
		else {
			defaultNaN = false;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#distance(java.lang.Iterable, java.lang.Iterable, double)
	 */
	public double distance(Iterable<? extends Number> x,
			Iterable<? extends Number> expected, double defaultValue) {
		double numerator=metric.distance(x, expected, defaultValue);
		List<Double> nullVector = new LinkedList<Double>();
		for(@SuppressWarnings("unused") Number n : expected) {
			nullVector.add(0d);
		}
		double denominator=metric.distance(expected,nullVector,defaultValue);
		double denominator2=metric.distance(x,nullVector,defaultValue);
		if ((denominator != 0) && (denominator2 != 0) ) {
			return numerator / denominator;
		} else if((denominator == 0) && (denominator2 == 0)){
			return numerator;
		} else if(defaultNaN) {
				return numerator;
		}
		else {
			return this.defaultValue;
		}
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#getName()
	 */
	public String getName() {
		return String.format("%s %s", bundle.getString("RELATIVE"),
			metric.getName());
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.math.QualityMeasure#setDefaultValue(double)
	 */
	public void setDefaultValue(double value) {
		super.setDefaultValue(value);
		if(Double.isNaN(defaultValue)) {
			defaultNaN = true;
		}
		else {
			defaultNaN = false;
		}
	}

	/**
	 * @param double1
	 */
	public void setRoot(double root) {
		metric.setRoot(root);
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#toString()
	 */
	public String toString() {
		return String.format("%s %s", getName(), bundle.getString("DISTANCE"));
	}

}
