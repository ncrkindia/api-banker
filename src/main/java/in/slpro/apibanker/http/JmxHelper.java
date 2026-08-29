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
 * @version 2.0.1
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
    public static void exportJmx(CollectionModel col, List<RequestModel> selectedRequests, RequestModel runnerModel, File file, in.slpro.apibanker.model.EnvironmentModel env) throws Exception {
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
        
        java.util.Set<String> usedVars = new java.util.HashSet<>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
        for (RequestModel req : selectedRequests) {
            java.util.List<String> texts = new ArrayList<>();
            texts.add(req.getUrl());
            texts.add(req.getBodyRawContent());
            if (req.getHeaders() != null) for (KeyValueItem kv : req.getHeaders()) { texts.add(kv.getKey()); texts.add(kv.getValue()); }
            if (req.getParams() != null) for (KeyValueItem kv : req.getParams()) { texts.add(kv.getKey()); texts.add(kv.getValue()); }
            if (req.getFormData() != null) for (KeyValueItem kv : req.getFormData()) { texts.add(kv.getKey()); texts.add(kv.getValue()); }
            if (req.getUrlencodedData() != null) for (KeyValueItem kv : req.getUrlencodedData()) { texts.add(kv.getKey()); texts.add(kv.getValue()); }
            
            for (String t : texts) {
                if (t != null) {
                    java.util.regex.Matcher m = p.matcher(t);
                    while (m.find()) usedVars.add(m.group(1));
                }
            }
        }
        
        for (String v : usedVars) {
            String val = in.slpro.apibanker.model.VariableHelper.resolveVariables("{{" + v + "}}", col, env);
            if (val == null) val = "";
            Element arg = doc.createElement("elementProp");
            arg.setAttribute("name", v);
            arg.setAttribute("elementType", "Argument");
            Element argName = doc.createElement("stringProp");
            argName.setAttribute("name", "Argument.name");
            argName.setTextContent(v);
            Element argVal = doc.createElement("stringProp");
            argVal.setAttribute("name", "Argument.value");
            argVal.setTextContent(val);
            Element argMeta = doc.createElement("stringProp");
            argMeta.setAttribute("name", "Argument.metadata");
            argMeta.setTextContent("=");
            arg.appendChild(argName);
            arg.appendChild(argVal);
            arg.appendChild(argMeta);
            userVarsColl.appendChild(arg);
        }

        Element planHash = doc.createElement("hashTree");
        rootHash.appendChild(planHash);

        // ThreadGroup
        Element threadGroup = doc.createElement("ThreadGroup");
        threadGroup.setAttribute("guiclass", "ThreadGroupGui");
        threadGroup.setAttribute("testclass", "ThreadGroup");
        threadGroup.setAttribute("testname", "Thread Group");
        threadGroup.setAttribute("enabled", "true");
        planHash.appendChild(threadGroup);

        int threads = 1;
        int rampTime = 1;
        int loops = 1;
        boolean scheduler = false;
        int durationSec = 0;
        
        if (runnerModel != null && runnerModel.getBodyRawContent() != null) {
            try {
                com.google.gson.JsonObject cfg = com.google.gson.JsonParser.parseString(runnerModel.getBodyRawContent()).getAsJsonObject();
                if (cfg.has("vusers")) threads = cfg.get("vusers").getAsInt();
                if (cfg.has("rampUp")) rampTime = cfg.get("rampUp").getAsInt();
                
                String mode = cfg.has("runMode") ? cfg.get("runMode").getAsString() : "iterations";
                if ("duration".equals(mode)) {
                    scheduler = true;
                    loops = -1; // continue forever until scheduler stops
                    if (cfg.has("durationSec")) durationSec = cfg.get("durationSec").getAsInt();
                } else {
                    if (cfg.has("iterations")) loops = cfg.get("iterations").getAsInt();
                }
            } catch (Exception ignored) {}
        }

        addStringProp(doc, threadGroup, "ThreadGroup.on_sample_error", "continue");
        addStringProp(doc, threadGroup, "ThreadGroup.num_threads", String.valueOf(threads));
        addStringProp(doc, threadGroup, "ThreadGroup.ramp_time", String.valueOf(rampTime));
        addBoolProp(doc, threadGroup, "ThreadGroup.scheduler", scheduler);
        if (scheduler) {
            addStringProp(doc, threadGroup, "ThreadGroup.duration", String.valueOf(durationSec));
            addStringProp(doc, threadGroup, "ThreadGroup.delay", "0");
        }

        Element controller = doc.createElement("elementProp");
        controller.setAttribute("name", "ThreadGroup.main_controller");
        controller.setAttribute("elementType", "LoopController");
        controller.setAttribute("guiclass", "LoopControlPanel");
        controller.setAttribute("testclass", "LoopController");
        controller.setAttribute("testname", "Loop Controller");
        controller.setAttribute("enabled", "true");
        addBoolProp(doc, controller, "LoopController.continue_forever", false);
        addStringProp(doc, controller, "LoopController.loops", String.valueOf(loops));
        threadGroup.appendChild(controller);

        Element threadHash = doc.createElement("hashTree");
        planHash.appendChild(threadHash);

        // Add HTTP Samplers
        for (RequestModel req : selectedRequests) {
            if ("runner".equalsIgnoreCase(req.getType()))
                continue;

            Element sampler = doc.createElement("HTTPSamplerProxy");
            sampler.setAttribute("guiclass", "HttpTestSampleGui");
            sampler.setAttribute("testclass", "HTTPSamplerProxy");
            sampler.setAttribute("testname", req.getName());
            sampler.setAttribute("enabled", "true");
            threadHash.appendChild(sampler);

            String jvarUrl = jmeterVar(req.getUrl());
            if (jvarUrl == null) jvarUrl = "";
            String protocol = "";
            String domain = "";
            String port = "";
            String path = "";

            String resolvedUrl = in.slpro.apibanker.model.VariableHelper.resolveVariables(req.getUrl(), col, env);
            if (resolvedUrl == null) resolvedUrl = "";

            if (jvarUrl.contains("${")) {
                // If the URL contains variables, they might contain protocols, colons, or slashes.
                // JMeter throws "Illegal character in host" if the Domain field evaluates to strings containing '/' or ':'.
                // The most robust way to handle dynamic URLs in JMeter is to pass the FULL URL into the Path field.
                protocol = "";
                domain = "";
                port = "";
                
                if (resolvedUrl.startsWith("http://") || resolvedUrl.startsWith("https://")) {
                    path = jvarUrl; // JMeter will evaluate this to a full URL and route correctly.
                } else {
                    // If the variable doesn't contain a protocol, we must supply it so JMeter recognizes it as a full URL in Path.
                    if (jvarUrl.startsWith("http://") || jvarUrl.startsWith("https://")) {
                        path = jvarUrl;
                    } else {
                        path = "http://" + jvarUrl;
                    }
                }
            } else {
                // For static URLs, parse normally into JMeter's distinct UI fields
                String u = jvarUrl;
                if (u.contains("://")) {
                    int idx = u.indexOf("://");
                    protocol = u.substring(0, idx);
                    u = u.substring(idx + 3);
                }
                int slashIdx = u.indexOf("/");
                if (slashIdx == -1) {
                    domain = u;
                    path = "";
                } else {
                    domain = u.substring(0, slashIdx);
                    path = u.substring(slashIdx);
                }
                int colonIdx = domain.indexOf(":");
                if (colonIdx != -1) {
                    port = domain.substring(colonIdx + 1);
                    domain = domain.substring(0, colonIdx);
                }
                
                if (protocol.isEmpty()) protocol = "http";
                if (domain.isEmpty()) domain = "localhost";
                if (path.isEmpty()) path = "/";
            }
            if (path.isEmpty()) path = "/";

            addStringProp(doc, sampler, "HTTPSampler.domain", domain);
            addStringProp(doc, sampler, "HTTPSampler.port", port);
            addStringProp(doc, sampler, "HTTPSampler.protocol", protocol);
            addStringProp(doc, sampler, "HTTPSampler.contentEncoding", "UTF-8");
            addStringProp(doc, sampler, "HTTPSampler.path", path);
            addStringProp(doc, sampler, "HTTPSampler.method", req.getMethod());
            addBoolProp(doc, sampler, "HTTPSampler.follow_redirects", true);
            addBoolProp(doc, sampler, "HTTPSampler.use_keepalive", true);

            // empty args elem (we could export params/body here if needed)
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

        // Add Listeners (Disabled by default)
        addResultCollector(doc, threadHash, "ViewResultsFullVisualizer", "View Results Tree");
        addResultCollector(doc, threadHash, "TableVisualizer", "View Results in Table");

        // Save doc to file
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }

    private static String jmeterVar(String input) {
        if (input == null) return null;
        return input.replace("{{", "${").replace("}}", "}");
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

    private static void addResultCollector(Document doc, Element parentHash, String guiClass, String name) {
        Element rc = doc.createElement("ResultCollector");
        rc.setAttribute("guiclass", guiClass);
        rc.setAttribute("testclass", "ResultCollector");
        rc.setAttribute("testname", name);
        rc.setAttribute("enabled", "false");
        
        addBoolProp(doc, rc, "ResultCollector.error_logging", false);
        
        Element objProp = doc.createElement("objProp");
        Element pName = doc.createElement("name");
        pName.setTextContent("saveConfig");
        objProp.appendChild(pName);
        
        Element value = doc.createElement("value");
        value.setAttribute("class", "SampleSaveConfiguration");
        
        String[] trueProps = {"time", "latency", "timestamp", "success", "label", "code", "message", "threadName", "dataType", "assertions", "subresults", "fieldNames", "saveAssertionResultsFailureMessage", "bytes", "sentBytes", "url", "threadCounts", "idleTime", "connectTime"};
        String[] falseProps = {"encoding", "responseData", "samplerData", "xml", "responseHeaders", "requestHeaders", "responseDataOnError"};
        
        for (String prop : trueProps) {
            Element e = doc.createElement(prop);
            e.setTextContent("true");
            value.appendChild(e);
        }
        for (String prop : falseProps) {
            Element e = doc.createElement(prop);
            e.setTextContent("false");
            value.appendChild(e);
        }
        
        Element assertionsResults = doc.createElement("assertionsResultsToSave");
        assertionsResults.setTextContent("0");
        value.appendChild(assertionsResults);
        
        objProp.appendChild(value);
        rc.appendChild(objProp);
        
        Element filename = doc.createElement("stringProp");
        filename.setAttribute("name", "filename");
        rc.appendChild(filename);
        
        parentHash.appendChild(rc);
        parentHash.appendChild(doc.createElement("hashTree"));
    }
}


