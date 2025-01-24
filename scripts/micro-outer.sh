#!bin/bash

# Print the dot ansd wait
loop() {
for i in {2..25}
do
    echo -n "."
    sleep 0.1
done

}


#==============================#
# Execute the micro-benchmarks

for j in {0..0}
do
echo -n " Micro-bench $j executing..."

loop && bash micro-inner.sh $j Main null > log.txt 2>errlog.txt

diff <(sort /home/adityaanand/PhD/Research-Workspace/static-analysis/Speculative-Stack-Allocation/out/testcase/test$j.res) <(sort /home/adityaanand/PhD/Research-Workspace/static-analysis/Speculative-Stack-Allocation/tests/micro-bench/test$j/result.txt)

if [ $? -ne 0 ]; then
   echo -e " !!! Error :: Micro-bench $j failed  \xE2\x9D\x8C"
else
   echo -e " Micro-bench $j : Passed \xE2\x9C\x94"
fi
done











#
##28
##bash run.sh 28
#echo -n " Micro-bench 28 executing..."
#
#loop && bash run.sh 28 > log.txt 2>errlog.txt
#
#diff /home/aditya/Documents/Research-Workspace/stava-contextual/out/testcase/test28.res /home/aditya/Documents/Research-Workspace/stava-contextual/tests/test28/result.txt
#
#if [ $? -ne 0 ]; then
#   echo -e " !!! Error :: Micro-bench 28 failed  \xE2\x9D\x8C"
#else
#   echo -e " Micro-bench 28 : Passed \xE2\x9C\x94"
#fi
##==============================#
##29
#echo -n " Micro-bench 29 executing..."
#
#loop && bash run.sh 29 > log.txt 2>errlog.txt
#
#diff /home/aditya/Documents/Research-Workspace/stava-contextual/out/testcase/test29.res /home/aditya/Documents/Research-Workspace/stava-contextual/tests/test29/result.txt
#
#if [ $? -ne 0 ]; then
#   echo -e " !!! Error :: Micro-bench 38 failed  \xE2\x9D\x8C"
#else
#   echo -e " Micro-bench 29 : Passed \xE2\x9C\x94"
#fi
##==============================#
##37
#echo -n " Micro-bench 37 executing..."
#
#loop && bash run.sh 37 > log.txt 2>errlog.txt
#
#diff /home/aditya/Documents/Research-Workspace/stava-contextual/out/testcase/test37.res /home/aditya/Documents/Research-Workspace/stava-contextual/tests/test37/result.txt
#
#if [ $? -ne 0 ]; then
#   echo -e " !!! Error :: Micro-bench 37 failed  \xE2\x9D\x8C"
#else
#   echo -e " Micro-bench 37 : Passed \xE2\x9C\x94"
#fi
##==============================#
##38
#echo -n " Micro-bench 38 executing..."
#
#loop && bash run.sh 38 > log.txt 2>errlog.txt
#
#diff /home/aditya/Documents/Research-Workspace/stava-contextual/out/testcase/test38.res /home/aditya/Documents/Research-Workspace/stava-contextual/tests/test38/result.txt
#
#if [ $? -ne 0 ]; then
#   echo -e " !!! Error :: Micro-bench 38 failed  \xE2\x9D\x8C"
#else
#   echo -e " Micro-bench 38 : Passed \xE2\x9C\x94"
#fi
##==============================#
##39
#echo -n " Micro-bench 39 executing..."
#
#loop && bash run.sh 39 > log.txt 2>errlog.txt
#
#diff /home/aditya/Documents/Research-Workspace/stava-contextual/out/testcase/test39.res /home/aditya/Documents/Research-Workspace/stava-contextual/tests/test39/result.txt
#
#if [ $? -ne 0 ]; then
#   echo -e " !!! Error :: Micro-bench 39 failed  \xE2\x9D\x8C"
#else
#   echo -e " Micro-bench 39 : Passed \xE2\x9C\x94"
#fi
##==============================#
##40
#echo -n " Micro-bench 40 executing..."
#
#loop && bash run.sh 40 > log.txt 2>errlog.txt
#
#diff /home/aditya/Documents/Research-Workspace/stava-contextual/out/testcase/test40.res /home/aditya/Documents/Research-Workspace/stava-contextual/tests/test40/result.txt
#
#if [ $? -ne 0 ]; then
#   echo -e " !!! Error :: Micro-bench 40 failed  \xE2\x9D\x8C"
#else
#   echo -e " Micro-bench 40 : Passed \xE2\x9C\x94"
#fi
##==============================#
##42
#echo -n " Micro-bench 42 executing..."
#
#loop && bash run.sh 42 > log.txt 2>errlog.txt
#
#diff /home/aditya/Documents/Research-Workspace/stava-contextual/out/testcase/test42.res /home/aditya/Documents/Research-Workspace/stava-contextual/tests/test42/result.txt
#
#if [ $? -ne 0 ]; then
#   echo -e " !!! Error :: Micro-bench 42 failed  \xE2\x9D\x8C"
#else
#   echo -e " Micro-bench 42 : Passed \xE2\x9C\x94"
#fi
##==============================#
##43
#echo -n " Micro-bench 43 executing..."
#
#loop && bash run.sh 43 > log.txt 2>errlog.txt
#
#diff /home/aditya/Documents/Research-Workspace/stava-contextual/out/testcase/test43.res /home/aditya/Documents/Research-Workspace/stava-contextual/tests/test43/result.txt
#
#if [ $? -ne 0 ]; then
#   echo -e " !!! Error :: Micro-bench 43 failed  \xE2\x9D\x8C"
#else
#   echo -e " Micro-bench 43 : Passed \xE2\x9C\x94"
#fi
