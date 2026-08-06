package in.slpro.apibanker.http;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import in.slpro.apibanker.model.CollectionModel;
import in.slpro.apibanker.model.KeyValueItem;
import in.slpro.apibanker.model.RequestModel;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * JmxHelper
 *
 * <p>
 * Utility class providing robust bidirectional parsing and generation
 * of Apache JMeter (.jmx) XML test plan files. It allows ApiBanker to seamlessly
 * import requests from external performance tests or export ApiBanker collections
 * into a format ready for distributed load testing.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.0.0-beta
 * @since 1.0.0
 */
public class JmxHelper {

    /**
     * Parses a standard Apache JMeter (.jmx) XML file and converts its
     * HTTP Samplers into a list of ApiBanker {@link RequestModel}s.
     * 
     * @param file The JMeter .jmx file to parse.
     * @return A List of extracted RequestModels representing the HTTP requests in
     *         the plan.
     * @throws Exception If XML parsing fails or the file format is completely
     *                   unrecognized.
     */
    public static List<RequestModel> importJmx(File file) throws Exception {
        List<RequestModel> list = new ArrayList<>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        doc.getDocumentElement().normalize();

        NodeList samplerList = doc.getElementsByTagName("HTTPSamplerProxy");
        for (int i = 0; i < samplerList.getLength(); i++) {
            Node node = samplerList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node;
                String name = elem.getAttribute("testname");
                if (name == null || name.isBlank())
                    name = "JMeter Request " + (i + 1);

                String domain = getPropValue(elem, "HTTPSampler.domain");
                String path = getPropValue(elem, "HTTPSampler.path");
                String protocol = getPropValue(elem, "HTTPSampler.protocol");
                String port = getPropValue(elem, "HTTPSampler.port");
                String method = getPropValue(elem, "HTTPSampler.method");
                if (method == null || method.isBlank())
                    method = "GET";

                if (protocol == null || protocol.isBlank())
                    protocol = "http";
                String url = protocol + "://" + (domain != null ? domain : "localhost") +
                        ((port != null && !port.isBlank()) ? ":" + port : "") +
                        (path != null ? path : "");

                RequestModel req = new RequestModel();
                req.setName(name);
                req.setUrl(url);
                req.setMethod(method.toUpperCase());
                list.add(req);
            }
        }
        return list;
    }

    /**
     * Generates a fully compatible Apache JMeter (.jmx) XML file from a given
     * ApiBanker {@link CollectionModel}. This creates a standard Test Plan, Thread
     * Group,
     * and maps each ApiBanker request into an HTTPSamplerProxy.
     * 
     * @param col  The ApiBanker Collection to export.
     * @param file The target file where the generated XML will be saved.
     * @throws Exception If the XML Document cannot be generated or transformed to
     *                   the output file.
     */
    public static void exportJmx(CollectionModel col, File file) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        // root jmeterTestPlan
        Element root = doc.createElement("jmeterTestPlan");
        root.setAttribute("version", "1.2");
        root.setAttribute("properties", "5.0");
        root.setAttribute("jmeter", "5.5");
        doc.appendChild(root);

        Element rootHash = doc.createElement("hashTree");
        root.appendChild(rootHash);

        // TestPlan
        Element testPlan = doc.createElement("TestPlan");
        testPlan.setAttribute("guiclass", "TestPlanGui");
        testPlan.setAttribute("testclass", "TestPlan");
        testPlan.setAttribute("testname", col.getName() + " Plan");
        testPlan.setAttribute("enabled", "true");
        rootHash.appendChild(testPlan);

        addStringProp(doc, testPlan, "TestPlan.comments", "Exported from ApiBanker");
        addBoolProp(doc, testPlan, "TestPlan.functional_mode", false);
        addBoolProp(doc, testPlan, "TestPlan.tearDown_on_shutdown", true);
        addBoolProp(doc, testPlan, "TestPlan.serialize_threadgroups", false);

        Element userVars = doc.createElement("elementProp");
        userVars.setAttribute("name", "TestPlan.user_defined_variables");
        userVars.setAttribute("elementType", "Arguments");
        userVars.setAttribute("guiclass", "ArgumentsPanel");
        userVars.setAttribute("testclass", "Arguments");
        userVars.setAttribute("testname", "User Defined Variables");
        userVars.setAttribute("enabled", "true");
        Element userVarsColl = doc.createElement("collectionProp");
        userVarsColl.setAttribute("name", "Arguments.arguments");
        userVars.appendChild(userVarsColl);
        testPlan.appendChild(userVars);

        Element planHash = doc.createElement("hashTree");
        rootHash.appendChild(planHash);

        // ThreadGroup
        Element threadGroup = doc.createElement("ThreadGroup");
        threadGroup.setAttribute("guiclass", "ThreadGroupGui");
        threadGroup.setAttribute("testclass", "ThreadGroup");
        threadGroup.setAttribute("testname", "Thread Group");
        threadGroup.setAttribute("enabled", "true");
        planHash.appendChild(threadGroup);

        addStringProp(doc, threadGroup, "ThreadGroup.on_sample_error", "continue");
        addStringProp(doc, threadGroup, "ThreadGroup.num_threads", "1");
        addStringProp(doc, threadGroup, "ThreadGroup.ramp_time", "1");
        addBoolProp(doc, threadGroup, "ThreadGroup.scheduler", false);

        Element controller = doc.createElement("elementProp");
        controller.setAttribute("name", "ThreadGroup.main_controller");
        controller.setAttribute("elementType", "LoopController");
        controller.setAttribute("guiclass", "LoopControlPanel");
        controller.setAttribute("testclass", "LoopController");
        controller.setAttribute("testname", "Loop Controller");
        controller.setAttribute("enabled", "true");
        addBoolProp(doc, controller, "LoopController.continue_forever", false);
        addStringProp(doc, controller, "LoopController.loops", "1");
        threadGroup.appendChild(controller);

        Element threadHash = doc.createElement("hashTree");
        planHash.appendChild(threadHash);

        // Add HTTP Samplers
        for (RequestModel req : col.getRequests()) {
            if ("runner".equalsIgnoreCase(req.getType()))
                continue;

            Element sampler = doc.createElement("HTTPSamplerProxy");
            sampler.setAttribute("guiclass", "HttpTestSampleGui");
            sampler.setAttribute("testclass", "HTTPSamplerProxy");
            sampler.setAttribute("testname", req.getName());
            sampler.setAttribute("enabled", "true");
            threadHash.appendChild(sampler);

            // Parse URL
            String protocol = "http";
            String domain = "localhost";
            String port = "";
            String path = "/";
            try {
                URI uri = new URI(req.getUrl());
                if (uri.getScheme() != null)
                    protocol = uri.getScheme();
                if (uri.getHost() != null)
                    domain = uri.getHost();
                if (uri.getPort() != -1)
                    port = String.valueOf(uri.getPort());
                if (uri.getPath() != null)
                    path = uri.getPath();
                if (uri.getQuery() != null)
                    path += "?" + uri.getQuery();
            } catch (Exception ignored) {
            }

            addStringProp(doc, sampler, "HTTPSampler.domain", domain);
            addStringProp(doc, sampler, "HTTPSampler.port", port);
            addStringProp(doc, sampler, "HTTPSampler.protocol", protocol);
            addStringProp(doc, sampler, "HTTPSampler.contentEncoding", "UTF-8");
            addStringProp(doc, sampler, "HTTPSampler.path", path);
            addStringProp(doc, sampler, "HTTPSampler.method", req.getMethod());
            addBoolProp(doc, sampler, "HTTPSampler.follow_redirects", true);
            addBoolProp(doc, sampler, "HTTPSampler.use_keepalive", true);

            // empty args elem
            Element argsElem = doc.createElement("elementProp");
            argsElem.setAttribute("name", "HTTPsampler.Arguments");
            argsElem.setAttribute("elementType", "Arguments");
            argsElem.setAttribute("guiclass", "HTTPArgumentsPanel");
            argsElem.setAttribute("testclass", "Arguments");
            argsElem.setAttribute("enabled", "true");
            Element argsColl = doc.createElement("collectionProp");
            argsColl.setAttribute("name", "Arguments.arguments");
            argsElem.appendChild(argsColl);
            sampler.appendChild(argsElem);

            Element samplerHash = doc.createElement("hashTree");
            threadHash.appendChild(samplerHash);
        }

        // Save doc to file
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }

    /**
     * Helper method to extract the value of a specific JMeter property node
     * (e.g., 'HTTPSampler.domain') from a given parent XML element.
     * 
     * @param parent   The parent XML Element node to search within.
     * @param propName The exact name attribute of the target property node.
     * @return The text content of the property if found; otherwise an empty string.
     */
    private static String getPropValue(Element parent, String propName) {
        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                if (propName.equals(e.getAttribute("name"))) {
                    return e.getTextContent();
                }
            }
        }
        return "";
    }

    /**
     * Helper method to append a String property node to a parent XML element.
     * 
     * @param doc    The active XML Document being built.
     * @param parent The parent XML Element to attach the property to.
     * @param name   The name of the property.
     * @param val    The string value of the property.
     */
    private static void addStringProp(Document doc, Element parent, String name, String val) {
        Element p = doc.createElement("stringProp");
        p.setAttribute("name", name);
        p.setTextContent(val);
        parent.appendChild(p);
    }

    /**
     * Helper method to append a Boolean property node to a parent XML element.
     * 
     * @param doc    The active XML Document being built.
     * @param parent The parent XML Element to attach the property to.
     * @param name   The name of the property.
     * @param val    The boolean value of the property.
     */
    private static void addBoolProp(Document doc, Element parent, String name, boolean val) {
        Element p = doc.createElement("boolProp");
        p.setAttribute("name", name);
        p.setTextContent(String.valueOf(val));
        parent.appendChild(p);
    }
}


