package com.hireconnect.profile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recruiter_profiles",
       indexes = {
           @Index(name = "idx_rec_user",  columnList = "user_id", unique = true),
           @Index(name = "idx_rec_email", columnList = "email")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecruiterProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(name = "mobile", length = 15)
    private String mobile;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "company_size")
    private String companySize;

    @Column(name = "industry")
    private String industry;

    @Column(name = "website")
    private String website;

    @Column(name = "company_description", columnDefinition = "TEXT")
    private String companyDescription;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "designation")
    private String designation;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
