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
import org.sbml.simulator.gui.table.LegendTableModel;

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
     * Slope of linear regression m, and yintercept c. 
     */
    private double m = 1, c= 0;
    
    /**
     * If provided, colors of {@link LegendTableModel} are used as node colors.
     */
    private LegendTableModel legendTable = null;
    
    /**
     * determines default node color.
     */
    private Color DEFAULT_NODE_COLOR;
    
    /**
     * Constructs a new nodesize-manipulator on the given graph. Minimum node
     * size and maximum node size per default and reactions line widths per
     * default.
     * Node colors per default value.
     * 
     * @param graph
     * @param document
     * @param minMaxOfSpecies
     * @param minMaxOfReactions
     */
    public ManipulatorOfNodeSize(SBML2GraphML graph, SBMLDocument document,
            double[] minMaxOfSpecies, double[] minMaxOfReactions) {
        super(graph, document, minMaxOfReactions, (float)0.1, 6);
        DEFAULT_NODE_COLOR = new Color(176, 226, 255);
        computeSpeciesAdjusting(minMaxOfSpecies[0], minMaxOfSpecies[1], 8, 50);
    }
    
    /**
     * Constructs a new nodesize-manipulator on the given graph. Minimum node
     * size and maximum node size as given. If minimum node size greater than
     * maximum node size, default values will be used.
     * Node colors uniform as user given.
     * @param graph
     * @param document
     * @param minMaxOfSpecies
     * @param minMaxOfReactions
     * @param minNodeSize
     * @param maxNodeSize
     * @param reactionsMinLinewidth
     * @param reactionsMaxLineWidth
     */
    public ManipulatorOfNodeSize(SBML2GraphML graph, SBMLDocument document,
            Color uniformNodeColor, double[] minMaxOfSpecies,
            double[] minMaxOfReactions, double minNodeSize, double maxNodeSize,
            float reactionsMinLinewidth, float reactionsMaxLineWidth) {
        // no use of this() to avoid computation of adjusting twice
        super(graph, document, minMaxOfReactions, reactionsMinLinewidth,
                reactionsMaxLineWidth);
        if (minNodeSize > maxNodeSize) {
            minNodeSize = 8;
            maxNodeSize = 50;
        }
        DEFAULT_NODE_COLOR = uniformNodeColor;
        computeSpeciesAdjusting(minMaxOfSpecies[0], minMaxOfSpecies[1],
                minNodeSize, maxNodeSize);
    }
    
    /**
     * Constructs a new nodesize-manipulator on the given graph. Minimum node
     * size and maximum node size as given. If minimum node size greater than
     * maximum node size, default values will be used.
     * Nodecolors as in given {@link LegendTableModel}.
     * @param graph
     * @param document
     * @param legendTableModel
     * @param minMaxOfSpecies
     * @param minMaxOfReactions
     * @param minNodeSize
     * @param maxNodeSize
     * @param reactionsMinLinewidth
     * @param reactionsMaxLineWidth
     */
    public ManipulatorOfNodeSize(SBML2GraphML graph, SBMLDocument document, LegendTableModel legendTableModel,
            double[] minMaxOfSpecies, double[] minMaxOfReactions,
            double minNodeSize, double maxNodeSize,
            float reactionsMinLinewidth, float reactionsMaxLineWidth) {
        // no use of this() to avoid computation of adjusting twice
        super(graph, document, minMaxOfReactions, reactionsMinLinewidth,
                reactionsMaxLineWidth);
        this.legendTable = legendTableModel;
        if (minNodeSize > maxNodeSize) {
            minNodeSize = 8;
            maxNodeSize = 50;
        }
        computeSpeciesAdjusting(minMaxOfSpecies[0], minMaxOfSpecies[1],
                minNodeSize, maxNodeSize);
    }
    
    /**
     * Computes adjusting values for given limits.
     * @param lowerDataLimit
     * @param upperDataLimit
     * @param minNodeSize
     * @param maxNodeSize
     */
    private void computeSpeciesAdjusting(double lowerDataLimit, double upperDataLimit, double minNodeSize, double maxNodeSize){
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
        double ratio = nr.getHeight() / nr.getWidth(); //keep ratio in case of elliptic nodes
        nr.setSize(size*ratio, size);
        //use standard color if no legendTableModel is provided
        Color color = null;
        if(legendTable != null){
            color = legendTable.getColorFor(id);
        }else if (color == null){
            //ensure that color is never null in case of wrong LegendTableModel
            color = DEFAULT_NODE_COLOR;
        }
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
