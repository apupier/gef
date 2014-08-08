/*******************************************************************************
 * Copyright (c) 2014 Fabian Steeg, hbz.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabian Steeg, hbz - initial API & implementation
 *
 *******************************************************************************/
package org.eclipse.gef4.zest.fx.ui.view;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.gef4.dot.DotExport;
import org.eclipse.gef4.dot.DotImport;
import org.eclipse.gef4.internal.dot.export.DotFileUtils;
import org.eclipse.gef4.zest.fx.ZestFxModule;
import org.eclipse.gef4.zest.fx.ui.ZestFxUiModule;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.google.inject.Guice;
import com.google.inject.util.Modules;

/**
 * Render DOT content with ZestFx and Graphviz
 *
 * @author Fabian Steeg (fsteeg)
 *
 */
public class DotGraphView extends ZestFxUiView {

	private static final String EXTENSION = "dot";

	private static final String LOAD_DOT_FILE = "Load *.dot file...";
	private static final String SYNC_EXPORT_PDF = "Sync with printable PDF using Graphviz";
	private static final String SYNC_IMPORT_DOT = "Sync with *.dot files in the workspace";
	private static final String DOT_SELECT_SHORT = "Where is 'dot'?";
	private static final String DOT_SELECT_LONG = "Please specify the folder that contains 'dot' (http://www.graphviz.org/)";
	private static final String NOT_FOUND_LONG = "The application 'dot' was not found in the specified directory";
	private static final String NOT_FOUND_SHORT = "Not found";
	private static final String FORMAT_PDF = "pdf";
	private boolean listenToDotContent = false;
	private boolean linkImage = false;
	private String currentDot = "digraph{}";
	private IFile currentFile = null;
	private ExportToggle exportAction;

	public DotGraphView() {
		super(Guice.createInjector(Modules.override(new ZestFxModule())//
				.with(new ZestFxUiModule())));
		setGraph(new DotImport(currentDot).newGraphInstance());
	}

	@Override
	public void createPartControl(final Composite parent) {
		exportAction = new ExportToggle();
		add(new UpdateToggle().action(this), ISharedImages.IMG_ELCL_SYNCED);
		add(new LoadFile().action(this), ISharedImages.IMG_OBJ_FILE);
		add(exportAction.action(this), ISharedImages.IMG_ETOOL_PRINT_EDIT);
		super.createPartControl(parent);
	}

