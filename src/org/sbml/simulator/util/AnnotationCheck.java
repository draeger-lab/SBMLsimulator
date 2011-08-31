/*
 *  SBMLsqueezer creates rate equations for reactions in SBML files
 *  (http://sbml.org).
 *  Copyright (C) 2009 ZBIT, University of Tübingen, Andreas Dräger
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.sbml.simulator.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.util.StringTools;

/**
 * This class is intended to be analoguos to SBMLannotator but based on JSBML
 * classes for later inclusion into SBMLsqueezer.
 * 
 * @author Andreas Dr&auml;ger
 * 
 */
public class AnnotationCheck {

	// private static KEGGPortType serv;
	private static KEGGparser kegg;

	/**
	 * This is the link to the MIRIAM resources.
	 */
	private static final MIRIAMparser miriam = new MIRIAMparser();

	static {
		try {
			final String fileSeparator = System.getProperty("file.separator");
			final String resources = System.getProperty("user.dir")
					+ fileSeparator + "resources" + fileSeparator;
			final String keggPath = resources + "kegg" + fileSeparator;
//			miriam.setMIRIAMfile(resources + "MIRIAM.xml");
			kegg = new KEGGparser(keggPath + "compound", keggPath + "reaction",
					keggPath + "enzyme");
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	/**
	 * This method extracts all the KEGG ids stored in the given CVTerm object.
	 * It translates URIs if necessary and returns a List containing all ids.
	 * 
	 * @param cv
	 *            The CVTerm containing references to KEGG
	 * @return A List of KEGG ids belonging to this CVTerm.
	 */
	public static List<String> getKEGGids(CVTerm cv) {
		Vector<String> ids = new Vector<String>();
		for (int k = 0; k < cv.getNumResources(); k++) {
			String identifier = cv.getResourceURI(k);
			if (!identifier.startsWith("urn"))
				identifier = miriam.getMiriamURI(identifier);
			// String urls[] = miriam.getLocations(identifier);
			if (identifier != null) {
				if (identifier.contains(":kegg."))
					ids.add(identifier.substring(
							identifier.indexOf(":kegg.") + 6).replace(
							"compound", "cpd").replace("reaction", "rn"));
			} else
				System.err.println(" no URL available for resource identifier "
						+ identifier);
		}
		return ids;
	}

	/**
	 * This method returns a List of all KEGG ids belonging to the given species
	 * according to the name of the species.
	 * 
	 * @param s
	 *            A species whose KEGG compound ids are wanted.
	 * @return A List of all KEGG compound ids according to the name of the
	 *         species.
	 * @throws IOException
	 */
	public static List<String> getKEGGids(Species s) throws IOException {
		if (s.getNumCVTerms() == 0) {
			CVTerm cv = identifyKEGGids(s);
			if (cv.getResources().size() > 0)
				s.addCVTerm(cv);
		}
		Vector<String> sIDs = new Vector<String>();
		for (int j = 0; j < s.getNumCVTerms(); j++)
			sIDs.addAll(getKEGGids(s.getCVTerm(j)));
		return sIDs;
	}

	/**
	 * From the name of the given species this method identifies all KEGG
	 * reaction ids this species is involved in and returns these ids in an
	 * array.
	 * 
	 * @param species
	 * @return An array of all KEGG reaction ids the given species is involved
	 *         in.
	 * @throws IOException
	 */
	public static String[] getKEGGreactionIDs(Species species)
			throws IOException {
		List<String> keggIDs = getKEGGids(species);
		List<String> reactions = new Vector<String>();
		for (String id : keggIDs)
			// serv.get_reactions_by_compound(id);
			for (String rID : kegg.getReactionsByCompound(id))
				reactions.add(rID);
		return reactions.toArray(new String[] {});
	}

	/**
	 * Returns for a given SBase all KEGG reaction or compound ids annotated in
	 * CV terms. Thereby only values for the given qualifier are considered.
	 * Values for the qualifier can be taken from libsbmlconstants.
	 * 
	 * @param sb
	 *            An SBase object for which KEGG ids should be annotated.
	 * @param qualifier
	 *            The relation between KEGG id and SBase
	 * @return A list of KEGG ids for the given SBase in relation to the object
	 *         as specified by the qualifier.
	 */
	public static List<String> getKeggsIDs(SBase sb, CVTerm.Qualifier qualifier) {
		Vector<String> ids = new Vector<String>();
		for (int i = 0; i < sb.getNumCVTerms(); i++) {
			CVTerm cvt = sb.getCVTerm(i);
			if ((cvt.getModelQualifierType() == qualifier)
					|| (cvt.getBiologicalQualifierType() == qualifier)) {
				List<String> attr = cvt.getResources();
				for (int j = 0; j < attr.size(); j++) {
					String value = attr.get(i);
					if (!value.startsWith("urn"))
						value = miriam.getMiriamURI(value);
					// Examples:
					// urn:miriam:kegg.reaction:R00258
					// urn:miriam:kegg.compound:C00234
					String id = "";
					if (value.contains(":kegg.")) {
						id = value.substring(value.lastIndexOf(':'));
						if (value.contains(".reaction:"))
							id = "rn".concat(id);
						else if (value.contains(".compound:"))
							id = "cpd".concat(id);
						ids.add(id);
					}
				}
			}
		}
		return ids;
	}

	/**
	 * This method identifies the most probable KEGG reaction ids for the given
	 * reaction. First it looks at all modifiers and tries to identify known
	 * enzyme reactions from the given KEGG compound id of the modifier. If this
	 * does not lead to any success, the intersection between all reaction ids
	 * of all reactants and products is used as the reaction id of the given
	 * reaction.
	 * 
	 * @param r
	 *            A reaction whose KEGG reaction id is to be identified.
	 * @return A CVTerm that can be empty or contain a list of URIs pointing to
	 *         the corresponding reaction ids in KEGG.
	 * @throws IOException
	 */
	public static CVTerm identifyKEGGids(Reaction r) throws IOException {
		r.setMetaId("meta_" + r.getId());
		// System.out.println(r.getMetaId());
		int i, j;
		List<String> ids = new Vector<String>();
		if (r.getNumModifiers() > 0) {
			for (i = 0; i < r.getNumModifiers(); i++) {
				for (String id : getKEGGids(r.getModel().getSpecies(
						r.getModifier(i).getSpecies())))
					for (String keggID : kegg.getReactionsByEnzyme(id))
						// serv.get_reactions_by_enzyme(id))
						ids.add(keggID);
			}
		}
		if (ids.size() == 0) {
			Vector<String[]> idsTmp = new Vector<String[]>();
			for (i = 0; i < r.getNumReactants(); i++) {
				// System.out.println("reactant:\t"
				// + r.getReactant(i).getSpecies());
				Species species = r.getReactant(i).getSpeciesInstance();
				String rids[] = getKEGGreactionIDs(species);
				// TODO: verallgemeinern!
				if (!species.getId().equals("PROTON"))
					idsTmp.add(rids);
			}
			for (i = 0; i < r.getNumProducts(); i++) {
				// System.out.println("product:\t" +
				// r.getProduct(i).getSpecies());
				Species species = r.getProduct(i).getSpeciesInstance();
				// TODO: verallgemeinern!
				if (!species.getId().equals("PROTON"))
					idsTmp.add(getKEGGreactionIDs(species));
			}
			for (i = 0; i < idsTmp.size(); i++) {
				String curr[] = idsTmp.get(i);
				for (j = 0; j < curr.length; j++) {
					String id = curr[j];
					int found = 1;
					for (int k = 0; k < idsTmp.size(); k++) {
						if (k == i)
							continue;
						for (int l = 0; l < idsTmp.get(k).length; l++)
							if (id.equals(idsTmp.get(k)[l])) {
								found++;
								break;
							}
					}
					if ((found == idsTmp.size()) && !ids.contains(id))
						ids.add(id);
				}
			}
			// serv.get_reactions_by_compound();
			// // SBML2LaTeX latex = new SBML2LaTeX();
			// // StringWriter sw = new StringWriter();
			// // BufferedWriter bw = new BufferedWriter(sw);
			// // latex.format(cv, bw);
			// // bw.close();
			// // System.out.println(sw);
			// r.addCVTerm(cv);
			// System.out.println(r.toSBML());
			// }
		}
		// System.out.println(r.getId() + "\t" + ids.toString());
		CVTerm cv = new CVTerm();
		if (ids.size() > 0) {
			cv.setQualifierType(CVTerm.Type.BIOLOGICAL_QUALIFIER);
			cv.setBiologicalQualifierType(CVTerm.Qualifier.BQB_IS);
			for (String id : ids) {
				List<String> compounds = new Vector<String>();
				for (String string : Arrays.asList(kegg
						.getCompoundsByReaction(id)))
					compounds.add(string);
				// serv.get_compounds_by_reaction(id));
				int l;
				for (l = 0; l < r.getNumReactants(); l++) {
					compounds.removeAll(getKeggsIDs(r.getModel().getSpecies(
							r.getReactant(l).getSpecies()),
							CVTerm.Qualifier.BQB_IS));
				}
				for (l = 0; l < r.getNumProducts(); l++)
					compounds.removeAll(getKeggsIDs(r.getModel().getSpecies(
							r.getProduct(l).getSpecies()),
							CVTerm.Qualifier.BQB_IS));
				if (compounds.size() == 0) {
					String urn = "urn:miriam:kegg.reaction:" + id.split(":")[1];
					cv.addResource(urn);
				}
			}
		}
		return cv;
	}

	/**
	 * This method uses the name of the species to identify KEGG ids belonging
	 * to the corresponding compound. It returns a CVTerm object that can be
	 * empty if no ids can be found or that contains valid URIs pointing to the
	 * KEGG resource.
	 * 
	 * @param s
	 *            A species for which the KEGG ids are to be found.
	 * @return A CVTerm that may be empty or that contains valid URIs pointing
	 *         to corresponding KEGG compounds.
	 * @throws IOException
	 */
	public static CVTerm identifyKEGGids(Species s) throws IOException {
		s.setMetaId("meta_" + s.getId());
		String keggIds[] = StringOperations.merge(kegg.searchCompoundsByName(s
				.getName()), kegg.searchCompoundsByName(s.getId()));
		if (keggIds.length == 0) {
			// if (s instanceof BioCycCompound) {
			// BioCycCompound c = (BioCycCompound) s;
			// for (String syn : c.getSynonyms())
			// StringOperations.merge(keggIds, kegg
			// .searchCompoundsByName(syn));
			// if (keggIds.length == 0)
			// for (String syn : c.getSynonyms())
			// StringOperations.merge(keggIds, kegg
			// .searchClosestCompoundsByName(syn));
			// }
			if (keggIds.length == 0)
				keggIds = kegg.searchClosestCompoundsByName(s.getName());
			if (keggIds.length > 0) {
				String names[] = new String[] {};
				for (String id : keggIds)
					names = StringOperations.merge(names, kegg
							.searchCompoundsByID(id));
				// if (s instanceof BioCycCompound)
				// System.err.println("Name: " + s.getName() + "\tSynonyms: "
				// + ((BioCycCompound) s).getSynonyms().toString()
				// + "\tKEGG ids: " + Arrays.toString(keggIds)
				// + "\tKEGG names: " + Arrays.toString(names));
				// else
				System.err.println("Name: " + s.getName() + "\tKEGG ids: "
						+ Arrays.toString(keggIds) + "\tKEGG names: "
						+ Arrays.toString(names));
			}
		}
		// serv.search_compounds_by_name(s.getName());
		CVTerm cv = new CVTerm();
		if (keggIds.length > 0) {
			cv.setQualifierType(CVTerm.Type.BIOLOGICAL_QUALIFIER);
			for (String name : keggIds) {
				cv.setBiologicalQualifierType(CVTerm.Qualifier.BQB_IS);
				String urn = "urn:miriam:kegg.compound:";
				urn += name.contains(":") ? name.split(":")[1] : name;
				cv.addResource(urn);
			}
		}
		return cv;
	}

	/**
	 * 
	 * @param model
	 * @param replacement
	 *            number to be used as a replacement for "n" in empirical
	 *            formulas.
	 * @throws IOException
	 */
	public void checkAtomBalance(Model model, int replacement)
			throws IOException {
		Hashtable<String, Integer> defect;
		int noCheckPossible = 0, withDefect = 0;
		for (Reaction r : model.getListOfReactions()) {
			defect = checkAtomBalance(r, replacement);
			if (defect == null)
				noCheckPossible++;
			else if (defect.size() > 0)
				withDefect++;
		}
		System.out
				.printf(
						"No check possible for %d reactions.\n%d reactions with defect.\n",
						noCheckPossible, withDefect);
	}

	/**
	 * 
	 * @param r
	 * @param replacement
	 *            number to be used as a replacement of "n" in empirical
	 *            formulas.
	 * @return null if no check is possible. Otherwise an hash with the Atom
	 *         defects.
	 * @throws IOException
	 */
	private Hashtable<String, Integer> checkAtomBalance(Reaction r,
			int replacement) throws IOException {
		Hashtable<String, Integer> atomsLeft;
		Hashtable<String, Integer> atomsRight;
		Hashtable<String, Integer> defect;
		atomsLeft = countAtoms(r.getListOfReactants(), replacement);
		atomsRight = countAtoms(r.getListOfProducts(), replacement);
		if ((atomsLeft == null || atomsLeft.size() == 0)
				|| (atomsRight == null || atomsRight.size() == 0)) {
			System.out.printf("Couldn't check atom balance of reaction %s.\n",
					r.getId());
			return null;
		}
		defect = new Hashtable<String, Integer>();
		for (String key : atomsLeft.keySet()) {
			if (!atomsRight.containsKey(key))
				defect.put(key, atomsLeft.get(key));
			else {
				int left = atomsLeft.get(key).intValue(), right = atomsRight
						.get(key);
				if (left != right)
					defect.put(key, Integer.valueOf(Math.abs(left - right)));
			}
		}
		for (String key : atomsRight.keySet()) {
			if (!atomsLeft.containsKey(key))
				defect.put(key, atomsRight.get(key));
			else {
				int left = atomsLeft.get(key).intValue(), right = atomsRight
						.get(key);
				if (left != right)
					defect.put(key, Integer.valueOf(Math.abs(left - right)));
			}
		}
		// TODO: bad to write C5H8 here!!
		if (defect.size() > 0) {
			System.out.printf(
					"Detected incorrect atom balance in reaction %s:\n", r
							.getId());
			printReaction(r);
			System.out.printf("atoms left:\t%s\n", atomsLeft.toString());
			System.out.printf("atoms right:\t%s\n", atomsRight.toString());
			System.out.printf("defect:\t%s\n\n", defect.toString());
		}
		return defect;
	}

	/**
	 * 
	 * @param r
	 * @throws IOException
	 */
	public void printReaction(Reaction r) throws IOException {
		printReaction(r, true);
	}

	/**
	 * 
	 * @param r
	 * @param formula
	 *            if true the sum formula (empirical formula) of the reacting
	 *            species are printet. If false the KEGG ids are shown. If the
	 *            required information is not available, the ids of the species
	 *            are printed.
	 * @throws IOException
	 */
	private void printReaction(Reaction r, boolean formula) throws IOException {
		boolean plus = false;
		for (SpeciesReference specRef : r.getListOfReactants()) {
			if (plus)
				System.out.printf("+ ");
			else
				plus = true;
			if (specRef.getStoichiometry() != 1)
				System.out.printf("%s ", StringTools.toString(specRef
						.getStoichiometry()));
			String keggID = getKEGGids(specRef.getSpeciesInstance()).get(0);
			KEGGcompound compound = kegg.getCompoundForURI(keggID);
			if (compound == null)
				System.err.printf(
						"\nCouldn't find any compound for %s with id %s.\n",
						specRef.getSpecies(), keggID);
			String empiricalFormula = compound == null ? specRef.getSpecies()
					: compound.getFormula();
			if (empiricalFormula == null)
				empiricalFormula = specRef.getSpecies();
			System.out.printf("%s ", formula ? empiricalFormula : keggID);
		}
		System.out.printf(r.getReversible() ? "<=>" : "->");
		plus = false;
		for (SpeciesReference specRef : r.getListOfProducts()) {
			if (plus)
				System.out.printf(" +");
			else
				plus = true;
			if (specRef.getStoichiometry() != 1)
				System.out.printf(" %s", StringTools.toString(specRef
						.getStoichiometry()));
			System.out.printf(" %s", formula ? kegg.getCompoundForURI(
					getKEGGids(specRef.getSpeciesInstance()).get(0))
					.getFormula() : getKEGGids(specRef.getSpeciesInstance())
					.get(0));
		}
		if (r.getNumProducts() == 0)
			System.out.println(" empty set");
		System.out.println();
	}

	// /**
	// *
	// * @param r
	// * @throws IOException
	// */
	// public void printReactionInKEGGIDs(Reaction r) throws IOException {
	// printReaction(r, false);
	// }

	/**
	 * 
	 * @param stoichiometry
	 * @param formula
	 * @param replacement
	 *            used if "n" occurs in the formula (as number of atoms).
	 * @return
	 */
	public Hashtable<String, Integer> countAtoms(double stoichiometry,
			String formula, int replacement) {
		Hashtable<String, Integer> atomCount = new Hashtable<String, Integer>();
		StringBuilder name = new StringBuilder(), number = new StringBuilder();
		StringBuilder newFormula = new StringBuilder();
		boolean brackets = false;
		boolean digitAfter = false;
		for (int i = 0; i < formula.length(); i++) {
			char c = formula.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				if (brackets)
					name.append(c);
				else {
					if (digitAfter) {
						if (Character.isUpperCase(c)) {
							if (number.length() == 0)
								number.append(1);
							Hashtable<String, Integer> inlay = countAtoms(1,
									name.toString(), replacement);
							for (String key : inlay.keySet()) {
								if (!atomCount.containsKey(key))
									atomCount.put(key, Integer.valueOf(0));
								int mult = number.toString().contains("n") ? replacement
										: Integer.parseInt(number.toString());
								int val = (int) (stoichiometry
										* inlay.get(key).intValue() * mult);
								atomCount.put(key, Integer.valueOf(val));
							}
							digitAfter = false;
							System.out.printf("cut:\t%s\t%s", name, number);
						} else {
							number.append(c);
						}
					}
					if (!digitAfter) {
						newFormula.append(c);
					}
				}
			} else if (c == '(') {
				brackets = true;
				digitAfter = false;
				name = new StringBuilder();
				number = new StringBuilder();
			} else if (c == ')') {
				brackets = false;
				digitAfter = true;
			}
		}
		for (char c : newFormula.toString().toCharArray())
			if (Character.isLetterOrDigit(c)) {
				if (Character.isUpperCase(c)) {
					if (name.length() > 0) {
						if (number.length() == 0)
							number.append(1);
						if (digitAfter) {
							Hashtable<String, Integer> inlay = countAtoms(1,
									name.toString(), replacement);
							for (String key : inlay.keySet()) {
								if (!atomCount.containsKey(key))
									atomCount.put(key, Integer.valueOf(0));
								int mult = number.toString().contains("n") ? replacement
										: Integer.parseInt(number.toString());
								int val = (int) (stoichiometry
										* inlay.get(key).intValue() * mult);
								atomCount.put(key, Integer.valueOf(val));
							}
							digitAfter = false;
						} else {
							String key = name.toString();
							if (atomCount.containsKey(key))
								atomCount.put(key, (int) (atomCount.get(key)
										.intValue() + stoichiometry
										* Integer.parseInt(number.toString())));
							else {
								int num = number.toString().contains("n") ? num = replacement
										: Integer.parseInt(number.toString());
								atomCount.put(key, (int) (stoichiometry * num));
							}
						}
					}
					name = new StringBuilder(Character.toString(c));
					number = new StringBuilder();
				} else if (Character.isDigit(c)) {
					number.append(Character.toString(c));
				} else {
					name.append(c);
				}
			}
		if (name.length() > 0) {
			String key = name.toString();
			if (number.length() == 0)
				number.append(1);
			if (atomCount.containsKey(key))
				atomCount.put(key, atomCount.get(key).intValue()
						+ (int) (stoichiometry * Integer.parseInt(number
								.toString())));
			else {
				int num = number.toString().contains("n") ? num = replacement
						: Integer.parseInt(number.toString());
				atomCount.put(key, (int) (stoichiometry * num));
			}
		}
		return atomCount;
	}

	/**
	 * 
	 * @param listOfSpecRefs
	 * @param replacement
	 *            number to be used as a replacement if "n" occurs in an
	 *            empirical formula.
	 * @return
	 * @throws IOException
	 */
	public Hashtable<String, Integer> countAtoms(
			ListOf<SpeciesReference> listOfSpecRefs, int replacement)
			throws IOException {
		Hashtable<String, Integer> atomCount = new Hashtable<String, Integer>();
		for (SpeciesReference specRef : listOfSpecRefs) {
			List<String> keggIDs = getKEGGids(specRef.getSpeciesInstance());
			if (keggIDs == null) {
				atomCount.clear();
				break;
			}
			KEGGcompound comp = kegg.getCompoundForURI(keggIDs.get(0));
			if (comp == null) {
				atomCount.clear();
				break;
			}
			String formula = comp.getFormula();
			if (formula != null) {
				// TODO: consider better replacement.
				Hashtable<String, Integer> count = countAtoms(specRef
						.getStoichiometry(), formula, replacement);
				for (String key : count.keySet()) {
					if (!atomCount.containsKey(key))
						atomCount.put(key, Integer.valueOf(0));
					atomCount.put(key, Integer.valueOf(atomCount.get(key)
							.intValue()
							+ count.get(key).intValue()));
				}
			} else {
				atomCount.clear();
				break;
			}
		}
		// if (atomCount.containsKey("R"))
		// atomCount.clear();
		return atomCount;
	}

	/**
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws XMLStreamException 
	 */
  public static void main(String args[]) throws XMLStreamException, IOException {
    SBMLDocument doc = SBMLReader.read(new File(args[0]));
    AnnotationCheck ac = new AnnotationCheck(doc.getModel());
  }

	private Model model;

	/**
	 * 
	 * @return
	 */
	public Model getModel() {
		return model;
	}

	/**
	 * 
	 * @param m
	 */
	public AnnotationCheck(Model m) {
		model = m;
		// // printNamesOfReactionsWithoutAnnotation(model);
		// // printReactionAnnotation(m.getReaction("GLYCK"));
		// // identifyKEGGReactionIDs(m);
		// try {
		// // System.setOut(new PrintStream(new File(
		// // "/home/draeger/AtomBalancesEcoli.txt")));
		// checkAtomBalance(m);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// Species q8 = m.getSpecies("q8");
		// identifyReactionsWithIdenticalKEGGAnnotation(m);

		// for (Species s : m.getListOfSpecies())
		// try {
		// KEGGcompound compound = kegg.getCompoundForURI(s.filterCVTerms(
		// Qualifier.BQB_IS, "kegg").get(0));
		// Set<String> r = new HashSet<String>();
		// if (compound != null && compound.getFormula() != null
		// && compound.getFormula().contains("n")) {
		// System.out.printf("%s\t%s\t", s.getId(), compound
		// .getFormula());
		// for (Reaction re : m.getListOfReactions())
		// if (re.involves(s))
		// r.add(re.getId());
		// String rids[] = r.toArray(new String[] {});
		// Arrays.sort(rids);
		// System.out.println(Arrays.toString(rids));
		// }
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		// "AMMQT8_2", "DHNAOT", "Lump20"
		// for (String id : new String[] { "PROTOHEMEFERROCHELAT_RXN"})
		// try {
		// System.out.println(id);
		// Reaction r = m.getReaction(id);
		// printReactionInKEGGIDs(r);
		// printReaction(r);
		// checkAtomBalance(r, 7);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		// identifyIdenticalReaction(m);
		// identifyUnconnectedSpecies(m);

		// writeReactionsInGibbsPredFormat(m.getListOfReactions());
		selectDragonXDescriptors();
	}

	public void selectDragonXDescriptors() {
		String descriptors[] = { "MW", "AMW", "Sv", "Se", "Sp", "Ss", "Mv",
				"Me", "Mp", "Ms", "nAT", "nSK", "nBT", "nBO", "nBM", "SCBO",
				"ARR", "nCIC", "nCIR", "RBN", "RBF", "nDB", "nTB", "nAB", "nH",
				"nC", "nN", "nO", "nP", "nS", "nF", "nCL", "nBR", "nI", "nB",
				"nHM", "nX", "nR03", "nR04", "nR05", "nR06", "nR07", "nR08",
				"nR09", "nR10", "nR11", "nR12", "nBnz", "ZM1", "ZM1V", "ZM2",
				"ZM2V", "Qindex", "SNar", "HNar", "GNar", "Xt", "Dz", "Ram",
				"Pol", "LPRS", "VDA", "MSD", "SMTI", "SMTIV", "GMTI", "GMTIV",
				"Xu", "SPI", "W", "WA", "Har", "Har2", "QW", "TI1", "TI2",
				"STN", "HyDp", "RHyDp", "w", "ww", "Rww", "D/D", "Wap",
				"WhetZ", "Whetm", "Whetv", "Whete", "Whetp", "J", "JhetZ",
				"Jhetm", "Jhetv", "Jhete", "Jhetp", "MAXDN", "MAXDP", "DELS",
				"TIE", "S0K", "S1K", "S2K", "S3K", "PHI", "BLI", "PW2", "PW3",
				"PW4", "PW5", "PJI2", "CSI", "ECC", "AECC", "DECC", "MDDD",
				"UNIP", "CENT", "VAR", "BAC", "Lop", "ICR", "D/Dr03", "D/Dr04",
				"D/Dr05", "D/Dr06", "D/Dr07", "D/Dr08", "D/Dr09", "D/Dr10",
				"D/Dr11", "D/Dr12", "T(N..N)", "T(N..O)", "T(N..S)", "T(N..P)",
				"T(N..F)", "T(N..Cl)", "T(N..Br)", "T(N..I)", "T(O..O)",
				"T(O..S)", "T(O..P)", "T(O..F)", "T(O..Cl)", "T(O..Br)",
				"T(O..I)", "T(S..S)", "T(S..P)", "T(S..F)", "T(S..Cl)",
				"T(S..Br)", "T(S..I)", "T(P..P)", "T(P..F)", "T(P..Cl)",
				"T(P..Br)", "T(P..I)", "T(F..F)", "T(F..Cl)", "T(F..Br)",
				"T(F..I)", "T(Cl..Cl)", "T(Cl..Br)", "T(Cl..I)", "T(Br..Br)",
				"T(Br..I)", "T(I..I)", "MWC01", "MWC02", "MWC03", "MWC04",
				"MWC05", "MWC06", "MWC07", "MWC08", "MWC09", "MWC10", "TWC",
				"SRW01", "SRW02", "SRW03", "SRW04", "SRW05", "SRW06", "SRW07",
				"SRW08", "SRW09", "SRW10", "MPC01", "MPC02", "MPC03", "MPC04",
				"MPC05", "MPC06", "MPC07", "MPC08", "MPC09", "MPC10", "piPC01",
				"piPC02", "piPC03", "piPC04", "piPC05", "piPC06", "piPC07",
				"piPC08", "piPC09", "piPC10", "TPC", "piID", "PCR", "PCD",
				"CID", "BID", "X0", "X1", "X2", "X3", "X4", "X5", "X0A", "X1A",
				"X2A", "X3A", "X4A", "X5A", "X0v", "X1v", "X2v", "X3v", "X4v",
				"X5v", "X0Av", "X1Av", "X2Av", "X3Av", "X4Av", "X5Av", "X0sol",
				"X1sol", "X2sol", "X3sol", "X4sol", "X5sol", "XMOD", "RDCHI",
				"RDSQ", "ISIZ", "IAC", "AAC", "IDE", "IDM", "IDDE", "IDDM",
				"IDET", "IDMT", "IVDE", "IVDM", "HVcpx", "HDcpx", "Uindex",
				"Vindex", "Xindex", "Yindex", "IC0", "TIC0", "SIC0", "CIC0",
				"BIC0", "IC1", "TIC1", "SIC1", "CIC1", "BIC1", "IC2", "TIC2",
				"SIC2", "CIC2", "BIC2", "IC3", "TIC3", "SIC3", "CIC3", "BIC3",
				"IC4", "TIC4", "SIC4", "CIC4", "BIC4", "IC5", "TIC5", "SIC5",
				"CIC5", "BIC5", "ATS1m", "ATS2m", "ATS3m", "ATS4m", "ATS5m",
				"ATS6m", "ATS7m", "ATS8m", "ATS1v", "ATS2v", "ATS3v", "ATS4v",
				"ATS5v", "ATS6v", "ATS7v", "ATS8v", "ATS1e", "ATS2e", "ATS3e",
				"ATS4e", "ATS5e", "ATS6e", "ATS7e", "ATS8e", "ATS1p", "ATS2p",
				"ATS3p", "ATS4p", "ATS5p", "ATS6p", "ATS7p", "ATS8p", "MATS1m",
				"MATS2m", "MATS3m", "MATS4m", "MATS5m", "MATS6m", "MATS7m",
				"MATS8m", "MATS1v", "MATS2v", "MATS3v", "MATS4v", "MATS5v",
				"MATS6v", "MATS7v", "MATS8v", "MATS1e", "MATS2e", "MATS3e",
				"MATS4e", "MATS5e", "MATS6e", "MATS7e", "MATS8e", "MATS1p",
				"MATS2p", "MATS3p", "MATS4p", "MATS5p", "MATS6p", "MATS7p",
				"MATS8p", "GATS1m", "GATS2m", "GATS3m", "GATS4m", "GATS5m",
				"GATS6m", "GATS7m", "GATS8m", "GATS1v", "GATS2v", "GATS3v",
				"GATS4v", "GATS5v", "GATS6v", "GATS7v", "GATS8v", "GATS1e",
				"GATS2e", "GATS3e", "GATS4e", "GATS5e", "GATS6e", "GATS7e",
				"GATS8e", "GATS1p", "GATS2p", "GATS3p", "GATS4p", "GATS5p",
				"GATS6p", "GATS7p", "GATS8p", "EPS0", "EPS1", "EEig01x",
				"EEig02x", "EEig03x", "EEig04x", "EEig05x", "EEig06x",
				"EEig07x", "EEig08x", "EEig09x", "EEig10x", "EEig11x",
				"EEig12x", "EEig13x", "EEig14x", "EEig15x", "EEig01d",
				"EEig02d", "EEig03d", "EEig04d", "EEig05d", "EEig06d",
				"EEig07d", "EEig08d", "EEig09d", "EEig10d", "EEig11d",
				"EEig12d", "EEig13d", "EEig14d", "EEig15d", "EEig01r",
				"EEig02r", "EEig03r", "EEig04r", "EEig05r", "EEig06r",
				"EEig07r", "EEig08r", "EEig09r", "EEig10r", "EEig11r",
				"EEig12r", "EEig13r", "EEig14r", "EEig15r", "ESpm01u",
				"ESpm02u", "ESpm03u", "ESpm04u", "ESpm05u", "ESpm06u",
				"ESpm07u", "ESpm08u", "ESpm09u", "ESpm10u", "ESpm11u",
				"ESpm12u", "ESpm13u", "ESpm14u", "ESpm15u", "ESpm01x",
				"ESpm02x", "ESpm03x", "ESpm04x", "ESpm05x", "ESpm06x",
				"ESpm07x", "ESpm08x", "ESpm09x", "ESpm10x", "ESpm11x",
				"ESpm12x", "ESpm13x", "ESpm14x", "ESpm15x", "ESpm01d",
				"ESpm02d", "ESpm03d", "ESpm04d", "ESpm05d", "ESpm06d",
				"ESpm07d", "ESpm08d", "ESpm09d", "ESpm10d", "ESpm11d",
				"ESpm12d", "ESpm13d", "ESpm14d", "ESpm15d", "ESpm01r",
				"ESpm02r", "ESpm03r", "ESpm04r", "ESpm05r", "ESpm06r",
				"ESpm07r", "ESpm08r", "ESpm09r", "ESpm10r", "ESpm11r",
				"ESpm12r", "ESpm13r", "ESpm14r", "ESpm15r", "BEHm1", "BEHm2",
				"BEHm3", "BEHm4", "BEHm5", "BEHm6", "BEHm7", "BEHm8", "BELm1",
				"BELm2", "BELm3", "BELm4", "BELm5", "BELm6", "BELm7", "BELm8",
				"BEHv1", "BEHv2", "BEHv3", "BEHv4", "BEHv5", "BEHv6", "BEHv7",
				"BEHv8", "BELv1", "BELv2", "BELv3", "BELv4", "BELv5", "BELv6",
				"BELv7", "BELv8", "BEHe1", "BEHe2", "BEHe3", "BEHe4", "BEHe5",
				"BEHe6", "BEHe7", "BEHe8", "BELe1", "BELe2", "BELe3", "BELe4",
				"BELe5", "BELe6", "BELe7", "BELe8", "BEHp1", "BEHp2", "BEHp3",
				"BEHp4", "BEHp5", "BEHp6", "BEHp7", "BEHp8", "BELp1", "BELp2",
				"BELp3", "BELp4", "BELp5", "BELp6", "BELp7", "BELp8", "GGI1",
				"GGI2", "GGI3", "GGI4", "GGI5", "GGI6", "GGI7", "GGI8", "GGI9",
				"GGI10", "JGI1", "JGI2", "JGI3", "JGI4", "JGI5", "JGI6",
				"JGI7", "JGI8", "JGI9", "JGI10", "JGT", "LP1", "Eig1Z",
				"Eig1m", "Eig1v", "Eig1e", "Eig1p", "SEigZ", "SEigm", "SEigv",
				"SEige", "SEigp", "AEigZ", "AEigm", "AEigv", "AEige", "AEigp",
				"VEA1", "VEA2", "VRA1", "VRA2", "VED1", "VED2", "VRD1", "VRD2",
				"VEZ1", "VEZ2", "VRZ1", "VRZ2", "VEm1", "VEm2", "VRm1", "VRm2",
				"VEv1", "VEv2", "VRv1", "VRv2", "VEe1", "VEe2", "VRe1", "VRe2",
				"VEp1", "VEp2", "VRp1", "VRp2", "nCp", "nCs", "nCt", "nCq",
				"nCrs", "nCrt", "nCrq", "nCar", "nCbH", "nCb-", "nCconj",
				"nR=Cp", "nR=Cs", "nR=Ct", "n=C=", "nR#CH/X", "nR#C-", "nROCN",
				"nArOCN", "nRNCO", "nArNCO", "nRSCN", "nArSCN", "nRNCS",
				"nArNCS", "nRCOOH", "nArCOOH", "nRCOOR", "nArCOOR", "nRCONH2",
				"nArCONH2", "nRCONHR", "nArCONHR", "nRCONR2", "nArCONR2",
				"nROCON", "nArOCON", "nRCOX", "nArCOX", "nRCSOH", "nArCSOH",
				"nRCSSH", "nArCSSH", "nRCOSR", "nArCOSR", "nRCSSR", "nArCSSR",
				"nRCHO", "nArCHO", "nRCO", "nArCO", "nCONN", "nC=O(O)2",
				"nN=C-N<", "nC(=N)N2", "nRC=N", "nArC=N", "nRCNO", "nArCNO",
				"nRNH2", "nArNH2", "nRNHR", "nArNHR", "nRNR2", "nArNR2",
				"nN-N", "nN=N", "nRCN", "nArCN", "nN+", "nNq", "nRNHO",
				"nArNHO", "nRNNOx", "nArNNOx", "nRNO", "nArNO", "nRNO2",
				"nArNO2", "nN(CO)2", "nC=N-N<", "nROH", "nArOH", "nOHp",
				"nOHs", "nOHt", "nROR", "nArOR", "nROX", "nArOX", "nO(C=O)2",
				"nH2O", "nSH", "nC=S", "nRSR", "nRSSR", "nSO", "nS(=O)2",
				"nSOH", "nSOOH", "nSO2OH", "nSO3OH", "nSO2", "nSO3", "nSO4",
				"nSO2N", "nPO3", "nPO4", "nPR3", "nP(=O)O2R", "nP(=O)R3/nPR5",
				"nCH2RX", "nCHR2X", "nCR3X", "nR=CHX", "nR=CRX", "nR#CX",
				"nCHRX2", "nCR2X2", "nR=CX2", "nCRX3", "nArX", "nCXr", "nCXr=",
				"nCconjX", "nAziridines", "nOxiranes", "nThiranes",
				"nAzetidines", "nOxetanes", "nThioethanes", "nBeta-Lactams",
				"nPyrrolidines", "nOxolanes", "ntH-Thiophenes", "nPyrroles",
				"nPyrazoles", "nImidazoles", "nFuranes", "nThiophenes",
				"nOxazoles", "nIsoxazoles", "nThiazoles", "nIsothiazoles",
				"nTriazoles", "nPyridines", "nPyridazines", "nPyrimidines",
				"nPyrazines", "n135-Triazines", "n124-Triazines", "nHDon",
				"nHAcc", "nHBonds", "C-001", "C-002", "C-003", "C-004",
				"C-005", "C-006", "C-007", "C-008", "C-009", "C-010", "C-011",
				"C-012", "C-013", "C-014", "C-015", "C-016", "C-017", "C-018",
				"C-019", "C-020", "C-021", "C-022", "C-023", "C-024", "C-025",
				"C-026", "C-027", "C-028", "C-029", "C-030", "C-031", "C-032",
				"C-033", "C-034", "C-035", "C-036", "C-037", "C-038", "C-039",
				"C-040", "C-041", "C-042", "C-043", "C-044", "U-045", "H-046",
				"H-047", "H-048", "H-049", "H-050", "H-051", "H-052", "H-053",
				"H-054", "H-055", "O-056", "O-057", "O-058", "O-059", "O-060",
				"O-061", "O-062", "O-063", "Se-064", "Se-065", "N-066",
				"N-067", "N-068", "N-069", "N-070", "N-071", "N-072", "N-073",
				"N-074", "N-075", "N-076", "N-077", "N-078", "N-079", "U-080",
				"F-081", "F-082", "F-083", "F-084", "F-085", "Cl-086",
				"Cl-087", "Cl-088", "Cl-089", "Cl-090", "Br-091", "Br-092",
				"Br-093", "Br-094", "Br-095", "I-096", "I-097", "I-098",
				"I-099", "I-100", "F-101", "Cl-102", "Br-103", "I-104",
				"U-105", "S-106", "S-107", "S-108", "S-109", "S-110", "Si-111",
				"B-112", "U-113", "U-114", "P-115", "P-116", "P-117", "P-118",
				"P-119", "P-120", "Ui", "Hy", "AMR", "TPSA(NO)", "TPSA(Tot)",
				"MLOGP", "MLOGP2", "ALOGP", "ALOGP2", "LAI", "GVWAI-80",
				"GVWAI-50", "Inflammat-80", "Inflammat-50", "Depressant-80",
				"Depressant-50", "Psychotic-80", "Psychotic-50",
				"Hypertens-80", "Hypertens-50", "Hypnotic-80", "Hypnotic-50",
				"Neoplastic-80", "Neoplastic-50", "Infective-80",
				"Infective-50", "BLTF96", "BLTD48", "BLTA96", "B01[C-C]",
				"B01[C-N]", "B01[C-O]", "B01[C-S]", "B01[C-P]", "B01[C-F]",
				"B01[C-Cl]", "B01[C-Br]", "B01[C-I]", "B01[C-B]", "B01[C-Si]",
				"B01[C-X]", "B01[N-N]", "B01[N-O]", "B01[N-S]", "B01[N-P]",
				"B01[N-F]", "B01[N-Cl]", "B01[N-Br]", "B01[N-I]", "B01[N-B]",
				"B01[N-Si]", "B01[N-X]", "B01[O-O]", "B01[O-S]", "B01[O-P]",
				"B01[O-F]", "B01[O-Cl]", "B01[O-Br]", "B01[O-I]", "B01[O-B]",
				"B01[O-Si]", "B01[O-X]", "B01[S-S]", "B01[S-P]", "B01[S-F]",
				"B01[S-Cl]", "B01[S-Br]", "B01[S-I]", "B01[S-B]", "B01[S-Si]",
				"B01[S-X]", "B01[P-P]", "B01[P-F]", "B01[P-Cl]", "B01[P-Br]",
				"B01[P-I]", "B01[P-B]", "B01[P-Si]", "B01[P-X]", "B01[F-F]",
				"B01[F-Cl]", "B01[F-Br]", "B01[F-I]", "B01[F-B]", "B01[F-Si]",
				"B01[F-X]", "B01[Cl-Cl]", "B01[Cl-Br]", "B01[Cl-I]",
				"B01[Cl-B]", "B01[Cl-Si]", "B01[Cl-X]", "B01[Br-Br]",
				"B01[Br-I]", "B01[Br-B]", "B01[Br-Si]", "B01[Br-X]",
				"B01[I-I]", "B01[I-B]", "B01[I-Si]", "B01[I-X]", "B01[B-B]",
				"B01[B-Si]", "B01[B-X]", "B01[Si-Si]", "B01[Si-X]", "B01[X-X]",
				"B02[C-C]", "B02[C-N]", "B02[C-O]", "B02[C-S]", "B02[C-P]",
				"B02[C-F]", "B02[C-Cl]", "B02[C-Br]", "B02[C-I]", "B02[C-B]",
				"B02[C-Si]", "B02[C-X]", "B02[N-N]", "B02[N-O]", "B02[N-S]",
				"B02[N-P]", "B02[N-F]", "B02[N-Cl]", "B02[N-Br]", "B02[N-I]",
				"B02[N-B]", "B02[N-Si]", "B02[N-X]", "B02[O-O]", "B02[O-S]",
				"B02[O-P]", "B02[O-F]", "B02[O-Cl]", "B02[O-Br]", "B02[O-I]",
				"B02[O-B]", "B02[O-Si]", "B02[O-X]", "B02[S-S]", "B02[S-P]",
				"B02[S-F]", "B02[S-Cl]", "B02[S-Br]", "B02[S-I]", "B02[S-B]",
				"B02[S-Si]", "B02[S-X]", "B02[P-P]", "B02[P-F]", "B02[P-Cl]",
				"B02[P-Br]", "B02[P-I]", "B02[P-B]", "B02[P-Si]", "B02[P-X]",
				"B02[F-F]", "B02[F-Cl]", "B02[F-Br]", "B02[F-I]", "B02[F-B]",
				"B02[F-Si]", "B02[F-X]", "B02[Cl-Cl]", "B02[Cl-Br]",
				"B02[Cl-I]", "B02[Cl-B]", "B02[Cl-Si]", "B02[Cl-X]",
				"B02[Br-Br]", "B02[Br-I]", "B02[Br-B]", "B02[Br-Si]",
				"B02[Br-X]", "B02[I-I]", "B02[I-B]", "B02[I-Si]", "B02[I-X]",
				"B02[B-B]", "B02[B-Si]", "B02[B-X]", "B02[Si-Si]", "B02[Si-X]",
				"B02[X-X]", "B03[C-C]", "B03[C-N]", "B03[C-O]", "B03[C-S]",
				"B03[C-P]", "B03[C-F]", "B03[C-Cl]", "B03[C-Br]", "B03[C-I]",
				"B03[C-B]", "B03[C-Si]", "B03[C-X]", "B03[N-N]", "B03[N-O]",
				"B03[N-S]", "B03[N-P]", "B03[N-F]", "B03[N-Cl]", "B03[N-Br]",
				"B03[N-I]", "B03[N-B]", "B03[N-Si]", "B03[N-X]", "B03[O-O]",
				"B03[O-S]", "B03[O-P]", "B03[O-F]", "B03[O-Cl]", "B03[O-Br]",
				"B03[O-I]", "B03[O-B]", "B03[O-Si]", "B03[O-X]", "B03[S-S]",
				"B03[S-P]", "B03[S-F]", "B03[S-Cl]", "B03[S-Br]", "B03[S-I]",
				"B03[S-B]", "B03[S-Si]", "B03[S-X]", "B03[P-P]", "B03[P-F]",
				"B03[P-Cl]", "B03[P-Br]", "B03[P-I]", "B03[P-B]", "B03[P-Si]",
				"B03[P-X]", "B03[F-F]", "B03[F-Cl]", "B03[F-Br]", "B03[F-I]",
				"B03[F-B]", "B03[F-Si]", "B03[F-X]", "B03[Cl-Cl]",
				"B03[Cl-Br]", "B03[Cl-I]", "B03[Cl-B]", "B03[Cl-Si]",
				"B03[Cl-X]", "B03[Br-Br]", "B03[Br-I]", "B03[Br-B]",
				"B03[Br-Si]", "B03[Br-X]", "B03[I-I]", "B03[I-B]", "B03[I-Si]",
				"B03[I-X]", "B03[B-B]", "B03[B-Si]", "B03[B-X]", "B03[Si-Si]",
				"B03[Si-X]", "B03[X-X]", "B04[C-C]", "B04[C-N]", "B04[C-O]",
				"B04[C-S]", "B04[C-P]", "B04[C-F]", "B04[C-Cl]", "B04[C-Br]",
				"B04[C-I]", "B04[C-B]", "B04[C-Si]", "B04[C-X]", "B04[N-N]",
				"B04[N-O]", "B04[N-S]", "B04[N-P]", "B04[N-F]", "B04[N-Cl]",
				"B04[N-Br]", "B04[N-I]", "B04[N-B]", "B04[N-Si]", "B04[N-X]",
				"B04[O-O]", "B04[O-S]", "B04[O-P]", "B04[O-F]", "B04[O-Cl]",
				"B04[O-Br]", "B04[O-I]", "B04[O-B]", "B04[O-Si]", "B04[O-X]",
				"B04[S-S]", "B04[S-P]", "B04[S-F]", "B04[S-Cl]", "B04[S-Br]",
				"B04[S-I]", "B04[S-B]", "B04[S-Si]", "B04[S-X]", "B04[P-P]",
				"B04[P-F]", "B04[P-Cl]", "B04[P-Br]", "B04[P-I]", "B04[P-B]",
				"B04[P-Si]", "B04[P-X]", "B04[F-F]", "B04[F-Cl]", "B04[F-Br]",
				"B04[F-I]", "B04[F-B]", "B04[F-Si]", "B04[F-X]", "B04[Cl-Cl]",
				"B04[Cl-Br]", "B04[Cl-I]", "B04[Cl-B]", "B04[Cl-Si]",
				"B04[Cl-X]", "B04[Br-Br]", "B04[Br-I]", "B04[Br-B]",
				"B04[Br-Si]", "B04[Br-X]", "B04[I-I]", "B04[I-B]", "B04[I-Si]",
				"B04[I-X]", "B04[B-B]", "B04[B-Si]", "B04[B-X]", "B04[Si-Si]",
				"B04[Si-X]", "B04[X-X]", "B05[C-C]", "B05[C-N]", "B05[C-O]",
				"B05[C-S]", "B05[C-P]", "B05[C-F]", "B05[C-Cl]", "B05[C-Br]",
				"B05[C-I]", "B05[C-B]", "B05[C-Si]", "B05[C-X]", "B05[N-N]",
				"B05[N-O]", "B05[N-S]", "B05[N-P]", "B05[N-F]", "B05[N-Cl]",
				"B05[N-Br]", "B05[N-I]", "B05[N-B]", "B05[N-Si]", "B05[N-X]",
				"B05[O-O]", "B05[O-S]", "B05[O-P]", "B05[O-F]", "B05[O-Cl]",
				"B05[O-Br]", "B05[O-I]", "B05[O-B]", "B05[O-Si]", "B05[O-X]",
				"B05[S-S]", "B05[S-P]", "B05[S-F]", "B05[S-Cl]", "B05[S-Br]",
				"B05[S-I]", "B05[S-B]", "B05[S-Si]", "B05[S-X]", "B05[P-P]",
				"B05[P-F]", "B05[P-Cl]", "B05[P-Br]", "B05[P-I]", "B05[P-B]",
				"B05[P-Si]", "B05[P-X]", "B05[F-F]", "B05[F-Cl]", "B05[F-Br]",
				"B05[F-I]", "B05[F-B]", "B05[F-Si]", "B05[F-X]", "B05[Cl-Cl]",
				"B05[Cl-Br]", "B05[Cl-I]", "B05[Cl-B]", "B05[Cl-Si]",
				"B05[Cl-X]", "B05[Br-Br]", "B05[Br-I]", "B05[Br-B]",
				"B05[Br-Si]", "B05[Br-X]", "B05[I-I]", "B05[I-B]", "B05[I-Si]",
				"B05[I-X]", "B05[B-B]", "B05[B-Si]", "B05[B-X]", "B05[Si-Si]",
				"B05[Si-X]", "B05[X-X]", "B06[C-C]", "B06[C-N]", "B06[C-O]",
				"B06[C-S]", "B06[C-P]", "B06[C-F]", "B06[C-Cl]", "B06[C-Br]",
				"B06[C-I]", "B06[C-B]", "B06[C-Si]", "B06[C-X]", "B06[N-N]",
				"B06[N-O]", "B06[N-S]", "B06[N-P]", "B06[N-F]", "B06[N-Cl]",
				"B06[N-Br]", "B06[N-I]", "B06[N-B]", "B06[N-Si]", "B06[N-X]",
				"B06[O-O]", "B06[O-S]", "B06[O-P]", "B06[O-F]", "B06[O-Cl]",
				"B06[O-Br]", "B06[O-I]", "B06[O-B]", "B06[O-Si]", "B06[O-X]",
				"B06[S-S]", "B06[S-P]", "B06[S-F]", "B06[S-Cl]", "B06[S-Br]",
				"B06[S-I]", "B06[S-B]", "B06[S-Si]", "B06[S-X]", "B06[P-P]",
				"B06[P-F]", "B06[P-Cl]", "B06[P-Br]", "B06[P-I]", "B06[P-B]",
				"B06[P-Si]", "B06[P-X]", "B06[F-F]", "B06[F-Cl]", "B06[F-Br]",
				"B06[F-I]", "B06[F-B]", "B06[F-Si]", "B06[F-X]", "B06[Cl-Cl]",
				"B06[Cl-Br]", "B06[Cl-I]", "B06[Cl-B]", "B06[Cl-Si]",
				"B06[Cl-X]", "B06[Br-Br]", "B06[Br-I]", "B06[Br-B]",
				"B06[Br-Si]", "B06[Br-X]", "B06[I-I]", "B06[I-B]", "B06[I-Si]",
				"B06[I-X]", "B06[B-B]", "B06[B-Si]", "B06[B-X]", "B06[Si-Si]",
				"B06[Si-X]", "B06[X-X]", "B07[C-C]", "B07[C-N]", "B07[C-O]",
				"B07[C-S]", "B07[C-P]", "B07[C-F]", "B07[C-Cl]", "B07[C-Br]",
				"B07[C-I]", "B07[C-B]", "B07[C-Si]", "B07[C-X]", "B07[N-N]",
				"B07[N-O]", "B07[N-S]", "B07[N-P]", "B07[N-F]", "B07[N-Cl]",
				"B07[N-Br]", "B07[N-I]", "B07[N-B]", "B07[N-Si]", "B07[N-X]",
				"B07[O-O]", "B07[O-S]", "B07[O-P]", "B07[O-F]", "B07[O-Cl]",
				"B07[O-Br]", "B07[O-I]", "B07[O-B]", "B07[O-Si]", "B07[O-X]",
				"B07[S-S]", "B07[S-P]", "B07[S-F]", "B07[S-Cl]", "B07[S-Br]",
				"B07[S-I]", "B07[S-B]", "B07[S-Si]", "B07[S-X]", "B07[P-P]",
				"B07[P-F]", "B07[P-Cl]", "B07[P-Br]", "B07[P-I]", "B07[P-B]",
				"B07[P-Si]", "B07[P-X]", "B07[F-F]", "B07[F-Cl]", "B07[F-Br]",
				"B07[F-I]", "B07[F-B]", "B07[F-Si]", "B07[F-X]", "B07[Cl-Cl]",
				"B07[Cl-Br]", "B07[Cl-I]", "B07[Cl-B]", "B07[Cl-Si]",
				"B07[Cl-X]", "B07[Br-Br]", "B07[Br-I]", "B07[Br-B]",
				"B07[Br-Si]", "B07[Br-X]", "B07[I-I]", "B07[I-B]", "B07[I-Si]",
				"B07[I-X]", "B07[B-B]", "B07[B-Si]", "B07[B-X]", "B07[Si-Si]",
				"B07[Si-X]", "B07[X-X]", "B08[C-C]", "B08[C-N]", "B08[C-O]",
				"B08[C-S]", "B08[C-P]", "B08[C-F]", "B08[C-Cl]", "B08[C-Br]",
				"B08[C-I]", "B08[C-B]", "B08[C-Si]", "B08[C-X]", "B08[N-N]",
				"B08[N-O]", "B08[N-S]", "B08[N-P]", "B08[N-F]", "B08[N-Cl]",
				"B08[N-Br]", "B08[N-I]", "B08[N-B]", "B08[N-Si]", "B08[N-X]",
				"B08[O-O]", "B08[O-S]", "B08[O-P]", "B08[O-F]", "B08[O-Cl]",
				"B08[O-Br]", "B08[O-I]", "B08[O-B]", "B08[O-Si]", "B08[O-X]",
				"B08[S-S]", "B08[S-P]", "B08[S-F]", "B08[S-Cl]", "B08[S-Br]",
				"B08[S-I]", "B08[S-B]", "B08[S-Si]", "B08[S-X]", "B08[P-P]",
				"B08[P-F]", "B08[P-Cl]", "B08[P-Br]", "B08[P-I]", "B08[P-B]",
				"B08[P-Si]", "B08[P-X]", "B08[F-F]", "B08[F-Cl]", "B08[F-Br]",
				"B08[F-I]", "B08[F-B]", "B08[F-Si]", "B08[F-X]", "B08[Cl-Cl]",
				"B08[Cl-Br]", "B08[Cl-I]", "B08[Cl-B]", "B08[Cl-Si]",
				"B08[Cl-X]", "B08[Br-Br]", "B08[Br-I]", "B08[Br-B]",
				"B08[Br-Si]", "B08[Br-X]", "B08[I-I]", "B08[I-B]", "B08[I-Si]",
				"B08[I-X]", "B08[B-B]", "B08[B-Si]", "B08[B-X]", "B08[Si-Si]",
				"B08[Si-X]", "B08[X-X]", "B09[C-C]", "B09[C-N]", "B09[C-O]",
				"B09[C-S]", "B09[C-P]", "B09[C-F]", "B09[C-Cl]", "B09[C-Br]",
				"B09[C-I]", "B09[C-B]", "B09[C-Si]", "B09[C-X]", "B09[N-N]",
				"B09[N-O]", "B09[N-S]", "B09[N-P]", "B09[N-F]", "B09[N-Cl]",
				"B09[N-Br]", "B09[N-I]", "B09[N-B]", "B09[N-Si]", "B09[N-X]",
				"B09[O-O]", "B09[O-S]", "B09[O-P]", "B09[O-F]", "B09[O-Cl]",
				"B09[O-Br]", "B09[O-I]", "B09[O-B]", "B09[O-Si]", "B09[O-X]",
				"B09[S-S]", "B09[S-P]", "B09[S-F]", "B09[S-Cl]", "B09[S-Br]",
				"B09[S-I]", "B09[S-B]", "B09[S-Si]", "B09[S-X]", "B09[P-P]",
				"B09[P-F]", "B09[P-Cl]", "B09[P-Br]", "B09[P-I]", "B09[P-B]",
				"B09[P-Si]", "B09[P-X]", "B09[F-F]", "B09[F-Cl]", "B09[F-Br]",
				"B09[F-I]", "B09[F-B]", "B09[F-Si]", "B09[F-X]", "B09[Cl-Cl]",
				"B09[Cl-Br]", "B09[Cl-I]", "B09[Cl-B]", "B09[Cl-Si]",
				"B09[Cl-X]", "B09[Br-Br]", "B09[Br-I]", "B09[Br-B]",
				"B09[Br-Si]", "B09[Br-X]", "B09[I-I]", "B09[I-B]", "B09[I-Si]",
				"B09[I-X]", "B09[B-B]", "B09[B-Si]", "B09[B-X]", "B09[Si-Si]",
				"B09[Si-X]", "B09[X-X]", "B10[C-C]", "B10[C-N]", "B10[C-O]",
				"B10[C-S]", "B10[C-P]", "B10[C-F]", "B10[C-Cl]", "B10[C-Br]",
				"B10[C-I]", "B10[C-B]", "B10[C-Si]", "B10[C-X]", "B10[N-N]",
				"B10[N-O]", "B10[N-S]", "B10[N-P]", "B10[N-F]", "B10[N-Cl]",
				"B10[N-Br]", "B10[N-I]", "B10[N-B]", "B10[N-Si]", "B10[N-X]",
				"B10[O-O]", "B10[O-S]", "B10[O-P]", "B10[O-F]", "B10[O-Cl]",
				"B10[O-Br]", "B10[O-I]", "B10[O-B]", "B10[O-Si]", "B10[O-X]",
				"B10[S-S]", "B10[S-P]", "B10[S-F]", "B10[S-Cl]", "B10[S-Br]",
				"B10[S-I]", "B10[S-B]", "B10[S-Si]", "B10[S-X]", "B10[P-P]",
				"B10[P-F]", "B10[P-Cl]", "B10[P-Br]", "B10[P-I]", "B10[P-B]",
				"B10[P-Si]", "B10[P-X]", "B10[F-F]", "B10[F-Cl]", "B10[F-Br]",
				"B10[F-I]", "B10[F-B]", "B10[F-Si]", "B10[F-X]", "B10[Cl-Cl]",
				"B10[Cl-Br]", "B10[Cl-I]", "B10[Cl-B]", "B10[Cl-Si]",
				"B10[Cl-X]", "B10[Br-Br]", "B10[Br-I]", "B10[Br-B]",
				"B10[Br-Si]", "B10[Br-X]", "B10[I-I]", "B10[I-B]", "B10[I-Si]",
				"B10[I-X]", "B10[B-B]", "B10[B-Si]", "B10[B-X]", "B10[Si-Si]",
				"B10[Si-X]", "B10[X-X]", "F01[C-C]", "F01[C-N]", "F01[C-O]",
				"F01[C-S]", "F01[C-P]", "F01[C-F]", "F01[C-Cl]", "F01[C-Br]",
				"F01[C-I]", "F01[C-B]", "F01[C-Si]", "F01[C-X]", "F01[N-N]",
				"F01[N-O]", "F01[N-S]", "F01[N-P]", "F01[N-F]", "F01[N-Cl]",
				"F01[N-Br]", "F01[N-I]", "F01[N-B]", "F01[N-Si]", "F01[N-X]",
				"F01[O-O]", "F01[O-S]", "F01[O-P]", "F01[O-F]", "F01[O-Cl]",
				"F01[O-Br]", "F01[O-I]", "F01[O-B]", "F01[O-Si]", "F01[O-X]",
				"F01[S-S]", "F01[S-P]", "F01[S-F]", "F01[S-Cl]", "F01[S-Br]",
				"F01[S-I]", "F01[S-B]", "F01[S-Si]", "F01[S-X]", "F01[P-P]",
				"F01[P-F]", "F01[P-Cl]", "F01[P-Br]", "F01[P-I]", "F01[P-B]",
				"F01[P-Si]", "F01[P-X]", "F01[F-F]", "F01[F-Cl]", "F01[F-Br]",
				"F01[F-I]", "F01[F-B]", "F01[F-Si]", "F01[F-X]", "F01[Cl-Cl]",
				"F01[Cl-Br]", "F01[Cl-I]", "F01[Cl-B]", "F01[Cl-Si]",
				"F01[Cl-X]", "F01[Br-Br]", "F01[Br-I]", "F01[Br-B]",
				"F01[Br-Si]", "F01[Br-X]", "F01[I-I]", "F01[I-B]", "F01[I-Si]",
				"F01[I-X]", "F01[B-B]", "F01[B-Si]", "F01[B-X]", "F01[Si-Si]",
				"F01[Si-X]", "F01[X-X]", "F02[C-C]", "F02[C-N]", "F02[C-O]",
				"F02[C-S]", "F02[C-P]", "F02[C-F]", "F02[C-Cl]", "F02[C-Br]",
				"F02[C-I]", "F02[C-B]", "F02[C-Si]", "F02[C-X]", "F02[N-N]",
				"F02[N-O]", "F02[N-S]", "F02[N-P]", "F02[N-F]", "F02[N-Cl]",
				"F02[N-Br]", "F02[N-I]", "F02[N-B]", "F02[N-Si]", "F02[N-X]",
				"F02[O-O]", "F02[O-S]", "F02[O-P]", "F02[O-F]", "F02[O-Cl]",
				"F02[O-Br]", "F02[O-I]", "F02[O-B]", "F02[O-Si]", "F02[O-X]",
				"F02[S-S]", "F02[S-P]", "F02[S-F]", "F02[S-Cl]", "F02[S-Br]",
				"F02[S-I]", "F02[S-B]", "F02[S-Si]", "F02[S-X]", "F02[P-P]",
				"F02[P-F]", "F02[P-Cl]", "F02[P-Br]", "F02[P-I]", "F02[P-B]",
				"F02[P-Si]", "F02[P-X]", "F02[F-F]", "F02[F-Cl]", "F02[F-Br]",
				"F02[F-I]", "F02[F-B]", "F02[F-Si]", "F02[F-X]", "F02[Cl-Cl]",
				"F02[Cl-Br]", "F02[Cl-I]", "F02[Cl-B]", "F02[Cl-Si]",
				"F02[Cl-X]", "F02[Br-Br]", "F02[Br-I]", "F02[Br-B]",
				"F02[Br-Si]", "F02[Br-X]", "F02[I-I]", "F02[I-B]", "F02[I-Si]",
				"F02[I-X]", "F02[B-B]", "F02[B-Si]", "F02[B-X]", "F02[Si-Si]",
				"F02[Si-X]", "F02[X-X]", "F03[C-C]", "F03[C-N]", "F03[C-O]",
				"F03[C-S]", "F03[C-P]", "F03[C-F]", "F03[C-Cl]", "F03[C-Br]",
				"F03[C-I]", "F03[C-B]", "F03[C-Si]", "F03[C-X]", "F03[N-N]",
				"F03[N-O]", "F03[N-S]", "F03[N-P]", "F03[N-F]", "F03[N-Cl]",
				"F03[N-Br]", "F03[N-I]", "F03[N-B]", "F03[N-Si]", "F03[N-X]",
				"F03[O-O]", "F03[O-S]", "F03[O-P]", "F03[O-F]", "F03[O-Cl]",
				"F03[O-Br]", "F03[O-I]", "F03[O-B]", "F03[O-Si]", "F03[O-X]",
				"F03[S-S]", "F03[S-P]", "F03[S-F]", "F03[S-Cl]", "F03[S-Br]",
				"F03[S-I]", "F03[S-B]", "F03[S-Si]", "F03[S-X]", "F03[P-P]",
				"F03[P-F]", "F03[P-Cl]", "F03[P-Br]", "F03[P-I]", "F03[P-B]",
				"F03[P-Si]", "F03[P-X]", "F03[F-F]", "F03[F-Cl]", "F03[F-Br]",
				"F03[F-I]", "F03[F-B]", "F03[F-Si]", "F03[F-X]", "F03[Cl-Cl]",
				"F03[Cl-Br]", "F03[Cl-I]", "F03[Cl-B]", "F03[Cl-Si]",
				"F03[Cl-X]", "F03[Br-Br]", "F03[Br-I]", "F03[Br-B]",
				"F03[Br-Si]", "F03[Br-X]", "F03[I-I]", "F03[I-B]", "F03[I-Si]",
				"F03[I-X]", "F03[B-B]", "F03[B-Si]", "F03[B-X]", "F03[Si-Si]",
				"F03[Si-X]", "F03[X-X]", "F04[C-C]", "F04[C-N]", "F04[C-O]",
				"F04[C-S]", "F04[C-P]", "F04[C-F]", "F04[C-Cl]", "F04[C-Br]",
				"F04[C-I]", "F04[C-B]", "F04[C-Si]", "F04[C-X]", "F04[N-N]",
				"F04[N-O]", "F04[N-S]", "F04[N-P]", "F04[N-F]", "F04[N-Cl]",
				"F04[N-Br]", "F04[N-I]", "F04[N-B]", "F04[N-Si]", "F04[N-X]",
				"F04[O-O]", "F04[O-S]", "F04[O-P]", "F04[O-F]", "F04[O-Cl]",
				"F04[O-Br]", "F04[O-I]", "F04[O-B]", "F04[O-Si]", "F04[O-X]",
				"F04[S-S]", "F04[S-P]", "F04[S-F]", "F04[S-Cl]", "F04[S-Br]",
				"F04[S-I]", "F04[S-B]", "F04[S-Si]", "F04[S-X]", "F04[P-P]",
				"F04[P-F]", "F04[P-Cl]", "F04[P-Br]", "F04[P-I]", "F04[P-B]",
				"F04[P-Si]", "F04[P-X]", "F04[F-F]", "F04[F-Cl]", "F04[F-Br]",
				"F04[F-I]", "F04[F-B]", "F04[F-Si]", "F04[F-X]", "F04[Cl-Cl]",
				"F04[Cl-Br]", "F04[Cl-I]", "F04[Cl-B]", "F04[Cl-Si]",
				"F04[Cl-X]", "F04[Br-Br]", "F04[Br-I]", "F04[Br-B]",
				"F04[Br-Si]", "F04[Br-X]", "F04[I-I]", "F04[I-B]", "F04[I-Si]",
				"F04[I-X]", "F04[B-B]", "F04[B-Si]", "F04[B-X]", "F04[Si-Si]",
				"F04[Si-X]", "F04[X-X]", "F05[C-C]", "F05[C-N]", "F05[C-O]",
				"F05[C-S]", "F05[C-P]", "F05[C-F]", "F05[C-Cl]", "F05[C-Br]",
				"F05[C-I]", "F05[C-B]", "F05[C-Si]", "F05[C-X]", "F05[N-N]",
				"F05[N-O]", "F05[N-S]", "F05[N-P]", "F05[N-F]", "F05[N-Cl]",
				"F05[N-Br]", "F05[N-I]", "F05[N-B]", "F05[N-Si]", "F05[N-X]",
				"F05[O-O]", "F05[O-S]", "F05[O-P]", "F05[O-F]", "F05[O-Cl]",
				"F05[O-Br]", "F05[O-I]", "F05[O-B]", "F05[O-Si]", "F05[O-X]",
				"F05[S-S]", "F05[S-P]", "F05[S-F]", "F05[S-Cl]", "F05[S-Br]",
				"F05[S-I]", "F05[S-B]", "F05[S-Si]", "F05[S-X]", "F05[P-P]",
				"F05[P-F]", "F05[P-Cl]", "F05[P-Br]", "F05[P-I]", "F05[P-B]",
				"F05[P-Si]", "F05[P-X]", "F05[F-F]", "F05[F-Cl]", "F05[F-Br]",
				"F05[F-I]", "F05[F-B]", "F05[F-Si]", "F05[F-X]", "F05[Cl-Cl]",
				"F05[Cl-Br]", "F05[Cl-I]", "F05[Cl-B]", "F05[Cl-Si]",
				"F05[Cl-X]", "F05[Br-Br]", "F05[Br-I]", "F05[Br-B]",
				"F05[Br-Si]", "F05[Br-X]", "F05[I-I]", "F05[I-B]", "F05[I-Si]",
				"F05[I-X]", "F05[B-B]", "F05[B-Si]", "F05[B-X]", "F05[Si-Si]",
				"F05[Si-X]", "F05[X-X]", "F06[C-C]", "F06[C-N]", "F06[C-O]",
				"F06[C-S]", "F06[C-P]", "F06[C-F]", "F06[C-Cl]", "F06[C-Br]",
				"F06[C-I]", "F06[C-B]", "F06[C-Si]", "F06[C-X]", "F06[N-N]",
				"F06[N-O]", "F06[N-S]", "F06[N-P]", "F06[N-F]", "F06[N-Cl]",
				"F06[N-Br]", "F06[N-I]", "F06[N-B]", "F06[N-Si]", "F06[N-X]",
				"F06[O-O]", "F06[O-S]", "F06[O-P]", "F06[O-F]", "F06[O-Cl]",
				"F06[O-Br]", "F06[O-I]", "F06[O-B]", "F06[O-Si]", "F06[O-X]",
				"F06[S-S]", "F06[S-P]", "F06[S-F]", "F06[S-Cl]", "F06[S-Br]",
				"F06[S-I]", "F06[S-B]", "F06[S-Si]", "F06[S-X]", "F06[P-P]",
				"F06[P-F]", "F06[P-Cl]", "F06[P-Br]", "F06[P-I]", "F06[P-B]",
				"F06[P-Si]", "F06[P-X]", "F06[F-F]", "F06[F-Cl]", "F06[F-Br]",
				"F06[F-I]", "F06[F-B]", "F06[F-Si]", "F06[F-X]", "F06[Cl-Cl]",
				"F06[Cl-Br]", "F06[Cl-I]", "F06[Cl-B]", "F06[Cl-Si]",
				"F06[Cl-X]", "F06[Br-Br]", "F06[Br-I]", "F06[Br-B]",
				"F06[Br-Si]", "F06[Br-X]", "F06[I-I]", "F06[I-B]", "F06[I-Si]",
				"F06[I-X]", "F06[B-B]", "F06[B-Si]", "F06[B-X]", "F06[Si-Si]",
				"F06[Si-X]", "F06[X-X]", "F07[C-C]", "F07[C-N]", "F07[C-O]",
				"F07[C-S]", "F07[C-P]", "F07[C-F]", "F07[C-Cl]", "F07[C-Br]",
				"F07[C-I]", "F07[C-B]", "F07[C-Si]", "F07[C-X]", "F07[N-N]",
				"F07[N-O]", "F07[N-S]", "F07[N-P]", "F07[N-F]", "F07[N-Cl]",
				"F07[N-Br]", "F07[N-I]", "F07[N-B]", "F07[N-Si]", "F07[N-X]",
				"F07[O-O]", "F07[O-S]", "F07[O-P]", "F07[O-F]", "F07[O-Cl]",
				"F07[O-Br]", "F07[O-I]", "F07[O-B]", "F07[O-Si]", "F07[O-X]",
				"F07[S-S]", "F07[S-P]", "F07[S-F]", "F07[S-Cl]", "F07[S-Br]",
				"F07[S-I]", "F07[S-B]", "F07[S-Si]", "F07[S-X]", "F07[P-P]",
				"F07[P-F]", "F07[P-Cl]", "F07[P-Br]", "F07[P-I]", "F07[P-B]",
				"F07[P-Si]", "F07[P-X]", "F07[F-F]", "F07[F-Cl]", "F07[F-Br]",
				"F07[F-I]", "F07[F-B]", "F07[F-Si]", "F07[F-X]", "F07[Cl-Cl]",
				"F07[Cl-Br]", "F07[Cl-I]", "F07[Cl-B]", "F07[Cl-Si]",
				"F07[Cl-X]", "F07[Br-Br]", "F07[Br-I]", "F07[Br-B]",
				"F07[Br-Si]", "F07[Br-X]", "F07[I-I]", "F07[I-B]", "F07[I-Si]",
				"F07[I-X]", "F07[B-B]", "F07[B-Si]", "F07[B-X]", "F07[Si-Si]",
				"F07[Si-X]", "F07[X-X]", "F08[C-C]", "F08[C-N]", "F08[C-O]",
				"F08[C-S]", "F08[C-P]", "F08[C-F]", "F08[C-Cl]", "F08[C-Br]",
				"F08[C-I]", "F08[C-B]", "F08[C-Si]", "F08[C-X]", "F08[N-N]",
				"F08[N-O]", "F08[N-S]", "F08[N-P]", "F08[N-F]", "F08[N-Cl]",
				"F08[N-Br]", "F08[N-I]", "F08[N-B]", "F08[N-Si]", "F08[N-X]",
				"F08[O-O]", "F08[O-S]", "F08[O-P]", "F08[O-F]", "F08[O-Cl]",
				"F08[O-Br]", "F08[O-I]", "F08[O-B]", "F08[O-Si]", "F08[O-X]",
				"F08[S-S]", "F08[S-P]", "F08[S-F]", "F08[S-Cl]", "F08[S-Br]",
				"F08[S-I]", "F08[S-B]", "F08[S-Si]", "F08[S-X]", "F08[P-P]",
				"F08[P-F]", "F08[P-Cl]", "F08[P-Br]", "F08[P-I]", "F08[P-B]",
				"F08[P-Si]", "F08[P-X]", "F08[F-F]", "F08[F-Cl]", "F08[F-Br]",
				"F08[F-I]", "F08[F-B]", "F08[F-Si]", "F08[F-X]", "F08[Cl-Cl]",
				"F08[Cl-Br]", "F08[Cl-I]", "F08[Cl-B]", "F08[Cl-Si]",
				"F08[Cl-X]", "F08[Br-Br]", "F08[Br-I]", "F08[Br-B]",
				"F08[Br-Si]", "F08[Br-X]", "F08[I-I]", "F08[I-B]", "F08[I-Si]",
				"F08[I-X]", "F08[B-B]", "F08[B-Si]", "F08[B-X]", "F08[Si-Si]",
				"F08[Si-X]", "F08[X-X]", "F09[C-C]", "F09[C-N]", "F09[C-O]",
				"F09[C-S]", "F09[C-P]", "F09[C-F]", "F09[C-Cl]", "F09[C-Br]",
				"F09[C-I]", "F09[C-B]", "F09[C-Si]", "F09[C-X]", "F09[N-N]",
				"F09[N-O]", "F09[N-S]", "F09[N-P]", "F09[N-F]", "F09[N-Cl]",
				"F09[N-Br]", "F09[N-I]", "F09[N-B]", "F09[N-Si]", "F09[N-X]",
				"F09[O-O]", "F09[O-S]", "F09[O-P]", "F09[O-F]", "F09[O-Cl]",
				"F09[O-Br]", "F09[O-I]", "F09[O-B]", "F09[O-Si]", "F09[O-X]",
				"F09[S-S]", "F09[S-P]", "F09[S-F]", "F09[S-Cl]", "F09[S-Br]",
				"F09[S-I]", "F09[S-B]", "F09[S-Si]", "F09[S-X]", "F09[P-P]",
				"F09[P-F]", "F09[P-Cl]", "F09[P-Br]", "F09[P-I]", "F09[P-B]",
				"F09[P-Si]", "F09[P-X]", "F09[F-F]", "F09[F-Cl]", "F09[F-Br]",
				"F09[F-I]", "F09[F-B]", "F09[F-Si]", "F09[F-X]", "F09[Cl-Cl]",
				"F09[Cl-Br]", "F09[Cl-I]", "F09[Cl-B]", "F09[Cl-Si]",
				"F09[Cl-X]", "F09[Br-Br]", "F09[Br-I]", "F09[Br-B]",
				"F09[Br-Si]", "F09[Br-X]", "F09[I-I]", "F09[I-B]", "F09[I-Si]",
				"F09[I-X]", "F09[B-B]", "F09[B-Si]", "F09[B-X]", "F09[Si-Si]",
				"F09[Si-X]", "F09[X-X]", "F10[C-C]", "F10[C-N]", "F10[C-O]",
				"F10[C-S]", "F10[C-P]", "F10[C-F]", "F10[C-Cl]", "F10[C-Br]",
				"F10[C-I]", "F10[C-B]", "F10[C-Si]", "F10[C-X]", "F10[N-N]",
				"F10[N-O]", "F10[N-S]", "F10[N-P]", "F10[N-F]", "F10[N-Cl]",
				"F10[N-Br]", "F10[N-I]", "F10[N-B]", "F10[N-Si]", "F10[N-X]",
				"F10[O-O]", "F10[O-S]", "F10[O-P]", "F10[O-F]", "F10[O-Cl]",
				"F10[O-Br]", "F10[O-I]", "F10[O-B]", "F10[O-Si]", "F10[O-X]",
				"F10[S-S]", "F10[S-P]", "F10[S-F]", "F10[S-Cl]", "F10[S-Br]",
				"F10[S-I]", "F10[S-B]", "F10[S-Si]", "F10[S-X]", "F10[P-P]",
				"F10[P-F]", "F10[P-Cl]", "F10[P-Br]", "F10[P-I]", "F10[P-B]",
				"F10[P-Si]", "F10[P-X]", "F10[F-F]", "F10[F-Cl]", "F10[F-Br]",
				"F10[F-I]", "F10[F-B]", "F10[F-Si]", "F10[F-X]", "F10[Cl-Cl]",
				"F10[Cl-Br]", "F10[Cl-I]", "F10[Cl-B]", "F10[Cl-Si]",
				"F10[Cl-X]", "F10[Br-Br]", "F10[Br-I]", "F10[Br-B]",
				"F10[Br-Si]", "F10[Br-X]", "F10[I-I]", "F10[I-B]", "F10[I-Si]",
				"F10[I-X]", "F10[B-B]", "F10[B-Si]", "F10[B-X]", "F10[Si-Si]",
				"F10[Si-X]", "F10[X-X]" };
		String needed[] = { "MW", "Ss", "nR10", "ZM2", "ZM2V", "Xt", "JhetZ",
				"DECC", "T(O..O)", "piPC08", "X2", "X0Av", "X4Av", "X1sol",
				"IVDE", "BIC0", "SIC2", "BIC2", "SIC3", "BIC3", "ATS5m",
				"ATS4e", "ATS7e", "MATS7m", "MATS8m", "MATS8e", "GATS4m",
				"GATS3v", "GATS8v", "GATS5e", "EEig04x", "EEig06x", "EEig07x",
				"EEig11x", "EEig12x", "EEig15x", "EEig08d", "EEig06r",
				"EEig12r", "EEig14r", "EEig15r", "ESpm01d", "ESpm03d", "JGI5",
				"JGI8", "SEigZ", "SEigv", "SEige", "VEA1", "VRA1", "nCs",
				"nCrs", "nCconj", "nR=Cs", "nRCOOH", "nArCONH2", "nROCON",
				"nRCO", "nC=O(O)2", "nC(=N)N2", "nN(CO)2", "nOxolanes",
				"nHAcc", "C-001", "C-006", "C-008", "C-026", "C-030", "C-033",
				"C-035", "C-040", "H-052", "H-053", "H-054", "O-056", "O-058",
				"N-069", "ALOGP2", "BLTD48", "BLTA96", "B01[O-O]", "B01[O-P]",
				"B02[C-N]", "B02[O-O]", "B02[P-P]", "B03[C-S]", "B04[O-O]",
				"B04[O-S]", "B05[C-N]", "B05[N-P]", "B06[N-O]", "B07[C-N]",
				"B07[N-O]", "B07[O-O]", "B08[C-O]", "B08[C-S]", "B09[C-N]",
				"B09[N-O]", "B10[O-P]", "F01[C-C]", "F01[C-N]", "F01[N-O]",
				"F02[N-O]", "F02[O-O]", "F02[S-P]", "F03[N-O]", "F03[O-O]",
				"F05[C-N]", "F05[O-P]", "F06[C-N]", "F07[C-O]", "F08[C-C]",
				"F08[C-O]", "F08[O-P]", "F08[P-P]", "F09[C-N]", "F09[C-P]",
				"F10[N-O]", "F10[O-O]" };
		int indices[] = new int[needed.length];
		int i, j;
		for (i = 0; i < needed.length; i++) {
			for (j = 0; j < descriptors.length; j++) {
				if (needed[i].equals(descriptors[j])) {
					indices[i] = j;
					break;
				}
			}
		}
		String mol[] = { "48.12", "8.02", "3.28", "5.84", "4.17", "5.22",
				"0.55", "0.97", "0.70", "2.61", "6", "2", "5", "1", "0",
				"1.00", "0.000", "0", "0", "0", "0.000", "0", "0", "0", "4",
				"1", "0", "0", "0", "1", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "2.000",
				"1.309", "1.000", "0.556", "0.000", "0.000", "1.000", "1.000",
				"0.000", "4.000", "0.000", "0.000", "0.000", "1.000", "0.707",
				"4.000", "2.864", "1.000", "0.556", "0.000", "0.000", "1.000",
				"1.000", "1.000", "1.000", "1.000", "-0.602", "1.000", "0.000",
				"1.000", "1.000", "1.000", "1.000", "1.000", "1.000", "1.000",
				"0.375", "0.374", "0.919", "0.929", "0.607", "1.000", "2.667",
				"2.670", "1.088", "1.076", "1.648", "0.306", "0.306", "0.611",
				"0.073", "2.000", "2.351", "1.000", "1.450", "1.175", "4.025",
				"0.000", "0.000", "0.000", "0.000", "0.000", "2.000", "2.000",
				"1.000", "0.000", "0.000", "1.000", "0.000", "0.000", "4.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "1.000", "1.099",
				"1.099", "1.099", "1.099", "1.099", "1.099", "1.099", "1.099",
				"1.099", "12.888", "2.000", "2.000", "0.000", "2.000", "0.000",
				"2.000", "0.000", "2.000", "0.000", "2.000", "1.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.693", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "2.693", "2.693", "1.000",
				"0.000", "3.000", "3.000", "2.000", "1.000", "0.000", "0.000",
				"0.000", "0.000", "1.000", "1.000", "0.000", "0.000", "0.000",
				"0.000", "2.342", "1.342", "0.000", "0.000", "0.000", "0.000",
				"1.171", "1.342", "0.000", "0.000", "0.000", "0.000", "2.500",
				"1.500", "0.000", "0.000", "0.000", "0.000", "11.000", "1.000",
				"1.000", "15.510", "7.510", "1.252", "0.000", "0.000", "0.000",
				"1.000", "0.000", "0.000", "0.000", "1.000", "1.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "1.252", "7.510", "0.484",
				"1.333", "0.000", "1.792", "10.755", "0.693", "0.792", "0.000",
				"1.792", "10.755", "0.693", "0.792", "0.000", "0.811", "4.868",
				"0.314", "1.774", "0.000", "-999", "-999", "-999", "-999",
				"-999", "-999", "-999", "-999", "-999", "-999", "1.300",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.736", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.731", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.974", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "-1.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "-1.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "-1.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "-1.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"1.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "1.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "1.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "1.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "1.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"2.950", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.700", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.693", "0.693", "0.693", "0.693",
				"0.693", "0.693", "0.693", "0.693", "0.693", "0.693", "0.693",
				"0.693", "0.693", "0.693", "0.693", "1.374", "2.272", "3.284",
				"4.340", "5.413", "6.492", "7.573", "8.655", "9.736", "10.818",
				"11.900", "12.982", "14.063", "15.145", "16.227", "0.531",
				"0.399", "0.295", "0.215", "0.155", "0.111", "0.079", "0.056",
				"0.040", "0.028", "0.020", "0.014", "0.010", "0.007", "0.005",
				"3.553", "1.897", "0.083", "0.083", "0.000", "0.000", "0.000",
				"0.000", "1.365", "0.244", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "2.879", "1.466", "0.298", "0.298", "0.000",
				"0.000", "0.000", "0.000", "1.355", "0.302", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "3.070", "1.822", "0.941",
				"0.941", "0.151", "0.000", "0.000", "0.000", "1.081", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "3.071",
				"1.724", "0.380", "0.380", "0.000", "0.000", "0.000", "0.000",
				"1.260", "0.124", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "1.000", "0.801", "0.801", "0.960", "0.965", "0.834",
				"0.625", "0.626", "0.081", "0.071", "0.393", "0.976", "0.976",
				"1.840", "1.859", "1.276", "1.414", "0.707", "0.816", "0.408",
				"1.414", "0.707", "0.816", "0.408", "1.330", "0.665", "0.850",
				"0.425", "1.329", "0.665", "0.850", "0.425", "1.414", "0.707",
				"0.817", "0.408", "1.414", "0.707", "0.817", "0.408", "1.397",
				"0.698", "0.823", "0.412", "1", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "1", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "-999", "0", "0", "0", "0", "1", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "-999",
				"0", "3", "0", "0", "1", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "-999", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "-999",
				"1", "0", "0", "0", "0", "0", "0", "-999", "-999", "0", "0",
				"0", "0", "0", "0", "0.000", "1.262", "14.388", "0.000",
				"38.800", "0.203", "0.041", "0.613", "0.376", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "0.000", "0.000", "0.000", "0.000", "0.000", "0.000",
				"0.000", "-1.56", "-1.51", "-1.43", "0", "0", "0", "1", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "1", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0",
				"0", "0", "0", "0", "0", "0", "0" };
		System.out.println(indices.length + "\t" + needed.length + "\t"
				+ descriptors.length);
		for (i = 0; i < indices.length; i++) {
			System.out.println(descriptors[indices[i]] + ':' + mol[indices[i]]);
		}
	}

	public void writeReactionsInGibbsPredFormat(ListOf<Reaction> rlist) {
		// String sIds[] =
		// "q8h2	mqn8	CPD_8538	trnaglu	CPD_8537	glutrna	s2dmmq8	mql8"
		// .split("	");
		// Set<Reaction> rlist = new HashSet<Reaction>();
		// for (String string : sIds) {
		// for (Reaction r:m.getListOfReactions()) {
		// if (r.involves(m.getSpecies(string))) {
		// rlist.add(r);
		// }
		// }
		// }
		// R00762 1 C00354+1 C00001=1 C00085+1 C00668
		for (Reaction r : rlist) {
			System.out.print(r.getId() + "\t");
			boolean first = true;
			for (SpeciesReference specRef : r.getListOfReactants()) {
				if (!first)
					System.out.print("+");
				else
					first = false;
				System.out.print(specRef.getStoichiometry() + " ");
				System.out.print(specRef.getSpeciesInstance().filterCVTerms(
						Qualifier.BQB_IS, "kegg").get(0).substring(
						"urn:miriam:kegg.compound:".length()));
			}
			System.out.print("=");
			first = true;
			for (SpeciesReference specRef : r.getListOfProducts()) {
				if (!first)
					System.out.print("+");
				else
					first = false;
				System.out.print(specRef.getStoichiometry() + " ");
				System.out.print(specRef.getSpeciesInstance().filterCVTerms(
						Qualifier.BQB_IS, "kegg").get(0).substring(
						"urn:miriam:kegg.compound:".length()));
			}
			System.out.println();
		}
	}

	/**
	 * 
	 * @param m
	 */
	public void identifyUnconnectedSpecies(Model m) {
		for (Species s : m.getListOfSpecies()) {
			boolean involved = false;
			for (Reaction r : m.getListOfReactions()) {
				if (r.involves(s)) {
					involved = true;
					break;
				}
			}
			if (!involved)
				System.out.printf(
						"Species %s is not involved in any reactions.\n", s
								.getId());
		}
	}

  public void identifyIdenticalReaction(Model m) {
    for (int i = 0; i < m.getNumReactions(); i++) {
      Reaction r1 = m.getReaction(i);
      for (int j = i + 1; j < m.getNumReactions(); j++) {
        Reaction r2 = m.getReaction(j);
        boolean identical = r1.getNumReactants() == r2.getNumReactants()
            && r1.getNumProducts() == r2.getNumProducts();
        if (!identical)
          continue;
        for (SpeciesReference specRef : r1.getListOfReactants()) {
          identical &= r2.getListOfReactants().contains(specRef.getSpeciesInstance());
        }
        for (SpeciesReference specRef : r1.getListOfProducts()) {
          identical &= r2.getListOfProducts().contains(specRef.getSpeciesInstance());
        }
        if (identical)
          System.out.printf("Identical reactions:\t%s\t%s\n", r1.getId(), r2
              .getId());
      }
    }
  }

	/**
	 * 
	 * @param m
	 */
	public void identifyReactionsWithIdenticalKEGGAnnotation(Model m) {
		int i = 0, j = 0;
		Hashtable<Integer, Set<String>> reactionMap = new Hashtable<Integer, Set<String>>();
		for (Reaction r : m.getListOfReactions()) {
			for (String kegg : r.filterCVTerms(Qualifier.BQB_IS, "kegg")) {
				Integer id = Integer.parseInt(kegg.substring(kegg
						.lastIndexOf(':') + 2));
				if (!reactionMap.containsKey(id))
					reactionMap.put(id, new HashSet<String>());
				Set<String> s = reactionMap.get(id);
				s.add(r.getId());
			}
		}
		for (Integer key : reactionMap.keySet()) {
			Set<String> s = reactionMap.get(key);
			if (s.size() > 1) {
				System.out.printf("Reactions with KEGG id %s:\t%s\n", key, s);
				String ids[] = new String[s.size()];
				boolean notes[] = new boolean[s.size()];
				int cvterms[] = new int[s.size()];
				int maxIdx = 0;
				j = 0;
				for (String rid : s) {
					Reaction r = m.getReaction(rid);
					ids[j] = rid;
					notes[j] = r.isSetNotes();
					for (CVTerm term : r.filterCVTerms(Qualifier.BQB_IS))
						cvterms[j] += term.getNumResources();
					if (cvterms[j] > cvterms[maxIdx])
						maxIdx = j;
					if (r.isSetNotes())
						System.out.printf("%s\tnotes\n", rid);
					if (cvterms[j] > 1)
						System.out
								.printf("%s\tcvterms:\t%d\n", rid, cvterms[j]);
					j++;
				}
				i++;
			}
		}
		System.out.printf("Number of reactions with identical KEGG ids:\t%d\n",
				i);
	}

	/**
	 * 
	 * @param keggIDs
	 * @return
	 * @throws IOException
	 */
	private CVTerm identifyKEGGReactionIds(String... keggIDs)
			throws IOException {
		LinkedList<String> reactionIDs[] = new LinkedList[keggIDs.length];
		int i = 0;
		for (String keggID : keggIDs) {

			// TODO: Fraud!!
			if (keggID.equals("urn:miriam:kegg.compound:C01967"))
				keggID = "urn:miriam:kegg.compound:C00399";

			if (keggID.equals("urn:miriam:kegg.compound:C00080"))
				continue;

			reactionIDs[i] = new LinkedList<String>();
			for (String rID : kegg.getReactionsByCompound(keggID))
				reactionIDs[i].add(rID);
			i++;
		}
		for (i = reactionIDs.length - 2; i >= 0; i--)
			reactionIDs[i].retainAll(reactionIDs[i + 1]);
		CVTerm cvt = new CVTerm();
		cvt.setQualifierType(CVTerm.Type.BIOLOGICAL_QUALIFIER);
		cvt.setBiologicalQualifierType(Qualifier.BQB_IS);
		for (String resource : reactionIDs[0])
			cvt.addResource(resource);
		return cvt;
	}

	/**
	 * 
	 * @param r
	 * @return
	 */
	private String[] getKEGGIDsOfReactantsAndProducts(Reaction r) {
		List<String> keggIDs = new LinkedList<String>();
		for (SpeciesReference specRef : r.getListOfReactants())
			keggIDs.addAll(getKeggsIDs(specRef.getSpeciesInstance(),
					Qualifier.BQB_IS));
		for (SpeciesReference specRef : r.getListOfProducts())
			keggIDs.addAll(getKeggsIDs(specRef.getSpeciesInstance(),
					Qualifier.BQB_IS));
		return keggIDs.toArray(new String[] {});
	}

	/**
	 * 
	 * @param m
	 */
	public void identifyKEGGReactionIDs(Model m) {
		for (Reaction r : m.getListOfReactions())
			if (r.getNumCVTerms() == 0)
				try {
					CVTerm ct = identifyKEGGids(r);
					if (ct.isSetBiologicalQualifierType() && ct.isSetTypeQualifier()
							&& ct.getNumResources() > 0) {
						r.addCVTerm(ct);
						System.out.printf("%s: %s\n", r.getId(), ct.toString());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
	}

	/**
	 * 
	 * @param listOfSpecRef
	 * @param ids
	 */
	public void printCVTermsOf(ListOf<SpeciesReference> listOfSpecRef,
			boolean ids) {
		int i = 0, j;
		String keggID;
		final String cpdPrefix = "urn:miriam:kegg.compound:";
		for (SpeciesReference specRef : listOfSpecRef) {
			if (i > 0)
				System.out.print(" + ");
			if (specRef.getStoichiometry() != 1)
				System.out.printf(" %f ", specRef.getStoichiometry());
			if (ids)
				keggID = specRef.getSpecies();
			else {
				keggID = "?";
				for (CVTerm term : specRef.getSpeciesInstance().getCVTerms()) {
					for (j = 0; j < term.getNumResources()
							&& keggID.equals("?"); j++)
						if (term.getResourceURI(j).startsWith(cpdPrefix)) {
							keggID = term.getResourceURI(j).substring(
									cpdPrefix.length());
						}
					if (keggID.length() > 1)
						break;
				}
			}
			System.out.printf("%s", keggID);
			i++;
		}
	}

	/**
	 * 
	 * @param model
	 */
	public void printNamesOfReactionsWithoutAnnotation(Model model) {
		int i = 0;
		for (Reaction r : model.getListOfReactions()) {
			if (r.getNumCVTerms() == 0 && r.isSetName())
				System.out.printf("%d.\treaction id: %s\tname: %s\n", (++i), r
						.getId(), r.getName());
		}
	}

	public void printReactionAnnotation(Reaction r) {
		System.out.printf("%s:\n", r.getId());
		printCVTermsOf(r.getListOfReactants(), false);
		System.out.print(r.getReversible() ? " <=> " : " -> ");
		printCVTermsOf(r.getListOfProducts(), false);
		System.out.print("\n");
		printCVTermsOf(r.getListOfReactants(), true);
		System.out.print(r.getReversible() ? " <=> " : " -> ");
		printCVTermsOf(r.getListOfProducts(), true);
		System.out.println();
	}
}
