/opt/samoa/bin/samoa s4 target/SAMOA-S4-0.0.1-SNAPSHOT.jar "com.yahoo.labs.samoa.tasks.hl.HLTrainTestEvaluation \
 -d /home/hl/lab/lab3/result/resultS1VHTm \
 -r (com.yahoo.labs.samoa.streams.hl.HLSplitDataPreProcessor -a 0.2 -b 0.8 -o 1 -i 10000) \
 -l (com.yahoo.labs.samoa.learners.classifiers.trees.VerticalHoeffdingTree) \
 -s (com.yahoo.labs.samoa.streams.hl.ArffFileStream -f '/home/hl/lab/lab3/data/m10000.url') -f 1000 -i 10000"