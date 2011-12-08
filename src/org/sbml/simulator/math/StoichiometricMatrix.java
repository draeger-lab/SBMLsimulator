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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import eva2.tools.math.Jama.LUDecomposition;
import eva2.tools.math.Jama.Matrix;

/**
 * This Class represents a m x n stoichiometric matrix
 * 
 * @author Alexander D&ouml;rr
 * @date 2010-12-23
 * @version $Rev$
 * @since 1.0
 */
public class StoichiometricMatrix extends StabilityMatrix {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 8418337736328552077L;
	
	/**
	 * A {@link Logger} for this class.
	 */
	private static final Logger logger = Logger.getLogger(StoichiometricMatrix.class.getName());
	
	/**
	 * A m x r matrix holding the link matrix for the reduced stoichiometric
	 * matrix of this matrix
	 */
	private StabilityMatrix linkMatrix = null;
	
	/**
	 * A r x n matrix holding the reduced form of this matrix
	 */
	private StabilityMatrix reducedMatrix = null;
	
	/**
	 * A (m - r) x m matrix holding the conservation relations/moieties of this
	 * matrix
	 */
	private StabilityMatrix conservationRelations = null;
	
	/**
	 * A n x (n - r) matrix holding the feasible steady state fluxes of this
	 * matrix
	 */
	private StabilityMatrix steadyStateFluxes = null;
	
	/**
	 * A HashMap containing inversion of the permutations of the metabolites due
	 * to the LU decomposition and the algorithm
	 */
	private Map<Integer, Integer> permutations = null;
	
	/**
	 * A HashSet containing all indices of the removed rows when computing the
	 * reduced stoichiometric matrix
	 */
	private Set<Integer> linearDependent;
	
	/**
	 * A linked list containing the indices of the removed rows in the same
	 * order as they appear in the rows of the conservation relations
	 */
	private List<Integer> removedRows;
	
	/**
	 * The rank of this matrix
	 */
	private int rank;

	/**
	 * Creates a m x n stoichiometric matrix N
	 * 
	 * @param m
	 *            number of rows
	 * @param n
	 *            number of columns
	 */
	public StoichiometricMatrix(int m, int n) {
		super(m, n);
	}

	/**
	 * Creates a m x n stoichiometric matrix N with all entries set to c
	 * 
	 * @param m
	 *            number of rows
	 * @param n
	 *            number of columns
	 * @param c
	 *            scalar value
	 */
	public StoichiometricMatrix(int m, int n, int c) {
		super(m, n, c);
	}

	/**
	 * Creates a m x n stoichiometric matrix N with the entries given in array
	 * 
	 * @param array
	 *            entries
	 * @param m
	 *            number of rows
	 * @param n
	 *            number of columns
	 */
	public StoichiometricMatrix(double[][] array, int m, int n) {
		super(array, m, n);
	}

	/**
	 * Returns the link matrix of this matrix
	 * 
	 * @return
	 */
	public StabilityMatrix getLinkMatrix() {
		if (linkMatrix == null) {
			reduceModel();
		}

		return linkMatrix;
	}

	/**
	 * Returns the reduced form of this matrix
	 * 
	 * @return
	 */
	public StabilityMatrix getReducedMatrix() {
		if (reducedMatrix == null) {
			reduceModel();
		}

		return reducedMatrix;
	}

	/**
	 * Returns the conservation relations of this matrix
	 * 
	 * @return
	 */
	public StabilityMatrix getConservationRelations() {
		if (conservationRelations == null) {
			reduceModel();
		}

		return conservationRelations;
	}

