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

import org.sbml.jsbml.util.StringTools;

/**
 * An implementation of the relative squared error with a default value to avoid
 * division by zero. Actually, the exponent in this error function is 2 (squared
 * error). Irrespectively, it is possible to set the exponent to different
 * values.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2007-04-17
 * @version $Rev$
 * @since 1.0
 */
public class RelativeSquaredError extends N_Metric {

	/**
	 * Generated serial identifier.
	 */
	private static final long serialVersionUID = 1643317436479699973L;

	/**
	 * Constructs a new RelativeSquaredError. Here
	 * the root is the default value to be returned by the distance function.
	 */
	public RelativeSquaredError() {
		super(2d);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.squeezer.math.Distance#additiveTerm(double, double, double,
	 * double)
	 */
	@Override
	double additiveTerm(double x, double y, double root, double defaultValue) {
		return (y != 0d) ? Math.pow(Math.abs(x - y) / y, root) : Math.abs(x - y);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.squeezer.math.Distance#getName()
	 */
	@Override
	public String getName() {
		String name = super.getName();
		if (getRoot() != 2d) {
			name = String.format(
				"%s, %s = %s", name, StringTools.toString(getRoot()));
		}
		return name;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.squeezer.math.NMetric#overallDistance()
	 */
	@Override
	double overallDistance(double distance, double root, double defaultValue) {
		return distance;
	}

}
