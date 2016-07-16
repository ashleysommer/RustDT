package com.github.rustdt.ide.core.engine;

import com.github.rustdt.ide.core.operations.RustParseDescribeLauncher;
import com.github.rustdt.tooling.ops.RustParseDescribeParser;

import melnorme.lang.ide.core.LangCore;
import melnorme.lang.tooling.structure.SourceFileStructure;
import melnorme.lang.utils.concurrency.ConcurrentlyDerivedData;
import melnorme.lang.utils.concurrency.ConcurrentlyDerivedData.DataUpdateTask;
import melnorme.utilbox.collections.Indexable;
import melnorme.utilbox.concurrency.OperationCancellation;
import melnorme.utilbox.core.CommonException;
import melnorme.utilbox.misc.FileUtil;
import melnorme.utilbox.misc.Location;
import melnorme.utilbox.misc.StringUtil;

abstract class RustIndexUpdateTask extends DataUpdateTask<SourceFileStructure> {
	RustIndexUpdateTask(ConcurrentlyDerivedData<SourceFileStructure, ?> derivedData, String taskDisplayName) {
		super(derivedData, taskDisplayName);
	}
	
	@Override
	protected void handleRuntimeException(RuntimeException e) {
		LangCore.logInternalError(e);
	}
	
	static class RustIndexFileTouchedTask extends RustIndexUpdateTask {
		private final Location location;
		
		RustIndexFileTouchedTask(ConcurrentlyDerivedData<SourceFileStructure, ?> structureInfo, Location location) {
			super(structureInfo, "Updating index for " + location);
			this.location = location;
		}
		
		@Override
		protected SourceFileStructure createNewData() throws OperationCancellation {
			try {
				String source = FileUtil.readFileContents(location, StringUtil.UTF8);
				RustParseDescribeLauncher parseDescribeLauncher = new RustParseDescribeLauncher(
					LangCore.getToolManager(), this::isCancelled);
				String parseDescribeStdout = parseDescribeLauncher.getDescribeOutput(source, location);
				
				RustParseDescribeParser parseDescribeParser = new RustParseDescribeParser(location, source);
				SourceFileStructure fileStructure = parseDescribeParser.parse(parseDescribeStdout);
				
				if(fileStructure.getParserProblems().isEmpty() || !fileStructure.getChildren().isEmpty()) {
					return fileStructure;
				}
			} catch(CommonException e) {
				LangCore.logError("Could not parse file: " + location, e);
			}
			return new SourceFileStructure(location, Indexable.EMPTY_INDEXABLE, Indexable.EMPTY_INDEXABLE);
		}
	}
	
	static class RustIndexFileRemovedTask extends RustIndexUpdateTask {
		private final Location location;
		
		RustIndexFileRemovedTask(ConcurrentlyDerivedData<SourceFileStructure, ?> structureInfo, Location location) {
			super(structureInfo, "Removing index for " + location);
			this.location = location;
		}
		
		@Override
		protected SourceFileStructure createNewData() {
			return new SourceFileStructure(location, Indexable.EMPTY_INDEXABLE, Indexable.EMPTY_INDEXABLE);
		}
	}
}