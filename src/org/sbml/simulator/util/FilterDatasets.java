package org.sbml.simulator.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLException;
import org.simulator.math.odes.MultiTable;

import de.zbit.io.CSVWriter;

public class FilterDatasets {
  public static void main(String args[]) throws XMLStreamException,
    IOException, SBMLException {
    double[] timepoints = new double[11];
    double time = 0d;
//    for (int i=0;i!=25;i++) {
//      timepoints[i]=time;
//      time=BigDecimal.valueOf(time).add(BigDecimal.valueOf(0.1)).doubleValue();
//    }
//    for (int i=0;i<=10;i++) {
//      timepoints[i]=time;
//      time=BigDecimal.valueOf(time).add(BigDecimal.valueOf(1)).doubleValue();
//    }
    Set<String> columns = new HashSet<String>();
    columns.add("A");
    columns.add("D");
    columns.add("E");
    columns.add("F");
    
    for (double averagePrecision = 0.0; averagePrecision <= 0.2; averagePrecision += 0.01) {
      for (double systematicErrorPercentage = 0; systematicErrorPercentage <= 20.0; systematicErrorPercentage += 1.0) {
        for (double baselinePercentage = 0; baselinePercentage <= 20.0; baselinePercentage += 1.0) {
          for (int repetition = 1; repetition <= 3; repetition++) {
            averagePrecision = Math.round(averagePrecision * 100) / 100.0;
            systematicErrorPercentage = Math.round(systematicErrorPercentage);
            baselinePercentage = Math.round(baselinePercentage);
            String file = "files/Testdaten/" + "dataWithError_"
                + (int) (averagePrecision * 100) + "_"
                + (int) systematicErrorPercentage + "_"
                + (int) baselinePercentage + "_" + repetition + ".csv";
            String file2 = "files/Testdaten/" + "dataWithError_"
            + (int) (averagePrecision * 100) + "_"
            + (int) systematicErrorPercentage + "_"
            + (int) baselinePercentage + "_" + repetition + "_WithoutBC.csv";
            filterColumns(file, file2, columns);
          }
        }
      }
    }
  }
  
  public static void filterTimepoints(String inputFile, String outputFile,
    double[] timepoints) throws XMLStreamException, IOException, SBMLException {
    //read file with data
    BufferedReader reader = new BufferedReader(new FileReader(inputFile));
    ArrayList<double[]> data = new ArrayList<double[]>();
    
    try {
      String line = null;
      
      line = reader.readLine();
      String[] identifiers = line.split(",");
      while ((line = reader.readLine()) != null) {
        
        String[] splits = line.split(",");
        if (Arrays.binarySearch(timepoints, Double.valueOf(splits[0])) >= 0) {
          
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
      }
      reader.close();
      
      if (data.size() > 0) {
        double[][] dataMatrix = data.toArray(new double[data.size()][]);
        MultiTable table = new MultiTable(timepoints, dataMatrix,
          identifiers);
        
        if (outputFile != null) {
          CSVWriter writer = new CSVWriter();
          writer.write(table, ',', outputFile);
        }
        
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public static void filterColumns(String inputFile, String outputFile,
    Set<String> columns) throws XMLStreamException, IOException, SBMLException {
    
    
    try {
      //read file with data
      BufferedReader reader = new BufferedReader(new FileReader(inputFile));
      ArrayList<double[]> data = new ArrayList<double[]>();
      String line = null;
      List<Double> timepointsList = new ArrayList<Double>();
      line = reader.readLine();
      String[] identifiers = line.split(",");
      List<Integer> columnNumbers = new LinkedList<Integer>();
      ArrayList<String> newIdentifiers = new ArrayList<String>();
      newIdentifiers.add("Time");
      for (int i = 0; i != identifiers.length; i++) {
        if (columns.contains(identifiers[i])) {
          columnNumbers.add(i);
          newIdentifiers.add(identifiers[i]);
        }
      }
      while ((line = reader.readLine()) != null) {
        String[] splits = line.split(",");
        timepointsList.add(Double.valueOf(splits[0]));
        double[] row = new double[columnNumbers.size()];
        data.add(row);
        int i = 0;
        for (int col : columnNumbers) {
          row[i] = Double.valueOf(splits[col]);
          i++;
        }
        
      }
      reader.close();
      
      if (data.size() > 0) {
        double[] timepoints = new double[timepointsList.size()];
        for(int i=0;i!=timepoints.length;i++) {
          timepoints[i]=timepointsList.get(i);
        }
        double[][] dataMatrix = data.toArray(new double[data.size()][]);
        MultiTable table = new MultiTable(timepoints, dataMatrix,
          newIdentifiers.toArray(new String[newIdentifiers.size()]));
        
        if (outputFile != null) {
          CSVWriter writer = new CSVWriter();
          writer.write(table, ',', outputFile);
        }
        
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
