/*******************************************************************************
 * Copyright (c) 2015 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package com.github.rustdt.ide.core.engine;

import java.util.Optional;

import com.github.rustdt.ide.core.operations.RustParseDescribeLauncher;
import com.github.rustdt.tooling.ops.RustParseDescribeParser;

import melnorme.lang.ide.core.LangCore;
import melnorme.lang.ide.core.engine.SourceModelManager;
import melnorme.lang.ide.core.operations.ToolManager;
import melnorme.lang.tooling.structure.GlobalSourceStructure;
import melnorme.lang.tooling.structure.SourceFileStructure;
import melnorme.lang.tooling.structure.StructureElement;
import melnorme.utilbox.collections.Indexable;
import melnorme.utilbox.concurrency.OperationCancellation;
import melnorme.utilbox.core.CommonException;
import melnorme.utilbox.misc.Location;

public class RustSourceModelManager extends SourceModelManager {
	
	protected final ToolManager toolManager = LangCore.getToolManager();
	
	public RustSourceModelManager() {
	}
	
	@Override
	protected StructureUpdateTask createUpdateTask(StructureInfo structureInfo, String source) {
		return new RustStructureUpdateTask(structureInfo, source);
	}
	
	public class RustStructureUpdateTask extends StructureUpdateTask {
		
		protected final String source;
		
		public RustStructureUpdateTask(StructureInfo structureInfo, String source) {
			super(structureInfo);
			this.source = source;
		}
		
		@Override
		protected SourceFileStructure doCreateNewData() throws CommonException, OperationCancellation {
			
			Location fileLocation = structureInfo.getLocation();
			
			Optional<String> describeOutput = new RustParseDescribeLauncher(toolManager, cm).getDescribeOutput(source, fileLocation);
			if(!describeOutput.isPresent()) {
				// Absent case only relevant if DevelopmentCodeMarkers.TESTS_MODE is set. Would like to remove this workaround.
				return null; // null means outline is removed.
			}
			
			try {
				RustParseDescribeParser parseDescribe = new RustParseDescribeParser(fileLocation, source);
				SourceFileStructure newStructure = parseDescribe.parse(describeOutput.get());
				
				boolean keepPreviousStructure = !newStructure.getParserProblems().isEmpty() && newStructure.getChildren().isEmpty();
				if(keepPreviousStructure) {
					SourceFileStructure previousStructure = structureInfo.getStoredData().getOrNull();
					if(previousStructure != null) {
						Indexable<StructureElement> previousElements = previousStructure.cloneSubTree();
						
						return new SourceFileStructure(previousElements, newStructure.getParserProblems());
					}
				}
				System.out.println("RoustSourceModelManager - File touched: " + fileLocation);
				GlobalSourceStructure.fileTouched(fileLocation, newStructure);
				return newStructure;
			} catch(CommonException ce) {
				throw new CommonException("Error reading parse-describe output:", ce.toStatusException());
				// toolManager.logAndNotifyError("Error reading parse-describe output:", ce.toStatusException());
			}
		}
	}
}