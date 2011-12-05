/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;

import org.sbml.simulator.math.Euclidean;
import org.sbml.simulator.math.N_Metric;
import org.sbml.simulator.math.QualityMeasure;
import org.sbml.simulator.math.RelativeEuclidean;
import org.sbml.simulator.math.Relative_N_Metric;
import org.simulator.math.odes.MultiTable;

import de.zbit.util.prefs.SBPreferences;

/**
 * This class represents a quality measurement for the current simulation,
 * including a distance function and one or more experimental data sets.
 * 
 * @author Alexander D&ouml;rr
 * @version $Rev$
 * @since 1.0
 */
public class QualityMeasurement implements PropertyChangeListener {
  
	/**
	 * 
	 */
  QualityMeasure distance;
  
  /**
   * 
   */
  List<MultiTable> measurements;
  
  /**
   * Creates a new quality measurement.
   */
  public QualityMeasurement() {
    this.measurements = new LinkedList<MultiTable>();
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
    SBPreferences prefs = SBPreferences
        .getPreferencesFor(SimulationOptions.class);
    
    if ("distance".equals(property)) {
      distance = (QualityMeasure) evt.getNewValue();
      // TODO: Consider creating a new interface that provides a setRoot method.
      if (distance instanceof N_Metric) {
        if(!(distance instanceof Euclidean)) {
          ((N_Metric) distance).setRoot(prefs
              .getDouble(SimulationOptions.QUALITY_N_METRIC_ROOT));
        }
      } else if (distance instanceof Relative_N_Metric) {
          if(!(distance instanceof RelativeEuclidean)) {
            ((Relative_N_Metric) distance).setRoot(prefs
              .getDouble(SimulationOptions.QUALITY_N_METRIC_ROOT));
          }
      }
      prefs.put(SimulationOptions.QUALITY_MEASURE, distance.getClass()
          .getName());
      
    } else if ("measurements".equals(property)) {
      if (evt.getNewValue() == null) {
        measurements.remove(evt.getOldValue());
      } else {
        if (!measurements.contains(evt.getNewValue())) {
          measurements.add((MultiTable) evt.getNewValue());
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
  public List<MultiTable> getMeasurements() {
    return measurements;
  }
  
}
