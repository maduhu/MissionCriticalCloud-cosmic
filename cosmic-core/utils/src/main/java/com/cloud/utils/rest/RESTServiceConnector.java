//

//

package com.cloud.utils.rest;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstraction encapsulates client side code for REST service communication. It encapsulates access in a REST client. There can be different implementations of that client
 * implementing {@link RestClient}, and any of them should mention the needed data to work.
 * <p>
 * This connector allows the use of {@link JsonDeserializer} for specific classes. You can provide in the constructor a map of classes to deserializers for these classes.
 */
public class RESTServiceConnector {

    private static final Logger s_logger = LoggerFactory.getLogger(RESTServiceConnector.class);

    private static final Optional<String> ABSENT = Optional.absent();

    private final RestClient client;
    private final Gson gson;

    private RESTServiceConnector(final Builder builder) {
        client = builder.client;
        gson = setGsonDeserializer(builder.classToDeserializerMap);
    }

    private static Gson setGsonDeserializer(final Map<Class<?>, JsonDeserializer<?>> classToDeserializerMap) {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        for (final Map.Entry<Class<?>, JsonDeserializer<?>> entry : classToDeserializerMap.entrySet()) {
            gsonBuilder.registerTypeAdapter(entry.getKey(), entry.getValue());
        }
        return gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    public static Builder create() {
        return new Builder();
    }

    public <T> void executeUpdateObject(final T newObject, final String path) throws CloudstackRESTException {
        executeUpdateObject(newObject, path, new HashMap<>());
    }

    public <T> void executeUpdateObject(final T newObject, final String path, final Map<String, String> parameters) throws CloudstackRESTException {
        s_logger.debug("Executing update object on " + path);
        client.closeResponse(createAndExecuteRequest(HttpMethods.PUT, path, parameters, Optional.fromNullable(gson.toJson(newObject))));
    }

    private CloseableHttpResponse createAndExecuteRequest(final HttpMethods method, final String path, final Map<String, String> parameters, final Optional<String> jsonPayLoad)
            throws CloudstackRESTException {
        final HttpUriRequest httpRequest = HttpUriRequestBuilder.create()
                                                                .path(path)
                                                                .parameters(parameters)
                                                                .jsonPayload(jsonPayLoad)
                                                                .method(method)
                                                                .build();
        if (jsonPayLoad.isPresent()) {
            s_logger.debug("Built request '" + httpRequest + "' with payload: " + jsonPayLoad);
        }
        return executeRequest(httpRequest);
    }

    private CloseableHttpResponse executeRequest(final HttpUriRequest httpRequest) throws CloudstackRESTException {
        final CloseableHttpResponse response = client.execute(httpRequest);
        s_logger.debug("Executed request: " + httpRequest + " status was " + response.getStatusLine().toString());
        return response;
    }

    public <T> T executeCreateObject(final T newObject, final String uri) throws CloudstackRESTException {
        return executeCreateObject(newObject, uri, new HashMap<>());
    }

    public <T> T executeCreateObject(final T newObject, final String path, final Map<String, String> parameters) throws CloudstackRESTException {
        s_logger.debug("Executing create object on " + path);
        final CloseableHttpResponse response = createAndExecuteRequest(HttpMethods.POST, path, parameters, Optional.fromNullable(gson.toJson(newObject)));
        return (T) readResponseBody(response, newObject.getClass());
    }

    private <T> T readResponseBody(final CloseableHttpResponse response, final Type type) throws CloudstackRESTException {
        final HttpEntity entity = response.getEntity();
        try {
            final String stringEntity = EntityUtils.toString(entity);
            s_logger.trace("Response entity: " + stringEntity);
            EntityUtils.consumeQuietly(entity);
            return gson.fromJson(stringEntity, type);
        } catch (final IOException e) {
            throw new CloudstackRESTException("Could not deserialize response to JSON. Entity: " + entity, e);
        } finally {
            client.closeResponse(response);
        }
    }

    public void executeDeleteObject(final String path) throws CloudstackRESTException {
        s_logger.debug("Executing delete object on " + path);
        client.closeResponse(createAndExecuteRequest(HttpMethods.DELETE, path, new HashMap<>(), ABSENT));
    }

    public <T> T executeRetrieveObject(final Type returnObjectType, final String path) throws CloudstackRESTException {
        return executeRetrieveObject(returnObjectType, path, new HashMap<>());
    }

    public <T> T executeRetrieveObject(final Type returnObjectType, final String path, final Map<String, String> parameters) throws CloudstackRESTException {
        s_logger.debug("Executing retrieve object on " + path);
        final CloseableHttpResponse response = createAndExecuteRequest(HttpMethods.GET, path, parameters, ABSENT);
        return readResponseBody(response, returnObjectType);
    }

    public static class Builder {
        final private Map<Class<?>, JsonDeserializer<?>> classToDeserializerMap = new HashMap<>();
        private RestClient client;

        public Builder client(final RestClient client) {
            this.client = client;
            return this;
        }

        public Builder classToDeserializerMap(final Map<Class<?>, JsonDeserializer<?>> classToDeserializerMap) {
            this.classToDeserializerMap.clear();
            this.classToDeserializerMap.putAll(classToDeserializerMap);
            return this;
        }

        public Builder classToDeserializerEntry(final Class<?> clazz, final JsonDeserializer<?> deserializer) {
            classToDeserializerMap.put(clazz, deserializer);
            return this;
        }

        public RESTServiceConnector build() {
            return new RESTServiceConnector(this);
        }
    }
}
