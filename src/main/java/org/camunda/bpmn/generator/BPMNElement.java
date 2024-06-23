package org.camunda.bpmn.generator;

public class BPMNElement {

    private final Class type;
    private final Double height;
    private final Double width;

    public BPMNElement(Class inputType, Double inputHeight, Double inputWidth) {
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
