package org.sbml.simulator.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.xml.stream.XMLStreamException;

import org.jfree.data.statistics.Statistics;
import org.sbml.jsbml.SBMLException;
import org.simulator.math.odes.MultiBlockTable;

import de.zbit.io.CSVWriter;

public class ErrorIntroduction {
  public static void introduceError(String dataFile, String outputFile, double averagePrecision, double systematicErrorPercentage, double baselinePercentage)
    throws XMLStreamException, IOException, SBMLException {
    //read file with data
    BufferedReader reader = new BufferedReader(new FileReader(dataFile));
    ArrayList<double[]> data = new ArrayList<double[]>();
    ArrayList<Double> timepoints = new ArrayList<Double>();
    
    try {
      String line = null;
      
      line = reader.readLine();
      String[] identifiers = line.split(",");
      while ((line = reader.readLine()) != null) {
        
        String[] splits = line.split(",");
        timepoints.add(Double.valueOf(splits[0]));
        
        int size = splits.length - 1;
        if (size > 0) {
          double[] row = new double[size];
          data.add(row);
          for (int i = 0; i < size; i++) {
            Double value = Double.valueOf(splits[i + 1]);
            if (value != null) {
              row[i] = value;
            } else {
              row[i] = Double.NaN;
            }
          }
        }
      }
      reader.close();
      
      if (data.size() > 0) {
        double[][] dataMatrix = data.toArray(new double[data.size()][]);
        double[] timepointsArray = new double[timepoints.size()];
        
        for (int i = 0; i != timepointsArray.length; i++) {
          timepointsArray[i] = timepoints.get(i);
        }
        MultiBlockTable table = new MultiBlockTable(timepointsArray,
          dataMatrix, identifiers);
        
        introduceErrorHelp(table, averagePrecision, systematicErrorPercentage, baselinePercentage);
        
        if (outputFile != null) {
          CSVWriter writer = new CSVWriter();
          writer.write(table, ',', outputFile);
        }
        
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private static void introduceErrorHelp(MultiBlockTable table, double averagePrecision, double systematicErrorPercentage, double averageBaselinePercentage) {
    Random random = new Random();
    
    //value uniformly distributed between 0.5 * averagePrecision and 1.5 * averagePrecision
    double[] precisions = new double[table.getColumnCount() - 1];
    
    //values uniformly distributed between 1 +- systematicErrorPercentage % 
    double[] scaleFactors = new double[table.getColumnCount() - 1];
    
  //values uniformly distributed between 0 +- baselinePercentage % 
    double[] baselinePercentages = new double[table.getColumnCount() - 1];
    
    
    for (int i = 0; i != precisions.length; i++) {
      precisions[i] = (random.nextDouble() + 0.5) * averagePrecision;
      scaleFactors[i] = (random.nextDouble()
          * (2 * (systematicErrorPercentage / 100)) + 1 - systematicErrorPercentage / 100);
      baselinePercentages[i] = (random.nextDouble()
          * (2 * (averageBaselinePercentage / 100)) - averageBaselinePercentage / 100);
    }
    
    for (int column = 1; column != table.getColumnCount(); column++) {
      ArrayList<Double> values = new ArrayList<Double>();
      for(int row=0;row!=table.getRowCount();row++) {
        values.add(table.getValueAt(row, column));
      }
      double currentBaseline=baselinePercentages[column-1]*Statistics.calculateMedian(values);
      for (int row = 0; row != table.getRowCount(); row++) {
        //TODO
        double initialConcentration=0.5;
        double currentError = random.nextGaussian() * precisions[column-1] * initialConcentration;
        
        //correct data with precision values
        table.setValueAt(table.getValueAt(row, column)
              + currentError, row, column);
        
        //correct data with scaleFactors
        table.setValueAt(table.getValueAt(row, column) * scaleFactors[column-1],
          row, column);
        
        
      //correct data with baselineValues
        table.setValueAt(Math.max(table.getValueAt(row, column) + currentBaseline, 0d), row, column);
      }
    }
    
  }
  
  public static void main(String args[]) throws XMLStreamException, IOException, SBMLException {
    for(double averagePrecision=0.0;averagePrecision<=0.05;averagePrecision+=0.01) {
      for(double systematicErrorPercentage=0.0;systematicErrorPercentage<=5.0;systematicErrorPercentage+=1.0) {
        for(double baselinePercentage=0.0;baselinePercentage<=5.0;baselinePercentage+=1.0) {
          for(int repetition=1;repetition<=3;repetition++) {
            averagePrecision=Math.round(averagePrecision*100)/100.0;
            systematicErrorPercentage=Math.round(systematicErrorPercentage);
            baselinePercentage=Math.round(baselinePercentage);
            String file="files/ " + "dataWithError_" + (int)(averagePrecision*100) + "_" + (int)systematicErrorPercentage + "_" + (int)baselinePercentage + "_" + repetition  +".csv";
            introduceError(args[0],file,averagePrecision,systematicErrorPercentage,baselinePercentage);
          }
        }
      }
    }
  }
}
