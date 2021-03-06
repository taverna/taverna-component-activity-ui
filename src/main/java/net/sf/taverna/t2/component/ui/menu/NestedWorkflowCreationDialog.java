/**
 * 
 */
package net.sf.taverna.t2.component.ui.menu;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static java.util.Collections.sort;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static net.sf.taverna.t2.component.ui.menu.component.ComponentServiceCreatorAction.copyProcessor;
import static net.sf.taverna.t2.component.ui.menu.component.ComponentServiceCreatorAction.pasteProcessor;
import static net.sf.taverna.t2.workbench.views.graph.GraphViewComponent.graphControllerMap;
import static net.sf.taverna.t2.workflowmodel.utils.Tools.uniqueObjectName;
import static net.sf.taverna.t2.workflowmodel.utils.Tools.uniqueProcessorName;
import static org.apache.log4j.Logger.getLogger;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;

import net.sf.taverna.t2.activities.dataflow.DataflowActivity;
import net.sf.taverna.t2.annotation.AnnotationBeanSPI;
import net.sf.taverna.t2.annotation.annotationbeans.AbstractTextualValueAssertion;
import net.sf.taverna.t2.annotation.annotationbeans.DescriptiveTitle;
import net.sf.taverna.t2.lang.ui.DeselectingButton;
import net.sf.taverna.t2.workbench.edits.EditManager;
import net.sf.taverna.t2.workbench.helper.HelpEnabledDialog;
import net.sf.taverna.t2.workbench.models.graph.GraphController;
import net.sf.taverna.t2.workflowmodel.CompoundEdit;
import net.sf.taverna.t2.workflowmodel.Condition;
import net.sf.taverna.t2.workflowmodel.Dataflow;
import net.sf.taverna.t2.workflowmodel.DataflowInputPort;
import net.sf.taverna.t2.workflowmodel.DataflowOutputPort;
import net.sf.taverna.t2.workflowmodel.Datalink;
import net.sf.taverna.t2.workflowmodel.Edit;
import net.sf.taverna.t2.workflowmodel.EditException;
import net.sf.taverna.t2.workflowmodel.Edits;
import net.sf.taverna.t2.workflowmodel.EventForwardingOutputPort;
import net.sf.taverna.t2.workflowmodel.EventHandlingInputPort;
import net.sf.taverna.t2.workflowmodel.Merge;
import net.sf.taverna.t2.workflowmodel.MergeInputPort;
import net.sf.taverna.t2.workflowmodel.MergeOutputPort;
import net.sf.taverna.t2.workflowmodel.OutputPort;
import net.sf.taverna.t2.workflowmodel.Processor;
import net.sf.taverna.t2.workflowmodel.ProcessorInputPort;
import net.sf.taverna.t2.workflowmodel.ProcessorOutputPort;
import net.sf.taverna.t2.workflowmodel.TokenProcessingEntity;
import net.sf.taverna.t2.workflowmodel.impl.DataflowImpl;
import net.sf.taverna.t2.workflowmodel.processor.activity.ActivityConfigurationException;
import net.sf.taverna.t2.workflowmodel.processor.activity.ActivityInputPort;
import net.sf.taverna.t2.workflowmodel.utils.AnnotationTools;

import org.apache.log4j.Logger;

/**
 * @author alanrw
 * 
 */
