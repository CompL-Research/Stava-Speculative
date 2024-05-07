#!/bin/bash

# For printing time in echo statements
tstamp() { date +[%T]; }

# Sample script to be used to run the project on non-benchmark code.
# Set the paths according to your installation. All paths must be full paths.
# Instructions: ./run.sh ClassName
# Installed path of Java 8 JDK
#java_install_path="/home/aditya/Documents/Research-Workspace/JDK/jdk1.8.0_301"

java_install_path="/home/adityaanand/Documents/Research-Workspace/JDK/jdk1.8.0_301"

# The soot jar to be used.

soot_path=`realpath ../soot/sootclasses-trunk-jar-with-dependencies.jar`

# Path to stava repository
stava_path=`realpath ..`

# The directory to be analysed.
test_path=`realpath ../tests/test$1/`

# The directory inside which stava will output the results.
output_path=`realpath ../out/testcase/`

java_compiler="${java_install_path}/bin/javac"
java_vm="${java_install_path}/bin/java"

# find $test_path -type f -name '*.class' -delete
# echo compiling test...
echo -ne "$(tstamp) Compiling Test-Case: $1 \033[0K\r"
$java_compiler -cp $test_path ${test_path}/*.java  2>/dev/null
echo -e "$(tstamp) Compiled Test-Case: $1 \033[0K\r"

find ${stava_path}/src -type f -name '*.class' -delete
find $output_path -type f -name '*.info' -delete
find $output_path -type f -name '*.res' -delete
find $output_path -type f -name 'stats.txt' -delete

echo -ne "$(tstamp) Compiling the Static Analyser for OSASAD...\033[0K\r"
$java_compiler -cp $soot_path:${stava_path}/src ${stava_path}/src/main/Main.java 2>/dev/null
echo -e "$(tstamp) Compiled...\033[0K\r"
echo "$(tstamp) Generating the .res file..."
$java_vm -Xmx10g -Xss2m -classpath $soot_path:${stava_path}/src main.Main $java_install_path false $test_path Main $output_path $2 | tee >(grep -v '^\\[Debug\\]' > $output_path/log.txt)
