/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.CapabilityQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.osgi.framework.InvalidSyntaxException;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.core.PBSolverCP;
import org.sat4j.pb.orders.VarOrderHeapObjective;
import org.sat4j.pb.reader.OPBEclipseReader2007;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.*;

/**
 * This class is the interface between SAT4J and the planner. It produces a
 * boolean satisfiability problem, invokes the solver, and converts the solver result
 * back into information understandable by the planner.
 */
public class Projector {
	private static boolean DEBUG = false;
	private IQueryable picker;

	private Map variables; //key IU, value corresponding variable in the problem
	private Map noopVariables; //key IU, value corresponding no optionality variable in the problem, 
	private List abstractVariables;

	private TwoTierMap slice; //The IUs that have been considered to be part of the problem

	private Dictionary selectionContext;

	private int varCount = 1;

	private ArrayList constraints;
	private ArrayList dependencies;
	private ArrayList tautologies;
	private StringBuffer objective;
	private StringBuffer explanation = new StringBuffer("explain: "); //$NON-NLS-1$
	private Collection solution;

	private File problemFile;
	private MultiStatus result;

	public Projector(IQueryable q, Dictionary context) {
		picker = q;
		variables = new HashMap();
		noopVariables = new HashMap();
		slice = new TwoTierMap();
		constraints = new ArrayList();
		tautologies = new ArrayList();
		dependencies = new ArrayList();
		selectionContext = context;
		abstractVariables = new ArrayList();
		result = new MultiStatus(DirectorActivator.PI_DIRECTOR, IStatus.OK, "Problems resolving provisioning plan.", null);
	}

