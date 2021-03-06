/*******************************************************************************
 * Copyright (c) 2016 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.gef.mvc.examples.logo.policies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.gef.common.adapt.AdapterKey;
import org.eclipse.gef.geometry.planar.Dimension;
import org.eclipse.gef.geometry.planar.Point;
import org.eclipse.gef.mvc.examples.logo.model.GeometricShape;
import org.eclipse.gef.mvc.examples.logo.parts.GeometricModelPart;
import org.eclipse.gef.mvc.examples.logo.parts.GeometricShapePart;
import org.eclipse.gef.mvc.examples.logo.parts.PaletteElementPart;
import org.eclipse.gef.mvc.fx.domain.IDomain;
import org.eclipse.gef.mvc.fx.models.SelectionModel;
import org.eclipse.gef.mvc.fx.operations.DeselectOperation;
import org.eclipse.gef.mvc.fx.parts.IContentPart;
import org.eclipse.gef.mvc.fx.parts.IRootPart;
import org.eclipse.gef.mvc.fx.parts.IVisualPart;
import org.eclipse.gef.mvc.fx.policies.AbstractInteractionPolicy;
import org.eclipse.gef.mvc.fx.policies.CreationPolicy;
import org.eclipse.gef.mvc.fx.policies.IOnDragPolicy;
import org.eclipse.gef.mvc.fx.tools.ClickDragTool;
import org.eclipse.gef.mvc.fx.viewer.InfiniteCanvasViewer;
import org.eclipse.gef.mvc.fx.viewer.IViewer;

import com.google.common.collect.HashMultimap;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class CreateAndTranslateOnDragPolicy extends AbstractInteractionPolicy implements IOnDragPolicy {

	private GeometricShapePart createdShapePart;
	private Map<AdapterKey<? extends IOnDragPolicy>, IOnDragPolicy> dragPolicies;

	@Override
	public void abortDrag() {
		if (createdShapePart == null) {
			return;
		}

		// forward event to bend target part
		if (dragPolicies != null) {
			for (IOnDragPolicy dragPolicy : dragPolicies.values()) {
				dragPolicy.abortDrag();
			}
		}

		createdShapePart = null;
		dragPolicies = null;
	}

	@Override
	public void drag(MouseEvent event, Dimension delta) {
		if (createdShapePart == null) {
			return;
		}

		// forward drag events to bend target part
		if (dragPolicies != null) {
			for (IOnDragPolicy dragPolicy : dragPolicies.values()) {
				dragPolicy.drag(event, delta);
			}
		}
	}

	@Override
	public void endDrag(MouseEvent e, Dimension delta) {
		if (createdShapePart == null) {
			return;
		}

		// forward event to bend target part
		if (dragPolicies != null) {
			for (IOnDragPolicy dragPolicy : dragPolicies.values()) {
				dragPolicy.endDrag(e, delta);
			}
		}

		restoreRefreshVisuals(createdShapePart);
		createdShapePart = null;
		dragPolicies = null;
	}

	protected IViewer getContentViewer() {
		Map<AdapterKey<? extends IViewer>, IViewer> viewers = getHost().getRoot().getViewer().getDomain().getViewers();
		for (Entry<AdapterKey<? extends IViewer>, IViewer> e : viewers.entrySet()) {
			if (IDomain.CONTENT_VIEWER_ROLE.equals(e.getKey().getRole())) {
				return e.getValue();
			}
		}
		throw new IllegalStateException("Cannot find content viewer!");
	}

	@Override
	public PaletteElementPart getHost() {
		return (PaletteElementPart) super.getHost();
	}

	protected Point getLocation(MouseEvent e) {
		Point2D location = ((InfiniteCanvasViewer) getHost().getRoot().getViewer()).getCanvas().getContentGroup()
				.sceneToLocal(e.getSceneX(), e.getSceneY());
		return new Point(location.getX(), location.getY());
	}

	@Override
	public void hideIndicationCursor() {
	}

	@Override
	public boolean showIndicationCursor(KeyEvent event) {
		return false;
	}

	@Override
	public boolean showIndicationCursor(MouseEvent event) {
		return false;
	}

	@Override
	public void startDrag(MouseEvent event) {
		// find model part
		IRootPart<? extends Node> contentRoot = getContentViewer().getRootPart();
		IVisualPart<? extends Node> modelPart = contentRoot.getChildrenUnmodifiable().get(0);
		if (!(modelPart instanceof GeometricModelPart)) {
			throw new IllegalStateException("Cannot find GeometricModelPart.");
		}

		// copy the prototype
		GeometricShape copy = getHost().getContent().getCopy();
		// determine coordinates of prototype's origin in model coordinates
		Point2D localToScene = getHost().getVisual().localToScene(0, 0);
		Point2D originInModel = modelPart.getVisual().sceneToLocal(localToScene.getX(), localToScene.getY());
		// initially move to the originInModel
		double[] matrix = copy.getTransform().getMatrix();
		copy.getTransform().setTransform(matrix[0], matrix[1], matrix[2], matrix[3], originInModel.getX(),
				originInModel.getY());

		// create copy of host's geometry using CreationPolicy from root part
		CreationPolicy creationPolicy = contentRoot.getAdapter(CreationPolicy.class);
		init(creationPolicy);
		createdShapePart = (GeometricShapePart) creationPolicy.create(copy, (GeometricModelPart) modelPart,
				HashMultimap.<IContentPart<? extends Node>, String> create());
		commit(creationPolicy);

		// disable refresh visuals for the created shape part
		storeAndDisableRefreshVisuals(createdShapePart);

		// build operation to deselect all but the new part
		List<IContentPart<? extends Node>> toBeDeselected = new ArrayList<>(
				getContentViewer().getAdapter(SelectionModel.class).getSelectionUnmodifiable());
		toBeDeselected.remove(createdShapePart);
		DeselectOperation deselectOperation = new DeselectOperation(getContentViewer(), toBeDeselected);

		// execute on stack
		try {
			getHost().getRoot().getViewer().getDomain().execute(deselectOperation, new NullProgressMonitor());
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}

		// find drag target part
		dragPolicies = createdShapePart.getAdapters(ClickDragTool.ON_DRAG_POLICY_KEY);
		if (dragPolicies != null) {
			for (IOnDragPolicy dragPolicy : dragPolicies.values()) {
				dragPolicy.startDrag(event);
			}
		}
	}

}
