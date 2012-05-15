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
package org.sbml.simulator.gui.graph;

import de.zbit.graph.gui.TranslatorSBMLgraphPanel;

/**
 * This interface gathers all methods to change the visualization of a displayed
 * graph on a {@link TranslatorSBMLgraphPanel}. Therefore every class that
 * manipulates the graph should implement this interface and override the
 * methods.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public interface GraphManipulator {

    /**
     * Changes the visualization of a given node.
     * @param id
     * @param value
     * @param labels
     */
    public void dynamicChangeOfNode(String id, double value, boolean labels);
    
    /**
     * Changes the visualization of a given reaction.
     * @param id
     * @param value
     */
    public void dynamicChangeOfReaction(String id, double value, boolean labels);
    
    /**
     * Reverts the changes for the given id.
     * @param id
     */
    public void revertChanges(String id);    
   
}
