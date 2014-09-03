/*******************************************************************************
 * Copyright (c) 2014 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Nyßen (itemis AG) - initial API and implementation
 *     Matthias Wienand (itemis AG) - multi selection handles in root part
 *
 *******************************************************************************/
package org.eclipse.gef4.mvc.behaviors;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;

import org.eclipse.gef4.mvc.models.ISelectionModel;
import org.eclipse.gef4.mvc.parts.IContentPart;
import org.eclipse.gef4.mvc.parts.IRootPart;
import org.eclipse.gef4.mvc.parts.IVisualPart;

/**
 * The default selection behavior is responsible for creating and removing
 * selection feedback and handles.
 *
 * @author anyssen
 *
 * @param <VR>
 *            The visual root node of the UI toolkit this {@link IVisualPart} is
 *            used in, e.g. javafx.scene.Node in case of JavaFX.
 */
public class SelectionBehavior<VR> extends AbstractBehavior<VR> implements
		PropertyChangeListener {

	@Override
	public void activate() {
		super.activate();
		getHost().getRoot().getViewer().getSelectionModel()
				.addPropertyChangeListener(this);

		// create feedback and handles if we are already selected
		addFeedbackAndHandles(getHost().getRoot().getViewer()
				.getSelectionModel().getSelected());
	}

	protected void addFeedbackAndHandles(
			List<? extends IContentPart<VR>> selected) {
		// root is responsible for multi selection
		if (getHost() instanceof IRootPart && selected.size() > 1) {
			addFeedback(selected);
			addHandles(selected);
			// TODO: optimize performance (generating feedback and handles) as
			// this seems to slow down marquee selection
		} else if (selected.contains(getHost())) {
			addFeedback(Collections.singletonList(getHost()));
			if (selected.get(0) == getHost() && selected.size() <= 1) {
				addHandles(Collections.singletonList(getHost()));
			}
		}
	}

	@Override
	public void deactivate() {
		// remove any pending feedback
		removeFeedbackAndHandles(getHost().getRoot().getViewer()
				.getSelectionModel().getSelected());

		getHost().getRoot().getViewer().getSelectionModel()
				.removePropertyChangeListener(this);
		super.deactivate();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals(ISelectionModel.SELECTION_PROPERTY)) {
			List<IContentPart<VR>> oldSelection = (List<IContentPart<VR>>) event
					.getOldValue();
			List<IContentPart<VR>> newSelection = (List<IContentPart<VR>>) event
					.getNewValue();

			removeFeedbackAndHandles(oldSelection);
			addFeedbackAndHandles(newSelection);
		}
	}

	protected void removeFeedbackAndHandles(
			List<? extends IContentPart<VR>> selected) {
		// root is responsible for multi selection
		if (getHost() instanceof IRootPart && selected.size() > 1) {
			removeHandles(selected);
			removeFeedback(selected);
		} else if (selected.contains(getHost())) {
			removeHandles(Collections.singletonList(getHost()));
			removeFeedback(Collections.singletonList(getHost()));
		}
	}
}