package rss.feed.extractor.app.service;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public interface OutboundService {

	public Mono<String> fetchNewsFeed(String newsFeedLink);
}
