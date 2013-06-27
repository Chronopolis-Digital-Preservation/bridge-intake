/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.amqp;

import java.io.IOException;
import java.util.Map;
import org.apache.log4j.Logger;
import org.chronopolis.messaging.base.ChronMessage2;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 *
 * @author shake
 */
public class TopicProducer implements ChronProducer {

    private final Logger log = Logger.getLogger(TopicProducer.class);

    private RabbitTemplate template;
    private String defaultRoutingKey;

    public TopicProducer(RabbitTemplate template) {
        this.template = template;
    }

    public void send(ChronMessage2 message, String routingKey) {
        boolean done = false;
        int numTries = 0;
        log.debug("Preparing message " + message.toString());
        MessageProperties props = new MessageProperties();
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        // props.setContentType("application/json");

        if ( null == routingKey ) { 
            routingKey = defaultRoutingKey;
        }
        
        Map<String, Object> headers = message.getChronHeader().getHeader();
        if ( headers != null && !headers.isEmpty()) {
            for ( String key : headers.keySet()) {
                props.setHeader(key, headers.get(key));
            }
        }
        try {
            while ( !done && numTries < 3 ) {
                Message msg = new Message(message.createMessage(), props);
                log.info("Sending "+ message.getType() + " to " + routingKey);

                template.send(routingKey, msg);
                done = true;
            }
        } catch (IOException ex) {
            log.error(ex.toString());
            numTries++;
        }

        
    }


    
}
