package org.sbml.simulator.fba;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.fba.util.FBAutil;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.simulator.math.StabilityMatrix;
import org.sbml.simulator.math.StoichiometricMatrix;

import eva2.tools.math.Jama.Matrix;

/**
 * 
 * @author Simon Sch&auml;fer
 *
 */
public class FluxBalanceAnalysis {

  /**
   * 
   */
  private final static transient Logger logger = Logger
      .getLogger(FluxBalanceAnalysis.class.getName());

  // constants
  double RT = 2.480d;
  double r_max = 1d;

  double lambda_1 = 10d;
  double lambda_2 = 10d;
  double lambda_3 = 0.01d;
  double lambda_4 = 1d;

  //

  Model model;
  Matrix stoich;

  /**
   * 
   * @return
   */
  public Matrix getStoich() {
    return stoich;
  }

  /**
   * 
   * @param stoich
   */
  public void setStoich(Matrix stoich) {
    this.stoich = stoich;
  }

  /**
   * 
   * @return
   */
  public int getNum_spec() {
    return stoich.getRowDimension();
  }

  /**
   * 
   * @return
   */
  public int getNum_rec() {
    return stoich.getColumnDimension();
  }

  double fluxes[];
  double errors[];
  double conc[];

  private Map<String, Double> Gibbs_energies;

  private double[] c_eq;

  /**
   * 
   * @param model
   * @param Gibbs
   * @param conc
   * @throws IOException 
   */
  public FluxBalanceAnalysis(Model model, File Gibbs, File conc)
    throws IOException {
    this.model = model;
    Gibbs_energies = FBAutil.readGibbEnergyFile(model, Gibbs);
    logger.log(Level.INFO, Gibbs_energies.size() + " Gibbs energies given");
    c_eq = FBAutil.read_conc(model, conc);
  }

  /**
   * 
   * @param model
   * @param Gibbs
   * @param conc
   * @throws IOException 
   */
  public FluxBalanceAnalysis(Model model, String Gibbs, String conc) throws IOException {
    this(model, new File(Gibbs), new File(conc));
  }

  /**
   * 
   * @param doc
   * @param gibbs
   * @param conc
   * @throws XMLStreamException
   * @throws IOException
   */
  public FluxBalanceAnalysis(File doc, File gibbs, File conc)
      throws XMLStreamException, IOException {
    this(SBMLReader.read(doc).getModel(), gibbs, conc);
  }

  /**
   * 
   */
  public void generate_stoichiometry() {
    double stoech[][] = new double[model.getNumSpecies()][model
        .getNumReactions()];
    Reaction reaction;
    SpeciesReference specRef;
    for (int i = 0; i < model.getNumReactions(); i++) {
      reaction = model.getReaction(i);
      boolean trans = true;
      // throw transportreactions out
      
      if (reaction.getNumProducts() == reaction.getNumReactants()) {
        for (int j = 0; j < reaction.getNumProducts(); j++) {
          
          if (reaction.getReactant(j) != null) {
            if (reaction.getProduct(j).equals(reaction.getReactant(j))) {
              System.out.println("trans");
              
            } else {
              trans = false;
            }
          } else
            trans = false;
        }
      } else {
        trans = false;
      }
      if (!trans) {
        for (int j = 0; j < reaction.getNumReactants(); j++) {
          specRef = reaction.getReactant(j);
          stoech[model.getListOfSpecies().indexOf(specRef.getSpeciesInstance())][i] = -specRef
              .getStoichiometry();
        }
        for (int j = 0; j < reaction.getNumProducts(); j++) {
          specRef = reaction.getProduct(j);
          stoech[model.getListOfSpecies().indexOf(specRef.getSpeciesInstance())][i] = reaction
              .getProduct(j).getStoichiometry();
        }
        
      }
    }
    this.stoich = new Matrix(stoech);
  }

