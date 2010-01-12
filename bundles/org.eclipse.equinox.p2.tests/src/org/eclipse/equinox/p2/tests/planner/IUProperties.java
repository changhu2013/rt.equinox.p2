/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class IUProperties extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private IInstallableUnit b1;
	private IInstallableUnit b11;
	private IInstallableUnit c;
	private IProfile profile;
	private IPlanner planner;
	private IEngine engine;
	private String profileId;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "B1", new VersionRange("[1.0.0, 2.0.0)"), null));

		b1 = createIU("B1", Version.create("1.0.0"), true);

		b11 = createIU("B1", Version.create("1.1.0"), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "C", new VersionRange("[1.0.0, 3.0.0)"), null), NO_PROPERTIES, true);

		c = createIU("C", Version.createOSGi(2, 0, 0), true);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, b11, c});

		profileId = "TestProfile." + getName();
		profile = createProfile(profileId);
		planner = createPlanner();
		engine = createEngine();

	}

	public void testRemoveIUProperty() {
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile);
		req1.addInstallableUnits(new IInstallableUnit[] {a1});
		req1.setInstallableUnitProfileProperty(a1, "FOO", "BAR");
		req1.setInstallableUnitProfileProperty(b1, "FOO", "BAR");
		IProvisioningPlan pp1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(IStatus.OK, pp1.getStatus().getSeverity());
		IStatus s = engine.perform(pp1, null);
		if (!s.isOK())
			LogHelper.log(s);
		IQueryResult queryResult = getProfile(profileId).query(new IUProfilePropertyQuery("FOO", null), null);
		assertEquals(1, queryResultSize(queryResult));

		ProfileChangeRequest req2 = new ProfileChangeRequest(profile);
		req2.removeInstallableUnitProfileProperty(b1, "FOO");
		IProvisioningPlan pp2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(0, pp2.getOperands().length);
	}
}
