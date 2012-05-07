/*
 * $Id: IdentityMatrix.java 338 2012-02-17 07:03:11Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/SBMLsimulator/branches/Andreas/src/org/sbml/simulator/math/IdentityMatrix.java $
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
package org.sbml.simulator.stability.math;

/**
 * This Class represents a m x m identity matrix
 * 
 * @author Alexander D&ouml;rr
 * @version $Rev: 338 $
 * @since 1.0
 */
public class IdentityMatrix extends StabilityMatrix {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 5386269669997716715L;

	/**
	 * 
	 * @param m
	 */
	public IdentityMatrix(int m) {
		super(m, m, 0);
		setOne();
	}

	/**
	 * Sets in each row/column a one at the position characteristic for an
	 * identity matrix
	 */
	private void setOne() {
		for (int i = 0; i < this.getColumnDimension(); i++) {
			this.set(i, i, 1);
		}
	}

}
