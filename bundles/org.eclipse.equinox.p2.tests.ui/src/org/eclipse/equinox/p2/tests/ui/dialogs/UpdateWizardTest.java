/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.VersionRange;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.metadata.License;
import org.eclipse.equinox.internal.p2.ui.dialogs.ResolutionResultsWizardPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.SelectableIUsPage;
import org.eclipse.equinox.internal.p2.ui.model.AvailableUpdateElement;
import org.eclipse.equinox.internal.provisional.p2.core.*;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.ProvisioningWizardDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.UpdateWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Tests for the install wizard
 */
public class UpdateWizardTest extends WizardTest {

	private static final String SELECTION_PAGE = "IUSelectionPage";
	private static final String RESOLUTION_PAGE = "ResolutionPage";
	private static final String MAIN_IU = "MainIU";
	IInstallableUnit main, mainUpgrade1, mainUpgrade2, mainUpgradeWithLicense;

	protected void setUp() throws Exception {
		super.setUp();
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(MAIN_IU);
		iu.setVersion(Version.createOSGi(1, 0, 0));
		iu.setSingleton(true);
		iu.setCapabilities(new IProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, MAIN_IU, iu.getVersion())});
		main = MetadataFactory.createInstallableUnit(iu);
		install(main, true, false);
		IUpdateDescriptor update = MetadataFactory.createUpdateDescriptor(MAIN_IU, new VersionRange("[1.0.0, 1.0.0]"), 0, "update description");
		mainUpgrade1 = createIU(MAIN_IU, Version.createOSGi(2, 0, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, true, update, NO_REQUIRES);
		update = MetadataFactory.createUpdateDescriptor(MAIN_IU, new VersionRange("[1.0.0, 1.0.0]"), 0, "update description");
		mainUpgrade2 = createIU(MAIN_IU, Version.createOSGi(3, 0, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, true, update, NO_REQUIRES);
		iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(MAIN_IU);
		iu.setVersion(Version.createOSGi(4, 0, 0));
		iu.setSingleton(true);
		iu.setUpdateDescriptor(update);
		iu.setLicense(new License(null, "Update Wizard Test License to Accept"));
		iu.setCapabilities(new IProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, MAIN_IU, iu.getVersion())});
		mainUpgradeWithLicense = MetadataFactory.createInstallableUnit(iu);
	}

	/**
	 * Tests the wizard when a prior resolution has been done.
	 * This is the SDK 
	 */
	public void testUpdateWizardResolved() throws ProvisionException {
		IUElementListRoot root = new IUElementListRoot();
		AvailableUpdateElement element = new AvailableUpdateElement(root, upgrade, top1, TESTPROFILE, true);
		root.setChildren(new Object[] {element});
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.removeInstallableUnits(new IInstallableUnit[] {top1});
		request.addInstallableUnits(new IInstallableUnit[] {upgrade});
		PlannerResolutionOperation op = getResolvedOperation(request);
		UpdateWizard wizard = new UpdateWizard(Policy.getDefault(), TESTPROFILE, root, new Object[] {element}, op, null);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();

		try {
			SelectableIUsPage page1 = (SelectableIUsPage) wizard.getPage(SELECTION_PAGE);
			// should already have a plan
			assertTrue(page1.isPageComplete());
			ResolutionResultsWizardPage page2 = (ResolutionResultsWizardPage) wizard.getNextPage(page1);
			dialog.showPage(page2);
			assertTrue(page2.isPageComplete());

			// if another operation is scheduled for this profile, we should not be allowed to proceed
			Job job = ProvisioningOperationRunner.schedule(getLongTestOperation(), StatusManager.LOG);
			assertTrue(page1.isPageComplete());
			// causes recalculation of plan and status
			wizard.getNextPage(page1);
			// can't move to next page while op is running
			assertFalse(page1.isPageComplete());
			job.cancel();

		} finally {
			dialog.getShell().close();
		}
	}

	public void testUpdateWizardResolvedWithLicense() throws ProvisionException {
		IUElementListRoot root = new IUElementListRoot();
		AvailableUpdateElement element = new AvailableUpdateElement(root, mainUpgradeWithLicense, main, TESTPROFILE, true);
		root.setChildren(new Object[] {element});
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.removeInstallableUnits(new IInstallableUnit[] {main});
		request.addInstallableUnits(new IInstallableUnit[] {mainUpgradeWithLicense});
		PlannerResolutionOperation op = getResolvedOperation(request);
		UpdateWizard wizard = new UpdateWizard(Policy.getDefault(), TESTPROFILE, root, new Object[] {element}, op, null);
		ProvisioningWizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();

		try {
			SelectableIUsPage page1 = (SelectableIUsPage) wizard.getPage(SELECTION_PAGE);
			// should already have a plan
			assertTrue("1.0", page1.isPageComplete());
			// simulate the next button by getting next page and showing
			IWizardPage page = page1.getNextPage();
			dialog.showPage(page);
			// license needs approval
			assertFalse("1.1", wizard.canFinish());
			// finish button should be disabled
			while (dialog.getShell().getDisplay().readAndDispatch()) {
				// run event loop
			}
			Button finishButton = dialog.testGetButton(IDialogConstants.FINISH_ID);
			assertFalse("1.2", finishButton.isEnabled());
		} finally {
			dialog.getShell().close();
		}
	}

	/**
	 * Tests the wizard when a prior resolution has been done, but is in error.
	 */
	public void testUpdateWizardResolvedError() throws ProvisionException {
		IUElementListRoot root = new IUElementListRoot();
		AvailableUpdateElement element = new AvailableUpdateElement(root, mainUpgrade1, main, TESTPROFILE, true);
		AvailableUpdateElement element2 = new AvailableUpdateElement(root, mainUpgrade2, main, TESTPROFILE, true);
		root.setChildren(new Object[] {element, element2});
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.removeInstallableUnits(new IInstallableUnit[] {main});
		request.addInstallableUnits(new IInstallableUnit[] {mainUpgrade1, mainUpgrade2});
		PlannerResolutionOperation op = getResolvedOperation(request);
		UpdateWizard wizard = new UpdateWizard(Policy.getDefault(), TESTPROFILE, root, new Object[] {element, element2}, op, null);
		wizard.setSkipSelectionsPage(true);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();

		try {
			assertNotNull("1.0", wizard.getStartingPage());
		} finally {
			dialog.getShell().close();
		}
	}

	/**
	 * Tests the wizard when we have a successful resolution and want to open
	 * directly on the resolution page
	 */
	public void testUpdateWizardResolvedSkipSelections() throws ProvisionException {
		IUElementListRoot root = new IUElementListRoot();
		AvailableUpdateElement element = new AvailableUpdateElement(root, mainUpgrade1, main, TESTPROFILE, true);
		root.setChildren(new Object[] {element});
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.removeInstallableUnits(new IInstallableUnit[] {main});
		request.addInstallableUnits(new IInstallableUnit[] {mainUpgrade1});
		PlannerResolutionOperation op = getResolvedOperation(request);
		UpdateWizard wizard = new UpdateWizard(Policy.getDefault(), TESTPROFILE, root, new Object[] {element}, op, null);
		wizard.setSkipSelectionsPage(true);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();

		try {
			assertNotNull("1.0", wizard.getStartingPage());
			assertEquals("1.1", wizard.getStartingPage(), wizard.getPage(RESOLUTION_PAGE));
		} finally {
			dialog.getShell().close();
		}
	}

	/**
	 * Tests the wizard without a prior resolution being done.
	 * This is not the SDK workflow, but should be supported.
	 */
	public void testUpdateWizardUnresolved() {
		IUElementListRoot root = new IUElementListRoot();
		AvailableUpdateElement element = new AvailableUpdateElement(root, upgrade, top1, TESTPROFILE, true);
		root.setChildren(new Object[] {element});

		UpdateWizard wizard = new UpdateWizard(Policy.getDefault(), TESTPROFILE, root, new Object[] {element}, null, null);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();

		try {
			SelectableIUsPage page1 = (SelectableIUsPage) wizard.getPage(SELECTION_PAGE);
			assertFalse(page1.isPageComplete());
			// Will cause computation of a plan.
			ResolutionResultsWizardPage page2 = (ResolutionResultsWizardPage) wizard.getNextPage(page1);
			dialog.showPage(page2);
			assertTrue(page2.isPageComplete());

			// if another operation is scheduled for this profile, we should not be allowed to proceed
			Job job = ProvisioningOperationRunner.schedule(getLongTestOperation(), StatusManager.LOG);
			assertTrue(page1.isPageComplete());
			// causes recalculation of plan and status
			wizard.getNextPage(page1);
			// can't move to next page while op is running
			assertFalse(page1.isPageComplete());
			job.cancel();

		} finally {
			dialog.getShell().close();
		}
	}

	/**
	 * Tests the wizard when multiple versions are available.
	 */
	public void testBug277554MultipleVersions() throws ProvisionException {
		IUElementListRoot root = new IUElementListRoot();
		AvailableUpdateElement element = new AvailableUpdateElement(root, mainUpgrade1, main, TESTPROFILE, true);
		AvailableUpdateElement element2 = new AvailableUpdateElement(root, mainUpgrade2, main, TESTPROFILE, true);
		root.setChildren(new Object[] {element, element2});
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.removeInstallableUnits(new IInstallableUnit[] {main});
		request.addInstallableUnits(new IInstallableUnit[] {mainUpgrade2});
		PlannerResolutionOperation op = getResolvedOperation(request);
		UpdateWizard wizard = new UpdateWizard(Policy.getDefault(), TESTPROFILE, root, new Object[] {element2}, op, null);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();

		try {
			SelectableIUsPage page1 = (SelectableIUsPage) wizard.getPage(SELECTION_PAGE);
			// should already have a plan
			assertTrue("1.0", page1.isPageComplete());
			assertEquals("1.1", page1.getCheckedIUElements().length, 1);
			ResolutionResultsWizardPage page2 = (ResolutionResultsWizardPage) wizard.getNextPage(page1);
			dialog.showPage(page2);
			// should only have one root item in the resolution page
			assertEquals("1.2", 1, findTree(page2).getItemCount());
		} finally {
			dialog.getShell().close();
		}
	}

	protected Tree findTree(ResolutionResultsWizardPage page) {
		return findTree(page.getControl());
	}

	protected Tree findTree(Control control) {
		if (control instanceof Tree)
			return (Tree) control;
		if (control instanceof Composite) {
			Control[] children = ((Composite) control).getChildren();
			for (int i = 0; i < children.length; i++) {
				Tree tree = findTree(children[i]);
				if (tree != null)
					return tree;
			}

		}
		return null;
	}

}