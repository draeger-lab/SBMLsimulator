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

import org.sbml.simulator.math.odes.MultiBlockTable;

/**
 * @author Roland Keller
 * @version $Rev$
 * @since
 */
public class RelativeNMetric extends Distance {
	
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
		super(root, Double.NaN);
		metric=new NMetric(root);
	}
	
	
	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#additiveTerm(double, double, double, double)
	 */
	@Override
	double additiveTerm(double x_i, double y_i, double root, double defaultValue) {
		return metric.additiveTerm(x_i, y_i, root, defaultValue);
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#getDefaultRoot()
	 */
	@Override
	public double getDefaultRoot() {
		return 3d;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#getName()
	 */
	@Override
	public String getName() {
		return "Relative " + metric.getName();
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#overallDistance(double, double, double)
	 */
	@Override
	double overallDistance(double distance, double root, double defaultValue) {
		return metric.overallDistance(distance, root, defaultValue);
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @return
	 * computes the relative distance between simulated data (x) and experimental data (y)
	 */
	public double distance(MultiBlockTable x, MultiBlockTable y) {
		MultiBlockTable experimental=y;
		if (x.getBlockCount() > y.getBlockCount()) {
			MultiBlockTable swap = y;
			y = x;
			x = swap;
			experimental=x;
		}
		double d1 = 0d;
		double d2 = 0d;
		for (int i = 0; i < x.getBlockCount(); i++) {
			d1 += distance(x.getBlock(i), y.getBlock(i));
			d2 += distance(experimental.getBlock(i));
		}
		double numerator=overallDistance(d1, getRoot(), getDefaultValue());
		double denominator=overallDistance(d2, getRoot(), getDefaultValue());
		if(denominator != 0) {
			return numerator/denominator;
		}
		else {
			return numerator;
		}
	}

}
