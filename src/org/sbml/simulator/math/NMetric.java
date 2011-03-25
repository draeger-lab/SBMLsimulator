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
package org.sbml.simulator.math;

import java.util.Iterator;


/**
 * An implementation of an n-metric. An n-metric is basically the n-th root of
 * the sum of the distances of every single element in two vectors (arrays),
 * where this distance will always be exponentiated by the value of n.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2007-04-17
 * @version $Rev$
 * @since 1.0
 */
public class NMetric extends Distance {

	/**
	 * Generated serial identifier.
	 */
	private static final long serialVersionUID = -216525074796086162L;
	protected double root;

	/**
	 * Constructs a new NMetric with a default root of two. This will result in
	 * the Euclidean distance. Other metrics can be used by either setting the
	 * root to another value or explicitly using the distance function where the
	 * root must be given as an argument.
	 */
	public NMetric() {
		super();
		this.root=3;
	}

	/**
	 * Constructs a new NMetric with a costumized root. Depending on the values
	 * of root this results in different metrics. Some are especially important:
	 * <ul>
	 * <li>one is the Manhatten Norm or the city block metric.</li>
	 * <li>two is the Euclidean metric.</li>
	 * <li>Infinity is the maximum norm.</li>
	 * </ul>
	 * 
	 * @param root
	 */
	public NMetric(double root) {
		super(Double.NaN);
		this.root=root;
	}

	double additiveTerm(double x_i, double y_i, double root, double defaultValue) {
		return Math.pow(Math.abs(x_i - y_i), root);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.squeezer.math.Distance#getName()
	 */
	@Override
	public String getName() {
		return "N-metric";
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#distance(java.lang.Iterable, java.lang.Iterable, double)
	 */
	@Override
	public double distance(Iterable<? extends Number> x,
		Iterable<? extends Number> y, double defaultValue) {
		if(root==0d) {
			return defaultValue;
		}
		double d = 0;
		double x_i;
		double y_i;
		Iterator<? extends Number> yIterator = y.iterator();
		for (Number number : x) {
			if (!yIterator.hasNext()) {
				break;
			}
			x_i = number.doubleValue();
			y_i = yIterator.next().doubleValue();
			if (computeDistanceFor(x_i, y_i, root, defaultValue)) {
				d += additiveTerm(x_i, y_i, root, defaultValue);
			}
		}
		return overallDistance(d,root,defaultValue);
	}
	
	double overallDistance(double distance, double root, double defaultValue) {
		return Math.pow(distance, 1d / root);
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.math.Distance#toString()
	 */
	@Override
	public String toString() {
		return getName() + " distance";
	}
	
	public double getRoot() {
		return root;
	}

}
