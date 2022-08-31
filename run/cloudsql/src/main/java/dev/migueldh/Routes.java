
package dev.migueldh;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

public class Routes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        restConfiguration().bindingMode(RestBindingMode.json);

        onException(Exception.class)
        .handled(true)
        .log("Internal Server Error")
        .setBody(simple("Internal Server Error"));

        rest("/persons")
                .get()
                .to("direct:getPersons")

                .post()
                .type(Person.class)
                .to("direct:addPerson");

        from("direct:getPersons")
                .to("jpa://dev.migueldh.Person?resultClass=dev.migueldh.Person&namedQuery=findAll")
                .log("Person List: ${body} ");

        from("direct:addPerson")
                .to("jpa://dev.migueldh.Person?usePersist=true");

    }
}
