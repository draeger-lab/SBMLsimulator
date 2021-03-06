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
package org.sbml.simulator.math;

import org.simulator.math.odes.MultiTable;
import org.simulator.math.odes.MultiTable.Block.Column;

import eva2.tools.math.interpolation.BasicDataSet;
import eva2.tools.math.interpolation.InterpolationException;
import eva2.tools.math.interpolation.SplineInterpolation;

/**
 * This class provides method to calculate interpolation splines in order to
 * smooth input data and to increase the number of sampling points in between
 * data.
 * 
 * @author Roland Keller
 * @since 1.2
 */
public class SplineCalculation {
  /**
   * 
   * @param table
   * @param inBetweenTimePoints
   * @param negativeValuesPossible
   * @return
   */
  public static MultiTable calculateSplineValues(MultiTable table, int inBetweenTimePoints, boolean negativeValuesPossible) {

    SplineInterpolation sp = null;
    int columnStart = 1;
    double[] timePoints = new double[table.getTimePoints().length + (table.getTimePoints().length-1) * inBetweenTimePoints];
    int index = 0;
    for(int i = 1; i != table.getTimePoints().length; i++) {
      for(int j = 0; j <= inBetweenTimePoints; j++) {
        timePoints[index] = table.getTimePoint(i-1) + ((table.getTimePoint(i) - table.getTimePoint(i-1))/(inBetweenTimePoints + 1)) * j;
        index++;
      }
    }
    timePoints[timePoints.length-1] = table.getTimePoint(table.getTimePoints().length - 1);


    MultiTable result = new MultiTable();
    result.setTimePoints(timePoints);
    result.setTimeName(table.getTimeName());

    for(int block = 0; block != table.getBlockCount(); block++) {
      result.addBlock(table.getBlock(block).getIdentifiers());
      try {
        sp = new SplineInterpolation();
      } catch (InterpolationException e) {
        e.printStackTrace();
      }

      for(int col = columnStart; col != result.getColumnCount(); col++) {
        Column c = table.getColumn(col);
        double[] values = new double[table.getRowCount()];
        for(int row = 0; row!=table.getRowCount(); row++) {
          values[row] = c.getValue(row);
        }

        BasicDataSet dataset = new BasicDataSet(table.getTimePoints(),values, "Time", "Y");
        try {
          sp.setAbstractDataSet(dataset);
        } catch (InterpolationException e1) {
          e1.printStackTrace();
        }
        for(int row = 0; row != result.getRowCount(); row++) {
          try {
            double value = sp.getY(result.getTimePoint(row));
            if(negativeValuesPossible) {
              result.setValueAt(value, row, col);
            }
            else {
              if (Double.isNaN(value)) {
                result.setValueAt(Double.NaN, row, col);
              }
              else {
                result.setValueAt(Math.max(value,0d), row, col);
              }
            }

          } catch (InterpolationException e) {
            e.printStackTrace();
          }
        }
      }
      columnStart+= table.getBlock(0).getColumnCount();

    }
    return result;
  }

  /**
   * 
   * @param table
   * @param inBetweenTimePoints
   * @return
   */
  public static MultiTable calculateSplineValues(MultiTable table, int inBetweenTimePoints) {
    return calculateSplineValues(table, inBetweenTimePoints, true);
  }

}
