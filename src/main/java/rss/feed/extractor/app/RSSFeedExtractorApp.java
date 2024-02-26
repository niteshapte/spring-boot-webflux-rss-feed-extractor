package rss.feed.extractor.app;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.config.EnableWebFlux;

import rss.feed.extractor.app.service.RSSFeedExtractorAppService;

@ComponentScan("rss.feed.extractor.app*")
@EnableAutoConfiguration
@EnableWebFlux
@EnableScheduling
@EnableR2dbcRepositories
@SpringBootApplication
public class RSSFeedExtractorApp {
	
	@Autowired
	private RSSFeedExtractorAppService service;
	
	public static void main(String[] args) {
		SpringApplication.run(RSSFeedExtractorApp.class, args);
	}
	
	@PostConstruct
	public void run() {
		service.extractData();
	}
}