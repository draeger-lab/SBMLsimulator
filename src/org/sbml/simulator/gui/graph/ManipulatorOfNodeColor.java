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
 * This class is a {@link GraphManipulator}. It changes the color of the given
 * nodes and width of given reactions.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class ManipulatorOfNodeColor implements GraphManipulator{
    
    /**
     * Pointer to graph.
     */
    private SBML2GraphML graph;
    
    /**
     * Field for the color.
     */
    private float[] HSBcolor;
    
    /**
     * Lower limit of species data.
     */
    private double lowerSpeciesLimit;
    
    /**
     * Upper limit of species data.
     */
    private double upperSpeciesLimit;
    
    /**
     * Default node size.
     */
    private double DEFAULT_NODE_SIZE = 30;
    
    /**
     * Constructs node-color manipulator with a standard color.
     * 
     * @param graph
     */
    public ManipulatorOfNodeColor(SBML2GraphML graph, double lowerSpeciesLimit, double upperSpeciesLimit){
        this.graph = graph;
        this.lowerSpeciesLimit = lowerSpeciesLimit;
        this.upperSpeciesLimit = upperSpeciesLimit;
        HSBcolor = Color.RGBtoHSB(176, 226, 255, null); //standard color
    }
    
    /**
     * Constructs node-color manipulator with the given RGB color. 
     * 
     * @param graph
     * @param r
     * @param g
     * @param b
     */
    public ManipulatorOfNodeColor(SBML2GraphML graph, int r, int g, int b, double lowerSpeciesLimit, double upperSpeciesLimit){
        this.graph = graph;
        HSBcolor = Color.RGBtoHSB(r, g, b, null);
        this.lowerSpeciesLimit = lowerSpeciesLimit;
        this.upperSpeciesLimit = upperSpeciesLimit;
    }

    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.GraphManipulator#dynamicChangeOfNode(java.lang.String, double, boolean)
     */
    @Override
    public void dynamicChangeOfNode(String id, double value, boolean labels) {
        // TODO change saturation, white -> pure color
        NodeRealizer nr = graph.getSimpleGraph()
                .getRealizer(graph.getId2node().get(id));
        nr.setSize(DEFAULT_NODE_SIZE, DEFAULT_NODE_SIZE); //standard node size
        Color c = new Color(Color.HSBtoRGB(HSBcolor[0], (float)(value/upperSpeciesLimit), HSBcolor[2]));
        nr.setFillColor(c);
        
        /*
         * Label Node with ID and real value at this timepoint. Last label will
         * be treated as dynamic label
         * TODO labeling in own function
         */
        if (labels) {
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
        } else if (nr.labelCount() > 1) {
            // labels switched off, therefore remove them, if there are any
            nr.removeLabel(nr.getLabel(nr.labelCount() - 1));
        }
        graph.getSimpleGraph().updateViews();
    }

    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.GraphManipulator#dynamicChangeOfReaction(java.lang.String, double)
     */
    @Override
    public void dynamicChangeOfReaction(String id, double value) {
        // TODO Auto-generated method stub
        
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
