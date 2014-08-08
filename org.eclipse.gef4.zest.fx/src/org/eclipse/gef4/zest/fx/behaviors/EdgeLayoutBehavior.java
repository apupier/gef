/*******************************************************************************
 * Copyright (c) 2014 itemis AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API & implementation
 *
 *******************************************************************************/
package org.eclipse.gef4.zest.fx.behaviors;

import javafx.geometry.Bounds;
import javafx.scene.Node;

import org.eclipse.gef4.graph.Edge;
import org.eclipse.gef4.mvc.parts.IContentPart;
import org.eclipse.gef4.zest.fx.layout.GraphEdgeLayout;
import org.eclipse.gef4.zest.fx.layout.GraphLayoutContext;

public class EdgeLayoutBehavior extends AbstractLayoutBehavior {

	protected GraphEdgeLayout edgeLayout;

	@Override
	protected void initializeLayout(GraphLayoutContext glc) {
		edgeLayout = glc.getEdgeLayout((Edge) ((IContentPart<Node>) getHost())
				.getContent());
	}

	@Override
	protected void onBoundsChange(Bounds oldBounds, Bounds newBounds) {
	}

	@Override
	protected void onFlushChanges() {
		getHost().refreshVisual();
	}

}