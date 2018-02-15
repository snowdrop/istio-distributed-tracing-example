package me.snodrop.istio.dt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@Controller
public class SuggestionApplication {

	public static void main(String args[]) {
		SpringApplication.run(SuggestionApplication.class, args);
	}

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public AsyncRestTemplate asyncRestTemplate() {
	    return new AsyncRestTemplate();
    }

    @GetMapping("")
    public String index() {
	    return "index";
    }

}
