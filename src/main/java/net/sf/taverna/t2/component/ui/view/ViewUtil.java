/**
 * 
 */
package net.sf.taverna.t2.component.ui.view;

import static java.lang.String.format;
import static net.sf.taverna.t2.component.registry.ComponentUtil.calculateComponent;
import static net.sf.taverna.t2.component.registry.ComponentUtil.calculateComponentVersion;
import static net.sf.taverna.t2.component.registry.ComponentUtil.calculateFamily;
import static org.apache.log4j.Logger.getLogger;

import java.net.URL;
import java.util.List;

import net.sf.taverna.t2.component.ComponentActivityConfigurationBean;
import net.sf.taverna.t2.component.api.Component;
import net.sf.taverna.t2.component.api.Family;
import net.sf.taverna.t2.component.api.Version;
import net.sf.taverna.t2.workflowmodel.processor.activity.config.ActivityInputPortDefinitionBean;
import net.sf.taverna.t2.workflowmodel.processor.activity.config.ActivityOutputPortDefinitionBean;

import org.apache.log4j.Logger;

/**
 * @author alanrw
 * 
 */
public class ViewUtil {

	private static Logger logger = getLogger(ViewUtil.class);

	private static final String VERSION_DESCRIPTION_LABEL = "Component version description";
	private static final String COMPONENT_DESCRIPTION_LABEL = "Component description";
	private static final String FAMILY_DESCRIPTION_LABEL = "Family description";

	private static String plainFormat = "<tr><td><b>%1$s</b></td><td nowrap=\"wrap\" style=\"width:100px;\">%2$s</td></tr>";

	private static String headerFormat = "<tr><th>%1$s</th><th>%2$s</th></tr>";

	private static String rowFormat = "<tr><td><b>%1$s</b></td><td>%2$s</td></tr>";
	private static String rowLinkFormat = "<tr><td><b>%1$s</b></td><td><a href=\"%3$s\">%2$s</a></td></tr>";
	private static String descriptionFormat = "<tr><td colspan=\"2\"><b>%1$s</b></td></tr><tr><td colspan=\"2\" nowrap=\"wrap\" style=\"width:100px;\">%2$s</td></tr>";

	public static String getRawTablesHtml(Version.ID id) {
		StringBuilder html = new StringBuilder();

		URL registryBase = id.getRegistryBase();
		String registryLink = null;
		if (registryBase.getProtocol().startsWith("http"))
			registryLink = registryBase.toExternalForm();
		appendRow(html, "Component registry base", registryBase, registryLink);

		String familyName = id.getFamilyName();
		appendRow(html, "Component family", familyName, null);
		try {
			Family family = calculateFamily(registryBase, familyName);
			if (family != null)
				appendDescriptionHtml(html, FAMILY_DESCRIPTION_LABEL,
						family.getDescription());
		} catch (Exception e) {
			logger.error("failed to get component family description", e);
		}

		String componentName = id.getComponentName();
		String helpLink = null;
		try {
			URL helpURL = calculateComponentVersion(id).getHelpURL();
			if (helpURL != null)
				helpLink = helpURL.toExternalForm();
		} catch (Exception e) {
			logger.error(e);
		}

		appendRow(html, "Component name", componentName, helpLink);
		try {
			Component component = calculateComponent(registryBase, familyName,
					componentName);
			if (component != null)
				appendDescriptionHtml(html, COMPONENT_DESCRIPTION_LABEL,
						component.getDescription());
		} catch (Exception e) {
			logger.error("failed to get component description", e);
		}

		Integer componentVersion = id.getComponentVersion();

		if (componentVersion == null)
			appendRow(html, "Component version", "N/A", helpLink);
		else {
			appendRow(html, "Component version", componentVersion, helpLink);
			try {
				Version version = calculateComponentVersion(registryBase,
						familyName, componentName, componentVersion);
				if (version != null)
					appendDescriptionHtml(html, VERSION_DESCRIPTION_LABEL,
							version.getDescription());
			} catch (Exception e) {
				logger.error("failed to get component version description", e);
			}
		}

		if (id instanceof ComponentActivityConfigurationBean) {
			ComponentActivityConfigurationBean config = (ComponentActivityConfigurationBean) id;
			try {
				List<ActivityInputPortDefinitionBean> inputPortDefinitions = config
						.getPorts().getInputPortDefinitions();
				if (!inputPortDefinitions.isEmpty()) {
					appendHeaderRow(html, "Input Port Name", "Depth");
					for (ActivityInputPortDefinitionBean bean : inputPortDefinitions)
						appendPlainRow(html, bean.getName(), bean.getDepth());
				}
				List<ActivityOutputPortDefinitionBean> outputPortDefinitions = config
						.getPorts().getOutputPortDefinitions();
				if (!outputPortDefinitions.isEmpty()) {
					appendHeaderRow(html, "Output Port Name", "Depth");
					for (ActivityOutputPortDefinitionBean bean : outputPortDefinitions)
						appendPlainRow(html, bean.getName(), bean.getDepth());
				}
			} catch (Exception e) {
				logger.error("failed to get component port description", e);
			}
		}
		return html.toString();

	}

	private static void appendRow(StringBuilder html, Object label,
			Object value, String link) {
		if (link == null) {
			html.append(format(rowFormat, label.toString(), value.toString()));
		} else {
			html.append(format(rowLinkFormat, label.toString(),
					value.toString(), link));
		}
	}

	private static void appendHeaderRow(StringBuilder html, Object label1,
			Object label2) {
		html.append(format(headerFormat, label1.toString(), label2.toString()));
	}

	private static void appendPlainRow(StringBuilder html, Object value1,
			Object value2) {
		html.append(format(plainFormat, value1.toString(), value2.toString()));
	}

	private static void appendDescriptionHtml(StringBuilder html,
			String header, String description) {
		if ((description != null) && !description.isEmpty())
			html.append(format(descriptionFormat, header, description));
	}
}
