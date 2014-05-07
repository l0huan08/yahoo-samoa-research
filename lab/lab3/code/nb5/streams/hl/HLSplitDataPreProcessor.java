package com.yahoo.labs.samoa.streams.hl;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javacliparser.Configurable;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;

import com.yahoo.labs.samoa.core.ContentEvent;
import com.yahoo.labs.samoa.core.Processor;
import com.yahoo.labs.samoa.learners.InstanceContentEvent;
import com.yahoo.labs.samoa.topology.Stream;

/**
 * SplitDataPreProcessor is the processor for TrainTest Evaluation Task.
 * This processor split the data source into a% training data and b% testing data
 * For each data coming, it has a probability of 'a' to be training data,
 * and probability of 'b' to be testing data, and probability of (1-a-b) to be ignored.
 * 
 * @author Li Huang
 * edit 2014.4.14
 * edit 2014.4.29 always return false in process(), to avoid infinitely recreating 
 *   the processor.
 */
public final class HLSplitDataPreProcessor implements HLDataPreProcessor, Configurable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8764233637155672362L;
	
	private static final Logger logger = LoggerFactory.getLogger(HLSplitDataPreProcessor.class);
    private int id; 
    private Stream outputStream;
    private java.util.Random rand=new java.util.Random();
    
    public FloatOption trainRatioOption = new FloatOption(
            "trainRatio",
            'a',
            "percentage of training data",
            0.2, 0, 1);
    
    public FloatOption testRatioOption = new FloatOption(
            "testRatio",
            'b',
            "percentage of testing data",
            0.8, 0, 1);
    
    public MultiChoiceOption orderOption = new MultiChoiceOption(
    		"order", 
    		'o',
    		"order of splited data: 0-alternate  1-trainFirst", 
    		new String[]{"alternately","trainFirstThenTest"},
    		new String[]{"randomly select and send every instance as train or test instance",
    				"send first a% data as train data, then send the later b% data as test data"}, 
    		0);
    
    public IntOption numInstancesOption = new IntOption(
    		"numInstancesForTrainFirst", 
    		'i',
    		"max number of instances to sent. Only used for TrainFirst order mode", 
    		100,
    		0,Integer.MAX_VALUE
    		);
    
    private double trainRatio=0;
    private double testRatio=0;
    
    
    // ---------- add by hl 2014.4.19 , for trainFirstOrder mode -------
    public enum OrderMode{
    	alternate,
    	trainFirst
    }
    
    private OrderMode order= OrderMode.alternate;
    
    private int nInstanceSent=0;
    private int nMaxInstances=0; // max number of data to process, only used for TrainFirst order mode
    private int nMaxTrainInstance=0; //max number of trainInstace to sent
    private int nMaxTestInstance=0; //max number of trainInstace to sent
    
    private int parallelism=1; //number of HLSplitData processors
    // ------------------------------------------------------------------
    
    @Override
    public Stream getOutputStream()
    {
    	return outputStream;
    }
    
    @Override
    public void setOutputStream(Stream stream)
    {
    	outputStream=stream;
    }
    
    @Override
    public void init(int parallelism)
    {
    	this.parallelism = parallelism;
    	this.trainRatio=this.trainRatioOption.getValue();
    	this.testRatio=this.testRatioOption.getValue();
    	switch( this.orderOption.getChosenIndex() )
    	{
    	case 0:
    		this.order= OrderMode.alternate;
    		break;
    	case 1:
    		this.order=OrderMode.trainFirst;
    		break;
    	}
    	this.nMaxInstances = this.numInstancesOption.getValue();
    	
    	logger.info("init(): trainRatio={},testRatio={}",this.getTrainRatio(),this.getTestRatio());
    }
    
    @Override
    public boolean process(ContentEvent event) {
        // Possible refactor of the super-interface implementation
        // of source processor does not need this method
    	if (event instanceof InstanceContentEvent)
    	{
            //logger.info("process(): trainRatio={},testRatio={}",this.getTrainRatio(),this.getTestRatio());
  	    	InstanceContentEvent instEvent = (InstanceContentEvent)event;
    		
  	    	switch (this.order)
  	    	{
  	    	case alternate:
  	    		processAlternateOrder(instEvent);
  	    		break;
  	    	case trainFirst:
  	    		processTrainFirstOrder(instEvent);
  	    		break;
  	    	}
  	    	
    		// otherwise ignore this instance
  	    	// del by hl 2014.4.29 should always return false to avoid infinitely recreating the processors
            //return true; 
  	    	return false;
  	    	// }}  del by hl 2014.4.29
    	}
    	
    	return false;
    }

    //mode 1:  alternately train or test
    private void processAlternateOrder(InstanceContentEvent instEvent)
    {
    	//a probability
		double p = rand.nextDouble();
		if (p<trainRatio)
		{
			InstanceContentEvent newEvent = new InstanceContentEvent(
					instEvent.getInstanceIndex(),instEvent.getInstance(),true,false);
			//logger.info("send train event");
			
			//send last event
    		if (instEvent.isLastEvent())
    		{
    			newEvent.setLast(true);
    			logger.info("send last event");
    		}
			this.outputStream.put(newEvent);
		}
		else if ( p<(trainRatio+testRatio))
		{
			InstanceContentEvent newEvent = new InstanceContentEvent(
					instEvent.getInstanceIndex(),instEvent.getInstance(),false,true);
			//logger.info("send test event");
			
			//send last event
    		if (instEvent.isLastEvent())
    		{
    			newEvent.setLast(true);
    			logger.info("send last event");
    		}
			this.outputStream.put(newEvent);
		}
    }
   
    //mode 2: train first, then test
    private void processTrainFirstOrder(InstanceContentEvent instEvent)
    {
    	this.nInstanceSent++;
		if (this.nInstanceSent<=this.nMaxTrainInstance)
		{
			InstanceContentEvent newEvent = new InstanceContentEvent(
					instEvent.getInstanceIndex(),instEvent.getInstance(),true,false);
			//logger.info("send train event");
			
			//send last event
    		if (instEvent.isLastEvent())
    		{
    			newEvent.setLast(true);
    			logger.info("send last event");
    		}
			this.outputStream.put(newEvent);
		}
		else if ( this.nInstanceSent<=this.nMaxTrainInstance+this.nMaxTestInstance)
		{
			InstanceContentEvent newEvent = new InstanceContentEvent(
					instEvent.getInstanceIndex(),instEvent.getInstance(),false,true);
			//logger.info("send test event");
			
			//send last event
    		if (instEvent.isLastEvent())
    		{
    			newEvent.setLast(true);
    			logger.info("send last event");
    		}
			this.outputStream.put(newEvent);
		}
    }

    @Override
    public void onCreate(int id) {
        this.id = id;

    	this.nInstanceSent = 0;
    	this.nMaxTrainInstance = (int)(this.nMaxInstances*this.trainRatio/this.parallelism);
    	this.nMaxTestInstance =  (int)(this.nMaxInstances*this.testRatio/this.parallelism);
    	
        logger.debug("Creating HLSplitDataPreProcessor with id {}", this.id);
        logger.info("onCreate:id={},trainRatio={},testRatio={}",this.id,this.getTrainRatio(),this.getTestRatio());
    }

    @Override
    public Processor newProcessor(Processor p) {
    	HLSplitDataPreProcessor newProcessor = new HLSplitDataPreProcessor();
    	HLSplitDataPreProcessor originProcessor = (HLSplitDataPreProcessor) p;
    	
    	newProcessor.trainRatio = originProcessor.trainRatio;
    	newProcessor.testRatio = originProcessor.testRatio;
    	newProcessor.order = originProcessor.order;
    	
    	newProcessor.parallelism = originProcessor.parallelism;
    	newProcessor.nMaxInstances = originProcessor.nMaxInstances;
    	newProcessor.nMaxTrainInstance = originProcessor.nMaxTrainInstance;
    	newProcessor.nMaxTestInstance = originProcessor.nMaxTestInstance;
    	
    	newProcessor.outputStream=originProcessor.outputStream;
    	
    	logger.info("newProcessor(), id={},trainRatio={},testRatio={}",newProcessor.id,newProcessor.getTrainRatio(),newProcessor.getTestRatio());
    	
        return newProcessor;
    }
    
	public double getTrainRatio() {
		return this.trainRatio;
	}

	public double getTestRatio() {
		return this.testRatio;
	}
	
	public OrderMode getOrderIndex() {
		return this.order;
	}
	
	public int getParallelism() {
		return this.parallelism;
	}
	
}
