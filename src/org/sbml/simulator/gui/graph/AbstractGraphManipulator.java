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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

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
     * A {@link Logger} for this class.
     */
    private static final transient Logger logger = Logger.getLogger(AbstractGraphManipulator.class.getName());
    
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
     * Parameters for default DynamicChangeOfReaction.
     */
    private double m = 1, c = 0;
    
    /**
     * Saves mapping from reactionIDs to related reaction nodes.
     */
    protected Map<String, Node> reactionID2reactionNode;
    
    /**
     * Saves mapping from speciesID to related species nodes.
     */
    protected Map<String, Node> id2speciesNode;
    
    /**
     * Saves the used {@link SBMLDocument}.
     */
    protected SBMLDocument document;
    
    /**
     * Constructs an abstract graph manipulator.
     * @param graph
     * @param document
     */
    public AbstractGraphManipulator(SBML2GraphML graph, SBMLDocument document){
        this.graph = graph;
        reactionID2reactionNode = new HashMap<String, Node>();
        id2speciesNode = new HashMap<String, Node>();
        if (document.isSetModel()) {
        	Model m = document.getModel();
        	for (Map.Entry<String, Node> entry : graph.getId2node().entrySet()) {
        	    //generate mapping from id to reactionnode
        		if (m.containsReaction(entry.getKey())) {
        			reactionID2reactionNode.put(entry.getKey(), entry.getValue());
        		}
        		//generate mapping from id to speciesnode
        		if (m.containsSpecies(entry.getKey())) {
        		    id2speciesNode.put(entry.getKey(), entry.getValue());
        		}
        	}
        }
        this.document = document;
    }
    
    /**
     * Constructs an abstract graph manipulator on the given
     * {@link SBML2GraphML} and {@link SBMLDocument}. Additionally this
     * constructor provides a basic implementation of dynamicChangeofReaction
     * method with the given parameters.
     * 
     * @param graph
     * @param document
     * @param minMaxOfReactionsData
     * @param reactionsMinLineWidth
     * @param reactionsMaxLineWidth
     */
    public AbstractGraphManipulator(SBML2GraphML graph, SBMLDocument document,
            double[] minMaxOfReactionsData, float reactionsMinLineWidth,
            float reactionsMaxLineWidth) {
        this(graph, document);
        /*
         * Take absolute higher limit as xMax and 0 as xLow for regression.
         * Eventually reactions will end up in equillibrium.
         */
        computeReactionAdjusting(minMaxOfReactionsData[0],
                minMaxOfReactionsData[1], reactionsMinLineWidth, reactionsMaxLineWidth);
    }
    
    /**
     * Linear regression for two given points (xLowerLimit, yLowerLimit) and
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
     * Computes the adjusting values for given limits.
     * @param lowerReactionLimit
     * @param upperReactionLimit
     * @param minLineWidth
     * @param maxLineWidth
     */
    private void computeReactionAdjusting(double lowerReactionLimit,
            double upperReactionLimit, float minLineWidth, float maxLineWidth) {
        double xHigh = Math.abs(lowerReactionLimit) > Math
                .abs(upperReactionLimit) ? Math.abs(lowerReactionLimit) : Math
                .abs(upperReactionLimit);
        double[] linearRegression = computeBIAS(0, xHigh, minLineWidth,
                maxLineWidth);
        m = linearRegression[0];
        c = linearRegression[1];
    }
    
    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.GraphManipulator#dynamicChangeOfReaction(java.lang.String, double)
     */
    @Override
    public void dynamicChangeOfReaction(String id, double value, boolean labels) {
        LinkedList<Edge> edgeList = graph.getId2edge()
                .get(id);
        if (reactionID2reactionNode.get(id) != null) {
            ReactionNodeRealizer nr = (ReactionNodeRealizer) graph.getSimpleGraph().getRealizer(reactionID2reactionNode.get(id));
            // line width
            double absvalue = Math.abs(value);
            float lineWidth = (m != 1) ? (float) (absvalue * m + c) : 1;
            logger.finer(MessageFormat
                    .format("Reaction {0}: Abs. value={1}, m={2}, c={3}, computes to line width={4}",
                            new Object[] { id, absvalue, m, c, lineWidth }));
            
            // ReactionNode line width
            nr.setLineWidth(lineWidth);

            // edges line width
            for (Edge e : edgeList) {
                EdgeRealizer er = graph.getSimpleGraph().getRealizer(e);

                /*
                 * adjust lineWidth only if regression was computed, else use
                 * standard linewidth.
                 */
                // only line-width is supposed to change
                LineType currLinetype = er.getLineType();
                LineType newLineType = LineType.createLineType(lineWidth,
                        currLinetype.getEndCap(), currLinetype.getLineJoin(),
                        currLinetype.getMiterLimit(),
                        currLinetype.getDashArray(),
                        currLinetype.getDashPhase());

                er.setLineType(newLineType);

                /*
                 * determine arrowdirection
                 */
                if (value > 0) { // (!= 0), because simulation data at timepoint
                                 // 0.0 = 0
                    if (reactionID2reactionNode.get(id) == e.target()) {
                        // reactants
                        er.setSourceArrow(Arrow.NONE);
                        // TODO own arrow for reversible
                        // if(document.getModel().getReaction(id).isReversible()){
                        // er.setSourceArrow(Arrow.PLAIN);
                        // } else {
                        // er.setSourceArrow(Arrow.NONE);
                        // }
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
                        // TODO own arrow for reversible
                        // if(document.getModel().getReaction(id).isReversible()){
                        // er.setTargetArrow(Arrow.PLAIN);
                        // } else {
                        // er.setTargetArrow(Arrow.NONE);
                        // }
                    }
                }
            }
            /*
             * Label Node with ID and real value at this timepoint. Last label
             * will be treated as dynamic label
             */
            if (labels) {
                labelNode(nr, id, value);
            } else if (nr.labelCount() > 1) {
                // labels switched off, therefore remove them, if there are any
                nr.removeLabel(nr.getLabel(nr.labelCount() - 1));
            }
            graph.getSimpleGraph().updateViews();
        } else {
            logger.finer(MessageFormat.format(
                    "No ReactionNodeRealizer for ReactionID: {0}",
                    new Object[] { id }));
        }
    }
    
    /**
     * Labels the given node of this {@link NodeRealizer} with the given ID and value.
     * @param nr
     * @param id
     * @param value
     */
    protected void labelNode(NodeRealizer nr, String id, double value){
        String name = "";
        if(document.getModel().getSpecies(id) != null){
            name = document.getModel().getSpecies(id).isSetName() ? document
                    .getModel().getSpecies(id).getName() : id;
        }else if(document.getModel().getReaction(id) != null){
            name = document.getModel().getReaction(id).isSetName() ? document
                    .getModel().getReaction(id).getName() : id;
        }
        
        String label = MessageFormat.format("{0}: {1,number,0.0000}",
                new Object[] { name , value });
        
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
    
    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.GraphManipulator#revertChanges(java.lang.String)
     */
    @Override
    public void revertChanges(String id) {
        //revert nodes
        if (id2speciesNode.get(id) != null) {
            NodeRealizer nr = graph.getSimpleGraph().getRealizer(
                    graph.getId2node().get(id));
            double ratio = nr.getHeight() / nr.getWidth(); //keep ratio in case of elliptic nodes
            nr.setSize(DEFAULT_NODE_SIZE, DEFAULT_NODE_SIZE*ratio);
            nr.setFillColor(Color.LIGHT_GRAY);

            if (nr.labelCount() > 1) {
                // if not selected disable label
                nr.removeLabel(nr.getLabel(nr.labelCount() - 1));
            }
        }
        //revert reactions (lines and pseudonode)
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
            
            if (reactionID2reactionNode.get(id) != null) { 
                ReactionNodeRealizer nr = (ReactionNodeRealizer) graph
                        .getSimpleGraph().getRealizer(
                                reactionID2reactionNode.get(id));
                nr.setLineWidth(1);
                if (nr.labelCount() > 1) {
                    // if not selected disable label
                    nr.removeLabel(nr.getLabel(nr.labelCount() - 1));
                }
            } else {
                logger.finer(MessageFormat.format(
                        "No ReactionNodeRealizer for ReactionID: {0}",
                        new Object[] { id }));
            }
        }
        graph.getSimpleGraph().updateViews();
    }
    
}
