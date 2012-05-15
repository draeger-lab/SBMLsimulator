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

import org.sbml.jsbml.SBMLDocument;

import y.base.Edge;
import y.base.Node;
import y.view.Arrow;
import y.view.EdgeRealizer;
import y.view.LineType;
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
     * Default line width used. Can be changed by derived classes.
     */
    protected float DEFAULT_LINE_WIDTH = 1;
    
    /**
     * Maximum line width for default DynamicChangeOfReaction.
     */
    private float maxLineWidth = 6;
    
    /**
     * Minimum line width for default DynamicChangeOfReaction.
     */
    private float minLineWidth = (float) 0.1;
    
    /**
     * Parameters for default DynamicChangeOfReaction.
     */
    private double m = 1, c = 0;
    
    /**
     * Saves mapping from reactionIDs to related reaction nodes.
     */
    protected Map<String, Node> reactionID2reactionNode;
    
    /**
     * Saves the used {@link SBMLDocument}.
     */
    protected SBMLDocument document;

    /**
     * Constructs an abstract graph manipulator on the given {@link SBML2GraphML}.
     * @param graph
     */
    
    /**
     * Constructs an abstract graph manipulator on the given {@link SBML2GraphML} and {@link SBMLDocument}.
     * @param graph
     * @param document
     */
    public AbstractGraphManipulator(SBML2GraphML graph, SBMLDocument document){
        this.graph = graph;
        reactionID2reactionNode = graph.getReactionID2reactionNode();
        this.document = document;
    }
    
    /**
     * Constructs an abstract graph manipulator on the given {@link SBML2GraphML} and {@link SBMLDocument}.
     * Additionally this constructor provides a proper dynamicChangeofReaction method.
     * 
     * @param graph
     * @param document
     * @param minDataReaction
     * @param maxDataReaction
     */
    public AbstractGraphManipulator(SBML2GraphML graph, SBMLDocument document, double minDataReaction, double maxDataReaction){
        this(graph, document);
        /*
         * Take absolute higher limit as xMax and 0 as xLow for regression.
         * Eventually reactions will end up in equillibrium.
         */
        computeReactionAdjusting(minDataReaction, minDataReaction);
    }
    
    /**
     * Computes the adjusting values for given limits.
     * @param lowerReactionLimit
     * @param upperReactionLimit
     */
    private void computeReactionAdjusting(double lowerReactionLimit, double upperReactionLimit){
        double xHigh = Math.abs(lowerReactionLimit) > Math.abs(upperReactionLimit) ? Math
                .abs(lowerReactionLimit) : Math.abs(upperReactionLimit);
        double[] linearRegression = computeBIAS(0, xHigh, minLineWidth, maxLineWidth);
        m = linearRegression[0];
        c = linearRegression[1];
    }
    
    /**
     * Linear regression through two given points (xLowerLimit, yLowerLimit) and
     * (xUpperLimit, yUpperLimit).
     * 
     * @param xLowerLimit
     * @param xUpperLimit
     * @param yLowerLimit
     * @param yUpperLimit
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
        String label = MessageFormat.format("{0}: {1,number,0.0000}",
                new Object[] { document.getModel().getSpecies(id).isSetName() ? document.getModel().getSpecies(id).getName() : id , value });
        
        if (nr.labelCount() > 1) {
            nr.getLabel(nr.labelCount() - 1).setText(label);
        } else {
            nr.addLabel(new NodeLabel(label));
            NodeLabel nl = nr.getLabel(nr.labelCount() - 1);
            nl.setModel(NodeLabel.SIDES);
            nl.setPosition(NodeLabel.S); // South of node
            nl.setDistance(-3);
        }
    }
    
    protected void labelReaction(){
        //TODO
    }
    
    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.GraphManipulator#dynamicChangeOfReaction(java.lang.String, double)
     */
    @Override
    public void dynamicChangeOfReaction(String id, double value) {
        LinkedList<Edge> edgeList = graph.getId2edge()
                .get(id);
        
        for(Edge e : edgeList){
            EdgeRealizer er = graph.getSimpleGraph().getRealizer(e);

            /*
             *  adjust lineWidth only if regression was computed, else use
             *  standard linewidth.
             */
            double absvalue = Math.abs(value);
            float lineWidth = (m != 1) ?  (float)(absvalue * m + c) : er.getLineType().getLineWidth();
            //only line-width is supposed to change
            LineType currLinetype = er.getLineType();
            LineType newLineType = LineType.createLineType(
                    lineWidth, currLinetype.getEndCap(),
                    currLinetype.getLineJoin(), currLinetype.getMiterLimit(),
                    currLinetype.getDashArray(), currLinetype.getDashPhase());
            
            er.setLineType(newLineType);
            
            /*
             * determine arrowdirection
             */
            if (value > 0) { // (!= 0), because simulation data at timepoint 0.0 = 0
                if (reactionID2reactionNode.get(id) == e.target()) {
                    // reactants
                    er.setSourceArrow(Arrow.NONE);
                    //TODO own arrow for reversible
//                    if(document.getModel().getReaction(id).isReversible()){
//                        er.setSourceArrow(Arrow.PLAIN);
//                    } else {
//                        er.setSourceArrow(Arrow.NONE);
//                    }
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
                    //TODO own arrow for reversible
//                    if(document.getModel().getReaction(id).isReversible()){
//                        er.setTargetArrow(Arrow.PLAIN);
//                    } else {
//                        er.setTargetArrow(Arrow.NONE);
//                    }
                }
            }
        }
        graph.getSimpleGraph().updateViews();
    }
    
    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.GraphManipulator#revertChanges(java.lang.String)
     */
    @Override
    public void revertChanges(String id) {
        //revert nodes
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
        //revert lines
        if(graph.getId2edge().get(id) != null){
            LinkedList<Edge> listOfEdges = graph.getId2edge()
                    .get(id);
            for(Edge e : listOfEdges){
                EdgeRealizer er = graph.getSimpleGraph().getRealizer(e);
                er.setLineType(LineType.LINE_1);
                if (document.getModel().getReaction(id).isReversible()) {
                    if (reactionID2reactionNode.get(id) == e.target()){
                        er.setSourceArrow(Arrow.STANDARD);
                    } else {
                        er.setTargetArrow(Arrow.STANDARD);
                    }
                } else {
                    if (reactionID2reactionNode.get(id) == e.target()){
                        er.setSourceArrow(Arrow.NONE);
                    } else {
                        er.setTargetArrow(Arrow.STANDARD);
                    }
                }
            }
        }
        graph.getSimpleGraph().updateViews();
    }
    
}
