package com.fileweaver.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthenticatedKeyResolver implements HandlerMethodArgumentResolver {

    public static final String REQUEST_ATTR = "fileweaver.apiKey";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedKey.class)
            && ApiKey.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mav,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest req = webRequest.getNativeRequest(HttpServletRequest.class);
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No request");
        }
        Object key = req.getAttribute(REQUEST_ATTR);
        if (!(key instanceof ApiKey k)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing API key");
        }
        return k;
    }
}
