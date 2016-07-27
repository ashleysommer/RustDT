package melnorme.lang.tooling.structure;

import java.util.stream.Collectors;

import org.junit.Test;

import melnorme.lang.tooling.ast.SourceRange;
import melnorme.utilbox.collections.ArrayList2;
import melnorme.utilbox.collections.Indexable;
import melnorme.utilbox.core.CommonException;
import melnorme.utilbox.misc.Location;
import melnorme.utilbox.tests.CommonTest;

public class GlobalSourceStructureTest extends CommonTest {
	private final GlobalSourceStructure sourceStructure = new GlobalSourceStructure();
	
	@Test
	public void testInitialSourceStructure() throws Exception {
		assertStructureIs("");
	}
	
	@Test
	public void testEmptyListsAreNotAdded() throws Exception {
		sourceStructure.updateIndex(structure(Location.create("/test/path"), ArrayList2.create()));
		assertStructureIs("");
	}
	
	@Test
	public void testNonEmptyListsAreAdded() throws Exception {
		sourceStructure.updateIndex(structure(Location.create("/test/path"), ArrayList2.create(element("a"))));
		assertStructureIs("a");
	}
	
	@Test
	public void testEmptyStructuresRemoveAlreadyAddedStructures() throws Exception {
		sourceStructure.updateIndex(structure(Location.create("/test/path"), ArrayList2.create(element("a"))));
		sourceStructure.updateIndex(structure(Location.create("/test/path"), ArrayList2.create()));
		assertStructureIs("");
	}
	
	@Test
	public void testNonEmptyStructuresReplaceAlreadyAddedStructures() throws Exception {
		sourceStructure.updateIndex(structure(Location.create("/test/path"), ArrayList2.create(element("a"))));
		sourceStructure.updateIndex(structure(Location.create("/test/path"), ArrayList2.create(element("b"))));
		assertStructureIs("b");
	}
	
	@Test
	public void testEmptyStructuresDoNotRemoveStructuresFromDifferentPath() throws Exception {
		sourceStructure.updateIndex(structure(Location.create("/test/a"), ArrayList2.create(element("a"))));
		sourceStructure.updateIndex(structure(Location.create("/test/b"), ArrayList2.create()));
		assertStructureIs("a");
	}
	
	@Test
	public void testNonEmptyStructuresDoNotReplaceStructuresFromDifferentPath() throws Exception {
		sourceStructure.updateIndex(structure(Location.create("/test/a"), ArrayList2.create(element("a"))));
		sourceStructure.updateIndex(structure(Location.create("/test/b"), ArrayList2.create(element("b"))));
		assertStructureIs("ab");
		sourceStructure.updateIndex(structure(Location.create("/test/c"), ArrayList2.create(element("c"))));
		assertStructureIs("abc");
		sourceStructure.updateIndex(structure(Location.create("/test/d"), ArrayList2.create(element("d"))));
		assertStructureIs("abcd");
	}
	
	@Test
	public void testSourceStructureIsOrderedByPath() throws Exception {
		sourceStructure.updateIndex(structure(Location.create("/test/a"), ArrayList2.create(element("a"))));
		sourceStructure.updateIndex(structure(Location.create("/test/d"), ArrayList2.create(element("d"))));
		sourceStructure.updateIndex(structure(Location.create("/test/b"), ArrayList2.create(element("b"))));
		sourceStructure.updateIndex(structure(Location.create("/test/c"), ArrayList2.create(element("c"))));
		sourceStructure.updateIndex(structure(Location.create("/test/f"), ArrayList2.create(element("f"))));
		sourceStructure.updateIndex(structure(Location.create("/test/e"), ArrayList2.create(element("e"))));
		assertStructureIs("abcdef");
	}
	
	@Test
	public void testSoucreStructureIsOrderedByElementName() throws Exception {
		sourceStructure.updateIndex(structure(Location.create("/test/a"), ArrayList2.create(
			element("a"), element("d"), element("b"), element("c"), element("f"), element("e"))));
		assertStructureIs("abcdef");
	}
	
	@Test
	public void testSourceStructureIsFlattened() throws Exception {
		sourceStructure.updateIndex(structure(Location.create("/test/a"), ArrayList2.create(
			element("a"), element("b", element("c")), element("d"))));
		assertStructureIs("abcd");
	}
	
	
	private static SourceFileStructure structure(Location location, ArrayList2<StructureElement> children)
		throws CommonException {
		return new SourceFileStructure(location, children, Indexable.EMPTY_INDEXABLE);
	}
	
	private static StructureElement element(String name, StructureElement... children) {
		return new StructureElement(
			name, null, new SourceRange(0, 0), StructureElementKind.UNKNOWN, null, null, ArrayList2.create(children));
	}
	
	private void assertStructureIs(String expectedStructure) {
		String structure = sourceStructure.getGlobalSourceStructure()
			.stream()
			.map(StructureElement::getName)
			.collect(Collectors.joining());
		
		assertEquals(structure, expectedStructure);
	}
}
