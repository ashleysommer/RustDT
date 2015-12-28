/*******************************************************************************
 * Copyright (c) 2015, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package com.github.rustdt.ide.core.operations;

import com.github.rustdt.tooling.ops.RustSDKLocationValidator;

import melnorme.lang.ide.core.operations.AbstractToolManager;
import melnorme.lang.tooling.ops.util.PathValidator;

public class RustToolManager extends AbstractToolManager {
	
	@Override
	public PathValidator getSDKToolPathValidator() {
		return new RustSDKLocationValidator();
	}
	
}