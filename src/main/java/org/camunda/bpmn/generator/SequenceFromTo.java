package org.camunda.bpmn.generator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SequenceFromTo {

    private String fromID;

    private String toID;

    private String conditionExpression;

    public SequenceFromTo(String fromID, String toID) {
        this.fromID = fromID;
        this.toID = toID;
    }

    public SequenceFromTo(String fromID, String toID, String conditionExpression) {
        this.fromID = fromID;
        this.toID = toID;
        this.conditionExpression = convertConditionExpression(conditionExpression);
    }

    public String GetFromID() { return fromID; }

    public String GetToID() { return toID; }

    public String GetConditionExpression() { return conditionExpression; }

    public String convertConditionExpression(String conditionExpression) {
        String regex = "\\$\\{\\s*(.+)\\s*\\}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(conditionExpression);

        // Check if the input string matches the pattern
        if (matcher.matches()) {
            String dotRegex = "^\\.+";
            String result = matcher.group(1);
            result = result.replaceFirst(dotRegex, "");
            result = "=" + result;
            return result;
        } else {
            return conditionExpression;
        }
    }
}
