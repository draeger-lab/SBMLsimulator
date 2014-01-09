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
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import y.io.JPGIOHandler;
import y.io.ViewPortConfigurator;
import y.view.Graph2D;
import y.view.Graph2DView;


/**
 * This class handles image generating. 
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class ImageGenerator {

    /**
     * A {@link Logger} for this class.
     */
    private static final transient Logger logger = Logger.getLogger(ImageGenerator.class.getName());
    
    /**
     * Graph on which the images are taken.
     */
    private Graph2D graph;
    
    /**
     * Determines whether the whole graph is pictured or just the current
     * display window.
     */
    private boolean wholeGraph;
    
    /**
     * Fix point for video.
     */
    private Point viewPoint;
    
    /**
     * Fix point for video.
     */
    private double zoomLevel;
    
    /**
     * Constructs a new {@link ImageGenerator} on the given graph. Depending on
     * the given boolean, either the whole graph will be pictured or just the
     * current view.
     * 
     * @param graph
     * @param wholeGraph
     */
    public ImageGenerator(Graph2D graph, boolean wholeGraph) {
        this.graph = graph;
        this.wholeGraph = wholeGraph;
    }

	/**
	 * This function determines the fix points to generate videos. The fix
	 * points are either determined on the whole graph or on the current view.
	 * It ensures that graph elements will stay on their location even if the
	 * graph size changes during visualization (e. g. nodes getting
	 * bigger/smaller).
	 * 
	 * @param width
	 *            of the output resolution
	 */
    public void determineFixPoints(int width) {
        //save current view
        Graph2DView originalViewPort = (Graph2DView) graph.getCurrentView();
        
        //create dedicated view
        JPGIOHandler ioh = new JPGIOHandler();
        Graph2DView imageView = ioh.createDefaultGraph2DView(graph);
        
        //configure imageView such that whole graph is contained
        ViewPortConfigurator vpc = new ViewPortConfigurator();
        vpc.setGraph2D(imageView.getGraph2D());
        if (wholeGraph) {
            //use whole graph
            vpc.setGraph2D(imageView.getGraph2D());
            vpc.setClipType(ViewPortConfigurator.CLIP_GRAPH);
            //scale it to achieve high resolution
            vpc.setSizeType(ViewPortConfigurator.SIZE_USE_CUSTOM_WIDTH);
            vpc.setCustomWidth(width);
        } else {
            //use original view
            vpc.setGraph2DView(originalViewPort);
            vpc.setClipType(ViewPortConfigurator.CLIP_VIEW);
            //scale it anyway for high resultion
            vpc.setSizeType(ViewPortConfigurator.SIZE_USE_CUSTOM_WIDTH);
            vpc.setCustomWidth(width);
        }
        vpc.configure(imageView);
        
        //save initial viewpoint
        viewPoint = imageView.getViewPoint();
        //save initial zoomlevel
        zoomLevel = imageView.getZoom();
        
        logger.fine("Fixpoints set. Viewpoint: " + viewPoint + "; Zoomlevel: "
                + zoomLevel);
    }

    /**
     * Returned width and height represent the unscaled graph as a whole or the
     * width and height of the current display window.
     * @return {width, height}
     */
    public int[] getScreenshotResolution() {
        //save current view
        Graph2DView originalViewPort = (Graph2DView) graph.getCurrentView();
        
        //create dedicated view
        JPGIOHandler ioh = new JPGIOHandler();
        Graph2DView imageView = ioh.createDefaultGraph2DView(graph);

        //configure view
        ViewPortConfigurator vpc = new ViewPortConfigurator();
        if (wholeGraph) {
            //configure imageView such that whole graph is contained
            vpc.setGraph2D(imageView.getGraph2D());
            vpc.setClipType(ViewPortConfigurator.CLIP_GRAPH);
            vpc.setSizeType(ViewPortConfigurator.SIZE_USE_ORIGINAL);
        } else {
            //use original view
            vpc.setGraph2DView(originalViewPort);
            vpc.setClipType(ViewPortConfigurator.CLIP_VIEW);
            //do not scale it, because this method returns the real resolution
            vpc.setSizeType(ViewPortConfigurator.SIZE_USE_ORIGINAL);
        }
        vpc.configure(imageView);
        
        int width = imageView.getWidth();
        int height = imageView.getHeight();
        
        return new int[]{width, height};
    }

    /**
     * This method returns an {@link BufferedImage} of the given size. To
     * achieve higher resolutions than the actual graph size, the graph gets
     * scaled to the given size in a dedicated view to take the screen shot.
     * <p>
     * In order to generate videos as sequence of images, fix points should
     * initially be determined first with determineFixpoints(). Then each
     * picture will be taken from the same point of view and zoom level. This
     * means there will be no elements changing their position due to changing
     * their size while a dynamic simulation. Especially important if the whole
     * graph is pictured, because than the bounding box will vary if the
     * elements change their size.
     * @param width
     * @param height
     * @return
     */
    public BufferedImage takeGraphshot(int width, int height) {
        /*
         * Take screenshot in dedicated view to ensure that nothing is cut off
         * and to change screenshotresolution independent of actual view (this
         * allows higher screenshot resolutions without users noticing the
         * necessary scaling).
         */

        //unselect objects
        graph.unselectAll();
        
        //save current view
        Graph2DView originalViewPort = (Graph2DView) graph.getCurrentView();
        
        //create dedicated view for graphshot
        JPGIOHandler ioh = new JPGIOHandler();
        Graph2DView imageView = ioh.createDefaultGraph2DView(graph);
        
        //use original render settings
        imageView.setGraph2DRenderer(originalViewPort.getGraph2DRenderer());
        imageView.setRenderingHints(originalViewPort.getRenderingHints());
        
        //set dedicated view
        graph.setCurrentView(imageView);
        
        //settings for dedicated view
        ViewPortConfigurator vpc = new ViewPortConfigurator();
        if (wholeGraph) {
            vpc.setGraph2D(imageView.getGraph2D());
            //do not cut off anything not in view
            vpc.setClipType(ViewPortConfigurator.CLIP_GRAPH);
            //scale image to video size
            vpc.setSizeType(ViewPortConfigurator.SIZE_USE_CUSTOM_WIDTH);
            vpc.setCustomWidth(width);
        } else {
            //use current display window
            vpc.setGraph2DView(originalViewPort);
            vpc.setClipType(ViewPortConfigurator.CLIP_VIEW);
            //scale it anyway to ensure high resolution
            vpc.setSizeType(ViewPortConfigurator.SIZE_USE_CUSTOM_WIDTH);
            vpc.setCustomWidth(width);
        }
        //configure dedicated view with settings
        vpc.configure(imageView);
        
        /*
         * fix zoomlevel and viewpoint to prevent pixel jumping during video
         * generating. Make sure to call determineFixePoints(int width) before calling
         * this function.
         */
        if (zoomLevel != 0 && viewPoint != null) {
            imageView.setZoom(zoomLevel);
            imageView.setViewPoint(viewPoint.x, viewPoint.y);
        } else {
            logger.fine("No Fixpoints available. There might be some pixel jumping in videos.");
        }
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        //if BufferedImage is too large, overlap will be white
        Graphics2D paintArea = image.createGraphics();
        paintArea.setColor(Color.WHITE);
        paintArea.fillRect(0, 0, width, height);
        //paint screenshot
        imageView.paintVisibleContent(image.createGraphics());
        
        //restore original view
        graph.removeView(graph.getCurrentView());
        graph.setCurrentView(originalViewPort);
        
        return image;
    }
    
}
