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
import org.eclipse.ui.texteditor.ITextEditor;

import com.github.rustdt.ide.core.operations.RustParseDescribeLauncher;
import com.github.rustdt.ide.ui.actions.RustOpenDefinitionOperation;
import com.github.rustdt.tooling.ops.RustParseDescribeParser;

import melnorme.lang.ide.core.AbstractLangCore;
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
		setupOpenType();
	}
	
	// TODO: Evaluate source structure in background thread.
	private void setupOpenType() {
		evaluateGlobalSourceStructure();
		listenToUpdatesOfGlobalSourceStructure();
	}
	
	private void evaluateGlobalSourceStructure() {
		try {
			ResourcesPlugin.getWorkspace().getRoot().accept(this::resourceTouched);
		} catch(Exception e) {
			AbstractLangCore.log().logError("Global source structure could not be determined", e);
		}
	}
	
	private boolean resourceTouched(IResource resource) {
		convertToRustFileLocation(resource).ifPresent(RustEditorActionContributor::fileTouched);
		return true;
	}
	
	private void listenToUpdatesOfGlobalSourceStructure() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this::processEvent, IResourceChangeEvent.POST_CHANGE);
	}
	
	private void processEvent(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(this::applyResourceDelta);
		} catch(Exception e) {
			AbstractLangCore.log().logError("Global source structure could not be determined", e);
		}
	}
	
	private boolean applyResourceDelta(IResourceDelta delta) {
		convertToRustFileLocation(delta.getResource()).ifPresent(location -> {
			switch(delta.getKind()) {
			case IResourceDelta.ADDED:
			case IResourceDelta.CHANGED:
				fileTouched(location);
				break;
			case IResourceDelta.REMOVED:
				fileRemoved(location);
				break;
			}
		});
		return true;
	}
	
	private static Optional<Location> convertToRustFileLocation(IResource resource) {
		return Optional.of(resource)
				.filter(res -> res instanceof IFile)
				.filter(RustEditorActionContributor::isValidRustPath)
				.map(ResourceUtils::getResourceLocation);
	}
	
	// TODO: Find a good filter
	private static boolean isValidRustPath(IResource resource) {
		String path = resource.getFullPath().toString();
		return path.contains("src") && path.endsWith(".rs");
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
			String parseDescribeStdout = parseDescribeLauncher.getDescribeOutput(source, location);
			
			RustParseDescribeParser parseDescribeParser = new RustParseDescribeParser(location, source);
			SourceFileStructure fileStructure = parseDescribeParser.parse(parseDescribeStdout);
			
			if(fileStructure.getParserProblems().isEmpty()) {
				GlobalSourceStructure.fileTouched(location, fileStructure);
			}
		} catch(OperationCancellation | CommonException e) {
			throw new RuntimeException(e);
		}
	}
}