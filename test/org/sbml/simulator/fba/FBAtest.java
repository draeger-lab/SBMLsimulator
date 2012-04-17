package org.sbml.simulator.fba;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.util.StringTools;
import org.sbml.simulator.math.StabilityMatrix;
import org.sbml.simulator.math.StoichiometricMatrix;

import eva2.tools.math.Jama.Matrix;

public class FBAtest {

	/**
	 * args[0]: SBML model
	 * args[1]: number of given target fluxes
	 * args[2 - numberoftargetfluxes+2]: array with indices of target fluxes
	 * args[numberoftargetfluxes+3  - 2*numberoftargetfluxes+2]: array with value of targetfluxes in corresponding order to the indices
	 * 
	 * 
	 * @param args
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	public static void main(String[] args) throws IOException,
			XMLStreamException {
	  File sbmlFile = new File(args[0]);
	  int targetFluxIndices[] = null;
	  double targetFluxes[] = null;
    if (args.length > 1){
      targetFluxIndices = new int[Integer.valueOf(args[1])];
      for (int i=0; i < targetFluxIndices.length; i++) {
        targetFluxIndices[i] = Integer.parseInt(args[i + 2]); 
      }
      if (args.length == 2 * targetFluxIndices.length + 2) {
        targetFluxes = new double[targetFluxIndices.length];
        for (int i=0; i< targetFluxIndices.length; i++) {
          targetFluxes[i] = Double.parseDouble(args[targetFluxIndices.length + 3 + i]);
        }
      }
    }
		conductFBA(sbmlFile, targetFluxIndices, targetFluxes);
	}

	/**
	 * 
	 * @param sbmlFile
	 * @param targetFluxIndices
	 * @param targetFluxes
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	private static void conductFBA(File sbmlFile, int targetFluxIndices[], double targetFluxes[]) throws XMLStreamException, IOException {
	  SBMLDocument a = (new SBMLReader()).readSBML(sbmlFile.getAbsolutePath());

	  

    // get the Stoichiometry from a given model

    double stoech[][] = new double[a.getModel().getNumSpecies()][a
        .getModel().getNumReactions()];

    Model model = a.getModel();
    Reaction reaction;
    SpeciesReference specRef;
    // sehr ineffizient...
    for (int i = 0; i < model.getNumReactions(); i++) {
      reaction = model.getReaction(i);
      System.out.println(model.getReaction(i));
      for (int j = 0; j < reaction.getNumReactants(); j++) {
        specRef = reaction.getReactant(j);
        stoech[model.getListOfSpecies().indexOf(
            specRef.getSpeciesInstance())][i] = -specRef
            .getStoichiometry();
      }
      for (int j = 0; j < reaction.getNumProducts(); j++) {
        specRef = reaction.getProduct(j);
        // Hier war doppelter Zugriff... auf j-tes Element!
        stoech[model.getListOfSpecies().indexOf(
            specRef.getSpeciesInstance())][i] = specRef.getStoichiometry();
      }

    }

    Matrix stoich = new Matrix(stoech);
    
    // reduce Matrix
    
    //StabilityMatrix red_matrix = ConservationRelations.calculateConsRelations(new StabilityMatrix(stoich.getArrayCopy(),stoich.getRowDimension(),stoich.getColumnDimension()));
    
    
    StoichiometricMatrix red_matrix = new StoichiometricMatrix(stoich.getArray(),stoich.getRowDimension(),stoich.getColumnDimension());
    StabilityMatrix redu_matrix =  red_matrix.getReducedMatrix();
            
    stoich = (Matrix) redu_matrix;
    
  

    // generate the null space Matrix

    int rang = stoich.rank();
    int dim = stoich.getColumnDimension();
    int r = dim - rang; // Rangdefizienz

    //SingularValueDecompostion and Nullspace
    Matrix G = stoich.copy();
    G = G.svd().getV().transpose();
    
    int k[] = new int[r]; 
    
    for (int i = 0; i < r; i++){
      k[i]= G.getRowDimension()-r+i;
      System.out.println(k[i]);
    }
    int c[] = new int[G.getColumnDimension()];
    for (int i = 0; i < G.getColumnDimension();i++){
      c[i]=i;
    }
    
    G= G.getMatrix(k, c);
    
    
    
    
    
    /*
    
    // SingularValueDecomposition
    SimpleMatrix G = new SimpleMatrix(stoich.getArray());
    // Nullspace
    System.out.println(G.svd().getW());
    System.out.println(G.svd().getU());
    System.out.println(G.svd().getV());
    G = G.svd().nullSpace();
    // G.print();
    
    
    System.out.println(G);
    System.out.println(stoich);
    System.out.println(r);
    System.out.println(rang);
    */
    
    
    