  /**
   * 
   * @param matz
   * @param mu
   * @throws IOException
   */
  public void solve(Matrix matz, double[] mu) throws IOException {

    // get the Stoichiometry from a given model

    // reduce Matrix

    // StabilityMatrix red_matrix =
    // ConservationRelations.calculateConsRelations(new
    // StabilityMatrix(stoich.getArrayCopy(),stoich.getRowDimension(),stoich.getColumnDimension()));

    StoichiometricMatrix red_matrix = new StoichiometricMatrix(stoich
        .getArray(), stoich.getRowDimension(), stoich
        .getColumnDimension());
    StabilityMatrix redu_matrix = red_matrix.getReducedMatrix();

    stoich = (Matrix) redu_matrix;

    // generate the null space Matrix

    int rang = stoich.rank();
    int dim = stoich.getColumnDimension();
    int num_reactions = dim;
    int num_compounds = stoich.getRowDimension();
    int r = dim - rang;

    logger.log(Level.FINEST, Integer.toString(dim) + "reactions");

    // SingularValueDecompostion and Nullspace
    Matrix G = stoich.copy();
    G = G.svd().getV().transpose();

    int k[] = new int[r];

    for (int i = 0; i < r; i++) {
      k[i] = G.getRowDimension() - r + i;

    }
    int c[] = new int[G.getColumnDimension()];
    for (int i = 0; i < G.getColumnDimension(); i++) {
      c[i] = i;
    }

    G = G.getMatrix(k, c);

    // block diagonalization of matrix
    int num_cols = G.getColumnDimension();
    int num_rows = G.getRowDimension();
    Matrix K = new Matrix(num_rows, num_cols);

    K = G;

    int p[] = new int[K.getColumnDimension()];
    for (int i = 0; i < K.getColumnDimension(); i++) {
      p[i] = i;
    }
    /*
     * if (args.length > 1) { int num_target_fluxes =
     * Integer.valueOf(args[1]); // int tar_flux[] = new
     * int[num_target_fluxes];
     * 
     * for (int i = 0; i < num_target_fluxes; i++) { int temp = p[i]; p[i] =
     * Integer.valueOf(args[i + 2]); p[Integer.valueOf(args[i + 2])] = temp;
     * }
     * 
     * }
     */

    Matrix K_trans = K.transpose();
    K_trans = FBAutil.toRREF(K_trans);

    // System.out.println(K);

    Matrix Alpha = new Matrix(K.getRowDimension(), K.getColumnDimension());
    double target_fluxes[][] = new double[r][1];
    /*
     * if (args.length > 1) {
     * 
     * int num_target_fluxes = Integer.valueOf(args[1]);
     * 
     * for (int i = 0; i < num_target_fluxes; i++) { target_fluxes[i][0] =
     * Double.valueOf(args[num_target_fluxes + 3 + i]); }
     * 
     * /* in case that not enough target fluxes are given?!
     * 
     * while (num_target_fluxes< r){ Random rand = new Random();
     * target_fluxes[num_target_fluxes][0]= rand.nextInt(5);
     * num_target_fluxes++;
     * 
     * }
     * 
     * double alpha[] = new double[K.getColumnDimension()]; Alpha =
     * K.solve(new Matrix(target_fluxes));
     * 
     * }
     * 
     * // in case no targetfluxes are given, it will work with an example
     * array else
     */
    {
      Random rand = new Random();

      double alpha[] = new double[K_trans.getRowDimension()];
      for (int i = 0; i < r; i++) {

        target_fluxes[i][0] = rand.nextInt(5);
      }
      int ki[] = new int[r];
      for (int i = 0; i < r; i++) {
        ki[i] = i;
      }
      int ci[] = new int[K_trans.getColumnDimension()];
      for (int i = 0; i < K_trans.getColumnDimension(); i++) {
        ci[i] = i;
      }

      Alpha = K_trans.getMatrix(ki, ci).solve(new Matrix(target_fluxes));

    }

    K = FBAutil.rearrange(K_trans, p);

    num_reactions = FBAutil.Gibbs_to_file(model, "gibbs.txt");
    String list_of_reactions[] = new String[num_reactions];
    list_of_reactions = FBAutil.getkegg(model, list_of_reactions);
    FBAutil.Conc_to_file(model, "conc.txt");
    logger.log(Level.INFO,
            "Created gibbs.txt and conc.txt. "
                + "You need to add Energies and Concentrations to them,"
                + " then start again with the two files as Gibbs_file and Conc_file. "
                + "Values in the .txt-files have to be seperated by a tab from their names."
                + "For proper use, make sure all miriam annotations are given in the SBMLDocument!");

    /*
     * alle concenctrations in ein file und mit Nullen befüllen dann mit dem
     * file arbeiten und auf reihenfolge scheißen -> no need for a hashmap
     * 
     * Gibbs energien werden null gesetzt wenn nicht in hashmap enthalten
     */

    // model.getSpecies(1).getCVTerm(1).getBiologicalQualifierType();
    // CVTerm.Qualifier.BQB_IS

    double energies[] = new double[num_reactions];

    for (int i = 0; i < num_reactions; i++) {
      energies[i] = Gibbs_energies.get(list_of_reactions[i]);
    }

    logger.log(Level.FINE, "Concentrations:" + Arrays.toString(c_eq));
    logger.log(Level.FINE, "energies:" + Arrays.toString(energies));

    // generate Matrix and Arrays for scpsolver(quadratic programming with
    // quadratic constraints)

    

  }

  /**
   * 
   * @return
   */
  public double getRT() {
    return RT;
  }

  /**
   * 
   * @param rT
   */
  public void setRT(double rT) {
    RT = rT;
  }

  /**
   * 
   * @return
   */
  public double getR_max() {
    return r_max;
  }

  /**
   * 
   * @param rMax
   */
  public void setR_max(double rMax) {
    r_max = rMax;
  }

  /**
   * 
   * @return
   */
  public double getLambda_1() {
    return lambda_1;
  }

  /**
   * 
   * @param lambda_1
   */
  public void setLambda_1(double lambda_1) {
    this.lambda_1 = lambda_1;
  }

  /**
   * 
   * @return
   */
  public double getLambda_2() {
    return lambda_2;
  }

  /**
   * 
   * @param lambda_2
   */
  public void setLambda_2(double lambda_2) {
    this.lambda_2 = lambda_2;
  }

  /**
   * 
   * @return
   */
  public double getLambda_3() {
    return lambda_3;
  }

  /**
   * 
   * @param lambda_3
   */
  public void setLambda_3(double lambda_3) {
    this.lambda_3 = lambda_3;
  }

  /**
   * 
   * @return
   */
  public double getLambda_4() {
    return lambda_4;
  }

  /**
   * 
   * @param lambda_4
   */
  public void setLambda_4(double lambda_4) {
    this.lambda_4 = lambda_4;
  }

  /**
   * 
   * @return
   */
  public Model getModel() {
    return model;
  }

  /**
   * 
   * @param model
   */
  public void setModel(Model model) {
    this.model = model;
  }

  /**
   * 
   * @return
   */
  public double[] getFluxes() {
    return fluxes;
  }

  /**
   * 
   * @return
   */
  public double[] getErrors() {
    return errors;
  }

  /**
   * 
   * @return
   */
  public Map<String, Double> getGibbsEnergies() {
    return Gibbs_energies;
  }

  /**
   * 
   * @return
   */
  public double[] getEquilibriumConcentrations() {
    return c_eq;
  }

}