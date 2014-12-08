package org.chronopolis.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by shake on 11/17/14.
 */
@Entity
public class Node {

    @Id
    @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "node")
    private Set<Replication> replications = new HashSet<>();

    @OneToMany(mappedBy = "node")
    private Set<Restoration> restorations = new HashSet<>();

    @JsonIgnore
    public String password;
    public String username;

    // For JPA
    Node() {
    }

    public Node(final String username, final String password) {
        this.password = password;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public Set<Replication> getReplications() {
        return replications;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }
}