public class NestedWorkflowCreationDialog extends HelpEnabledDialog {
	private static final long serialVersionUID = 727059218457420449L;
	private static final Logger logger = getLogger(NestedWorkflowCreationDialog.class);
	private static final EditManager em = EditManager.getInstance();
	private static final Edits edits = em.getEdits();
	private static final AnnotationTools at = new AnnotationTools();
	private final List<TokenProcessingEntity> includedProcessors = new ArrayList<TokenProcessingEntity>();
	private List<? extends Processor> allProcessors;
	private final List<TokenProcessingEntity> includableProcessors = new ArrayList<TokenProcessingEntity>();
	private static final Comparator<TokenProcessingEntity> processorComparator = new Comparator<TokenProcessingEntity>() {
		@Override
		public int compare(TokenProcessingEntity o1, TokenProcessingEntity o2) {
			return o1.getLocalName().compareTo(o2.getLocalName());
		}
	};
	private static final ListCellRenderer defaultRenderer = new DefaultListCellRenderer();
	private static final ListCellRenderer processorRenderer = new ListCellRenderer() {
		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			TokenProcessingEntity p = (TokenProcessingEntity) value;
			return defaultRenderer.getListCellRendererComponent(list,
					p.getLocalName(), index, isSelected, cellHasFocus);
		}
	};

	private JList includableList = new JList();
	private JList includedList = new JList();
	private final Dataflow currentDataflow;
	private JButton excludeButton;
	private JButton includeButton;
	private JButton okButton;
	private JButton resetButton;
	private JTextField nameField = new JTextField(30);

	public NestedWorkflowCreationDialog(Frame owner, Object o, Dataflow dataflow) {
		super(owner, "Nested workflow creation", true, null);

		if (o instanceof Processor)
			includedProcessors.add((TokenProcessingEntity) o);
		this.currentDataflow = dataflow;

		allProcessors = dataflow.getProcessors();

		this.setLayout(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());

		okButton = new DeselectingButton(new OKAction(this));
		buttonPanel.add(okButton);

		resetButton = new DeselectingButton(new ResetAction(this));
		buttonPanel.add(resetButton);

		JButton cancelButton = new DeselectingButton(new CancelAction(this));
		buttonPanel.add(cancelButton);

		JPanel innerPanel = new JPanel(new BorderLayout());
		JPanel processorChoice = createProcessorChoicePanel(dataflow);
		innerPanel.add(processorChoice, CENTER);

		JPanel namePanel = new JPanel(new FlowLayout());
		namePanel.add(new JLabel("Workflow name: "));
		nameField.setText("nested");
		namePanel.add(nameField);
		innerPanel.add(namePanel, SOUTH);

		this.add(innerPanel, CENTER);

		this.add(buttonPanel, SOUTH);
		this.pack();
		this.setSize(new Dimension(500, 800));
	}

	private JPanel createProcessorChoicePanel(Dataflow dataflow) {
		JPanel result = new JPanel();
		result.setLayout(new GridLayout(0, 2));

		JPanel includedProcessorsPanel = createIncludedProcessorsPanel();
		JPanel includableProcessorsPanel = createIncludableProcessorsPanel();
		result.add(includableProcessorsPanel);
		result.add(includedProcessorsPanel);
		updateLists();
		return result;
	}

	private JPanel createIncludableProcessorsPanel() {
		JPanel result = new JPanel();
		result.setLayout(new BorderLayout());
		result.add(new JLabel("Possible services"), NORTH);
		includableList.setModel(new DefaultComboBoxModel(includableProcessors
				.toArray()));
		includableList.setCellRenderer(processorRenderer);
		result.add(new JScrollPane(includableList), CENTER);

		includeButton = new DeselectingButton("Include", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (Object o : includableList.getSelectedValues())
					if (o instanceof TokenProcessingEntity)
						includedProcessors.add((TokenProcessingEntity) o);
				calculateIncludableProcessors();
				updateLists();
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		buttonPanel.add(includeButton);
		result.add(buttonPanel, SOUTH);
		return result;
	}

	private void resetLists() {
		includedProcessors.clear();
		updateLists();
	}

	private JPanel createIncludedProcessorsPanel() {
		JPanel result = new JPanel();
		result.setLayout(new BorderLayout());
		result.add(new JLabel("Included services"), NORTH);
		includedList.setModel(new DefaultComboBoxModel(includedProcessors
				.toArray()));
		includedList.setCellRenderer(processorRenderer);
		result.add(new JScrollPane(includedList), CENTER);

		excludeButton = new DeselectingButton("Exclude", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (Object o : includedList.getSelectedValues())
					if (o instanceof TokenProcessingEntity)
						includedProcessors.remove((TokenProcessingEntity) o);
				calculateIncludableProcessors();
				updateLists();
			}
		});
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		buttonPanel.add(excludeButton);

		result.add(buttonPanel, SOUTH);
		return result;
	}

	private void updateLists() {
		calculateIncludableProcessors();
		sort(includedProcessors, processorComparator);
		sort(includableProcessors, processorComparator);
		includedList.setModel(new DefaultComboBoxModel(includedProcessors
				.toArray()));
		includableList.setModel(new DefaultComboBoxModel(includableProcessors
				.toArray()));
		boolean someIncludedProcessors = includedProcessors.size() > 0;
		excludeButton.setEnabled(someIncludedProcessors);
		okButton.setEnabled(someIncludedProcessors);
		resetButton.setEnabled(someIncludedProcessors);
		boolean someIncludableProcessors = includableProcessors.size() > 0;
		includeButton.setEnabled(someIncludableProcessors);
	}

	public void calculateIncludableProcessors() {
		includableProcessors.clear();
		if (includedProcessors.isEmpty())
			includableProcessors.addAll(allProcessors);
		else
			for (TokenProcessingEntity p : includedProcessors) {
				considerNearestUpstream(p);
				considerNearestDownstream(p);
			}
		sort(includableProcessors, processorComparator);
	}

	private void considerNearestDownstream(TokenProcessingEntity investigate) {
		if (investigate instanceof Processor)
			for (Condition condition : ((Processor) investigate)
					.getControlledPreconditionList())
				considerInclusion(condition.getTarget());

		for (EventForwardingOutputPort outputPort : investigate
				.getOutputPorts())
			for (Datalink datalink : outputPort.getOutgoingLinks()) {
				EventHandlingInputPort sink = datalink.getSink();
				if (sink instanceof ProcessorInputPort)
					considerInclusion(((ProcessorInputPort) sink)
							.getProcessor());
				else if (sink instanceof MergeInputPort)
					considerInclusion(((MergeInputPort) sink).getMerge());
				// The merge it self doesn't count as a processor
				else {
					// Ignore dataflow ports
				}
			}
	}

	private void considerNearestUpstream(TokenProcessingEntity investigate) {
		if (investigate instanceof Processor)
			for (Condition condition : ((Processor) investigate)
					.getPreconditionList())
				considerInclusion(condition.getControl());
		for (EventHandlingInputPort inputPort : investigate.getInputPorts()) {
			Datalink incomingLink = inputPort.getIncomingLink();
			if (incomingLink == null)
				continue;
			EventForwardingOutputPort source = incomingLink.getSource();
			if (source instanceof ProcessorOutputPort)
				considerInclusion(((ProcessorOutputPort) source).getProcessor());
			else if (source instanceof MergeOutputPort)
				considerInclusion(((MergeOutputPort) source).getMerge());
			else {
				// Ignore
			}
		}
	}

	private void considerInclusion(TokenProcessingEntity p) {
		if (!includedProcessors.contains(p)
				&& !includableProcessors.contains(p))
			includableProcessors.add(p);
	}

	private void createNestedWorkflow(ActionEvent e) {
		final List<Edit<?>> currentWorkflowEditList = new ArrayList<Edit<?>>();
		Map<Object, Object> oldNewMapping = new HashMap<Object, Object>();
		Map<Datalink, String> linkProcessorPortMapping = new HashMap<Datalink, String>();
		Map<EventForwardingOutputPort, DataflowOutputPort> outputPortMap = new HashMap<EventForwardingOutputPort, DataflowOutputPort>();
		Map<EventHandlingInputPort, DataflowInputPort> inputPortMap = new HashMap<EventHandlingInputPort, DataflowInputPort>();

		Processor nestingProcessor = createNestingProcessor(currentWorkflowEditList);

		Dataflow nestedDataflow = createNestedDataflow(e);

		transferProcessors(currentWorkflowEditList, oldNewMapping,
				nestedDataflow);
		transferDatalinks(oldNewMapping, linkProcessorPortMapping,
				outputPortMap, inputPortMap, nestedDataflow);
		transferConditions(currentWorkflowEditList, oldNewMapping,
				nestingProcessor);
		addDataflowToNestingProcessor(nestingProcessor, nestedDataflow);
		currentWorkflowEditList.add(edits.getAddProcessorEdit(currentDataflow,
				nestingProcessor));
		createDatalinkEdits(currentWorkflowEditList, oldNewMapping,
				linkProcessorPortMapping, nestingProcessor);

		try {
			GraphController gc = graphControllerMap.get(currentDataflow);
			gc.setExpandNestedDataflow(nestedDataflow, true);
			em.doDataflowEdit(currentDataflow, new CompoundEdit(
					currentWorkflowEditList));
			gc.redraw();
		} catch (EditException e1) {
			logger.error("failed to manufacture nested workflow", e1);
		}
	}

	private void addDataflowToNestingProcessor(Processor nestingProcessor,
			Dataflow nestedDataflow) {
		DataflowActivity da = new DataflowActivity();
		try {
			da.configure(nestedDataflow);
		} catch (ActivityConfigurationException e1) {
			logger.error("failed to set up dataflow in processor", e1);
		}
		try {
			edits.getAddActivityEdit(nestingProcessor, da).doEdit();
			edits.getDefaultDispatchStackEdit(nestingProcessor).doEdit();
			for (ActivityInputPort aip : da.getInputPorts()) {
				ProcessorInputPort pip = edits.createProcessorInputPort(
						nestingProcessor, aip.getName(), aip.getDepth());
				edits.getAddProcessorInputPortEdit(nestingProcessor, pip)
						.doEdit();
				edits.getAddActivityInputPortMappingEdit(da, aip.getName(),
						aip.getName()).doEdit();
			}
			for (OutputPort aop : da.getOutputPorts()) {
				ProcessorOutputPort pop = edits.createProcessorOutputPort(
						nestingProcessor, aop.getName(), aop.getDepth(),
						aop.getGranularDepth());
				edits.getAddProcessorOutputPortEdit(nestingProcessor, pop)
						.doEdit();
				edits.getAddActivityOutputPortMappingEdit(da, aop.getName(),
						aop.getName()).doEdit();
			}
		} catch (EditException e1) {
			logger.error("failed to add ports to processor", e1);
		}
	}

	private void createDatalinkEdits(List<Edit<?>> editList,
			Map<Object, Object> oldNewMapping,
			Map<Datalink, String> linkProcessorPortMapping,
			Processor nestingProcessor) {
		for (Datalink dl : currentDataflow.getLinks()) {
			if (oldNewMapping.containsKey(dl.getSource())
					&& oldNewMapping.containsKey(dl.getSink())) {
				// Internal to nested workflow
				editList.add(edits.getDisconnectDatalinkEdit(dl));
			} else if (oldNewMapping.containsKey(dl.getSource())) {
				// Coming out of nested workflow
				String portName = linkProcessorPortMapping.get(dl);
				ProcessorOutputPort nestedPort = null;
				for (ProcessorOutputPort pop : nestingProcessor
						.getOutputPorts())
					if (pop.getName().equals(portName)) {
						nestedPort = pop;
						break;
					}
				if (nestedPort != null) {
					Datalink replacementDatalink = edits.createDatalink(
							nestedPort, dl.getSink());
					editList.add(edits.getDisconnectDatalinkEdit(dl));
					editList.add(edits
							.getConnectDatalinkEdit(replacementDatalink));
				}
			} else if (oldNewMapping.containsKey(dl.getSink())) {
				// Coming into nested workflow
				String portName = linkProcessorPortMapping.get(dl);
				ProcessorInputPort nestedPort = null;
				for (ProcessorInputPort pip : nestingProcessor.getInputPorts())
					if (pip.getName().equals(portName)) {
						nestedPort = pip;
						break;
					}
				if (nestedPort != null) {
					Datalink replacementDatalink = edits.createDatalink(
							dl.getSource(), nestedPort);
					editList.add(edits.getDisconnectDatalinkEdit(dl));
					editList.add(edits
							.getConnectDatalinkEdit(replacementDatalink));
				}
			}
		}
	}

	private void transferConditions(List<Edit<?>> editList,
			Map<Object, Object> oldNewMapping, Processor nestingProcessor) {
		HashSet<Condition> alreadyConsidered = new HashSet<Condition>();
		for (Processor p : currentDataflow.getProcessors()) {
			boolean isTargetMoved = oldNewMapping.containsKey(p);
			for (Condition c : p.getPreconditionList()) {
				if (!alreadyConsidered.add(c))
					continue;

				Processor pre = c.getControl();
				boolean isControlMoved = oldNewMapping.containsKey(pre);
				if (isTargetMoved && isControlMoved) {
					// Add in new condition
					try {
						edits.getCreateConditionEdit(
								(Processor) oldNewMapping.get(pre),
								(Processor) oldNewMapping.get(p)).doEdit();
					} catch (EditException e1) {
						logger.error("failed to transfer condition", e1);
					}
				} else if (isTargetMoved) {
					editList.add(edits.getRemoveConditionEdit(pre, p));
					editList.add(edits.getCreateConditionEdit(pre,
							nestingProcessor));
				} else if (isControlMoved) {
					editList.add(edits.getRemoveConditionEdit(pre, p));
					editList.add(edits.getCreateConditionEdit(nestingProcessor,
							p));
				}
			}
		}
	}

	private void transferDatalinks(Map<Object, Object> oldNewMapping,
			Map<Datalink, String> linkProcessorPortMapping,
			Map<EventForwardingOutputPort, DataflowOutputPort> outputPortMap,
			Map<EventHandlingInputPort, DataflowInputPort> inputPortMap,
			Dataflow nestedDataflow) {
		HashSet<String> inputPortNames = new HashSet<String>();
		HashSet<String> outputPortNames = new HashSet<String>();
		for (Datalink dl : currentDataflow.getLinks()) {
			final EventForwardingOutputPort datalinkSource = dl.getSource();
			final EventHandlingInputPort datalinkSink = dl.getSink();
			if (oldNewMapping.containsKey(datalinkSource)
					&& oldNewMapping.containsKey(datalinkSink)) {
				// Internal to nested workflow
				Datalink newDatalink = edits.createDatalink(
						(EventForwardingOutputPort) oldNewMapping
								.get(datalinkSource),
						(EventHandlingInputPort) oldNewMapping
								.get(datalinkSink));
				try {
					edits.getConnectDatalinkEdit(newDatalink).doEdit();
				} catch (EditException e1) {
					logger.error("failed to connect datalink", e1);
				}
			} else if (oldNewMapping.containsKey(datalinkSource)) {
				DataflowOutputPort dop = null;
				if (!outputPortMap.containsKey(datalinkSource)) {
					String portName = uniqueObjectName(
							datalinkSource.getName(), outputPortNames);
					outputPortNames.add(portName);
					outputPortMap
							.put(datalinkSource, edits
									.createDataflowOutputPort(portName,
											nestedDataflow));
				}
				dop = outputPortMap.get(datalinkSource);
				String portName = dop.getName();
				// Coming out of nested workflow
				linkProcessorPortMapping.put(dl, portName);
				try {
					edits.getAddDataflowOutputPortEdit(nestedDataflow, dop)
							.doEdit();
					Datalink newDatalink = edits.createDatalink(
							(EventForwardingOutputPort) oldNewMapping
									.get(datalinkSource), dop
									.getInternalInputPort());
					edits.getConnectDatalinkEdit(newDatalink).doEdit();
				} catch (EditException e1) {
					logger.error("failed to add dataflow output", e1);
				}
			} else if (oldNewMapping.containsKey(datalinkSink)) {
				DataflowInputPort dip = null;
				if (!inputPortMap.containsKey(datalinkSink)) {
					String portName = uniqueObjectName(datalinkSink.getName(),
							inputPortNames);
					inputPortNames.add(portName);
					inputPortMap.put(
							datalinkSink,
							edits.createDataflowInputPort(portName,
									dl.getResolvedDepth(),
									dl.getResolvedDepth(), nestedDataflow));

				}
				dip = inputPortMap.get(datalinkSink);
				String portName = dip.getName();
				// Coming into nested workflow
				linkProcessorPortMapping.put(dl, portName);
				try {
					edits.getAddDataflowInputPortEdit(nestedDataflow, dip)
							.doEdit();
					Datalink newDatalink = edits.createDatalink(dip
							.getInternalOutputPort(),
							(EventHandlingInputPort) oldNewMapping
									.get(datalinkSink));
					edits.getConnectDatalinkEdit(newDatalink).doEdit();
				} catch (EditException e1) {
					logger.error("failed to add dataflow input", e1);
				}
			}
		}
	}

	private void transferProcessors(List<Edit<?>> editList,
			Map<Object, Object> oldNewMapping, Dataflow nestedDataflow) {
		for (TokenProcessingEntity entity : includedProcessors)
			try {
				if (entity instanceof Processor)
					transferProcessor(editList, oldNewMapping, nestedDataflow,
							(Processor) entity);
				else if (entity instanceof Merge)
					transferMerge(editList, oldNewMapping, nestedDataflow,
							(Merge) entity);
			} catch (Exception e1) {
				logger.error("failed to transfer processor", e1);
			}
	}

	private void transferMerge(List<Edit<?>> editList,
			Map<Object, Object> oldNewMapping, Dataflow nestedDataflow,
			Merge merge) throws EditException {
		editList.add(edits.getRemoveMergeEdit(currentDataflow, merge));
		Merge newMerge = edits.createMerge(nestedDataflow);
		edits.getAddMergeEdit(nestedDataflow, newMerge).doEdit();
		oldNewMapping.put(merge, newMerge);
		for (MergeInputPort mip : merge.getInputPorts()) {
			MergeInputPort newMip = edits.createMergeInputPort(newMerge,
					mip.getName(), mip.getDepth());
			edits.getAddMergeInputPortEdit(newMerge, newMip).doEdit();
			oldNewMapping.put(mip, newMip);
		}
		oldNewMapping.put(merge.getOutputPort(), newMerge.getOutputPort());
	}

	private void transferProcessor(List<Edit<?>> editList,
			Map<Object, Object> oldNewMapping, Dataflow nestedDataflow,
			Processor p) throws Exception {
		editList.add(edits.getRemoveProcessorEdit(currentDataflow, p));
		Processor newProcessor = pasteProcessor(copyProcessor(p),
				nestedDataflow);
		oldNewMapping.put(p, newProcessor);
		for (ProcessorInputPort pip : p.getInputPorts())
			for (ProcessorInputPort newPip : newProcessor.getInputPorts())
				if (pip.getName().equals(newPip.getName())) {
					oldNewMapping.put(pip, newPip);
					break;
				}
		for (ProcessorOutputPort pop : p.getOutputPorts())
			for (ProcessorOutputPort newPop : newProcessor.getOutputPorts())
				if (pop.getName().equals(newPop.getName())) {
					oldNewMapping.put(pop, newPop);
					break;
				}
	}

	private Processor createNestingProcessor(List<Edit<?>> editList) {
		Processor nestingProcessor = edits.createProcessor(uniqueProcessorName(
				nameField.getText(), currentDataflow));
		if (includedProcessors.size() != 1
				|| !(includedProcessors.get(0) instanceof Processor))
			return nestingProcessor;
		Processor includedProcessor = (Processor) includedProcessors.get(0);
		for (Class<?> c : at.getAnnotatingClasses(includedProcessor)) {
			AnnotationBeanSPI annotation = at.getAnnotation(includedProcessor,
					AbstractTextualValueAssertion.class);
			if ((annotation != null)
					&& (annotation instanceof AbstractTextualValueAssertion))
				editList.add(at.setAnnotationString(nestingProcessor, c,
						((AbstractTextualValueAssertion) annotation).getText()));
		}
		return nestingProcessor;
	}

	private Dataflow createNestedDataflow(ActionEvent e) {
		Dataflow nestedDataflow = edits.createDataflow();
		((DataflowImpl) nestedDataflow).setLocalName(nameField.getText());
		try {
			AnnotationTools at = new AnnotationTools();
			at.setAnnotationString(nestedDataflow, DescriptiveTitle.class,
					nameField.getText()).doEdit();
		} catch (EditException e2) {
			logger.error("failed to put annotation on nested dataflow", e2);
		}
		return nestedDataflow;
	}

	private final class OKAction extends AbstractAction {
		private static final long serialVersionUID = 6516891432445682857L;
		private final JDialog dialog;

		private OKAction(JDialog dialog) {
			super("OK");
			this.dialog = dialog;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (includedProcessors.isEmpty()) {
				showMessageDialog(
						null,
						"At least one service must be included in the nested workflow",
						"Nested workflow creation", WARNING_MESSAGE);
				return;
			}

			createNestedWorkflow(e);
			dialog.setVisible(false);
		}
	}

	private final class ResetAction extends AbstractAction {
		private static final long serialVersionUID = 7296742769289881218L;

		private ResetAction(JDialog dialog) {
			super("Reset");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			resetLists();
		}
	}

	private final class CancelAction extends AbstractAction {
		private static final long serialVersionUID = -7842176979437027091L;
		private final JDialog dialog;

		private CancelAction(JDialog dialog) {
			super("Cancel");
			this.dialog = dialog;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			dialog.setVisible(false);
		}
	}
}
