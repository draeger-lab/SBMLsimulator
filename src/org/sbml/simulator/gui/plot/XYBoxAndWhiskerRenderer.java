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
package org.sbml.simulator.gui.plot;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.Outlier;
import org.jfree.chart.renderer.OutlierList;
import org.jfree.chart.renderer.OutlierListCollection;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.util.PaintList;
import org.jfree.chart.util.RectangleEdge;
import org.jfree.data.statistics.BoxAndWhiskerXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class XYBoxAndWhiskerRenderer extends
		org.jfree.chart.renderer.xy.XYBoxAndWhiskerRenderer {
	
	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = -1232284307535723334L;
	
	/**
	 * 
	 */
	private PaintList artifactPaintList, meanPaintList;
	
	/**
	 * 
	 */
	public XYBoxAndWhiskerRenderer() {
		super();
		artifactPaintList = new PaintList();
		meanPaintList = new PaintList();
		setBaseToolTipGenerator(new BoxAndWhiskerXYToolTipGenerator());
	}
	
	/**
	 * 
	 * @param boxWidth
	 */
	public XYBoxAndWhiskerRenderer(double boxWidth) {
		super(boxWidth);
		artifactPaintList = new PaintList();
		meanPaintList = new PaintList();
	}

	/* (non-Javadoc)
	 * @see org.jfree.chart.renderer.xy.XYBoxAndWhiskerRenderer#drawHorizontalItem(java.awt.Graphics2D, org.jfree.chart.renderer.xy.XYItemRendererState, java.awt.geom.Rectangle2D, org.jfree.chart.plot.XYPlot, org.jfree.chart.axis.ValueAxis, org.jfree.chart.axis.ValueAxis, org.jfree.data.xy.XYDataset, int, int, boolean, int)
	 */
	protected void drawHorizontalItem(Graphics2D g2, XYItemRendererState state,
		Rectangle2D dataArea, XYPlot plot, ValueAxis domainAxis,
		ValueAxis rangeAxis, XYDataset dataset, int series, int item,
		boolean selected, int pass) {
		
		Boolean visible = getSeriesVisible(series);
		if ((visible == null) || !visible.booleanValue()) {
			return; 
		}
		
		// setup for collecting optional entity info...
		EntityCollection entities = null;
		if (state.getInfo() != null) {
			entities = state.getInfo().getOwner().getEntityCollection();
		}
		
		BoxAndWhiskerXYDataset boxAndWhiskerData = (BoxAndWhiskerXYDataset) dataset;
		
		Number x = boxAndWhiskerData.getX(series, item);
		Number yMax = boxAndWhiskerData.getMaxRegularValue(series, item);
		Number yMin = boxAndWhiskerData.getMinRegularValue(series, item);
		Number yMedian = boxAndWhiskerData.getMedianValue(series, item);
		Number yAverage = boxAndWhiskerData.getMeanValue(series, item);
		Number yQ1Median = boxAndWhiskerData.getQ1Value(series, item);
		Number yQ3Median = boxAndWhiskerData.getQ3Value(series, item);
		
		double xx = domainAxis.valueToJava2D(x.doubleValue(), dataArea,
			plot.getDomainAxisEdge());
		
		RectangleEdge location = plot.getRangeAxisEdge();
		double yyMax = rangeAxis.valueToJava2D(yMax.doubleValue(), dataArea,
			location);
		double yyMin = rangeAxis.valueToJava2D(yMin.doubleValue(), dataArea,
			location);
		double yyMedian = rangeAxis.valueToJava2D(yMedian.doubleValue(), dataArea,
			location);
		double yyAverage = 0d;
		if (yAverage != null) {
			yyAverage = rangeAxis.valueToJava2D(yAverage.doubleValue(), dataArea,
				location);
		}
		double yyQ1Median = rangeAxis.valueToJava2D(yQ1Median.doubleValue(),
			dataArea, location);
		double yyQ3Median = rangeAxis.valueToJava2D(yQ3Median.doubleValue(),
			dataArea, location);
		
		double exactBoxWidth = getBoxWidth();
		double width = exactBoxWidth;
		double dataAreaX = dataArea.getHeight();
		double maxBoxPercent = 0.1d;
		double maxBoxWidth = dataAreaX * maxBoxPercent;
		if (exactBoxWidth <= 0d) {
			int itemCount = boxAndWhiskerData.getItemCount(series);
			exactBoxWidth = dataAreaX / itemCount * 4.5 / 7;
			if (exactBoxWidth < 3) {
				width = 3;
			} else if (exactBoxWidth > maxBoxWidth) {
				width = maxBoxWidth;
			} else {
				width = exactBoxWidth;
			}
		}
		
		g2.setPaint(getItemPaint(series, item, selected));
		Stroke s = getItemStroke(series, item, selected);
		g2.setStroke(s);
		
		// draw the upper shadow
		g2.draw(new Line2D.Double(yyMax, xx, yyQ3Median, xx));
		g2.draw(new Line2D.Double(yyMax, xx - width / 2, yyMax, xx + width / 2));
		
		// draw the lower shadow
		g2.draw(new Line2D.Double(yyMin, xx, yyQ1Median, xx));
		g2.draw(new Line2D.Double(yyMin, xx - width / 2, yyMin, xx + width / 2));
		
		// draw the body
		Shape box = null;
		if (yyQ1Median < yyQ3Median) {
			box = new Rectangle2D.Double(yyQ1Median, xx - width / 2, yyQ3Median
					- yyQ1Median, width);
		} else {
			box = new Rectangle2D.Double(yyQ3Median, xx - width / 2, yyQ1Median
					- yyQ3Median, width);
		}
		if (getFillBox()) {
			g2.setPaint(lookupBoxPaint(series, item));
			g2.fill(box);
		}
		g2.setStroke(getItemOutlineStroke(series, item, selected));
		g2.setPaint(getItemOutlinePaint(series, item, selected));
		g2.draw(box);
		
		// draw median
		g2.setPaint(getArtifactPaint(series));
		g2.draw(new Line2D.Double(yyMedian, xx - width / 2, yyMedian, xx + width
				/ 2));
		
		// draw average - SPECIAL AIMS REQUIREMENT
		if (yAverage != null) {
			double aRadius = width / 4;
			// here we check that the average marker will in fact be visible
			// before drawing it...
			Paint p = getMeanPaint(series);
			if ((p != null) && (yyAverage > (dataArea.getMinX() - aRadius))
					&& (yyAverage < (dataArea.getMaxX() + aRadius))) {
				g2.setPaint(p);
				Ellipse2D.Double avgEllipse = new Ellipse2D.Double(yyAverage - aRadius,
					xx - aRadius, aRadius * 2, aRadius * 2);
				g2.fill(avgEllipse);
				g2.draw(avgEllipse);
			}
		}
		
		// FIXME: draw outliers
		
		// add an entity for the item...
		if (entities != null && box.intersects(dataArea)) {
			addEntity(entities, box, dataset, series, item, selected, yyAverage, xx);
		}
		
	}

	/* (non-Javadoc)
   * @see org.jfree.chart.renderer.xy.XYBoxAndWhiskerRenderer#drawVerticalItem(java.awt.Graphics2D, org.jfree.chart.renderer.xy.XYItemRendererState, java.awt.geom.Rectangle2D, org.jfree.chart.plot.XYPlot, org.jfree.chart.axis.ValueAxis, org.jfree.chart.axis.ValueAxis, org.jfree.data.xy.XYDataset, int, int, boolean, int)
   */
	@SuppressWarnings("unchecked")
	protected void drawVerticalItem(Graphics2D g2, XYItemRendererState state,
		Rectangle2D dataArea, XYPlot plot, ValueAxis domainAxis,
		ValueAxis rangeAxis, XYDataset dataset, int series, int item,
		boolean selected, int pass) {

		Boolean visible = getSeriesVisible(series);
		if ((visible == null) || !visible.booleanValue()) { 
			return; 
		}
		
		// setup for collecting optional entity info...
		EntityCollection entities = null;
		if (state.getInfo() != null) {
			entities = state.getInfo().getOwner().getEntityCollection();
		}
		
		BoxAndWhiskerXYDataset boxAndWhiskerData = (BoxAndWhiskerXYDataset) dataset;
		
		Number x = boxAndWhiskerData.getX(series, item);
		Number yMax = boxAndWhiskerData.getMaxRegularValue(series, item);
		Number yMin = boxAndWhiskerData.getMinRegularValue(series, item);
		Number yMedian = boxAndWhiskerData.getMedianValue(series, item);
		Number yAverage = boxAndWhiskerData.getMeanValue(series, item);
		Number yQ1Median = boxAndWhiskerData.getQ1Value(series, item);
		Number yQ3Median = boxAndWhiskerData.getQ3Value(series, item);
		List<Number> yOutliers = boxAndWhiskerData.getOutliers(series, item);
		
		double xx = domainAxis.valueToJava2D(x.doubleValue(), dataArea,
			plot.getDomainAxisEdge());
		
		RectangleEdge location = plot.getRangeAxisEdge();
		double yyMax = rangeAxis.valueToJava2D(yMax.doubleValue(), dataArea,
			location);
		double yyMin = rangeAxis.valueToJava2D(yMin.doubleValue(), dataArea,
			location);
		double yyMedian = rangeAxis.valueToJava2D(yMedian.doubleValue(), dataArea,
			location);
		double yyAverage = 0.0;
		if (yAverage != null) {
			yyAverage = rangeAxis.valueToJava2D(yAverage.doubleValue(), dataArea,
				location);
		}
		double yyQ1Median = rangeAxis.valueToJava2D(yQ1Median.doubleValue(),
			dataArea, location);
		double yyQ3Median = rangeAxis.valueToJava2D(yQ3Median.doubleValue(),
			dataArea, location);
		double yyOutlier;
		
		double exactBoxWidth = getBoxWidth();
		double width = exactBoxWidth;
		double dataAreaX = dataArea.getMaxX() - dataArea.getMinX();
		double maxBoxPercent = 0.1;
		double maxBoxWidth = dataAreaX * maxBoxPercent;
		if (exactBoxWidth <= 0.0) {
			int itemCount = boxAndWhiskerData.getItemCount(series);
			exactBoxWidth = dataAreaX / itemCount * 4.5 / 7;
			if (exactBoxWidth < 3) {
				width = 3;
			} else if (exactBoxWidth > maxBoxWidth) {
				width = maxBoxWidth;
			} else {
				width = exactBoxWidth;
			}
		}
		
		g2.setPaint(getItemPaint(series, item, selected));
		Stroke s = getItemStroke(series, item, selected);
		g2.setStroke(s);
		
		// draw the upper shadow
		g2.draw(new Line2D.Double(xx, yyMax, xx, yyQ3Median));
		g2.draw(new Line2D.Double(xx - width / 2, yyMax, xx + width / 2, yyMax));
		
		// draw the lower shadow
		g2.draw(new Line2D.Double(xx, yyMin, xx, yyQ1Median));
		g2.draw(new Line2D.Double(xx - width / 2, yyMin, xx + width / 2, yyMin));
		
		// draw the body
		Shape box = null;
		if (yyQ1Median > yyQ3Median) {
			box = new Rectangle2D.Double(xx - width / 2, yyQ3Median, width,
				yyQ1Median - yyQ3Median);
		} else {
			box = new Rectangle2D.Double(xx - width / 2, yyQ1Median, width,
				yyQ3Median - yyQ1Median);
		}
		if (getFillBox()) {
			g2.setPaint(lookupBoxPaint(series, item));
			g2.fill(box);
		}
		g2.setStroke(getItemOutlineStroke(series, item, selected));
		g2.setPaint(getItemOutlinePaint(series, item, selected));
		g2.draw(box);
		
		// draw median
		g2.setPaint(getArtifactPaint(series));
		g2.draw(new Line2D.Double(xx - width / 2, yyMedian, xx + width / 2,
			yyMedian));
		
		double aRadius = 0; // average radius
		double oRadius = width / 3; // outlier radius
		
		// draw average - SPECIAL AIMS REQUIREMENT
		if (yAverage != null) {
			aRadius = width / 4;
			// here we check that the average marker will in fact be visible
			// before drawing it...
			Paint p = getMeanPaint(series);
			if ((p != null) && (yyAverage > (dataArea.getMinY() - aRadius))
					&& (yyAverage < (dataArea.getMaxY() + aRadius))) {
				g2.setPaint(p);
				// TODO: Remove multiplier with 2 and set to the middle
				Ellipse2D.Double avgEllipse = new Ellipse2D.Double(xx - aRadius,
					yyAverage - aRadius, aRadius * 2, aRadius * 2);
				g2.fill(avgEllipse);
				g2.draw(avgEllipse);
			}
		}
		
		List<Outlier> outliers = new ArrayList<Outlier>();
		OutlierListCollection outlierListCollection = new OutlierListCollection();
		
		/*
		 * From outlier array sort out which are outliers and put these into an
		 * arraylist. If there are any farouts, set the flag on the
		 * OutlierListCollection
		 */
		
		for (int i = 0; i < yOutliers.size(); i++) {
			double outlier = ((Number) yOutliers.get(i)).doubleValue();
			if (outlier > boxAndWhiskerData.getMaxOutlier(series, item).doubleValue()) {
				outlierListCollection.setHighFarOut(true);
			} else if (outlier < boxAndWhiskerData.getMinOutlier(series, item)
					.doubleValue()) {
				outlierListCollection.setLowFarOut(true);
			} else if (outlier > boxAndWhiskerData.getMaxRegularValue(series, item)
					.doubleValue()) {
				yyOutlier = rangeAxis.valueToJava2D(outlier, dataArea, location);
				outliers.add(new Outlier(xx, yyOutlier, oRadius));
			} else if (outlier < boxAndWhiskerData.getMinRegularValue(series, item)
					.doubleValue()) {
				yyOutlier = rangeAxis.valueToJava2D(outlier, dataArea, location);
				outliers.add(new Outlier(xx, yyOutlier, oRadius));
			}
			Collections.sort(outliers);
		}
		
		// Process outliers. Each outlier is either added to the appropriate
		// outlier list or a new outlier list is made
		for (Iterator<Outlier> iterator = outliers.iterator(); iterator.hasNext();) {
			Outlier outlier = iterator.next();
			outlierListCollection.add(outlier);
		}
		
		// draw yOutliers
		double maxAxisValue = rangeAxis.valueToJava2D(rangeAxis.getUpperBound(),
			dataArea, location) + aRadius;
		double minAxisValue = rangeAxis.valueToJava2D(rangeAxis.getLowerBound(),
			dataArea, location) - aRadius;
		
		// draw outliers
		for (Iterator<OutlierList> iterator = outlierListCollection.iterator(); iterator
				.hasNext();) {
			OutlierList list = iterator.next();
			Outlier outlier = list.getAveragedOutlier();
			Point2D point = outlier.getPoint();
			
			if (list.isMultiple()) {
				drawMultipleEllipse(point, width, oRadius, g2);
			} else {
				drawEllipse(point, oRadius, g2);
			}
		}
		
		// draw farout
		if (outlierListCollection.isHighFarOut()) {
			drawHighFarOut(aRadius, g2, xx, maxAxisValue);
		}
		
		if (outlierListCollection.isLowFarOut()) {
			drawLowFarOut(aRadius, g2, xx, minAxisValue);
		}
		
		// add an entity for the item...
		if (entities != null && box.intersects(dataArea)) {
			addEntity(entities, box, dataset, series, item, selected, xx, yyAverage);
		}
		
	}
	
	/**
	 * 
	 * @param series
	 * @return
	 */
	public Paint getArtifactPaint(int series) {
		return artifactPaintList.getPaint(series);
	}
	
	/**
	 * 
	 * @param series
	 * @return
	 */
	public Paint getMeanPaint(int series) {
		return meanPaintList.getPaint(series);
	}
	
	/**
	 * 
	 * @param series
	 * @param pint
	 */
	public void setArtifactPaint(int series, Paint paint) {
		setArtifactPaint(series, paint, true);
	}
	
	/**
	 * 
	 * @param series
	 * @param paint
	 * @param notify
	 */
	public void setArtifactPaint(int series, Paint paint, boolean notify) {
		this.artifactPaintList.setPaint(series, paint);
		if (notify) {
			fireChangeEvent();
		}
	}
	
	/**
	 * 
	 * @param series
	 * @param paint
	 */
	public void setMeanPaint(int series, Paint paint) {
		meanPaintList.setPaint(series, paint);
	}

}
