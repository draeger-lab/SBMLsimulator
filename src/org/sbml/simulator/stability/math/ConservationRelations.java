/*
 * $Id: ConservationRelations.java 338 2012-02-17 07:03:11Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/SBMLsimulator/branches/Andreas/src/org/sbml/simulator/math/ConservationRelations.java $
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2012 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.stability.math;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * @author Alexander D&ouml;rr
 * @version $Rev: 338 $
 * @since 1.0
 */
public class ConservationRelations {

	/**
	 * Calculates all semi-positive conservation relations in a chemical
	 * reaction system with the tableau algorithm
	 */
	@SuppressWarnings("unchecked")
	public static StabilityMatrix calculateConsRelations(
			StabilityMatrix stoichiometric) {
		// tableauLeft and tableauRight are representing together the whole
		// tableau T
		StabilityMatrix ident = new IdentityMatrix(
				stoichiometric.getRowDimension());
		StabilityMatrix ret;
		

		List<Set<Integer>> listOfSetsS;
		List<int[]> combinations = new ArrayList<int[]>();
		List<Integer> listOfZeroRows = new ArrayList<Integer>();
		LinkedList<double[]> tableauLeft = new LinkedList<double[]>();
		LinkedList<double[]> tableauRight = new LinkedList<double[]>();

		for (int i = 0; i < stoichiometric.getRowDimension(); i++) {
			tableauLeft.add(stoichiometric.getRow(i));
		}
		for (int i = 0; i < ident.getRowDimension(); i++) {
			tableauRight.add(ident.getRow(i));
		}

		int j = 0, i = 0, rownum = 0;
		// for each column and as long as there are remaining tableaux to build
		// and matrixS contains not only zero values
		for (j = 0; j < stoichiometric.getColumnDimension() && !allZero(tableauLeft, j); j++) {
			System.out.println("j" + j + "size: " + tableauLeft.size());
			// System.out.println("j: " + j + " / " +
			// tableauLeft.getColumnDimension() + " rowDimension" +
			// tableauLeft.getRowDimension());
			// build sets
			listOfSetsS = buildSets(tableauRight);
			for (i = 0; i < tableauLeft.size(); i++) {
//				System.out.println(" i:" + i);
				// for each row
				if (get(tableauLeft, i, j) == 0) { 
					listOfZeroRows.add(Integer.valueOf(i));
					continue; // violating condition (3.7) anyways.
				}
				// for each remaining row
				for (int k = i + 1; k < tableauLeft.size(); k++) {
					//System.out.println("  k:" + k);
					// check conditions
					if (get(tableauLeft, k, j) == 0)
						continue; // otherwise violating condition (3.7)

					//System.out.println(get(tableauLeft, i, j) + " "+get(tableauLeft, k, j));
					if (get(tableauLeft, i, j) * get(tableauLeft, k, j) < 0) {
						// condition (3.7) satisfied now checking (3.8)
						//Set<Integer> intersection = new HashSet<Integer>(listOfSetsS.get(i));
						Set<Integer> intersection = new TreeSet<Integer>(listOfSetsS.get(i));
						intersection.retainAll(listOfSetsS.get(k));
						boolean isSubset = false;
						for (int l = 0; l < listOfSetsS.size() && !isSubset; l++) {
							if ((l == k) || (l == i)) {
								continue;
							}
							if (listOfSetsS.get(l).containsAll(intersection))
								isSubset = true;
						}
						if (!isSubset){ // condition (3.8) satisfied
							// create a new combination of rows
							combinations.add(new int[] { i, k });
						}
					}
				}
			}

			// backupLeft = new StabilityMatrix(combinations.size()
			// + listOfZeroRows.size(), tableauLeft.getColumnDimension(),
			// 0);
			// backupRight = new StabilityMatrix(combinations.size()
			// + listOfZeroRows.size(), tableauRight.getColumnDimension(),
			// 0);
			
			// backup variables for the tableaux
			LinkedList<double[]> backupLeft = new LinkedList<double[]>();
			LinkedList<double[]> backupRight = new LinkedList<double[]>();

			// build T(j+1)

			// uses all found combinations to construct rows for T(j+1)
			// single rows for addition and multiplication
			double[] rowS1;
			double[] rowS2;
			double[] rowI1;
			double[] rowI2;			

			// compute all theta values
			for (rownum = 0; rownum < combinations.size(); rownum++) {
				// calculate a row for the left tableau
				// i-th row of left tableau
				rowS1 = tableauLeft.get(combinations.get(rownum)[0]);
				
				// kth element of column j in left tableau
				// kth row of left tableau
				rowS2 = tableauLeft.get(combinations.get(rownum)[1]);
				
				// ith element of column j in left tableau
				
				// theta left:
				backupLeft.add(combine(rowS1, rowS2, 
						Math.abs(tableauLeft.get(combinations.get(rownum)[1])[j]), 
						Math.abs(tableauLeft.get(combinations.get(rownum)[0])[j])
						));

				// calculate a row for the right tableau
				// ith row of right tableau
				rowI1 = tableauRight.get(combinations.get(rownum)[0]);
				// kth element of jth column in left tableau (because j is in
				// [0,..,r])

				// kth row of right tableau
				rowI2 = tableauRight.get(combinations.get(rownum)[1]);
				
				// ith element of jth column in left tableau
				// theta right:
				
				backupRight.add(combine(rowI1, rowI2, Math.abs(tableauLeft.get(
						combinations.get(rownum)[1])[j]), Math.abs(tableauLeft.get(
								combinations.get(rownum)[0])[j])));
			}

			// takes over all rows from T(j) to T(j+1) where the entry at column
			// j is zero
			for (i = 0; i < listOfZeroRows.size(); i++) {
				backupLeft.add(tableauLeft.get(listOfZeroRows.get(i).intValue()));
				backupRight.add(tableauRight.get(listOfZeroRows.get(i).intValue()));
				rownum++;
			}
			tableauLeft = (LinkedList<double[]>) backupLeft.clone();
			tableauRight = (LinkedList<double[]>) backupRight.clone();
			
			combinations.clear();
			listOfZeroRows.clear();

		}
		ret = new StabilityMatrix(tableauRight.size(), stoichiometric.getRowDimension());
		for (int k = 0; k < ret.getRowDimension(); k++) {
			ret.setRow(k, tableauRight.get(k));
		}
		
		
		return ret;
	}

