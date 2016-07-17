package melnorme.lang.ide.core.engine;

import melnorme.lang.tooling.structure.SourceFileStructure;
import melnorme.lang.utils.concurrency.ConcurrentlyDerivedData;
import melnorme.utilbox.core.fntypes.CommonResult;

public abstract class StructureResult<SELF> extends ConcurrentlyDerivedData<CommonResult<SourceFileStructure>, SELF> {
}
