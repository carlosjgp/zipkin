/*
 * Copyright 2015-2018 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin.autoconfigure.collector.kafka;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.collector.Collector;
import zipkin2.collector.CollectorMetrics;
import zipkin2.collector.CollectorSampler;
import zipkin2.collector.kafka08.KafkaCollector;
import zipkin2.storage.InMemoryStorage;
import zipkin2.storage.StorageComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

public class ZipkinKafkaCollectorAutoConfigurationTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  AnnotationConfigApplicationContext context;

  @After
  public void close() {
    if (context != null) {
      context.close();
    }
  }

  @Test
  public void doesntProvidesCollectorComponent_whenKafkaZooKeeperUnset() {
    context = new AnnotationConfigApplicationContext();
    context.register(
        PropertyPlaceholderAutoConfiguration.class,
        ZipkinKafkaCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    thrown.expect(NoSuchBeanDefinitionException.class);
    context.getBean(Collector.class);
  }

  @Test
  public void providesCollectorComponent_whenZooKeeperSet() {
    context = new AnnotationConfigApplicationContext();
    addEnvironment(context, "zipkin.collector.kafka.zookeeper:localhost");
    context.register(
        PropertyPlaceholderAutoConfiguration.class,
        ZipkinKafkaCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    assertThat(context.getBean(KafkaCollector.class)).isNotNull();
  }

  @Test
  public void canOverrideProperty_topic() {
    context = new AnnotationConfigApplicationContext();
    addEnvironment(
        context,
        "zipkin.collector.kafka.zookeeper:localhost",
        "zipkin.collector.kafka.topic:zapkin");
    context.register(
        PropertyPlaceholderAutoConfiguration.class,
        ZipkinKafkaCollectorAutoConfiguration.class,
        InMemoryConfiguration.class);
    context.refresh();

    assertThat(context.getBean(ZipkinKafkaCollectorProperties.class).getTopic())
        .isEqualTo("zapkin");
  }

  @Configuration
  static class InMemoryConfiguration {
    @Bean
    CollectorSampler sampler() {
      return CollectorSampler.ALWAYS_SAMPLE;
    }

    @Bean
    CollectorMetrics metrics() {
      return CollectorMetrics.NOOP_METRICS;
    }

    @Bean
    StorageComponent storage() {
      return InMemoryStorage.newBuilder().build();
    }
  }
}
