/*
 * $Id: StabilityMatrix.java 338 2012-02-17 07:03:11Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/SBMLsimulator/branches/Andreas/src/org/sbml/simulator/math/StabilityMatrix.java $
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import eva2.tools.math.Jama.EigenvalueDecomposition;
import eva2.tools.math.Jama.Matrix;

/**
 * This Class extends the representation of a m x n Matrix with some additional
 * functions
 * 
 * @author Alexander D&ouml;rr
 * @date 2009-12-18
 * @version $Rev: 338 $
 * @since 1.0
 */
public class StabilityMatrix extends Matrix {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = -2678044872448572954L;

	/**
	 * Creates a m x n StabilityMatrix
	 * 
	 * @param m
	 *            number of rows
	 * @param n
	 *            number of columns
	 */
	public StabilityMatrix(int m, int n) {
		super(m, n);
	}

	/**
	 * Creates a m x n StabilityMatrix with all entries set to c
	 * 
	 * @param m
	 *            number of rows
	 * @param n
	 *            number of columns
	 * @param c
	 *            scalar value
	 */
	public StabilityMatrix(int m, int n, int c) {
		super(m, n, c);
	}

	/**
	 * Creates a m x n StabilityMatrix with the entries given in array
	 * 
	 * @param array
	 *            entries
	 * @param m
	 *            number of rows
	 * @param n
	 *            number of columns
	 */
	public StabilityMatrix(double[][] array, int m, int n) {
		super(array, m, n);
	}
	
	/* (non-Javadoc)
   * @see eva2.tools.math.Jama.Matrix#transpose()
   */
  @Override
  public StabilityMatrix transpose() {
    int m=this.getRowDimension();
    int n= this.getColumnDimension();
    StabilityMatrix X = new StabilityMatrix(n,m);
    double[][] C = X.getArray();
    for (int i = 0; i < m; i++) {
       for (int j = 0; j < n; j++) {
          C[j][i] = this.get(i, j);
       }
    }
    return X;
  }

  /**
	 * Returns the real part of the eigenvalues of this matrix
	 * 
	 * @return array with the eigenvalues
	 */
	public double[] getEigenvalues() {
		return (new EigenvalueDecomposition(this)).getRealEigenvalues();
	}

	/**
	 * Substitutes the values of row m with the given values
	 * 
	 * @param m
	 *            number of the row to be changed
	 * @param row
	 *            the new values of row m
	 */
	public void setRow(int m, double[] row) {
		for (int i = 0; i < this.getColumnDimension(); i++) {
			this.set(m, i, row[i]);
		}
	}

	/**
	 * Copy a row from the matrix.
	 * 
	 * @return row m in a one-dimensional array
	 */
	public double[] getRow(int m) {
		double[] vals = new double[this.getColumnDimension()];
		for (int i = 0; i < this.getColumnDimension(); i++) {
			vals[i] = this.get(m, i);
		}
		return vals;
	}

	/**
	 * Substitutes the values of column n with the given values
	 * 
	 * @param n
	 *            number of the column to be changed
	 * @param row
	 *            the new values of column n
	 */
	public void setColumn(int n, double[] column) {
		for (int i = 0; i < this.getRowDimension(); i++) {
			this.set(i, n, column[i]);
		}
	}

	/**
	 * Returns a copy of this matrix without row m and column n
	 * 
	 * @param m
	 *            row index
	 * @param n
	 *            column index
	 * @return this matrix without row m and column n
	 */
	public StabilityMatrix getSubmatrix(int m, int n) {

		StabilityMatrix submatrix = new StabilityMatrix(
				this.getRowDimension() - 1, this.getColumnDimension() - 1);
		int subm = 0, subn = 0;

		for (int row = 0; row < this.getRowDimension(); row++) {

			if (row == m) {
				continue;
			}
			for (int column = 0; column < this.getColumnDimension(); column++) {

				if (column == n) {
					continue;
				}
				submatrix.set(subm, subn, this.get(row, column));
				subn++;
			}
			subn = 0;
			subm++;
		}

		return submatrix;
	}

	/**
	 * Returns a copy of this matrix without rows and columns with an number
	 * greater than index
	 * 
	 * @param index
	 *            max row/column number
	 * @return this matrix without rows and columns greater than the given index
	 */
	public StabilityMatrix getSubmatrix(int index) {
		StabilityMatrix submatrix = new StabilityMatrix(index + 1, index + 1);

		for (int r = 0; r <= index; r++) {
			for (int c = 0; c <= index; c++) {
				submatrix.set(r, c, this.get(r, c));
			}
		}

		return submatrix;
	}

