/*
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2016 by the University of Tuebingen, Germany.
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
import java.util.logging.Logger;

import org.sbml.jsbml.SBMLDocument;

import de.zbit.sbml.layout.y.ILayoutGraph;
import y.base.Node;
import y.view.NodeRealizer;


/**
 * This class is a {@link IGraphManipulator}. It changes the color of the given
 * nodes as well as their size and width of given reactions. Changes of color
 * are relative to concentration changes, changes of node size are absolute to
 * concentration changes.
 * 
 * @author Fabian Schwarzkopf
 */
public class ManipulatorOfNodeSizeAndColor extends AGraphManipulator {

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(ManipulatorOfNodeSizeAndColor.class.getName());

  /**
   * Minimum and maximum Node Size with default initialization.
   */
  private static double minNodeSize = DEFAULT_MIN_NODE_SIZE,
      maxNodeSize = DEFAULT_MAX_NODE_SIZE;

  /**
   * if camera animation is active and overview is shown node size is different
   */
  private static double addSizeForOverview;

  /**
   * Field for the first color (high concentration), second color (mid
   * concentration), third color (low concentration).
   */
  private int[] RGBcolor1, RGBcolor2, RGBcolor3;

  /**
   * Saves minimum and maximum value of selected Species to save computation
   * time.
   */
  private double[] minMaxOfSelectedSpecies;

  /**
   * Constructs a new node-size and node-color manipulator. Changes of
   * node-size are computed absolute, changes of node-color are computed
   * relative. Minimum node size and maximum node size per default and
   * reactions line widths per default. Default gradient camine red (high) ->
   * white (mid) -> gold (low).
   * 
   * @param graph
   * @param document
   * @param core
   * @param selectedSpecies
   * @param selectedReactions
   */
  public ManipulatorOfNodeSizeAndColor(ILayoutGraph graph, SBMLDocument document,
    DynamicCore core, String[] selectedSpecies,
    String[] selectedReactions, double minNodeSize, double maxNodeSize, double addSizeForOverview, Color color2) {

    super(graph, document, core, selectedReactions, DEFAULT_MIN_LINEWIDTH,
      DEFAULT_MAX_LINEWIDTH);

    this.addSizeForOverview = addSizeForOverview;

    REVERT_NODE_SIZE = DEFAULT_MIN_NODE_SIZE;
    if (minNodeSize < maxNodeSize) {
      this.minNodeSize = minNodeSize;
      this.maxNodeSize = maxNodeSize;
    }

    Color[] minMax = calculateColorsFromDefault(color2);

    // min
    RGBcolor3 = new int[] { minMax[0].getRed(), minMax[0].getGreen(), minMax[0].getBlue() };
    // middle
    RGBcolor2 = new int[] { color2.getRed(), color2.getGreen(), color2.getBlue() };
    // max
    RGBcolor1 = new int[]{ minMax[1].getRed(), minMax[1].getGreen(), minMax[1].getBlue() };

    /*
     * Store min/max once to save computation time for absolute part
     * concentration changes.
     */
    minMaxOfSelectedSpecies = core.getMinMaxOfIDs(selectedSpecies);
  }

