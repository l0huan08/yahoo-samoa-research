/opt/samoa/bin/samoa local target/SAMOA-Local-0.0.1-SNAPSHOT.jar  "com.yahoo.labs.samoa.tasks.hl.HLTrainTestEvaluation \
 -d /home/hl/lab/lab3/result/resultLocalNB5n \
 -r (com.yahoo.labs.samoa.streams.hl.HLSplitDataPreProcessor -a 0.2 -b 0.8 -o 1 -i 50000) \
 -l (com.yahoo.labs.samoa.learners.classifiers.hl.NaiveBayes) \
 -s (ArffFileStream -f /home/hl/lab/lab3/data/n.arff) -f 10000 -i 50000"