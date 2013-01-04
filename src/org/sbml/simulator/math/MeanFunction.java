/*
 * $Id:  meanFunction.java 11:25:15 keller$
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
package org.sbml.simulator.math;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Roland Keller
 * @version $Rev$
 * @since 1.0
 * @date 2011-03-25
 */
public abstract class MeanFunction implements Serializable {
	
	/**
	 * Generated serial identifier.
	 */
	private static final long serialVersionUID = -7272984374334773096L;
	
	/**
	 * @param values
	 * @return
	 */
	public abstract double computeMean(double... values);
	
	/**
	 * @param values
	 * @return
	 */
	public double computeMean(List<Double> values) {
		double[] val = new double[values.size()];
		for (int i = 0; i != val.length; i++) {
			val[i] = values.get(i);
		}
		return computeMean(val);
	}
	
	/**
	 * @param x
	 * @return
	 */
	public double computeMean(Iterable<? extends Number> values) {
		List<Double> val = new ArrayList<Double>();
		for (Number number : values) {
			val.add(number.doubleValue());
		}
		return computeMean(val);
	}

}
