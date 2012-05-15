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
 * This class is a {@link GraphManipulator}. It changes the size of the given
 * nodes and width of given reactions.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class ManipulatorOfNodeSize extends AbstractGraphManipulator{    
    /**
     * Default setting for the minimum node size.
     */
    private double minNodeSize = 8;
    
    /**
     * Default setting for the maximum node size.
     */
    private double maxNodeSize = 50;
    
    /**
     * Slope of linear regression m, and yintercept c. 
     */
    private double m = 1, c= 0;
    
    /**
     * Constructs a new nodesize-manipulator on the given graph. Minimum node
     * size and maximum node size per default.
     * @param graph
     * @param document
     * @param minMaxOfSpecies
     * @param minMaxOfReactions
     */
    public ManipulatorOfNodeSize(SBML2GraphML graph, SBMLDocument document,
            double[] minMaxOfSpecies, double[] minMaxOfReactions) {
        super(graph, document, minMaxOfReactions[0], minMaxOfReactions[1]);
        computeSpeciesAdjusting(minMaxOfSpecies[0], minMaxOfSpecies[1]);
    }
    
    /**
     * Constructs a new nodesize-manipulator on the given graph. Minimum node
     * size and maximum node size as given.
     * @param graph
     * @param document
     * @param minNodeSize
     * @param maxNodeSize
     * @param minMaxOfSpecies
     * @param minMaxOfReactions
     */
    public ManipulatorOfNodeSize(SBML2GraphML graph, SBMLDocument document,
            double minNodeSize, double maxNodeSize, double[] minMaxOfSpecies,
            double[] minMaxOfReactions) {
        super(graph, document, minMaxOfReactions[0], minMaxOfReactions[1]);
        this.minNodeSize = minNodeSize;
        this.maxNodeSize = maxNodeSize;  
        computeSpeciesAdjusting(minMaxOfSpecies[0], minMaxOfSpecies[1]);
    }
    
    /**
     * Computes adjusting values for given limits.
     * @param lowerDataLimit
     * @param upperDataLimit
     */
    private void computeSpeciesAdjusting(double lowerDataLimit, double upperDataLimit){
        double[] linearRegression = computeBIAS(lowerDataLimit, upperDataLimit, minNodeSize, maxNodeSize);
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
