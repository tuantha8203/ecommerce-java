package tuantha8203.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(ProducerFactory<Object, Object> producerFactory) {
        // record.value() có thể là byte[] thô (message parse lỗi từ ErrorHandlingDeserializer, xem 6.3)
        // hoặc object domain bình thường (listener ném exception sau khi đã deserialize thành công) —
        // 2 loại này cần 2 serializer khác nhau khi đẩy sang topic .DLT.

        var dlqProducerFactory = new DefaultKafkaProducerFactory<>(
                producerFactory.getConfigurationProperties(),
                new StringSerializer(),
                new DelegatingByTypeSerializer(Map.of(
                        byte[].class, new ByteArraySerializer(),
                        Object.class, new JacksonJsonSerializer<>()
                ), true));
        var recoverer = new DeadLetterPublishingRecoverer(new KafkaTemplate<>(dlqProducerFactory));
        // retry 3 lần cách nhau 1s ngay tại partition đó, hết retry mới đẩy sang topic `<topic>.DLT`
        var backOff = new FixedBackOff(1000L, 3);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
