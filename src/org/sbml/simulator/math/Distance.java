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

import java.io.Serializable;
import java.util.Iterator;

import org.sbml.simulator.math.odes.MultiBlockTable;
import org.sbml.simulator.math.odes.MultiBlockTable.Block;
import org.sbml.simulator.util.ArrayIterator;

/**
 * This class is the basis of various implementations of distance functions.
 * 
 * @author Andreas Dr&auml;ger
 * @date 17.04.2007
 * @version $Rev$
 * @since 1.0
 */
public abstract class Distance implements Serializable {

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
	 * A value to express a parameter of the implementing class.
	 */
	protected double root;

	/**
	 * Default constructor. This sets the standard value for the parameter as
	 * given by the getStandardParameter() method. The default value is set to
	 * NaN.
	 */
	public Distance() {
		this.root = getDefaultRoot();
		this.defaultValue = Double.NaN;
	}

	/**
	 * Constructor, which allows setting the parameter value for root.
	 * 
	 * @param root
	 *            The parameter for this distance.
	 * @param defaultValue
	 */
	public Distance(double root, double defaultValue) {
		this.root = root;
		this.defaultValue = defaultValue;
	}

	/**
	 * The additive term to compute the distance when only two elements are
	 * given together with all default values.
	 * 
	 * @param x_i
	 * @param y_i
	 * @param root
	 * @param defaultValue
	 * @return
	 */
	abstract double additiveTerm(double x_i, double y_i, double root,
			double defaultValue);

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
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public double distance(MultiBlockTable x, MultiBlockTable y) {
		if (x.getBlockCount() > y.getBlockCount()) {
			MultiBlockTable swap = y;
			y = x;
			x = swap;
		}
		double d = 0d;
		for (int i = 0; i < x.getBlockCount(); i++) {
			d += distance(x.getBlock(i), y.getBlock(i));
		}
		return overallDistance(d, getRoot(), getDefaultValue());
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
	public double distance(MultiBlockTable.Block x, MultiBlockTable.Block y) {
		if (x.getColumnCount() > y.getColumnCount()) {
			MultiBlockTable.Block swap = y;
			y = x;
			x = swap;
		}
		double d = 0d;
		String identifiers[] = x.getIdentifiers();
		for (int i = 0; i < identifiers.length; i++) {
			d += distance(x.getColumn(i), y.getColumn(identifiers[i]));
		}
		return d;
	}
	
	/**
	 * Computes the distance of a matrix to a zero matrix
	 * 
	 * @param block
	 * @return
	 */
	public double distance(Block block) {
		Double[][] data1 = new Double[block.getData().length][]; 
		Double[][] data2 = new Double[block.getData().length][];
		for(int i=0;i!=data1.length;i++) {
			data1[i]=new Double[block.getData()[i].length];
			data2[i]=new Double[block.getData()[i].length];
			for(int j=0;j!=data1[i].length;j++) {
				data1[i][j]=0d;
				data2[i][j]=block.getData()[i][j];
			}
		}
		return distance(data1,data2);
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public double distance(Double x[][], Double y[][]) {
		double d = 0;
		int j = 0;
		for (Double[] x_i : x) {
			d += distance(new ArrayIterator<Double>(x_i),
					new ArrayIterator<Double>(y[j++]));
		}
		return d;
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
		return distance(x, y, root, defaultValue);
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
	 * @param root
	 *            Some necessary parameter.
	 * @param defaultValue
	 *            The value to be returned in cases in which no distance
	 *            computation is possible.
	 * @return The distance between the two arrays x and y.
	 * @throws IllegalArgumentException
	 */
	public double distance(Iterable<? extends Number> x,
			Iterable<? extends Number> y, double root, double defaultValue) {
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
		return overallDistance(d, root, defaultValue);
	}

	/**
	 * Computes the distance between two-dimensional {@link Iterable} elements.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public double distance(Iterable<Iterable<? extends Number>> x,
			Iterator<Iterable<? extends Number>> y) {
		double d = 0;
		for (Iterable<? extends Number> i : x) {
			d += distance(i, y.next());
		}
		return d;
	}

	/**
	 * Returns the default value for the parameter to compute the distance.
	 * 
	 * @return The root value of this {@link Distance} measure to be used if no
	 *         other value has been set.
	 */
	public abstract double getDefaultRoot();

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
	 * The name of this distance measurement.
	 * 
	 * @return A human-readable name representing the specific distance measure.
	 */
	public abstract String getName();

	/**
	 * Returns the currently set root or default value for the distance
	 * function.
	 * 
	 * @return
	 */
	public double getRoot() {
		return this.root;
	}

	/**
	 * This method allows to change the value of an already computed distance
	 * with the help of the given default values.
	 * 
	 * @param distance
	 * @param root
	 * @param defaultValue
	 * @return
	 */
	abstract double overallDistance(double distance, double root,
			double defaultValue);

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
	 * Set the current root to be used in the distance function to the specified
	 * value.
	 * 
	 * @param root
	 */
	public void setRoot(double root) {
		this.root = root;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("%s, root = %s, default = %s", getName(),
				getRoot(), getDefaultValue());
	}

}
