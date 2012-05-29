/*
 * $Id$
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

import java.io.Serializable;
import java.util.ArrayList;

import org.simulator.math.odes.MultiTable;

/**
 * This class is the basis of various implementations of distance functions.
 * 
 * @author Roland Keller
 * @author Andreas Dr&auml;ger
 * @date 17.04.2007
 * @version $Rev$
 * @since 1.0
 */
public abstract class QualityMeasure implements Serializable {

	/**
	 * Generated serial identifier.
	 */
	private static final long serialVersionUID = -1923357284664688319L;

	/**
	 * The return value of the distance function in cases where the distance
	 * cannot be computed.
	 */
	protected double defaultValue;
	
	/**
	 * 
	 */
	protected MeanFunction meanFunction;

	/**
	 * Default constructor. This sets the standard value for the parameter as
	 * given by the getStandardParameter() method. The default value is set to
	 * NaN.
	 */
	public QualityMeasure() {
		this.defaultValue = Double.NaN;
		meanFunction=new ArithmeticMean();
	}

	/**
	 * Constructor, which allows setting the parameter value for default value.
	 * 
	 * @param defaultValue
	 */
	public QualityMeasure(double defaultValue) {
		this.defaultValue = defaultValue;
		meanFunction=new ArithmeticMean();
	}

	/**
	 * Constructor, which allows setting the parameter values for meanFunction and default value.
	 * 
	 * @param defaultValue
	 * @param meanFunction
	 */
	public QualityMeasure(double defaultValue, MeanFunction mean) {
		this.defaultValue = defaultValue;
		this.meanFunction=mean;
	}

	/**
	 * This method decides whether or not to consider the given values for the
	 * computation of a distance. This method checks if both arguments x_i and
	 * y_i are not {@link Double.NaN} and differ from each other. If other
	 * conditions should be checked, this method can be overridden.
	 * 
	 * @param x_i
	 * @param y_i
	 * @param root
	 * @param defaultValue
	 * @return True if the given values x_i and y_i are valid and should be
	 *         considered to compute the distance.
	 */
	boolean computeDistanceFor(double x_i, double y_i, double root,
			double defaultValue) {
		return !Double.isNaN(y_i) && !Double.isNaN(x_i) && (y_i != x_i);
	}
	
	/**
	 * Returns the distance of the two vectors x and y where the currently set
	 * root is used. This can be obtained by invoking the {@see getRoot} method.
	 * It is possible that one matrix contains more columns than the other one.
	 * If so, the additional values in the bigger matrix are ignored and do not
	 * contribute to the distance. <code>NaN</code> values do also not
	 * contribute to the distance.
	 * 
	 * @param x
	 * @param y
	 * @return
	 * @throws IllegalArgumentException
	 */
	public double distance(Iterable<? extends Number> x,
			Iterable<? extends Number> y) {
		return distance(x, y,defaultValue);
	}

	/**
	 * Returns the distance of the two vectors x and y with the given root. This
	 * may be the root in a formal way or a default value to be returned if the
	 * distance uses a non defined operation. If one array is longer than the
	 * other one additional values do not contribute to the distance.
	 * {@link Double.NaN} values are also ignored.
	 * 
	 * @param x
	 *            an array
	 * @param y
	 *            another array
	 * @param defaultValue
	 *            The value to be returned in cases in which no distance
	 *            computation is possible.
	 * @return The distance between the two arrays x and y.
	 * @throws IllegalArgumentException
	 */
	public abstract double distance(Iterable<? extends Number> x,
			Iterable<? extends Number> y,double defaultValue);

	/**
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public double distance(MultiTable x, MultiTable expected) {
		MultiTable left = x;
		MultiTable right = expected;
		if (x.isSetTimePoints() && expected.isSetTimePoints()) {
			left = x.filter(expected.getTimePoints());
			right = expected.filter(x.getTimePoints());
		}
		
		ArrayList<Double> distances = new ArrayList<Double>();
		for (int i = 0; i < Math.min(left.getBlockCount(), right.getBlockCount()); i++) {
			distances.addAll(getColumnDistances(left.getBlock(i), right.getBlock(i)));
		}
		return meanFunction.computeMean(distances);
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public double distance(MultiTable.Block x, MultiTable.Block expected) {
		return meanFunction.computeMean(getColumnDistances(x,expected));
	}

	/**
	 * Computes the distance of two matrices as the sum of the distances of each
	 * row. It is possible that one matrix contains more columns than the other
	 * one. If so, the additional values in the bigger matrix are ignored and do
	 * not contribute to the distance. {@link Double.NaN} values do also not
	 * contribute to the distance. Only columns with matching identifiers are
	 * considered for the distance computation.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public ArrayList<Double> getColumnDistances(MultiTable.Block x, MultiTable.Block expected) {
		String identifiers[] = x.getIdentifiers();
		ArrayList<Double> distances= new ArrayList<Double>();
		
		for (int i = 0; i < identifiers.length; i++) {
			if(identifiers[i]!=null && expected.containsColumn(identifiers[i])) {
				distances.add(distance(x.getColumn(i), expected.getColumn(identifiers[i])));
			}
		}
		return distances;
	}
	
	/**
	 * Returns the default value that is returned by the distance function in
	 * cases in which the computation of the distance is not possible.
	 * 
	 * @return
	 */
	public double getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @return the meanFunction
	 */
	public final MeanFunction getMeanFunction() {
	    return meanFunction;
	}	

	/**
	 * The name of this distance measurement.
	 * 
	 * @return A human-readable name representing the specific distance measure.
	 */
	public abstract String getName();

	/**
	 * Set the value to be returned by the distance function in cases, in which
	 * no distance can be computed.
	 * 
	 * @param defaultValue
	 */
	public void setDefaultValue(double defaultValue) {
		this.defaultValue = defaultValue;
	}
	
	/**
	 * @param meanFunction the meanFunction to set
	 */
	public final void setMeanFunction(MeanFunction meanFunction) {
	    this.meanFunction = meanFunction;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public abstract String toString();

}
