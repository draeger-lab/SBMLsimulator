/*
 * $Id$
 * $URL$ 
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2014 by the University of Tuebingen, Germany.
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;

import y.base.Edge;
import y.base.Node;
import y.view.EdgeRealizer;
import y.view.LineType;
import y.view.NodeLabel;
import y.view.NodeRealizer;
import de.zbit.graph.io.SBML2GraphML;
import de.zbit.graph.sbgn.ReactionNodeRealizer;
import de.zbit.sbml.layout.y.ILayoutGraph;


/**
 * This class is an abstract graph manipulator, that provides some basic
 * functions and constants. Each {@link IGraphManipulator} should be derived
 * from this class.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public abstract class AGraphManipulator implements IGraphManipulator {

	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(AGraphManipulator.class.getName());

	/**
	 * Pointer to the graph.
	 */
	protected ILayoutGraph graph;

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
	 * Default min/max line width for {@link Reaction}s.
	 */
	public static final float DEFAULT_MIN_LINEWIDTH = 1, DEFAULT_MAX_LINEWIDTH = 6;

	/**
	 * Saves mapping from reactionIDs to related reaction nodes.
	 */
	protected Map<String, Set<Node>> reactionID2reactionNode;

	/**
	 * Saves mapping from species node to reaction node
	 */
	protected Map<String, Integer> hiddenNodesForReaction;
	protected Map<String, Set<Edge>> hiddenEdgesForSpecies;

	/**
	 * Saves mapping from speciesID to related species nodes.
	 */
	protected Map<String, Set<Node>> id2speciesNode;

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
	 * Saves minimum and maximum value of selected {@link Reaction}s to save
	 * computation time.
	 */
	protected double[] minMaxOfselectedReactions;

	/**
	 * Used line width for dynamic visualization of {@link Reaction}s.
	 */
	private float minLineWidth = DEFAULT_MIN_LINEWIDTH,
			maxLineWidth = DEFAULT_MAX_LINEWIDTH;

	/**
	 * Constructs an abstract graph manipulator.
	 * @param layoutGraph
	 * @param document
	 * @param core
	 */
	public AGraphManipulator(ILayoutGraph layoutGraph, SBMLDocument document, DynamicCore core) {
		this.graph = layoutGraph;
		reactionID2reactionNode = new HashMap<String, Set<Node>>();
		id2speciesNode = new HashMap<String, Set<Node>>();
		hiddenEdgesForSpecies = new HashMap<String, Set<Edge>>();
		hiddenNodesForReaction = new HashMap<String, Integer>();
		if (document.isSetModel()) {
			id2speciesNode.putAll(layoutGraph.getSpeciesId2nodes());
			reactionID2reactionNode.putAll(layoutGraph.getReactionId2nodes());
		}
		this.document = document;
		this.core = core;
		id2minMaxData = core.getId2minMaxData();
	}

	/**
	 * Constructs an abstract graph manipulator on the given
	 * {@link SBML2GraphML} and {@link SBMLDocument}. Additionally, this
	 * constructor provides a basic implementation of
	 * {@link #dynamicChangeOfReaction(String, double, boolean)} method with the
	 * given parameters.
	 * 
	 * @param isbmlLayoutGraph
	 * @param document
	 * @param core
	 * @param selectedReactions
	 * @param reactionsMinLineWidth
	 * @param reactionsMaxLineWidth
	 */
	public AGraphManipulator(ILayoutGraph isbmlLayoutGraph, SBMLDocument document,
			DynamicCore core, String[] selectedReactions,
			float reactionsMinLineWidth, float reactionsMaxLineWidth) {
		this(isbmlLayoutGraph, document, core);

		minMaxOfselectedReactions = core.getMinMaxOfIDs(selectedReactions);
		this.minLineWidth = reactionsMinLineWidth;
		this.maxLineWidth = reactionsMaxLineWidth;
	}

	/**
	 * Maps the given value which has to be within [x1, x2] to codomain [y1,
	 * y2]. This is done implicit by a linear regression through the points (x1,
	 * y1) and (x2, y2). If x2 <= x1 (e.g., only one timepoint given) than a
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
			return (y1 + y2) / 2.0d;
		}

		return ((y2 - y1) / (x2 - x1)) * (v - x1) + y1;
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.gui.graph.IGraphManipulator#dynamicChangeOfReaction(java.lang.String, double)
	 */
	@Override
	public void dynamicChangeOfReaction(String id, double value, boolean labels) { 
		Set<Node> processNodes = reactionID2reactionNode.get(id);
		if ((processNodes != null) && (value != 0d)) {
			float lineWidth = 1f;
			for (Node pNode : processNodes) {
				lineWidth = manipulateProcessNode(id, value, labels, pNode);
			}
			// edges line width
			Set<List<Edge>> edgeSets = graph.getReactionId2edges().get(id);
			for (List<Edge> edgeList : edgeSets) {
				manipulateEdges(id, value, processNodes, lineWidth, edgeList);
			}
			graph.getGraph2D().updateViews();
		} else if ((processNodes != null) && (value == 0d)) {
			revertChanges(id);

			logger.finer(MessageFormat.format(
					"ReactionID: {0} value=0. Reverting id.",
					new Object[] { id }));

			/*
			 * If labels are enable show them anyway.
			 */
			if (labels) {
				for (Node pNode : reactionID2reactionNode.get(id)) {
					ReactionNodeRealizer nr = (ReactionNodeRealizer) graph.getGraph2D().getRealizer(pNode);
					labelNode(nr, id, value);
				}
				graph.getGraph2D().updateViews();
			}
		} else if (processNodes == null) {
			logger.finer(MessageFormat.format(
					"No ReactionNodeRealizer for ReactionID: {0}",
					new Object[] { id }));
		}
	}

	/**
	 * @param reactionID 
	 * @param value
	 * @param processNodes
	 * @param lineWidth
	 * @param edgeList
	 */
	private void manipulateEdges(String reactionID, double value, Set<Node> processNodes,
			float lineWidth, List<Edge> edgeList) {
		for (Edge edge : edgeList) {
			EdgeRealizer edgeRealizer = graph.getGraph2D().getRealizer(edge);

			/*
			 * adjust lineWidth only if regression was computed, else use
			 * standard line width.
			 */
			// only line-width is supposed to change
			LineType currLinetype = edgeRealizer.getLineType();
			LineType newLineType = LineType.createLineType(lineWidth,
					currLinetype.getEndCap(), currLinetype.getLineJoin(),
					currLinetype.getMiterLimit(),
					currLinetype.getDashArray(),
					currLinetype.getDashPhase());

			edgeRealizer.setLineType(newLineType);

			/*
			 * determine arrow direction
			 */
			// TODO: This must depend on the original reaction!
//			Reaction reaction = document.getModel().getReaction(reactionID);
//			if (value > 0d) { // (!= 0), because simulation data at time point
//				// 0.0 = 0
//				if (processNodes == edge.target()) {
//					// reactants
//					edgeRealizer.setSourceArrow(Arrow.NONE);
//					// TODO own arrow for reversible
//					// if (document.getModel().getReaction(id).isReversible()) {
//					// er.setSourceArrow(Arrow.PLAIN);
//					// } else {
//					// er.setSourceArrow(Arrow.NONE);
//					// }
//				} else {
//					// TODO: These can also be modifiers!
//					// products
//					edgeRealizer.setTargetArrow(Arrow.DELTA);
//				}
//			} else {
//				if (processNodes == edge.target()) {
//					// TODO: These can also be modifiers!
//					// products
//					edgeRealizer.setSourceArrow(Arrow.DELTA);
//				} else {
//					// reactants
//					edgeRealizer.setTargetArrow(Arrow.NONE);
//					// TODO own arrow for reversible
//					// if (document.getModel().getReaction(id).isReversible()) {
//					// er.setTargetArrow(Arrow.PLAIN);
//					// } else {
//					// er.setTargetArrow(Arrow.NONE);
//					// }
//				}
//			}
		}
	}

	/**
	 * @param id
	 * @param value
	 * @param labels
	 * @param pNode
	 * @return used linewidth
	 */
	private float manipulateProcessNode(String id, double value, boolean labels,
			Node pNode) {
		ReactionNodeRealizer nr = (ReactionNodeRealizer) graph.getGraph2D().getRealizer(pNode);
		// line width
		double absvalue = Math.abs(value);

		/*
		 * Take absolute higher limit as xMax and 0 as xLow for regression.
		 * Eventually reactions will end up in equillibrium.
		 */
		double xHigh =
			Math.abs(minMaxOfselectedReactions[0]) > Math.abs(minMaxOfselectedReactions[1]) ?
				Math.abs(minMaxOfselectedReactions[0]) : 
				Math.abs(minMaxOfselectedReactions[1]);
		float lineWidth = (float) adjustValue(0, xHigh, minLineWidth, maxLineWidth, absvalue);
		logger.finer(MessageFormat
				.format("Reaction {0}: Abs. value={1}, computes to line width={2}",
						new Object[] { id, absvalue, lineWidth }));

		// ReactionNode line width
		nr.setLineWidth(lineWidth);
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
		return lineWidth;
	}


	/**
	 * Labels the given node of this {@link NodeRealizer} with the given ID and value.
	 * @param nr
	 * @param id
	 * @param value
	 */
	protected void labelNode(NodeRealizer nr, String id, double value) {
		String name = "";
		if (document.getModel().getSpecies(id) != null) {
			name = document.getModel().getSpecies(id).isSetName() ? document
					.getModel().getSpecies(id).getName() : id;
		} else if (document.getModel().getReaction(id) != null) {
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
		if ((percent >= 0d) && (percent <= 1d)) {
			for (int i = 0; i < outcolor.length; i++) {
				outcolor[i] = (int) (RGBcolor1[i] * percent + RGBcolor2[i]
						* (1 - percent));
			}
			return outcolor;
		} else if (percent > 1d) {
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

		if ((percent >= 0d) && (percent <= 0.5d)) {
			// color interpolation between color2 (mid concentration) and color3
			// (low concentration)
			double resPercent = adjustValue(0, 0.5, 0, 1, percent);
			return linearColorInterpolation(resPercent, RGBcolor2, RGBcolor3);
		} else if ((percent > 0.5d) && (percent <= 1.0d)) {
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
			for (Node node : graph.getSpeciesId2nodes().get(id)) {
				NodeRealizer nr = graph.getGraph2D().getRealizer(node);
				double ratio = nr.getHeight() / nr.getWidth(); //keep ratio in case of elliptic nodes
				nr.setSize(REVERT_NODE_SIZE, REVERT_NODE_SIZE*ratio);
				nr.setFillColor(Color.LIGHT_GRAY);

				if (nr.labelCount() > 1) {
					// if not selected disable label
					nr.removeLabel(nr.getLabel(nr.labelCount() - 1));
				}
			}
		}
		//revert reactions (lines and pseudonode)
		if (graph.getReactionId2edges().get(id) != null) {
			for (List<Edge> listOfEdges : graph.getReactionId2edges().get(id)) {
				for (Edge edge : listOfEdges) {
					EdgeRealizer er = graph.getGraph2D().getRealizer(edge);
					er.setLineType(LineType.LINE_1);
					// TODO: This must depend on the original reaction!
//					if (document.getModel().getReaction(id).isReversible()) {
//						if (reactionID2reactionNode.get(id) == edge.target()) {
//							er.setSourceArrow(Arrow.DELTA);
//						} else {
//							er.setTargetArrow(Arrow.DELTA);
//						}
//					} else {
//						if (reactionID2reactionNode.get(id) == edge.target()) {
//							er.setSourceArrow(Arrow.NONE);
//						} else {
//							er.setTargetArrow(Arrow.DELTA);
//						}
//					}
				}

				if (reactionID2reactionNode.get(id) != null) { 
					for (Node pNode : reactionID2reactionNode.get(id)) {
						ReactionNodeRealizer nr = (ReactionNodeRealizer) graph
								.getGraph2D().getRealizer(pNode);
						nr.setLineWidth(1);
						if (nr.labelCount() > 1) {
							// if not selected disable label
							nr.removeLabel(nr.getLabel(nr.labelCount() - 1));
						}
					}
				} else {
					logger.finer(MessageFormat.format(
							"No ReactionNodeRealizer for ReactionID: {0}",
							new Object[] { id }));
				}
			}
			graph.getGraph2D().updateViews();
		}
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.gui.graph.IGraphManipulator#hide(y.base.Node, boolean)
	 */
	@Override
	public void hide(String id, Node node, boolean b) {
		// TODO!
//		Graph2D graph2D = graph.getGraph2D();
//		assert node != null;
//		if (b) {
//			if (hiddenEdgesForSpecies.get(id) != null) return; //species already hidden
//			logger.info("hiding " + id);
//			// get reactions where id is necessary participant in
//			// increase the hidden nodes count for this reactions
//			// if the count is > 2 the reaction must be already hidden
//			// save removed reaction-edges for later
//			// remove process node
//			// save out-edges of node for later
//			// remove node
//			Set<Edge> hiddenEdges = new HashSet<Edge>();
//			for (String reaction : graph.getSpeciesId2reactions().get(id)) {
//				int count = incrementHiddenNodesCountFor(reaction);
//				if (count < 2) {
//					for (Node pNode : reactionID2reactionNode.get(reaction)) {
//						for (EdgeCursor e = pNode.outEdges(); e.ok(); e.next()) {
//							hiddenEdges.add((Edge) e.current());
//						}
//						graph2D.removeNode(pNode);
//					}
//				}
//			}
//			for (EdgeCursor e = node.outEdges(); e.ok(); e.next()) {
//				hiddenEdges.add((Edge) e.current());
//			}
//			hiddenEdgesForSpecies.put(id, hiddenEdges);
//			graph2D.removeNode(node);
//		}
//		else {
//			if (hiddenEdgesForSpecies.get(id) == null) {
//				return; //species wasn't hidden
//			}
//			logger.info("unhiding " + id);
//			// retrieve node
//			// decrement hiddenNodesForReaction count
//			// traverse reactions for counts == 0
//			// retrieve pNode and Edges
//			// reinsert node, pNode and Edges to graph
//			// TODO Not tested because no matching model is available
//			for (String reaction : graph.getSpeciesId2reactions().get(id)) {
//				int count = decrementHiddenNodesCountFor(reaction);
//				if (count == 0) {
//					for (Node pNode : graph.getReactionId2nodes().get(reaction)) {
//						graph2D.reInsertNode(pNode);
//					}
//				}
//			}
//			for (Node n : id2speciesNode.get(id)) {
//				graph2D.reInsertNode(n);
//			}
//			for (Edge e : hiddenEdgesForSpecies.remove(id)) {
//				graph2D.reInsertEdge(e);
//			}
//		}
	}

	/**
	 * @param reaction
	 */
	private int incrementHiddenNodesCountFor(String reaction) {
		if (hiddenNodesForReaction.get(reaction) == null) {
			hiddenNodesForReaction.put(reaction, new Integer(1));
			return 1;
		}
		else {
			int i = hiddenNodesForReaction.get(reaction) + 1;
			hiddenNodesForReaction.put(reaction, i);
			return i;
		}
	}

	/**
	 * @param reaction
	 */
	private int decrementHiddenNodesCountFor(String reaction) {
		assert hiddenNodesForReaction.get(reaction) != null;
		int i = hiddenNodesForReaction.get(reaction) - 1;
		hiddenNodesForReaction.put(reaction, i);
		return i;
	}

	/**
	 * 
	 * @param value
	 * @return
	 */
	protected boolean isInvisible(double value) {
		return Math.abs(value) < 1e-7d;
	}

}

