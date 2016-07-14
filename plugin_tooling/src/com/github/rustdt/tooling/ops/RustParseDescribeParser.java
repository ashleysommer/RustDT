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
package com.github.rustdt.tooling.ops;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertFail;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertNotNull;

import java.util.Iterator;

import melnorme.lang.tooling.EProtection;
import melnorme.lang.tooling.ElementAttributes;
import melnorme.lang.tooling.ast.ParserErrorTypes;
import melnorme.lang.tooling.ast.SourceRange;
import melnorme.lang.tooling.common.ParserError;
import melnorme.lang.tooling.parser.TextBlocksReader;
import melnorme.lang.tooling.parser.TextBlocksReader.BlockVisitorX;
import melnorme.lang.tooling.parser.TextBlocksReader.TextBlocksSubReader;
import melnorme.lang.tooling.structure.AbstractStructureParser;
import melnorme.lang.tooling.structure.SourceFileStructure;
import melnorme.lang.tooling.structure.StructureElement;
import melnorme.lang.tooling.structure.StructureElementKind;
import melnorme.lang.utils.parse.StringCharSource;
import melnorme.utilbox.collections.ArrayList2;
import melnorme.utilbox.core.CommonException;
import melnorme.utilbox.misc.Location;
import melnorme.utilbox.misc.StringUtil;
import melnorme.utilbox.status.Severity;

public class RustParseDescribeParser extends AbstractStructureParser {
	
	public RustParseDescribeParser(Location location, String source) {
		super(location, source);
	}
	
	protected void reportError(String pattern, Object... arguments) throws CommonException {
		CommonException ce = CommonException.fromMsgFormat(pattern, arguments);
		handleParseError(ce);
	}
	
	/** The default implementation throws, but it could just log the error. */
	protected void handleParseError(CommonException ce) throws CommonException {
		throw ce;
	}
	
	@Override
	public SourceFileStructure parse(String describeOutput) throws CommonException {
		TextBlocksReader reader = new TextBlocksReader(new StringCharSource(describeOutput));
		
		reader.expectText("RUST_PARSE_DESCRIBE");
		
		reader.consumeText();
		
		try(TextBlocksSubReader subReader = reader.enterBlock()) {
			return parseSourceFileStructure_Contents(subReader);
		}
	}
	
	protected SourceFileStructure parseSourceFileStructure_Contents(TextBlocksSubReader reader)
			throws CommonException {
		ArrayList2<ParserError> parserProblems;
		
		reader.expectText("MESSAGES");
		try(TextBlocksSubReader subReader = reader.enterBlock()) {
			parserProblems = parseSubElements(subReader, this::parseMessage);
		}
		
		ArrayList2<StructureElement> structureChildren = parseStructureElements(reader);
		
		return new SourceFileStructure(location, structureChildren, parserProblems);
	}
	
	/* -----------------  ----------------- */
	
	protected ArrayList2<StructureElement> parseStructureElements(TextBlocksSubReader subReader)
			throws CommonException {
		ArrayList2<StructureElement> elements = parseSubElements(subReader, this::parseStructureElement);
		
		ArrayList2<StructureElement> reorganizedChildren = new ArrayList2<>();
		reorganizeChildren(reorganizedChildren, elements.iterator());
		return reorganizedChildren;
	}
	
	protected <RET> ArrayList2<RET> parseSubElements(TextBlocksSubReader subReader,
			BlockVisitorX<RET, CommonException> elementParser) throws CommonException {
		ArrayList2<RET> children = new ArrayList2<>();
		
		while(!subReader.aheadIsEnd()) {
			RET element = elementParser.consumeChildren(subReader);
			children.add(element);
		}
		
		return children;
	}
	
	protected void reorganizeChildren(ArrayList2<StructureElement> reorganizedElems, Iterator<StructureElement> iter) {
		
		ArrayList2<StructureElement> useElements = null;
		
		while(iter.hasNext()) {
			StructureElement child = iter.next();
			if(child.getKind() == StructureElementKind.USE) {
				if(useElements == null) {
					useElements = new ArrayList2<>();
				}
				useElements.add(child);
			}
			
			if(child.getKind() != StructureElementKind.USE || !iter.hasNext()) {
				if(useElements != null) {
					int startPos = useElements.get(0).getSourceRange().getStartPos();
					int endPos = useElements.get(useElements.size()-1).getSourceRange().getEndPos();
					SourceRange sr = SourceRange.srStartToEnd(startPos, endPos);
					StructureElement aggregatedUses = new StructureElement("use declarations", 
						SourceRange.srStartToEnd(startPos, startPos), sr, 
						StructureElementKind.USE_GROUP, null, null, useElements);
					useElements = null;
					
					reorganizedElems.add(aggregatedUses);
				}
			}
			
			if(child.getKind() != StructureElementKind.USE) {
				reorganizedElems.add(child);
			}
		}
	}
	
