package rss.feed.extractor.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Log4j2	
@Service
public class OutboundServiceImpl implements OutboundService {
	
	@Qualifier("WebClientWithTimeout")
	@Autowired
	private WebClient.Builder builder;

	public Mono<String> fetchNewsFeed(String newsFeedLink) {
		return builder.baseUrl(newsFeedLink)
				.build()
				.get()
				.accept(MediaType.APPLICATION_RSS_XML)
				.retrieve()
				.bodyToMono(String.class)
				.subscribeOn(Schedulers.boundedElastic())
				.onErrorResume(e -> {
	                log.error("Error occurred while making the call to {}. Error message: {}", newsFeedLink, e.getMessage());
	                return Mono.just("FAILED");
	            });
	}
}