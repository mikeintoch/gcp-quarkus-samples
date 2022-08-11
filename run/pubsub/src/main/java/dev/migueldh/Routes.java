package dev.migueldh;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

public class Routes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        restConfiguration().bindingMode(RestBindingMode.json);

        rest("/")
         .post()
         .to("direct:printMessage");


        from("direct:printMessage")
         .transform().jsonpath("$.message.data")
         .unmarshal().base64()
        .log("Hello ${body}!");

         
    }
    
}
