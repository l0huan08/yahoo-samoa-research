/**
 * 
 */
package com.yahoo.labs.samoa.learners.classifiers.hl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javacliparser.*;
import com.yahoo.labs.samoa.core.Processor;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.learners.Learner;
import com.yahoo.labs.samoa.topology.Stream;
import com.yahoo.labs.samoa.topology.TopologyBuilder;

/**
 * @author hl
 * NaiveBayes Classifier for SAMOA
 * Plan 6
 * 
 * Vertical Parallel Naive Bayes algorithm
 * support numeric & nominal attributes
 * 
 * 2014.4.16 first version
 * 2014.4.18 use new topology
 * 2014.4.29 correct bug of connecting streams, to make NBAttStat to create 
 *   multiple times in S4 mode
 * 2014.5.5 only support p1=1, because if p1>1, will not create NBAttStata in node 2
 */
public class NaiveBayes implements Learner, Configurable{
	
	/*
	 * NB-6 Topology:
	 * 
	 *                   |----------testStream(shuffle)--------------------------------------------------|		
	 *                   |                                                                               \/
	 * inputStream-->[NBDistributor:1]--trainStream(all)->[NBAttributeStat:p1]-attStatStream(all)->[NBModel:p2]-->resultStream
	 *                                              
	 * Use multiple NBModel to do horizontal parallel for testing instances
	 * and NBAttributeStat update the NBModels
	 */

	/**
	 * 
	 */
	private static final long serialVersionUID = -2927602391151792705L;
	
	private static Logger logger = LoggerFactory.getLogger(NaiveBayes.class);

	/**
	 * Parallel Level , it is the number of this Classifiers
	 * also the number of NBDistributorProcessor
	 */
	private int parallelism; // should be 1, useless
	
	private NBDistributor nbDistributorP;
	private NBAttributeStat nbAttStatP;
	//private NBClassStat nbClassStatP;
	//private NBAggregator nbAggregatorP;
	private NBModelProcessor nbModelP;
	
	private Stream trainStream;
	private Stream testStream;
	private Stream attStatStream;
	//private Stream classStatStream;
	//private Stream aggControlStream;
	//private Stream aggResponseStream;
		
	private Stream resultStream; //the data of prediction result
	
	
	public IntOption nParallelAttributeStat = new IntOption(
            "parallelAttributeStat",
            'a',
            "number of AttributeStat Processors",
            1, 1, 1); //2014.5.5 only support 1 attributeStat
          //  1, 1, Integer.MAX_VALUE);
	
//	public IntOption nParallelClassStat = new IntOption(
//            "parallelClassStat",
//            'c',
//            "number of ClassStat Processors",
//            1, 1, Integer.MAX_VALUE);
	
	public IntOption nParallelModel = new IntOption(
            "parallelModel",
            'm',
            "number of NBModel Processors",
            1, 1, Integer.MAX_VALUE);
	
	public IntOption nBatchSize = new IntOption(
            "batchSize",
            'b',
            "train and testing is group by group, batchSize is number of instances in a group",
            200, 1, Integer.MAX_VALUE);
	
	@Override
	public void init(TopologyBuilder builder, Instances dataset, int parallelism) {
		logger.info("================================================");
		logger.info("Begin init NaiveBayes Classifier topology.");
		
		this.parallelism = parallelism;//no use. [only used for bagging algorithms]
		
		// Create Processors
		int p1 = this.nParallelAttributeStat.getValue();
		//int p2 = nParallelClassStat.getValue();
		int p2 = this.nParallelModel.getValue();
		int batchSize = this.nBatchSize.getValue();
		
		this.nbDistributorP = new NBDistributor.Builder().batchSize(batchSize).build();
		this.nbAttStatP = new NBAttributeStat.Builder().dataset(dataset).p1(p1).updateFrequency(batchSize).build();
		this.nbModelP = new NBModelProcessor.Builder().dataset(dataset).build(); 
		
		builder.addProcessor(this.nbDistributorP,1);
		builder.addProcessor(this.nbAttStatP,p1);
		builder.addProcessor(this.nbModelP,p2);
		
		logger.debug("NB Processors added.");
		
		// Create Streams
//---------------- way 1 ---------------------------------------		
		//this.trainStream = builder.createInputKeyStream(nbAttStatP,nbDistributorP);
		//this.trainStream = builder.createInputShuffleStream(nbAttStatP,nbDistributorP); //this line is not useful
		this.trainStream = builder.createInputAllStream(nbAttStatP,nbDistributorP); //this line is not useful
			
		
		this.attStatStream = builder.createInputAllStream(nbModelP,nbAttStatP);
		this.testStream = builder.createInputShuffleStream(nbModelP,nbDistributorP);
		this.resultStream = builder.createStream(this.nbModelP);
		
		this.nbDistributorP.setTrainStream(trainStream);
		this.nbDistributorP.setTestStream(testStream);
		this.nbAttStatP.setAttStatStream(attStatStream);
		this.nbModelP.setResultStream(resultStream);
		
// ---------------  way 2 -------------------------------------		
//		this.trainStream = builder.createStream(nbDistributorP);
//		this.nbDistributorP.setTrainStream(trainStream);
//		builder.connectInputAllStream(trainStream, nbAttStatP);
//		
//		this.attStatStream = builder.createStream(nbAttStatP);
//		this.nbAttStatP.setAttStatStream(attStatStream);
//		builder.connectInputAllStream(attStatStream, nbModelP);
//		
//		this.testStream = builder.createStream(nbDistributorP);
//		this.nbDistributorP.setTestStream(testStream);
//		builder.connectInputShuffleStream(testStream, nbModelP);
//		
//		this.resultStream = builder.createStream(this.nbModelP);
//		this.nbModelP.setResultStream(resultStream);
// -------------------------------------------------------------
		
		logger.info("Sucessfully initializing NaiveBayes classifier topology.");
		
		builder.build();
	}
	
	@Override
	public Processor getInputProcessor() {
		return this.nbDistributorP;
	}

	@Override
	public Stream getResultStream() {
		return this.resultStream;
	}

}