    // block diagonalization of matrix
    int num_cols = G.getColumnDimension();
    int num_rows = G.getRowDimension();
    Matrix K = new Matrix(num_rows, num_cols);

    K= G;
    

    // p speichert indices von K
    int p[] = new int[K.getColumnDimension()];
    for (int i = 0; i < K.getColumnDimension(); i++) {
      p[i] = i;
    }
    
    
    
    if ((targetFluxIndices != null) && (targetFluxIndices.length > 1)) {
      //int tar_flux[] = new int[num_target_fluxes];
            
      for (int i = 0; i < targetFluxIndices.length ;i++){
        int temp = p[i];
        p[i] = targetFluxIndices[i];
        p[targetFluxIndices[i]] = temp;
      }
      
      
    }
    
    Matrix K_trans = K.transpose();
    K_trans = toRREF(K_trans);
    
    // System.out.println(K);

    Matrix Alpha = new Matrix(K.getRowDimension(),K.getColumnDimension());
    double target_fluxes[][] = new double[r][1];
    if ((targetFluxIndices != null) && (targetFluxIndices.length > 0)) {
            
      for(int i = 0; i < targetFluxIndices.length; i++){
        target_fluxes[i][0] = targetFluxes[i];
      }
      
      /*
       * in case that not enough target fluxes are given?!
       * 
      while (num_target_fluxes< r){
        Random rand = new Random();
        target_fluxes[num_target_fluxes][0]= rand.nextInt(5);
        num_target_fluxes++;
        
      }
      */
      double alpha[] = new double[K.getColumnDimension()];
      Alpha = K.solve(new Matrix(target_fluxes));
      
      
    }
    
    // in case no targetfluxes are given, it will work with an example array
    else{
      Random rand = new Random();
      
            
      double alpha[] = new double[K_trans.getRowDimension()];
      for (int i = 0; i <r;i++){
        target_fluxes[i][0] = rand.nextInt(5);
      }
      int ki [] = new int[r];
      for( int i = 0; i < r; i++){
        ki[i] = i;
      }
      int ci[] = new int[K_trans.getColumnDimension()];
      for (int i = 0; i< K_trans.getColumnDimension();i++){
        ci[i] = i;
      }
      
      Alpha = K_trans.getMatrix(ki, ci).solve(new Matrix(target_fluxes));       
    }
    
    K = rearrange(K_trans, p);

    System.out.println(G);
    System.out.println(K);
    System.out.println(Alpha);
    System.out.println(target_fluxes.length);
    Gibbs_to_file(model, "test.txt");
    
