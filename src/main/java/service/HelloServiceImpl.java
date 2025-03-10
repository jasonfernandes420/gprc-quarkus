package service;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.flydubai.HelloReply;
import org.flydubai.HelloRequest;
import org.flydubai.HelloService;

@GrpcService
public class HelloServiceImpl implements HelloService {
    private static final String SOAP_ENDPOINT = "http://localhost:8080/HelloSoapService";

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        String clientName = request.getClientName();
        String welcomeMessage = extractWelcomeMessage(callSoapService(clientName));
        if (welcomeMessage.startsWith("Error")) {
            return Uni.createFrom().item(() ->
                    HelloReply.newBuilder().setMessage(welcomeMessage).build());
        }
        return Uni.createFrom().item(() ->
                HelloReply.newBuilder().setMessage("Hello " + welcomeMessage).build()
        );
    }
    private String callSoapService(String clientName) {
        System.out.println("Entered callSoapService");
        Client client = ClientBuilder.newClient();
        String soapRequest = String.format(
                "<SOAP-ENV:Envelope\n" +
                        "   xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                        "   xmlns:m=\"http://www.flydubai.com/HelloSoap\">\n" +
                        "    <SOAP-ENV:Body>\n" +
                        "        <m:HelloSoap>\n" +
                        "            <m:ClientName>%s</m:ClientName>\n" +
                        "        </m:HelloSoap>\n" +
                        "    </SOAP-ENV:Body>\n" +
                        "</SOAP-ENV:Envelope>",
                clientName
        );
        System.out.println("initiating response callSoapService="+soapRequest);

        Response response = client.target(SOAP_ENDPOINT)
                .request()
                .header("Content-Type", null)
                .header("Content-Type", "text/xml;charset=UTF-8")
                .post(Entity.entity(soapRequest, MediaType.TEXT_XML));

        if (response.getStatus() == 200) {
            return response.readEntity(String.class); // Return the entire SOAP response
        } else {
            return "Error calling SOAP service: " + response.getStatus();
        }

    }

    private String extractWelcomeMessage(String soapResponse) {
        if (soapResponse.startsWith("Error")) {
            return soapResponse;
        }

        int startIndex = soapResponse.indexOf("Welcome") + "Welcome ".length();
        int endIndex = soapResponse.indexOf("</ns2:Response>");
        if (endIndex != -1) {
            return soapResponse.substring(startIndex, endIndex);
        } else {
            return "Could not extract welcome message from SOAP response.";
        }
    }
}
