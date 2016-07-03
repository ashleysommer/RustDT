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
package melnorme.lang.tooling.structure;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertNotNull;
import static melnorme.utilbox.core.CoreUtil.areEqual;
import static melnorme.utilbox.core.CoreUtil.nullToEmpty;

import melnorme.lang.tooling.common.ParserError;
import melnorme.utilbox.collections.Indexable;
import melnorme.utilbox.misc.HashcodeUtil;

public abstract class SourceFileStructure_Default extends AbstractStructureContainer {
	
	protected final Indexable<ParserError> parserProblems;
	
	public SourceFileStructure_Default(Indexable<StructureElement> children,
			Indexable<ParserError> parserProblems) {
		super(children);
		this.parserProblems = nullToEmpty(parserProblems);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if(!(obj instanceof SourceFileStructure)) return false;
		
		SourceFileStructure other = (SourceFileStructure) obj;
		
		return
				areEqual(children, other.children);
	}
	
	@Override
	public int hashCode() {
		return HashcodeUtil.combinedHashCode(children);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
	
	/* -----------------  ----------------- */
	
	public Indexable<ParserError> getParserProblems() {
		return parserProblems;
	}
	
	public StructureElement getStructureElementAt(int offset) {
		return new StructureElementFinderByOffset(offset).findInnerMost(this);
	}
	
	/* ----------------- Utils ----------------- */
	
	public static class StructureElementFinderByOffset {
		
		protected final int offset;
		
		protected StructureElement pickedElement;
		
		public StructureElementFinderByOffset(int offset) {
			this.offset = offset;
		}
		
		// Note: This could be optimized if element tree is sorted
		public StructureElement findInnerMost(IStructureElementContainer container) {
			visitContainer(container);
			return pickedElement;
		}
		
		protected void visitContainer(IStructureElementContainer container) {
			assertNotNull(container);
			
			for(StructureElement childElement : container.getChildren()) {
				if(childElement.getSourceRange().inclusiveContains(offset)) {
					
					if(pickedElement == null) {
						pickedElement = childElement;
					} else if(pickedElement.getSourceRange().inclusiveContains(childElement.getSourceRange())) {
						// childElement has a more specific source-range, so it's a better match
						pickedElement = childElement;
					}
					
				}
				
				visitContainer(childElement); // Check children
			}
			
		}
		
	}
	
}