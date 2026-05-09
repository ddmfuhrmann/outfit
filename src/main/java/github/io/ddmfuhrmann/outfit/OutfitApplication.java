package github.io.ddmfuhrmann.outfit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class OutfitApplication {

	public static void main(String[] args) {
		SpringApplication.run(OutfitApplication.class, args);
	}

}
