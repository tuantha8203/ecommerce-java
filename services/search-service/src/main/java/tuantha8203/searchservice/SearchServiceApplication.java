package tuantha8203.searchservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import tuantha8203.common.config.KafkaErrorHandlerConfig;

@SpringBootApplication
@Import(KafkaErrorHandlerConfig.class)
public class SearchServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SearchServiceApplication.class, args);
	}

}
