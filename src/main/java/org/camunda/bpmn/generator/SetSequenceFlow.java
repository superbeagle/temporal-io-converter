package org.camunda.bpmn.generator;

import io.camunda.zeebe.model.bpmn.instance.Process;
import io.camunda.zeebe.model.bpmn.instance.FlowNode;
import io.camunda.zeebe.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.xml.ModelInstance;

public class SetSequenceFlow {

    public static SequenceFlow setSequenceFlow(ModelInstance modelInstance, Process process, FlowNodeInfo fromFNI, FlowNodeInfo toFNI) {

        SequenceFlow sf = modelInstance.newInstance(SequenceFlow.class);
        process.addChildElement(sf);

        String targetId = toFNI.getNewId();
        String sourceId = fromFNI.getNewId();

        FlowNode targetFlowNode = modelInstance.getModelElementById(targetId);
        FlowNode sourceFlowNode = modelInstance.getModelElementById(sourceId);

        sf.setSource(sourceFlowNode);
        sf.setTarget(targetFlowNode);

        return sf;
    }
}
