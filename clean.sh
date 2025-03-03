#!/bin/bash
find ./src -type f -name '*.class' -delete
find ./out/testcase -type f -name '*.info' -delete
find ./out/testcase -type f -name '*.res' -delete
find ./out/testcase -type f -name 'stats.txt' -delete
find ./logs -type f -name '*.log' -delete