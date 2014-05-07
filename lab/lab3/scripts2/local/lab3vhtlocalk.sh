/opt/samoa/bin/samoa local target/SAMOA-Local-0.0.1-SNAPSHOT.jar  "com.yahoo.labs.samoa.tasks.hl.HLTrainTestEvaluation \
 -d /home/hl/lab/lab3/result/resultLocalVHTk \
 -r (com.yahoo.labs.samoa.streams.hl.HLSplitDataPreProcessor -a 0.2 -b 0.8 -o 1 -i 500000) \
 -l (com.yahoo.labs.samoa.learners.classifiers.trees.VerticalHoeffdingTree) \
 -s (ArffFileStream -f /home/hl/lab/lab3/data/k.arff) -f 100000 -i 500000"