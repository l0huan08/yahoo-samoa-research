/opt/samoa/bin/samoa s4 target/SAMOA-S4-0.0.1-SNAPSHOT.jar "com.yahoo.labs.samoa.tasks.hl.HLTrainTestEvaluation \
 -d /home/hl/lab/lab3/result/resultS2NB6k \
 -r (com.yahoo.labs.samoa.streams.hl.HLSplitDataPreProcessor -a 0.2 -b 0.8 -o 1 -i 500000) \
 -l (com.yahoo.labs.samoa.learners.classifiers.hl.NaiveBayes -a 1 -m 2) \
 -p 1 \
 -s (com.yahoo.labs.samoa.streams.hl.ArffFileStream -f '/home/hl/lab/lab3/data/k.url') -f 100000 -i 500000"
