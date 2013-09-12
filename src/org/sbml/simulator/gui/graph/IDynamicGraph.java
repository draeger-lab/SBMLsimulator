/*
 * $Id$
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
package org.sbml.simulator.gui.graph;

import java.awt.image.BufferedImage;

import org.simulator.math.odes.MultiTable;

/**
 * Interface to ensure any {@link DynamicCore} observer provides the necessary
 * methods and eventually identifies the implementing class as a view in
 * MVC-pattern.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public interface IDynamicGraph {

    /**
	 * Method invoked by related {@link DynamicCore} when play thread is done.
	 */
	public void donePlay();
	
	/**
     * Return a {@link BufferedImage} with given resolution of currently
     * displayed Graph.
     * @param width
     * @param height
     * @return
     */
	public BufferedImage takeGraphshot(int width, int height);
	
	/**
     * Method invoked by related {@link DynamicCore} when time point changes.
     * @param timepoint
     * @param updateThem
     */
	public void updateGraph(double timepoint, MultiTable updateThem);
	
}
