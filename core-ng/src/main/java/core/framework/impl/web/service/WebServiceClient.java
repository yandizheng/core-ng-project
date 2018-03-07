package core.framework.impl.web.service;

import core.framework.api.http.HTTPStatus;
import core.framework.http.ContentType;
import core.framework.http.HTTPClient;
import core.framework.http.HTTPMethod;
import core.framework.http.HTTPRequest;
import core.framework.http.HTTPResponse;
import core.framework.impl.json.JSONMapper;
import core.framework.impl.log.ActionLog;
import core.framework.impl.log.LogManager;
import core.framework.impl.web.HTTPServerHandler;
import core.framework.impl.web.bean.RequestBeanMapper;
import core.framework.impl.web.route.Path;
import core.framework.json.JSON;
import core.framework.log.Severity;
import core.framework.util.Encodings;
import core.framework.util.Exceptions;
import core.framework.util.Maps;
import core.framework.util.Strings;
import core.framework.validate.ValidationException;
import core.framework.web.service.RemoteServiceException;
import core.framework.web.service.WebServiceClientInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author neo
 */
public class WebServiceClient {
    private final Logger logger = LoggerFactory.getLogger(WebServiceClient.class);

    private final String serviceURL;
    private final HTTPClient httpClient;
    private final RequestBeanMapper mapper;
    private final LogManager logManager;
    private WebServiceClientInterceptor interceptor;

    public WebServiceClient(String serviceURL, HTTPClient httpClient, RequestBeanMapper mapper, LogManager logManager) {
        this.serviceURL = serviceURL;
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.logManager = logManager;
    }

    // used by generated code, must be public
    public String serviceURL(String pathPattern, Map<String, Object> pathParams) {
        StringBuilder builder = new StringBuilder(serviceURL);
        Path path = Path.parse(pathPattern).next; // skip the first '/'
        while (path != null) {
            String value = path.value;
            if ("/".equals(value)) {
                builder.append(value);
            } else if (Strings.startsWith(value, ':')) {
                int paramIndex = value.indexOf('(');
                int endIndex = paramIndex > 0 ? paramIndex : value.length();
                String variable = value.substring(1, endIndex);
                String pathParam = pathParam(pathParams, variable);
                builder.append('/').append(Encodings.uriComponent(pathParam));
            } else {
                builder.append('/').append(value);
            }
            path = path.next;
        }
        return builder.toString();
    }

    private String pathParam(Map<String, Object> pathParams, String variable) {
        Object param = pathParams.get(variable);
        if (param == null) throw new ValidationException(Maps.newHashMap(variable, Strings.format("path param must not be null, name={}", variable)));
        // convert logic matches PathParams
        if (param instanceof String) {
            String paramValue = (String) param;
            if (Strings.isEmpty(paramValue))
                throw new ValidationException(Maps.newHashMap(variable, Strings.format("path param must not be empty, name={}", variable)));
            return paramValue;
        } else if (param instanceof Number) {
            return String.valueOf(param);
        } else if (param instanceof Enum) {
            return JSON.toEnumValue((Enum<?>) param);
        } else {
            throw Exceptions.error("not supported path param type, please contact arch team, type={}", param.getClass().getCanonicalName());
        }
    }

    // used by generated code, must be public
    public Object execute(HTTPMethod method, String serviceURL, Type requestType, Object requestBean, Type responseType) {
        HTTPRequest request = new HTTPRequest(method, serviceURL);
        request.accept(ContentType.APPLICATION_JSON);
        request.header(HTTPServerHandler.HEADER_CLIENT.toString(), logManager.appName);
        linkContext(request);

        if (requestType != null) {
            addRequestBean(request, method, requestType, requestBean);
        }

        if (interceptor != null) {
            logger.debug("intercept request, interceptor={}", interceptor.getClass().getCanonicalName());
            interceptor.intercept(request);
        }

        HTTPResponse response = httpClient.execute(request);
        validateResponse(response);
        if (void.class != responseType) {
            return JSONMapper.fromJSON(responseType, response.body());
        } else {
            return null;
        }
    }

    void addRequestBean(HTTPRequest request, HTTPMethod method, Type requestType, Object requestBean) {
        if (method == HTTPMethod.GET || method == HTTPMethod.DELETE) {
            Map<String, String> queryParams = mapper.toParams(requestType, requestBean);
            addQueryParams(request, queryParams);
        } else if (method == HTTPMethod.POST || method == HTTPMethod.PUT || method == HTTPMethod.PATCH) {
            byte[] json = mapper.toJSON(requestType, requestBean);
            request.body(json, ContentType.APPLICATION_JSON);
        } else {
            throw Exceptions.error("not supported method, method={}", method);
        }
    }

    public void intercept(WebServiceClientInterceptor interceptor) {
        if (this.interceptor != null) throw Exceptions.error("found duplicate interceptor, previous={}", this.interceptor.getClass().getCanonicalName());
        this.interceptor = interceptor;
    }

    void addQueryParams(HTTPRequest request, Map<String, String> queryParams) {
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            String value = entry.getValue();
            if (value != null) request.addParam(entry.getKey(), value);
        }
    }

    private void linkContext(HTTPRequest httpRequest) {
        ActionLog actionLog = logManager.currentActionLog();
        if (actionLog == null) return;  // web service client may be used without action log context

        httpRequest.header(HTTPServerHandler.HEADER_REF_ID.toString(), actionLog.refId());

        if (actionLog.trace) {
            httpRequest.header(HTTPServerHandler.HEADER_TRACE.toString(), "true");
        }
    }

    void validateResponse(HTTPResponse response) {
        HTTPStatus status = response.status();
        if (status.code >= 200 && status.code < 300) return;
        byte[] responseBody = response.body();
        try {
            ErrorResponse error = JSONMapper.fromJSON(ErrorResponse.class, responseBody);
            logger.debug("failed to call remote service, id={}, severity={}, errorCode={}, remoteStackTrace={}", error.id, error.severity, error.errorCode, error.stackTrace);
            throw new RemoteServiceException(error.message, parseSeverity(error.severity), error.errorCode);
        } catch (RemoteServiceException e) {
            throw e;
        } catch (Throwable e) {
            String responseText = response.text();
            logger.warn("failed to decode response, statusCode={}, responseText={}", status.code, responseText, e);
            throw new RemoteServiceException(Strings.format("internal communication failed, status={}, responseText={}", status.code, responseText), Severity.ERROR, "REMOTE_SERVICE_ERROR", e);
        }
    }

    private Severity parseSeverity(String severity) {
        if (severity == null) return Severity.ERROR;
        return Severity.valueOf(severity);
    }
}
