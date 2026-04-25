package com.fileweaver.api.lambda;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fileweaver.Application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * API Gateway -> Lambda entrypoint. Boots the Spring application once per
 * container and replays HTTP requests through the Spring DispatcherServlet.
 */
public class ApiGatewayLambdaHandler implements RequestStreamHandler {

    private static SpringBootLambdaContainerHandler<?, ?> handler;

    static {
        try {
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(Application.class);
        } catch (ContainerInitializationException e) {
            throw new IllegalStateException("Could not initialize Spring Boot Lambda handler", e);
        }
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        handler.proxyStream(input, output, context);
    }
}