	public ParserError parseMessage(TextBlocksSubReader topReader) throws CommonException {
		try(TextBlocksSubReader reader = topReader.enterBlock()) {
			String messageSeverity = reader.consumeText();
			Severity severity = parseSeverity(messageSeverity);
			SourceRange sourceRange  = parseSourceRange(reader);
			String messageText = reader.consumeText();
			return new ParserError(ParserErrorTypes.GENERIC_ERROR, severity, sourceRange, messageText, null);
		}
	}
	
	public Severity parseSeverity(String severity) throws CommonException {
		try {
			return Severity.fromString(severity);
		} catch(CommonException e) {
			reportError("Invalid message severity `{0}`.", severity);
			return Severity.ERROR;
		}
	}
	
	protected StructureElement parseStructureElement(TextBlocksSubReader reader) throws CommonException {
		String item = reader.consumeText();
		try(TextBlocksSubReader contentsReader = reader.enterBlock()) {
			StructureElement element = consumeStructureElement(contentsReader, item);
			assertNotNull(element);
			return element;
		}
	}
	
	protected StructureElement consumeStructureElement(TextBlocksSubReader reader, String kind)
			throws CommonException {
		StructureElementKind elementKind = parseElementKind(kind);
		
		String name = reader.consumeText();
		SourceRange sourceRange  = parseSourceRange(reader);
		if(sourceRange == null) {
			reportError("Empty source range.");
			sourceRange = SourceRange.srStartToEnd(0, 0);
		}
		SourceRange nameSourceRange = parseSourceRange(reader);
		if(nameSourceRange == null) {
			int start = sourceRange.getStartPos();
			nameSourceRange = SourceRange.srStartToEnd(start, start);
		}
		
		String type = parseElementType(reader);
		ElementAttributes elementAttributes = parseElementAttributes(reader);
		
		ArrayList2<StructureElement> children = parseStructureElements(reader);
		
		return new StructureElement(name, nameSourceRange, sourceRange, elementKind, 
			elementAttributes, type, children);
	}
	
	public SourceRange parseSourceRange(TextBlocksReader reader) throws CommonException {
		if(reader.aheadIsEnd()) {
			reportError("Missing source range.");
			return SourceRange.srStartToEnd(0, 0);
		}
		if(!reader.aheadIsBlockStart()) {
			reportError("Invalid source range.");
			return SourceRange.srStartToEnd(0, 0);
		}
		
		return reader.consumeBlock((subReader) -> {
			return parseSourceRangeContents(subReader);
		});
	}
	
	public SourceRange parseSourceRangeContents(TextBlocksSubReader subReader) throws CommonException {
		if(subReader.aheadIsEnd()) {
			return null;
		}
		int start = parseSourceLocation(subReader.consumeText());
		int end = !subReader.aheadIsEnd() ?
				parseSourceLocation(subReader.consumeText()) :
				start;
		return SourceRange.srStartToEnd(start, end);
	}
	
	public StructureElementKind parseElementKind(String kindText) throws CommonException {
		kindText = kindText.toUpperCase();
		switch (kindText) {
		case "CRATE":
		case "EXTERNCRATE":
			kindText = StructureElementKind.EXTERN_CRATE.toString();
			break;
		case "TYPEALIAS":
			kindText = StructureElementKind.TYPE_ALIAS.toString();
			break;
		case "ENUMVARIANT":
		case "ENUMMEMBER":
			kindText = StructureElementKind.ENUM_VARIANT.toString();
			break;
		}
		
		try {
			return StructureElementKind.valueOf(kindText);
		} catch(IllegalArgumentException e) {
			reportError("Unknown element kind `{0}`.", kindText);
			return StructureElementKind.UNKNOWN;
		}
	}
	
	public ElementAttributes parseElementAttributes(TextBlocksSubReader reader) throws CommonException {
		
		return reader.consumeBlock((subReader) -> {
			EProtection prot = null;
			if(reader.aheadIsText()) {
				String protText = reader.consumeText();
				prot = parseProt_PubPriv(protText);
			}
			
			if(!subReader.aheadIsEnd()) {
//				reportError("Unknow attribute `{0}`.", consumeText);
				reportError("Unknown attribute.");
				subReader.skipToEnd();
			}
			return new ElementAttributes(prot);
		});
		
	}
	
	public EProtection parseProt_PubPriv(String consumeText) throws CommonException {
		String attribute = consumeText.toLowerCase();
		switch (attribute) {
		case "pub":
		case "public":
			return EProtection.PUBLIC;
		case "priv":
		case "private":
			return EProtection.PRIVATE;
		default:
			reportError("Unknown protection `{0}`.", consumeText);
			return null;
		}
	}
	
	/* -----------------  ----------------- */
	
	public String parseElementType(TextBlocksSubReader reader) throws CommonException {
		if(reader.aheadIsText()) {
			String type = reader.consumeText();
			return StringUtil.emptyAsNull(type);
		}
		if(reader.aheadIsBlockStart()) {
			reader.consumeBlock((subReader) -> {
				if(!subReader.aheadIsEnd()) {
					reportError("Unknown element type.");
				}
				return null;
			});
			return null;
		}
		reportError("Expected element type, {0}.", reader.errorAtTokenStart());
		return null;
	}
	
	protected void unimplemented() {
		assertFail("BLAH");
	}
	
}