package com.content.springboot_rest_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity{

    @Column(unique = true, nullable = false, length = 50)
    private String name;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<User> users = new HashSet<>();

}
