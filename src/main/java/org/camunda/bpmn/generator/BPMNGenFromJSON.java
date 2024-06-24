package org.camunda.bpmn.generator;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.model.bpmn.impl.instance.ConditionExpressionImpl;
import io.camunda.zeebe.model.bpmn.instance.*;
import io.camunda.zeebe.model.bpmn.instance.Process;
import io.camunda.zeebe.model.bpmn.instance.bpmndi.BpmnDiagram;
import io.camunda.zeebe.model.bpmn.instance.bpmndi.BpmnPlane;
import org.camunda.bpm.model.xml.impl.instance.ModelTypeInstanceContext;

import static io.camunda.zeebe.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BPMNGenFromJSON {
    public static void main(String[] args) throws Exception {
        //try {

        // Read and parse file
        // Need to pass in two args - arg[0] is the input file and arg[1] is the output file
        if (args.length < 2) {
            System.out.println("Two arguments are required for this BPMN from JSON program. One for the input file followed by one for the output file.");
            return;
        }

        // Read in JSON file
        File file = new File(args[0]);

        // Create hash map to map ids in old file node objects with ids in new file
        HashMap<String, Object> idMap = new HashMap<>();

        // Create hash of flow nodes for drawing of sequence flows later
        HashMap<String, Object> flowNodesMap = new HashMap<>();

        // Create hash map of JSON elements to search for
        HashMap<String, Object> JSONElementsMap = new HashMap<>();
        JSONToBPMNElement bpmnElement = new JSONToBPMNElement(StartEvent.class, 36d, 36d);
        JSONElementsMap.put("start", bpmnElement);
        bpmnElement = new JSONToBPMNElement(UserTask.class, 80d, 100d);
        JSONElementsMap.put("Data-MO-Activity-Assignment", bpmnElement);
        bpmnElement = new JSONToBPMNElement(CallActivity.class, 80d, 100d);
        JSONElementsMap.put("Data-MO-Activity-SubProcess", bpmnElement);
        bpmnElement = new JSONToBPMNElement(ServiceTask.class, 80d, 100d);
        JSONElementsMap.put("service", bpmnElement);
        bpmnElement = new JSONToBPMNElement(EndEvent.class, 36d, 36d);
        JSONElementsMap.put("end", bpmnElement);
        JSONElementsMap.put("Data-MO-Event-Exception", bpmnElement);
        bpmnElement = new JSONToBPMNElement(ExclusiveGateway.class, 50d, 50d);
        JSONElementsMap.put("exclusive", bpmnElement);
        bpmnElement = new JSONToBPMNElement(ParallelGateway.class, 50d, 50d);
        JSONElementsMap.put("parallel", bpmnElement);
        //bpmnElement = new JSONToBPMNElement(SequenceFlow.class, 0d,0d);
        //JSONElementsMap.put("Data-MO-Connector-Transition", bpmnElement);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode json = objectMapper.readTree(file);

        // Create BPMN model using Camunda Model APIs
        BpmnModelInstance modelInstance = Bpmn.createEmptyModel();

        Definitions definitions = modelInstance.newInstance(Definitions.class);
        definitions.setTargetNamespace(BPMN20_NS);
        definitions.setAttributeValueNs("http://camunda.org/schema/modeler/1.0", "modeler:executionPlatform", "Camunda Cloud");
        modelInstance.setDefinitions(definitions);

        Process process = modelInstance.newInstance(Process.class);
        process.setExecutable(true); // Want to make sure it is executable by default in Modeler
        process.setName((json.findValue("name")).asText());
        definitions.addChildElement(process);


        // For the diagram, a diagram and a plane element needs to be created. The plane is set in a diagram object and the diagram is added as a child element
        BpmnDiagram bpmnDiagram = modelInstance.newInstance(BpmnDiagram.class);
        BpmnPlane plane = modelInstance.newInstance(BpmnPlane.class);

        plane.setBpmnElement(process);

        bpmnDiagram.setBpmnPlane(plane);
        definitions.addChildElement(bpmnDiagram);

        // Add start element
        bpmnElement = (JSONToBPMNElement) JSONElementsMap.get("start");

        Double x = Double.valueOf(200);
        Double y = Double.valueOf(200);

        BpmnModelElementInstance element = (BpmnModelElementInstance) modelInstance.newInstance(bpmnElement.getType());
        process.addChildElement(element);
        plane = DrawShape.drawShape(plane, modelInstance, element, x, y, bpmnElement.getHeight(), bpmnElement.getWidth(), true, false);
        FlowNodeInfo fni = new FlowNodeInfo(element.getAttributeValue("id"), x, y, x, y, bpmnElement.getType().toString(), bpmnElement.getHeight(), bpmnElement.getWidth());
        flowNodesMap.put("start", fni);
        FlowNodeInfo previousFNI = fni;

        // Get list of 'steps' in process to create
        JsonNode states = json.findValue("states");
        List<JsonNode> stateList = objectMapper.readValue(states.traverse(), new TypeReference<List<JsonNode>>() {
        });

        // Figure out sequence flows
        // Get first task
        Integer index = Integer.valueOf(0);
        String taskName = stateList.get(0).findValue("name").asText();
        SequenceFromTo sft = new SequenceFromTo("start", taskName);
        HashMap< Integer, SequenceFromTo > sequences = new HashMap<>();
        sequences.put( index, sft);
        String previousTaskName = taskName;
        index++;

        Iterator seqIter = stateList.iterator();

        while(seqIter.hasNext()) {
            JsonNode node = (JsonNode) seqIter.next();
            String type = node.findValue("type").asText();
            Boolean end = (node.findValue("end") != null) ? true:false;

            switch (type) {
                case("event"):
                    String actionMode = (node.findValue("actionMode")).asText();

                    if (actionMode.equals("parallel")) {
                        JsonNode actions = node.findValue("actions");
                        Iterator actionIter = actions.iterator();
                        while (actionIter.hasNext()) {
                            JsonNode action = (JsonNode) actionIter.next();
                            sft = new SequenceFromTo(node.findValue("name").asText(), action.findValue("name").asText());
                            sequences.put(index, sft);
                            index++;
                            sft = new SequenceFromTo(action.findValue("name").asText(), node.findValue("name").asText()+"-join");
                            sequences.put(index, sft);
                            index++;
                        }
                    }
                    previousTaskName = node.findValue("name").asText()+"-join";
                    break;

                case("switch"):
                    sft = new SequenceFromTo(previousTaskName, node.findValue("name").asText());
                    sequences.put(index, sft);
                    index++;

                    JsonNode dataConditions = node.findValue("dataConditions");

                    Iterator dataIter = dataConditions.iterator();

                    while(dataIter.hasNext()){
                        JsonNode dataCond = (JsonNode) dataIter.next();
                        sft = new SequenceFromTo(node.findValue("name").asText(), dataCond.findValue("transition").asText(), dataCond.findValue("condition").asText());

                        sequences.put(index, sft);
                        index++;
                    }
                    break;

                case("operation"):
                    if(end){
                        sft = new SequenceFromTo(node.findValue("name").asText(), "end-"+node.findValue("name").asText());
                        sequences.put(index, sft);
                        index++;
                    }
                    break;
            }

        }

        Iterator iter = stateList.iterator();

        while (iter.hasNext()) {
            JsonNode node = (JsonNode) iter.next();
            switch ((node.findValue("type")).asText()) {
                case ("event"):
                    String actionMode = (node.findValue("actionMode")).asText();
                    if (actionMode.equals("parallel")) {
                        bpmnElement = (JSONToBPMNElement) JSONElementsMap.get("parallel");
                        x = x + 100;
                        element = (BpmnModelElementInstance) modelInstance.newInstance(bpmnElement.getType());
                        process.addChildElement(element);
                        plane = DrawShape.drawShape(plane, modelInstance, element, x, y, bpmnElement.getHeight(), bpmnElement.getWidth(), true, false);
                        fni = new FlowNodeInfo(element.getAttributeValue("id"), x, y, x, y, bpmnElement.getType().toString(), bpmnElement.getHeight(), bpmnElement.getWidth());
                        flowNodesMap.put(node.findValue("name").asText(), fni);

                        // Get actions
                        JsonNode actions = node.findValue("actions");
                        Iterator actionIter = actions.iterator();
                        HashMap<String, FlowNodeInfo> parallelNodes = new HashMap<>();
                        int i = 0;
                        while (actionIter.hasNext()) {
                            if(i == 0) {
                                x = x + 150;
                            }
                            JsonNode action = (JsonNode) actionIter.next();
                            bpmnElement = (JSONToBPMNElement) JSONElementsMap.get("service");
                            element = (BpmnModelElementInstance) modelInstance.newInstance(bpmnElement.getType());
                            element.setAttributeValue("name", action.findValue("name").asText());
                            process.addChildElement(element);
                            plane = DrawShape.drawShape(plane, modelInstance, element, x, y, bpmnElement.getHeight(), bpmnElement.getWidth(), true, false);

                            fni = new FlowNodeInfo(element.getAttributeValue("id"), x, y, x, y, bpmnElement.getType().toString(), bpmnElement.getHeight(), bpmnElement.getWidth());
                            flowNodesMap.put(action.findValue("name").asText(), fni);
                            parallelNodes.put(fni.getNewId(), fni);
                            y = y + 100;
                            i = i + 1;
                        }

                        y = y - (100 * i);

                        bpmnElement = (JSONToBPMNElement) JSONElementsMap.get("parallel");
                        x = x + 150;
                        element = (BpmnModelElementInstance) modelInstance.newInstance(bpmnElement.getType());
                        process.addChildElement(element);
                        plane = DrawShape.drawShape(plane, modelInstance, element, x, y, bpmnElement.getHeight(), bpmnElement.getWidth(), true, false);

                        fni = new FlowNodeInfo(element.getAttributeValue("id"), x, y, x, y, bpmnElement.getType().toString(), bpmnElement.getHeight(), bpmnElement.getWidth());
                        flowNodesMap.put(node.findValue("name").asText()+"-join", fni);
                    }
                    break;

                case ("switch"):
                    bpmnElement = (JSONToBPMNElement) JSONElementsMap.get("exclusive");
                    x = x + 150;
                    element = (BpmnModelElementInstance) modelInstance.newInstance(bpmnElement.getType());
                    process.addChildElement(element);
                    plane = DrawShape.drawShape(plane, modelInstance, element, x, y, bpmnElement.getHeight(), bpmnElement.getWidth(), true, false);
                    fni = new FlowNodeInfo(element.getAttributeValue("id"), x, y, x, y, bpmnElement.getType().toString(), bpmnElement.getHeight(), bpmnElement.getWidth());
                    flowNodesMap.put(node.findValue("name").asText(), fni);
                    break;

                case ("operation"):
                    bpmnElement = (JSONToBPMNElement) JSONElementsMap.get("service");
                    x = x + 150;
                    element = (BpmnModelElementInstance) modelInstance.newInstance(bpmnElement.getType());
                    element.setAttributeValue("name", node.findValue("name").asText());
                    process.addChildElement(element);
                    plane = DrawShape.drawShape(plane, modelInstance, element, x, y, bpmnElement.getHeight(), bpmnElement.getWidth(), true, false);
                    fni = new FlowNodeInfo(element.getAttributeValue("id"), x, y, x, y, bpmnElement.getType().toString(), bpmnElement.getHeight(), bpmnElement.getWidth());
                    flowNodesMap.put(node.findValue("name").asText(), fni);

                    Boolean end = (node.findValue("end") != null) ? true:false;
                    if(end){
                        bpmnElement = (JSONToBPMNElement) JSONElementsMap.get("end");
                        x = x + 150;
                        element = (BpmnModelElementInstance) modelInstance.newInstance(bpmnElement.getType());
                        process.addChildElement(element);
                        plane = DrawShape.drawShape(plane, modelInstance, element, x, y, bpmnElement.getHeight(), bpmnElement.getWidth(), true, false);
                        fni = new FlowNodeInfo(element.getAttributeValue("id"), x, y, x, y, bpmnElement.getType().toString(), bpmnElement.getHeight(), bpmnElement.getWidth());
                        flowNodesMap.put("end-"+node.findValue("name").asText(), fni);
                    }

                    break;
            }

        }

        // Now draw the sequence flows
        seqIter = sequences.values().iterator();

        while(seqIter.hasNext()){
            sft = (SequenceFromTo) seqIter.next();
            String from = sft.GetFromID();
            String to = sft.GetToID();
            FlowNodeInfo fromFNI = (FlowNodeInfo) flowNodesMap.get(from);
            FlowNodeInfo toFNI = (FlowNodeInfo) flowNodesMap.get(to);

            SequenceFlow sf = modelInstance.newInstance(SequenceFlow.class);
            process.addChildElement(sf);

            String targetId = toFNI.getNewId();
            String sourceId = fromFNI.getNewId();

            FlowNode targetFlowNode = modelInstance.getModelElementById(targetId);
            FlowNode sourceFlowNode = modelInstance.getModelElementById(sourceId);

            sf.setSource(sourceFlowNode);
            sf.setTarget(targetFlowNode);

            if(sft.GetConditionExpression() != null) {
                ConditionExpression conditionExpression = modelInstance.newInstance(ConditionExpression.class);
                conditionExpression.setTextContent(sft.GetConditionExpression());
                sf.setConditionExpression(conditionExpression);
            }

            plane = DrawFlow.drawFlow(plane, modelInstance, sf, fromFNI, toFNI, null, 0d, 0d);

        }


        // Write output
        Bpmn.validateModel(modelInstance);
        File outputFile = new File(args[1]);
        Bpmn.writeModelToFile(outputFile, modelInstance);

        System.out.println("File has been successfully converted and can be found at "+args[1]);
    }
}
