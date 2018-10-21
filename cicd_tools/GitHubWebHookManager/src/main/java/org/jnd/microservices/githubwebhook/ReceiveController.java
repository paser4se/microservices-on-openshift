package org.jnd.microservices.githubwebhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/receive")
public class ReceiveController {

    private Log log = LogFactory.getLog(ReceiveController.class);

    @RequestMapping(value = "/postreceive", method = RequestMethod.POST, produces = "application/json")
    ResponseEntity<String> postreceive(@RequestBody Object githubmessage, @RequestHeader HttpHeaders headers) {

        log.info("postreceive received");
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            log.info("message : "+mapper.writeValueAsString(githubmessage));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }


        return new ResponseEntity<>("OK", null, HttpStatus.OK);
    }
}
