package se.svt.core.lib.utils.rest;

import lombok.Setter;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Setter
class RestClientFactoryBean implements FactoryBean<Object>, InitializingBean, ApplicationContextAware {

    private final ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

    private String name;
    private String url;
    private Class<?> type;
    private RetryOperationsInterceptor retryInterceptor;
    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.hasText(this.name, "Name must be set");
    }

    @Override
    public Object getObject() throws Exception {
        RestTemplate restTemplate = applicationContext.getBean(RestTemplate.class);
        RestClientContext context = applicationContext.getBean(RestClientContext.class);

        RestClientSpecification specification = context.findByRestClientName(name);

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.addInterface(type);

        RestClientInterceptor interceptor = new RestClientInterceptor(specification, restTemplate, getServiceUrl(context));

        if (nonNull(retryInterceptor)) {
            proxyFactory.addAdvice(retryInterceptor);
            interceptor.setRetryEnabled(true);
        }

        proxyFactory.addAdvice(interceptor);

        return proxyFactory.getProxy(classLoader);
    }

    private URI getServiceUrl(RestClientContext context) {
        if (isEmpty(url)) {
            return context.findServiceUriByName(name);
        }
        return URI.create(url);
    }

    @Override
    public Class<?> getObjectType() {
        return this.type;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
