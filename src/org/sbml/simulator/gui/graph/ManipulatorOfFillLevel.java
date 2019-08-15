package org.sbml.simulator.gui.graph;

import java.awt.Color;
import java.util.logging.Logger;

import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;

import de.zbit.graph.sbgn.CloneMarker;
import de.zbit.graph.sbgn.FillLevelNodeRealizer;
import de.zbit.gui.ColorPalette;
import de.zbit.sbml.layout.y.ILayoutGraph;
import y.base.Node;
import y.view.NodeLabel;
import y.view.NodeRealizer;

/**
 * This class is a {@link IGraphManipulator}. It changes the color of the given
 * nodes as well as their filling level. Changes of color are relative to concentration changes, 
 * changes of filling level are absolute to concentration changes.
 * 
 * @author Lea Buchweitz
 */

public class ManipulatorOfFillLevel extends AGraphManipulator {

	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(ManipulatorOfFillLevel.class.getName());

	/**
	 * Node size per default 30.
	 */
	private double nodeSize = 30;
	
	/**
	 * if camera animation is active and overview is shown node size is different
	 */
	private static double addSizeForOverview;

	/**
	 * determines default node color.
	 */
	private Color DEFAULT_NODE_COLOR = ColorPalette.SECOND_3015; 

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
	 * Constructs fill level manipulator with default RGB colors as color1
	 * (high) -> color2 (mid) -> color3 (low) and a constant node size.
	 * {@link Reaction}s line widths as given. Concentration changes as user given.
	 * 
	 * @param graph
	 * @param document
	 * @param core
	 * @param selectedSpecies
	 * @param nodeSize
	 * @param color3 
	 * @param color2 
	 * @param color1 
	 * @param color1
	 */
	public ManipulatorOfFillLevel(ILayoutGraph layoutGraph, SBMLDocument document, DynamicCore core,
			String[] selectedSpecies, double nodeSize, double addSizeForOverview, Color color1, Color color2, Color color3) {
		super(layoutGraph, document, core);
		
		this.addSizeForOverview = addSizeForOverview;

		this.nodeSize = nodeSize;
		
		// default colors
		// max concentration
		RGBcolor1 = new int[] {color1.getRed(),
				color1.getGreen(),
				color1.getBlue()};
		// middle concentration
		RGBcolor2 = new int[] {color2.getRed(),
				color2.getGreen(),
				color2.getBlue()};
		// min concentration
		RGBcolor3 = new int[] {color3.getRed(),
				color3.getGreen(),
				color3.getBlue()};

		/*
		 * Store min/max once to save computation time in case of absolute
		 * concentration changes.
		 */
		minMaxOfSelectedSpecies = core.getMinMaxOfIDs(selectedSpecies);

	}

	/**
	 * Constructs fill level manipulator with the given RGB colors as color1
	 * (high) -> color2 (mid) -> color3 (low) and a constant node size.
	 * {@link Reaction}s line widths as given. Concentration changes as user given.
	 * 
	 * @param graph
	 * @param document
	 * @param core
	 * @param selectedSpecies
	 * @param color1
	 * @param nodeSize
	 */

	public ManipulatorOfFillLevel(ILayoutGraph graph, SBMLDocument document,
			DynamicCore core, String[] selectedSpecies, double nodeSize, double addSizeForOverview, Color color2) {

		super(graph, document, core);
		
		this.addSizeForOverview = addSizeForOverview;
		
		this.nodeSize = nodeSize;

		Color[] minMax = calculateColorsFromDefault(color2);

		// min
		RGBcolor3 = new int[] { minMax[0].getRed(), minMax[0].getGreen(), minMax[0].getBlue() };
		// middle
		RGBcolor2 = new int[] { color2.getRed(), color2.getGreen(), color2.getBlue() };
		// max
		RGBcolor1 = new int[]{ minMax[1].getRed(), minMax[1].getGreen(), minMax[1].getBlue() };

		/*
		 * Store min/max once to save computation time in case of absolute
		 * concentration changes.
		 */
		minMaxOfSelectedSpecies = core.getMinMaxOfIDs(selectedSpecies);
	}

	@Override
	public void dynamicChangeOfNode(String id, double value, boolean labels) {
		if (id2speciesNode.get(id) != null) {
			for (Node node : graph.getSpeciesId2nodes().get(id)) {
				
				double addSize = 0;
				if(DynamicCore.cameraAnimation && DynamicCore.playAgain) {
					addSize = addSizeForOverview;
				} 

				double minValueFill, maxValueFill, minValueColor, maxValueColor;
				// absolute filling level      
				minValueFill = id2minMaxData.get(id)[0];
				maxValueFill = id2minMaxData.get(id)[1];
				// relative color
				minValueColor = minMaxOfSelectedSpecies[0];
				maxValueColor = minMaxOfSelectedSpecies[1];

				double percentFill = adjustValue(minValueFill, maxValueFill, 0, 1, value);
				int[] angles = degreeInterpolationForFillingLevel(percentFill);
				
				double percentColor = adjustValue(minValueColor, maxValueColor, 0, 1, value);
				int[] RGBinterpolated = linearColorInterpolationForThree(percentColor,
						RGBcolor1, RGBcolor2, RGBcolor3);

				NodeRealizer nr;
				if(!node2Realizer.containsKey(node)) {
					nr = graph.getGraph2D().getRealizer(node);
					node2Realizer.put(node, nr);
				} else {
					nr = node2Realizer.get(node);
				}

				FillLevelNodeRealizer realizerFillLevel;
				if(!node2FillRealizer.containsKey(node)) {
					realizerFillLevel = new FillLevelNodeRealizer(nr, percentFill, angles,
							new Color(RGBcolor2[0], RGBcolor2[1], RGBcolor2[2]));
					node2FillRealizer.put(node, realizerFillLevel);
					realizerFillLevel.setNodeIsCloned(((CloneMarker) nr).isNodeCloned());
				} else {
					realizerFillLevel = node2FillRealizer.get(node);
				}

				graph.getGraph2D().setRealizer(node, realizerFillLevel);

				NodeLabel nl = (NodeLabel) nr.getLabel();
				realizerFillLevel.setLabel(nl);
				
				double ratio = realizerFillLevel.getHeight() / realizerFillLevel.getWidth();

				realizerFillLevel.setAngles(angles);
				realizerFillLevel.setPercent(percentFill);             
				realizerFillLevel.setSize(nodeSize+addSize, nodeSize+addSize*ratio);                    
				realizerFillLevel.setColor(new Color(RGBinterpolated[0], RGBinterpolated[1],
						RGBinterpolated[2]));

				/*
				 * Label Node with ID and real value at this timepoint. Last label will
				 * be treated as dynamic label
				 */
				
                if (labels) {
                    labelNode(realizerFillLevel, id, value);
                } else if (nr.labelCount() > 1) {
                    // labels switched off, therefore remove them, if there are any
                	nr.removeLabel(nr.getLabel(nr.labelCount()-1));
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
