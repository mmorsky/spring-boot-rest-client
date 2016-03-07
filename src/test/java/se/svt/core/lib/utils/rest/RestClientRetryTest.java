package se.svt.core.lib.utils.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.springframework.test.web.client.MockRestServiceServer.createServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
@SpringApplicationConfiguration(classes = RestClientRetryTest.TestConfiguration.class)
public class RestClientRetryTest {

    @Autowired
    private FooClient fooClient;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${svt.rest-client.services.localhost}")
    private String requestUrl;

    private MockRestServiceServer server;

    @Configuration
    @EnableAutoConfiguration
    @EnableRestClients
    @EnableRetry
    protected static class TestConfiguration {

    }

    @RestClient(name = "localhost", retryOn = {HttpStatus.SERVICE_UNAVAILABLE}, retryOnException = {IOException.class})
    interface FooClient {

        @RequestMapping
        Void foo();

    }

    @Before
    public void setUp() throws Exception {
        server = createServer(restTemplate);
    }

    @After
    public void tearDown() throws Exception {
        server.verify();
    }

    @Test
    public void testRetry() throws Exception {
        server.expect(requestTo(defaultUrl()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        server.expect(requestTo(defaultUrl()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess());

        fooClient.foo();
    }

    @Test(expected = HttpServerErrorException.class)
    public void testShouldNotRetry() throws Exception {
        server.expect(requestTo(defaultUrl()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withServerError());

        fooClient.foo();
    }

    @Test
    public void testShouldRetryOnIoException() throws Exception {
        server.expect(requestTo(defaultUrl()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(request -> {
                throw new IOException();
            });

        server.expect(requestTo(defaultUrl()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess());

        fooClient.foo();
    }

    @Test(expected = RuntimeException.class)
    public void testShouldNotRetryOnOtherExceptions() throws Exception {
        server.expect(requestTo(defaultUrl()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(request -> {
                throw new RuntimeException();
            });

        fooClient.foo();
    }

    private String defaultUrl() {
        return requestUrl + "/";
    }
}
