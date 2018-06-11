package com.theniceidea.summer.springproxyvertx.srv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theniceidea.summer.model.SummerSum;
import com.theniceidea.summer.springproxyvertx.base.RestExceptionModel;
import com.theniceidea.summer.springproxyvertx.base.RestSucessModel;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;

import static com.theniceidea.summer.core.srv.Summers.sum;

class RouterHandlerItem implements Handler<RoutingContext>{
    private static ObjectMapper objectMapper = new ObjectMapper();
    private String path;
    private Object bean;
    private Method method;
    private Class<? extends SummerSum> modelClass;
    private HashMap<String, Field> modelFields;

    @Override
    public void handle(RoutingContext routingContext) {
        try {
            handl(routingContext);
        } catch (IOException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    private void handl(RoutingContext routingContext)
        throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException {
        HttpServerRequest request = routingContext.request();
        HttpMethod method = request
            .method();
        SummerSum model;
        if(HttpMethod.POST == method || HttpMethod.PUT == method || HttpMethod.DELETE == method){
            String body = routingContext.getBody()
                .toString(Charset.forName("utf-8"));
            if(!StringUtils.isEmpty(body)){
                model = objectMapper.readValue(body, modelClass);
            }else model = modelClass.newInstance();
        }else if(HttpMethod.GET == method){
            model = modelClass.newInstance();

        }else throw new RuntimeException("not support ");
        routingContext.queryParams().forEach(entry -> {
            writeBeanValue(entry.getKey(), entry.getValue(), model);
        });
        this.invoke(routingContext, model);
    }
    private void invoke(RoutingContext routingContext, SummerSum model){
        try {
            this.method.invoke(this.bean, model);
            RestSucessModel resultModel = (RestSucessModel) model.inst(RestSucessModel.class);
            resultModel.setRoutingContext(routingContext);
            resultModel.setResult(model.getResult());
            sum(resultModel);
        }catch (Exception e){
            RestExceptionModel exceptionModel = (RestExceptionModel) model.inst(RestExceptionModel.class);
            exceptionModel.setRoutingContext(routingContext);
            exceptionModel.setException(e);
            if(!sum(exceptionModel)) throw new RuntimeException(e.getMessage(), e);
        }
    }
    private void writeBeanValue(String key, String value, SummerSum model){
        try {
            BeanUtils.setProperty(model, key, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            //这里不log了
        }

//        PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(modelClass, key);
//        if(isNull(descriptor)) return;
//        PropertyEditor editor = descriptor.createPropertyEditor(model);
//        Class<?> propertyType = descriptor.getPropertyType();
//        editor.setValue(ConfigurationValueConverter.fromString(value, propertyType));
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Object getBean() {
        return bean;
    }

    public void setBean(Object bean) {
        this.bean = bean;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Class<? extends SummerSum> getModelClass() {
        return modelClass;
    }

    public void setModelClass(Class<? extends SummerSum> modelClass) {
        this.modelClass = modelClass;
    }

    public HashMap<String, Field> getModelFields() {
        return modelFields;
    }

    public void setModelFields(HashMap<String, Field> modelFields) {
        this.modelFields = modelFields;
    }
}