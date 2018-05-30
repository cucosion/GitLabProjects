package com.ppolivka.gitlabprojects.checkout;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBImageIcon;
import com.ppolivka.gitlabprojects.api.dto.ProjectDto;
import com.ppolivka.gitlabprojects.api.dto.ServerDto;
import com.ppolivka.gitlabprojects.common.GitLabIcons;
import com.ppolivka.gitlabprojects.configuration.SettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import static com.intellij.ui.JBColor.WHITE;
import static com.ppolivka.gitlabprojects.util.MessageUtil.showErrorDialog;

/**
 * Dialog displayed when checking out new project
 *
 * @author ppolivka
 * @since 28.10.2015
 */
public class GitLabCheckoutDialog extends DialogWrapper {

    private static final Logger LOG = Logger.getInstance("#com.ppolivka.gitlabprojects.checkout.GitLabCheckoutDialog");

    private JPanel mainView;
    private JButton refreshButton;
    private JTree allProjects;
    private JComboBox serverList;
    private JTextField filterTxtField;

    private SettingsState settingsState;

    private String lastUsedUrl = "";
    private Project project;


    private DefaultTreeCellRenderer loadingCellRenderer;
    private DefaultTreeCellRenderer filterCellRenderer;

    GitLabCheckoutDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        init();
    }

    @Override
    protected void init() {
        super.init();
        setTitle("GitLab Checkout");
        setHorizontalStretch(2);
        setOKButtonText("Checkout");

        Border emptyBorder = BorderFactory.createCompoundBorder();
        refreshButton.setBorder(emptyBorder);

        filterTxtField.addActionListener(evt -> {
            TreeModel model = allProjects.getModel();
            allProjects.setModel(null);
            allProjects.setModel(model);
        });

        settingsState = SettingsState.getInstance();

        ArrayList<ServerDto> servers = new ArrayList<>(settingsState.getAllServers());
        CollectionComboBoxModel collectionComboBoxModel = new CollectionComboBoxModel(servers, servers.get(0));
        serverList.setModel(collectionComboBoxModel);



        loadingCellRenderer = new DefaultTreeCellRenderer();

        filterCellRenderer = new Renderer();

        filterCellRenderer.setClosedIcon(AllIcons.Nodes.Folder);
        filterCellRenderer.setOpenIcon(AllIcons.Nodes.Folder);
        filterCellRenderer.setLeafIcon(GitLabIcons.gitLabIcon);

        loadingCellRenderer.setBackgroundNonSelectionColor(WHITE);
        JBImageIcon loadingIcon = GitLabIcons.loadingIcon;
        loadingIcon.setImageObserver(allProjects);
        loadingCellRenderer.setLeafIcon(loadingIcon);
        loadingCellRenderer.setTextNonSelectionColor(JBColor.GRAY);

        allProjects.setCellRenderer(filterCellRenderer);
        allProjects.setScrollsOnExpand(true);
        allProjects.setAutoscrolls(true);
        allProjects.setDragEnabled(false);
        MouseListener ml = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                okAction(false);
                int selRow = allProjects.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = allProjects.getPathForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    DefaultMutableTreeNode selectedNode =
                            ((DefaultMutableTreeNode) (selPath != null ? selPath.getLastPathComponent() : null));
                    String url;
                    if (selectedNode.getChildCount() == 0 && !allProjects.isRootVisible()) {
                        url = selectedNode.toString();
                        okAction(true);
                        lastUsedUrl = url;
                        if (e.getClickCount() == 2) {
                            close(OK_EXIT_CODE);
                        }
                    }
                }
            }
        };
        allProjects.addMouseListener(ml);
        refreshButton.addActionListener(e -> refreshTree());
        Set<ProjectDto> projectDtos = settingsState.getProjects();
        reDrawTree(projectDtos == null ? noProjects() : projectDtos);
        serverList.addActionListener(e -> refreshTree());

    }

    private void okAction(boolean enabled) {
        try {
            setOKActionEnabled(enabled);
        } catch (Throwable t) {
            // do nothing
            LOG.debug("Error changing status of OK action.", t);
        }
    }

    public String getLastUsedUrl() {
        return lastUsedUrl;
    }

    private void refreshTree() {
        if(serverList.getSelectedItem() == null) {
            return;
        }
        treeLoading();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Refreshing Tree..") {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    settingsState.reloadProjects((ServerDto) serverList.getSelectedItem());
                    Set<ProjectDto> projectDtos = settingsState.getProjects();
                    reDrawTree(projectDtos == null ? noProjects() : projectDtos);
                } catch (Throwable e) {
                    showErrorDialog(project, "Cannot log-in to GitLab Server with provided token", "Cannot Login To GitLab");
                }
            }
        });


    }

    private Set<ProjectDto> noProjects() {
        return new HashSet<>();
    }

    private void treeLoading() {
        allProjects.setCellRenderer(loadingCellRenderer);
        allProjects.setRootVisible(true);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("loading...");
        allProjects.setModel(new DefaultTreeModel(root));
    }

    private void reDrawTree(Set<ProjectDto> projectDtos) {
        allProjects.setCellRenderer(filterCellRenderer);
        allProjects.setRootVisible(false);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("My Projects");
        Map<String, DefaultMutableTreeNode> namespaceMap = new HashMap<>();
        for (ProjectDto projectDto : projectDtos) {
            String namespace = projectDto.getNamespace();
            DefaultMutableTreeNode namespaceNode;
            if (namespaceMap.containsKey(namespace)) {
                namespaceNode = namespaceMap.get(namespace);
            } else {
                namespaceNode = new DefaultMutableTreeNode(namespace);
                namespaceMap.put(namespace, namespaceNode);
            }

            DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(projectDto.getName());
            DefaultMutableTreeNode sshNode = new DefaultMutableTreeNode(projectDto.getSshUrl());
            projectNode.add(sshNode);
            DefaultMutableTreeNode httpNode = new DefaultMutableTreeNode(projectDto.getHttpUrl());
            projectNode.add(httpNode);
            namespaceNode.add(projectNode);
        }
        for (DefaultMutableTreeNode namespaceNode : namespaceMap.values()) {
            root.add(namespaceNode);
        }
        allProjects.setModel(new DefaultTreeModel(root));


    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainView;
    }

    public class Renderer extends DefaultTreeCellRenderer{

        private JLabel lblNull = new JLabel();

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean arg2, boolean arg3, boolean arg4, int arg5, boolean arg6) {

            Component c = super.getTreeCellRendererComponent(tree, value, arg2, arg3, arg4, arg5, arg6);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            if (matchesFilter(node)) {
                c.setForeground(JBColor.BLACK);
                return c;
            }
            else if (containsMatchingChild(node)) {
                c.setForeground(JBColor.GRAY);
                return c;
            }
            else {
                return lblNull;
            }
        }

        private boolean matchesFilter(DefaultMutableTreeNode node) {
            return node.toString().contains(filterTxtField.getText());
        }

        private boolean containsMatchingChild(DefaultMutableTreeNode node) {
            Enumeration<DefaultMutableTreeNode> e = node.breadthFirstEnumeration();
            while (e.hasMoreElements()) {
                if (matchesFilter(e.nextElement())) {
                    return true;
                }
            }

            return false;
        }
    }
}