	/**
	 * This method calculates the reduced stoichiometric matrix with its
	 * metabolic link matrix and its conservation relations as side products.
	 * Refering to Palsson, the conservation relations are also known as the
	 * left null space of N. This is achieved by performing an QR decomposition
	 * as described in "Conservation analysis of large biochemical networks" by
	 * Ravishankar Rao Vallabhajosyula and Herbert Sauro. The permutation matrix
	 * is calculated through LU decomposition.
	 * 
	 */
	private void reduceModel() {
		StabilityMatrix stoich;
		permutations = new HashMap<Integer, Integer>();
		removedRows = new LinkedList<Integer>();

		if (!((this.getRowDimension() > 0) && (this.getColumnDimension() > 0))) {
			logger.fine("Wrong dimensions");
		}

		// check if matrix has to be augmented befor performing the LU
		// decomposition
		if (this.getRowDimension() >= this.getColumnDimension()) {
			stoich = this;
		} else {
			stoich = augmentN();
		}

		LUDecomposition lu = new LUDecomposition(stoich);

		int m = stoich.getRowDimension();

		StabilityMatrix P = new StabilityMatrix(m, m, 0);

		// build the permutation matrix out of the pivot elements from the LU
		// decomposition
		int[] pivot = lu.getPivot();
		for (int i = 0; i < pivot.length; i++) {
			P.set(pivot[i], i, 1);
		}
		// System.out.println("P");
		// System.out.println(P);

		QRDecomposition qr = new QRDecomposition(stoich.transpose().times(P));

		// Matrix Q = qr.getQ();
		Matrix R = roundValues(qr.getR());

		// System.out.println("Q");
		// System.out.println(Q);
		//
		// System.out.println("R");
		// System.out.println(R);

		StabilityMatrix Rt = new StabilityMatrix(R.copy().getArray(),
				R.getRowDimension(), R.getColumnDimension());

		// dividing each row its diagonal element
		for (int i = 0; i < Rt.getRowDimension(); i++) {
			double unity = Rt.get(i, i);
			if (unity != 0d) {
				for (int j = 0; j < Rt.getColumnDimension(); j++) {
					Rt.set(i, j, (Math.round(Rt.get(i, j) / unity * 1E4d)) / 1E4d);
				}
			}
		}
		rank = Rt.getRowDimension();

		// Gauss-Jordan reduction to eliminate non-zero values above the
		// diagonal
		for (int i = Rt.getRowDimension() - 1; i >= 0; i--) {
			if (Rt.get(i, i) == 0d) {
				rank--;

			} else {

				double[] row = Rt.getRow(i);

				for (int j = i - 1; j >= 0; j--) {
					double value = Rt.get(j, i);

					for (int k = row.length - 1; k >= j; k--) {
						Rt.set(j, k, Rt.get(j, k) - (value * row[k]));
					}

				}

			}

		}
		// System.out.println("rank: " + rank);
		// System.out.println("Rt");
		// System.out.println(Rt);

		StabilityMatrix L = new StabilityMatrix(this.getRowDimension(), rank);
		StabilityMatrix Lo = new StabilityMatrix(this.getRowDimension() - rank,
				this.getRowDimension());

		buildReducedN(P);

		// build the conservation and the link matrix
		int l = 0, n, loindex;
		double value;
		double[] column;
		for (int i = 0; i < L.getRowDimension(); i++) {
			if (i < rank) {
				L.set(i, i, 1);
			} else {
				column = Rt.getColumn(i);
				for (int j = 0; j < L.getColumnDimension(); j++) {
					if (column[j] != 0d) {
						value = Math.round(column[j] * 1E3d) / 1E3d;
						n = permutations.get(j);
						Lo.set(l, n, -value);
					}
				}

				m = permutations.get(i);
				Lo.set(l, m, 1);

				l++;
				loindex = 0;

				for (int j = 0; j < Lo.getColumnDimension(); j++) {
					if (!linearDependent.contains(j)) {
						if (Lo.get(i - rank, j) != 0) {
							L.set(i, loindex, -Lo.get(i - rank, j));
						}
						loindex++;
					}
				}
			}
		}

		this.linkMatrix = L;
		this.conservationRelations = Lo;

	}

