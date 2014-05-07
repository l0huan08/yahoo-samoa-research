package com.yahoo.labs.samoa.tasks.hl;

/*
 * #%L
 * SAMOA
 * %%
 * Copyright (C) 2013 Yahoo! Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javacliparser.ClassOption;
import com.github.javacliparser.Configurable;
import com.github.javacliparser.FileOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.StringOption;
import com.yahoo.labs.samoa.evaluation.BasicClassificationPerformanceEvaluator;
import com.yahoo.labs.samoa.evaluation.ClassificationPerformanceEvaluator;
import com.yahoo.labs.samoa.evaluation.EvaluatorProcessor;
import com.yahoo.labs.samoa.learners.Learner;
import com.yahoo.labs.samoa.learners.classifiers.trees.VerticalHoeffdingTree;
import com.yahoo.labs.samoa.moa.streams.InstanceStream;
import com.yahoo.labs.samoa.moa.streams.generators.RandomTreeGenerator;
import com.yahoo.labs.samoa.topology.ComponentFactory;
import com.yahoo.labs.samoa.topology.Stream;
import com.yahoo.labs.samoa.topology.Topology;
import com.yahoo.labs.samoa.topology.TopologyBuilder;

import com.yahoo.labs.samoa.tasks.*;
import com.yahoo.labs.samoa.streams.hl.*;

/**
 * Train and Test Evaluation task is a scheme in evaluating performance of online classifiers which
 *  uses some of the source instances for training online classifiers model and
 *  uses some of the source instances for testing the model(Train and test)
 *
 * Topology:
 * DataSourceProcessor[1] --shuffle--> DataPreProcessor[p] --shuffle--> Learner,paralelism=p[1] --shuffle-->Evaluator[1]--> result 
 * Finished
 * @author Li Huang
 * 
 */
public class HLTrainTestEvaluation implements Task, Configurable {

    /**
	 * gen seiral id by HL
	 */
	private static final long serialVersionUID = 8142762384570430648L;

	private static Logger logger = LoggerFactory.getLogger(HLTrainTestEvaluation.class);

    public ClassOption learnerOption = new ClassOption("learner", 'l', "Classifier to train.", Learner.class, VerticalHoeffdingTree.class.getName());

    //------------ add by Li Huang --------------
    public ClassOption streamTrainOption = new ClassOption("trainStream", 's', "Stream to learn from.", InstanceStream.class,
            RandomTreeGenerator.class.getName());
    
    public ClassOption dataPreProcessorOption = new ClassOption("dataPreProcessor",'r',"the pre-processor of data source, that divide data into trainining and test sets.", HLDataPreProcessor.class,
    			HLSplitDataPreProcessor.class.getName());
    
    public IntOption parallelPreprocessorOption = new IntOption("parallelPreprocessor",'p', "the number of pre-processors",1,1,Integer.MAX_VALUE);
    //---------------------------------------------
    
    public ClassOption evaluatorOption = new ClassOption("evaluator", 'e', "Classification performance evaluation method.",
            ClassificationPerformanceEvaluator.class, BasicClassificationPerformanceEvaluator.class.getName());

    public IntOption instanceLimitOption = new IntOption("instanceLimit", 'i', "Maximum number of instances to test/train on  (-1 = no limit).", 1000000, -1,
            Integer.MAX_VALUE);

    public IntOption timeLimitOption = new IntOption("timeLimit", 't', "Maximum number of seconds to test/train for (-1 = no limit).", -1, -1,
            Integer.MAX_VALUE);

    public IntOption sampleFrequencyOption = new IntOption("sampleFrequency", 'f', "How many instances between samples of the learning performance.", 100000,
            0, Integer.MAX_VALUE);

