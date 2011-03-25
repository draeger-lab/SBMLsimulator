/*
 * $Id:  Euclidean.java 09:56:46 keller$
 * $URL: Euclidean.java $
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

/**
 * @author Roland Keller
 * @version $Rev$
 * @since
 */
public class Euclidean extends NMetric {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2520265250898674233L;
	
	/**
	 * 
	 */
	public Euclidean() {
		super(2d);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sbml.squeezer.math.Distance#getName()
	 */
	@Override
	public String getName() {
		return "Euclidean";
	}


}
