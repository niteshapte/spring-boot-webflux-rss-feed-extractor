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
		String sql = "SELECT DISTINCT b.id, "
				+ "a.channel_rss_uuid, "
				+ "a.name channel_name, "
				+ "b.channel_rss_feed_uuid, "
				+ "b.name category, "
				+ "b.lang, "
				+ "a.country_code, "
				+ "b.link channel_rss_feed_link "
				+ "FROM "
				+ "tabloid.channels_rss a, "
				+ "tabloid.channel_rss_feed b "
				+ "WHERE "
				+ "a.channel_rss_uuid = b.channel_rss_uuid "
				+ "AND a.is_enabled = 'YES' "
				+ "AND b.is_enabled = 'YES' "
				+ "ORDER BY b.id ASC";
		
		return databaseClient.sql(sql)
				.map((k,v) -> converter.read(RSSFeedLinkDataDbDTO.class, k, v)).all().defaultIfEmpty(new RSSFeedLinkDataDbDTO()).doOnError(e -> log.error(e));
	}
}