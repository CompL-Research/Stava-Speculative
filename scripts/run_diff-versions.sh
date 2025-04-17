#!/bin/bash
# Script to be used to run the project on a benchmark.

# For printing time in echo statements
tstamp() { date +[%T]; }

# Set the paths according to your installation. All paths must be full paths.
# Arguments: 1) name of benchmark eg: bash benchmark.sh dacapo

# List of DaCapo benchmarks
# declare -a dacapo_chopin_bench=("fop")

# #Iterate over each benchmark
# for benchmark in "${dacapo_chopin_bench[@]}"
# do
#     echo "*****************************************************************************"
#     echo "$(tstamp) ***** Benchmark: "\"$benchmark\" " *****" 
#     echo -ne "$(tstamp) Generating the benchmark directory...\033[0K\r"
#     # Create the benchmark directory
#     cd ../benchmarks/dacapo-chopin/dacapo-23.11-chopin
#     rm -rf out/*
#     mkdir out
#     unzip $benchmark.zip
#     mv $benchmark/* out/
#     echo -e "$(tstamp) \e[32mBenchmark directory created...\e[0m"
#     cd ../../../scripts
#     echo -e "$(tstamp) \e[32mGenerating the .res file specopt mode...\e[0m"
#     bash benchmark.sh newDacapo printpldi newDacapo
#     cp ../out/dacapo-chopin/*.res ${benchmark}-pldi-24.res
#     echo -e "$(tstamp) \e[32mGenerating the .res file specoptini mode...\e[0m"
#     bash benchmark.sh newDacapo specoptini newDacapo
#     cp ../out/dacapo-chopin/*.res ${benchmark}-specoptini.res
#     echo -e "$(tstamp) \e[32mGenerating the .res file specoptinibranch mode...\e[0m"
#     bash benchmark.sh newDacapo specoptinibranch newDacapo
#     cp ../out/dacapo-chopin/*.res ${benchmark}-specoptinibranch.res
#     echo -e "$(tstamp) \e[32mDone $benchmark !!!\033[0K\r\e[0m"
# done
# echo -e "\e[32m==================================================================\e[0m"

# # List of DaCapo benchmarks
# declare -a dacapo_chopin_bench=("lusearch" "luindex" "h2")

# #Iterate over each benchmark
# for benchmark in "${dacapo_chopin_bench[@]}"
# do
#     echo "*****************************************************************************"
#     echo "$(tstamp) ***** Benchmark: "\"$benchmark\" " *****" 
#     echo -ne "$(tstamp) Generating the benchmark directory...\033[0K\r"
#     # Create the benchmark directory
#     cd ../benchmarks/dacapo
#     rm -rf out/*
#     /home/aditya/wd/openj9-jdk8-pldi24/openj9-openjdk-jdk8/build/linux-x86_64-normal-server-release/images/j2sdk-image/bin/java -javaagent:poa-trunk.jar -jar dacapo-9.12-MR1-bach.jar $benchmark  
#     echo -e "$(tstamp) \e[32mBenchmark directory created...\e[0m"
#     cd ../../scripts
    
#     echo -e "$(tstamp) \e[32mGenerating the .res file specopt mode...\e[0m"
#     bash benchmark.sh dacapo printpldi null
#     cp ../out/dacapo/*.res ${benchmark}-pldi-24.res

#     echo -e "$(tstamp) \e[32mGenerating the .res file specoptini mode...\e[0m"
#     bash benchmark.sh dacapo specopt null
#     cp ../out/dacapo/*.res ${benchmark}-specopt.res

#     echo -e "$(tstamp) \e[32mGenerating the .res file specoptini mode...\e[0m"
#     bash benchmark.sh dacapo specoptini null
#     cp ../out/dacapo/*.res ${benchmark}-specoptini.res

#     echo -e "$(tstamp) \e[32mGenerating the .res file specoptinibranch mode...\e[0m"
#     bash benchmark.sh dacapo specoptinibranch null
#     cp ../out/dacapo/*.res ${benchmark}-specoptinibranch.res

#     echo -e "$(tstamp) \e[32mDone $benchmark !!!\033[0K\r\e[0m"
# done
# echo -e "\e[32m==================================================================\e[0m"


# List of DaCapo benchmarks
declare -a spec_jvm=("crypto.signverify")

#Iterate over each benchmark
for benchmark in "${spec_jvm[@]}"
do
    echo "*****************************************************************************"
    echo "$(tstamp) ***** Benchmark: "\"$benchmark\" " *****" 
    echo -ne "$(tstamp) Generating the benchmark directory...\033[0K\r"
    # Create the benchmark directory
    cd ../benchmarks/spec-jvm
    rm -rf out/*
    mkdir out
    unzip $benchmark.zip
    mv $benchmark/* out/
    echo -e "$(tstamp) \e[32mBenchmark directory created...\e[0m"
    cd ../../scripts
    
    # echo -e "$(tstamp) \e[32mGenerating the .res file specopt mode...\e[0m"
    # bash benchmark.sh jvm printpldi null
    # cp ../out/spec-jvm/*.res ${benchmark}-pldi-24.res

    # echo -e "$(tstamp) \e[32mGenerating the .res file specoptini mode...\e[0m"
    # bash benchmark.sh jvm specopt null
    # cp ../out/spec-jvm/*.res ${benchmark}-specopt.res

    echo -e "$(tstamp) \e[32mGenerating the .res file specoptini mode...\e[0m"
    bash benchmark.sh jvm specoptini null
    cp ../out/spec-jvm/*.res ${benchmark}-specoptini.res

    # echo -e "$(tstamp) \e[32mGenerating the .res file specoptinibranch mode...\e[0m"
    # bash benchmark.sh jvm specoptinibranch null
    # cp ../out/spec-jvm/*.res ${benchmark}-specoptinibranch.res

    echo -e "$(tstamp) \e[32mDone $benchmark !!!\033[0K\r\e[0m"
done
echo -e "\e[32m==================================================================\e[0m"