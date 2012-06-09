package org.sbml.simulator.fba;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.util.StringTools;
import org.sbml.simulator.stability.math.ConservationRelations;
import org.sbml.simulator.stability.math.StabilityMatrix;

import de.zbit.util.logging.LogUtil;
import eva2.tools.math.Jama.Matrix;

public class FBAtestcplex {

  /**
   * 
   */
  private static final transient Logger logger = Logger.getLogger(FBAtestcplex.class.getName());
  
  /**
   * 
   * @param <IloBoolVar>
   * @param args
   * @throws XMLStreamException
   * @throws IOException
   */
	public static <IloBoolVar> void main(String[] args)
			throws XMLStreamException, IOException {
		LogUtil.initializeLogging(Level.FINE, "org.sbml");
		// LogUtil.initializeLogging(Level.FINE, "org.sbml");
    FluxBalanceAnalysis flux = new FluxBalanceAnalysis(new File(args[0]),
      new File(args[1]), new File(args[2]));
    Model model = flux.getModel();
		flux.generate_stoichiometry();
		int num_reactions = FBAutil.Gibbs_to_file(model, "gibbs.txt");
		String list_of_reactions[] = new String[num_reactions];
		list_of_reactions = FBAutil.getkegg(model, list_of_reactions);
		FBAutil.Conc_to_file(model, "conc.txt");

		double energies[] = new double[num_reactions];
		System.out.printf("Number of Gibbs energy values: %d\n", energies.length);
		
		Map<String, Double> Gibbs_energies = flux.getGibbsEnergies();
		Double energyValue;
		for (int i = 0; i < num_reactions; i++) {
		  energyValue = Gibbs_energies.get(list_of_reactions[i]);
			energies[i] = energyValue != null ? energyValue.doubleValue() : 0d;
		}
		
		
		
		Matrix stoich = flux.getStoich();
		//System.out.println(stoich);
		int rec = flux.getNum_rec();
		int spec = flux.getNum_spec();
		System.out.printf("Number of reactions: %d\n", rec);
    //		System.out.println(2);
		try {
			double lambda1 = 100d;
			int lambda2 = 10;
			double lambda3 = 0.01d;

			// generate Nullspace
			Matrix S = stoich;
			Matrix V = S.svd().getV();
			// System.out.println(S);
			// System.out.println(V);
			int r = S.rank();
			double RT = 2.14;

			int num_bases = V.getColumnDimension() - r;
			Matrix K = new Matrix(V.getColumnDimension(), num_bases);

			for (int i = 0; i < num_bases; i++) {
				for (int j = 0; j < V.getRowDimension(); j++) {
					K.set(j, i, V.get(j, i + r));
				}
			}
			// System.out.println(FBAutil.toRREF(new Matrix(t)));
			//System.out.println(K);
			System.out.printf("Number of reactions: %d\n", rec);
			System.out.printf("Number of species: %d\n", spec);
			K = FBAutil.toRREF(K.transpose()).transpose();
			StabilityMatrix s = new StabilityMatrix(stoich.transpose()
					.getArray(), stoich.getColumnDimension(),
					stoich.getRowDimension());
//			
//	     StoichiometricMatrix n = new StoichiometricMatrix(stoich.getArray(),stoich.getRowDimension(),stoich.getColumnDimension());
//	     System.out.println("steady state fluxes");
//
//	    System.out.println(stoich);
//			System.out.println(n.getConservationRelations());
			//System.out.println(s);
			// TODO: Problem!!!
			Matrix K_sp = ConservationRelations.calculateConsRelations(s);
			
			
			// K = FBAutil.toRREF(K_sp).transpose();
			//System.out.println(K.getColumnDimension());
			K = K_sp.transpose();
		
			System.out.println("Kernel matrix:");
			System.out.println(K);
			// System.out.println(K);
			//System.out.println(K);
//			double c_eq[] = { Math.log(12), Math.log(23), Math.log(4),
//					Math.log(5), Math.log(13), Math.log(44), Math.log(12),
//					Math.log(23), Math.log(4), Math.log(5), Math.log(13),
//					Math.log(44), Math.log(12), Math.log(23), Math.log(4),
//					Math.log(5), Math.log(13), Math.log(44), Math.log(12),
//					Math.log(23), Math.log(4), Math.log(5), Math.log(13),
//					Math.log(44), Math.log(12), Math.log(23), Math.log(4),
//					Math.log(5), Math.log(13), Math.log(44), Math.log(12),
//					Math.log(23), Math.log(4), Math.log(5), Math.log(13),
//					Math.log(44), Math.log(12), Math.log(23), Math.log(4),
//					Math.log(5), Math.log(13), Math.log(44), Math.log(12),
//					Math.log(23), Math.log(4), Math.log(5), Math.log(13),
//					Math.log(44), Math.log(12), Math.log(23), Math.log(4),
//					Math.log(5), Math.log(13), Math.log(44), Math.log(12),
//					Math.log(23), Math.log(4), Math.log(5), Math.log(13),
//					Math.log(44), Math.log(12), Math.log(23), Math.log(4),
//					Math.log(5), Math.log(13) };
//			double gibbs[] = { 2, 24.34, -24.24, 3.455, -45.233, 243.44, 23,
//					-3.43, -32.34, 12.32, 7.456, 22, 24.34, -24.24, 3.455,
//					-405.233, 243.44, 23, -3.43, -32.34, 12.32, 7.456, 22,
//					24.34, -24.24, 3.455, -405.233, 243.44, 23, -3.43, -32.34,
//					12.32, 7.456, 22, 24.34, -24.24, 3.455, -405.233, 243.44,
//					23, -3.43, -32.34, 12.32, 7.456, 22, 24.34, -24.24, 3.455,
//					-405.233, 243.44, 23, -3.43, -32.34, 12.32, 7.456, 22,
//					24.34, -24.24, 3.455, -405.233, 243.44, 23, -3.43, -32.34,
//					12.32, 7.456, 22, 24.34, -24.24, 3.455, -405.233, 243.44,
//					23, -3.43, -32.34, 12.32, 7.456 };
//			

			double g[] = energies;
			System.out.printf("Number of reactions: %d\n", num_reactions);
			System.out.printf("Number of Gibbs energy values: %d\n", energies.length);
			System.out.printf("Row dimension of kernel matrix: %d\n", K.getRowDimension());
			double gibbs[] = new double[K.getRowDimension()];
			// Hier gab es eine Index out of bounds exception
			for (int i = 0; i < Math.min(gibbs.length, g.length); i++) {
				gibbs[i] = g[i];
			}

			double c_eq[] = flux.getEquilibriumConcentrations();
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

			// System.out.println(Arrays.toString(Gibb_eq));

			// QuadraticProgram lin = new QuadraticProgram((new Matrix(16, 16,
			// 0)
			// .getArray()), new double[] {
			// 1 - 2 * c_eq[0],
			// 1 - 2 * c_eq[1],
			// 1 - 2 * c_eq[2],
			// 1 - 2 * c_eq[3],
			// 1,
			// 1,
			// 1,
			// 1,
			// K.get(0, 0) + K.get(1, 0) + K.get(2, 0) + K.get(3, 0)
			// + K.get(4, 0) + K.get(5, 0),
			// K.get(0, 1) + K.get(1, 1) + K.get(2, 1) + K.get(3, 1)
			// + K.get(4, 1) + K.get(5, 1), 2, 2, 2, 2, 2, 2 });
			//

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

			System.out.printf("Result by CPLEX: %b\n", cplex.solve());

			// lin.addConstraint(new LinearEqualsConstraint(new SparseVector(
			// new double[] { RT * S.get(0, 0), RT * S.get(1, 0),
			// RT * S.get(2, 0), RT * S.get(3, 0), 0, 0, 0, 0, 0, 0,
			// -1, 0, 0, 0, 0, 0 }), 0, "t"));
			//
			// lin.addConstraint(new LinearEqualsConstraint(new SparseVector(
			// new double[] { RT * S.get(0, 1), RT * S.get(1, 1),
			// RT * S.get(2, 1), RT * S.get(3, 1), 0,0, 0, 0, 0, 0, 0,
			// 0, -1, 0, 0, 0, 0 }), 0, "a"));
			// lin.addConstraint(new LinearEqualsConstraint(new SparseVector(
			// new double[] { RT * S.get(0, 2), RT * S.get(1, 2),
			// RT * S.get(2, 2), RT * S.get(3, 2), 0, 0, 0, 0, 0, 0,
			// 0, 0, -1, 0, 0, 0 }), 0, "b"));
			// lin.addConstraint(new LinearEqualsConstraint(new SparseVector(
			// new double[] { RT * S.get(0, 3), RT * S.get(1, 3),
			// RT * S.get(2, 3), RT * S.get(3, 3), 0, 0, 0, 0, 0, 0,
			// 0, 0, 0, -1, 0, 0 }), 0, "d"));
			// lin.addConstraint(new LinearEqualsConstraint(new SparseVector(
			// new double[] { RT * S.get(0, 4), RT * S.get(1, 4),
			// RT * S.get(2, 4), RT * S.get(3, 4), 0, 0, 0, 0, 0, 0,
			// 0, 0, 0, 0, -1, 0 }), 0, "e"));
			// lin.addConstraint(new LinearEqualsConstraint(new SparseVector(
			// new double[] { RT * S.get(0, 5), RT * S.get(1, 5),
			// RT * S.get(2, 5), RT * S.get(3, 5), 0, 0, 0, 0, 0, 0,
			// 0, 0, 0, 0, 0, -1 }), 0, "f"));
			//

			// lin.setMinProblem(true);
			// QuadraticProgramSolver uu = (QuadraticProgramSolver)
			// SolverFactory.newDefault();
			// double sol[] = uu.solve(lin);
			// System.out.println(sol);

			if (cplex.solve()) {
				cplex.output()
						.println("Solution status = " + cplex.getStatus());
				cplex.output().println(
						"Solution value  = " + cplex.getObjValue());
				BufferedWriter w = new BufferedWriter(new FileWriter(new File("correctfluxes.txt")));
				double[] val = cplex.getValues(x);
				int ncols = cplex.getNcols();
				for (int j = 0; j < ncols; ++j){
					cplex.output().println(
							"Column: " + j + " Value = " + val[j]);
					w.write("Column: " + j + " Value = " + val[j] + '\n');
				}
				// generate fluxes:
				
				
				System.out.printf("no_conc %d\n", spec);
				System.out.printf("Flux i + value\n");
				for (int j = 0; j < list_of_reactions.length; j++) {
				
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
					double k = (sum * sign[j]);
          System.out.printf("reaction %s:\t%s\n", list_of_reactions[j], StringTools.toString(k));
					w.write(Double.toString(k) + '\n');
				}
				w.close();

			}
			cplex.end();
		} catch (IloException e) {
			logger.fine("Concert exception '" + e + "' caught");
		}
	}

}
