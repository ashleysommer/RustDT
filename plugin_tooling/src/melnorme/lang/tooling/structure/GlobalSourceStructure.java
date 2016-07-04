package melnorme.lang.tooling.structure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import melnorme.utilbox.collections.ArrayList2;
import melnorme.utilbox.collections.Indexable;
import melnorme.utilbox.misc.Location;

public class GlobalSourceStructure {
	private static final ArrayList2<StructureElement> aggregatedElements = ArrayList2.create();
	private static final Map<StructureElement, Location> typeLocations = new HashMap<>();
	
	public static void fileUpdated(SourceFileStructure fileStructure, Location location) {
		Indexable<StructureElement> children = fileStructure.getChildren();
		aggregatedElements.addAll(children.toArrayList());
		for (StructureElement child : children) {
			typeLocations.put(child, location);
		}
	}
	
	public static SourceFileStructure getGlobalSourceStructure() {
		Indexable<StructureElement> children = aggregatedElements.map(StructureElement::cloneSubTree);
		return new SourceFileStructure(null, children, null) {
			@Override
			public Optional<Location> getLocation(IStructureElement structureElement) {
				return Optional.ofNullable(typeLocations.get(structureElement));
			}
		};
	};
}
