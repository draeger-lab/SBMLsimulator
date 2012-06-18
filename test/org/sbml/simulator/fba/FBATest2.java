package org.sbml.simulator.fba;

import java.io.IOException;
import java.text.ChoiceFormat;
import java.util.logging.Level;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;

import scpsolver.constraints.LinearBiggerThanEqualsConstraint;
import scpsolver.constraints.LinearEqualsConstraint;
import scpsolver.constraints.QuadraticSmallerThanEqualsContraint;
import scpsolver.lpsolver.SolverFactory;
import scpsolver.qpsolver.QuadraticProgram;
import scpsolver.qpsolver.QuadraticProgramSolver;
import scpsolver.util.SparseVector;

import de.zbit.util.logging.LogUtil;
import eva2.tools.math.Jama.Matrix;


public class FBATest2 {

  public static void main(String[] args) throws XMLStreamException,
      IOException {
    SBMLDocument a = (new SBMLReader()).readSBML(args[0]);
    LogUtil.initializeLogging(Level.FINE, "org.sbml");
    FluxBalanceAnalysis flux = new FluxBalanceAnalysis(a.getModel(),
        "Gibbs_energies_hepato.txt", "Conc.txt");
    flux.generate_stoichiometry();
//    Matrix stoich = flux.getStoich();
    int rec = flux.getNum_rec();
    int spec = flux.getNum_spec();

    Matrix q = new Matrix(2 * rec + spec, 2 * rec + spec, 0);
    double k[] = new double[2 * rec + spec];
    for (int i = 0; i < spec; i++) {
      q.set(i, i, 1);
      k[i] = -2;
    }
    double rand[] = new double[2 * rec];
    for (int i = 0; i < 2 * rec; i++) {
      rand[i] = 0;
    }
    Matrix S = new Matrix(4, 6);
    S.set(0, 0, 1);
    S.set(1, 0, 0);
    S.set(2, 0, -1);
    S.set(3, 0, 1);

    S.set(0, 1, 0);
    S.set(1, 1, -1);
    S.set(2, 1, 1);
    S.set(3, 1, 1);

    S.set(0, 2, -1);
    S.set(1, 2, 1);
    S.set(2, 2, 1);
    S.set(3, 2, 0);

    S.set(0, 3, 2);
    S.set(1, 3, 2);
    S.set(2, 3, 0);
    S.set(3, 3, 0);

    S.set(0, 4, 0);
    S.set(1, 4, 1);
    S.set(2, 4, 0);
    S.set(3, 4, 0);

    S.set(0, 5, 4);
    S.set(1, 5, -1);
    S.set(2, 5, 1);
    S.set(3, 5, -3);

    Matrix V = S.svd().getV();
    System.out.println(S);
    System.out.println(V);
//    int r = S.rank();
    double RT = 2.14;
    Matrix K = new Matrix(6, 2);
//    double c[] = V.getColumn(4);
//    double d[] = V.getColumn(5);
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 6; j++) {
        K.set(j, i, V.get(j, i + 4));
      }
    }
    double[][] t = { { -3, 2, 3, 2 }, { 0, 0, 0, -1 }, { -1, 0, 1, 3 } };
    System.out.println(FBAutil.toRREF(new Matrix(t)));
    K = FBAutil.toRREF(K.transpose()).transpose();
    double c_eq[] = { 2000, 1786, 2341, 4332 };
    double gibbs[] = { 22, -32.34, 24.24, 3.455, 45.233, -23.44 };

    double Gibb_eq[] = new double[6];
    for (int i = 0; i < Gibb_eq.length; i++) {
      double sum = 0;
      for (int j = 0; j < S.getRowDimension(); j++) {
        sum += S.get(j, i) * c_eq[j];
      }
      Gibb_eq[i] = gibbs[i] + 2.14 * sum;
    }

    QuadraticProgram lin = new QuadraticProgram((new Matrix(16, 16, 0)
        .getArray()), new double[] {
        1 - 2 * c_eq[0],
        1 - 2 * c_eq[1],
        1 - 2 * c_eq[2],
        1 - 2 * c_eq[3],
        1,
        1,
        1,
        1,
        K.get(0, 0) + K.get(1, 0) + K.get(2, 0) + K.get(3, 0)
            + K.get(4, 0) + K.get(5, 0),
        K.get(0, 1) + K.get(1, 1) + K.get(2, 1) + K.get(3, 1)
            + K.get(4, 1) + K.get(5, 1), 2, 2, 2, 2, 2, 2 });

    Matrix cons1 = new Matrix(16, 16, 0);
    cons1.set(8, 0, RT * sumup(K.getColumn(0), S.transpose().getColumn(0)));
    cons1.set(8, 1, RT * sumup(K.getColumn(0), S.transpose().getColumn(1)));
    cons1.set(8, 2, RT * sumup(K.getColumn(0), S.transpose().getColumn(2)));
    cons1.set(8, 3, RT * sumup(K.getColumn(0), S.transpose().getColumn(3)));
    cons1.set(9, 0, RT * sumup(K.getColumn(1), S.transpose().getColumn(0)));
    cons1.set(9, 1, RT * sumup(K.getColumn(1), S.transpose().getColumn(1)));
    cons1.set(9, 2, RT * sumup(K.getColumn(1), S.transpose().getColumn(2)));
    cons1.set(9, 3, RT * sumup(K.getColumn(1), S.transpose().getColumn(3)));

    cons1.set(0, 8, RT * sumup(K.getColumn(0), S.transpose().getColumn(0)));
    cons1.set(1, 8, RT * sumup(K.getColumn(0), S.transpose().getColumn(1)));
    cons1.set(2, 8, RT * sumup(K.getColumn(0), S.transpose().getColumn(2)));
    cons1.set(3, 8, RT * sumup(K.getColumn(0), S.transpose().getColumn(3)));
    cons1.set(0, 9, RT * sumup(K.getColumn(1), S.transpose().getColumn(0)));
    cons1.set(1, 9, RT * sumup(K.getColumn(1), S.transpose().getColumn(1)));
    cons1.set(2, 9, RT * sumup(K.getColumn(1), S.transpose().getColumn(2)));
    cons1.set(3, 9, RT * sumup(K.getColumn(1), S.transpose().getColumn(3)));

    cons1.set(10, 8, -K.get(0, 0));
    cons1.set(11, 8, -K.get(1, 0));
    cons1.set(12, 8, -K.get(2, 0));
    cons1.set(13, 8, -K.get(3, 0));
    cons1.set(14, 8, -K.get(4, 0));
    cons1.set(15, 8, -K.get(5, 0));
    cons1.set(10, 9, -K.get(0, 1));
    cons1.set(11, 9, -K.get(1, 1));
    cons1.set(12, 9, -K.get(2, 1));
    cons1.set(13, 9, -K.get(3, 1));
    cons1.set(14, 9, -K.get(4, 1));
    cons1.set(15, 9, -K.get(5, 1));

    cons1.set(8, 10, -K.get(0, 0));
    cons1.set(8, 11, -K.get(1, 0));
    cons1.set(8, 12, -K.get(2, 0));
    cons1.set(8, 13, -K.get(3, 0));
    cons1.set(8, 14, -K.get(4, 0));
    cons1.set(8, 15, -K.get(5, 0));
    cons1.set(9, 10, -K.get(0, 1));
    cons1.set(9, 11, -K.get(1, 1));
    cons1.set(9, 12, -K.get(2, 1));
    cons1.set(9, 13, -K.get(3, 1));
    cons1.set(9, 14, -K.get(4, 1));
    cons1.set(9, 15, -K.get(5, 1));

    double c1[] = new double[] { 0, 0, 0, 0, 0, 0, 0, 0,
        sumup(K.getColumn(0), gibbs), sumup(K.getColumn(1), gibbs), 0,
        0, 0, 0, 0, 0 };

    lin.addConstraint(new QuadraticSmallerThanEqualsContraint(cons1
        .getArray(), c1, ChoiceFormat.previousDouble(0.0), "na"));
    lin.addConstraint(new LinearEqualsConstraint(new SparseVector(
        new double[] { RT * S.get(0, 0), RT * S.get(1, 0),
            RT * S.get(2, 0), RT * S.get(3, 0), 0, 0, 0, 0, 0, 0,
            -1, 0, 0, 0, 0, 0 }), 0, "t"));

    lin.addConstraint(new LinearEqualsConstraint(new SparseVector(
        new double[] { RT * S.get(0, 1), RT * S.get(1, 1),
            RT * S.get(2, 1), RT * S.get(3, 1), 0, 0, 0, 0, 0, 0,
            0, -1, 0, 0, 0, 0 }), 0, "a"));
    lin.addConstraint(new LinearEqualsConstraint(new SparseVector(
        new double[] { RT * S.get(0, 2), RT * S.get(1, 2),
            RT * S.get(2, 2), RT * S.get(3, 2), 0, 0, 0, 0, 0, 0,
            0, 0, -1, 0, 0, 0 }), 0, "b"));
    lin.addConstraint(new LinearEqualsConstraint(new SparseVector(
        new double[] { RT * S.get(0, 3), RT * S.get(1, 3),
            RT * S.get(2, 3), RT * S.get(3, 3), 0, 0, 0, 0, 0, 0,
            0, 0, 0, -1, 0, 0 }), 0, "d"));
    lin.addConstraint(new LinearEqualsConstraint(new SparseVector(
        new double[] { RT * S.get(0, 4), RT * S.get(1, 4),
            RT * S.get(2, 4), RT * S.get(3, 4), 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, -1, 0 }), 0, "e"));
    lin.addConstraint(new LinearEqualsConstraint(new SparseVector(
        new double[] { RT * S.get(0, 5), RT * S.get(1, 5),
            RT * S.get(2, 5), RT * S.get(3, 5), 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, -1 }), 0, "f"));

    lin.addConstraint(new LinearBiggerThanEqualsConstraint(new double[] {
        1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ChoiceFormat
        .nextDouble(0.0), "r"));

    lin.addConstraint(new LinearBiggerThanEqualsConstraint(new double[] {
        0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ChoiceFormat
        .nextDouble(0.0), "t"));

    lin.addConstraint(new LinearBiggerThanEqualsConstraint(new double[] {
        0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ChoiceFormat
        .nextDouble(0.0), "s"));

    lin.addConstraint(new LinearBiggerThanEqualsConstraint(new double[] {
        0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ChoiceFormat
        .nextDouble(0.0), "rt"));

    lin.addConstraint(new LinearBiggerThanEqualsConstraint(new double[] {
        0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ChoiceFormat
        .nextDouble(0.0), "rs"));

    lin.addConstraint(new LinearBiggerThanEqualsConstraint(new double[] {
        0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ChoiceFormat
        .nextDouble(0.0), "ssd"));

    lin.addConstraint(new LinearBiggerThanEqualsConstraint(new double[] {
        0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ChoiceFormat
        .nextDouble(0.0), "ssr"));

    lin.addConstraint(new LinearBiggerThanEqualsConstraint(new double[] {
        0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 }, ChoiceFormat
        .nextDouble(0.0), "rssd"));

    lin.addConstraint(new LinearEqualsConstraint(new double[] { 1, 0, 0, 0,
        1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 0, "haral"));

    lin.addConstraint(new LinearEqualsConstraint(new double[] { 0, 1, 0, 0,
        0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 0, "harald"));

    lin.addConstraint(new LinearEqualsConstraint(new double[] { 0, 0, 1, 0,
        0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 0, "harad"));

    lin.addConstraint(new LinearEqualsConstraint(new double[] { 0, 0, 0, 1,
        0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 }, 0, "harld"));
    
    lin.setMinProblem(true);
    QuadraticProgramSolver uu = (QuadraticProgramSolver) SolverFactory.newDefault();
//    double sol[] = 
    	uu.solve(lin);
    
    /*
     * try { //create a buffered reader that connects to the console, we use
     * it so we can read lines BufferedReader in = new BufferedReader(new
     * InputStreamReader(System.in));
     * 
     * //read a line from the console String lineFromInput; while(
     * (lineFromInput = in.readLine())!= null){ out.println(lineFromInput);
     * }
     * 
     * //create an print writer for writing to a file
     * 
     * 
     * 
     * 
     * //close the file (VERY IMPORTANT!)
     * 
     * } catch(IOException e) {
     * System.out.println("Error during reading/writing"); }
     * 
     * 
     * 
     * 
     * } out.close();
     */

  }

  public static int sum(double[] a) {
    int sum = 0;
    for (int i = 0; i < a.length; i++) {
      sum += a[i];
    }
    return sum;
  }

  public static double sumup(double[] c, double e[]) {
    double sum = 0;
    for (int j = 0; j < c.length; j++) {
      sum += c[j] * e[j];
    }
    return sum;
  }
}

/*
 * package org.sbml.fba;
 * 
 * import java.io.BufferedReader; import java.io.FileReader; import
 * java.io.FileWriter; import java.io.IOException; import
 * java.text.ChoiceFormat; import java.text.DecimalFormat; import
 * java.util.Arrays; import java.util.Random;
 * 
 * import javax.xml.stream.XMLStreamException;
 * 
 * import org.sbml.jsbml.CVTerm; import org.sbml.jsbml.Model; import
 * org.sbml.jsbml.Reaction; import org.sbml.jsbml.SBMLDocument; import
 * org.sbml.jsbml.SBMLReader; import org.sbml.jsbml.SpeciesReference; import
 * org.sbml.simulator.math.StabilityMatrix; import
 * org.sbml.simulator.math.StoichiometricMatrix;
 * 
 * import scpsolver.constraints.LinearSmallerThanEqualsConstraint; import
 * scpsolver.constraints.QuadraticSmallerThanEqualsContraint; import
 * scpsolver.lpsolver.LinearProgramSolver; import
 * scpsolver.lpsolver.SolverFactory; import scpsolver.qpsolver.QuadraticProgram;
 * import scpsolver.qpsolver.QuadraticProgramSolver; import
 * scpsolver.util.SparseVector;
 * 
 * import eva2.tools.math.Jama.Matrix;
 * 
 * public class FBAtest {
 *//**
 * args[0]: SBML model args[1]: number of given target fluxes args[2 -
 * numberoftargetfluxes+2]: array with indices of target fluxes
 * args[numberoftargetfluxes+3 - 2*numberoftargetfluxes+2]: array with value of
 * targetfluxes in corresponding order to the indices
 * 
 * 
 * @param args
 * @throws IOException
 * @throws XMLStreamException
 */
/*
 * public static void main(String[] args) throws IOException, XMLStreamException
 * { SBMLDocument a = (new SBMLReader()).readSBML(args[0]);
 * 
 * // get the Stoichiometry from a given model
 * 
 * double stoech[][] = new double[a.getModel().getNumSpecies()][a
 * .getModel().getNumReactions()];
 * 
 * Model model = a.getModel(); Reaction reaction; SpeciesReference specRef; for
 * (int i = 0; i < model.getNumReactions(); i++) { reaction =
 * model.getReaction(i);
 * 
 * for (int j = 0; j < reaction.getNumReactants(); j++) { specRef =
 * reaction.getReactant(j); stoech[model.getListOfSpecies().indexOf(
 * specRef.getSpeciesInstance())][i] = -specRef .getStoichiometry(); } for (int
 * j = 0; j < reaction.getNumProducts(); j++) { specRef =
 * reaction.getProduct(j); stoech[model.getListOfSpecies().indexOf(
 * specRef.getSpeciesInstance())][i] = reaction
 * .getProduct(j).getStoichiometry(); }
 * 
 * }
 * 
 * Matrix stoich = new Matrix(stoech);
 * 
 * // reduce Matrix
 * 
 * // StabilityMatrix red_matrix = //
 * ConservationRelations.calculateConsRelations(new //
 * StabilityMatrix(stoich.getArrayCopy
 * (),stoich.getRowDimension(),stoich.getColumnDimension()));
 * 
 * StoichiometricMatrix red_matrix = new StoichiometricMatrix(stoich
 * .getArray(), stoich.getRowDimension(), stoich .getColumnDimension());
 * StabilityMatrix redu_matrix = red_matrix.getReducedMatrix();
 * 
 * stoich = (Matrix) redu_matrix;
 * 
 * // generate the null space Matrix
 * 
 * int rang = stoich.rank(); int dim = stoich.getColumnDimension(); int
 * num_reactions = dim; int num_stoffe = stoich.getRowDimension(); int r = dim -
 * rang;
 * 
 * // SingularValueDecompostion and Nullspace Matrix G = stoich.copy(); G =
 * G.svd().getV().transpose();
 * 
 * int k[] = new int[r];
 * 
 * for (int i = 0; i < r; i++) { k[i] = G.getRowDimension() - r + i;
 * System.out.println(k[i]); } int c[] = new int[G.getColumnDimension()]; for
 * (int i = 0; i < G.getColumnDimension(); i++) { c[i] = i; }
 * 
 * G = G.getMatrix(k, c);
 * 
 * // block diagonalization of matrix int num_cols = G.getColumnDimension(); int
 * num_rows = G.getRowDimension(); Matrix K = new Matrix(num_rows, num_cols);
 * 
 * K = G;
 * 
 * int p[] = new int[K.getColumnDimension()]; for (int i = 0; i <
 * K.getColumnDimension(); i++) { p[i] = i; }
 * 
 * if (args.length > 1) { int num_target_fluxes = Integer.valueOf(args[1]); //
 * int tar_flux[] = new int[num_target_fluxes];
 * 
 * for (int i = 0; i < num_target_fluxes; i++) { int temp = p[i]; p[i] =
 * Integer.valueOf(args[i + 2]); p[Integer.valueOf(args[i + 2])] = temp; }
 * 
 * }
 * 
 * Matrix K_trans = K.transpose(); K_trans = toRREF(K_trans);
 * 
 * // System.out.println(K);
 * 
 * Matrix Alpha = new Matrix(K.getRowDimension(), K.getColumnDimension());
 * double target_fluxes[][] = new double[r][1]; if (args.length > 1) {
 * 
 * int num_target_fluxes = Integer.valueOf(args[1]);
 * 
 * for (int i = 0; i < num_target_fluxes; i++) { target_fluxes[i][0] =
 * Double.valueOf(args[num_target_fluxes + 3 + i]); }
 * 
 * 
 * in case that not enough target fluxes are given?!
 * 
 * while (num_target_fluxes< r){ Random rand = new Random();
 * target_fluxes[num_target_fluxes][0]= rand.nextInt(5); num_target_fluxes++;
 * 
 * }
 * 
 * double alpha[] = new double[K.getColumnDimension()]; Alpha = K.solve(new
 * Matrix(target_fluxes));
 * 
 * }
 * 
 * // in case no targetfluxes are given, it will work with an example array else
 * { Random rand = new Random();
 * 
 * double alpha[] = new double[K_trans.getRowDimension()]; for (int i = 0; i <
 * r; i++) {
 * 
 * target_fluxes[i][0] = rand.nextInt(5); } int ki[] = new int[r]; for (int i =
 * 0; i < r; i++) { ki[i] = i; } int ci[] = new
 * int[K_trans.getColumnDimension()]; for (int i = 0; i <
 * K_trans.getColumnDimension(); i++) { ci[i] = i; }
 * 
 * Alpha = K_trans.getMatrix(ki, ci).solve(new Matrix(target_fluxes));
 * 
 * }
 * 
 * K = rearrange(K_trans, p);
 * 
 * Gibbs_to_file(model, "test.txt"); //
 * model.getSpecies(1).getCVTerm(1).getBiologicalQualifierType(); //
 * CVTerm.Qualifier.BQB_IS
 * 
 * double Gibbs_energies[] = read_file(model, "Gibbs.txt", true);
 * System.out.println(model.getNumSpecies());
 * 
 * double c_eq[] = read_file(model, "Conc.txt", false);
 * 
 * // TODO log of concentrations
 * 
 * // generate Matrix and Arrays for scpsolver(quadratic programming with //
 * quadratic constraints) double lambda_1 = 10.0; double lambda_2 = 10.0; double
 * lambda_3 = 0.01; double lambda_4 = 1.0;
 * 
 * Matrix Q = new Matrix(2 * num_reactions + num_stoffe, 2 * num_reactions +
 * num_stoffe, 0); for (int i = 0; i < num_stoffe; i++) { Q.set(i, i, lambda_1);
 * }
 * 
 * // concentrations // Matrix M = new //
 * Matrix(2*num_reactions+num_stoffe,K.getRowDimension
 * ()+2+stoich.getColumnDimension(),0); double M[] = new double[2 *
 * num_reactions + num_stoffe];
 * 
 * for (int i = 0; i < num_stoffe; i++) { M[i] = lambda_1 * -2 * c_eq[i];
 * 
 * }
 * 
 * // fluxes for (int i = num_stoffe; i < num_stoffe + num_reactions; i++) {
 * M[i] = 0.0; for (int j = 0; j < K.getRowDimension(); j++) { M[i] += K.get(j,
 * i - num_stoffe); }
 * 
 * // for (int j = 1; j < K.getRowDimension()+1;j++){
 * 
 * // M.set(i,j,K.get(j-1,i-num_stoffe));
 * 
 * }
 * 
 * // concentration errors
 * 
 * for (int i = num_stoffe + num_reactions; i < num_stoffe + 2 num_reactions;
 * i++) {
 * 
 * M[i] = lambda_3 - lambda_2; }
 * 
 * 
 * M.set(i,K.getRowDimension()+1,lambda_3); }
 * 
 * // concentration errors for (int i = 0; i < stoich.getRowDimension();i++) {
 * for (int j = 0; j < stoich.getColumnDimension();j++){
 * 
 * M.set(i+num_reactions+num_stoffe+1, K.getRowDimension()+2+j, -stoich.get(i,
 * j)); } }
 * 
 * 
 * // constraints double RT = 2.480;
 * 
 * Matrix Constraint_1 = new Matrix(2 * num_reactions + num_stoffe, 2
 * num_reactions + num_stoffe, 0);
 * 
 * for (int i = 0; i < num_stoffe; i++) { for (int j = num_stoffe; j <
 * num_stoffe + num_reactions; j++) { Constraint_1.set(i, j, RT * stoich.get(i,
 * j - num_stoffe)); } }
 * 
 * for (int i = num_stoffe; i < num_stoffe + num_reactions; i++) { for (int j =
 * 0; j < num_stoffe; j++) { Constraint_1.set(i, j, RT * stoich.get(j, i -
 * num_stoffe)); } for (int j = num_stoffe + num_reactions; j < num_stoffe + 2
 * num_reactions; j++) { if (j - num_reactions == i) { Constraint_1.set(i, j,
 * 1); } } }
 * 
 * for (int i = num_stoffe + num_reactions; i < num_stoffe + 2 num_reactions;
 * i++) { for (int j = num_stoffe; j < num_stoffe + num_reactions; j++) { if (i
 * - num_reactions == j) { Constraint_1.set(i, j, 1); } } }
 * 
 * double cons1[] = new double[num_stoffe + 2 * num_reactions];
 * 
 * for (int i = 0; i < num_stoffe + 2 * num_reactions; i++) { cons1[i] = 0.0; }
 * 
 * for (int i = num_stoffe; i < num_stoffe + num_reactions; i++) { cons1[i] =
 * Gibbs_energies[i - num_stoffe]; }
 * 
 * 
 * double cons2[] = new double[num_stoffe + 2* num_reactions]; double r_max =
 * 1.0;
 * 
 * 
 * for (int i = 0; i < num_stoffe; i++) { double total_quant = 0; for (int j =
 * 0; j < stoich.getColumnDimension(); j++){ total_quant += stoich.get(i, j); }
 * cons2[i] = RT*-r_max*total_quant; } for (int i = num_stoffe; i <
 * num_stoffe+num_reactions; i++){ cons2[i]=1.0; } for (int i =
 * num_stoffe+num_reactions; i < num_stoffe+2*num_reactions; i++){ cons2[i] =
 * r_max; }
 * 
 * 
 * 
 * 
 * // call ScpSolver to solve minimization problem
 * 
 * QuadraticProgram minimize = new QuadraticProgram(Q.getArray(), M);
 * minimize.addConstraint(new QuadraticSmallerThanEqualsContraint(
 * Constraint_1.getArray(), cons1, ChoiceFormat
 * .previousDouble(0.0),"Riesenrotz")); minimize.addConstraint(new
 * LinearSmallerThanEqualsConstraint(cons2,ChoiceFormat
 * .previousDouble(0.0),"nochmehrRiesenROTZ")); QuadraticProgramSolver solver
 * =(QuadraticProgramSolver) SolverFactory.getSolver("CPLEX"); double[] sol =
 * solver.solve(minimize);
 * 
 * 
 * 
 * }
 *//**
 * return an array with needed Gibbsenergies
 * 
 * @param model
 * @param string
 * @return
 * @throws IOException
 */
/*
 * private static double[] read_file(Model model, String string, Boolean a)
 * throws IOException { if (a) { double energies[] = new
 * double[model.getNumReactions() + 2];// TODO FileReader r = new
 * FileReader(string); BufferedReader read = new BufferedReader(r); for (int i =
 * 0; i < model.getNumReactions() + 2; i++) {// TODO String k[] = new String[2];
 * k = read.readLine().split("\t"); energies[i] = Double.valueOf(k[1]); } return
 * energies; } else { double concentrations[] = new
 * double[model.getNumSpecies()]; FileReader r = new FileReader(string);
 * BufferedReader read = new BufferedReader(r); for (int i = 0; i <
 * model.getNumSpecies(); i++) { String k[] = new String[2]; k =
 * read.readLine().split("\t"); concentrations[i] = Double.valueOf(k[1]); }
 * return concentrations; } }
 *//**
 * write Miriam Annotations of all Reactions to a file
 * 
 * @param model
 * @param outputfile
 * @throws IOException
 */
/*
 * 
 * public static void Gibbs_to_file(Model model, String outputfile) throws
 * IOException { FileWriter outfile = new FileWriter(outputfile); String prefix
 * = "urn:miriam:kegg.reaction:"; for (int i = 0; i < model.getNumReactions();
 * i++) { Reaction r = model.getReaction(i); for (CVTerm term : r.getCVTerms())
 * { if (term.getBiologicalQualifierType() == CVTerm.Qualifier.BQB_IS) { for
 * (String resource : term.getResources()) { if (resource.startsWith(prefix)) {
 * outfile .write(resource.substring(prefix.length()) + '\n'); } } } } }
 * outfile.close(); }
 * 
 * public static Matrix roundMatrix(Matrix k) { for (int i = 0; i <
 * k.getRowDimension(); i++) { for (int j = 0; j < k.getColumnDimension(); j++)
 * { k.set(i, j, roundTwoDecimals(k.get(i, j))); } } return k;
 * 
 * }
 *//**
 * round double to human readable format
 * 
 * @param d
 * @return
 */
/*
 * public static double roundTwoDecimals(double d) { DecimalFormat twoDForm =
 * new DecimalFormat("#.###"); return Double.valueOf(twoDForm.format(d)); }
 *//**
 * rearrange the matrix to fit a steady state null space
 * 
 * @param matrix
 * @param p
 * @return
 */
/*
 * 
 * public static Matrix rearrange(Matrix matrix, int p[]) {
 * 
 * double H[][] = matrix.getArray(); double out[][] = new
 * double[matrix.getRowDimension()][matrix .getColumnDimension()];
 * 
 * for (int j = 0; j < matrix.getRowDimension(); j++) {
 * 
 * out[j] = H[p[j]]; }
 * 
 * return new Matrix(out).transpose();
 * 
 * }
 *//**
 * compute the row reduced echolon form of a matrix
 * 
 * @param M
 */
/*
 * 
 * double[][] mtx = { { 1, 2, -1, -4}, { 2, 3, -1,-11}, {-2, 0, -3, 22}}; K =
 * new Matrix(mtx);
 * 
 * public static Matrix toRREF(Matrix matrix) { int rowCount =
 * matrix.getRowDimension(); if (rowCount == 0) return matrix;
 * 
 * int columnCount = matrix.getColumnDimension(); double M[][] =
 * matrix.getArray();
 * 
 * int lead = 0; for (int r = 0; r < rowCount; r++) { if (lead >= columnCount)
 * break; { int i = r; while (M[i][lead] == 0) { i++; if (i == rowCount) { i =
 * r; lead++; if (lead == columnCount) return new Matrix(M); } } double[] temp =
 * M[r];
 * 
 * M[r] = M[i]; M[i] = temp; }
 * 
 * { double lv = M[r][lead]; for (int j = 0; j < columnCount; j++) M[r][j] /=
 * lv; }
 * 
 * for (int i = 0; i < rowCount; i++) { if (i != r) { double lv = M[i][lead];
 * for (int j = 0; j < columnCount; j++) M[i][j] -= lv * M[r][j]; } } lead++; }
 * return matrix; }
 *//**
 * Exchange row s and t in matrix
 * 
 * @param matrix
 * @param s
 * @param t
 * @return matrix
 */
/*
 * 
 * public static Matrix rowexchange(Matrix matrix, int s, int t) { int num_rows
 * = matrix.getRowDimension(); int num_cols = matrix.getColumnDimension();
 * double hold;
 * 
 * for (int i = 0; i < num_cols; i++) { hold = matrix.get(s, i); matrix.set(s,
 * i, matrix.get(t, i)); matrix.set(t, i, hold); } return matrix;
 * 
 * }
 *//**
 * Exchange column s and t in matrix
 * 
 * @param matrix
 * @param s
 * @param t
 * @return
 */
/*
 * 
 * public static Matrix colexchange(Matrix matrix, int s, int t) {
 * 
 * int num_rows = matrix.getRowDimension(); int num_cols =
 * matrix.getColumnDimension(); double hold;
 * 
 * for (int i = 0; i < num_rows; i++) { hold = matrix.get(i, s); matrix.set(i,
 * s, matrix.get(i, t)); matrix.set(i, t, hold); } return matrix;
 * 
 * } // // public static Matrix echolonform(Matrix matrix, int p[]) { // int
 * num_rows = matrix.getRowDimension(); // int num_cols =
 * matrix.getColumnDimension(); // Matrix mat = matrix.copy(); // int hold; //
 * int red_index = num_rows - 1; // // for (int i = 0; i <= red_index; i++) { //
 * // if (mat.get(i, i) == 0) { // System.out.println("jo"); // int j = i + 1;
 * // while (mat.get(i, j) == 0 && (j < num_cols)) { // j++; // } // if (j ==
 * num_cols) { // if (i < red_index) { // mat = rowexchange(mat, i, red_index);
 * // } // red_index--; // } else { // mat = colexchange(mat, i, j); // hold =
 * p[j]; // p[j] = p[i]; // p[i] = hold; // } // // } else { // Matrix matb =
 * mat.copy(); // for (int m = i + 1; i <= red_index; m++) { // for (int j = i;
 * j < num_cols; j++) { // System.out.println(m); // System.out.println(i); //
 * if (mat.get(m, i) != 0) { // matb.set(m, j, (mat.get(m, j) * mat.get(i, i) //
 * / mat.get(m, i) - mat.get(i, j))); // } // } // } // for (int m = i + 1; i <=
 * red_index; m++) { // for (int j = i; j < num_cols; j++) { // mat.set(m, j,
 * matb.get(m, j)); // } // } // i++; // } // // } // // return mat; // } // //
 * // public static void toRREF(double[][] M) { // // int rowCount = M.length;
 * // // if (rowCount == 0) // // return; // // // // int columnCount =
 * M[0].length; // // // // int lead = 0; // // for (int r = 0; r < rowCount;
 * r++) { // // if (lead >= columnCount) // // break; // // { // // int i = r;
 * // // while (M[i][lead] == 0) { // // i++; // // if (i == rowCount) { // // i
 * = r; // // lead++; // // if (lead == columnCount) // // return; // // } // //
 * } // // double[] temp = M[r]; // // M[r] = M[i]; // // M[i] = temp; // // }
 * // // // // { // // double lv = M[r][lead]; // // for (int j = 0; j <
 * columnCount; j++) // // M[r][j] /= lv; // // } // // // // for (int i = 0; i
 * < rowCount; i++) { // // if (i != r) { // // double lv = M[i][lead]; // //
 * for (int j = 0; j < columnCount; j++) // // M[i][j] -= lv * M[r][j]; // // }
 * // // } // // lead++; // // } // // } // // }
 */