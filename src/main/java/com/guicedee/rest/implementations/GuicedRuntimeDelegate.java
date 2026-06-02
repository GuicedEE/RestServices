package com.guicedee.rest.implementations;

import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Variant.VariantListBuilder;
import jakarta.ws.rs.ext.RuntimeDelegate;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletionStage;

/**
 * Lightweight {@link RuntimeDelegate} for GuicedEE's Vert.x REST pipeline.
 * <p>
 * Supports {@link Response} building (status, entity, headers, media type) without
 * requiring a full JAX-RS implementation. Methods not needed by the Vert.x pipeline
 * throw {@link UnsupportedOperationException}.
 */
public class GuicedRuntimeDelegate extends RuntimeDelegate {

    @Override
    public ResponseBuilder createResponseBuilder() {
        return new GuicedResponseBuilder();
    }

    @Override
    public UriBuilder createUriBuilder() {
        throw new UnsupportedOperationException("UriBuilder is not supported in GuicedEE REST — use Vert.x routing directly.");
    }

    @Override
    public VariantListBuilder createVariantListBuilder() {
        throw new UnsupportedOperationException("VariantListBuilder is not supported in GuicedEE REST.");
    }

    @Override
    public <T> T createEndpoint(Application application, Class<T> endpointType) {
        throw new UnsupportedOperationException("createEndpoint is not supported in GuicedEE REST — use Vert.x routing directly.");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) {
        if (type == MediaType.class) {
            return (HeaderDelegate<T>) new MediaTypeHeaderDelegate();
        }
        if (type == Date.class) {
            return (HeaderDelegate<T>) new DateHeaderDelegate();
        }
        // Fallback: toString/fromString pass-through
        return new HeaderDelegate<>() {
            @Override
            public T fromString(String value) {
                throw new UnsupportedOperationException("Header parsing not supported for " + type.getName());
            }

            @Override
            public String toString(T value) {
                return value == null ? "" : value.toString();
            }
        };
    }

    @Override
    public Link.Builder createLinkBuilder() {
        throw new UnsupportedOperationException("Link.Builder is not supported in GuicedEE REST.");
    }

    @Override
    public SeBootstrap.Configuration.Builder createConfigurationBuilder() {
        throw new UnsupportedOperationException("SeBootstrap is not supported in GuicedEE REST — use Vert.x web server.");
    }

    @Override
    public CompletionStage<SeBootstrap.Instance> bootstrap(Application application, SeBootstrap.Configuration configuration) {
        throw new UnsupportedOperationException("SeBootstrap is not supported in GuicedEE REST — use Vert.x web server.");
    }

    @Override
    public CompletionStage<SeBootstrap.Instance> bootstrap(Class<? extends Application> clazz, SeBootstrap.Configuration configuration) {
        throw new UnsupportedOperationException("SeBootstrap is not supported in GuicedEE REST — use Vert.x web server.");
    }

    @Override
    public EntityPart.Builder createEntityPartBuilder(String partName) {
        throw new UnsupportedOperationException("EntityPart is not supported in GuicedEE REST.");
    }

    // ─── ResponseBuilder ─────────────────────────────────────────────────────────

    private static class GuicedResponseBuilder extends ResponseBuilder {
        private int status = 200;
        private Object entity;
        private final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        private MediaType mediaType;

        @Override
        public Response build() {
            return new GuicedResponse(status, entity, headers, mediaType);
        }

        @Override
        public ResponseBuilder clone() {
            GuicedResponseBuilder copy = new GuicedResponseBuilder();
            copy.status = this.status;
            copy.entity = this.entity;
            copy.headers.putAll(this.headers);
            copy.mediaType = this.mediaType;
            return copy;
        }

        @Override
        public ResponseBuilder status(int status) {
            this.status = status;
            return this;
        }

        @Override
        public ResponseBuilder status(int status, String reasonPhrase) {
            this.status = status;
            return this;
        }

        @Override
        public ResponseBuilder entity(Object entity) {
            this.entity = entity;
            return this;
        }

        @Override
        public ResponseBuilder entity(Object entity, Annotation[] annotations) {
            this.entity = entity;
            return this;
        }

        @Override
        public ResponseBuilder allow(String... methods) {
            headers.put("Allow", Arrays.stream(methods).map(m -> (Object) m).toList());
            return this;
        }

        @Override
        public ResponseBuilder allow(Set<String> methods) {
            headers.put("Allow", methods.stream().map(m -> (Object) m).toList());
            return this;
        }

        @Override
        public ResponseBuilder cacheControl(CacheControl cacheControl) {
            headers.putSingle("Cache-Control", cacheControl);
            return this;
        }

