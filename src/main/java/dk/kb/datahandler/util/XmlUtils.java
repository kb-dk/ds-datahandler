package dk.kb.datahandler.util;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XmlUtils {

    /**
     * Extract sub-nodes of type tagName that are direct children to the parent tag. 
     * 
     * @param elements List of elements
     * @param tagName name of children tag to extract
     * @return Total list of elements of the tag type that are direct children to the top nodes
     */
        
    public static List<Element> getDirectChildrenByTag(List<Element> elements, String tagName) {
        List<Element> res = new ArrayList<>();

        for (Element element : elements) {
            NodeList allChildren = element.getElementsByTagName(tagName);
            for (int i = 0; i < allChildren.getLength(); i++) {
                if (allChildren.item(i).getParentNode().equals(element))
                    res.add((Element) allChildren.item(i));
            }
        }

        return res;
    }

    /**
     * 
     * @param elements List of elements
     * @param tagName list of tags that will be exctraced recursive.
     * @return Total list of elements of direct children to the recursive tagName list.
     */
    public static List<Element> getDirectChildrenByTagRecursive(List<Element> elements, String... tagNames) {
        List<Element> previous = elements;
        for (String tag : tagNames) {
            previous = getDirectChildrenByTag(previous, tag);
        }        

        return previous;
    }

    
}
