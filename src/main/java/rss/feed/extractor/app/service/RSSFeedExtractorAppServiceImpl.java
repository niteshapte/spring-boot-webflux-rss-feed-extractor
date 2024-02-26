package rss.feed.extractor.app.service;

import java.io.StringReader;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import rss.feed.extractor.app.dto.db.RSSFeedLinkDataDbDTO;
import rss.feed.extractor.app.dto.response.FeedItem;
import rss.feed.extractor.app.repository.RSSFeedLinksRepository;

@RequiredArgsConstructor
@Log4j2
@Service
public class RSSFeedExtractorAppServiceImpl implements RSSFeedExtractorAppService {
	
	private final RSSFeedLinksRepository repository;
	
	private final OutboundService outboundService;
	
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
	
	private void handle(List<RSSFeedLinkDataDbDTO> newsFeedLinkList) {
		Flux.fromIterable(newsFeedLinkList)
        .flatMap(newsFeedLinkDataDbDTO -> fetchNewsAndSaveToDB(newsFeedLinkDataDbDTO))
        .subscribe();
    }
	
	private Mono<Void> fetchNewsAndSaveToDB(RSSFeedLinkDataDbDTO newsFeedLinkDataDbDTO) {
		return outboundService.fetchNewsFeed(newsFeedLinkDataDbDTO.getFeedLink())
		.flatMap(rawXML -> {
            execute(rawXML, newsFeedLinkDataDbDTO).subscribe(); // Must be subscribed and not returned
            return Mono.empty();
        })
		.onErrorResume(e -> {
			log.error(e.getMessage());
			return Mono.empty();
		})
        .then();
	}
	
	private Mono<Void> execute(String rawXML, RSSFeedLinkDataDbDTO newsFeedLinkDataDbDTO) {
		return Mono.just(rawXML)
		.filter(f -> !f.equals("FAILED"))
		.subscribeOn(Schedulers.parallel())
		.flatMap(newsFeedXML -> sanitizeNewsFeedXML(newsFeedLinkDataDbDTO.getFeedLink(), newsFeedXML))
		.filter(f -> !f.equals("NA"))
		.flatMapMany(this::convertXMLToRssFeedItems)
		.filter(f -> filter())
		.flatMap(item -> {
			synchronized (item) {
				log.info("Item {}", item);
				// operation on items
				return Mono.empty();
			}
		})
		.onErrorResume(e -> {
			log.error(e.getMessage());
			return Mono.empty();
		}).then();
	}

	private boolean filter() {
		// Your filter logic
		return true;
	}
	
	private Mono<String> sanitizeNewsFeedXML(String newsFeedLink, String newsFeedXML) {
		if(null != newsFeedXML && !newsFeedXML.trim().equals("") && !newsFeedXML.trim().startsWith("<")) {
    		log.error("Response from RSS LINK {} is not XML.", newsFeedLink);
    		return Mono.just("NA");
    	}
		
		if (newsFeedXML.trim().matches("(?s).*<\\s*html.*>.*<\\s*/html\\s*>.*")) {
    		log.error("Response from RSS LINK {} was HTML.", newsFeedLink);
    		return Mono.just("NA");
        }
		
		String unescapedXML = StringEscapeUtils.unescapeHtml4(newsFeedXML);
    	
    	org.jsoup.nodes.Document cleanedDoc = Jsoup.parse(unescapedXML, "", Parser.xmlParser());
        String cleanedXML = cleanedDoc.toString().replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll(" async ", "").replaceAll("&", "&amp;").strip().trim();
        
		return Mono.just(cleanedXML);
	}
	
	public Flux<FeedItem> convertXMLToRssFeedItems(String sanitizedNewsFeedXML) {
		try {
        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(sanitizedNewsFeedXML)));

            NodeList itemNodeList = document.getElementsByTagName("item");
            
            Set<FeedItem> itemList = new HashSet<FeedItem>();

