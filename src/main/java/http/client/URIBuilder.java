package http.client;

import http.client.utils.CollectionUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public class URIBuilder {

    private static final String PATH_DELIMITER = "/";

    private static final String HTTP_PATTERN = "(?i)(http|https):";
    private static final String HOST_PATTERN = "([^\\[/?#:]*)";
    private static final String PORT_PATTERN = "(\\d*(?:\\{[^/]+?\\})?)";
    private static final String PATH_PATTERN = "([^?#]*)";
    private static final String LAST_PATTERN = "(.*)";

    private static final Pattern HTTP_URL_PATTERN = Pattern.compile(
            "^" + HTTP_PATTERN + "(//" + HOST_PATTERN + "(:" + PORT_PATTERN + ")?" + ")?" +
                    PATH_PATTERN + "(\\?" + LAST_PATTERN + ")?");

    private boolean encode = false;
    private Charset charset;
    private String scheme;
    private String host;
    private String port;
    private String path;
    private Map<String, List<String>> queryParams = new HashMap<>();

    public WithHttpUri builder() {
        return new InternalUriBuilder();
    }

    public final class InternalUriBuilder implements WithHttpUri, WithPath, WithQuery, WithEncode, FinalBuilder {

        @Override
        public WithPath fromHttpUrl(String httpUrl) {
            requireNonNull(httpUrl);
            Matcher matcher = HTTP_URL_PATTERN.matcher(httpUrl);
            if (matcher.matches()) {
                String scheme = matcher.group(1);
                URIBuilder.this.scheme = (scheme != null) ? scheme.toLowerCase() : null;
                URIBuilder.this.host = matcher.group(3);
                URIBuilder.this.port = matcher.group(5);
                URIBuilder.this.path = matcher.group(6);
                return this;
            } else {
                throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
            }
        }

        @Override
        public WithQuery path(String path) {
            requireNonNull(path);
            if (!path.isBlank()) {
                URIBuilder.this.path += (path.startsWith(PATH_DELIMITER)) ? path : PATH_DELIMITER + path;
                return this;
            } else {
                throw new IllegalArgumentException("Path can't be blank.");
            }
        }

        @Override
        public WithEncode queryParams(Map<String, List<String>> queryParams) {
            requireNonNull(queryParams);
            URIBuilder.this.queryParams = queryParams;
            return this;
        }

        @Override
        public FinalBuilder encode() {
            URIBuilder.this.encode = true;
            URIBuilder.this.charset = UTF_8;
            return this;
        }

        @Override
        public FinalBuilder encode(Charset charset) {
            requireNonNull(charset);
            URIBuilder.this.encode = true;
            URIBuilder.this.charset = charset;
            return this;
        }

        @Override
        public URI build() {
            StringBuilder uri = new StringBuilder();
            uri.append(URIBuilder.this.scheme);
            uri.append("://");
            uri.append(URIBuilder.this.host);
            uri.append(':');
            uri.append(URIBuilder.this.port);
            uri.append(URIBuilder.this.path);
            if (!URIBuilder.this.queryParams.isEmpty()) {
                uri.append("?");
                uri.append(buildQuery());
            }
            return tryToCreateUri(uri.toString());
        }

        private URI tryToCreateUri(String uri) {
            try {
                return new URI(uri);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }

        private String buildQuery() {
            StringBuilder queryBuilder = new StringBuilder();
            queryParams.forEach((name, values) -> {
                if (CollectionUtils.isEmpty(values)) {
                    if (queryBuilder.length() != 0) {
                        queryBuilder.append('&');
                    }
                    queryBuilder.append(name);
                } else {
                    for (Object value : values) {
                        if (queryBuilder.length() != 0) {
                            queryBuilder.append('&');
                        }
                        queryBuilder.append(name);
                        if (value != null) {
                            queryBuilder.append('=').append(value.toString());
                        }
                    }
                }
            });
            String query = queryBuilder.toString();
            return (URIBuilder.this.encode) ? URLEncoder.encode(query, URIBuilder.this.charset) : query;
        }
    }

    public interface WithHttpUri extends FinalBuilder {
        WithPath fromHttpUrl(String httpUrl);
    }

    public interface WithPath extends FinalBuilder {
        WithQuery path(String path);
    }

    public interface WithQuery extends FinalBuilder {
        WithEncode queryParams(Map<String, List<String>> queryParams);
    }

    public interface WithEncode extends FinalBuilder {
        FinalBuilder encode();

        FinalBuilder encode(Charset charset);
    }

    public interface FinalBuilder {
        URI build();
    }
}
