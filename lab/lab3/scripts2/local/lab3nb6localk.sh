/opt/samoa/bin/samoa local target/SAMOA-Local-0.0.1-SNAPSHOT.jar "com.yahoo.labs.samoa.tasks.hl.HLTrainTestEvaluation \
 -d /home/hl/lab/lab3/result/resultLocalNB6k \
 -r (com.yahoo.labs.samoa.streams.hl.HLSplitDataPreProcessor -a 0.2 -b 0.8 -o 1 -i 500000) \
 -l (com.yahoo.labs.samoa.learners.classifiers.hl.NaiveBayes -a 1 -m 1) \
 -p 1 \
 -s (com.yahoo.labs.samoa.moa.streams.ArffFileStream -f '/home/hl/lab/lab3/data/k.arff') -f 100000 -i 500000"