	public void encode(IInstallableUnit[] ius, IProgressMonitor monitor) {
		try {
			long start = 0;
			if (DEBUG) {
				start = System.currentTimeMillis();
				System.out.println("Start projection: " + start); //$NON-NLS-1$
			}

			Iterator iusToEncode = picker.query(InstallableUnitQuery.ANY, new Collector(), null).iterator();
			while (iusToEncode.hasNext()) {
				processIU((IInstallableUnit) iusToEncode.next());
			}
			createConstraintsForSingleton();
			for (int i = 0; i < ius.length; i++) {
				createMustHaves(ius[i]);
			}
			createOptimizationFunction();
			persist();
			if (DEBUG) {
				long stop = System.currentTimeMillis();
				System.out.println("Projection complete: " + (stop - start)); //$NON-NLS-1$
			}
		} catch (IllegalStateException e) {
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, e.getMessage(), e));
		}
	}

	//Create an optimization function favoring the highest version of each IU  
	private void createOptimizationFunction() {
		objective = new StringBuffer("min:"); //$NON-NLS-1$
		Set s = slice.entrySet();

		//Add the abstract variables
		for (Iterator iterator = abstractVariables.iterator(); iterator.hasNext();) {
			objective.append(" -2 ").append((String) iterator.next()); //$NON-NLS-1$
		}

		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			HashMap conflictingEntries = (HashMap) entry.getValue();
			if (conflictingEntries.size() <= 1) {
				objective.append(" 1 ").append(getVariable((IInstallableUnit) conflictingEntries.values().iterator().next())); //$NON-NLS-1$
				continue;
			}
			List toSort = new ArrayList(conflictingEntries.values());
			Collections.sort(toSort);
			double weight = Math.pow(10, toSort.size() - 1); //TODO We could start at toSort -1, but be careful
			int count = toSort.size();
			for (Iterator iterator2 = toSort.iterator(); iterator2.hasNext();) {
				count--;
				objective.append(' ').append((int) weight).append(' ').append(getVariable((IInstallableUnit) iterator2.next()));
				weight = weight / 10;
			}
		}
		objective.append(" ;"); //$NON-NLS-1$
	}

	private void createMustHaves(IInstallableUnit iu) {
		tautologies.add(" +1 " + getVariable(iu) + " = 1;"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void createNegation(IInstallableUnit iu) {
		tautologies.add(" +1" + getVariable(iu) + " = 0;"); //$NON-NLS-1$//$NON-NLS-2$
	}

	// Check whether the requirement is applicable
	private boolean isApplicable(RequiredCapability req) {
		String filter = req.getFilter();
		if (filter == null)
			return true;
		try {
			return DirectorActivator.context.createFilter(filter).match(selectionContext);
		} catch (InvalidSyntaxException e) {
			return false;
		}
	}

	//Write the problem generated into a temporary file
	private void persist() {
		try {
			problemFile = File.createTempFile("p2Encoding", ".opb"); //$NON-NLS-1$//$NON-NLS-2$
			BufferedWriter w = new BufferedWriter(new FileWriter(problemFile));
			int clauseCount = tautologies.size() + dependencies.size() + constraints.size();

			w.write("* #variable= " + varCount + " #constraint= " + clauseCount + "  "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			w.newLine();
			w.write("*"); //$NON-NLS-1$
			w.newLine();

			if (clauseCount == 0) {
				w.close();
				return;
			}
			w.write(objective.toString());
			w.newLine();
			w.newLine();

			w.write(explanation + " ;"); //$NON-NLS-1$
			w.newLine();
			w.newLine();

			for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
				w.write((String) iterator.next());
				w.newLine();
			}
			for (Iterator iterator = constraints.iterator(); iterator.hasNext();) {
				w.write((String) iterator.next());
				w.newLine();
			}
			for (Iterator iterator = tautologies.iterator(); iterator.hasNext();) {
				w.write((String) iterator.next());
				w.newLine();
			}
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean isApplicable(IInstallableUnit iu) {
		String enablementFilter = iu.getFilter();
		if (enablementFilter == null)
			return true;
		try {
			return DirectorActivator.context.createFilter(enablementFilter).match(selectionContext);
		} catch (InvalidSyntaxException e) {
			return false;
		}
	}

	public void processIU(IInstallableUnit iu) {
		slice.put(iu.getId(), iu.getVersion(), iu);
		explanation.append(" ").append(getVariable(iu)); //$NON-NLS-1$
		if (!isApplicable(iu)) {
			createNegation(iu);
			return;
		}

		RequiredCapability[] reqs = iu.getRequiredCapabilities();
		if (reqs.length == 0) {
			return;
		}
		for (int i = 0; i < reqs.length; i++) {
			if (!isApplicable(reqs[i]))
				continue;

			try {
				expandRequirement(iu, reqs[i]);
			} catch (IllegalStateException ise) {
				result.add(new Status(IStatus.WARNING, DirectorActivator.PI_DIRECTOR, ise.getMessage(), ise));
				createNegation(iu);
			}
		}
		addOptionalityExpression();
	}

	private void addOptionalityExpression() {
		if (optionalityExpression != null && countOptionalIUs > 0)
			dependencies.add(optionalityExpression + " >= 0;"); //$NON-NLS-1$
		optionalityExpression = null;
		countOptionalIUs = 0;
	}

	private String optionalityExpression = null;
	private int countOptionalIUs = 0;

	private void expandOptionalRequirement(IInstallableUnit iu, RequiredCapability req) {
		String abstractVar = getAbstractVariable();
		String expression = " -1 " + abstractVar; //$NON-NLS-1$
		Collector matches = picker.query(new CapabilityQuery(req), new Collector(), null);
		if (optionalityExpression == null)
			optionalityExpression = " -1 " + getVariable(iu) + " 1 " + getNoOptionalVariable(iu); //$NON-NLS-1$ //$NON-NLS-2$ 

		int countMatches = 0;
		for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
			IInstallableUnit match = (IInstallableUnit) iterator.next();
			if (!isApplicable(match))
				continue;
			countMatches++;
			expression += " 1 " + getVariable(match); //$NON-NLS-1$
		}
		countOptionalIUs += countMatches;
		if (countMatches > 0) {
			dependencies.add(" -1 " + getNoOptionalVariable(iu) + " -1 " + abstractVar + " >= -1;"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			dependencies.add(expression + " >= 0;"); //$NON-NLS-1$
			//			dependencies.add("-1" + getVariable(iu) + " 1 " + abstractVar + " >=0;"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			optionalityExpression += " 1 " + abstractVar; //$NON-NLS-1$
		}

		if (DEBUG)
			System.out.println("No IU found to satisfy optional dependency of " + iu + " req " + req); //$NON-NLS-1$//$NON-NLS-2$
	}

	private void expandNormalRequirement(IInstallableUnit iu, RequiredCapability req) {
		//Generate the regular requirement
		String expression = "-1 " + getVariable(iu); //$NON-NLS-1$
		Collector matches = picker.query(new CapabilityQuery(req), new Collector(), null);

		int countMatches = 0;
		for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
			IInstallableUnit match = (IInstallableUnit) iterator.next();
			if (!isApplicable(match))
				continue;
			countMatches++;
			expression += " +1 " + getVariable(match); //$NON-NLS-1$
		}

		if (countMatches > 0) {
			dependencies.add(expression + " >= 0;"); //$NON-NLS-1$
		} else {
			throw new IllegalStateException("No IU found to satisfy dependency of " + iu + " req " + req); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	private void expandRequirement(IInstallableUnit iu, RequiredCapability req) {
		if (req.isOptional())
			expandOptionalRequirement(iu, req);
		else
			expandNormalRequirement(iu, req);
	}

	//Create constraints to deal with singleton
	//When there is a mix of singleton and non singleton, several constraints are generated 
	private void createConstraintsForSingleton() {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			HashMap conflictingEntries = (HashMap) entry.getValue();
			if (conflictingEntries.size() < 2)
				continue;

			Collection conflictingVersions = conflictingEntries.values();
			String singletonRule = ""; //$NON-NLS-1$
			ArrayList nonSingleton = new ArrayList();
			int countSingleton = 0;
			for (Iterator conflictIterator = conflictingVersions.iterator(); conflictIterator.hasNext();) {
				IInstallableUnit conflictElt = (IInstallableUnit) conflictIterator.next();
				if (conflictElt.isSingleton()) {
					singletonRule += " -1 " + getVariable(conflictElt); //$NON-NLS-1$
					countSingleton++;
				} else {
					nonSingleton.add(conflictElt);
				}
			}
			if (countSingleton == 0)
				continue;

			for (Iterator iterator2 = nonSingleton.iterator(); iterator2.hasNext();) {
				constraints.add(singletonRule + " -1 " + getVariable((IInstallableUnit) iterator2.next()) + " >= -1;"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			singletonRule += " >= -1;"; //$NON-NLS-1$
			constraints.add(singletonRule);
		}
	}

	//Return the corresponding variable 
	private String getVariable(IInstallableUnit iu) {
		String v = (String) variables.get(iu);
		if (v == null) {
			//			v = new String("x" + varCount++ + "--" + iu.toString()); //$NON-NLS-1$
			v = new String("x" + varCount++); //$NON-NLS-1$
			variables.put(iu, v);
		}
		return v;
	}

	private String getAbstractVariable() {
		String newVar = new String("x" + varCount++); //$NON-NLS-1$
		abstractVariables.add(newVar);
		return newVar;
	}

	private String getNoOptionalVariable(IInstallableUnit iu) {
		String v = (String) noopVariables.get(iu);
		if (v == null) {
			//			v = new String("x" + varCount++ + "-noop-" + iu.toString()); //$NON-NLS-1$
			v = new String("x" + varCount++); //$NON-NLS-1$
			noopVariables.put(iu, v);
		}
		return v;
	}

	public IStatus invokeSolver(IProgressMonitor monitor) {
		if (result.getSeverity() == IStatus.ERROR)
			return result;
		IPBSolver solver = SolverFactory.newEclipseP2();
		solver.setTimeout(60);
		OPBEclipseReader2007 reader = new OPBEclipseReader2007(solver);
		// CNF filename is given on the command line 
		long start = System.currentTimeMillis();
		if (DEBUG)
			System.out.println("Invoking solver: " + start); //$NON-NLS-1$
		FileReader fr = null;
		try {
			fr = new FileReader(problemFile);
			PBSolverCP problem = (PBSolverCP) reader.parseInstance(fr);
			if (problem.getOrder() instanceof VarOrderHeapObjective) {
				((VarOrderHeapObjective) problem.getOrder()).setObjectiveFunction(reader.getObjectiveFunction());
			}
			if (problem.isSatisfiable()) {
				if (DEBUG) {
					System.out.println("Satisfiable !"); //$NON-NLS-1$
					System.out.println(reader.decode(problem.model()));
				}
				backToIU(problem);
				long stop = System.currentTimeMillis();
				if (DEBUG)
					System.out.println("Solver solution found: " + (stop - start)); //$NON-NLS-1$
			} else {
				if (DEBUG)
					System.out.println("Unsatisfiable !"); //$NON-NLS-1$
				result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, 1, "No solution found because the problem is unsatisfiable", null)); //$NON-NLS-1$
			}
		} catch (FileNotFoundException e) {
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, 1, "Missing problem file:" + problemFile, e)); //$NON-NLS-1$
		} catch (ParseFormatException e) {
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, 1, "Format error in problem file: " + problemFile, e)); //$NON-NLS-1$
		} catch (ContradictionException e) {
			result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, 1, "No solution found because of a trivial contradiction", e)); //$NON-NLS-1$
		} catch (TimeoutException e) {
			result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, 1, "No solution found.", e)); //$NON-NLS-1$
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (fr != null)
					fr.close();
			} catch (IOException e) {
				//ignore
			}
			problemFile.delete();
		}
		return result;
	}

	private void backToIU(IProblem problem) {
		solution = new ArrayList();
		for (Iterator allIUs = variables.entrySet().iterator(); allIUs.hasNext();) {
			Entry entry = (Entry) allIUs.next();
			int match = Integer.parseInt(((String) entry.getValue()).substring(1));
			if (problem.model(match)) {
				solution.add(((IInstallableUnit) entry.getKey()).unresolved());
			}
		}
	}

	private void printSolution(Collection state) {
		ArrayList l = new ArrayList(state);
		Collections.sort(l);
		System.out.println("Numbers of IUs selected:" + l.size()); //$NON-NLS-1$
		for (Iterator iterator = l.iterator(); iterator.hasNext();) {
			System.out.println(iterator.next());
		}
	}

	public Collection extractSolution() {
		if (DEBUG)
			printSolution(solution);
		return solution;
	}
}
