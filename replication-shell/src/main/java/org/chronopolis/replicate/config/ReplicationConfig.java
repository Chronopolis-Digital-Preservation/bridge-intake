package org.chronopolis.replicate.config;

import com.rabbitmq.client.ConnectionFactory;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.amqp.ConnectionListenerImpl;
import org.chronopolis.amqp.TopicProducer;
import org.chronopolis.amqp.error.ErrorHandlerImpl;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.CredentialRequestInterceptor;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.settings.AMQPSettings;
import org.chronopolis.common.settings.AceSettings;
import org.chronopolis.common.settings.ChronopolisSettings;
import org.chronopolis.common.settings.SMTPSettings;
import org.chronopolis.db.common.RestoreRepository;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.ReplicateMessageListener;
import org.chronopolis.replicate.jobs.AceRegisterJobListener;
import org.chronopolis.replicate.jobs.BagDownloadJobListener;
import org.chronopolis.replicate.jobs.TokenStoreDownloadJobListener;
import org.chronopolis.replicate.processor.CollectionInitProcessor;
import org.chronopolis.replicate.processor.CollectionRestoreLocationProcessor;
import org.chronopolis.replicate.processor.CollectionRestoreRequestProcessor;
import org.chronopolis.replicate.processor.FileQueryProcessor;
import org.chronopolis.replicate.processor.FileQueryResponseProcessor;
import org.chronopolis.replicate.util.URIUtil;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import retrofit.RestAdapter;

import javax.annotation.Resource;

/**
 * Created by shake on 4/16/14.
 */
@Configuration
@Import({JPAConfiguration.class})
public class ReplicationConfig {
    public final Logger log = LoggerFactory.getLogger(ReplicationConfig.class);

    @Resource
    Environment env;

    @Bean
    AceService aceService(AceSettings aceSettings) {
        String endpoint = URIUtil.buildAceUri(aceSettings.getAmHost(),
                aceSettings.getAmPort(),
                aceSettings.getAmPath()).toString();

        CredentialRequestInterceptor interceptor = new CredentialRequestInterceptor(
                aceSettings.getAmUser(),
                aceSettings.getAmPassword());

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(endpoint)
                .setRequestInterceptor(interceptor)
                .build();

        return restAdapter.create(AceService.class);
    }

    @Bean
    MailUtil mailUtil(SMTPSettings smtpSettings) {
        MailUtil mailUtil = new MailUtil();
        mailUtil.setSmtpFrom(smtpSettings.getFrom());
        mailUtil.setSmtpTo(smtpSettings.getTo());
        mailUtil.setSmtpHost(smtpSettings.getHost());
        mailUtil.setSmtpSend(smtpSettings.getSend());
        return mailUtil;
    }

    @Bean
    MessageFactory messageFactory(ReplicationSettings chronopolisSettings) {
        return new MessageFactory(chronopolisSettings);
    }

    @Bean
    ConnectionListener connectionListener() {
        return new ConnectionListenerImpl();
    }

    @Bean
    ConnectionFactory rabbitConnectionFactory(AMQPSettings amqpSettings) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setRequestedHeartbeat(60);
        connectionFactory.setConnectionTimeout(300);
        connectionFactory.setVirtualHost(amqpSettings.getVirtualHost());

