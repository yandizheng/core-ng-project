public class TestWebService$Client implements core.framework.impl.web.service.TestWebService {
    private final core.framework.impl.web.service.WebServiceClient client;

    public TestWebService$Client(core.framework.impl.web.service.WebServiceClient $1) {
        this.client = $1;
    }

    public java.util.List batch(java.util.List param0) {
        java.lang.reflect.Type requestType = core.framework.util.Types.list(core.framework.impl.web.service.TestWebService.TestRequest.class);
        Object requestBean = param0;
        java.util.Map pathParams = new java.util.HashMap();
        String serviceURL = client.serviceURL("/test", pathParams);
        java.util.List response = (java.util.List) client.execute(core.framework.http.HTTPMethod.PUT, serviceURL, requestType, requestBean, core.framework.util.Types.list(core.framework.impl.web.service.TestWebService.TestResponse.class));
        return response;
    }

    public void create(java.lang.Integer param0, core.framework.impl.web.service.TestWebService.TestRequest param1) {
        java.lang.reflect.Type requestType = core.framework.impl.web.service.TestWebService.TestRequest.class;
        Object requestBean = param1;
        java.util.Map pathParams = new java.util.HashMap();
        pathParams.put("id", param0);
        String serviceURL = client.serviceURL("/test/:id", pathParams);
        java.lang.Void response = (java.lang.Void) client.execute(core.framework.http.HTTPMethod.PUT, serviceURL, requestType, requestBean, void.class);
    }

    public void delete(java.lang.String param0) {
        java.lang.reflect.Type requestType = null;
        Object requestBean = null;
        java.util.Map pathParams = new java.util.HashMap();
        pathParams.put("id", param0);
        String serviceURL = client.serviceURL("/test/:id", pathParams);
        java.lang.Void response = (java.lang.Void) client.execute(core.framework.http.HTTPMethod.DELETE, serviceURL, requestType, requestBean, void.class);
    }

    public java.util.Optional get(java.lang.Integer param0) {
        java.lang.reflect.Type requestType = null;
        Object requestBean = null;
        java.util.Map pathParams = new java.util.HashMap();
        pathParams.put("id", param0);
        String serviceURL = client.serviceURL("/test/:id", pathParams);
        java.util.Optional response = (java.util.Optional) client.execute(core.framework.http.HTTPMethod.GET, serviceURL, requestType, requestBean, core.framework.util.Types.optional(core.framework.impl.web.service.TestWebService.TestResponse.class));
        return response;
    }

    public void patch(java.lang.Integer param0, core.framework.impl.web.service.TestWebService.TestRequest param1) {
        java.lang.reflect.Type requestType = core.framework.impl.web.service.TestWebService.TestRequest.class;
        Object requestBean = param1;
        java.util.Map pathParams = new java.util.HashMap();
        pathParams.put("id", param0);
        String serviceURL = client.serviceURL("/test/:id", pathParams);
        java.lang.Void response = (java.lang.Void) client.execute(core.framework.http.HTTPMethod.PATCH, serviceURL, requestType, requestBean, void.class);
    }

    public core.framework.impl.web.service.TestWebService.TestResponse search(core.framework.impl.web.service.TestWebService.TestSearchRequest param0) {
        java.lang.reflect.Type requestType = core.framework.impl.web.service.TestWebService.TestSearchRequest.class;
        Object requestBean = param0;
        java.util.Map pathParams = new java.util.HashMap();
        String serviceURL = client.serviceURL("/test", pathParams);
        core.framework.impl.web.service.TestWebService.TestResponse response = (core.framework.impl.web.service.TestWebService.TestResponse) client.execute(core.framework.http.HTTPMethod.GET, serviceURL, requestType, requestBean, core.framework.impl.web.service.TestWebService.TestResponse.class);
        return response;
    }

}
