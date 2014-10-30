/**
 * 
 */
package net.sf.taverna.t2.component.ui.menu.component;

import static net.sf.taverna.t2.component.ui.serviceprovider.ComponentServiceIcon.getIcon;
import static net.sf.taverna.t2.component.ui.util.Utils.currentDataflowIsComponent;
import static org.apache.log4j.Logger.getLogger;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.sf.taverna.t2.lang.observer.Observable;
import net.sf.taverna.t2.lang.observer.Observer;
import net.sf.taverna.t2.workbench.file.FileManager;
import net.sf.taverna.t2.workbench.file.events.FileManagerEvent;

import org.apache.log4j.Logger;

/**
 * @author alanrw
 */
public class ComponentCloseAction extends AbstractAction implements
		Observer<FileManagerEvent> {
	private static final long serialVersionUID = -153986599735293879L;
	private static final String CLOSE_COMPONENT = "Close component";
	@SuppressWarnings("unused")
	private static Logger logger = getLogger(ComponentCloseAction.class);

	private Action closeAction;

	public ComponentCloseAction(Action closeWorkflowAction, FileManager fm) {
		super(CLOSE_COMPONENT, getIcon());
		closeAction = closeWorkflowAction;
		fm.addObserver(this);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		closeAction.actionPerformed(arg0);
	}

	@Override
	public boolean isEnabled() {
		return currentDataflowIsComponent();
	}

	@Override
	public void notify(Observable<FileManagerEvent> sender,
			FileManagerEvent message) throws Exception {
		setEnabled(currentDataflowIsComponent());
	}
}
