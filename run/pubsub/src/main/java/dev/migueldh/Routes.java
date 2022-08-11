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
         .to("direct:printMessage");


        from("direct:printMessage")
         .transform().jsonpath("$.message.data")
         .unmarshal().base64()
         .unmarshal().json(JsonLibrary.Jackson)
        .to("direct:database");


        from("direct:database")
         .setBody(simple("INSERT INTO person(name,age) values ('${body[name]}', '${body[age]}')"))
        .to("jdbc:default")
        .setBody(simple("Information Received"));

         
    }
    
}
