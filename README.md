# Stava with context-senstive and inlining results

* Stava is a static program analysis for identifying stack allocable objects of code written in Java. With the results generated, a JVM can be instructed 
  to allocate those objects on the stack instead of the heap. Analysis is performed on java bytecode and Stava will only generate partial results if 
  library code is unavailable. This project is based on the [PYE](https://dl.acm.org/doi/10.1145/3337794) framework.

* For each different calling context (where context is the callsite) it analyzes the methods seprately and genrates conetxt senstive results.

* Along with that it gives the inlining information for each call-site such that if the callee is inlined in the caller then list of all the objects which can be stack allocated on the caller's stack.
  
## Getting Started

### Installation
This project only requires a working installation of Java 8. Clone the repo and you're good to go! Use scripts from the [scripts](https://github.com/adityaanand7/stava-contextual/tree/master/scripts) package and set them up according to your installation.

## Analysing Code 
Sample scripts are provided in the [scripts](https://github.com/adityaanand7/stava-contextual/tree/master/scripts) directory. There are 2 types of usecases for stava.
* Benchmark Code: This code is expected to be precompiled. These can be benchmarks like DaCapo.
* Application Code: This is code written by user that has to be compiled.
More instructions [here](https://github.com/adityaanand7/stava-contextual/blob/master/scripts/README.md).

## Built With
* [Soot](https://github.com/soot-oss/soot)- a Java optimization framework which enables this project to look into class files and much more. 

## Authors
* [*Aditya Anand*](https://adityaanand7.github.io)
* [*Manas Thakur*](https://manas.gitlab.io) 

