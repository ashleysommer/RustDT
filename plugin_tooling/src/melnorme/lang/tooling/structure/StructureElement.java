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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import melnorme.lang.tooling.ElementAttributes;
import melnorme.lang.tooling.LANG_SPECIFIC;
import melnorme.lang.tooling.ast.SourceRange;
import melnorme.utilbox.collections.Indexable;
import melnorme.utilbox.misc.Location;

@LANG_SPECIFIC
public class StructureElement extends StructureElement_Default {
	private final Optional<Location> location;
	
	public StructureElement(Optional<Location> location, String name, SourceRange nameSourceRange, SourceRange sourceRange,
			StructureElementKind elementKind, ElementAttributes elementAttributes, String type,
			Indexable<StructureElement> children) {
		super(name, nameSourceRange, sourceRange, elementKind, elementAttributes, type, children);
		this.location = location;
	}
	
	public Optional<Location> getLocation() {
		return location;
	}
	
	public StructureElement cloneTree() {
		return cloneWithChildren(cloneSubTree());
	}
	
	public List<StructureElement> flattenTree() {
		List<StructureElement> flattenedElements = new ArrayList<>();
		StructureElement rootElement = cloneWithChildren(Indexable.EMPTY_INDEXABLE);
		flattenedElements.add(rootElement);
		flattenedElements.addAll(flattenSubTree());
		return flattenedElements;
	}
	
	private StructureElement cloneWithChildren(Indexable<StructureElement> children) {
		return new StructureElement(location, name, nameSourceRange2, sourceRange, elementKind, elementAttributes, type, children);
	}
}