        @Override
        public ResponseBuilder encoding(String encoding) {
            headers.putSingle("Content-Encoding", encoding);
            return this;
        }

        @Override
        public ResponseBuilder header(String name, Object value) {
            if (value == null) {
                headers.remove(name);
            } else {
                headers.add(name, value);
            }
            return this;
        }

        @Override
        public ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
            this.headers.clear();
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }

        @Override
        public ResponseBuilder language(String language) {
            headers.putSingle("Content-Language", language);
            return this;
        }

        @Override
        public ResponseBuilder language(Locale language) {
            headers.putSingle("Content-Language", language);
            return this;
        }

        @Override
        public ResponseBuilder type(MediaType type) {
            this.mediaType = type;
            if (type != null) {
                headers.putSingle("Content-Type", type);
            }
            return this;
        }

        @Override
        public ResponseBuilder type(String type) {
            if (type == null) {
                this.mediaType = null;
            } else {
                // Parse manually — avoid MediaType.valueOf which recurses through RuntimeDelegate
                this.mediaType = parseMediaType(type);
            }
            if (type != null) {
                headers.putSingle("Content-Type", type);
            }
            return this;
        }

        private static MediaType parseMediaType(String value) {
            if (value == null || value.isEmpty()) return MediaType.WILDCARD_TYPE;
            String[] parts = value.split(";");
            String mediaRange = parts[0].trim();
            String type = "*", subtype = "*";
            int slash = mediaRange.indexOf('/');
            if (slash > 0) {
                type = mediaRange.substring(0, slash).trim();
                subtype = mediaRange.substring(slash + 1).trim();
            }
            Map<String, String> params = new LinkedHashMap<>();
            for (int i = 1; i < parts.length; i++) {
                String param = parts[i].trim();
                int eq = param.indexOf('=');
                if (eq > 0) params.put(param.substring(0, eq).trim(), param.substring(eq + 1).trim());
            }
            return new MediaType(type, subtype, params);
        }

        @Override
        public ResponseBuilder variant(Variant variant) {
            return this;
        }

        @Override
        public ResponseBuilder contentLocation(URI location) {
            headers.putSingle("Content-Location", location);
            return this;
        }

        @Override
        public ResponseBuilder cookie(NewCookie... cookies) {
            if (cookies != null) {
                for (NewCookie cookie : cookies) {
                    headers.add("Set-Cookie", cookie);
                }
            }
            return this;
        }

        @Override
        public ResponseBuilder expires(Date expires) {
            headers.putSingle("Expires", expires);
            return this;
        }

        @Override
        public ResponseBuilder lastModified(Date lastModified) {
            headers.putSingle("Last-Modified", lastModified);
            return this;
        }

        @Override
        public ResponseBuilder location(URI location) {
            headers.putSingle("Location", location);
            return this;
        }

        @Override
        public ResponseBuilder tag(EntityTag tag) {
            headers.putSingle("ETag", tag);
            return this;
        }

        @Override
        public ResponseBuilder tag(String tag) {
            headers.putSingle("ETag", tag);
            return this;
        }

        @Override
        public ResponseBuilder variants(Variant... variants) {
            return this;
        }

        @Override
        public ResponseBuilder variants(List<Variant> variants) {
            return this;
        }

        @Override
        public ResponseBuilder links(Link... links) {
            if (links != null) {
                for (Link link : links) {
                    headers.add("Link", link);
                }
            }
            return this;
        }

        @Override
        public ResponseBuilder link(URI uri, String rel) {
            headers.add("Link", "<" + uri + ">; rel=\"" + rel + "\"");
            return this;
        }