    public StringOption evaluationNameOption = new StringOption("evalutionName", 'n', "Identifier of the evaluation", "Prequential_"
            + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

    public FileOption dumpFileOption = new FileOption("dumpFile", 'd', "File to append intermediate csv results to", null, "csv", true);

    
    /**
     * Get the datasource
     */
    private HLDataSourceProcessor dataSourceProcessor; //add by hl
    private HLDataPreProcessor dataPreProcessor; //add by hl

    // private PrequentialSourceTopologyStarter preqStarter;

    // private EntranceProcessingItem sourcePi;

    private Stream sourcePiOutputStream;
    
    private Stream prePiOutputStream; // add by HL

    private Learner classifier;

    private EvaluatorProcessor evaluator;

    // private ProcessingItem evaluatorPi;

    private Stream evaluatorPiInputStream;

    private Topology prequentialTopology;

    private TopologyBuilder builder;

    public void getDescription(StringBuilder sb, int indent) {
        sb.append("Prequential evaluation");
    }

    @Override
    public void init() {
        // TODO remove the if statement
        // theoretically, dynamic binding will work here!
        // test later!
        // for now, the if statement is used by Storm

        if (builder == null) {
            builder = new TopologyBuilder();
            logger.debug("Sucessfully instantiating TopologyBuilder");

            builder.initTopology(evaluationNameOption.getValue());
            logger.debug("Sucessfully initializing SAMOA topology with name {}", evaluationNameOption.getValue());
        }

        // instantiate PrequentialSourceProcessor and its output stream (sourcePiOutputStream)
        dataSourceProcessor = new HLDataSourceProcessor();
        dataSourceProcessor.setStreamSource((InstanceStream) this.streamTrainOption.getValue());
        dataSourceProcessor.setMaxNumInstances(instanceLimitOption.getValue());
        builder.addEntranceProcessor(dataSourceProcessor);
       
        logger.debug("Sucessfully instantiating HLDataSourceProcessor");

        // preqStarter = new PrequentialSourceTopologyStarter(preqSource, instanceLimitOption.getValue());
        // sourcePi = builder.createEntrancePi(preqSource, preqStarter);
        // sourcePiOutputStream = builder.createStream(sourcePi);

        
        sourcePiOutputStream = builder.createStream(dataSourceProcessor);
        // preqStarter.setInputStream(sourcePiOutputStream);

        // init pre-processor and connect it to sourcePiOutputStream
        dataPreProcessor = (HLDataPreProcessor)this.dataPreProcessorOption.getValue();
        dataPreProcessor.init( this.parallelPreprocessorOption.getValue());
        builder.addProcessor(dataPreProcessor, this.parallelPreprocessorOption.getValue());
        builder.connectInputShuffleStream(sourcePiOutputStream,dataPreProcessor);
        prePiOutputStream = builder.createStream(dataPreProcessor);
        dataPreProcessor.setOutputStream(prePiOutputStream);
        
        logger.debug("Sucessfully instantiating pre-processor");
        
        
        
        // instantiate classifier and connect it to sourcePiOutputStream
        classifier = (Learner) this.learnerOption.getValue();
        classifier.init(builder, dataSourceProcessor.getDataset(), 1);
        builder.connectInputShuffleStream(prePiOutputStream, classifier.getInputProcessor());
        logger.debug("Sucessfully instantiating Classifier");

        evaluatorPiInputStream = classifier.getResultStream();
        evaluator = new EvaluatorProcessor.Builder((ClassificationPerformanceEvaluator) this.evaluatorOption.getValue())
                .samplingFrequency(sampleFrequencyOption.getValue()).dumpFile(dumpFileOption.getFile()).build();

        // evaluatorPi = builder.createPi(evaluator);
        // evaluatorPi.connectInputShuffleStream(evaluatorPiInputStream);
        builder.addProcessor(evaluator);
        builder.connectInputShuffleStream(evaluatorPiInputStream, evaluator);

        logger.debug("Sucessfully instantiating EvaluatorProcessor");

        prequentialTopology = builder.build();
        logger.debug("Sucessfully building the topology");
    }

    @Override
    public void setFactory(ComponentFactory factory) {
        // TODO unify this code with init()
        // for now, it's used by S4 App
        // dynamic binding theoretically will solve this problem
        builder = new TopologyBuilder(factory);
        logger.debug("Sucessfully instantiating TopologyBuilder");

        builder.initTopology(evaluationNameOption.getValue());
        logger.debug("Sucessfully initializing SAMOA topology with name {}", evaluationNameOption.getValue());

    }

    public Topology getTopology() {
        return prequentialTopology;
    }
    //
    // @Override
    // public TopologyStarter getTopologyStarter() {
    // return this.preqStarter;
    // }
}
