package com.ppolivka.gitlabprojects.api.dto;

import java.io.Serializable;

/**
 * DTO Class representing one GitLab Project
 *
 * @author ppolivka
 * @since 10.10.2015
 */
public class ProjectDto implements Serializable {
    private String name;
    private String namespace;
    private String sshUrl;
    private String httpUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getSshUrl() {
        return sshUrl;
    }

    public void setSshUrl(String sshUrl) {
        this.sshUrl = sshUrl;
    }

    public String getHttpUrl() {
        return httpUrl;
    }

    public void setHttpUrl(String httpUrl) {
        this.httpUrl = httpUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProjectDto that = (ProjectDto) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;
        if (sshUrl != null ? !sshUrl.equals(that.sshUrl) : that.sshUrl != null) return false;
        return httpUrl != null ? httpUrl.equals(that.httpUrl) : that.httpUrl == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
        result = 31 * result + (sshUrl != null ? sshUrl.hashCode() : 0);
        result = 31 * result + (httpUrl != null ? httpUrl.hashCode() : 0);
        return result;
    }
}
