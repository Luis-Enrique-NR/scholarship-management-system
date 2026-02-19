package pe.com.security.scholarship.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "partners")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Partner {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 256)
  private String idClient;

  @Column(nullable = false, length = 256)
  private String nameClient;

  @Column(length = 256)
  private String secretClient;

  @Column(length = 256)
  private String scopes;

  @Column(length = 256)
  private String grantTypes;

  @Column(length = 256)
  private String authenticationMethods;

  @Column(length = 256)
  private String redirectUri;

  @Column(length = 256)
  private String redirectUriLogout;
}
