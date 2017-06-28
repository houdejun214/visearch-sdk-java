package com.visenze.visearch.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.visenze.visearch.*;
import com.visenze.visearch.internal.http.ViSearchHttpClient;

import java.io.IOException;
import java.util.*;

class BaseViSearchOperations {

    public static final String RESULT = "result";
    public static final String OBJECTS = "objects";
    public static final String METHOD = "method";
    public static final String PAGE = "page";
    public static final String LIMIT = "limit";
    public static final String TOTAL = "total";
    public static final String GROUP_LIMIT = "group_limit";
    public static final String GROUP_RESULTS = "group_results";

    final ViSearchHttpClient viSearchHttpClient;
    final ObjectMapper objectMapper;

    BaseViSearchOperations(ViSearchHttpClient viSearchHttpClient, ObjectMapper objectMapper) {
        this.viSearchHttpClient = viSearchHttpClient;
        this.objectMapper = objectMapper;
    }

    PagedSearchResult pagify(String rawResponse, String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            List<ImageResult> result = new ArrayList<ImageResult>();
            List<ObjectSearchResult> objects = null;
            List<GroupSearchResult> groupResults = null;

            if(node.has(RESULT))
                result = deserializeListResult(rawResponse, node.get(RESULT), ImageResult.class);
            else if (node.has(OBJECTS))
                objects = deserializeListResult(rawResponse, node.get(OBJECTS), ObjectSearchResult.class);
            else if (node.has(GROUP_RESULTS)) {
                JsonNode groupResultsNode = node.get(GROUP_RESULTS) ;
                if (groupResultsNode instanceof ArrayNode){
                    ArrayNode arrayNode = (ArrayNode) groupResultsNode;
                    groupResults = new ArrayList<GroupSearchResult>(arrayNode.size());
                    for (int i = 0, len = arrayNode.size() ; i < len ; i++) {
                        JsonNode groupNode = arrayNode.get(i);

                        GroupSearchResult groupSearchResult = new GroupSearchResult();

                        // extract group value
                        Iterator<String> it = groupNode.fieldNames();
                        while (it.hasNext())
                        {
                            String key = it.next();
                            if (key.equals(RESULT))
                            {
                                groupSearchResult.setResult(deserializeListResult(rawResponse, groupNode.get(RESULT), ImageResult.class));
                            }
                            else {
                                groupSearchResult.setGroupValue(groupNode.get(key).textValue());
                            }
                        }


                        groupResults.add(groupSearchResult);

                    }
                }

            }


            JsonNode methodNode = node.get(METHOD);
            if (methodNode == null) {
                throw new InternalViSearchException(ResponseMessages.INVALID_RESPONSE_FORMAT, rawResponse);
            }
            JsonNode pageNode = node.get(PAGE);
            JsonNode limitNode = node.get(LIMIT);
            JsonNode totalNode = node.get(TOTAL);
            JsonNode groupLimitNode = node.get(GROUP_LIMIT) ;

            PagedSearchResult pagedResult = new PagedSearchResult(result);
            if(pageNode!=null) pagedResult.setPage(pageNode.asInt());
            if(limitNode!=null) pagedResult.setLimit(limitNode.asInt());
            if(totalNode!=null) pagedResult.setTotal(totalNode.asInt());
            if(groupLimitNode!=null) pagedResult.setGroupLimit(groupLimitNode.asInt());

            pagedResult.setObjects(objects);


            return pagedResult;
        } catch (IOException e) {
            throw new InternalViSearchException(ResponseMessages.PARSE_RESPONSE_ERROR, e, rawResponse);
        }
    }

    <T> T deserializeObjectResult(String rawResponse, String json, Class<T> clazz) {
        try {
            return objectMapper.reader(clazz).readValue(json);
        } catch (IOException e) {
            throw new InternalViSearchException(ResponseMessages.PARSE_RESPONSE_ERROR, e, rawResponse);
            // throw new ViSearchException("Could not parse the ViSearch response for " +
            //        clazz.getSimpleName() + ": " + json, e, json);
        }
    }

    @SuppressWarnings("unchecked")
    <T> List<T> deserializeListResult(String rawResponse, JsonNode node, Class<T> clazz) {
        String json = node.toString();
        try {
            CollectionType listType = TypeFactory.defaultInstance().constructCollectionType(List.class, clazz);
            return (List<T>) objectMapper.readerFor(listType).readValue(json);
        } catch (IOException e) {
            throw new InternalViSearchException(ResponseMessages.PARSE_RESPONSE_ERROR, e, rawResponse);
        }
    }

    @SuppressWarnings("unchecked")
    <T, U> Map<T, U> deserializeMapResult(String rawResponse, JsonNode node, Class<T> keyClass, Class<T> valueClass) {
        String json = node.toString();
        try {
            MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, keyClass, valueClass);
            return (Map<T, U>) objectMapper.readerFor(mapType).readValue(node);
        } catch (IOException e) {
            throw new InternalViSearchException(ResponseMessages.PARSE_RESPONSE_ERROR, e, rawResponse);
        }
    }
}
