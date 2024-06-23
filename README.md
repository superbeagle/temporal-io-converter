# JSON to Camunda 8 BPMN Converter 


## How to use this utility
After cloning the repository and performing the necessary maven commands, either run the

```org.camunda.bpmn.generator.BPMNGenFromJSON```

main class in your IDE, passing in as arguments the input file and the output file or generate an executable jar file. The contents of the input file will be read and the output BPMN file will be generated. If you wish to generate an executable jar file issue the following maven command

```mvn clean compile assembly:single```

and execute the following command using the resulting jar file

```java -jar BPMNModelGenerator-1.0-SNAPSHOT-jar-with-dependencies input-file output-file```

A sample process in JSON:
![](./readme_images/sampleJSONProcessDiagram.png)

And after conversion:
![](./readme_images/ConvertedProcessFromJSON.png)

## Notes and TODOs
It has been tested with a limited number of JSON examples and it may require tweaks depending on your process.
