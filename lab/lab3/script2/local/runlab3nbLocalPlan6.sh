/opt/samoa/bin/samoa local target/SAMOA-Local-0.0.1-SNAPSHOT.jar "com.yahoo.labs.samoa.tasks.hl.HLTrainTestEvaluation \
 -d /home/hl/lab/lab3/result/resultLocalPlan6k \
 -l (com.yahoo.labs.samoa.learners.classifiers.hl.NaiveBayes -a 2 -m 2 -b 100) \
 -r (com.yahoo.labs.samoa.streams.hl.HLSplitDataPreProcessor \
     -a 0.2 -b 0.8 -o 1 -i 2000)
 -p 1 \
 -s (com.yahoo.labs.samoa.moa.streams.ArffFileStream -f '/home/hl/data/m.arff') -f 100 -i 2000"
