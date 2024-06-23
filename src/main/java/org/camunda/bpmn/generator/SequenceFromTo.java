package org.camunda.bpmn.generator;

public class SequenceFromTo {

    private String fromID;

    private String toID;

    public SequenceFromTo(String fromID, String toID) {
        this.fromID = fromID;
        this.toID = toID;
    }

    public String GetFromID() { return fromID; }

    public String GetToID() { return toID; }
}
