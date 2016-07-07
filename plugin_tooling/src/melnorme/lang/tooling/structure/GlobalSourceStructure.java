package melnorme.lang.tooling.structure;

import static java.util.Comparator.comparing;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import melnorme.utilbox.collections.ArrayList2;
import melnorme.utilbox.misc.Location;

// TODO: Update type structure in background thread when the file is edited or when the workspace content changes.
public class GlobalSourceStructure {
	private static final SortedMap<Location, SortedSet<StructureElement>> aggregatedElements = new TreeMap<>(comparing(Location::toPath));
	
	private static final EnumSet<StructureElementKind> HIDDEN_ELEMENT_KINDS = EnumSet.of(StructureElementKind.EXTERN_CRATE,
			StructureElementKind.USE_GROUP, StructureElementKind.USE, StructureElementKind.VAR);
	
	public static void fileUpdated(Location location, SourceFileStructure fileStructure) {
		SortedSet<StructureElement> elementsAtLocation = new TreeSet<>(comparing(StructureElement::getName));
		
		fileStructure.flattenSubTree()
				.stream()
				.filter(el -> !HIDDEN_ELEMENT_KINDS.contains(el.getKind()))
				.forEach(elementsAtLocation::add);
		
		aggregatedElements.put(location, elementsAtLocation);
	}
	
	public static void fileRemoved(Location location) {
		aggregatedElements.remove(location);
	}
	
	// TODO: Find a way of reusing structure elements without cloning the whole structure.
	public static SourceFileStructure getGlobalSourceStructure() {
		List<StructureElement> children = aggregatedElements
				.values()
				.stream()
				.flatMap(Set::stream)
				.map(StructureElement::cloneSubTree)
				.collect(Collectors.toList());
		
		return new SourceFileStructure(new ArrayList2<>(children), null);
	};
}