        @Override
        public ResponseBuilder link(String uri, String rel) {
            headers.add("Link", "<" + uri + ">; rel=\"" + rel + "\"");
            return this;
        }
    }

    // ─── Response implementation ─────────────────────────────────────────────────

    private static class GuicedResponse extends Response {
        private final int status;
        private final Object entity;
        private final MultivaluedMap<String, Object> headers;
        private final MediaType mediaType;

        GuicedResponse(int status, Object entity, MultivaluedMap<String, Object> headers, MediaType mediaType) {
            this.status = status;
            this.entity = entity;
            this.headers = new MultivaluedHashMap<>(headers);
            this.mediaType = mediaType;
        }

        @Override
        public int getStatus() {
            return status;
        }

        @Override
        public StatusType getStatusInfo() {
            return Status.fromStatusCode(status);
        }

        @Override
        public Object getEntity() {
            return entity;
        }

        @Override
        public <T> T readEntity(Class<T> entityType) {
            throw new UnsupportedOperationException("readEntity not supported — use getEntity().");
        }

        @Override
        public <T> T readEntity(GenericType<T> entityType) {
            throw new UnsupportedOperationException("readEntity not supported — use getEntity().");
        }

        @Override
        public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
            throw new UnsupportedOperationException("readEntity not supported — use getEntity().");
        }

        @Override
        public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
            throw new UnsupportedOperationException("readEntity not supported — use getEntity().");
        }

        @Override
        public boolean hasEntity() {
            return entity != null;
        }

        @Override
        public boolean bufferEntity() {
            return false;
        }

        @Override
        public void close() {
            // No-op
        }

        @Override
        public MediaType getMediaType() {
            return mediaType;
        }

        @Override
        public Locale getLanguage() {
            Object lang = headers.getFirst("Content-Language");
            return lang instanceof Locale l ? l : null;
        }

        @Override
        public int getLength() {
            return -1;
        }

        @Override
        public Set<String> getAllowedMethods() {
            List<Object> allow = headers.get("Allow");
            if (allow == null) return Set.of();
            Set<String> methods = new HashSet<>();
            for (Object o : allow) methods.add(o.toString());
            return methods;
        }

        @Override
        public Map<String, NewCookie> getCookies() {
            return Map.of();
        }

        @Override
        public EntityTag getEntityTag() {
            return null;
        }

        @Override
        public Date getDate() {
            return null;
        }

        @Override
        public Date getLastModified() {
            Object lm = headers.getFirst("Last-Modified");
            return lm instanceof Date d ? d : null;
        }

        @Override
        public URI getLocation() {
            Object loc = headers.getFirst("Location");
            if (loc instanceof URI u) return u;
            if (loc != null) return URI.create(loc.toString());
            return null;
        }

        @Override
        public Set<Link> getLinks() {
            return Set.of();
        }

        @Override
        public boolean hasLink(String relation) {
            return false;
        }

        @Override
        public Link getLink(String relation) {
            return null;
        }

        @Override
        public Link.Builder getLinkBuilder(String relation) {
            return null;
        }

        @Override
        public MultivaluedMap<String, Object> getMetadata() {
            return headers;
        }

        @Override
        public MultivaluedMap<String, Object> getHeaders() {
            return headers;
        }

        @Override
        public MultivaluedMap<String, String> getStringHeaders() {
            MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
            headers.forEach((k, v) -> {
                List<String> stringValues = new ArrayList<>();
                for (Object o : v) stringValues.add(o == null ? "" : o.toString());
                result.put(k, stringValues);
            });
            return result;
        }

        @Override
        public String getHeaderString(String name) {
            List<Object> values = headers.get(name);
            if (values == null || values.isEmpty()) return null;
            return values.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(null);
        }
    }

    // ─── HeaderDelegate implementations ──────────────────────────────────────────

    private static class MediaTypeHeaderDelegate implements HeaderDelegate<MediaType> {
        @Override
        public MediaType fromString(String value) {
            if (value == null || value.isEmpty()) {
                return MediaType.WILDCARD_TYPE;
            }
            // Parse manually to avoid circular call to MediaType.valueOf -> HeaderDelegate.fromString
            String type = "*";
            String subtype = "*";
            Map<String, String> params = new LinkedHashMap<>();

            // Split off parameters (e.g. "application/json;charset=utf-8")
            String[] parts = value.split(";");
            String mediaRange = parts[0].trim();

            int slash = mediaRange.indexOf('/');
            if (slash > 0) {
                type = mediaRange.substring(0, slash).trim();
                subtype = mediaRange.substring(slash + 1).trim();
            } else if (!mediaRange.equals("*")) {
                type = mediaRange;
            }

            for (int i = 1; i < parts.length; i++) {
                String param = parts[i].trim();
                int eq = param.indexOf('=');
                if (eq > 0) {
                    params.put(param.substring(0, eq).trim(), param.substring(eq + 1).trim());
                }
            }

            return new MediaType(type, subtype, params);
        }

        @Override
        public String toString(MediaType value) {
            if (value == null) return "";
            // Format manually — avoid MediaType.toString() which recurses through RuntimeDelegate
            StringBuilder sb = new StringBuilder();
            sb.append(value.getType()).append('/').append(value.getSubtype());
            Map<String, String> params = value.getParameters();
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    sb.append(';').append(entry.getKey()).append('=').append(entry.getValue());
                }
            }
            return sb.toString();
        }
    }

    private static class DateHeaderDelegate implements HeaderDelegate<Date> {
        @Override
        public Date fromString(String value) {
            throw new UnsupportedOperationException("Date header parsing not supported.");
        }

        @Override
        public String toString(Date value) {
            return value == null ? "" : value.toString();
        }
    }
}


