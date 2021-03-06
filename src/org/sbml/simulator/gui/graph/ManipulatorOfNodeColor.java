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

import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;

import y.base.Node;
import y.view.NodeRealizer;
import de.zbit.gui.ColorPalette;
import de.zbit.sbml.layout.y.ILayoutGraph;


/**
 * This class is a {@link IGraphManipulator}. It changes the color of the given
 * nodes and width of given reactions.
 * 
 * @author Fabian Schwarzkopf
 */
public class ManipulatorOfNodeColor extends AGraphManipulator {

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(ManipulatorOfNodeColor.class.getName());

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
   * Node size per default 30.
   */
  private double nodeSize = 30;

  /**
   * if camera animation is active and overview is shown node size is different
   */
  private static double addSizeForOverview;

  /**
   * Concentration changes relativ or absolute? Per default false.
   */
  private boolean relativeConcentrations = false;

  /**
   * Constructs node-color manipulator with default gradient camine red (high)
   * -> white (mid) -> gold (low) and default node size and default reactions
   * line widths.
   * Concentration changes absolute per default.
   * 
   * @param graph
   * @param document
   * @param core
   * @param selectedSpecies
   * @param selectedReactions
   */
  public ManipulatorOfNodeColor(ILayoutGraph graph, SBMLDocument document, DynamicCore core,
    String[] selectedSpecies,
    String[] selectedReactions) {

    super(graph, document, core, selectedReactions, DEFAULT_MIN_LINEWIDTH, DEFAULT_MAX_LINEWIDTH);

    REVERT_NODE_SIZE = nodeSize;

    // default colors
    RGBcolor1 = new int[] { ColorPalette.CAMINE_RED.getRed(),
      ColorPalette.CAMINE_RED.getGreen(),
      ColorPalette.CAMINE_RED.getBlue() };
    RGBcolor2 = new int[]{255, 255, 255}; //white
    RGBcolor3 = new int[]{ ColorPalette.GOLD.getRed(),
      ColorPalette.GOLD.getGreen(),
      ColorPalette.GOLD.getBlue() };
    /*
     * Store min/max once to save computation time in case of absolute
     * concentration changes.
     */
    minMaxOfSelectedSpecies = core.getMinMaxOfIDs(selectedSpecies);
  }

  /**
   * Constructs node-color manipulator with the given RGB colors as color1
   * (high) -> color2 (mid) -> color3 (low) and a constant node size.
   * {@link Reaction}s line widths as given. Concentration changes as user given.
   * 
   * @param graph
   * @param document
   * @param core
   * @param selectedSpecies
   * @param selectedReactions
   * @param relativeConcentrations
   * @param nodeSize
   * @param color1
   * @param color2
   * @param color3
   * @param reactionsMinLineWidth
   * @param reactionsMaxLineWidth
   */
  public ManipulatorOfNodeColor(ILayoutGraph graph, SBMLDocument document,
    DynamicCore core, String[] selectedSpecies,
    String[] selectedReactions, boolean relativeConcentrations,
    double nodeSize, double addSizeForOverview, Color color1, Color color2, Color color3,
    float reactionsMinLineWidth, float reactionsMaxLineWidth) {

    // no use of this() because of other super constructor
    super(graph, document, core, selectedReactions, reactionsMinLineWidth, reactionsMaxLineWidth);

    this.addSizeForOverview = addSizeForOverview;

    // gather settings
    this.nodeSize = nodeSize;
    REVERT_NODE_SIZE = nodeSize;
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
    this.relativeConcentrations = relativeConcentrations;

    /*
     * Store min/max once to save computation time in case of absolute
     * concentration changes.
     */
    minMaxOfSelectedSpecies = core.getMinMaxOfIDs(selectedSpecies);
  }

