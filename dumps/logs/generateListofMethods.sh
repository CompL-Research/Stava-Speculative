#With optlevel as warm 
taskset -c 18 /home/aditya/Documents/openj9/openj9/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/images/j2sdk-image/bin/java -XX:-EnableHCR -Xjit:Count=0,optlevel=warm,traceEscapeAnalysis,log=logs/avrora.debug,EAResfile=../avrora-contextual.res,debugCounters={AllocationStatistics*},staticDebugCounters={AllocationStatistics*} -jar ../benchmarks/dacapo/dacapo-9.12-MR1-bach.jar avrora

#Without optlevel 
#taskset -c 18 /home/aditya/Documents/openj9/openj9/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/images/j2sdk-image/bin/java -XX:-EnableHCR -Xjit:Count=0,traceEscapeAnalysis,log=logs/avrora.debug,EAResfile=../avrora-contextual.res,debugCounters={AllocationStatistics*},staticDebugCounters={AllocationStatistics*} -jar ../benchmarks/dacapo/dacapo-9.12-MR1-bach.jar avrora

cat avrora* | rg 'method=' > methodlist.txt









#old Way
#tail -n+1 methodlist.txt | tr -d \" |tr -d "\t" >finalMethodList.txt
#sed -i 's/method=//g' finalMethodList.txt
#sort finalMethodList.txt | uniq > finalMethods.txt

