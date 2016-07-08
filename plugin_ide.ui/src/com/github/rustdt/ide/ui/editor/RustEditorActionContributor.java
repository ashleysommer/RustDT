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

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.ITextEditor;

import com.github.rustdt.ide.core.operations.RustParseDescribeLauncher;
import com.github.rustdt.ide.ui.actions.RustOpenDefinitionOperation;
import com.github.rustdt.tooling.ops.RustParseDescribeParser;

import melnorme.lang.ide.core.LangCore;
import melnorme.lang.ide.core.utils.ResourceUtils;
import melnorme.lang.ide.ui.editor.EditorUtils.OpenNewEditorMode;
import melnorme.lang.ide.ui.editor.LangEditorActionContributor;
import melnorme.lang.tooling.ast.SourceRange;
import melnorme.lang.tooling.structure.GlobalSourceStructure;
import melnorme.lang.tooling.structure.SourceFileStructure;
import melnorme.utilbox.concurrency.OperationCancellation;
import melnorme.utilbox.core.CommonException;
import melnorme.utilbox.misc.FileUtil;
import melnorme.utilbox.misc.Location;
import melnorme.utilbox.misc.StringUtil;

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
			boolean isFile = resource instanceof IFile;
			if(!isFile) {
				return true;
			}
			Location location = ResourceUtils.getResourceLocation(resource);
			switch(delta.getKind()) {
			case IResourceDelta.ADDED:
			case IResourceDelta.CHANGED:
				fileTouched(location);
				break;
			case IResourceDelta.REMOVED:
				fileRemoved(location);
				break;
			}
			return true;
		});
	}
	
	private static void fileRemoved(Location location) {
		System.out.println("RustEditorActionContributor - File removed: " + location);
		GlobalSourceStructure.fileRemoved(location);
	}
	
	private static void fileTouched(Location location) {
		System.out.println("RustEditorActionContributor - File touched: " + location);
		try {
			String source = FileUtil.readFileContents(location, StringUtil.UTF8);
			RustParseDescribeLauncher parseDescribeLauncher = new RustParseDescribeLauncher(LangCore.getToolManager(), () -> false);
			Optional<String> parseDescribeStdout = parseDescribeLauncher.getDescribeOutput(source, location);
			
			RustParseDescribeParser parseDescribeParser = new RustParseDescribeParser(location, source);
			SourceFileStructure fileStructure = parseDescribeParser.parse(parseDescribeStdout.get());
			
			if(fileStructure.getParserProblems().isEmpty()) {
				GlobalSourceStructure.fileTouched(location, fileStructure);
			}
		} catch(OperationCancellation | CommonException e) {
			throw new RuntimeException(e);
		}
	}
}