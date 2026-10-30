package atf.reporting.standalone;

import java.io.StringReader;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import org.w3c.dom.*;

/** Test-only boundary inspection; production reporting never acquires project-discovery dependencies. */
final class StandaloneBoundaryRules {
    private static final Pattern EXTERNAL_IMPORT = Pattern.compile("^\\s*import\\s+(?:static\\s+)?atf\\.(?!reporting\\.)");
    private StandaloneBoundaryRules() { }
    static boolean externalAtfImport(String line) { return EXTERNAL_IMPORT.matcher(line).find(); }
    static boolean hasAtfDependency(String pom) throws Exception {
        var dependencies = document(pom).getElementsByTagNameNS("*", "dependency");
        for (int index=0;index<dependencies.getLength();index++) {
            Element dependency=(Element)dependencies.item(index);
            String group=value(dependency,"groupId"),artifact=value(dependency,"artifactId");
            if(group.equals("atf.api")||group.equals("${project.groupId}")||artifact.startsWith("atf-")) return true;
        }
        return false;
    }
    static boolean hasParent(String pom) throws Exception {
        Node root=document(pom).getDocumentElement();
        for(Node child=root.getFirstChild();child!=null;child=child.getNextSibling())
            if(child instanceof Element element&&"parent".equals(element.getLocalName())) return true;
        return false;
    }
    private static String value(Element element,String name) {
        var values=element.getElementsByTagNameNS("*",name);
        return values.getLength()==0?"":values.item(0).getTextContent().trim();
    }
    private static Document document(String pom) throws Exception {
        var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(pom)));
    }
}
