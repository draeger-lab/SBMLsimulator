/*
 * $Id: QualityMeasurement.java 16:09:32 draeger$ $URL: QualityMeasurement.java
 * $ --------------------------------------------------------------------- This
 * file is part of SBMLsimulator, a Java-based simulator for models of
 * biochemical processes encoded in the modeling language SBML.
 * 
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation. A copy of the license agreement is provided in the file
 * named "LICENSE.txt" included with this software distribution and also
 * available online as <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */

package org.sbml.simulator;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;

import org.sbml.simulator.math.QualityMeasure;
import org.sbml.simulator.math.odes.MultiBlockTable;

/**
 * This class represents a quality measurement for the current simulation,
 * including a distance function and one or more experimental data sets.
 * 
 * @author Alexander D&ouml;rr
 * @version $Rev$
 * @since 1.0
 */

public class QualityMeasurement implements PropertyChangeListener {
  
  QualityMeasure distance;
  
  List<MultiBlockTable> measurements;
  
  /**
   * Creates a new quality measurement.
   */
  public QualityMeasurement() {
    this.measurements = new LinkedList<MultiBlockTable>();
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent
   * )
   */
  public void propertyChange(PropertyChangeEvent evt) {
    String property = evt.getPropertyName();
    
    if (property == "distance") {
      this.distance = (QualityMeasure) evt.getNewValue();
    } else if (property == "measurements") {
      if (evt.getNewValue() == null) {
        measurements.remove(evt.getOldValue());
      } else {
        if (!measurements.contains(evt.getNewValue())) {
          measurements.add((MultiBlockTable) evt.getNewValue());
        }
      }
    }
  }
  
  /**
   * @return the distance
   */
  public QualityMeasure getDistance() {
    return distance;
  }
  
  /**
   * @return the measurements
   */
  public List<MultiBlockTable> getMeasurements() {
    return measurements;
  }
  
}
