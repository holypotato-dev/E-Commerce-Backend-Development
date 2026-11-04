package com.aadil.ecommerce.entities;

import com.aadil.ecommerce.enums.AdminRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "AdminInvitation")
@Getter
@Setter
@NoArgsConstructor
public class AdminInvitationEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private Long invitationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_ID")
    private UserEntity user;

    @Column(updatable = false)
    private String tokenHash;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AdminRequestStatus adminRequestStatus;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

  @CreateTimeStamp (nullable = false)
  private LocalDateTime createdAt;
}
