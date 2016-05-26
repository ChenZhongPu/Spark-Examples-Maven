import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static com.intellij.notification.Notifications.*;

/**
 * Created by chenzhongpu on 5/25/16.
 */
public class MVNToCompileAction extends AnAction{

    private String projectBasePath;
    private File examplesDir;

    private DocumentBuilderFactory documentBuilderFactory;
    private XPath xPath;

    public MVNToCompileAction() {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        xPath = XPathFactory.newInstance().newXPath();
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        File[] files = examplesDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".iml");
            }
        });

        // save first
        File cacheIML = new File(System.getProperty("user.home") + File.separator + ".spark-examples-maven");

        try {
            if (!cacheIML.exists()) {
                if (cacheIML.mkdir())
                {
                    Files.copy(files[0].toPath(),
                            new File(cacheIML.getAbsolutePath() + File.separator + files[0].getName()).toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(files[0]);
            NodeList nodeList = document.getElementsByTagName("orderEntry");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element)nodeList.item(i);
                if (element.getAttribute("scope").equals("PROVIDED")) {
                    element.setAttribute("scope", "COMPILE");
                }
            }

            // write into file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new File(files[0].getAbsolutePath()));
            transformer.transform(source, result);

            Bus.notify(new Notification("BakckgroundChibiChara", "Spark examples maven",
                    "Maven Srope has been RunTime. Synchronize Examples to make effect right now", NotificationType.INFORMATION));

        } catch (Exception e) {
            Bus.notify(new Notification("BakckgroundChibiChara", "Spark examples maven",
                    e.toString(), NotificationType.ERROR));
        }

    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);

        projectBasePath = project.getBasePath();

        if (projectBasePath != null) {
            String rootPomPath = projectBasePath + File.separator + "pom.xml";
            File rootPomFile = new File(rootPomPath);
            examplesDir = new File(projectBasePath + File.separator + "examples");
            if (rootPomFile.exists()) {
                try {
                    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                    Document document = documentBuilder.parse(rootPomFile);
                    NodeList groupNodeList = (NodeList)xPath.evaluate("/project/groupId", document.getDocumentElement(), XPathConstants.NODESET);
                    NodeList artifactNodeList = (NodeList)xPath.evaluate("/project/artifactId", document.getDocumentElement(), XPathConstants.NODESET);
                    if (groupNodeList.item(0).getTextContent().equals("org.apache.spark") &&
                            artifactNodeList.item(0).getTextContent().startsWith("spark-parent")) {
                        e.getPresentation().setEnabled(true);
                    }
                    else {
                        e.getPresentation().setEnabled(false);
                    }
                } catch (Exception ex) {
                    e.getPresentation().setEnabled(false);
                }

            }
            else {
                e.getPresentation().setEnabled(false);
            }
        }
        else {
            e.getPresentation().setEnabled(false);
        }

    }


}
