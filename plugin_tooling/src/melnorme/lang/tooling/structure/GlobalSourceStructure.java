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

import melnorme.utilbox.misc.Location;

public class GlobalSourceStructure {
	private static final EnumSet<StructureElementKind> HIDDEN_ELEMENT_KINDS = EnumSet.of(
		StructureElementKind.EXTERN_CRATE, StructureElementKind.USE_GROUP, StructureElementKind.USE,
		StructureElementKind.VAR);
	
	private final SortedMap<Location, SortedSet<StructureElement>> aggregatedElements = new TreeMap<>(
		comparing(Location::toPath));
	
	public synchronized void fileTouched(Location location, SourceFileStructure fileStructure) {
		SortedSet<StructureElement> elementsAtLocation = new TreeSet<>(comparing(StructureElement::getName));
		
		fileStructure.visitSubTree(el -> {
			if(!HIDDEN_ELEMENT_KINDS.contains(el.getKind())) {
				elementsAtLocation.add(el);
			}
		});
		
		aggregatedElements.put(location, elementsAtLocation);
	}
	
	public synchronized void fileRemoved(Location location) {
		aggregatedElements.remove(location);
	}
	
	public synchronized List<StructureElement> getGlobalSourceStructure() {
		return aggregatedElements
			.values()
			.stream()
			.flatMap(Set::stream)
			.collect(Collectors.toList());
	};
}
