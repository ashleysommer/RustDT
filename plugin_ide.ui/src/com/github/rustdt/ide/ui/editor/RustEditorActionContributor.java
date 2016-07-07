/*******************************************************************************
 * Copyright (c) 2014, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package com.github.rustdt.ide.ui.editor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.ITextEditor;

import com.github.rustdt.ide.ui.actions.RustOpenDefinitionOperation;

import melnorme.lang.ide.core.utils.ResourceUtils;
import melnorme.lang.ide.ui.editor.EditorUtils.OpenNewEditorMode;
import melnorme.lang.ide.ui.editor.LangEditorActionContributor;
import melnorme.lang.tooling.ast.SourceRange;
import melnorme.lang.tooling.structure.GlobalSourceStructure;

public class RustEditorActionContributor extends LangEditorActionContributor {
	
	@Override
	protected RustOpenDefinitionOperation createOpenDefinitionOperation(ITextEditor editor, SourceRange range,
			OpenNewEditorMode newEditorMode) {
		return new RustOpenDefinitionOperation(editor, range, newEditorMode);
	}
	
	@Override
	protected void registerOtherEditorHandlers() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(event -> {
			try {
				processEvent(event);
			} catch(CoreException e) {
				throw new RuntimeException(e);
			}
		}, IResourceChangeEvent.POST_CHANGE);
	}
	
	void processEvent(IResourceChangeEvent event) throws CoreException {
		event.getDelta().accept(delta -> {
			IResource resource = delta.getResource();
			switch(delta.getKind()) {
			case IResourceDelta.ADDED:
				// TODO: Run ParseDescribe
				break;
			case IResourceDelta.REMOVED:
				GlobalSourceStructure.fileRemoved(ResourceUtils.getResourceLocation(resource));
				break;
			case IResourceDelta.CHANGED:
				// TODO: Run ParseDescribe?
				break;
			}
			return true;
		});
	}
}