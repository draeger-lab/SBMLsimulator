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
package org.sbml.simulator.gui;

import org.sbml.simulator.gui.plot.Plot;


/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class PlotTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Plot p = new Plot(); //"Test Plot", "X", "Y");
		int length = 100;
		for (int i=1; i<length; i++) {
			double x = i;
			x /= length;
			p.setConnectedPoint(x, Math.log(x), 1);
			p.setUnconnectedPoint(x, Math.sin(x), 2);
		}
		p.setInfoString(1, "Log", 1);
		p.setInfoString(2, "Log", 1);
		p.setShowLegend(true);
	}

}
