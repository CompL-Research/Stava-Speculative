# Static Analyzer for Speculative Stack Allocation (SASSA)
* This work uses the idea of staging the escape analysis across static and dynamic time.
  
* For a language like Java where statically code for the whole program is not available, in that case, we denote the unknown in some form of dependencies (Idea from [PYE framework]: https://dl.acm.org/doi/10.1145/3337794).

* This code-base has a static analyzer that first generates intraprocedural points-to-analysis and dependencies, then using both it performs precise escape analysis to identify the list of stack allocable objects at runtime.

* Further at places like polymorphic call-sites or statements in conditionals (where the static analysis can go conservative and imprecise), if there is a possibility of improving precision by doing speculation at runtime based on runtime profile information, it generates speculative results conditional to profile data at runtime.

* The idea is to also have the deoptimization information for each place where speculation is made. Note that here deoptimization refers to performing heapification of the objects for this optimization.

* The generated result is then given to the JVM, where the JVM is instructed to allocate those objects on the stack instead of the heap. In case some speculations are being made at runtime and we have statically generated results for those speculations, we use those results to allocate objects on the stack. In case the speculation goes wrong, we deoptimize and heapify the objects
.
## Getting Started

### Installation
This project only requires a working installation of Java 8. Clone the repo and you're good to go! Use scripts from the [scripts](https://github.com/adityaanand7/Speculative-Stack-Allocation/tree/main/scripts) package and set them up according to your installation.

## Analysing Code 
Sample scripts are provided in the [scripts](https://github.com/adityaanand7/Speculative-Stack-Allocation/tree/main/scripts) directory. There are 2 types of usecases for stava.
* Benchmark Code: This code is expected to be precompiled. These can be benchmarks like DaCapo.
* Application Code: This is code written by user that has to be compiled.
More instructions [here](https://github.com/adityaanand7/Speculative-Stack-Allocation/README.md).

## Built With
* [Soot](https://github.com/soot-oss/soot)- a Java optimization framework which enables this project to look into class files and much more. 

## Authors
* [*Aditya Anand*](https://adityaanand7.github.io)
* [*Manas Thakur*](https://www.cse.iitb.ac.in/~manas/) 