        return connectionFactory;
    }

    @Bean
    CachingConnectionFactory connectionFactory(ConnectionFactory rabbitConnectionFactory) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitConnectionFactory);

        connectionFactory.setPublisherConfirms(true);
        connectionFactory.setPublisherReturns(true);

        connectionFactory.addConnectionListener(connectionListener());
        connectionFactory.setAddresses("adapt-mq.umiacs.umd.edu");

        return connectionFactory;
    }

    @Bean
    RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate();
        template.setExchange("chronopolis-control");
        template.setConnectionFactory(connectionFactory);
        template.setMandatory(true);


        return template;
    }

    @Bean
    TopicProducer producer(RabbitTemplate rabbitTemplate) {
        return new TopicProducer(rabbitTemplate);
    }

    @Bean
    FileQueryProcessor fileQueryProcessor(TopicProducer producer) {
        return new FileQueryProcessor(producer);
    }

    @Bean
    FileQueryResponseProcessor fileQueryResponseProcessor(TopicProducer producer) {
        return new FileQueryResponseProcessor(producer);
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    Scheduler scheduler() {
        try {
            return StdSchedulerFactory.getDefaultScheduler();
        } catch (SchedulerException e) {
            throw new BeanCreationException("Could not create scheduler", e);
        }
    }

    @Bean
    AceRegisterJobListener aceRegisterJobListener(MessageFactory messageFactory,
                                                  ReplicationSettings replicationSettings,
                                                  TopicProducer producer,
                                                  MailUtil mailUtil) {
        AceRegisterJobListener jobListener = new AceRegisterJobListener(
                "ace-register",
                scheduler(),
                producer,
                messageFactory,
                replicationSettings,
                mailUtil);

        try {
            scheduler().getListenerManager().addJobListener(jobListener,
                    GroupMatcher.<JobKey>groupEquals("AceRegister"));
        } catch (SchedulerException e) {
            throw new BeanCreationException("Could not register listener", e);
        }

        return jobListener;
    }

    @Bean
    BagDownloadJobListener bagDownloadJobListener(MessageFactory messageFactory,
                                                  ReplicationSettings replicationSettings,
                                                  TopicProducer producer,
                                                  MailUtil mailUtil) {
        BagDownloadJobListener jobListener = new BagDownloadJobListener(
                "bag-download",
                scheduler(),
                replicationSettings,
                mailUtil,
                messageFactory,
                producer);

        try {
            scheduler().getListenerManager().addJobListener(jobListener,
                    GroupMatcher.<JobKey>groupEquals("BagDownload"));
        } catch (SchedulerException e) {
            throw new BeanCreationException("Could not register listener", e);
        }

        return jobListener;
    }

    @Bean
    TokenStoreDownloadJobListener tokenStoreDownloadJobListener(MessageFactory messageFactory,
                                                                ReplicationSettings replicationSettings,
                                                                TopicProducer producer,
                                                                MailUtil mailUtil) {
        TokenStoreDownloadJobListener jobListener = new TokenStoreDownloadJobListener(
                "token-store-download",
                scheduler(),
                replicationSettings,
                mailUtil,
                messageFactory,
                producer);

        try {
            scheduler().getListenerManager().addJobListener(jobListener,
                    GroupMatcher.<JobKey>groupEquals("TokenDownload"));
        } catch (SchedulerException e) {
            throw new BeanCreationException("Could not register listener", e);
        }

        return jobListener;
    }

    @Bean
    @DependsOn({"tokenStoreDownloadJobListener",
                "bagDownloadJobListener",
                "aceRegisterJobListener"})
    CollectionInitProcessor collectionInitProcessor(MessageFactory messageFactory,
                                                    ReplicationSettings replicationSettings,
                                                    AceService aceService,
                                                    TopicProducer producer,
                                                    MailUtil mailUtil) {
        return new CollectionInitProcessor(producer,
                messageFactory,
                replicationSettings,
                mailUtil,
                scheduler(),
                aceService);
    }

    @Bean
    CollectionRestoreRequestProcessor collectionRestoreRequestProcessor(ChronProducer producer,
                                                                        MessageFactory messageFactory,
                                                                        AceService aceService,
                                                                        RestoreRepository restoreRepository) {
        return new CollectionRestoreRequestProcessor(producer,
                messageFactory,
                aceService,
                restoreRepository);
    }

    @Bean
    CollectionRestoreLocationProcessor collectionRestoreLocationProcessor(ChronopolisSettings chronopolisSettings,
                                                                          ChronProducer producer,
                                                                          MessageFactory messageFactory,
                                                                          RestoreRepository restoreRepository) {
        return new CollectionRestoreLocationProcessor(chronopolisSettings,
                producer,
                messageFactory,
                restoreRepository);
    }

    @Bean
    MessageListener messageListener(CollectionRestoreRequestProcessor collectionRestoreRequestProcessor,
                                    CollectionRestoreLocationProcessor collectionRestoreLocationProcessor,
                                    CollectionInitProcessor collectionInitProcessor,
                                    FileQueryProcessor fileQueryProcessor,
                                    FileQueryResponseProcessor fileQueryResponseProcessor) {
        return new ReplicateMessageListener(
                fileQueryProcessor,
                fileQueryResponseProcessor,
                collectionInitProcessor,
                collectionRestoreRequestProcessor,
                collectionRestoreLocationProcessor);
    }

    @Bean
    ErrorHandlerImpl errorHandler() {
        return new ErrorHandlerImpl();
    }

    @Bean
    TopicExchange topicExchange() {
        return new TopicExchange("chronopolis-control");
    }

    @Bean
    Queue broadcastQueue(ReplicationSettings replicationSettings) {
       return new Queue(replicationSettings.getBroadcastQueueName(), true);
    }

    @Bean
    Binding broadcastBinding(ReplicationSettings replicationSettings,
                             Queue broadcastQueue) {
        return BindingBuilder.bind(broadcastQueue)
                             .to(topicExchange())
                             .with(replicationSettings.getBroadcastQueueBinding());
    }


    @Bean
    Queue directQueue(ReplicationSettings replicationSettings) {
       return new Queue(replicationSettings.getDirectQueueName(), true);
    }

    @Bean
    Binding directBinding(ReplicationSettings replicationSettings,
                          Queue directQueue) {
        return BindingBuilder.bind(directQueue)
                             .to(topicExchange())
                             .with(replicationSettings.getDirectQueueBinding());
    }

    @Bean
    RabbitAdmin rabbitAdmin(final Binding directBinding,
                            final Binding broadcastBinding,
                            final Queue directQueue,
                            final Queue broadcastQueue,
                            CachingConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.declareExchange(topicExchange());

        // admin.declareQueue(testQueue());
        admin.declareQueue(broadcastQueue);
        admin.declareQueue(directQueue);

        // admin.declareBinding(testBinding());
        admin.declareBinding(broadcastBinding);
        admin.declareBinding(directBinding);

        return admin;
    }

    @Bean
    @DependsOn("rabbitAdmin")
    SimpleMessageListenerContainer simpleMessageListenerContainer(MessageListener messageListener,
                                                                  ReplicationSettings replicationSettings,
                                                                  CachingConnectionFactory connectionFactory) {
        String broadcastQueueName = replicationSettings.getBroadcastQueueName();
        String directQueueName = replicationSettings.getDirectQueueName();

        log.info("Broadcast queue {} bound to {}",
                broadcastQueueName,
                replicationSettings.getBroadcastQueueBinding());
        log.info("Direct queue {} bound to {}",
                directQueueName,
                replicationSettings.getDirectQueueBinding());

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setErrorHandler(errorHandler());
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(broadcastQueueName, directQueueName);
        container.setMessageListener(messageListener);
        return container;
    }

}