	private void add(Action action, String imageName) {
		action.setId(action.getText());
		action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(imageName));
		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
		mgr.add(action);
	}

	private void setGraphAsync(final String dot) {
		final DotGraphView view = this;
		getViewSite().getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (!dot.trim().isEmpty()) {
					DotImport dotImport = new DotImport(dot);
					if (dotImport.getErrors().size() > 0) {
						System.err.println(String.format(
								"Could not import DOT: %s, DOT: %s",
								dotImport.getErrors(), dot));
						return;
					}
					setGraph(dotImport.newGraphInstance());
					exportAction.linkCorrespondingImage(view);
				}
			}
		});

	}

	private boolean toggle(Action action, boolean input) {
		action.setChecked(!action.isChecked());
		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
		for (IContributionItem item : mgr.getItems()) {
			if (item.getId() != null && item.getId().equals(action.getText())) {
				ActionContributionItem i = (ActionContributionItem) item;
				i.getAction().setChecked(!i.getAction().isChecked());
				return !input;
			}
		}
		return input;
	}

	private boolean updateGraph(IFile file) {
		if (file == null || file.getLocationURI() == null || !file.exists()) {
			return false;
		}
		String dotString = currentDot;
		try {
			dotString = DotFileUtils.read(DotFileUtils.resolve(file
					.getLocationURI().toURL()));
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
		currentDot = dotString;
		setGraphAsync(dotString);
		return true;
	}

	private IWorkspaceRunnable updateGraphRunnable(final IFile f) {
		if (!listenToDotContent
				&& !f.getLocation().toString().endsWith(EXTENSION)) {
			return null;
		}
		IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable() {
			@Override
			public void run(final IProgressMonitor monitor)
					throws CoreException {
				if (updateGraph(f)) {
					currentFile = f;
				}
			}
		};
		return workspaceRunnable;
	}

	/**
	 * Store and access the path to the 'dot' executable in the preference
	 * store. The path can be set by the user, using a directory selection
	 * dialog. The selected location is stored in the bundle's preferences and
	 * available from there after the initial setting.
	 */
	private static class DotDirStore {

		private DotDirStore() {/* Enforce non-instantiability */
		}

		/** @return The path to the folder containing the local 'dot' executable */
		public static String getDotDirPath() {
			if (dotPathFromPreferences().length() == 0) {
				setDotDirPath(); // set the preferences
			}
			return dotPathFromPreferences();
		}

		/** Sets the path to the local 'dot' executable based on user selection. */
		public static void setDotDirPath() {
			IWorkbenchWindow parent = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
			dialog.setMessage(DOT_SELECT_LONG);
			dialog.setText(DOT_SELECT_SHORT);
			processUserInput(parent, dialog);
		}

		private static boolean containsDot(final File folder) {
			String[] files = folder.list();
			for (int i = 0; i < files.length; i++) {
				if (files[i].equals("dot") || files[i].equals("dot.exe")) {
					return true;
				}
			}
			return false;
		}

		private static String dotPathFromPreferences() {
			return zestFxUiPrefs().get("dotpath", "");
		}

		private static void processUserInput(final IWorkbenchWindow parent,
				final DirectoryDialog dialog) {
			String selectedPath = dialog.open();
			if (selectedPath != null) {
				if (!containsDot(new File(selectedPath))) {
					MessageDialog.openError(parent.getShell(), NOT_FOUND_SHORT,
							NOT_FOUND_LONG);
				} else {
					Preferences preferences = zestFxUiPrefs();
					preferences.put("dotpath", selectedPath + File.separator);
					try {
						preferences.flush();
					} catch (BackingStoreException e) {
						e.printStackTrace();
					}
				}
			}
		}

		private static Preferences zestFxUiPrefs() {
			Preferences preferences = ConfigurationScope.INSTANCE
					.getNode("corg.eclipse.gef4");
			Preferences sub1 = preferences.node("zest.fx.ui");
			return sub1;
		}

	}

	private class ExportToggle {

		private File generateImageFromGraph(final boolean refresh,
				final String format, DotGraphView view) {
			DotExport dotExport = new DotExport(view.currentDot);
			File image = dotExport.toImage(DotDirStore.getDotDirPath(), format,
					null);
			if (view.currentFile == null) {
				return image;
			}
			try {
				URL url = view.currentFile.getParent().getLocationURI().toURL();
				File copy = DotFileUtils.copySingleFile(
						DotFileUtils.resolve(url), view.currentFile.getName()
						+ "." + format, image);
				return copy;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			if (refresh) {
				refreshParent(view.currentFile);
			}
			return image;
		}

		private void openFile(File file, DotGraphView view) {
			if (view.currentFile == null) { // no workspace file for cur. graph
				IFileStore fileStore = EFS.getLocalFileSystem().getStore(
						new Path(""));
				fileStore = fileStore.getChild(file.getAbsolutePath());
				if (!fileStore.fetchInfo().isDirectory()
						&& fileStore.fetchInfo().exists()) {
					IWorkbenchPage page = view.getSite().getPage();
					try {
						IDE.openEditorOnFileStore(page, fileStore);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				}
			} else {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IPath location = Path.fromOSString(file.getAbsolutePath());
				IFile copy = workspace.getRoot().getFileForLocation(location);
				IEditorRegistry registry = PlatformUI.getWorkbench()
						.getEditorRegistry();
				if (registry.isSystemExternalEditorAvailable(copy.getName())) {
					try {
						view.getViewSite()
								.getPage()
								.openEditor(
								new FileEditorInput(copy),
										IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				}
			}
		}

		private void refreshParent(final IFile file) {
			try {
				file.getParent().refreshLocal(IResource.DEPTH_ONE, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		Action action(final DotGraphView view) {
			return new Action(DotGraphView.SYNC_EXPORT_PDF, SWT.TOGGLE) {
				@Override
				public void run() {
					linkImage = toggle(this, linkImage);
					if (view.currentFile != null) {
						linkCorrespondingImage(view);
					}
				}
			};
		}

		void linkCorrespondingImage(DotGraphView view) {
			if (view.linkImage) {
				File image = generateImageFromGraph(true,
						DotGraphView.FORMAT_PDF, view);
				openFile(image, view);
			}
		}

	}

	private class LoadFile {

		Action action(final DotGraphView view) {
			return new Action(DotGraphView.LOAD_DOT_FILE) {
				@Override
				public void run() {
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
							.getRoot();
					ResourceListSelectionDialog dialog = new ResourceListSelectionDialog(
							view.getViewSite().getShell(), root, IResource.FILE);
					if (dialog.open() == Window.OK) {
						Object[] selected = dialog.getResult();
						if (selected != null) {
							view.updateGraph((IFile) selected[0]);
						}
					}
				}
			};
		}

	}

	private class UpdateToggle {

		/** Listener that passes a visitor if a resource is changed. */
		private IResourceChangeListener resourceChangeListener;

		/** If a *.dot file is visited, update the graph. */
		private IResourceDeltaVisitor resourceVisitor;

		Action action(final DotGraphView view) {

			Action toggleUpdateModeAction = new Action(
					DotGraphView.SYNC_IMPORT_DOT, SWT.TOGGLE) {

				@Override
				public void run() {
					listenToDotContent = toggle(this, listenToDotContent);
					toggleResourceListener();
				}

				private void toggleResourceListener() {
					IWorkspace workspace = ResourcesPlugin.getWorkspace();
					if (view.listenToDotContent) {
						workspace.addResourceChangeListener(
								resourceChangeListener,
								IResourceChangeEvent.POST_BUILD
								| IResourceChangeEvent.POST_CHANGE);
					} else {
						workspace
						.removeResourceChangeListener(resourceChangeListener);
					}
				}
			};

			resourceChangeListener = new IResourceChangeListener() {
				@Override
				public void resourceChanged(final IResourceChangeEvent event) {
					if (event.getType() != IResourceChangeEvent.POST_BUILD
							&& event.getType() != IResourceChangeEvent.POST_CHANGE) {
						return;
					}
					IResourceDelta rootDelta = event.getDelta();
					try {
						rootDelta.accept(resourceVisitor);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			};

			resourceVisitor = new IResourceDeltaVisitor() {
				@Override
				public boolean visit(final IResourceDelta delta) {
					IResource resource = delta.getResource();
					if (resource.getType() == IResource.FILE) {
						try {
							final IFile f = (IFile) resource;
							IWorkspaceRunnable workspaceRunnable = view
									.updateGraphRunnable(f);
							IWorkspace workspace = ResourcesPlugin
									.getWorkspace();
							if (!workspace.isTreeLocked()) {
								workspace.run(workspaceRunnable, null);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					return true;
				}

			};
			return toggleUpdateModeAction;
		}
	}
}