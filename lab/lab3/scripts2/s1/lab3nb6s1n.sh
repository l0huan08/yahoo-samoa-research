/opt/samoa/bin/samoa s4 target/SAMOA-S4-0.0.1-SNAPSHOT.jar "com.yahoo.labs.samoa.tasks.hl.HLTrainTestEvaluation \
 -d /home/hl/lab/lab3/result/resultS1NB6n \
 -r (com.yahoo.labs.samoa.streams.hl.HLSplitDataPreProcessor -a 0.2 -b 0.8 -o 1 -i 50000) \
 -l (com.yahoo.labs.samoa.learners.classifiers.hl.NaiveBayes -a 1 -m 1) \
 -p 1 \
 -s (com.yahoo.labs.samoa.streams.hl.ArffFileStream -f '/home/hl/lab/lab3/data/n.url') -f 10000 -i 50000"
