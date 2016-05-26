import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Created by chenzhongpu on 5/25/16.
 */
public class MVNRestoreAction extends AnAction{

    private String projectBasePath;
    private File examplesDir;

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        File cacheIML = new File(System.getProperty("user.home") +
                File.separator + ".spark-examples-maven" + examplesDir.getAbsolutePath());

        try {
            if (cacheIML.exists())
            {
                File[] files = cacheIML.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".iml");
                    }
                });

                File desFile = new File(examplesDir.getAbsolutePath() + File.separator +
                        files[0].getName());

                Files.copy(files[0].toPath(), desFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            Notifications.Bus.notify(new Notification("BakckgroundChibiChara", "Spark examples maven",
                    "Maven Srope has been restored. Synchronize Examples to make effect right now.", NotificationType.INFORMATION));
        }
        catch (Exception e) {
            Notifications.Bus.notify(new Notification("BakckgroundChibiChara", "Spark examples maven",
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
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                try {
                    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                    Document document = documentBuilder.parse(rootPomFile);
                    XPath xPath = XPathFactory.newInstance().newXPath();
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