  /**
   * Constructs node-color manipulator with the given RGB colors as color1
   * (high) -> color2 (mid) -> color3 (low) and a constant node size.
   * {@link Reaction}s line widths as given. Concentration changes as user given.
   * 
   * @param graph
   * @param document
   * @param core
   * @param selectedSpecies
   * @param selectedReactions
   * @param relativeConcentrations
   * @param nodeSize
   * @param color1
   * @param reactionsMinLineWidth
   * @param reactionsMaxLineWidth
   */
  public ManipulatorOfNodeColor(ILayoutGraph graph, SBMLDocument document,
    DynamicCore core, String[] selectedSpecies,
    String[] selectedReactions, boolean relativeConcentrations,
    double nodeSize, double addSizeForOverview, Color color2, float reactionsMinLineWidth, 
    float reactionsMaxLineWidth) {

    // no use of this() because of other super constructor
    super(graph, document, core, selectedReactions, reactionsMinLineWidth, reactionsMaxLineWidth);

    this.addSizeForOverview = addSizeForOverview;

    // gather settings
    this.nodeSize = nodeSize;
    REVERT_NODE_SIZE = nodeSize;

    Color[] minMax = calculateColorsFromDefault(color2);

    // min
    RGBcolor3 = new int[] { minMax[0].getRed(), minMax[0].getGreen(), minMax[0].getBlue() };
    // middle
    RGBcolor2 = new int[] { color2.getRed(), color2.getGreen(), color2.getBlue() };
    // max
    RGBcolor1 = new int[]{ minMax[1].getRed(), minMax[1].getGreen(), minMax[1].getBlue() };

    this.relativeConcentrations = relativeConcentrations;

    /*
     * Store min/max once to save computation time in case of absolute
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
      for (Node node : graph.getSpeciesId2nodes().get(id)) {
        hide(id, node, false);

        NodeRealizer nr;
        if(!node2Realizer.containsKey(node)) {
          nr = graph.getGraph2D().getRealizer(node);
          node2Realizer.put(node, nr);
        } else {
          nr = node2Realizer.get(node);
          graph.getGraph2D().setRealizer(node, nr);        			
        }

        double addSize = 0;
        if(DynamicCore.cameraAnimation && DynamicCore.playAgain) {
          addSize = addSizeForOverview;
        }

        // init visualization style
        double ratio = nr.getHeight() / nr.getWidth(); //keep ratio in case of elliptic nodes
        nr.setSize(nodeSize+addSize, nodeSize+addSize*ratio); //standard node size

        double minValue, maxValue; // values to compute adjusting 
        if (relativeConcentrations) {
          /*
           * Realtive changes. Use species specific min/max values.
           */
          minValue = id2minMaxData.get(id)[0];
          maxValue = id2minMaxData.get(id)[1];
        } else {
          /*
           * Absolute changes. Use min/max values of all selected species.
           */
          minValue = minMaxOfSelectedSpecies[0];
          maxValue = minMaxOfSelectedSpecies[1];
        }
        // compute adjusting
        double percent = adjustValue(minValue, maxValue, 0, 1, value);
        int[] RGBinterpolated = linearColorInterpolationForThree(percent,
          RGBcolor1, RGBcolor2, RGBcolor3);
        logger.finer(MessageFormat
          .format("Species {0}: value={1}, results in percent={2} and r={3}, g={4}, b={5}",
            new Object[] { id, value, percent,
              RGBinterpolated[0], RGBinterpolated[1],
              RGBinterpolated[2] }));

        // visualize
        nr.setFillColor(new Color(RGBinterpolated[0], RGBinterpolated[1], RGBinterpolated[2]));

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
      // update view
      graph.getGraph2D().updateViews();
    }
  }

  public void setNewMinMaxNodeSize(double minNodeSize, double maxNodeSize) {}

  /**
   * resets the adding factor for the node sizes 
   * @param newSize
   */
  @Override
  public void setAddSizeForOverview(int newSize) {
    addSizeForOverview = newSize;
  }
}
