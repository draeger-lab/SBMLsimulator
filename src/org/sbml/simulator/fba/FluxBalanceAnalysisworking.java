package org.sbml.simulator.fba;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.fba.util.FBAutil;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.simulator.math.ConservationRelations;
import org.sbml.simulator.math.StabilityMatrix;

import de.zbit.util.logging.LogUtil;
import eva2.tools.math.Jama.Matrix;

public class FluxBalanceAnalysisworking {

	public static <IloBoolVar> void main(String[] args)
			throws XMLStreamException, IOException {
		Logger logger = null;
		LogUtil.initializeLogging(Level.FINE, "org.sbml");
		SBMLDocument a = (new SBMLReader()).readSBML(args[0]);
		// LogUtil.initializeLogging(Level.FINE, "org.sbml");
		
		FluxBalanceAnalysis flux = new FluxBalanceAnalysis(a.getModel(),
				"files/Gibbs.txt", "files/conc.txt");
		flux.generate_stoichiometry();
		int num_reactions;
		
		num_reactions = FBAutil.Gibbs_to_file(a.getModel(), "gibbs.txt");
		
		String list_of_reactions[] = new String[num_reactions];
		
		list_of_reactions = FBAutil.getkegg(a.getModel(), list_of_reactions);
		
		FBAutil.Conc_to_file(a.getModel(), "conc.txt");

		
		//read extern information (gibbs energies, concentration values)
		
		Hashtable<String, Double> Gibbs_energies = (Hashtable<String, Double>) FBAutil
				.readGibbEnergyFile(flux.getModel(), new File("files/Gibbs.txt"));
		

		double c_eq[] = FBAutil.read_conc(a.getModel(), new File("files/conc.txt"));

		double energies[] = new double[num_reactions];
		System.out.println(energies.length);
		
		
		for (int i = 0; i < num_reactions; i++) {
			energies[i] = Gibbs_energies.get(list_of_reactions[i]);
		}
		
		
		
		Matrix stoich = flux.getStoich();
		//System.out.println(stoich);
		int rec = flux.getNum_rec();
		int spec = flux.getNum_spec();
		
		try {
			double lambda1 = 100;
			int lambda2 = 10;
			double lambda3 = 0.01;

			// generate Nullspace
			Matrix S = stoich;
			Matrix V = S.svd().getV();
			int r = S.rank();
			double RT = 2.14;

			int num_bases = V.getColumnDimension() - r;
			Matrix K = new Matrix(V.getColumnDimension(), num_bases);

			for (int i = 0; i < num_bases; i++) {
				for (int j = 0; j < V.getRowDimension(); j++) {
					K.set(j, i, V.get(j, i + r));
				}
			}
			K = FBAutil.toRREF(K.transpose()).transpose();
			StabilityMatrix s = new StabilityMatrix(stoich.transpose()
					.getArray(), stoich.getColumnDimension(),
					stoich.getRowDimension());
			//nonnegative Basis
			
			// TODO: Problem mit folgenden beiden Zeilen:
			// Matrix K_sp = ConservationRelations.calculateConsRelations(s);
		  // K = K_sp.transpose();			

			double gibbs[]  = energies;
			double g[] = gibbs;
			
			gibbs = new double[K.getRowDimension()];
			for (int i = 0; i < gibbs.length; i++) {
				gibbs[i] = g[i];
			}

			double c[] = c_eq;
			c_eq = new double[S.getRowDimension()];
			for (int i = 0; i < c_eq.length; i++) {
				c_eq[i] = c[i];
			}

			int sign[] = new int[gibbs.length];
			for (int i = 0; i < gibbs.length; i++) {
				sign[i] = (int) Math.signum(gibbs[i]);
				gibbs[i] = gibbs[i];
			}
			
			//Gibbsenergies
			
			double Gibb_eq[] = new double[gibbs.length];
			for (int i = 0; i < Gibb_eq.length; i++) {
				double sum = 0;
				for (int j = 0; j < S.getRowDimension(); j++) {
					sum += S.get(j, i) * c_eq[j];
				}
				Gibb_eq[i] = gibbs[i] + 2.14 * sum;
			}
			for (int i = 0; i < gibbs.length; i++) {
				sign[i] = (int) Math.signum(Gibb_eq[i]);
				Gibb_eq[i] = -Math.abs(Gibb_eq[i]);
			}

		
			//Optimization problem
			
			IloCplex cplex = new IloCplex();

			// Variable bounds
			double[] lb = new double[spec + K.getColumnDimension() + rec];
			double[] ub = new double[spec + K.getColumnDimension() + rec];

			for (int i = 0; i < spec; i++) {
				lb[i] = 0;
				ub[i] = Double.MAX_VALUE;
			}

			for (int i = spec; i < ub.length; i++) {
				lb[i] = -Double.MAX_VALUE;
				ub[i] = Double.MAX_VALUE;
			}

			IloNumVar[] x = cplex.numVarArray(ub.length, lb, ub);
			double var = (lambda1 - lambda1 * 2 * c_eq[0]);

			double[] objvals = new double[ub.length];
			// conc
			for (int i = 0; i < spec; i++) {
				objvals[i] = - lambda1 * 2 * c_eq[i];

			}

			// alpha
			for (int i = 0; i < K.getColumnDimension(); i++) {

				double sum = 0;
				for (int j = 0; j < K.getRowDimension(); j++) {
					sum += K.get(j, i);
				}

				objvals[spec + i] = sum;

			}

			// errors
			for (int i = spec + K.getColumnDimension(); i < objvals.length; i++) {
				objvals[i] = lambda3;
			}

			// quad conc

			IloNumExpr q_conc = cplex.prod(lambda1,cplex.prod(x[0], x[0]));
			for (int i = 1; i < spec; i++) {
				IloNumExpr temp = q_conc;
				q_conc = cplex.sum(cplex.prod(lambda1,cplex.prod(x[i], x[i])), temp);

			}

			cplex.addMinimize(cplex.sum(cplex.scalProd(x, objvals), q_conc));

			// Constraints

			// J_i * G_i < 0

			for (int i = 0; i < K.getRowDimension(); i++) {
				// dummy
				IloNumExpr jg = cplex.prod(0, x[spec]);

				for (int j = 0; j < K.getColumnDimension(); j++) {

					IloNumExpr temp = jg;
					jg = cplex.sum(cplex.prod(K.get(i, j), x[spec + j]), temp);
				}
				cplex.prod(jg, Gibb_eq[i]);
				// cplex.addLe(jg, -Double.MIN_NORMAL);
			}

			for (int i = 0; i < K.getRowDimension(); i++) {
				IloNumExpr jg = cplex.prod(x[spec], K.get(i, 0));

				for (int j = 1; j < K.getColumnDimension(); j++) {
					jg = cplex.sum(jg, cplex.prod(K.get(i, j), x[spec + j]));
				}

				cplex.addLe(cplex.prod(jg, Gibb_eq[i]), -Double.MIN_NORMAL);
			}

			// gibbsErrors

			for (int i = 0; i < rec; i++) {
				// dummy
				int t = spec + K.getColumnDimension();
				IloNumExpr rec_error = cplex.prod(0, x[t]);

				for (int j = 0; j < S.getRowDimension(); j++) {
					IloNumExpr temp = rec_error;
					rec_error = cplex.sum(cplex.prod(RT * S.get(j, i), x[j]),
							rec_error);
				}
				rec_error = cplex.sum(rec_error, cplex.prod(-1, x[t + i]));
				cplex.addEq(rec_error, Gibb_eq[i] - gibbs[i]);
			}
			// target fluxes

			// cplex.addEq(cplex.prod(x[spec],K.get(0,0)), 11);

			System.out.println(cplex.solve());

			

			if (cplex.solve()) {
				cplex.output()
						.println("Solution status = " + cplex.getStatus());
				cplex.output().println(
						"Solution value  = " + cplex.getObjValue());
				FileWriter w = new FileWriter(new File("correctfluxes.txt"));
				double[] val = cplex.getValues(x);
				int ncols = cplex.getNcols();
				for (int j = 0; j < ncols; ++j){
					cplex.output().println(
							"Column: " + j + " Value = " + val[j]);
					w.write("Column: " + j + " Value = " + val[j] + '\n');
				}
				// generate fluxes:
				
				
				System.out.println("no_conc" + spec);
				System.out.println("Flux i + value");
				for (int j = 0; j < rec; j++) {
				
					double sum = 0;
					for (int i = spec; i < spec + K.getColumnDimension(); i++) {
						sum += val[i] * K.get(j, i - spec);
					}
					// if (sign[j] > 0){
					// sum = -Math.abs(sum);
					//
					//
					// } else Math.abs(sum);
					//if(sum != 0)
						System.out.println(list_of_reactions[j]);
					double k = (sum * sign[j]);
				
					w.write(Double.toString(k) + '\n');
				}
				w.close();

			}
			cplex.end();
		} catch (IloException e) {
			System.err.println("Concert exception '" + e + "' caught");
		}
	}
}
