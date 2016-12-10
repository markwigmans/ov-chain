package com.ximedes.ov.engine.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class TestRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("timer:foo")
                .to("log:bar");
    }
}