	/**
	 * Returns a new StabilityMatrix without the given columns
	 * 
	 * @param integers
	 *            array with indices of the columns to be dropped, the indices
	 *            have to be unique and in ascending order
	 * 
	 * @return
	 */
	public StabilityMatrix dropColumns(Integer[] integers) {
		StabilityMatrix submatrix = new StabilityMatrix(getRowDimension(),
				getColumnDimension() - integers.length);

		for (int i = 0, n = 0; i < getColumnDimension(); i++) {

			if (Arrays.binarySearch(integers, i) < 0) {
				submatrix.setColumn(n, getColumn(i));
				n++;
			}
		}

		return submatrix;
	}

	/**
	 * Returns a new StabilityMatrix without the given rows
	 * 
	 * @param integers
	 *            array with indices of the rows to be dropped, the indices have
	 *            to be unique and in ascending order
	 * 
	 * @return
	 */
	public StabilityMatrix dropRows(Integer[] integers) {
		StabilityMatrix submatrix = new StabilityMatrix(getRowDimension()
				- integers.length, getColumnDimension());

		for (int i = 0, n = 0; i < getRowDimension(); i++) {

			if (Arrays.binarySearch(integers, i) < 0) {
				submatrix.setRow(n, getRow(i));
				n++;
			}
		}

		return submatrix;
	}

	/**
	 * Changes the position of column i with column j and vice versa
	 * 
	 * @param i
	 * @param j
	 */
	public void swapColumns(int i, int j) {
		double[] columni = this.getColumn(i);

		this.setColumn(i, this.getColumn(j));
		this.setColumn(j, columni);
	}

	/**
	 * Changes the position of column i with column j and vice versa
	 * 
	 * @param i
	 * @param j
	 */
	public void swapRows(int i, int j) {
		double[] rowi = this.getRow(i);

		this.setRow(i, this.getRow(j));
		this.setRow(j, rowi);
	}

	/**
	 * Returns a HashSet with all column indeces at which the entry is equal to
	 * value
	 * 
	 * @param m
	 *            row index
	 * @return
	 */
	public Set<Integer> getColIndecesEqual(int m, double value) {
		HashSet<Integer> set = new HashSet<Integer>();
		for (int i = 0; i < getColumnDimension(); i++) {
			if (get(m, i) == value) {
				set.add(i);
			}
		}

		return set;
	}

	/**
	 * Returns an array with all column indeces at which the entry is not equal
	 * to the given value
	 * 
	 * @param m
	 *            row index
	 * @return
	 */
	public Integer[] getColIndecesDiffer(int m, double value) {
		ArrayList<Integer> list = new ArrayList<Integer>();

		for (int i = 0; i < getColumnDimension(); i++) {
			if (get(m, i) != value) {
				list.add(i);
			}
		}

		Integer arr[] = new Integer[list.size()];

		return list.toArray(arr);
	}

	/**
	 * Checks if all entries in this matrix are zero
	 * 
	 * @return
	 */
	public boolean allZero() {
		for (int i = 0; i < this.getRowDimension(); i++) {
			for (int j = 0; j < this.getColumnDimension(); j++) {
				if (this.get(i, j) != 0d){
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Sums up the given row with the row at position m and returns the result
	 * 
	 * @param m
	 * @param tosum
	 * @return
	 */
	public double[] plusRow(int m, double[] tosum) {
		double[] row = this.getRow(m);

		for (int i = 0; i < row.length; i++) {
			row[i] += tosum[i];
		}

		return row;
	}

	/**
	 * 
	 * @param m
	 * @param scalar
	 * @return
	 */
	public double[] mulitplyRow(int m, double scalar) {
		double[] row = this.getRow(m);

		for (int i = 0; i < row.length; i++) {
			row[i] = row[i] * scalar;
		}

		return row;
	}

	/**
	 * Clone the StabilityMatrix object.
	 */
	@Override
	public StabilityMatrix clone() {
		return this.copy();
	}

	/**
	 * Returns a deep copy of a this StabilityMatrix
	 */
	@Override
	public StabilityMatrix copy() {
		StabilityMatrix copy = new StabilityMatrix(this.getRowDimension(), this
				.getColumnDimension());
		double[][] array = copy.getArray();
		for (int i = 0; i < this.getRowDimension(); i++) {
			for (int j = 0; j < this.getColumnDimension(); j++) {
				array[i][j] = this.get(i, j);
			}
		}
		return copy;
	}

	public StabilityMatrix times(StabilityMatrix B) {
		return new StabilityMatrix((((Matrix) (this)).times((Matrix) B))
				.getArray(), this.getRowDimension(), B.getColumnDimension());
	}

}
