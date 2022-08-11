package dev.migueldh;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;

public class Routes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        restConfiguration().bindingMode(RestBindingMode.json);

        rest("/")
         .post()
         .to("direct:processMessage");

         onException(Exception.class)
         .handled(true)
         .log("Server Error")
         .setBody(simple("Internal Server Error"));


        from("direct:processMessage")
         .marshal().json(JsonLibrary.Jackson)
        .choice()
          .when().jsonpath("$.message.data",true)
            .to("direct:printMessage")
          .otherwise() 
            .to("direct:printEmptyMessage");

        from("direct:printMessage")
         .transform().jsonpath("$.message.data")
         .unmarshal().base64()
         .log("Hello ${body}!")
         .setBody(simple("Information Received"));

         from("direct:printEmptyMessage")
          .log("Hello World!")
          .setBody(simple("Information Received was empty"));


    }
    
}
