#!/bin/bash

# For printing time in echo statements
tstamp() { date +[%T]; }

# Sample script to be used to run the project on non-benchmark code.
# Set the paths according to your installation. All paths must be full paths.
# Instructions: ./run.sh ClassName
# Installed path of Java 8 JDK
#java_install_path="/home/aditya/Documents/Research-Workspace/JDK/jdk1.8.0_301"

java_install_path="/home/adityaanand/PhD/Research-Workspace/JDK/jdk1.8.0_301"

# The soot jar to be used.

soot_path=`realpath ../soot/sootclasses-trunk-jar-with-dependencies.jar`

# Path to stava repository
stava_path=`realpath ..`

# The directory to be analysed.
test_path=`realpath ../tests/micro-bench/test$1/`

# The directory inside which stava will output the results.
output_path=`realpath ../out/testcase/`

java_compiler="${java_install_path}/bin/javac"
java_vm="${java_install_path}/bin/java"

# find $test_path -type f -name '*.class' -delete
# echo compiling test...
echo -ne "$(tstamp) \e[32mCompiling Test-Case: $1 \033[0K\r\e[0m"
#rm $test_path ${test_path}/*.class 2>&1
error_output=$($java_compiler -cp $test_path ${test_path}/*.java 2>&1)
if [ $? -ne 0 ]; then
  echo "!!! Error in compiling Test CASE"
  echo "$error_output"  # Print the captured error output
  exit 1
fi

echo -e "$(tstamp) \e[32mCompiled Test-Case: $1 \033[0K\r\e[0m"

find ${stava_path}/src -type f -name '*.class' -delete
find $output_path -type f -name '*.info' -delete
find $output_path -type f -name '*.res' -delete
find $output_path -type f -name 'stats.txt' -delete
find ${stava_path}/logs -type f -name '*.log' -delete

echo -ne "$(tstamp) \e[32mCompiling the Static Analyser for OSASAD...\033[0K\r\e[0m"
$java_compiler -cp $soot_path:${stava_path}/src ${stava_path}/src/main/Main.java 2>/dev/null
echo -e "$(tstamp) \e[32mCompiled...\033[0K\r\e[0m"
echo -e "$(tstamp) \e[32mGenerating the .res file...\e[0m"
echo -e "\e[32m==================================================================\e[0m"
$java_vm -Xmx10g -Xss2m -classpath $soot_path:${stava_path}/src main.Main $java_install_path false $test_path $2 $output_path $3 | tee >(grep -v '^\\[INFO\\]' > $output_path/log.txt)
echo -e "\e[32m==================================================================\e[0m"

#$java_compiler ${stava_path}/src/utils/LogCleaner.java
#$java_vm ${stava_path}/src/utils/LogCleaner $stava_path