            for (int i = 0; i < itemNodeList.getLength(); i++) {
                Element item = (Element) itemNodeList.item(i);

                String title = getElementValue(item, "title");
                String image = getElementValue(item, "image");
                String thumbimage = getElementValue(item, "thumbimage");
                String encodedImage = extractImageUrlFromContent(item);
                String descImage = extractImageUrlFromDescription(item);
                String link = getLinkValue(item);
                String description = getElementValue(item, "description");
                String contentDescription = getElementValue(item, "media:content");
                String pubDate = getPublishedDateElementValue(item, "pubDate");
                String content = getElementAttribute(item, "media:content", "url");
                String thumbnail = getElementAttribute(item, "media:thumbnail", "url");
                String enclosure = getElementAttribute(item, "enclosure", "url");
                
                if(null == image || "".equals(image)) {
                	if(null != content && !content.isEmpty()) {
                		image = content;
                	} else if (null != enclosure && !enclosure.isEmpty()) {
                		image = enclosure;
                	} else if (null != thumbnail && !thumbnail.isEmpty()) {
                		image = thumbnail;
                	} else if(null != thumbimage && !thumbimage.isEmpty()) {
            			image = thumbimage;
            		} else if (null != encodedImage && !encodedImage.isEmpty()){
            			image = encodedImage;
            		} else if (null != descImage && !descImage.isEmpty()){
            			image = descImage;
            		} else {
            			image = null;
            		}
                }
                
                FeedItem feedItem = FeedItem.builder().title(title).link(link).description(description).image(image).pubDate(pubDate).content(content).enclosure(enclosure).contentDescription(contentDescription).build();
                
                if(null != feedItem) {
                	itemList.add(feedItem);
                }
            }
			return Flux.fromIterable(itemList);
		} catch (Exception e) {
			log.error("Exception occured while creating FeedItem object. Exception {}", e.getMessage());
			return Flux.empty();
		}
	}
	
	private String extractImageUrlFromContent(Element item) {
	    String contentEncoded = getElementValue(item, "content:encoded");
	    
	    if(null != contentEncoded && !"".equals(contentEncoded)) {
	    	return extractImageUrlFromHtml(contentEncoded);
	    } else {
	    	return null;
	    }
	}
	
	private String extractImageUrlFromDescription(Element item) {
	    String description = getElementValue(item, "description");
	    
	    if(null != description && !"".equals(description)) {
	    	return extractImageUrlFromHtml(description);
	    } else {
	    	return null;
	    }
	}

	private String extractImageUrlFromHtml(String html) {
	    try {
	    	org.jsoup.nodes.Document doc = Jsoup.parse(html);
	        Elements imgElements = doc.select("figure img[src]");
	        
	        if (!imgElements.isEmpty()) {
	            return imgElements.first().attr("src");
	        }
	        
	        Elements imgElements2 = doc.select("p img[src]");
	        
	        if (!imgElements2.isEmpty()) {
	            return imgElements2.first().attr("src");
	        }
	        
	        Elements imgElements3 = doc.select("img[src]");
	        
	        if (!imgElements3.isEmpty()) {
	            return imgElements3.first().attr("src");
	        }
	        
	    } catch (Exception e) {
	        log.error("Error extracting image URL: {}", e.getMessage());
	    }
	    return null;
	}
	
	private static String getElementValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return null;
    }
	
	private static String getPublishedDateElementValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        } else {
        	NodeList nodeList2 = element.getElementsByTagName("publishedDate");
        	if (nodeList2.getLength() > 0) {
                return nodeList2.item(0).getTextContent().trim();
            } else {
            	NodeList nodeList3 = element.getElementsByTagName("pubdate");
            	if (nodeList3.getLength() > 0) {
                    return nodeList3.item(0).getTextContent().trim();
                }
            }
        }
        return null;
    }

    private static String getElementAttribute(Element element, String tagName, String attributeName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Element tagElement = (Element) nodeList.item(0);
            return tagElement.getAttribute(attributeName).trim();
        }
        return null;
    }

    private static String getLinkValue(Element item) {
        NodeList linkNodes = item.getElementsByTagName("link");
        NodeList atomLinkNodes = item.getElementsByTagName("atom:link");

        if (linkNodes.getLength() > 0) {
            Element linkElement = (Element) linkNodes.item(0);
            return linkElement.getTextContent().trim();
        } else if (atomLinkNodes.getLength() > 0) {
            Element atomLinkElement = (Element) atomLinkNodes.item(0);
            return atomLinkElement.getTextContent().trim();
        }
        return null;
    }
}