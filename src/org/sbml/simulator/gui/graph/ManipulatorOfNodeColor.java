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

import org.sbml.jsbml.SBMLDocument;

import y.view.NodeRealizer;
import de.zbit.graph.io.SBML2GraphML;


/**
 * This class is a {@link GraphManipulator}. It changes the color of the given
 * nodes and width of given reactions.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class ManipulatorOfNodeColor extends AbstractGraphManipulator{
    /**
     * Field for the color.
     */
    private float[] HSBcolor;
    
    /**
     * Slope of linear regression m, and yintercept c.
     */
    private double m, c;
    
    /**
     * Constructs node-color manipulator with a standard color.
     * @param graph
     * @param document
     * @param minMaxOfSpeciesData
     * @param minMaxOfReactionsData
     */
    public ManipulatorOfNodeColor(SBML2GraphML graph, SBMLDocument document,
            double[] minMaxOfSpeciesData,
            double[] minMaxOfReactionsData) {
        super(graph, document, minMaxOfReactionsData[0], minMaxOfReactionsData[1]);
        DEFAULT_NODE_SIZE = 30;
        HSBcolor = Color.RGBtoHSB(176, 226, 255, null); //standard color
        computeSpeciesAdjusting(minMaxOfSpeciesData[0], minMaxOfSpeciesData[1]);
    }
    
    /**
     * Constructs node-color manipulator with the given RGB color.
     * @param graph
     * @param document
     * @param r
     * @param g
     * @param b
     * @param minMaxOfSpeciesData
     * @param minMaxOfReactionsData
     */
    public ManipulatorOfNodeColor(SBML2GraphML graph, SBMLDocument document,
            int r, int g, int b, double[] minMaxOfSpeciesData, double[] minMaxOfReactionsData) {
        super(graph, document, minMaxOfReactionsData[0], minMaxOfReactionsData[1]);
        DEFAULT_NODE_SIZE = 30;
        HSBcolor = Color.RGBtoHSB(r, g, b, null);
        computeSpeciesAdjusting(minMaxOfSpeciesData[0], minMaxOfSpeciesData[1]);
    }
    
    /**
     * Computes adjusting values for given limits.
     * @param lowerSpeciesLimit
     * @param upperSpeciesLimit
     */
    private void computeSpeciesAdjusting(double lowerSpeciesLimit, double upperSpeciesLimit){
        double[] linearRegression = computeBIAS(lowerSpeciesLimit, upperSpeciesLimit, 0, 1);
        m = linearRegression[0];
        c = linearRegression[1];
    }

    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.GraphManipulator#dynamicChangeOfNode(java.lang.String, double, boolean)
     */
    @Override
    public void dynamicChangeOfNode(String id, double value, boolean labels) {
        NodeRealizer nr = graph.getSimpleGraph()
                .getRealizer(graph.getId2node().get(id));
        nr.setSize(DEFAULT_NODE_SIZE, DEFAULT_NODE_SIZE); //standard node size
        Color color = new Color(Color.HSBtoRGB(HSBcolor[0], (float)(value*m+c), HSBcolor[2]));
        nr.setFillColor(color);
        
        /*
         * Label Node with ID and real value at this timepoint. Last label will
         * be treated as dynamic label
         */
        if (labels) {
            labelNode(nr, id, value);
        } else if (nr.labelCount() > 1) {
            // labels switched off, therefore remove them, if there are any
            nr.removeLabel(nr.getLabel(nr.labelCount() - 1));
        }
        graph.getSimpleGraph().updateViews();
    }
}
