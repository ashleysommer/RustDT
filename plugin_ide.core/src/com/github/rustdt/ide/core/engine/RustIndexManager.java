package com.github.rustdt.ide.core.engine;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.github.rustdt.ide.core.operations.RustParseDescribeLauncher;
import com.github.rustdt.tooling.ops.RustParseDescribeParser;

import melnorme.lang.ide.core.AbstractLangCore;
import melnorme.lang.ide.core.LangCore;
import melnorme.lang.ide.core.engine.IndexManager;
import melnorme.lang.ide.core.utils.ResourceUtils;
import melnorme.lang.tooling.structure.GlobalSourceStructure;
import melnorme.lang.tooling.structure.SourceFileStructure;
import melnorme.utilbox.concurrency.OperationCancellation;
import melnorme.utilbox.core.CommonException;
import melnorme.utilbox.misc.FileUtil;
import melnorme.utilbox.misc.Location;
import melnorme.utilbox.misc.StringUtil;

public class RustIndexManager extends IndexManager {
	private final IResourceChangeListener resourcesChanged = this::processEvent;
	
	public RustIndexManager() {
		evaluateGlobalSourceStructure();
		listenToUpdatesOfGlobalSourceStructure();
		asOwner().bind(this::stopListeningToUpdatesOfGlobalSourceStructure);
	}
	
	private void evaluateGlobalSourceStructure() {
		new Job("Searching types") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					ResourcesPlugin.getWorkspace().getRoot().accept(RustIndexManager.this::addResource);
				} catch(Exception e) {
					AbstractLangCore.log().logError("Global source structure could not be determined", e);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}
	
	private void listenToUpdatesOfGlobalSourceStructure() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourcesChanged, IResourceChangeEvent.POST_CHANGE);
	}
	
	private void stopListeningToUpdatesOfGlobalSourceStructure() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourcesChanged);
	}
	
	private boolean addResource(IResource resource) {
		convertToRustFileLocation(resource).ifPresent(RustIndexManager::fileTouched);
		return true;
	}
	
	private void processEvent(IResourceChangeEvent event) {
		new Job("Updating types") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					event.getDelta().accept(RustIndexManager.this::applyResourceDelta);
				} catch(Exception e) {
					AbstractLangCore.log().logError("Global source structure could not be determined", e);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
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
			.filter(RustIndexManager::isValidRustPath)
			.map(ResourceUtils::getResourceLocation);
	}
	
	private static boolean isValidRustPath(IResource resource) {
		String path = resource.getFullPath().toString();
		return path.endsWith(".rs");
	}
	
	private static void fileRemoved(Location location) {
		System.out.println("RustEditorActionContributor - File removed: " + location);
		GlobalSourceStructure.fileRemoved(location);
	}
	
	private static void fileTouched(Location location) {
		System.out.println("RustEditorActionContributor - File touched: " + location);
		try {
			String source = FileUtil.readFileContents(location, StringUtil.UTF8);
			RustParseDescribeLauncher parseDescribeLauncher = new RustParseDescribeLauncher(LangCore.getToolManager(),
				() -> false);
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
