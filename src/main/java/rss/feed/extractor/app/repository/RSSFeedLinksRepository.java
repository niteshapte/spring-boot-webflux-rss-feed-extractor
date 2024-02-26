package rss.feed.extractor.app.repository;

import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import rss.feed.extractor.app.dto.db.RSSFeedLinkDataDbDTO;

@Repository
public interface RSSFeedLinksRepository { 
	
	public Flux<RSSFeedLinkDataDbDTO> getNewsFeedLinks();
}