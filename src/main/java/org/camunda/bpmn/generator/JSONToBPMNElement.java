package org.camunda.bpmn.generator;

public class JSONToBPMNElement {

    private Class type;
    private Double height;
    private Double width;

    public JSONToBPMNElement(Class inputType, Double inputHeight, Double inputWidth) {
        type = inputType;
        height = inputHeight;
        width = inputWidth;
    }

    public Class getType() {
        return type;
    }

    public Double getHeight() {
        return height;
    }

    public Double getWidth() {
        return width;
    }

}
