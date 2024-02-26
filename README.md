# spring-boot-webflux-rss-feed-extractor
Extract news from RSS Feeds

Tested with 1500+ news feed URLs and it took around 4 mins 30 secs.

To adjust the buffer size and delay elements according to your needs in a reactive programming scenario, you can modify the `buffer` and `delayElements` operators as follows:

```java
public void extractData() {
    repository.getNewsFeedLinks()
        .filter(f -> null != f && null != f.getFeedLink())
        .buffer(100)  // Adjust buffer size here
        .delayElements(Duration.ofSeconds(5))  // Adjust delay duration here
        .parallel()
        .runOn(Schedulers.parallel())
        .doOnNext(newsFeedLinkList -> handle(newsFeedLinkList))
        .subscribe();
}