    double Gibbs_energies[] = read_Gibbs_file(model, sbmlFile.getAbsolutePath()
        .substring(0, sbmlFile.getAbsolutePath().lastIndexOf('/'))
        + "/Gibbs.txt");
    System.out.println(Arrays.toString(Gibbs_energies));
  }






  /**
	 * 
	 * @param model
	 * @param string
	 * @return
	 * @throws IOException
	 */
	private static double[] read_Gibbs_file(Model model,String string) throws IOException {
		
		List<Double> energies = new LinkedList<Double>();
		FileReader r = new FileReader(string);
		BufferedReader read = new BufferedReader(r);
		String k[] = new String[2];
		String line;
		for(int i = 0; i < model.getNumReactions()+2; i++){//TODO
		  line = read.readLine();
      if ((line != null) && (line.length() > 0) && (line.contains("\t"))) {
        k = line.split("\t");
        energies.add(Double.valueOf(k[1]));
      }
		}
		double e[] = new double[energies.size()];
		for (int i=0; i<energies.size(); i++) {
		  e[i] = energies.get(i).doubleValue();
		}
		return e;
	}

	/**
	 * write Miriam Annotations of all Reactions to a file
	 * 
	 * @param model
	 * @param outputfile
	 * @throws IOException
	 */

	public static void Gibbs_to_file(Model model, String outputfile)
			throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
		String prefix = "urn:miriam:kegg.reaction:";
    for (int i = 0; i < model.getNumReactions(); i++) {
      Reaction r = model.getReaction(i);
      List<String> terms = r.filterCVTerms(CVTerm.Qualifier.BQB_IS, prefix);
      if (terms.size() > 0) {
        StringBuffer sb = new StringBuffer();
        sb.append(terms.get(0).substring(prefix.length()));
        sb.append('\t');
        for (int j=0; j<r.getNumReactants(); j++) {
          if (j > 0) {
            sb.append('+');
          }
          write(sb, r.getReactant(j));
        }
        sb.append('=');
        for (int j=0; j<r.getNumProducts(); j++) {
          if (j > 0) {
            sb.append('+');
          }
          write(sb, r.getProduct(j));
        }
        String line = sb.toString();
        if (!line.contains("XXX")) {
          bw.append(sb);
          bw.append('\n');
        }
      }
    }
		bw.close();
	}
	
	/**
	 * 
	 * @param sb
	 * @param sr
	 * @throws IOException
	 */
	private static void write(StringBuffer sb, SpeciesReference sr) throws IOException {
	  Species species;
	  String prefix = "urn:miriam:kegg.compound:";
    sb.append(StringTools.toString(sr.getStoichiometry()));
    sb.append(' ');
    species = sr.getSpeciesInstance();
    List<String> terms = species.filterCVTerms(CVTerm.Qualifier.BQB_IS, prefix);
    if (terms.size() > 0) {
      sb.append(terms.get(0).substring(prefix.length()));
    } else {
      sb.append("XXX");
    }
  }






  public static Matrix roundMatrix(Matrix k){
		for( int i = 0; i < k.getRowDimension(); i++){
			for(int j = 0; j< k.getColumnDimension();j++){
				k.set(i, j, roundTwoDecimals(k.get(i,j)));
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
	 * double[][] mtx = { { 1, 2, -1, -4}, { 2, 3, -1,-11}, {-2, 0, -3,
	 * 22}}; K = new Matrix(mtx);
	 */
	public static Matrix toRREF(Matrix matrix) {
		int rowCount = matrix.getRowDimension();
		if (rowCount == 0)
			return matrix;

		int columnCount = matrix.getColumnDimension();
		double M[][] = matrix.getArray();

		int lead = 0;
		for (int r = 0; r < rowCount; r++) {
			if (lead >= columnCount)
				break;
			{
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
				for (int j = 0; j < columnCount; j++)
					M[r][j] /= lv;
			}

			for (int i = 0; i < rowCount; i++) {
				if (i != r) {
					double lv = M[i][lead];
					for (int j = 0; j < columnCount; j++)
						M[i][j] -= lv * M[r][j];
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
	
	/**
	 * War auskommentiert...
	 * @param matrix
	 * @param p
	 * @return
	 */
  public static Matrix echolonform(Matrix matrix, int p[]) {
    int num_rows = matrix.getRowDimension();
    int num_cols = matrix.getColumnDimension();
    Matrix mat = matrix.copy();
    int hold;
    int red_index = num_rows - 1;
    
    for (int i = 0; i <= red_index; i++) {
      
      if (mat.get(i, i) == 0) {
        System.out.println("jo");
        int j = i + 1;
        while (mat.get(i, j) == 0 && (j < num_cols)) {
          j++;
        }
        if (j == num_cols) {
          if (i < red_index) {
            mat = rowexchange(mat, i, red_index);
          }
          red_index--;
        } else {
          mat = colexchange(mat, i, j);
          hold = p[j];
          p[j] = p[i];
          p[i] = hold;
        }
        
      } else {
        Matrix matb = mat.copy();
        for (int m = i + 1; i <= red_index; m++) {
          for (int j = i; j < num_cols; j++) {
            System.out.println(m);
            System.out.println(i);
            if (mat.get(m, i) != 0) {
              matb
                  .set(m, j,
                    (mat.get(m, j) * mat.get(i, i) / mat.get(m, i) - mat.get(i,
                      j)));
            }
          }
        }
        for (int m = i + 1; i <= red_index; m++) {
          for (int j = i; j < num_cols; j++) {
            mat.set(m, j, matb.get(m, j));
          }
        }
        i++;
      }
      
    }
    
    return mat;
  }
	
  /**
   * War auskommentiert.
   * @param M
   */
  public static void toRREF(double[][] M) {
    int rowCount = M.length;
    if (rowCount == 0)
      return;
    
    int columnCount = M[0].length;
    
    int lead = 0;
    for (int r = 0; r < rowCount; r++) {
      if (lead >= columnCount)
        break;
      {
        int i = r;
        while (M[i][lead] == 0) {
          i++;
          if (i == rowCount) {
            i = r;
            lead++;
            if (lead == columnCount)
              return;
          }
        }
        double[] temp = M[r];
        M[r] = M[i];
        M[i] = temp;
      }
      
      {
        double lv = M[r][lead];
        for (int j = 0; j < columnCount; j++)
          M[r][j] /= lv;
      }
      
      for (int i = 0; i < rowCount; i++) {
        if (i != r) {
          double lv = M[i][lead];
          for (int j = 0; j < columnCount; j++)
            M[i][j] -= lv * M[r][j];
        }
      }
      lead++;
    }
  }
	
	
}
