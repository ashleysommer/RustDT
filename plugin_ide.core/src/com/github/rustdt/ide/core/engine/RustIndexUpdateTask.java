package com.github.rustdt.ide.core.engine;

import com.github.rustdt.ide.core.operations.RustParseDescribeLauncher;
import com.github.rustdt.tooling.ops.RustParseDescribeParser;

import melnorme.lang.ide.core.LangCore;
import melnorme.lang.ide.core.engine.StructureResult;
import melnorme.lang.ide.core.engine.StructureUpdateTask;
import melnorme.lang.tooling.structure.SourceFileStructure;
import melnorme.utilbox.collections.Indexable;
import melnorme.utilbox.concurrency.OperationCancellation;
import melnorme.utilbox.core.CommonException;
import melnorme.utilbox.misc.FileUtil;
import melnorme.utilbox.misc.Location;
import melnorme.utilbox.misc.StringUtil;

abstract class RustIndexUpdateTask extends StructureUpdateTask {
	RustIndexUpdateTask(StructureResult derivedData, Location location) {
		super(derivedData, location);
	}
	
	@Override
	protected void handleRuntimeException(RuntimeException e) {
		LangCore.logInternalError(e);
	}
	
	static class RustIndexFileTouchedTask extends RustIndexUpdateTask {
		private final Location location;
		
		RustIndexFileTouchedTask(StructureResult structureResult, Location location) {
			super(structureResult, location);
			this.location = location;
		}
		
		@Override
		protected SourceFileStructure doCreateNewData() throws OperationCancellation, CommonException {
			String source = FileUtil.readFileContents(location, StringUtil.UTF8);
			RustParseDescribeLauncher parseDescribeLauncher = new RustParseDescribeLauncher(
				LangCore.getToolManager(), this::isCancelled);
			String parseDescribeStdout = parseDescribeLauncher.getDescribeOutput(source, location);
			
			RustParseDescribeParser parseDescribeParser = new RustParseDescribeParser(location, source);
			SourceFileStructure fileStructure = parseDescribeParser.parse(parseDescribeStdout);
			
			if(fileStructure.getParserProblems().isEmpty() || !fileStructure.getChildren().isEmpty()) {
				return fileStructure;
			} else {
				return new SourceFileStructure(location, Indexable.EMPTY_INDEXABLE, Indexable.EMPTY_INDEXABLE);
			}
		}
	}
	
	static class RustIndexFileRemovedTask extends RustIndexUpdateTask {
		private final Location location;
		
		RustIndexFileRemovedTask(StructureResult structureResult, Location location) {
			super(structureResult, location);
			this.location = location;
		}
		
		@Override
		protected SourceFileStructure doCreateNewData() {
			return new SourceFileStructure(location, Indexable.EMPTY_INDEXABLE, Indexable.EMPTY_INDEXABLE);
		}
	}
}