	/**
	 * When the number of reactions is greater than the number of metabolites,
	 * the LU decomposition does not work. This is circumvented by adding
	 * additional zero rows at the end of the matrix as described in
	 * "Conservation analysis of large biochemical networks" by Ravishankar Rao
	 * Vallabhajosyula and Herbert Sauro.
	 * 
	 * @return
	 */
	private StabilityMatrix augmentN() {
		StabilityMatrix augmentedN = new StabilityMatrix(
				this.getColumnDimension(), this.getColumnDimension(), 0);

		for (int i = 0; i < this.getRowDimension(); i++) {
			augmentedN.setRow(i, this.getRow(i));
		}

		return augmentedN;
	}

	/**
	 * This method uses the permutation matrix to build the reduced
	 * stoichiometric matrix
	 * 
	 * @param permutationMatrix
	 */
	private void buildReducedN(StabilityMatrix permutationMatrix) {
		linearDependent = new HashSet<Integer>();
		reducedMatrix = new StabilityMatrix(rank, this.getColumnDimension());

		// save permutations of the rows due to the LU
		// decomposition and add dependent rows to the hash, strictly speaking
		// rows that are moved to a new position with a higher index than the
		// rank
		for (int i = 0; i < this.getRowDimension(); i++) {
			for (int j = 0; j < this.getRowDimension(); j++) {

				if (permutationMatrix.get(i, j) == 1.0) {
					if (j >= rank) {
						linearDependent.add(i);
						removedRows.add(i);
					}
					permutations.put(j, i);
					continue;

				}
			}
		}

		int l = 0;

		// exclude linear dependent rows when building the reduced
		// stoichiometric matrix
		for (int i = 0; i < this.getRowDimension(); i++) {

			if (!linearDependent.contains(i)) {
				reducedMatrix.setRow(l, this.getRow(i));
				l++;
			}
		}
	}

	/**
	 * This method calculates the feasible steady state fluxes of the model with
	 * this stoichiometric matrix, referring to Palsson also known as the null
	 * space of N. This is achieved by performing an SVD decomposition as
	 * described in "Systems Biology: Properties of Reconstructed Networks" by
	 * Bernhard O. Palsson.
	 */
	private void calculateSteadyStateFluxes() {
		SingularValueDecomposition svd = new SingularValueDecomposition(this);
		int rank = svd.rank();
        
		steadyStateFluxes = new StabilityMatrix(this.getColumnDimension(),
				this.getColumnDimension() - rank);
		Matrix V = svd.getV();

		for (int i = 0; i < this.getColumnDimension() - rank; i++) {
			for (int j = 0; j < this.getColumnDimension(); j++) {
				steadyStateFluxes.set(j, i, V.get(j, rank + i));
			}
		}

	}

	/**
	 * This method changes values that are as good as zero to zero due to
	 * numerical inaccuracy.
	 * 
	 * @param matrix
	 * @return
	 */
	public static Matrix roundValues(Matrix matrix) {
		for (int m = 0; m < matrix.getRowDimension(); m++) {
			for (int n = 0; n < matrix.getColumnDimension(); n++) {

				if (matrix.get(m, n) < 1E-9 && matrix.get(m, n) > 0) {
					matrix.set(m, n, 0);
				} else if (matrix.get(m, n) > -1E-9 && matrix.get(m, n) < 0) {
					matrix.set(m, n, 0);
				}

			}
		}
		return matrix;
	}

	/**
	 * Returns the feasible steady state fluxes of this matrix
	 * 
	 * @return
	 */
	public StabilityMatrix getSteadyStateFluxes() {
		if (steadyStateFluxes == null) {
			calculateSteadyStateFluxes();
		}
		return steadyStateFluxes;
	}

	/**
	 * This method returns a linked list of integer values corresponding to the
	 * rows of this matrix, that are removed when building the reduced
	 * stoichiometric matrix. Please note that the indices correspond to the
	 * array notation, where e.g. 0 stands for the first row in the matrix.
	 * Their sequence corresponds the order of the appearance in the
	 * conservation relations.
	 * 
	 * @return
	 */
	public List<Integer> getLinearDependent() {
		if (removedRows == null) {
			reduceModel();
		}
		return removedRows;
	}

}
