package com.nnmilestoempty.config.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@WebFilter(urlPatterns = {"/register", "/register/**", "/login"})
public class RequestThrottleFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RequestThrottleFilter.class);

    private static final int MAX_REQUESTS_PER_SECOND = 1;

    private final LoadingCache<String, Integer> requestCountsPerIpAddress;

    public RequestThrottleFilter() {
        requestCountsPerIpAddress = CacheBuilder.newBuilder().
                expireAfterWrite(3, TimeUnit.SECONDS).build(new CacheLoader<String, Integer>() {
            public Integer load(String key) {
                return 0;
            }
        });
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        String clientIpAddress = getClientIP(httpServletRequest);
        if (isMaximumRequestsPerSecondExceeded(clientIpAddress)) {
            logger.warn("Throttling request url={} from ip={}", httpServletRequest.getRequestURL(), clientIpAddress);
            httpServletResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpServletResponse.getWriter().write(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);

    }

    private boolean isMaximumRequestsPerSecondExceeded(String clientIpAddress) {
        int requests = 0;
        try {
            requests = requestCountsPerIpAddress.get(clientIpAddress);
            if (requests > MAX_REQUESTS_PER_SECOND) {
                requestCountsPerIpAddress.put(clientIpAddress, requests);
                return true;
            }
        } catch (ExecutionException e) {
            // This client IP isn't in the cache.
        }
        requests++;
        requestCountsPerIpAddress.put(clientIpAddress, requests);
        return false;
    }

    public String getClientIP(HttpServletRequest request) {
        String result = request.getRemoteAddr();
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            result = xfHeader.split(",")[0];
        }

        return result;
    }
}
