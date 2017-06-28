package com.visenze.visearch.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.visenze.visearch.*;
import com.visenze.visearch.internal.http.ViSearchHttpClient;
import com.visenze.visearch.internal.http.ViSearchHttpResponse;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SearchOperationsImpl extends BaseViSearchOperations implements SearchOperations {

    private static final String ENDPOINT_DISCOVER_SEARCH = "/discoversearch";
    private static final String ENDPOINT_UPLOAD_SEARCH = "/uploadsearch";
    private static final String ENDPOINT_SEARCH = "/search";
    private static final String ENDPOINT_RECOMMENDATION = "/recommendation";
    private static final String ENDPOINT_COLOR_SEARCH = "/colorsearch";
    private static final String ENDPOINT_SIMILAR_PRODUCTS_SEARCH = "/similarproducts";
    private static final String DETECTION_ALL = "all";
    public static final String PRODUCT_TYPES = "product_types";
    public static final String PRODUCT_TYPES_LIST = "product_types_list";
    public static final String IM_ID = "im_id";
    public static final String FACETS = "facets";
    public static final String QINFO = "qinfo";
    public static final String OBJECT_TYPES_LIST = "object_types_list";
    public static final String GROUP_RESULT = "group_result";

    public SearchOperationsImpl(ViSearchHttpClient viSearchHttpClient, ObjectMapper objectMapper) {
        super(viSearchHttpClient, objectMapper);
    }

    @Override
    public PagedSearchResult search(SearchParams searchParams) {
        try {
            ViSearchHttpResponse response = viSearchHttpClient.get(ENDPOINT_SEARCH, searchParams.toMap());
            return getPagedResult(response);
        } catch (InternalViSearchException e) {
            return new PagedSearchResult(e.getMessage(), e.getCause(), e.getServerRawResponse());
        }
    }

    @Override
    public PagedSearchResult recommendation(SearchParams searchParams) {
        try {
            ViSearchHttpResponse response = viSearchHttpClient.get(ENDPOINT_RECOMMENDATION, searchParams.toMap());
            return getPagedResult(response);
        } catch (InternalViSearchException e) {
            return new PagedSearchResult(e.getMessage(), e.getCause(), e.getServerRawResponse());
        }
    }

    @Override
    public PagedSearchResult colorSearch(ColorSearchParams colorSearchParams) {
        try {
            ViSearchHttpResponse response = viSearchHttpClient.get(ENDPOINT_COLOR_SEARCH, colorSearchParams.toMap());
            return getPagedResult(response);
        } catch (InternalViSearchException e) {
            return new PagedSearchResult(e.getMessage(), e.getCause(), e.getServerRawResponse());
        }
    }

    @Override
    public PagedSearchResult uploadSearch(UploadSearchParams uploadSearchParams) {
        try {
            return uploadSearchInternal(uploadSearchParams);
        } catch (InternalViSearchException e) {
            return new PagedSearchResult(e.getMessage(), e.getCause(), e.getServerRawResponse());
        }
    }

    private PagedSearchResult uploadSearchInternal(UploadSearchParams uploadSearchParams) {
        File imageFile = uploadSearchParams.getImageFile();
        InputStream imageStream = uploadSearchParams.getImageStream();
        String imageUrl = uploadSearchParams.getImageUrl();
        ViSearchHttpResponse response;

        // if im_id is available no need to check for image
        if (!Strings.isNullOrEmpty(uploadSearchParams.getImId())){
            response = viSearchHttpClient.post(ENDPOINT_UPLOAD_SEARCH, uploadSearchParams.toMap());
        }
        else if (imageFile == null && imageStream == null && (Strings.isNullOrEmpty(imageUrl))) {
            throw new InternalViSearchException(ResponseMessages.INVALID_IMAGE_SOURCE);
            // throw new IllegalArgumentException("Must provide either an image File, InputStream of the image, or a valid image url to perform upload search");
        } else if (imageFile != null) {
            try {
                response = viSearchHttpClient.postImage(ENDPOINT_UPLOAD_SEARCH, uploadSearchParams.toMap(), new FileInputStream(imageFile), imageFile.getName());
            } catch (FileNotFoundException e) {
                throw new InternalViSearchException(ResponseMessages.INVALID_IMAGE_OR_URL, e);
                // throw new IllegalArgumentException("Could not open the image file.", e);
            }
        } else if (imageStream != null) {
            response = viSearchHttpClient.postImage(ENDPOINT_UPLOAD_SEARCH, uploadSearchParams.toMap(), imageStream, "image-stream");
        } else {
            response = viSearchHttpClient.post(ENDPOINT_UPLOAD_SEARCH, uploadSearchParams.toMap());
        }
        return getPagedResult(response);
    }

    /**
     * Perform real disover search
     * @param uploadSearchParams
     * @return
     */
    @Override
    public PagedSearchResult discoverSearch(UploadSearchParams uploadSearchParams) {
        try {
            return postImageSearch(uploadSearchParams, ENDPOINT_DISCOVER_SEARCH);
        } catch (InternalViSearchException e) {
            return new PagedSearchResult(e.getMessage(), e.getCause(), e.getServerRawResponse());
        }
    }

    /**
     * Perform real disover search
     * @param uploadSearchParams
     * @return
     */
    @Override
    public PagedSearchResult similarProductsSearch(UploadSearchParams uploadSearchParams) {
        try {
            return postImageSearch(uploadSearchParams, ENDPOINT_SIMILAR_PRODUCTS_SEARCH);
        } catch (InternalViSearchException e) {
            return new PagedSearchResult(e.getMessage(), e.getCause(), e.getServerRawResponse());
        }
    }

    /**
     * Perform real disover search
     * @param uploadSearchParams
     * @return
     */
    private PagedSearchResult postImageSearch(UploadSearchParams uploadSearchParams, String endpointMethod) {
        File imageFile = uploadSearchParams.getImageFile();
        InputStream imageStream = uploadSearchParams.getImageStream();
        String imageUrl = uploadSearchParams.getImageUrl();
        ViSearchHttpResponse response;

        // if im_id is available no need to check for image
        if (!Strings.isNullOrEmpty(uploadSearchParams.getImId())){
            response = viSearchHttpClient.post(endpointMethod, uploadSearchParams.toMap());
        }
        else if (imageFile == null && imageStream == null && (Strings.isNullOrEmpty(imageUrl))) {
            throw new InternalViSearchException(ResponseMessages.INVALID_IMAGE_SOURCE);
            // throw new IllegalArgumentException("Must provide either an image File, InputStream of the image, or a valid image url to perform upload search");
        } else if (imageFile != null) {
            try {
                response = viSearchHttpClient.postImage(endpointMethod, uploadSearchParams.toMap(), new FileInputStream(imageFile), imageFile.getName());
            } catch (FileNotFoundException e) {
                throw new InternalViSearchException(ResponseMessages.INVALID_IMAGE_OR_URL, e);
                // throw new IllegalArgumentException("Could not open the image file.", e);
            }
        } else if (imageStream != null) {
            response = viSearchHttpClient.postImage(endpointMethod, uploadSearchParams.toMap(), imageStream, "image-stream");
        } else {
            response = viSearchHttpClient.post(endpointMethod, uploadSearchParams.toMap());
        }
        return getPagedResult(response);
    }

    private PagedSearchResult getPagedResult(ViSearchHttpResponse httpResponse) {
        String response = httpResponse.getBody();
        Map<String, String> headers = httpResponse.getHeaders();
        JsonNode node;
        try {
            node = objectMapper.readTree(response);
        } catch (JsonProcessingException e) {
            throw new InternalViSearchException(ResponseMessages.PARSE_RESPONSE_ERROR, e, response);
            // throw new ViSearchException("Could not parse the ViSearch response: " + response, e, response);
        } catch (IOException e) {
            throw new InternalViSearchException(ResponseMessages.PARSE_RESPONSE_ERROR, e, response);
            // throw new ViSearchException("Could not parse the ViSearch response: " + response, e, response);
        }
        checkResponseStatus(node);

        PagedSearchResult result = pagify(response, response);

        JsonNode productTypesNode = node.get(PRODUCT_TYPES);
        if (productTypesNode != null) {
            List<ProductType> productTypes = deserializeListResult(response, productTypesNode, ProductType.class);
            result.setProductTypes(productTypes);
        }
        JsonNode productTypesListNode = node.get(PRODUCT_TYPES_LIST);
        if (productTypesListNode != null) {
            List<ProductType> productTypesList = deserializeListResult(response, productTypesListNode, ProductType.class);
            result.setProductTypesList(productTypesList);
        }
        JsonNode objectTypesListNode = node.get(OBJECT_TYPES_LIST);
        if (objectTypesListNode != null) {
            List<ProductType> objectTypesList = deserializeListResult(response, objectTypesListNode, ProductType.class);
            result.setObjectTypesList(objectTypesList);
        }
        JsonNode imIdNode = node.get(IM_ID);
        if (imIdNode != null) {
            result.setImId(imIdNode.asText());
        }
        JsonNode facetsNode = node.get(FACETS);
        if (facetsNode != null) {
            List<Facet> facets = deserializeListResult(response, facetsNode, Facet.class);
            result.setFacets(facets);
        }
        JsonNode qinfoNode = node.get(QINFO);
        if (qinfoNode != null) {
            Map<String, String> qinfo = deserializeMapResult(response, qinfoNode, String.class, String.class);
            result.setQueryInfo(qinfo);
        }
        // For similarproducts search, try to cover it's result into discoversearch result.
        JsonNode groupResult = node.get(GROUP_RESULT);
        if (groupResult != null && groupResult instanceof ArrayNode) {
            List<ProductType> productTypes = result.getProductTypes();
            List<ObjectSearchResult> objects = Lists.newArrayList();
            ArrayNode arrayNode = (ArrayNode) groupResult;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode oneGroup = arrayNode.get(i);
                ProductType productType = productTypes.get(i);
                ObjectSearchResult objectSearchResult = new ObjectSearchResult();
                objectSearchResult.setResult(deserializeListResult(response, oneGroup, ImageResult.class));
                objectSearchResult.setScore(productType.getScore());
                objectSearchResult.setAttributes(productType.getAttributes());
                objectSearchResult.setAttributesList(productType.getAttributesList());
                objectSearchResult.setBox(productType.getBox());
                objectSearchResult.setType(productType.getType());
                objects.add(objectSearchResult);
            }
            result.setObjects(objects);
            result.setObjectTypesList(result.getProductTypesList());
        }

        // added grouped response for group_by field


        result.setRawJson(node.toString());
        result.setHeaders(headers);
        return result;
    }

    private static void checkResponseStatus(JsonNode node) {
        String json = node.toString();
        JsonNode statusNode = node.get("status");
        if (statusNode == null) {
            throw new InternalViSearchException(ResponseMessages.INVALID_RESPONSE_FORMAT, json);
            // throw new ViSearchException("There was a malformed ViSearch response: " + json, json);
        } else {
            String status = statusNode.asText();
            if (!"OK".equals(status)) {
                JsonNode errorNode = node.get("error");
                if (errorNode == null) {
                    throw new InternalViSearchException(ResponseMessages.INVALID_RESPONSE_FORMAT, json);
                    // throw new ViSearchException("An unknown error occurred in ViSearch: " + json, json);
                }
                String message = errorNode.path(0).asText();
                throw new InternalViSearchException(message, json);
                // throw new ViSearchException("An error occurred calling ViSearch: " + message, json);
            }
        }
    }
}
