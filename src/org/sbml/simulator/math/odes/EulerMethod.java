/*
 *  SBMLsqueezer creates rate equations for reactions in SBML files
 *  (http://sbml.org).
 *  Copyright (C) 2009 ZBIT, University of Tübingen, Andreas Dräger
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sbml.simulator.math.odes;

import eva2.tools.math.Mathematics;

/**
 * @author Andreas Dr&auml;ger
 * @date 14:37:21, 2010-08-03
 */
public class EulerMethod extends AbstractDESSolver {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 9094797527506196715L;

	/**
	 * 
	 */
	public EulerMethod() {
		super();
	}

	/**
	 * 
	 * @param stepSize
	 */
	public EulerMethod(double stepSize) {
		super(stepSize);
	}
	
	/**
	 * 
	 * @param stepSize
	 * @param nonnegative
	 */
	public EulerMethod(double stepSize, boolean nonnegative) {
		super(stepSize, nonnegative);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eva2.tools.math.des.AbstractDESSolver#getName()
	 */
	@Override
	public String getName() {
		return "Euler's method";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eva2.tools.math.des.AbstractDESSolver#computeChange(eva2.tools.math.des
	 * .DESystem, double[], double, double, double[])
	 */
	@Override
	public double[] computeChange(DESystem DES, double[] yPrev, double t,
			double stepSize, double[] change) throws IntegrationException {
		DES.getValue(t, yPrev, change);
		Mathematics.scale(stepSize, change);
		return change;
	}
}
