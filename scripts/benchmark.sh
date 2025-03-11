#!/bin/bash
# Script to be used to run the project on a benchmark.

# For printing time in echo statements
tstamp() { date +[%T]; }

# Set the paths according to your installation. All paths must be full paths.
# Arguments: 1) name of benchmark eg: bash benchmark.sh dacapo

# Installed path of Java 8 JDK
java_install_path="/home/aditya/wd/JDK/jdk1.8.0_301"

# Path to the directory containing all benchmarks. The subdirectories here must
# contain individual benchmarks 
benchmarks_base_path=`realpath ../benchmarks/`

# The soot jar to be used.
soot_path=`realpath ../soot/sootclasses-trunk-jar-with-dependencies.jar`
# soot_path="/home/dj/github/soot/target/sootclasses-trunk-jar-with-dependencies.jar"

# Path to stava repository
stava_path=`realpath ..`
stava_run="${stava_path}/src/"

# The directory inside which stava will output the results.
output_base_path=`realpath ../out/`
java_compiler="${java_install_path}/bin/javac"
java_vm="${java_install_path}/bin/java"

benchmark_name=""

clean () {
    echo clearing all files... >/dev/null 2>&1
    find  ${stava_path}/src -type f -name '*.class' -delete
    find $1 -type f -name '*.res' -delete
    find $1 -type f -name '*.info' -delete    
    find $1 -type f -name 'stats.txt' -delete
}
if [[ $1 == "dacapo" ]]; then
    benchmark_path="${benchmarks_base_path}/dacapo"
    output_path="${output_base_path}/dacapo"
    main_class="Harness"
    benchmark_name="DaCapo"
elif [[ $1 == "newDacapo" ]]; then
    benchmark_path="${benchmarks_base_path}/dacapo-chopin/dacapo-23.11-chopin"
    output_path="${output_base_path}/dacapo-chopin"
    main_class="Harness"
    benchmark_name="New DaCapo"
elif [[ $1 == "jbb" ]]; then
    benchmark_path="${benchmarks_base_path}/spec-jbb/"
    output_path="${output_base_path}/spec-jbb/"
    main_class="spec.jbb.JBBmain"
    benchmark_name="SpecJBB"
elif [[ $1 == "jvm" ]]; then
    benchmark_path="${benchmarks_base_path}/spec-jvm/"
    output_path="${output_base_path}/spec-jvm/"
    main_class="spec.harness.Launch"
    benchmark_name="SpecJVM 2008"
elif [[ $1 == "ren" ]]; then
        benchmark_path="${benchmarks_base_path}/renaissance/"
        output_path="${output_base_path}/renaissance/"
        main_class="org.renaissance.core.Launcher"
elif [[ $1 == "jgfall" ]]; then
    for dir in ${benchmarks_base_path}/jgf/JGF*
    do
        lib=${dir##*/}
        echo $lib
        output_path="${output_base_path}/jgf/${lib}"
        clean $output_path
        execute $dir $lib $output_path
    done
    exit 0
elif [[ $1 == "moldyn" ]]; then
    benchmark_path="${benchmarks_base_path}/jgf/Moldyn"
    output_path="${output_base_path}/jgf/Moldyn"
    main_class="JGFMolDynBenchSizeA"
    benchmark_name="JGFMolDynBenchSizeA"
elif [[ $1 == "barrier" ]]; then
    benchmark_path="${benchmarks_base_path}/jgf/JGFBarrierBench"
    output_path="${output_base_path}/jgf/JGFBarrierBench"
    main_class="JGFBarrierBench"
    benchmark_name="JGFBarrierBench"
elif [[ $1 == "montecarlo" ]]; then
    benchmark_path="${benchmarks_base_path}/jgf/JGFMonteCarloBenchSizeA"
    output_path="${output_base_path}/jgf/JGFMonteCarloBenchSizeA"
    main_class="JGFMonteCarloBenchSizeA"
    benchmark_name="JGFMonteCarloBenchSizeA"
elif [[ $1 == "raytracer" ]]; then
    benchmark_path="${benchmarks_base_path}/jgf/RayTracer"
    output_path="${output_base_path}/jgf/RayTracer"
    main_class="JGFRayTracerBenchSizeA"
    benchmark_name="JGFRayTracerBenchSizeA"
elif [[ $1 == "crypt" ]]; then
    benchmark_path="${benchmarks_base_path}/jgf/JGFCryptBenchSizeA"
    output_path="${output_base_path}/jgf/JGFCryptBenchSizeA"
    main_class="JGFCryptBenchSizeA"
    benchmark_name="JGFCryptBenchSizeA"
else
    echo path not recognised
    exit 0
fi
clean $output_path
echo -e "$(tstamp) \e[32mCompiling Stava -- Benchmark-Suite: \"$benchmark_name\" \033[0K\r\e[0m"
output=$($java_compiler -cp $soot_path:${stava_path}/src ${stava_path}/src/main/Main.java 2>&1)
if [ $? -ne 0 ]; then
  echo "$output"  # Show full output, including "Note:" warnings when there's an error
  echo -e "$(tstamp) \e[31m!!! Error in compiling the Static Analyser !!! \033[0m"
  exit 1
fi
echo -e "$(tstamp) \e[32mCompiled!!!\033[0K\r\e[0m"
echo -e "$(tstamp) \e[32mGenerating the .res file...\e[0m"
echo -e "\e[32m==================================================================\e[0m"
$java_vm -Xmx32g -Xss10m -classpath $soot_path:$stava_run main.Main $java_install_path true $benchmark_path $main_class $output_path $2 $3
echo -e "\e[32m==================================================================\e[0m"