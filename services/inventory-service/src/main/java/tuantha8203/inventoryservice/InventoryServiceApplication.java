package tuantha8203.inventoryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import tuantha8203.common.config.KafkaErrorHandlerConfig;

@SpringBootApplication
@Import(KafkaErrorHandlerConfig.class)
public class InventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);
	}

}
