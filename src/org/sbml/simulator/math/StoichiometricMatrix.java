package org.sbml.simulator.math;

import java.util.HashMap;
import java.util.HashSet;

import eva2.tools.math.Jama.LUDecomposition;
import eva2.tools.math.Jama.Matrix;

/**
 * This Class represents a m x n stoichimetric matrix
 * 
 * @author <a href="mailto:a.doerr@uni-tuebingen.de">Alexander D&ouml;rr</a>
 * @date 2010-12-23
 * @since 1.4
 * 
 */
public class StoichiometricMatrix extends StabilityMatrix {

	private static final long serialVersionUID = 1L;
	private StabilityMatrix linkMatrix = null;
	private StabilityMatrix reducedMatrix = null;
	private StabilityMatrix conservationRelations = null;
	private StabilityMatrix steadyStateFluxes = null;
	private HashMap<Integer, Integer> permutations = null;
	private HashSet<Integer> linearDependent;
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

	public StabilityMatrix getLinkMatrix() {
		if (linkMatrix == null) {
			reduceModel();
		}

		return linkMatrix;
	}

	public StabilityMatrix getReducedMatrix() {
		if (reducedMatrix == null) {
			reduceModel();
		}

		return reducedMatrix;
	}

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
	 * is calculated through LU decomposition
	 * 
	 * Vallabhajosyula
	 */
	private void reduceModel() {
		StabilityMatrix stoich;
		permutations = new HashMap<Integer, Integer>();

		if (!(this.getRowDimension() > 0 && this.getColumnDimension() > 0)) {
			System.out.println("Wrong dimensions");
		}
		if (this.getRowDimension() >= this.getColumnDimension()) {
			stoich = this;
		} else {
			stoich = augmentN();
		}

		LUDecomposition lu = new LUDecomposition(stoich);
		int m = stoich.getRowDimension();

		StabilityMatrix P = new StabilityMatrix(m, m, 0);

		int[] pivot = lu.getPivot();
		for (int i = 0; i < pivot.length; i++) {
			P.set(pivot[i], i, 1);
		}
		System.out.println("P");
		System.out.println(P);

		QRDecomposition qr = new QRDecomposition(stoich.transpose().times(P));

		// Matrix Q = qr.getQ();
		Matrix R = roundValues(qr.getR());

		// System.out.println("Q");
		// System.out.println(Q);
		//
		// System.out.println("R");
		// System.out.println(R);

		StabilityMatrix Rt = new StabilityMatrix(R.copy().getArray(), R
				.getRowDimension(), R.getColumnDimension());

		for (int i = 0; i < Rt.getRowDimension(); i++) {
			double unity = Rt.get(i, i);
			if (unity != 0.0) {
				for (int j = 0; j < Rt.getColumnDimension(); j++) {
					Rt
							.set(i, j, (Math.round(Rt.get(i, j) / unity
									* 10000.)) / 10000.);
				}
			}

		}
		rank = Rt.getRowDimension();

		for (int i = Rt.getRowDimension() - 1; i >= 0; i--) {
			if (Rt.get(i, i) == 0.0) {
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

		int l = 0, n, loindex;
		double value;
		double[] column;
		for (int i = 0; i < L.getRowDimension(); i++) {
			if (i < rank) {
				L.set(i, i, 1);
			} else {
				column = Rt.getColumn(i);
				for (int j = 0; j < L.getColumnDimension(); j++) {
					if (column[j] != 0.0) {
						value = Math.round(column[j] * 1000.) / 1000.;
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
		StabilityMatrix augmentedN = new StabilityMatrix(this
				.getColumnDimension(), this.getColumnDimension(), 0);

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
		for (int i = 0; i < this.getRowDimension(); i++) {
			for (int j = 0; j < this.getRowDimension(); j++) {

				if (permutationMatrix.get(i, j) == 1.0) {
					if (j >= rank) {
						linearDependent.add(i);
					}
					permutations.put(j, i);
					continue;

				}
			}
		}
		int l = 0;
		for (int i = 0; i < this.getRowDimension(); i++) {

			if (!linearDependent.contains(i)) {
				reducedMatrix.setRow(l, this.getRow(i));
				l++;
			}

		}

	}

	/**
	 * This method calculates the feasible steady state fluxes of the model with
	 * this stoichiometrix matrix, referring to Palsson also known as the null
	 * space of N. This is achieved by performing an SVD decomposition as
	 * described in "Conservation Analysis in Biochemical Networks" by Herbert
	 * Sauro
	 */
	private void calculateSteadyStateFluxes() {

	}

	/**
	 * This method changes values that are as good as zero to zero
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

	public StabilityMatrix getSteadyStateFluxes() {
		if (steadyStateFluxes == null) {
			calculateSteadyStateFluxes();
		}
		return steadyStateFluxes;
	}

	/**
	 * This method returns a HashSet of integer values corresponding to the rows
	 * of this matrix, that are removed when building the reduced stoichiometrix
	 * matrix. Please note that the indeces correspond to the array notation,
	 * where e.g. 0 stands for the first row in the matrix.
	 * 
	 * @return
	 */
	public HashSet<Integer> getLinearDependent() {
		if (linearDependent == null) {
			reduceModel();
		}
		return linearDependent;
	}

}
