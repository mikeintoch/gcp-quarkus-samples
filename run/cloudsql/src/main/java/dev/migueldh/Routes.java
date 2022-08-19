package dev.migueldh;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;

public class Routes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        restConfiguration().bindingMode(RestBindingMode.json);

        rest("/persons")
         .get()
         .to("direct:getPersons")
         .post()
         .to("direct:addPerson");

         onException(Exception.class)
         .handled(true)
         .log("Internal Server Error")
         .setBody(simple("Internal Server Error"));
     

         from("direct:getPersons")
         .setBody(simple("Select * from person"))
        .to("jdbc:default")
        .log("We have ${header[CamelJdbcRowCount]} persons in the database.");


        from("direct:addPerson")
         .setBody(simple("INSERT INTO person(name,age) values ('${body[name]}', '${body[age]}')"))
        .to("jdbc:default")
        .setBody(simple("Information Received"));

         
    }
    
}
