/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *     Ericsson AB (Pascal Rapicault) - Bug 387115 - Allow to export everything
 *     Ericsson AB (Hamdan Msheik) - Bug 398833
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler.migration;

import java.net.URI;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.dialogs.ISelectableIUsPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard;
import org.eclipse.equinox.internal.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class ImportFromInstallationPage_c extends AbstractImportPage_c implements ISelectableIUsPage {

	protected IProvisioningAgent otherInstanceAgent = null;
	private IProfile toBeImportedProfile = null;
	//	private File instancePath = null;
	private URI[] metaURIs = null;
	private URI[] artiURIs = null;

	//	private IProvisioningAgentProvider agentProvider;

	public ImportFromInstallationPage_c(ProvisioningUI ui, ProvisioningOperationWizard wizard, boolean firstTime) {
		super("importfrominstancepage", ui, wizard); //$NON-NLS-1$
		setTitle(firstTime ? ProvUIMessages.ImportFromInstallationPage_DIALOG_TITLE_FIRSTRUN : ProvUIMessages.ImportFromInstallationPage_DIALOG_TITLE);
		setDescription(NLS.bind(ProvUIMessages.ImportFromInstallationPage_DIALOG_DESCRIPTION, Platform.getProduct().getName()));
	}

	public ImportFromInstallationPage_c(ProvisioningUI ui, ImportFromInstallationWizard_c wizard, IProfile toImportFrom, boolean firstTime) {
		super("importfrominstancepage", ui, wizard); //$NON-NLS-1$
		setTitle(firstTime ? ProvUIMessages.ImportFromInstallationPage_DIALOG_TITLE_FIRSTRUN : ProvUIMessages.ImportFromInstallationPage_DIALOG_TITLE);
		setDescription(NLS.bind(firstTime ? ProvUIMessages.ImportFromInstallationPage_DIALOG_DESCRIPTION_FIRSTRUN : ProvUIMessages.ImportFromInstallationPage_DIALOG_DESCRIPTION, Platform.getProduct().getName()));
		toBeImportedProfile = toImportFrom;
	}

	@Override
	protected void createContents(Composite composite) {
		createInstallationTable(composite);
	}

	//	private void showProfile() {
	//
	//		//TODO remove already installed installable units from the profile to be imported
	//		IQueryResult<IInstallableUnit> result = profile.available(null, null);
	//		Profile p1 = (Profile) profile;
	//		while (iterator)
	//			for (IInstallableUnit unit : result.iterator()) {
	//
	//			}
	//		final ProfileElement element = new ProfileElement(null, toBeImportedProfile.getProfileId()) {
	//			@Override
	//			public org.eclipse.equinox.p2.query.IQueryable<?> getQueryable() {
	//				return toBeImportedProfile;
	//			}
	//		};
	//		element.setQueryable(toBeImportedProfile);
	//
	//		viewer.setInput(element);
	//		updatePageCompletion();
	//	}

	//	@Override
	//	protected String getDestinationLabel() {
	//		return Messages.ImportFromInstallationPage_DESTINATION_LABEL;
	//	}

	@Override
	protected String getDialogTitle() {
		return ProvUIMessages.ImportFromInstallationPage_DIALOG_TITLE;
	}

	@Override
	protected Object getInput() {

		Object input = null;

		if (toBeImportedProfile != null) {
			final ProfileElement element = new ProfileElement(null, toBeImportedProfile.getProfileId()) {
				@Override
				public org.eclipse.equinox.p2.query.IQueryable<?> getQueryable() {
					return toBeImportedProfile;
				}
			};
			element.setQueryable(toBeImportedProfile);
			input = element;
		} else {
			input = new IInstallableUnit[0];
		}

		return input;
	}

	@Override
	protected String getInvalidDestinationMessage() {
		return "";//ProvUIMessages.ImportFromInstallationPage_INVALID_DESTINATION; //$NON-NLS-1$
	}

	@Override
	protected String getNoOptionsMessage() {
		return ProvUIMessages.ImportFromInstallationPage_SELECT_COMPONENT;
	}

	//	@Override
	//	protected boolean validateDestinationGroup() {
	//		return validateDestinationGroup(new NullProgressMonitor());
	//	}

	//	private IProvisioningAgentProvider getAgentProvider() {
	//		if (agentProvider == null) {
	//			ServiceTracker<IProvisioningAgentProvider, IProvisioningAgentProvider> tracker = new ServiceTracker<IProvisioningAgentProvider, IProvisioningAgentProvider>(Platform.getBundle(Constants.Bundle_ID).getBundleContext(), IProvisioningAgentProvider.class, null);
	//			tracker.open();
	//			agentProvider = tracker.getService();
	//			tracker.close();
	//		}
	//		return agentProvider;
	//	}

	//	boolean validateDestinationGroup(IProgressMonitor monitor) {
	//		SubMonitor progress = SubMonitor.convert(monitor, 100);
	//
	//		boolean rt;
	//		if (Display.findDisplay(Thread.currentThread()) == null) {
	//			Callable<Boolean> getSuperValidateDest = new Callable<Boolean>() {
	//				Boolean validated;
	//
	//				public Boolean call() throws Exception {
	//					Display.getDefault().syncExec(new Runnable() {
	//						public void run() {
	//							validated = ImportFromInstallationPage_c.super.validateDestinationGroup();
	//						}
	//					});
	//					return validated;
	//				}
	//			};
	//			ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	//			Future<Boolean> getSuperDestTask = executor.submit(getSuperValidateDest);
	//
	//			try {
	//				rt = getSuperDestTask.get().booleanValue();
	//			} catch (Exception e) {
	//				return false;
	//			} finally {
	//				executor.shutdown();
	//			}
	//		} else
	//			rt = super.validateDestinationGroup();
	//
	//		if (rt) {
	//			try {
	//				String destination;
	//				if (Display.findDisplay(Thread.currentThread()) == null) {
	//					Callable<String> getDestinationValue = new Callable<String>() {
	//						String des;
	//
	//						public String call() throws Exception {
	//							if (Display.findDisplay(Thread.currentThread()) == null) {
	//								Display.getDefault().syncExec(new Runnable() {
	//									public void run() {
	//										des = getDestinationValue();
	//									}
	//								});
	//							} else
	//								des = getDestinationValue();
	//							return des;
	//						}
	//					};
	//					ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	//					Future<String> getDestTask = executor.submit(getDestinationValue);
	//					try {
	//						destination = getDestTask.get();
	//					} finally {
	//						executor.shutdown();
	//					}
	//				} else
	//					destination = getDestinationValue();
	//
	//				String toBeImportedProfileId = null;
	//				try {
	//					File config = new File(destination, "configuration/config.ini"); //$NON-NLS-1$
	//					URI configArea = config.getParentFile().toURI();
	//					InputStream is = null;
	//					// default area
	//					File p2DataArea = new File(destination, "p2"); //$NON-NLS-1$
	//					try {
	//						Properties props = new Properties();
	//						is = new FileInputStream(config);
	//						props.load(is);
	//						toBeImportedProfileId = props.getProperty("eclipse.p2.profile"); //$NON-NLS-1$
	//						String url = props.getProperty("eclipse.p2.data.area"); //$NON-NLS-1$
	//						if (url != null) {
	//							final String CONFIG_DIR = "@config.dir/"; //$NON-NLS-1$
	//							final String FILE_PROTOCOL = "file:"; //$NON-NLS-1$
	//							if (url.startsWith(CONFIG_DIR))
	//								url = FILE_PROTOCOL + url.substring(CONFIG_DIR.length());
	//							p2DataArea = new File(URIUtil.makeAbsolute(URIUtil.fromString(new File(url.substring(FILE_PROTOCOL.length())).isAbsolute() ? url : url.substring(FILE_PROTOCOL.length())), configArea));
	//						}
	//					} catch (IOException ioe) {
	//						//ignore
	//					} finally {
	//						try {
	//							is.close();
	//						} catch (IOException ioe) {
	//							//ignore
	//						}
	//						is = null;
	//					}
	//					if (p2DataArea.exists()) {
	//						boolean createAgent = true;
	//						if (otherInstanceAgent != null) {
	//							// don't create agent again if the selection is not changed
	//							if (!p2DataArea.equals(instancePath)) {
	//								otherInstanceAgent.stop();
	//								otherInstanceAgent = null;
	//								// update cached specified path by users
	//								instancePath = p2DataArea;
	//								cleanLocalRepository();
	//							} else
	//								createAgent = false;
	//						}
	//						if (createAgent)
	//							otherInstanceAgent = getAgentProvider().createAgent(p2DataArea.toURI());
	//						ArtifactRepositoryFactory factory = new ExtensionLocationArtifactRepositoryFactory();
	//						factory.setAgent(agent);
	//						IArtifactRepository artiRepo = factory.load(new File(destination).toURI(), 0, progress.newChild(50));
	//						artiURIs = new URI[] {artiRepo.getLocation()};
	//						MetadataRepositoryFactory metaFatory = new ExtensionLocationMetadataRepositoryFactory();
	//						metaFatory.setAgent(agent);
	//						IMetadataRepository metaRepo = metaFatory.load(new File(destination).toURI(), 0, progress.newChild(50));
	//						metaURIs = new URI[] {metaRepo.getLocation()};
	//
	//					} else
	//						throw new FileNotFoundException();
	//				} catch (ProvisionException e) {
	//					if (otherInstanceAgent != null) {
	//						toBeImportedProfile = null;
	//						IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
	//						IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
	//						IProfileRegistry registry = (IProfileRegistry) otherInstanceAgent.getService(IProfileRegistry.SERVICE_NAME);
	//						if (toBeImportedProfileId != null)
	//							toBeImportedProfile = registry.getProfile(toBeImportedProfileId);
	//						if (toBeImportedProfile == null) {
	//							IProfile[] existingProfiles = registry.getProfiles();
	//							if (existingProfiles.length == 1) {
	//								toBeImportedProfile = existingProfiles[0];
	//							} else {
	//								for (IProfile existingProfile : existingProfiles) {
	//									if (toBeImportedProfile == null)
	//										toBeImportedProfile = existingProfile;
	//									else if ((toBeImportedProfile.getTimestamp() < existingProfile.getTimestamp())) // assuming last modified one is we are looking for
	//										toBeImportedProfile = existingProfile;
	//								}
	//							}
	//						}
	//						IAgentLocation location = (IAgentLocation) otherInstanceAgent.getService(IAgentLocation.SERVICE_NAME);
	//						URI engineDataArea = location.getDataArea("org.eclipse.equinox.p2.engine"); //$NON-NLS-1$
	//						progress.setWorkRemaining(50);
	//						IMetadataRepository metaRepo = manager.loadRepository(engineDataArea.resolve("profileRegistry/" + toBeImportedProfile.getProfileId() + ".profile"), progress.newChild(25)); //$NON-NLS-1$//$NON-NLS-2$
	//						metaURIs = new URI[] {metaRepo.getLocation()};
	//						IArtifactRepository artiRepo = artifactManager.loadRepository(new File(destination).toURI(), progress.newChild(25));
	//						artiURIs = new URI[] {artiRepo.getLocation()};
	//					} else
	//						throw new Exception();
	//				}
	//			} catch (Exception e) {
	//				Display.getDefault().asyncExec(new Runnable() {
	//					public void run() {
	//						setErrorMessage(getInvalidDestinationMessage());
	//					}
	//				});
	//				rt = false;
	//				if (otherInstanceAgent != null)
	//					otherInstanceAgent.stop();
	//				otherInstanceAgent = null;
	//				toBeImportedProfile = null;
	//				cleanLocalRepository();
	//			} finally {
	//				monitor.done();
	//			}
	//		}
	//		return rt;
	//	}

	//	@Override
	//	protected void giveFocusToDestination() {
	//		destinationBrowseButton.setFocus();
	//	}

	//	@Override
	//	protected void handleDestinationBrowseButtonPressed() {
	//		DirectoryDialog dialog = new DirectoryDialog(getContainer().getShell());
	//		dialog.setText(getDialogTitle());
	//		dialog.setFilterPath(getDestinationValue());
	//		final String selectedFileName = dialog.open();
	//
	//		if (selectedFileName != null) {
	//			setDestinationValue(selectedFileName);
	//			handleDestinationChanged(selectedFileName);
	//		}
	//	}

	//	@Override
	//	protected void handleDestinationChanged(String newDestination) {
	//		try {
	//			getContainer().run(true, false, new IRunnableWithProgress() {
	//
	//				public void run(IProgressMonitor monitor) {
	//					Object input = null;
	//					if (validateDestinationGroup(monitor)) {
	//						final IProfile currentProfile = toBeImportedProfile;
	//						final ProfileElement element = new ProfileElement(null, currentProfile.getProfileId()) {
	//							@Override
	//							public org.eclipse.equinox.p2.query.IQueryable<?> getQueryable() {
	//								return currentProfile;
	//							}
	//						};
	//						element.setQueryable(currentProfile);
	//						input = element;
	//
	//					}
	//					final Object viewerInput = input;
	//					Display.getDefault().asyncExec(new Runnable() {
	//
	//						public void run() {
	//							viewer.setInput(viewerInput);
	//							updatePageCompletion();
	//						}
	//					});
	//				}
	//			});
	//		} catch (InvocationTargetException e) {
	//			setErrorMessage(e.getLocalizedMessage());
	//			setPageComplete(false);
	//		} catch (InterruptedException e) {
	//			// won't happen
	//		}
	//	}

	//	@Override
	//	protected boolean validDestination() {
	//		if (this.destinationNameField == null)
	//			return true;
	//		File file = new File(getDestinationValue());
	//		return file.exists() && file.isDirectory();
	//	}

	class ImportFromInstallationLabelProvider extends IUDetailsLabelProvider {
		@Override
		public String getColumnText(Object element, int columnIndex) {
			String text = super.getColumnText(element, columnIndex);
			// it's the order of label provider
			if (columnIndex == 0) {
				IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
				return getIUNameWithDetail(iu);
			}
			return text;
		}

		@Override
		public Color getForeground(Object element) {
			IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
			if (hasInstalled(iu))
				return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
			return super.getForeground(element);
		}
	}

	@Override
	protected ITableLabelProvider getLabelProvider() {
		return new ImportFromInstallationLabelProvider();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (otherInstanceAgent != null) {
			otherInstanceAgent.stop();
			otherInstanceAgent = null;
			toBeImportedProfile = null;
		}
		cleanLocalRepository();
	}

	public void cleanLocalRepository() {
		if (metaURIs != null && metaURIs.length > 0) {
			IProvisioningAgent runningAgent = getProvisioningUI().getSession().getProvisioningAgent();
			IMetadataRepositoryManager manager = (IMetadataRepositoryManager) runningAgent.getService(IMetadataRepositoryManager.SERVICE_NAME);
			for (URI uri : metaURIs)
				manager.removeRepository(uri);
			IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) runningAgent.getService(IArtifactRepositoryManager.SERVICE_NAME);
			for (URI uri : artiURIs)
				artifactManager.removeRepository(uri);
		}
	}

	public Object[] getCheckedIUElements() {
		return viewer.getCheckedElements();
	}

	public Object[] getSelectedIUElements() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setCheckedElements(Object[] elements) {
		new UnsupportedOperationException();
	}

	public ProvisioningContext getProvisioningContext() {
		ProvisioningContext context = new ProvisioningContext(getProvisioningUI().getSession().getProvisioningAgent());
		context.setArtifactRepositories(artiURIs);
		context.setMetadataRepositories(metaURIs);
		return context;
	}
}