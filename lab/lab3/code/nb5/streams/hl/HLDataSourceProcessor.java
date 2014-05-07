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

import com.yahoo.labs.samoa.core.ContentEvent;
import com.yahoo.labs.samoa.core.EntranceProcessor;
import com.yahoo.labs.samoa.core.Processor;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.learners.InstanceContentEvent;
import com.yahoo.labs.samoa.moa.options.AbstractOptionHandler;
import com.yahoo.labs.samoa.moa.streams.InstanceStream;
import com.yahoo.labs.samoa.topology.Stream;

import com.yahoo.labs.samoa.streams.*;
/**
 * HLDataSource Processor is the processor for HLTrainTest Evaluation Task.
 * 
 * finished
 * 
 * @author Arinto Murdopo, edit by Li Huang
 * 
 */
public final class HLDataSourceProcessor implements EntranceProcessor {

    /**
	 * 
	 */
	private static final long serialVersionUID = -4382512327730562087L;
	private static final Logger logger = LoggerFactory.getLogger(HLDataSourceProcessor.class);
    private int id;
    private boolean isInited = false;
    private StreamSource streamSource;
    private Instance firstInstance;
    private int numberInstances;
    private int numInstanceSent = 0;

    protected InstanceStream sourceStream;
    
    @Override
    public boolean process(ContentEvent event) {
        // TODO: possible refactor of the super-interface implementation
        // of source processor does not need this method
        return false;
    }

    @Override
    public boolean hasNext() {
    	return streamSource.hasMoreInstances() && (numInstanceSent < numberInstances);
    }

    @Override
    public ContentEvent nextEvent() {
        InstanceContentEvent contentEvent = null;
        
        //modify by hl 2014.4.16. Make sure the last event is sent.
        if ( hasNext() )
        {
            numInstanceSent++;
            //logger.info("read {} inst",numInstanceSent); // del later. debug |!|
            contentEvent = new InstanceContentEvent(numInstanceSent, nextInstance(), true, true);
            
            if (!hasNext())
            {
            	contentEvent.setLast(true);
            }
        }
        
        return contentEvent;
    }

    @Override
    public void onCreate(int id) {
        this.id = id;
        initStreamSource(sourceStream);
        logger.debug("Creating HLDataSourceProcessor with id {}", this.id);
    }

    @Override
    public Processor newProcessor(Processor p) {
    	HLDataSourceProcessor newProcessor = new HLDataSourceProcessor();
    	HLDataSourceProcessor originProcessor = (HLDataSourceProcessor) p;
        if (originProcessor.getStreamSource() != null) {
            newProcessor.setStreamSource(originProcessor.getStreamSource().getStream());
        }
        return newProcessor;
    }

    /**
     * Method to send instances via input stream
     * 
     * @param inputStream
     * @param numberInstances
     */
    public void sendInstances(Stream inputStream, int numberInstances) {
        int numInstanceSent = 0;
        initStreamSource(sourceStream);
        
        while (streamSource.hasMoreInstances() && numInstanceSent < numberInstances) {
            numInstanceSent++;
            InstanceContentEvent contentEvent = new InstanceContentEvent(numInstanceSent, nextInstance(), true, true);
            inputStream.put(contentEvent);
        }

        sendEndEvaluationInstance(inputStream);
    }

    public StreamSource getStreamSource() {
        return streamSource;
    }

    public void setStreamSource(InstanceStream stream) {
        this.sourceStream = stream;
    }

    public Instances getDataset() {
        if (firstInstance == null) {
            initStreamSource(sourceStream);
        }
        return firstInstance.dataset();
    }

    private Instance nextInstance() {
        if (this.isInited) {
            return streamSource.nextInstance().getData();
        } else {
            this.isInited = true;
            return firstInstance;
        }
    }

    private void sendEndEvaluationInstance(Stream inputStream) {
        InstanceContentEvent contentEvent = new InstanceContentEvent(-1, firstInstance, false, true);
        contentEvent.setLast(true);
        inputStream.put(contentEvent);
    }

    private void initStreamSource(InstanceStream stream) {
        if (stream instanceof AbstractOptionHandler) {
            ((AbstractOptionHandler) (stream)).prepareForUse();
        }

        this.streamSource = new StreamSource(stream);
        firstInstance = streamSource.nextInstance().getData();
    }

    public void setMaxNumInstances(int value) {
        numberInstances = value;
    }
}
