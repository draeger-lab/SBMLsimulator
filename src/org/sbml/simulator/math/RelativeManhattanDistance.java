/*
 * $Id:  RelativeManhattan.java 10:15:03 keller$
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

/**
 * @author Roland Keller
 * @version $Rev$
 * @since 1.0
 */
public class RelativeManhattanDistance extends Relative_N_Metric {


	/**
	 * 
	 */
	private static final long serialVersionUID = -5446188147827736620L;

	/**
	 * 
	 */
	public RelativeManhattanDistance() {
		super(new ManhattanDistance());
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sbml.simulator.math.Relative_N_Metric#setRoot(double)
	 */
	public void setRoot(double root) {
		//root should not be changed
	}
	
}
