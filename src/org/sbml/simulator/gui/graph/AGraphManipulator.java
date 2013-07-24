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
 * functions and constants. Each {@link IGraphManipulator} should be derived from this class.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public abstract class AGraphManipulator implements IGraphManipulator{
    
    /**
     * A {@link Logger} for this class.
     */
    private static final transient Logger logger = Logger.getLogger(AGraphManipulator.class.getName());
    
    /**
     * Pointer to the graph.
     */
    protected SBML2GraphML graph;
    
    /**
     * Default node size to revert changes of nodes. Can be changed by derived
     * classes.
     */
    protected double REVERT_NODE_SIZE = 8;
    
    /**
     * Default min/max node size for species.
     */
    public static final double DEFAULT_MIN_NODE_SIZE = 8, DEFAULT_MAX_NODE_SIZE = 50;
    
    /**
     * Default min/max line width for Reactions.
     */
    public static final float DEFAULT_MIN_LINEWIDTH = 1, DEFAULT_MAX_LINEWIDTH = 6;
    
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
     * Saves the used {@link DynamicCore}.
     */
    protected DynamicCore core;
    
    /**
     * Saves a mapping from every core element to it's specific min and max
     * value.
     */
    protected Map<String, double[]> id2minMaxData;
    
    /**
     * Saves minimum and maximum value of selected Reactions to save computation
     * time.
     */
    protected double[] minMaxOfselectedReactions;
    
    /**
     * Used linewidth for dynamic visualization of reactions.
     */
    private float minLineWidth = DEFAULT_MIN_LINEWIDTH,
            maxLineWidth = DEFAULT_MAX_LINEWIDTH;
    
    /**
     * Constructs an abstract graph manipulator.
     * @param graph
     * @param document
     * @param core
     */
    public AGraphManipulator(SBML2GraphML graph, SBMLDocument document, DynamicCore core) {
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
        this.core = core;
        id2minMaxData = core.getId2minMaxData();
    }
    
    /**
     * Constructs an abstract graph manipulator on the given
     * {@link SBML2GraphML} and {@link SBMLDocument}. Additionally this
     * constructor provides a basic implementation of dynamicChangeofReaction
     * method with the given parameters.
     * 
     * @param graph
     * @param document
     * @param core
     * @param selectedReactions
     * @param reactionsMinLineWidth
     * @param reactionsMaxLineWidth
     */
    public AGraphManipulator(SBML2GraphML graph, SBMLDocument document,
            DynamicCore core, String[] selectedReactions,
            float reactionsMinLineWidth, float reactionsMaxLineWidth) {
        this(graph, document, core);
        
        minMaxOfselectedReactions = core.getMinMaxOfIDs(selectedReactions);
        this.minLineWidth = reactionsMinLineWidth;
        this.maxLineWidth = reactionsMaxLineWidth;
    }

    /**
     * Maps the given value which has to be within [x1, x2] to codomain [y1,
     * y2]. This is done implicit by a linear regression through the points (x1,
     * y1) and (x2, y2). If x2 <= x1 (e.g. only one timepoint given) than a
     * point in the middle of y1 and y2 will be returned.
     * 
     * @param x1
     * @param x2
     * @param y1
     * @param y2
     * @param v
     * @return
     */
    protected double adjustValue(double x1, double x2, double y1, double y2,
            double v) {
        if (x2 <= x1) {
            /*
             * No proper limits given, return value inbetween codomain.
             */
            return (y1+y2) / 2.0;
        }
        
        return ((y2 - y1) / (x2 - x1)) * (v - x1) + y1;
    }
    
    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.IGraphManipulator#dynamicChangeOfReaction(java.lang.String, double)
     */
    @Override
    public void dynamicChangeOfReaction(String id, double value, boolean labels) {
        LinkedList<Edge> edgeList = graph.getId2edge()
                .get(id);
        if ((reactionID2reactionNode.get(id) != null) && (value != 0)) {
            ReactionNodeRealizer nr = (ReactionNodeRealizer) graph.getSimpleGraph().getRealizer(reactionID2reactionNode.get(id));
            // line width
            double absvalue = Math.abs(value);
            
            /*
             * Take absolute higher limit as xMax and 0 as xLow for regression.
             * Eventually reactions will end up in equillibrium.
             */
            double xHigh = Math.abs(minMaxOfselectedReactions[0]) > Math
                    .abs(minMaxOfselectedReactions[1]) ? Math.abs(minMaxOfselectedReactions[0]) : Math
                    .abs(minMaxOfselectedReactions[1]);
            float lineWidth = (float) adjustValue(0, xHigh, minLineWidth, maxLineWidth, absvalue);
            logger.finer(MessageFormat
                    .format("Reaction {0}: Abs. value={1}, computes to line width={2}",
                            new Object[] { id, absvalue, lineWidth }));
            
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
                        // if(document.getModel().getReaction(id).isReversible()) {
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
                        // if(document.getModel().getReaction(id).isReversible()) {
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
        } else if ((reactionID2reactionNode.get(id) != null) && (value == 0)) {
            revertChanges(id);
            
            logger.finer(MessageFormat.format(
                    "ReactionID: {0} value=0. Reverting id.",
                    new Object[] { id }));
            
            /*
             * If labels are enable show them anyway.
             */
            if (labels) {
                ReactionNodeRealizer nr = (ReactionNodeRealizer) graph
                        .getSimpleGraph().getRealizer(
                                reactionID2reactionNode.get(id));
                labelNode(nr, id, value);
                graph.getSimpleGraph().updateViews();
            }
        } else if (reactionID2reactionNode.get(id) == null) {
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
    protected void labelNode(NodeRealizer nr, String id, double value) {
        String name = "";
        if(document.getModel().getSpecies(id) != null) {
            name = document.getModel().getSpecies(id).isSetName() ? document
                    .getModel().getSpecies(id).getName() : id;
        }else if(document.getModel().getReaction(id) != null) {
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
    
    /**
     * Linear interpolation over two given colors.
     * 
     * @param percent [0,1]
     * @param RGBcolor1 low concentration (percent -> 0)
     * @param RGBcolor2 high concentration (percent -> 1)
     * @return
     */
    protected int[] linearColorInterpolation(double percent, int[] RGBcolor1,
            int[] RGBcolor2) {
        
        int[] outcolor = {0, 0, 0};
        if (percent >= 0 && percent <= 1) {
            for (int i = 0; i < outcolor.length; i++) {
                outcolor[i] = (int) (RGBcolor1[i] * percent + RGBcolor2[i]
                        * (1 - percent));
            }
            return outcolor;
        } else if (percent > 1) {
            //maybe round-off error
            return RGBcolor1;
        } else {
            //maybe round-off error
            return RGBcolor2;
        }
    }
    
    
    /**
     * Linear interpolation over three given colors. 
     * 
     * @param percent [0,1]
     * @param RGBcolor1 low concentration (percent -> 0)
     * @param RGBcolor2 mid concentration
     * @param RGBcolor3 high concentration (percent -> 1)
     * @return
     */
    protected int[] linearColorInterpolationForThree(double percent,
            int[] RGBcolor1, int[] RGBcolor2, int[] RGBcolor3) {
        
        if (percent >= 0 && percent <= 0.5) {
            // color interpolation between color2 (mid concentration) and color3
            // (low concentration)
            double resPercent = adjustValue(0, 0.5, 0, 1, percent);
            return linearColorInterpolation(resPercent, RGBcolor2, RGBcolor3);
        } else if (percent > 0.5 && percent <= 1.0) {
            //color interpolation between color1 (high concentration) and color2 (mid concentration)
            double resPercent = adjustValue(0.5, 1, 0, 1, percent);
            return linearColorInterpolation(resPercent, RGBcolor1, RGBcolor2);
        } else if (percent > 1) {
            // maybe round-off error
            return RGBcolor1;
        } else {
            // maybe round-off error
            return RGBcolor3;
        }
    }

    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.IGraphManipulator#revertChanges(java.lang.String)
     */
    @Override
    public void revertChanges(String id) {
        //revert nodes
        if (id2speciesNode.get(id) != null) {
            NodeRealizer nr = graph.getSimpleGraph().getRealizer(
                    graph.getId2node().get(id));
            double ratio = nr.getHeight() / nr.getWidth(); //keep ratio in case of elliptic nodes
            nr.setSize(REVERT_NODE_SIZE, REVERT_NODE_SIZE*ratio);
            nr.setFillColor(Color.LIGHT_GRAY);

            if (nr.labelCount() > 1) {
                // if not selected disable label
                nr.removeLabel(nr.getLabel(nr.labelCount() - 1));
            }
        }
        //revert reactions (lines and pseudonode)
        if(graph.getId2edge().get(id) != null) {
            LinkedList<Edge> listOfEdges = graph.getId2edge()
                    .get(id);
            for(Edge e : listOfEdges) {
                EdgeRealizer er = graph.getSimpleGraph().getRealizer(e);
                er.setLineType(LineType.LINE_1);
                if (document.getModel().getReaction(id).isReversible()) {
                    if (reactionID2reactionNode.get(id) == e.target()) {
                        er.setSourceArrow(Arrow.STANDARD);
                    } else {
                        er.setTargetArrow(Arrow.STANDARD);
                    }
                } else {
                    if (reactionID2reactionNode.get(id) == e.target()) {
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
