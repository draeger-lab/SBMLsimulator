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
package org.sbml.simulator.math;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Alexander D&ouml;rr
 * @version $Rev$
 * @since 1.0
 */
public class ConservationRelations {

	/**
	 * Calculates all semi-positive conservation relations in a chemical
	 * reaction system with the tableau algorithm
	 */
	public static StabilityMatrix calculateConsRelations(StabilityMatrix stoichiometric) {
		// tableauLeft and tableauRight are representing together the whole
		// tableau T
		StabilityMatrix tableauLeft = stoichiometric;
		StabilityMatrix tableauRight = new IdentityMatrix(stoichiometric
				.getRowDimension());
		// backup varibales for the tableaux
		StabilityMatrix backupLeft, backupRight;

		List<Set<Integer>> listOfSetsS;
		List<int[]> combinations = new ArrayList<int[]>();
		List<Integer> listOfZeroRows = new ArrayList<Integer>();

		int j = 0, i = 0, rownum = 0;

		// for each column and as long as there are remaining tableaux to build
		// and matrixS contains not only zero values
		for (j = 0; j < tableauLeft.getColumnDimension()
				&& !tableauLeft.allZero(); j++) {
			// build sets
			listOfSetsS = buildSets(tableauRight);
			for (i = 0; i < tableauLeft.getRowDimension(); i++) {
				// for each row
				if (tableauLeft.get(i, j) == 0) {
					listOfZeroRows.add(Integer.valueOf(i));
					continue; // violating condition (3.7) anyways.
				}
				// for each remaining row
				for (int k = i + 1; k < tableauLeft.getRowDimension(); k++) {
					// check conditions
					if (tableauLeft.get(k, j) == 0)
						continue; // otherwise violating condition (3.7)

					if (tableauLeft.get(i, j) * tableauLeft.get(k, j) < 0) {
						// condition (3.7) satisfied now checking (3.8)
						Set<Integer> intersection = new HashSet<Integer>(
								listOfSetsS.get(i));
						intersection.retainAll(listOfSetsS.get(k));
						boolean isSubset = false;
						for (int l = 0; l < listOfSetsS.size() && !isSubset; l++) {
							if ((l == k) || (l == i)) {
								continue;
							}
							if (listOfSetsS.get(l).containsAll(intersection))
								isSubset = true;
						}
						if (!isSubset) // condition (3.8) satisfied
							// create a new combination of rows
							combinations.add(new int[] { i, k });
					}
				}
			}
			backupLeft = new StabilityMatrix(combinations.size()
					+ listOfZeroRows.size(), tableauLeft.getColumnDimension(),
					0);
			backupRight = new StabilityMatrix(combinations.size()
					+ listOfZeroRows.size(), tableauRight.getColumnDimension(),
					0);

			// build T(j+1)

			// uses all found combinations to construct rows for T(j+1)
			// single rows for addition und multiplication
			StabilityMatrix rowS1 = new StabilityMatrix(1, stoichiometric
					.getColumnDimension());
			StabilityMatrix rowS2 = new StabilityMatrix(1, stoichiometric
					.getColumnDimension());
			StabilityMatrix rowI1 = new StabilityMatrix(1, stoichiometric
					.getRowDimension());
			StabilityMatrix rowI2 = new StabilityMatrix(1, stoichiometric
					.getRowDimension());
			// compute all theta values
			for (rownum = 0; rownum < combinations.size(); rownum++) {
				// calculate a row for the left tableau
				// ith row of left tableau
				rowS1
						.setRow(0, tableauLeft
								.getRow(combinations.get(rownum)[0]));
				// kth element of column j in left tableau
				rowS1.multi(Math.abs(tableauLeft.get(
						combinations.get(rownum)[1], j)));
				// kth row of left tableau
				rowS2
						.setRow(0, tableauLeft
								.getRow(combinations.get(rownum)[1]));
				// ith element of column j in left tableau
				rowS2.multi(Math.abs(tableauLeft.get(
						combinations.get(rownum)[0], j)));
				// theta left:
				backupLeft.setRow(rownum, rowS1.plusRow(0, rowS2.getRow(0)));

				// calculate a row for the right tableau
				// ith row of right tableau
				rowI1.setRow(0, tableauRight
						.getRow(combinations.get(rownum)[0]));
				// kth element of jth column in left tableau (because j is in
				// [0,..,r])
				rowI1.multi(Math.abs(tableauLeft.get(
						combinations.get(rownum)[1], j)));
				// kth row of right tableau
				rowI2.setRow(0, tableauRight
						.getRow(combinations.get(rownum)[1]));
				// ith element of jth column in left tableau
				rowI2.multi(Math.abs(tableauLeft.get(
						combinations.get(rownum)[0], j)));
				// theta right:
				backupRight.setRow(rownum, rowI1.plusRow(0, rowI2.getRow(0)));
			}

			// takes over all rows from T(j) to T(j+1) where the entry at column
			// j is zero
			for (i = 0; i < listOfZeroRows.size(); i++) {
				backupLeft.setRow(rownum, tableauLeft.getRow(listOfZeroRows
						.get(i).intValue()));
				backupRight.setRow(rownum, tableauRight.getRow(listOfZeroRows
						.get(i).intValue()));
				rownum++;
			}
			tableauLeft = backupLeft.clone();
			tableauRight = backupRight.clone();

			combinations.clear();
			listOfZeroRows.clear();

		}
		
		return tableauRight;
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
	private static List<Set<Integer>> buildSets(StabilityMatrix matrixI) {
		List<Set<Integer>> listOfSets = new LinkedList<Set<Integer>>();
		for (int i = 0; i < matrixI.getRowDimension(); i++) {
			listOfSets.add(matrixI.getColIndecesEqual(i, 0));
		}
		return listOfSets;
	}

}
