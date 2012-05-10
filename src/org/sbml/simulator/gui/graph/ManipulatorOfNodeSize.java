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

import y.base.Edge;
import y.view.LineType;
import y.view.NodeLabel;
import y.view.NodeRealizer;
import de.zbit.graph.io.SBML2GraphML;

/**
 * This class is a {@link GraphManipulator}. It changes the size of the given
 * nodes and width of given reactions.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class ManipulatorOfNodeSize implements GraphManipulator{
    
    /**
     * Pointer to graph.
     */
    private SBML2GraphML graph;
    
    /**
     * Default setting for the minimum node size.
     */
    private double minNodeSize = 8;
    
    /**
     * Default setting for the maximum node size.
     */
    private double maxNodeSize = 50;
    
    /**
     * slope of linear regression. 
     */
    private double m = 1;
    
    /**
     * yintercept of linear regression. 
     */
    private double c = 0;
    
    /**
     * Constructs a new nodesize-manipulator on the give graph without linear regression.
     * @param graph
     */
    public ManipulatorOfNodeSize(SBML2GraphML graph){
        this.graph = graph;
    }

    /**
     * Constructs a new nodesize-manipulator on the given graph. Minimum node
     * size and maximum node size per default.
     * @param graph
     * @param lowerDataLimit
     * @param upperDataLimit
     */
    public ManipulatorOfNodeSize(SBML2GraphML graph, double lowerSpeciesLimit, double upperSpeciesLimit){
        this.graph = graph;
        getLinearRegression(minNodeSize, maxNodeSize, lowerSpeciesLimit, upperSpeciesLimit);
    }
    
    /**
     * Constructs a new nodesize-manipulator on the given graph. Minimum node
     * size and maximum node size as given.
     * @param graph
     * @param minNodeSize
     * @param maxNodeSize
     * @param lowerDataLimit
     * @param upperDataLimit
     */
    public ManipulatorOfNodeSize(SBML2GraphML graph, double minNodeSize, double maxNodeSize, double lowerSpeciesLimit, double upperSpeciesLimit){
        this.graph = graph;
        this.minNodeSize = minNodeSize;
        this.maxNodeSize = maxNodeSize;
        getLinearRegression(minNodeSize, maxNodeSize, lowerSpeciesLimit, upperSpeciesLimit);
    }
    
    /**
     * Compute linear regression for given values. 
     * @param minNodeSize
     * @param maxNodeSize
     * @param lowerDataLimit
     * @param upperDataLimit
     */
    private void getLinearRegression(double minNodeSize, double maxNodeSize, double lowerDataLimit, double upperDataLimit){
        double[] linearRegression = BIASComputation.computeBIAS(lowerDataLimit, upperDataLimit, minNodeSize, maxNodeSize);
        m = linearRegression[0];
        c = linearRegression[1];
    }

    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.GraphManipulator#dynamicChangeOfNode(java.lang.String, double, double, boolean)
     */
    @Override
    public void dynamicChangeOfNode(String id, double value, boolean labels) {
        double size = value*m + c; //adjust value by linear regression
        NodeRealizer nr = graph.getSimpleGraph()
                .getRealizer(graph.getId2node().get(id));
        nr.setSize(size, size);
        nr.setFillColor(new Color(176, 226, 255)); //standard color

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
        LinkedList<Edge> listOfEdges = graph.getId2edge()
                .get(id);
        for (Edge e : listOfEdges) {
            float valueF = (float) value;
            LineType currLinetype = graph.getSimpleGraph()
                    .getRealizer(e).getLineType();
            LineType newLineType = LineType.createLineType(valueF,
                    currLinetype.getEndCap(), currLinetype.getLineJoin(),
                    currLinetype.getMiterLimit(), currLinetype.getDashArray(),
                    currLinetype.getDashPhase());
            graph.getSimpleGraph().getRealizer(e)
                    .setLineType(newLineType);
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
            nr.setSize(8, 8);
            nr.setFillColor(Color.LIGHT_GRAY);

            if (nr.labelCount() > 1) {
                // if not selected disable label
                nr.removeLabel(nr.getLabel(nr.labelCount() - 1));
            }
        }
        graph.getSimpleGraph().updateViews();
    }

}
