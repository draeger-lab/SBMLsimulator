/*
 * Copyright (C) 2009 ZBIT, University of Tübingen, Andreas Dräger
 */
package org.sbml.simulator.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.biojava.bio.seq.io.ParseException;
import org.biojava.ontology.Ontology;
import org.biojava.ontology.Term;
import org.biojava.ontology.Triple;
import org.biojava.ontology.io.OboParser;

/**
 * @author Andreas Dr&auml;ger <a
 *         href="mailto:andreas.draeger@uni-tuebingen.de">
 *         andreas.draeger@uni-tuebingen.de</a>
 * 
 */
public class OBO2GraphViz {

	Ontology ontology;
	List<String> arcs;
	Set<Term> nodes;
	private List<Term> exclude;

	/**
	 * 
	 * @param oboFileName
	 * @param ontoName
	 * @param ontoDescription
	 * @param root
	 * @param exclude
	 *            Term to be excluded
	 * @throws ParseException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public OBO2GraphViz(String oboFileName, String ontoName,
			String ontoDescription, String root, String... exclude)
			throws ParseException, FileNotFoundException, IOException {
		OboParser parser = new OboParser();
		ontology = parser.parseOBO(new BufferedReader(new FileReader(
				oboFileName)), ontoName, ontoDescription);
		arcs = new Vector<String>();
		nodes = new HashSet<Term>();
		createGraph(root, exclude);
	}

	private void createGraph(String root, String... exclude) {
		if (exclude == null) {
			this.exclude = null;
		} else {
			this.exclude = new LinkedList<Term>();
			for (String e : exclude) {
				this.exclude.add(ontology.getTerm(e));
			}
		}
		traverse(ontology.getTerm(root));
		System.out.print("digraph ");
		System.out.print(ontology.getName());
		System.out.println(" {");
		for (Term t : nodes) {
			System.out.print("  ");
			System.out.print(t.getName().replace(":", ""));
			System.out.println(" [label=\""
					+ lineBreaks(t.getDescription(), 18) + "\"];");
		}
		for (String arc : arcs) {
			System.out.print("  ");
			System.out.println(arc);
		}
		System.out.println('}');

	}

	/**
	 * Inserts line breaks within the given string.
	 * 
	 * @param orig
	 * @param length
	 * @return
	 */
	private String lineBreaks(String orig, int length) {
		StringBuffer out = new StringBuffer();
		// Symol \u00e4 is a German umlaut a, a letter that will
		// normally not occur in our original Strings.
		String tmp = orig.replace("\\,", ",").replace(" ", " \u00e4");
		tmp = tmp.replace("-", "-\u00e4");
		String parts[] = tmp.contains("\u00e4") ? tmp.split("\u00e4")
				: new String[] { orig };
		for (int i = 0, curr = 0; i < parts.length; i++) {
			String part = parts[i];
			if ((part.length() + curr >= length)
					|| (i < parts.length - 1 && part.length()
							+ parts[i + 1].length() + curr >= length)) {
				out.append(part.trim());
				out.append("\\n");
				curr = 0;
			} else {
				out.append(part);
				curr += part.length();
			}
		}
		return out.toString();
	}

	private void traverse(Term subject) {
		Set<Triple> triples = ontology.getTriples(null, subject, null);
		String arc;
		for (Triple triple : triples) {
			boolean child = false;
			if (exclude != null) {
				for (Term t : exclude) {
					child |= isChildOf(triple.getSubject(), t);
				}
			}
			if (!child) {
				nodes.add(triple.getSubject());
				nodes.add(triple.getObject());
				arc = triple.toString().replace(
						triple.getPredicate().getName(), "->").replace(":", "");
				if (!arcs.contains(arc))
					arcs.add(arc);
				traverse(triple.getSubject());
			}
		}
	}

	/**
	 * Traverses the systems biology ontology starting at Term subject until
	 * either the root (SBO:0000000) or the Term object is reached.
	 * 
	 * @param subject
	 *            Child
	 * @param object
	 *            Parent
	 * @return true if subject is a child of object.
	 */
	private boolean isChildOf(Term subject, Term object) {
		// if (subject.equals(object))
		// return true;
		Set<Triple> relations = ontology.getTriples(subject, null, null);
		for (Triple triple : relations) {
			if (triple.getObject().equals(object))
				return true;
			if (isChildOf(triple.getObject(), object))
				return true;
		}
		return false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String exclude[] = new String[args.length - 4];
			for (int i = 4; i < args.length; i++)
				exclude[i - 4] = args[i];
			new OBO2GraphViz(args[0], args[1], args[2], args[3], exclude);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
