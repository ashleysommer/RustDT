package com.github.rustdt.ide.core.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;

import com.github.rustdt.ide.core.engine.RustIndexUpdateTask.RustIndexFileRemovedTask;
import com.github.rustdt.ide.core.engine.RustIndexUpdateTask.RustIndexFileTouchedTask;

import melnorme.lang.ide.core.LangCore;
import melnorme.lang.ide.core.engine.IndexManager;
import melnorme.lang.ide.core.utils.ResourceUtils;
import melnorme.lang.tooling.structure.GlobalSourceStructure;
import melnorme.lang.tooling.structure.SourceFileStructure;
import melnorme.lang.tooling.structure.StructureElement;
import melnorme.lang.utils.concurrency.ConcurrentlyDerivedData;
import melnorme.utilbox.misc.Location;

public class RustIndexManager extends IndexManager {
	private final GlobalSourceStructure sourceStructure = new GlobalSourceStructure();
	
	private final Map<Location, ConcurrentlyDerivedData<SourceFileStructure, ?>> startedIndexUpdates = new HashMap<>();
	
	private final IResourceChangeListener resourcesChanged = this::processResourceChangeEvent;
	
	public RustIndexManager() {
		evaluateGlobalSourceStructure();
		listenToUpdatesOfGlobalSourceStructure();
		asOwner().bind(this::stopListeningToUpdatesOfGlobalSourceStructure);
	}
	
	private void evaluateGlobalSourceStructure() {
		try {
			ResourcesPlugin.getWorkspace().getRoot().accept(RustIndexManager.this::addResource);
		} catch(Exception e) {
			LangCore.logError("Could not initialize Rust type index", e);
		}
	}
	
	private boolean addResource(IResource resource) {
		convertToRustFileLocation(resource).ifPresent(this::enqueueFileTouchedTask);
		return true;
	}
	
	private void listenToUpdatesOfGlobalSourceStructure() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourcesChanged, IResourceChangeEvent.POST_CHANGE);
	}
	
	private void stopListeningToUpdatesOfGlobalSourceStructure() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourcesChanged);
	}
	
	private void processResourceChangeEvent(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(RustIndexManager.this::applyResourceDelta);
		} catch(Exception e) {
			LangCore.logError("Could not update Rust type index", e);
		}
	}
	
	private boolean applyResourceDelta(IResourceDelta delta) {
		convertToRustFileLocation(delta.getResource()).ifPresent(location -> {
			switch(delta.getKind()) {
			case IResourceDelta.ADDED:
			case IResourceDelta.CHANGED:
				enqueueFileTouchedTask(location);
				break;
			case IResourceDelta.REMOVED:
				enqueueFileRemovedTask(location);
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
	
	private void enqueueFileRemovedTask(Location location) {
		System.out.println("RustIndexManager - File removed: " + location);
		
		ConcurrentlyDerivedData<SourceFileStructure, ?> sourceFileStructure = getOrCreateStructureInfo(location);
		RustIndexUpdateTask indexUpdateTask = new RustIndexFileRemovedTask(sourceFileStructure, location);
		
		sourceFileStructure.setUpdateTask(indexUpdateTask);
		executor.submitTask(indexUpdateTask);
	}
	
	private void enqueueFileTouchedTask(Location location) {
		System.out.println("RustIndexManager - File touched: " + location);
		
		ConcurrentlyDerivedData<SourceFileStructure, ?> structureInfo = getOrCreateStructureInfo(location);
		RustIndexUpdateTask indexUpdateTask = new RustIndexFileTouchedTask(structureInfo, location);
		
		structureInfo.setUpdateTask(indexUpdateTask);
		executor.submitTask(indexUpdateTask);
	}
	
	private ConcurrentlyDerivedData<SourceFileStructure, ?> getOrCreateStructureInfo(Location location) {
		ConcurrentlyDerivedData<SourceFileStructure, ?> structureInfo = startedIndexUpdates.get(location);
		if(structureInfo == null) {
			structureInfo = createStructureInfo();
			startedIndexUpdates.put(location, structureInfo);
		}
		return structureInfo;
	}
	
	private ConcurrentlyDerivedData<SourceFileStructure, ?> createStructureInfo() {
		return new ConcurrentlyDerivedData<SourceFileStructure, ConcurrentlyDerivedData<?, ?>>() {
			@Override
			protected void doHandleDataChanged() {
				sourceStructure.updateIndex(getStoredData());
			}
		};
	}
	
	@Override
	public List<StructureElement> getGlobalSourceStructure() {
		return sourceStructure.getGlobalSourceStructure();
	}
}
