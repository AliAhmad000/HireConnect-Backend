package com.hireconnect.profile.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "candidate_preferred_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatePreferredLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String location;
}