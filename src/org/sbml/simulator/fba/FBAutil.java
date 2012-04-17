package org.sbml.simulator.fba;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;

import eva2.tools.math.Jama.Matrix;

public class FBAutil {

	private final static Logger logger = Logger.getLogger(FBAutil.class
			.getName());

	/**
	 * return an array with needed Gibbsenergies
	 * 
	 * @param model
	 * @param string
	 * @return
	 * @throws IOException
	 */
	public static Map<String, Double> readGibbEnergyFile(Model model, File string)
			throws IOException {

		// double energies[] = new double[model.getNumReactions() + 2];// TODO
		Map<String, Double> energies = new Hashtable<String, Double>();

		FileReader r = new FileReader(string);
		BufferedReader read = new BufferedReader(r);
		String q;
		String k[] = new String[2];
		logger.log(Level.FINEST, Integer.toString(model.getNumReactions())
				+ "Reactions");
		while ((q = read.readLine()) != null) {
			k = q.split("\t");
			energies.put(k[0], Double.valueOf(k[1]));
		}

		return energies;

	}

	/**
	 * 
	 * @param model
	 * @param string
	 * @return
	 * @throws IOException
	 */
	public static double[] read_conc(Model model, File string)
			throws IOException {

		// double concentrations[] = new double[model.getNumSpecies()];
		double concentrations[] = new double[model.getNumSpecies()];

		FileReader r = new FileReader(string);
		BufferedReader read = new BufferedReader(r);
		String k[] = new String[2];
		String q;
		int i = 0;
		while ((q = read.readLine()) != null) {
			k = q.split("\t");

			concentrations[i] = Double.valueOf(k[1]);
			if (concentrations[i] != 0)
				concentrations[i] = Math.log(concentrations[i]);
			i++;
		}
		return concentrations;
	}

	public static String[] getkegg(Model model, String k[]) {

		String prefix = "urn:miriam:kegg.reaction:";
		int j = 0;
		for (int i = 0; i < model.getNumReactions(); i++) {
			Reaction r = model.getReaction(i);
			if (r.getCVTerms().isEmpty()){
				k[j] = "dummy";
				j++;
			}
			
			for (CVTerm term : r.getCVTerms()) {

				if (term.getBiologicalQualifierType() == CVTerm.Qualifier.BQB_IS) {
					for (String resource : term.getResources()) {

						if (resource.startsWith(prefix)) {
							k[j] = resource.substring(prefix.length());
							j++;
						}
					}
				}
			}
		}
		return k;
	}

	/**
	 * write Miriam Annotations of all Reactions to a file
	 * 
	 * @param model
	 * @param outputfile
	 * @throws IOException
	 */
	public static int Gibbs_to_file(Model model, String outputfile)
			throws IOException {
		int num_reactions = 0;
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
		String prefix = "urn:miriam:kegg.reaction:";
		for (int i = 0; i < model.getNumReactions(); i++) {
			Reaction r = model.getReaction(i);
		
			logger.log(Level.FINEST, r.toString());
		
			if (r.getCVTerms().isEmpty()){
				bw.append("dummy\t0.0");
				bw.newLine();
				num_reactions++;
			}
				
			for (CVTerm term : r.getCVTerms()) {
				
				logger.log(Level.FINEST, term.toString());
				if (term.getBiologicalQualifierType() == CVTerm.Qualifier.BQB_IS) {
					for (String resource : term.getResources()) {
						logger.log(Level.FINEST, "found resources in"
								+ Integer.toString(i));
						if (resource.startsWith(prefix)) {
							bw.append(resource.substring(prefix.length())
									+ "\t0.0");
							bw.newLine();
							num_reactions++;
						}
					}
				}
			}
		}
		bw.close();
		return num_reactions;

	}

	/**
	 * 
	 * @param model
	 * @param fileName
	 * @throws IOException
	 */

