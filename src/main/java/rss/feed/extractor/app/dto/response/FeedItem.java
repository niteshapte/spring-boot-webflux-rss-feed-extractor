package rss.feed.extractor.app.dto.response;

import javax.xml.bind.annotation.XmlElement;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedItem {

	private String title;
	
    private String description;
    
    private Object link;
    
    private String pubDate;
    
    private String image;
    
    private String guid;
    
    @XmlElement(name = "content", namespace="http://www.w3.org/XML/1998/namespace", required = true)
    private String content;
    
    @XmlElement(name = "enclosure")
    private String enclosure;
    
    private String contentDescription;
}
