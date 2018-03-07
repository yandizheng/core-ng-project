package core.framework.impl.web.service;

import core.framework.api.web.service.DELETE;
import core.framework.api.web.service.GET;
import core.framework.api.web.service.PATCH;
import core.framework.api.web.service.POST;
import core.framework.api.web.service.PUT;
import core.framework.api.web.service.Path;
import core.framework.api.web.service.PathParam;
import core.framework.http.HTTPMethod;
import core.framework.impl.reflect.Params;
import core.framework.impl.validate.type.JSONTypeValidator;
import core.framework.impl.web.bean.RequestBeanMapper;
import core.framework.impl.web.bean.ResponseBeanTypeValidator;
import core.framework.impl.web.route.PathPatternValidator;
import core.framework.util.Exceptions;
import core.framework.util.Sets;
import core.framework.util.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * @author neo
 */
public class WebServiceInterfaceValidator {
    private final Class<?> serviceInterface;
    private final RequestBeanMapper requestBeanMapper;
    private final ResponseBeanTypeValidator responseBeanTypeValidator;

    public WebServiceInterfaceValidator(Class<?> serviceInterface, RequestBeanMapper requestBeanMapper, ResponseBeanTypeValidator responseBeanTypeValidator) {
        this.serviceInterface = serviceInterface;
        this.requestBeanMapper = requestBeanMapper;
        this.responseBeanTypeValidator = responseBeanTypeValidator;
    }

    public void validate() {
        if (!serviceInterface.isInterface())
            throw Exceptions.error("service interface must be interface, serviceInterface={}", serviceInterface.getCanonicalName());

        for (Method method : serviceInterface.getDeclaredMethods()) {
            validate(method);
        }
    }

    private void validate(Method method) {
        validateHTTPMethod(method);

        HTTPMethod httpMethod = HTTPMethods.httpMethod(method);

        Path path = method.getDeclaredAnnotation(Path.class);
        if (path == null) throw Exceptions.error("method must have @Path, method={}", method);
        new PathPatternValidator(path.value()).validate();

        validateResponseBeanType(method.getGenericReturnType());

        Set<String> pathVariables = pathVariables(path.value());
        Type requestBeanType = null;

        Annotation[][] annotations = method.getParameterAnnotations();
        Type[] paramTypes = method.getGenericParameterTypes();
        Set<String> pathParams = Sets.newHashSet();

        for (int i = 0; i < paramTypes.length; i++) {
            Type paramType = paramTypes[i];
            PathParam pathParam = Params.annotation(annotations[i], PathParam.class);
            if (pathParam != null) {
                validatePathParamType(paramType);
                pathParams.add(pathParam.value());
            } else {
                if (requestBeanType != null)
                    throw Exceptions.error("service method must not have more than one bean param, previous={}, current={}", requestBeanType.getTypeName(), paramType.getTypeName());
                requestBeanType = paramType;

                if (httpMethod == HTTPMethod.GET || httpMethod == HTTPMethod.DELETE) {
                    requestBeanMapper.registerQueryParamBean(requestBeanType);
                } else {
                    requestBeanMapper.registerRequestBean(requestBeanType);
                }
            }
        }

        if (pathVariables.size() != pathParams.size() || !pathVariables.containsAll(pathParams))
            throw Exceptions.error("service method @PathParam params must match variable in path pattern, path={}, method={}", path.value(), method);
    }

    private Set<String> pathVariables(String path) {
        Set<String> names = Sets.newHashSet();
        String[] tokens = Strings.split(path, '/');
        for (String token : tokens) {
            if (Strings.startsWith(token, ':')) {
                int paramIndex = token.indexOf('(');
                int endIndex = paramIndex > 0 ? paramIndex : token.length();
                boolean isNew = names.add(token.substring(1, endIndex));
                if (!isNew) throw Exceptions.error("path must not have duplicate param name, path={}", path);
            }
        }
        return names;
    }

    private void validatePathParamType(Type paramType) {
        if (!(paramType instanceof Class))
            throw Exceptions.error("path param must be class, type={}", paramType.getTypeName());

        Class<?> paramClass = (Class<?>) paramType;

        if (paramClass.isPrimitive())
            throw Exceptions.error("primitive class is not supported, please use object class, paramClass={}", paramClass);

        if (Integer.class.equals(paramClass)) return;
        if (Long.class.equals(paramClass)) return;
        if (String.class.equals(paramClass)) return;
        if (paramClass.isEnum()) {
            JSONTypeValidator.validateEnumClass(paramClass);
            return;
        }
        throw Exceptions.error("path param class is not supported, paramClass={}", paramClass);
    }

    private void validateResponseBeanType(Type responseBeanType) {
        if (void.class == responseBeanType) return;
        responseBeanTypeValidator.validate(responseBeanType);
    }

    private void validateHTTPMethod(Method method) {
        int count = 0;
        if (method.isAnnotationPresent(GET.class)) count++;
        if (method.isAnnotationPresent(POST.class)) count++;
        if (method.isAnnotationPresent(PUT.class)) count++;
        if (method.isAnnotationPresent(DELETE.class)) count++;
        if (method.isAnnotationPresent(PATCH.class)) count++;
        if (count != 1)
            throw Exceptions.error("method must have exact one http method annotation, method={}", method);
    }
}
