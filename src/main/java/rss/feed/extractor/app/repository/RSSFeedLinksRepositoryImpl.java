package rss.feed.extractor.app.repository;

import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import rss.feed.extractor.app.dto.db.RSSFeedLinkDataDbDTO;

@Log4j2
@RequiredArgsConstructor
@Repository
public class RSSFeedLinksRepositoryImpl implements RSSFeedLinksRepository {
	
	private final DatabaseClient databaseClient;
	
	private final R2dbcConverter converter;
	
	@Override
	public Flux<RSSFeedLinkDataDbDTO> getNewsFeedLinks() {
		String sql = "SELECT link FROM the.table";
		
		return databaseClient.sql(sql)
				.map((k,v) -> converter.read(RSSFeedLinkDataDbDTO.class, k, v)).all().defaultIfEmpty(new RSSFeedLinkDataDbDTO()).doOnError(e -> log.error(e));
	}
}
