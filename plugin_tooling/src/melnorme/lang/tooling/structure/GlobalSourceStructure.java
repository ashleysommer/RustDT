package melnorme.lang.tooling.structure;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import melnorme.utilbox.collections.ArrayList2;
import melnorme.utilbox.collections.HashSet2;
import melnorme.utilbox.collections.Indexable;

// TODO: Update type structure in background thread when the file is edited or when the workspace content changes.
public class GlobalSourceStructure {
	private static final Set<StructureElement> aggregatedElements = new TreeSet<>(
			Comparator.comparing((StructureElement el) -> el.getName())
					.thenComparing((StructureElement el) -> el.getLocation().map(loc -> loc.getPath()).orElse(null)));
	
	private static final Set<StructureElementKind> HIDDEN_ELEMENT_KINDS = Collections.unmodifiableSet(new HashSet2<>(
			StructureElementKind.USE_GROUP));
	
	// TODO: Flatten before filtering
	public static void addFileStructure(SourceFileStructure fileStructure) {
		for(StructureElement child : fileStructure.getChildren()) {
			if(!HIDDEN_ELEMENT_KINDS.contains(child.getKind())) {
				aggregatedElements.add(child.cloneSubTree());
			}
		}
	}
	
	// TODO: Find a way of reusing structure elements without cloning the whole structure.
	public static SourceFileStructure getGlobalSourceStructure() {
		Indexable<StructureElement> children = new ArrayList2<>(aggregatedElements.stream()
				.map((child) -> child.cloneSubTree()).collect(Collectors.toList()));
		return new SourceFileStructure(children, null);
	};
}
