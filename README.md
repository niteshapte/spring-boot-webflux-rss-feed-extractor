# spring-boot-webflux-rss-feed-extractor
Extract news from RSS Feeds

Tested with 1500+ news feed URLs and it took around 4 mins 30 secs.

Adjust the buffer size and delay elements according to your need. 
<code>
public void extractData() {
			repository.getNewsFeedLinks()
			.filter(f -> null != f && null != f.getFeedLink())
			.buffer(100)
		    .delayElements(Duration.ofSeconds(5))
		    .parallel() 
	        .runOn(Schedulers.parallel()) 
			.doOnNext(newsFeedLinkList -> handle(newsFeedLinkList))
			.subscribe();
}
</code> 
