/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.mirror;

import java.net.URL;
import java.util.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.Activator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;

/**
 * An application that performs mirroring of artifacts between repositories.
 */
public class MirrorApplication implements IApplication {

	private String[] rootSpecs;
	private URL sourceLocation;
	private URL destinationLocation;
	private IMetadataRepository source;
	private IMetadataRepository destination;
	private boolean transitive = false;
	private boolean append = false;

	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified.
	 */
	public static String[] getArrayArgsFromString(String list, String separator) {
		if (list == null || list.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		List result = new ArrayList();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) { //$NON-NLS-1$
				if ((token.indexOf('[') >= 0 || token.indexOf('(') >= 0) && tokens.hasMoreTokens())
					result.add(token + separator + tokens.nextToken());
				else
					result.add(token);
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		initializeFromArguments((String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS));
		setupRepositories();
		new Mirroring().mirror(source, destination, rootSpecs, transitive);
		return IApplication.EXIT_OK;
	}

	private void setupRepositories() throws ProvisionException {
		if (destinationLocation == null || sourceLocation == null)
			throw new IllegalStateException("Must specify a source and destination"); //$NON-NLS-1$
		destination = initializeDestination();
		source = getManager().loadRepository(sourceLocation, null);
	}

	private IMetadataRepositoryManager getManager() {
		return (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
	}

	private IMetadataRepository initializeDestination() throws ProvisionException {
		try {
			IMetadataRepository repository = getManager().loadRepository(destinationLocation, null);
			if (!repository.isModifiable())
				throw new IllegalArgumentException("Metadata repository not modifiable: " + destinationLocation); //$NON-NLS-1$
			if (!append)
				repository.removeAll();
			return repository;
		} catch (ProvisionException e) {
			//fall through and create repo
		}
		String repositoryName = destinationLocation + " - metadata"; //$NON-NLS-1$
		return getManager().createRepository(destinationLocation, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		//do nothing
	}

	public void initializeFromArguments(String[] args) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {
			// check for args without parameters (i.e., a flag arg)
			if (args[i].equalsIgnoreCase("-append")) //$NON-NLS-1$
				append = true;

			// check for args with parameters. If we are at the last argument or 
			// if the next one has a '-' as the first character, then we can't have 
			// an arg with a param so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			String arg = args[++i];

			if (args[i - 1].equalsIgnoreCase("-source")) //$NON-NLS-1$
				sourceLocation = new URL(arg);
			if (args[i - 1].equalsIgnoreCase("-destination")) //$NON-NLS-1$
				destinationLocation = new URL(arg);
			if (args[i - 1].equalsIgnoreCase("-roots")) //$NON-NLS-1$
				rootSpecs = getArrayArgsFromString(arg, ","); //$NON-NLS-1$
			if (args[i - 1].equalsIgnoreCase("-transitive")) //$NON-NLS-1$
				transitive = true;
		}
	}
}
