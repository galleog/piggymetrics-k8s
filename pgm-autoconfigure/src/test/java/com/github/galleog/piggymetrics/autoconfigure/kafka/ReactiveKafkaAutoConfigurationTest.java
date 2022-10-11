package com.github.galleog.piggymetrics.autoconfigure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.SenderOptions;

/**
 * Tests for {@link ReactiveKafkaAutoConfiguration}.
 */
class ReactiveKafkaAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ReactiveKafkaAutoConfiguration.class));

    /**
     * Beans for {@link ReceiverOptions} and {@link ReactiveKafkaProducerTemplate} shouldn't be created when the
     * {@code spring.kafka.consumer.subscribeTopics} property doesn't exist and
     * there are no external {@link ReceiverOptions} bean.
     */
    @Test
    void shouldCreateReactiveKafkaProducerTemplateWithoutReactiveKafkaConsumerTemplateWhenNoTopicsAreSpecified() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SenderOptions.class);
            assertThat(context).hasSingleBean(ReactiveKafkaProducerTemplate.class);
            assertThat(context).doesNotHaveBean(ReceiverOptions.class);
            assertThat(context).doesNotHaveBean(ReactiveKafkaConsumerTemplate.class);
        });
    }

    /**
     * All beans should be created when the {@code spring.kafka.consumer.subscribeTopics} property is specified.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateAllBeans() {
        contextRunner.withPropertyValues("spring.kafka.consumer.subscribeTopics=foo")
                .run(context -> {
                    assertThat(context).hasSingleBean(SenderOptions.class);
                    assertThat(context).hasSingleBean(ReactiveKafkaProducerTemplate.class);
                    assertThat(context).hasSingleBean(ReceiverOptions.class);
                    assertThat(context.getBean(ReceiverOptions.class).subscriptionTopics())
                            .containsExactly("foo");
                    assertThat(context).hasSingleBean(ReactiveKafkaConsumerTemplate.class);
                });
    }

    /**
     * No extra {@link SenderOptions} beans should be created if such a bean already exists.
     */
    @Test
    void shouldNotCreateSenderOptionsWhenItAlreadyExists() {
        contextRunner.withBean(SenderOptions.class, SenderOptions::create)
                .run(context -> {
                    assertThat(context).hasSingleBean(SenderOptions.class);
                    assertThat(context).hasSingleBean(ReactiveKafkaProducerTemplate.class);
                });
    }

    /**
     * Neither {@link SenderOptions} nor {@link ReactiveKafkaProducerTemplate} bean should be created
     * if a {@link ReactiveKafkaProducerTemplate} bean already exists.
     */
    @Test
    void shouldNotCreateReactiveKafkaProducerTemplateWhenItAlreadyExists() {
        contextRunner.withBean(
                ReactiveKafkaProducerTemplate.class,
                () -> new ReactiveKafkaProducerTemplate<>(SenderOptions.create())
        ).run(context -> {
            assertThat(context).doesNotHaveBean(SenderOptions.class);
            assertThat(context).hasSingleBean(ReactiveKafkaProducerTemplate.class);
        });
    }

    /**
     * No extra {@link ReceiverOptions} beans should be created if such a bean already exists.
     */
    @Test
    void shouldNotCreateReceiverOptionsWhenItAlreadyExists() {
        contextRunner.withBean(ReceiverOptions.class, ReceiverOptions::create)
                .withPropertyValues("spring.kafka.consumer.subscribeTopics=foo")
                .run(context -> {
                    assertThat(context).hasSingleBean(ReceiverOptions.class);
                    assertThat(context).hasSingleBean(ReactiveKafkaConsumerTemplate.class);
                });
    }

    /**
     * Neither {@link ReceiverOptions} nor {@link ReactiveKafkaConsumerTemplate} bean should be created
     * if a {@link ReactiveKafkaConsumerTemplate} bean already exists.
     */
    @Test
    void shouldNotCreateReactiveKafkaConsumerTemplateWhenItAlreadyExists() {
        contextRunner.withBean(
                ReactiveKafkaConsumerTemplate.class,
                () -> new ReactiveKafkaConsumerTemplate<>(ReceiverOptions.create())
        ).run(context -> {
            assertThat(context).doesNotHaveBean(ReceiverOptions.class);
            assertThat(context).hasSingleBean(ReactiveKafkaConsumerTemplate.class);
        });
    }

    /**
     * {@link SenderOptionsCustomizer} should be invoked when creating {@link SenderOptions}.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldCustomizeSenderOptions() {
        var customizer = mock(SenderOptionsCustomizer.class);
        var captor = ArgumentCaptor.forClass(SenderOptions.class);
        when(customizer.customize(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        contextRunner.withBean(SenderOptionsCustomizer.class, () -> customizer)
                .run(context -> assertThat(captor.getValue()).isEqualTo(context.getBean(SenderOptions.class)));
    }

    /**
     * {@link ReceiverOptionsCustomizer} should be invoked when creating {@link ReceiverOptions}.
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldCustomizeReceiverOptions() {
        var customizer = mock(ReceiverOptionsCustomizer.class);
        var captor = ArgumentCaptor.forClass(ReceiverOptions.class);
        when(customizer.customize(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        contextRunner.withBean(ReceiverOptionsCustomizer.class, () -> customizer)
                .withPropertyValues("spring.kafka.consumer.subscribeTopics=foo")
                .run(context -> assertThat(captor.getValue()).isEqualTo(context.getBean(ReceiverOptions.class)));
    }
}