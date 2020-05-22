/*
 * Copyright (c) 2020. MobilityData IO.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mobilitydata.gtfsvalidator.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.logging.log4j.Logger;
import org.mobilitydata.gtfsvalidator.domain.entity.ExecParam;
import org.mobilitydata.gtfsvalidator.usecase.port.ExecParamRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * This provides context to go from execution parameters contained in an .json file to an internal representation using
 * {@code ExecParam}.
 */
public class JsonExecParamParser implements ExecParamRepository.ExecParamParser {
    private final String parameterJsonString;
    private final ObjectReader objectReader;
    private final Logger logger;

    /**
     * @param parameterJsonString the .json containing the execution parameters to parse, as string
     * @param objectReader        reader from jackson library to parse the configuration .json file
     * @param logger              logger to log relevant information
     */
    public JsonExecParamParser(final String parameterJsonString,
                               final ObjectReader objectReader,
                               final Logger logger) {
        this.parameterJsonString = parameterJsonString;
        this.objectReader = objectReader;
        this.logger = logger;
    }

    /**
     * This method allows parsing execution parameters found in a .json file to an internal representation using
     * {@code ExecParam}. Returns a collection of the extracted {@link ExecParam} mapped on their keys. They key of each
     * {@link ExecParam} is associated with the field paramKey of each object to be parsed from the .json file.
     *
     * @return a collection of {@link ExecParam} mapped on the name associated to the execution parameter they
     * represent
     */
    @Override
    public Map<String, ExecParam> parse() {
        final Map<String, ExecParam> toReturn = new HashMap<>();
        try {
            objectReader.readTree(parameterJsonString).fields()
                    .forEachRemaining(field -> {
                        if (!checkExistingOptions(toReturn, field)) {
                            toReturn.put(field.getKey(), new ExecParam(field.getKey(), field.getValue().asText()));
                        }
                    });
            return toReturn;
        } catch (JsonProcessingException e) {
            logger.info("could not find execution-parameters.json file  -- will consider default values for" +
                    " execution parameters");
            return toReturn;
        }
    }

    /**
     * Method verifies for all options (except --exclusion):
     * - if option have been declared once
     * Throws a ParseException if these requirement are not met
     *
     * @param execParamCollection option collection
     * @param option              option as {@code JsonNode} to analyze
     * @throws RuntimeException if option (except --exclude) has been declared more than once
     */
    private boolean checkExistingOptions(final Map<String, ExecParam> execParamCollection,
                                         final Map.Entry<String, JsonNode> option) throws RuntimeException {
        if (execParamCollection.containsKey(option.getKey())) {
            if (option.getKey().equals(ExecParamRepository.EXCLUSION_KEY)) {
                return true;
            } else {
                throw new RuntimeException("Option: " + option.getKey() + "already defined");
            }
        } else {
            return false;
        }
    }
}