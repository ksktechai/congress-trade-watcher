package nz.co.ksktech.congresstrades.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import nz.co.ksktech.congresstrades.domain.Member;

@ApplicationScoped
public class MemberRepository implements PanacheRepository<Member> {

  public Optional<Member> findByFullName(String fullName) {
    return find("fullName", fullName).firstResultOptional();
  }
}
