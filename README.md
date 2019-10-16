# Sen4Smells
Sen4Smells is tool for prioritizing architecture-sensitive smells based on a technical debt index (e.g., ADI, SDI, etc.)
Sen4Smells is licenced under the Apache License V2.0.

The main functionality of the Sen4Smells is providing assistence to engineers for interpretating Technical Debt metrics in terms problematic Architectural Smells and system packages.

Sen4Smells is able to perform a sensitivity analysis for a collection of system values provided by a predetermined debt index. Our approach relies on two building blocks: 
1. The adaptation of an existing SA method to AS-based debt indices.
2. A decomposition strategy for dealing with the index at different granularity levels. 

By leveraging on Architectural Smells, the goal of the Sensitivity Analysis is to understand how variations in the Technical Debt can be attributed to variations in features of system elements. To do so, the sensitivity analysis performs a screening of the multiple variables affecting the index over time, and returns the most sensitive ones (i.e., key elements) to the engineer (tool user). The inputs for this analysis are: a set of previous system versions, the formula for computing a particular Technical Debt, and the granularity level for the variables (e.g., smell types, individual smells, or packages). 

The tool is designed as pipeline, in which existing modules for detecting smells and computing metrics from the source code can be configured. These smells and metrics depend on the Technical Debt under consideration, which is also a parameter for the pipeline. 

![Tool Pipeline](https://github.com/tommantonela/Sen4Smells/blob/gh-pages/general_pipeline.png)

## Usage

A description of the software's architecture, tutorials and file examples can be found in the project's [Wiki](https://github.com/tommantonela/Sen4Smells/wiki).
A video demonstrating usage cases can be found at https://www.youtube.com/watch?v=6RL0qCqZYPM.

## Reproducibility Kit
A dataset with 3 analyzed systems (Apache Camel, Apache Cxf, and Hibernate) is avalable [here](https://github.com/tommantonela/Sen4Smells/wiki).

<!--
## Contact information

- _Antonela Tommasel_ (ISISTAN, CONICET-UNICEN. Argentina) antonela.tommasel@isistan.unicen.edu.ar
- _J. Andres Diaz-Pace_ (ISISTAN, CONICET-UNICEN. Argentina) andres.diazpace@isistan.unicen.edu.ar 
- _Ilaria Pigazzini_ (Department of Informatics, Systems and Communication, Università of Milano-Bicocca, Italy) i.pigazzini@campus.unimib.it 
- _Francesca Arcelli Fontana_ (Department of Informatics, Systems and Communication, Università of Milano-Bicocca, Italy) arcelli@disco.unimib.it 
-->
