/**
 * 
 */
package net.sf.taverna.t2.component.ui.menu.component;

import static java.lang.String.format;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static net.sf.taverna.t2.component.ui.serviceprovider.ComponentServiceIcon.getIcon;
import static net.sf.taverna.t2.component.ui.util.Utils.refreshComponentServiceProvider;
import static org.apache.log4j.Logger.getLogger;

import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.SwingWorker;

import net.sf.taverna.t2.component.api.Component;
import net.sf.taverna.t2.component.api.RegistryException;
import net.sf.taverna.t2.component.api.Version;
import net.sf.taverna.t2.component.ui.panel.ComponentChooserPanel;
import net.sf.taverna.t2.component.ui.serviceprovider.ComponentServiceProviderConfig;
import net.sf.taverna.t2.workbench.file.FileManager;
import net.sf.taverna.t2.workflowmodel.ConfigurationException;
import net.sf.taverna.t2.workflowmodel.Dataflow;

import org.apache.log4j.Logger;

/**
 * @author alanrw
 * 
 */
public class ComponentDeleteAction extends AbstractAction {
	private static final String COMPONENT_PROBLEM_TITLE = "Component Problem";
	private static final String CONFIRM_MSG = "Are you sure you want to delete %s?";
	private static final String CONFIRM_TITLE = "Delete Component Confirmation";
	private static final String DELETE_COMPONENT_LABEL = "Delete component...";
	private static final String DELETE_FAILED_TITLE = "Component Deletion Error";
	private static final String FAILED_MSG = "Unable to delete %s: %s";
	private static final String OPEN_COMPONENT_MSG = "The component is open";
	private static final String TITLE = "Component choice";
	private static final String WHAT_COMPONENT_MSG = "Unable to determine component";
	private static final long serialVersionUID = -2992743162132614936L;
	private static final Logger logger = getLogger(ComponentDeleteAction.class);
	private static final FileManager fm = FileManager.getInstance();

	public ComponentDeleteAction() {
		super(DELETE_COMPONENT_LABEL, getIcon());
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		ComponentChooserPanel panel = new ComponentChooserPanel();
		int answer = showConfirmDialog(null, panel, TITLE, OK_CANCEL_OPTION);
		if (answer == OK_OPTION)
			doDelete(panel.getChosenComponent());
	}

	private void doDelete(final Component chosenComponent) {
		if (chosenComponent == null) {
			showMessageDialog(null, WHAT_COMPONENT_MSG,
					COMPONENT_PROBLEM_TITLE, ERROR_MESSAGE);
		} else if (componentIsInUse(chosenComponent)) {
			showMessageDialog(null, OPEN_COMPONENT_MSG,
					COMPONENT_PROBLEM_TITLE, ERROR_MESSAGE);
		} else if (showConfirmDialog(null,
				format(CONFIRM_MSG, chosenComponent.getName()), CONFIRM_TITLE,
				YES_NO_OPTION) == YES_OPTION)
			new SwingWorker<ComponentServiceProviderConfig, Object>() {
				@Override
				protected ComponentServiceProviderConfig doInBackground()
						throws Exception {
					return deleteComponent(chosenComponent);
				}

				@Override
				protected void done() {
					refresh(chosenComponent, this);
				}
			}.execute();
	}

	private ComponentServiceProviderConfig deleteComponent(Component component)
			throws RegistryException {
		ComponentServiceProviderConfig config = new ComponentServiceProviderConfig(
				component.getFamily());
		component.delete();
		return config;
	}

	protected void refresh(Component component,
			SwingWorker<ComponentServiceProviderConfig, Object> worker) {
		try {
			refreshComponentServiceProvider(worker.get());
		} catch (ExecutionException e) {
			logger.error("failed to delete component", e.getCause());
			showMessageDialog(
					null,
					format(FAILED_MSG, component.getName(), e.getCause()
							.getMessage()), DELETE_FAILED_TITLE, ERROR_MESSAGE);
		} catch (ConfigurationException e) {
			logger.error("failed to handle service panel update "
					+ "after deleting component", e);
		} catch (InterruptedException e) {
			logger.warn("interrupted during component deletion", e);
		}
	}

	private static boolean componentIsInUse(Component component) {
		for (Dataflow d : fm.getOpenDataflows()) {
			Object dataflowSource = fm.getDataflowSource(d);
			if (dataflowSource instanceof Version.ID
					&& ((Version.ID) dataflowSource).mostlyEqualTo(component))
				return true;
		}
		return false;
	}

}
