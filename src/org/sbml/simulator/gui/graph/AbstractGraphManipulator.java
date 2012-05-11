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
import java.util.LinkedList;
import java.util.Map;

import org.sbml.jsbml.Reaction;

import y.base.Edge;
import y.base.Node;
import y.view.Arrow;
import y.view.EdgeRealizer;
import y.view.LineType;
import y.view.NodeLabel;
import y.view.NodeRealizer;
import de.zbit.graph.io.SBML2GraphML;
import de.zbit.graph.sbgn.ReactionNodeRealizer;


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
     * Saves mapping from reactionIDs to related reaction nodes.
     */
    protected Map<String, Node> reactionID2reactionNode;

    /**
     * Constructs an abstract graph manipulator on the given {@link SBML2GraphML}.
     * @param graph
     */
    public AbstractGraphManipulator(SBML2GraphML graph){
        this.graph = graph;
        reactionID2reactionNode = graph.getReactionID2reactionNode();
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
     * @see org.sbml.simulator.gui.graph.GraphManipulator#dynamicChangeOfReaction(java.lang.String, double)
     */
    @Override
    public void dynamicChangeOfReaction(String id, double value) {
        System.out.println(value);
        LinkedList<Edge> listOfEdges = graph.getId2edge()
                .get(id);
        
        for(Edge e : listOfEdges){
            EdgeRealizer er = graph.getSimpleGraph().getRealizer(e);
            
            if (value > 0) { // (!= 0), because computed data at timepoint 0.0 = 0
                if (reactionID2reactionNode.get(id) == e.target()) {
                    // reactants
                    er.setSourceArrow(Arrow.NONE);
                } else {
                    // products
                    er.setTargetArrow(Arrow.STANDARD);
                }
            } else {
                if (reactionID2reactionNode.get(id) == e.target()) {
                    // products
                    er.setSourceArrow(Arrow.STANDARD);
                } else {
                    // reactants
                    er.setTargetArrow(Arrow.NONE);
                }
            }
        }
        
        //TODO 2 cases: value > 0 & value < 0
//        for (Edge e : listOfEdges) {
//            float valueF = (float) value;
//            LineType currLinetype = graph.getSimpleGraph()
//                    .getRealizer(e).getLineType();
//            LineType newLineType = LineType.createLineType(valueF,
//                    currLinetype.getEndCap(), currLinetype.getLineJoin(),
//                    currLinetype.getMiterLimit(), currLinetype.getDashArray(),
//                    currLinetype.getDashPhase());
//            graph.getSimpleGraph().getRealizer(e)
//                    .setLineType(newLineType);
//        }

        graph.getSimpleGraph().updateViews();
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
