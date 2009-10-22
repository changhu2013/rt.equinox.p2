/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.VersionRange;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SimpleOptionalTest4 extends AbstractProvisioningTest {
	//A dep B op, C, D op 
	//X dep B

	private IInstallableUnit a1;
	private IInstallableUnit b1;
	private IInstallableUnit b2;
	private IInstallableUnit b3;
	private IInstallableUnit b4;

	private IProfile profile;
	private IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();
		b1 = createIU("B", Version.create("1.0.0"), false);
		b2 = createIU("B", Version.create("2.0.0"), false);
		b3 = createIU("B", Version.create("3.0.0"), false);
		b4 = createIU("B", Version.create("4.0.0"), false);

		//B's dependency is missing
		IRequiredCapability[] reqA = new IRequiredCapability[4];
		reqA[0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0,1.0.0]"), null, true, false, true);
		reqA[1] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[2.0.0,2.0.0]"), null, true, false, true);
		reqA[2] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[3.0.0,3.0.0]"), null, true, false, true);
		reqA[3] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[4.0.0,4.0.0]"), null, true, false, true);
		a1 = createIU("A", Version.create("1.0.0"), reqA);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, b2, b3, b4});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstallation() {
		//Ensure that D's installation does not fail because of C's absence
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {a1, b1});
		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, a1);
		assertInstallOperand(plan, b1);
		assertInstallOperand(plan, b2);
		assertInstallOperand(plan, b3);
		assertInstallOperand(plan, b4);
	}
}