  /**
   * Constructs a new node-size and node-color manipulator. Changes of
   * node-size are computed absolute, changes of node-color are computed
   * relative. Minimum node size and maximum node size as given. If minimum
   * node size greater than maximum node size, default values will be used.
   * Color gradient as given by color1 (high) -> color2 (mid) -> color3 (low).
   * Reactions line widths as given.
   * 
   * @param graph
   * @param document
   * @param core
   * @param selectedSpecies
   * @param selectedReactions
   * @param minNodeSize
   * @param maxNodeSize
   * @param color1
   * @param color2
   * @param color3
   * @param reactionsMinLinewidth
   * @param reactionsMaxLineWidth
   */
  public ManipulatorOfNodeSizeAndColor(ILayoutGraph graph,
    SBMLDocument document, DynamicCore core, String[] selectedSpecies,
    String[] selectedReactions, double minNodeSize, double maxNodeSize, double addSizeForOverview,
    Color color1, Color color2, Color color3,
    float reactionsMinLineWidth, float reactionsMaxLineWidth) {

    // no use of this() because of other super constructor
    super(graph, document, core, selectedReactions, reactionsMinLineWidth, reactionsMaxLineWidth);

    this.addSizeForOverview = addSizeForOverview;

    // gather settings
    if (minNodeSize < maxNodeSize) {
      this.minNodeSize = minNodeSize;
      this.maxNodeSize = maxNodeSize;
    } // else ignore input and use default node sizes
    int r = color1.getRed();
    int g = color1.getGreen();
    int b = color1.getBlue();
    RGBcolor1 = new int[] { r, g, b };
    r = color2.getRed();
    g = color2.getGreen();
    b = color2.getBlue();
    RGBcolor2 = new int[] { r, g, b };
    r = color3.getRed();
    g = color3.getGreen();
    b = color3.getBlue();
    RGBcolor3 = new int[] { r, g, b }; 

    /*
     * Store min/max once to save computation time for absolute part of
     * concentration changes.
     */
    minMaxOfSelectedSpecies = core.getMinMaxOfIDs(selectedSpecies);
  }

  /* (non-Javadoc)
   * @see org.sbml.simulator.gui.graph.IGraphManipulator#dynamicChangeOfNode(java.lang.String, double, boolean)
   */
  @Override
  public void dynamicChangeOfNode(String id, double value, boolean labels) {
    if (id2speciesNode.get(id) != null) {

      double addSize = 0;
      if(DynamicCore.cameraAnimation && DynamicCore.playAgain) {
        addSize = addSizeForOverview;
      } 

      // compute adusting of node size (absolute)
      double size = adjustValue(minMaxOfSelectedSpecies[0],
        minMaxOfSelectedSpecies[1], minNodeSize+addSize, maxNodeSize+addSize, value);

      boolean invisible = isInvisible(value);

      // compute adjusting of node color (relative)
      double percent = adjustValue(id2minMaxData.get(id)[0],
        id2minMaxData.get(id)[1], 0, 1, value);
      int[] RGBinterpolated = linearColorInterpolationForThree(percent,
        RGBcolor1, RGBcolor2, RGBcolor3);
      logger.finer(MessageFormat
        .format("Species {0}: value={1}, results in node size={2} and color percent={3} and r={4}, g={5}, b={6}",
          new Object[] { id, value, size, percent,
            RGBinterpolated[0], RGBinterpolated[1],
            RGBinterpolated[2] }));

      for (Node node : graph.getSpeciesId2nodes().get(id)) {
        if (invisible) {
          hide(id, node, true);
        }
        else {
          hide(id, node, false);
          // visualize

          NodeRealizer nr;
          if(!node2Realizer.containsKey(node)) {
            nr = graph.getGraph2D().getRealizer(node);
            node2Realizer.put(node, nr);
          } else {
            nr = node2Realizer.get(node);
            graph.getGraph2D().setRealizer(node, nr);        			
          }

          double ratio = nr.getHeight() / nr.getWidth(); // keep ratio in case of elliptic nodes
          nr.setSize(size, size * ratio);
          nr.setFillColor(new Color(RGBinterpolated[0], RGBinterpolated[1],
            RGBinterpolated[2]));
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
        }
      }
      // update view
      graph.getGraph2D().updateViews();
    }
  }

  /**
   * sets new min and max node sizes if they were changed
   * @param minNodeSize
   * @param maxNodeSize
   */
  public void setNewMinMaxNodeSize(double minNodeSize, double maxNodeSize) {
    if(this.minNodeSize != minNodeSize) {
      this.minNodeSize = minNodeSize;
    }
    if(this.maxNodeSize != maxNodeSize) {
      this.maxNodeSize = maxNodeSize;
    }
  }

  /**
   * resets the adding factor for the node sizes 
   * @param newSize
   */
  @Override
  public void setAddSizeForOverview(int newSize) {
    addSizeForOverview = newSize;
  }

}