	public static void Conc_to_file(Model model, String fileName)
			throws IOException {
	  BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
		String prefix = "urn:miriam:kegg.compound:";
		for (Species spec : model.getListOfSpecies()) {
			System.out.println(spec);
			for (CVTerm term : spec.getCVTerms()) {
				///System.out.println(("cv"));
				if (term.getBiologicalQualifierType() == CVTerm.Qualifier.BQB_IS) {
					for (String res : term.getResources()) {
						if (res.startsWith(prefix)) {
							bw.append(res.substring(prefix.length()) + "\t0.0");
							bw.newLine();
						}
					}
				}
			}
		}
		bw.close();
	}

	/**
	 * 
	 * @param k
	 * @return
	 */
	public static Matrix roundMatrix(Matrix k) {
		for (int i = 0; i < k.getRowDimension(); i++) {
			for (int j = 0; j < k.getColumnDimension(); j++) {
				k.set(i, j, roundTwoDecimals(k.get(i, j)));
			}
		}
		return k;

	}

	/**
	 * round double to human readable format
	 * 
	 * @param d
	 * @return
	 */
	public static double roundTwoDecimals(double d) {
		DecimalFormat twoDForm = new DecimalFormat("#.###");
		return Double.valueOf(twoDForm.format(d));
	}

	/**
	 * rearrange the matrix to fit a steady state null space
	 * 
	 * @param matrix
	 * @param p
	 * @return
	 */

	public static Matrix rearrange(Matrix matrix, int p[]) {

		double H[][] = matrix.getArray();
		double out[][] = new double[matrix.getRowDimension()][matrix
				.getColumnDimension()];

		for (int j = 0; j < matrix.getRowDimension(); j++) {

			out[j] = H[p[j]];
		}

		return new Matrix(out).transpose();

	}

	/**
	 * compute the row reduced echolon form of a matrix
	 * 
	 * @param M
	 */
	/*
	 * double[][] mtx = { { 1, 2, -1, -4}, { 2, 3, -1,-11}, {-2, 0, -3, 22}}; K
	 * = new Matrix(mtx);
	 */
	public static Matrix toRREF(Matrix matrix) {
		int rowCount = matrix.getRowDimension();
		if (rowCount == 0) {
			return matrix;
		}
		int columnCount = matrix.getColumnDimension();
		double M[][] = matrix.getArray();

		int lead = 0;
		for (int r = 0; r < rowCount; r++) {
			if (lead >= columnCount) {
				break;
			} else {
				int i = r;
				while (M[i][lead] == 0) {
					i++;
					if (i == rowCount) {
						i = r;
						lead++;
						if (lead == columnCount)
							return new Matrix(M);
					}
				}
				double[] temp = M[r];

				M[r] = M[i];
				M[i] = temp;
			}

			{
				double lv = M[r][lead];
				for (int j = 0; j < columnCount; j++) {
					M[r][j] /= lv;
				}
			}

			for (int i = 0; i < rowCount; i++) {
				if (i != r) {
					double lv = M[i][lead];
					for (int j = 0; j < columnCount; j++) {
						M[i][j] -= lv * M[r][j];
					}
				}
			}
			lead++;
		}
		return matrix;
	}

	/**
	 * Exchange row s and t in matrix
	 * 
	 * @param matrix
	 * @param s
	 * @param t
	 * @return matrix
	 */

	public static Matrix rowexchange(Matrix matrix, int s, int t) {
		int num_rows = matrix.getRowDimension();
		int num_cols = matrix.getColumnDimension();
		double hold;

		for (int i = 0; i < num_cols; i++) {
			hold = matrix.get(s, i);
			matrix.set(s, i, matrix.get(t, i));
			matrix.set(t, i, hold);
		}
		return matrix;

	}

	/**
	 * Exchange column s and t in matrix
	 * 
	 * @param matrix
	 * @param s
	 * @param t
	 * @return
	 */

	public static Matrix colexchange(Matrix matrix, int s, int t) {

		int num_rows = matrix.getRowDimension();
		int num_cols = matrix.getColumnDimension();
		double hold;

		for (int i = 0; i < num_rows; i++) {
			hold = matrix.get(i, s);
			matrix.set(i, s, matrix.get(i, t));
			matrix.set(i, t, hold);
		}
		return matrix;

	}

}