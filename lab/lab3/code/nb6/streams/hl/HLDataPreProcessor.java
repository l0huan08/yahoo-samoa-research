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


import com.yahoo.labs.samoa.core.Processor;
import com.yahoo.labs.samoa.topology.Stream;

/**
 * DataPreProcessor is the processor for TrainTest Evaluation Task.
 * Do some filter work, such as split the source data into a% training data
 * and b% testing data
 * 
 * finished
 * 
 * Topology
 * inputStream -shuffle-> dataPreProcessor -shuffle->outputStream
 * 
 * @author Arinto Murdopo, changed by Li Huang
 */
public interface HLDataPreProcessor extends Processor {
    public Stream getOutputStream();
    public void setOutputStream(Stream stream);
    /**
     * Must init here, for parse the command line args.
     * Cannot init at the onCreate() or newProcessor()
     */
    public void init(int parallelism);
}
