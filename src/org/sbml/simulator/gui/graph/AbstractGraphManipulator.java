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

import java.awt.Color;
import java.text.MessageFormat;

import y.view.NodeLabel;
import y.view.NodeRealizer;
import de.zbit.graph.io.SBML2GraphML;


/**
 * This class is an abstract graph manipulator, that provides some basic
 * functions. Each {@link GraphManipulator} should be derived from this class.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public abstract class AbstractGraphManipulator implements GraphManipulator{
    
    /**
     * Pointer to the graph.
     */
    protected SBML2GraphML graph;
    
    /**
     * Default node size used. Can be changed by derived classes.
     */
    protected double DEFAULT_NODE_SIZE = 8;

    /**
     * Constructs an abstract graph manipulator on the given {@link SBML2GraphML}.
     * @param graph
     */
    public AbstractGraphManipulator(SBML2GraphML graph){
        this.graph = graph;
    }
    
    /**
     * Linear regression through two given points (xLowerLimit, yLowerLimit) and
     * (xUpperLimit, yUpperLimit).
     * 
     * @param lowerDataLimit
     * @param upperDataLimit
     * @return first index of array represents the slope, second index the
     *         yintercept.
     */
    protected double[] computeBIAS(double xLowerLimit, double xUpperLimit, double yLowerLimit, double yUpperLimit){
        double slope = (yUpperLimit-yLowerLimit) / (xUpperLimit - xLowerLimit);
        double yintercept = yLowerLimit-slope*xLowerLimit;
        return new double[] { slope, yintercept };
    }
    
    /**
     * Labels the given node of this {@link NodeRealizer} with the given ID and value.
     * @param nr
     * @param id
     * @param value
     */
    protected void labelNode(NodeRealizer nr, String id, double value){
        if (nr.labelCount() > 1) {
            nr.getLabel(nr.labelCount() - 1).setText(
                    MessageFormat.format("{0}: {1,number,0.0000}",
                            new Object[] { id, value }));
        } else {
            nr.addLabel(new NodeLabel(MessageFormat.format(
                    "{0}: {1,number,0.0000}",
                    new Object[] { id, value })));
            NodeLabel nl = nr.getLabel(nr.labelCount() - 1);
            nl.setModel(NodeLabel.SIDES);
            nl.setPosition(NodeLabel.S); // South of node
            nl.setDistance(-3);
        }
    }
    
    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.GraphManipulator#revertChanges(java.lang.String)
     */
    @Override
    public void revertChanges(String id) {
        if (graph.getId2node().get(id) != null) {
            NodeRealizer nr = graph.getSimpleGraph().getRealizer(
                    graph.getId2node().get(id));
            nr.setSize(DEFAULT_NODE_SIZE, DEFAULT_NODE_SIZE);
            nr.setFillColor(Color.LIGHT_GRAY);

            if (nr.labelCount() > 1) {
                // if not selected disable label
                nr.removeLabel(nr.getLabel(nr.labelCount() - 1));
            }
        }
        graph.getSimpleGraph().updateViews();
    }
    
}
