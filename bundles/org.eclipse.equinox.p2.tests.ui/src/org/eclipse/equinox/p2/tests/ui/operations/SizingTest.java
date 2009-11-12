/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.operations;

import org.eclipse.equinox.p2.engine.IProvisioningPlan;

import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.SizingPhaseSet;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

/**
 * 
 */
public class SizingTest extends AbstractProvisioningUITest {
	public void testEmptySizing() {
		String profileId = "testEmptySizing";
		IProfile testProfile = createProfile(profileId);
		ProfileChangeRequest request = new ProfileChangeRequest(testProfile);
		IProvisioningPlan plan = null;
		plan = getSession().getProvisioningPlan(request, new ProvisioningContext(), getMonitor());
		long size = SizingPhaseSet.SIZE_NOTAPPLICABLE;
		size = getSession().getSize(plan, profileId, new ProvisioningContext(), getMonitor());
		assertEquals("1.0", 0, size);
	}

	/**
	 * Tests a simple sizing operation with an IU containing no artifacts
	 */
	public void testSimpleSizing() {
		IInstallableUnit f1 = createIU("f1", DEFAULT_VERSION, true);
		String profileId = "testSimpleSizing";
		IProfile testProfile = createProfile(profileId);
		ProfileChangeRequest request = new ProfileChangeRequest(testProfile);
		request.addInstallableUnits(new IInstallableUnit[] {f1});
		IProvisioningPlan plan = null;
		plan = getSession().getProvisioningPlan(request, new ProvisioningContext(), getMonitor());
		long size = SizingPhaseSet.SIZE_NOTAPPLICABLE;
		size = getSession().getSize(plan, profileId, new ProvisioningContext(), getMonitor());
		assertEquals("1.0", 0, size);
	}
}
