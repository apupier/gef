/*******************************************************************************
 * Copyright (c) 2016 itemis AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tamas Miklossy (itemis AG) - initial implementation (bug #477980)
 *                                - Add support for polygon-based node shapes (bug #441352)
 *                                - modify grammar to allow empty attribute lists (bug #461506)
 *                                - Add support for all dot attributes (bug #461506)		
 *
 *******************************************************************************/
package org.eclipse.gef.dot.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.gef.dot.internal.DotFileUtils;
import org.eclipse.gef.dot.internal.parser.DotInjectorProvider;
import org.eclipse.gef.dot.internal.parser.dot.DotAst;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.junit4.util.ParseHelper;
import org.eclipse.xtext.junit4.validation.ValidationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

@RunWith(XtextRunner.class)
@InjectWith(DotInjectorProvider.class)
public class DotParserTests {

	@Inject
	private ParseHelper<DotAst> parserHelper;

	@Inject
	private ValidationTestHelper validationTestHelper;

	@Test
	public void testEmptyString() {
		try {
			DotAst dotAst = parserHelper.parse("");
			assertNull(dotAst);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testEmptyGraph() {
		testString(DotTestGraphs.EMPTY);
	}

	@Test
	public void testEmptyDirectedGraph() {
		testString(DotTestGraphs.EMPTY_DIRECTED);
	}

	@Test
	public void testEmptyStrictGraph() {
		testString(DotTestGraphs.EMPTY_STRICT);
	}

	@Test
	public void testEmptyStrictDirectedGraph() {
		testString(DotTestGraphs.EMPTY_STRICT_DIRECTED);
	}

	@Test
	public void testGraphWithOneNode() {
		testString(DotTestGraphs.ONE_NODE);
	}

	@Test
	public void testGraphWithOneNodeAndEmptyNodeAttributeList() {
		testString(DotTestGraphs.EMPTY_NODE_ATTRIBUTE_LIST);
	}

	@Test
	public void testGraphWithOneEdge() {
		testString(DotTestGraphs.ONE_EDGE);
	}

	@Test
	public void testDirectedGraphWithOneEdge() {
		testString(DotTestGraphs.ONE_DIRECTED_EDGE);
	}

	@Test
	public void testGraphWithOneEdgeAndEmptyEdgeAttributeList() {
		testString(DotTestGraphs.EMPTY_EDGE_ATTRIBUTE_LIST);
	}

	@Test
	public void testDirectedGraphWithOneEdgeAndEmptyEdgeAttributeList() {
		testString(DotTestGraphs.EMPTY_DIRECTED_EDGE_ATTRIBUTE_LIST);
	}

	@Test
	public void testGraphWithEmptyGraphAttributeStatement() {
		testString(DotTestGraphs.EMPTY_GRAPH_ATTRIBUTE_STATEMENT);
	}

	@Test
	public void testGraphWithEmptyNodeAttributeStatement() {
		testString(DotTestGraphs.EMPTY_NODE_ATTRIBUTE_STATEMENT);
	}

	@Test
	public void testGraphWithEmptyEdgeAttributeStatement() {
		testString(DotTestGraphs.EMPTY_EDGE_ATTRIBUTE_STATEMENT);
	}

	@Test
	public void testArrowShapesDeprecated() {
		testFile("arrowshapes_deprecated.dot");
	}

	@Test
	public void testArrowShapesDirectionBoth() {
		testFile("arrowshapes_direction_both.dot");
	}

	@Test
	public void testArrowShapesInvalidModifiers() {
		testFile("arrowshapes_invalid_modifiers.dot");
	}

	@Test
	public void testArrowShapesMultiple() {
		testFile("arrowshapes_multiple.dot");
	}

	@Test
	public void testArrowShapesSingle() {
		testFile("arrowshapes_single.dot");
	}

	@Test
	public void testAttributesGraph() {
		testFile("attributes_graph.dot");
	}

	@Test
	public void testBasicDirectedGraph() {
		testFile("basic_directed_graph.dot");
	}

	@Test
	public void testGlobalEdgeGraph() {
		testFile("global_edge_graph.dot");
	}

	@Test
	public void testGlobalNodeGraph() {
		testFile("global_node_graph.dot");
	}

	@Test
	public void testIdMatchesKeyword() {
		testFile("id_matches_keyword.dot");
	}

	@Test
	public void testLabeledGraph() {
		testFile("labeled_graph.dot");
	}

	@Test
	public void testLayoutGridGraph() {
		testFile("layout_grid_graph.dot");
	}

	@Test
	public void testLayoutRadialGraph() {
		testFile("layout_radial_graph.dot");
	}

	@Test
	public void testLayoutSpringGraph() {
		testFile("layout_spring_graph.dot");
	}

	@Test
	public void testLayoutTreeGraph() {
		testFile("layout_tree_graph.dot");
	}

	@Test
	public void testNodeShapesPolygonBased() {
		testFile("nodeshapes_polygon_based.dot");
	}

	@Test
	public void testNodeGroups() {
		testFile("node_groups.dot");
	}

	@Test
	public void testSampleInput() {
		testFile("sample_input.dot");
	}

	@Test
	public void testSimpleDigraph() {
		testFile("simple_digraph.dot");
	}

	@Test
	public void testSimpleGraph() {
		testFile("simple_graph.dot");
	}

	@Test
	public void testStyledGraph() {
		testFile("styled_graph.dot");
	}

	@Test
	public void testStyledGraph2() {
		testFile("styled_graph2.dot");
	}

	private void testFile(String fileName) {
		String fileContents = DotFileUtils
				.read(new File(DotTestUtils.RESOURCES_TESTS + fileName));
		testString(fileContents);
	}

	private void testString(String text) {
		try {
			DotAst dotAst = parserHelper.parse(text);
			assertNotNull(dotAst);
			validationTestHelper.assertNoErrors(dotAst);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
