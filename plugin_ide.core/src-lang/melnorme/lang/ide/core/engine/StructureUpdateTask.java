package melnorme.lang.ide.core.engine;

import melnorme.lang.ide.core.LangCore;
import melnorme.lang.ide.core.engine.SourceModelManager.StructureInfo;
import melnorme.lang.tooling.structure.SourceFileStructure;
import melnorme.lang.utils.concurrency.ConcurrentlyDerivedData.DataUpdateTask;
import melnorme.utilbox.concurrency.OperationCancellation;
import melnorme.utilbox.core.CommonException;
import melnorme.utilbox.core.fntypes.CommonResult;
import melnorme.utilbox.misc.Location;

public abstract class StructureUpdateTask extends DataUpdateTask<CommonResult<SourceFileStructure>> {
	public StructureUpdateTask(StructureResult structureResult, Location location) {
		super(structureResult, location.toString());
	}
	
	public StructureUpdateTask(StructureInfo structureInfo) {
		super(structureInfo, structureInfo.getKey2().toString());
	}
	
	@Override
	protected void handleRuntimeException(RuntimeException e) {
		LangCore.logInternalError(e);
	}
	
	@Override
	protected final CommonResult<SourceFileStructure> createNewData() throws OperationCancellation {
		try {
			return new CommonResult<>(doCreateNewData());
		} catch(CommonException e) {
			return new CommonResult<>(null, e);
		}
	}
	
	protected abstract SourceFileStructure doCreateNewData() throws CommonException, OperationCancellation;
}