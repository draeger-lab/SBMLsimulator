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
package org.sbml.simulator.util;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * Provides an {@link Iterator} and {@link Enumeration} for a given array of
 * type T. Furthermore, this class implements {@link Iterable}. Hence, this is a
 * flexible class for iterating over arrays of arbitrary types.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-08-27
 * @version $Rev$
 * @since 1.0
 */
public class ArrayIterator<T> implements Iterable<T>, Iterator<T>,
		Enumeration<T> {

	/**
	 * The array to be iterated
	 */
	private T[] array;
	/**
	 * The current position during the iteration
	 */
	private int currPos;

	/**
	 * 
	 */
	public ArrayIterator(T[] array) {
		this.array = array;
		this.currPos = 0;
	}

	/**
	 * Grants access to the underlying array.
	 * 
	 * @return The original array for random access.
	 */
	public T[] getArray() {
		return array;
	}

	/**
	 * Yields the index within the array at the current state.
	 * 
	 * @return The current position within the array during the iteration
	 *         process.
	 */
	public int getCurrentPostition() {
		return currPos;
	}

	/**
	 * Gives the number of elements in the underlying array.
	 * 
	 * @return The total number of elements in the iteration process.
	 */
	public int getElementCount() {
		return array.length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Enumeration#hasMoreElements()
	 */
	public boolean hasMoreElements() {
		return hasNext();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext() {
		return currPos < array.length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<T> iterator() {
		return new ArrayIterator<T>(array);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Iterator#next()
	 */
	public T next() {
		return array[currPos++];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Enumeration#nextElement()
	 */
	public T nextElement() {
		return next();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Iterator#remove()
	 */
	public void remove() {
		// don't remove anything!
		throw new UnsupportedOperationException(
				"cannot remove anything from the underlying object");
	}

}