	public static double get(LinkedList<double[]> rows, int i, int j) {
		return rows.get(i)[j];
	}
	
	public static double[] combine(double[] row1, double[] row2, double value1,double value2){
		double[] ret = new double[row1.length];
		
		for (int i = 0; i < ret.length; i++) {
			ret[i] = (row1[i] *value1) + (row2[i] *value2);
		}
		return ret;
	}

	public static boolean allZero(LinkedList<double[]> tableauLeft, int k) {
		for (int i = 0; i < tableauLeft.size(); i++) {
			double[] row = tableauLeft.get(i);
			for (int j = k; j < row.length; j++) {
				if (row[j] != 0d) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Returns a list of sets (one for each row) of a matrix with the column
	 * indices where the elements are zero
	 * 
	 * @param matrixI
	 *            matrix from which to build the sets
	 * @param listofsets
	 *            ArrayList to save the sets
	 */
	private static List<Set<Integer>> buildSets(List<double[]> rows) {

		List<Set<Integer>> listOfSets = new LinkedList<Set<Integer>>();
		for (int i = 0; i < rows.size(); i++) {
			double[] row = rows.get(i);
			//HashSet<Integer> set = new HashSet<Integer>();
			TreeSet<Integer> set = new TreeSet<Integer>();

			for (int j = 0; j < row.length; j++) {
				if (row[j] == 0d) {
					set.add(j);
				}				
			}
			listOfSets.add(set);
		}
		return listOfSets;
	}

}
